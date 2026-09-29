package CodeGenerator;

import java.io.*;
import java.util.*;

import Fava.FavaBaseVisitor;
import Fava.FavaParser;
import VM.OpCode;
import VM.Instruction.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class CodeGen extends FavaBaseVisitor<Type> {

    private final ArrayList<Instruction> code = new ArrayList<>();
    private final ArrayList<Object> constantPool = new ArrayList<>();
    private final TypeCheck typeCheck;

    private final LinkedHashMap<String, VarInfo> globals = new LinkedHashMap<>();
    private final LinkedHashMap<String, FuncInfo> functions = new LinkedHashMap<>();
    private final Map<String, List<Integer>> pendingCalls = new LinkedHashMap<>();

    private final Deque<Map<String, VarInfo>> scopes = new ArrayDeque<>();

    private FuncInfo currentFunction = null;
    private int nextLocalAddr = 2;

    public CodeGen(TypeCheck typeCheck) {
        this.typeCheck = typeCheck;
    }

    private String normalize(String id) {
        return id.toLowerCase();
    }

    private int addConstant(Object value) {
        for (int i = 0; i < constantPool.size(); i++) {
            if (constantPool.get(i).equals(value)) {
                return i;
            }
        }
        constantPool.add(value);
        return constantPool.size() - 1;
    }

    private Type typeOf(ParseTree node) {
        return typeCheck.getType(node);
    }

    private boolean isNumeric(Type t) {
        return t == Type.INT || t == Type.REAL;
    }

    private boolean isRealOperation(Type t1, Type t2) {
        return t1 == Type.REAL || t2 == Type.REAL;
    }

    private void emitNumericPromotion(Type from, Type to) {
        if (from == Type.INT && to == Type.REAL) {
            emit(OpCode.itod);
        }
    }

    private void emitAssignmentConversion(Type from, Type to) {
        if (from == Type.INT && to == Type.REAL) {
            emit(OpCode.itod);
        }
    }

    private void visitBinaryOperands(FavaParser.ExprContext left, Type leftType,
                                     FavaParser.ExprContext right, Type rightType) {
        visit(left);
        emitNumericPromotion(leftType, rightType);

        visit(right);
        emitNumericPromotion(rightType, leftType);
    }

    private OpCode equalityOpcode(Type t1, Type t2, String op) {
        boolean equalsOp = op.equals("=");

        if (t1 == Type.STRING && t2 == Type.STRING) {
            return equalsOp ? OpCode.seq : OpCode.sneq;
        }
        if (t1 == Type.BOOL && t2 == Type.BOOL) {
            return equalsOp ? OpCode.beq : OpCode.bneq;
        }
        if (isRealOperation(t1, t2)) {
            return equalsOp ? OpCode.deq : OpCode.dneq;
        }
        return equalsOp ? OpCode.ieq : OpCode.ineq;
    }

    private OpCode arithmeticOpcode(String op, boolean real) {
        return switch (op) {
            case "+" -> real ? OpCode.dadd : OpCode.iadd;
            case "-" -> real ? OpCode.dsub : OpCode.isub;
            case "*" -> real ? OpCode.dmult : OpCode.imult;
            case "/" -> real ? OpCode.ddiv : OpCode.idiv;
            default -> throw new IllegalArgumentException("Unsupported arithmetic op: " + op);
        };
    }

    private void emitStringConversion(Type t) {
        switch (t) {
            case INT -> emit(OpCode.itos);
            case REAL -> emit(OpCode.dtos);
            case BOOL -> emit(OpCode.btos);
            default -> {
            }
        }
    }

    private int currentAddress() {
        return code.size();
    }

    private int emitPlaceholder(OpCode opc) {
        int addr = code.size();
        code.add(new Instruction1Arg(opc, -1));
        return addr;
    }

    private void patchJump(int instructionIndex, int target) {
        code.set(instructionIndex, new Instruction1Arg(code.get(instructionIndex).getOpCode(), target));
    }

    private Type declaredType(FavaParser.TypeContext ctx) {
        if (ctx.BOOLTYPE() != null) return Type.BOOL;
        if (ctx.INTTYPE() != null) return Type.INT;
        if (ctx.REALTYPE() != null) return Type.REAL;
        return Type.STRING;
    }

    private void pushScope() {
        scopes.push(new LinkedHashMap<>());
    }

    private void popScope() {
        scopes.pop();
    }

    private void declareInCurrentScope(String rawName, VarInfo info) {
        scopes.peek().put(normalize(rawName), info);
    }

    private VarInfo lookupVar(String rawName) {
        String name = normalize(rawName);
        for (Map<String, VarInfo> scope : scopes) {
            VarInfo v = scope.get(name);
            if (v != null) return v;
        }
        return globals.get(name);
    }

    private void registerGlobals(FavaParser.ProgContext ctx) {
        int nextGlobal = 0;
        for (FavaParser.GlobalDeclContext gd : ctx.globalDecl()) {
            Type t = declaredType(gd.type());
            for (FavaParser.DeclItemContext item : gd.declItem()) {
                String name = normalize(item.ID().getText());
                globals.put(name, new VarInfo(VarKind.GLOBAL, nextGlobal++, t));
            }
        }
    }

    private void registerFunctionHeaders(FavaParser.ProgContext ctx) {
        for (FavaParser.FuncDeclContext fd : ctx.funcDecl()) {
            String name = normalize(fd.ID().getText());
            Type retType = fd.returnType() == null ? Type.VOID : declaredType(fd.returnType().type());

            List<Type> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();
            if (fd.paramList() != null) {
                for (FavaParser.ParamContext p : fd.paramList().param()) {
                    paramTypes.add(declaredType(p.type()));
                    paramNames.add(p.ID().getText());
                }
            }

            functions.put(name, new FuncInfo(name, retType, paramTypes, paramNames));
        }
    }

    private void patchPendingCalls() {
        for (Map.Entry<String, List<Integer>> e : pendingCalls.entrySet()) {
            FuncInfo fn = functions.get(e.getKey());
            if (fn == null || fn.address < 0) continue;
            for (int idx : e.getValue()) {
                patchJump(idx, fn.address);
            }
        }
    }

    private void emitCall(String fname) {
        String name = normalize(fname);
        FuncInfo fn = functions.get(name);
        if (fn != null && fn.address >= 0) {
            emit(OpCode.call, fn.address);
        } else {
            int idx = emitPlaceholder(OpCode.call);
            pendingCalls.computeIfAbsent(name, k -> new ArrayList<>()).add(idx);
        }
    }

    private boolean blockContainsExplicitReturn(FavaParser.BlockContext ctx) {
        for (FavaParser.StatContext st : ctx.stat()) {
            if (statContainsExplicitReturn(st)) {
                return true;
            }
        }
        return false;
    }

    private boolean statContainsExplicitReturn(FavaParser.StatContext st) {
        if (st instanceof FavaParser.ReturnStatContext) {
            return true;
        }

        if (st instanceof FavaParser.BlockStatContext b) {
            return blockContainsExplicitReturn(b.block());
        }

        if (st instanceof FavaParser.IfStatContext i) {
            return statContainsExplicitReturn(i.stat());
        }

        if (st instanceof FavaParser.IfElseStatContext ie) {
            return statContainsExplicitReturn(ie.stat(0)) ||
                    statContainsExplicitReturn(ie.stat(1));
        }

        if (st instanceof FavaParser.WhileStatContext w) {
            return statContainsExplicitReturn(w.stat());
        }

        return false;
    }

    @Override
    public Type visitProg(FavaParser.ProgContext ctx) {
        registerGlobals(ctx);
        registerFunctionHeaders(ctx);

        for (FavaParser.GlobalDeclContext gd : ctx.globalDecl()) {
            visit(gd);
        }

        emitCall("main");
        emit(OpCode.halt);

        for (FavaParser.FuncDeclContext fd : ctx.funcDecl()) {
            visit(fd);
        }

        patchPendingCalls();
        return null;
    }

    @Override
    public Type visitGlobalDecl(FavaParser.GlobalDeclContext ctx) {
        int n = ctx.declItem().size();
        emit(OpCode.galloc, n);

        for (FavaParser.DeclItemContext item : ctx.declItem()) {
            if (item.expr() != null) {
                VarInfo info = globals.get(normalize(item.ID().getText()));
                Type exprType = typeOf(item.expr());

                visit(item.expr());
                emitAssignmentConversion(exprType, info.type);
                emit(OpCode.gstore, info.addr);
            }
        }

        return null;
    }

    @Override
    public Type visitFuncDecl(FavaParser.FuncDeclContext ctx) {
        String fname = normalize(ctx.ID().getText());
        FuncInfo fn = functions.get(fname);
        fn.address = currentAddress();
        currentFunction = fn;

        nextLocalAddr = 2;
        pushScope();

        int nParams = fn.paramTypes.size();
        for (int i = 0; i < nParams; i++) {
            int addr = i - nParams;
            declareInCurrentScope(fn.paramNames.get(i),
                    new VarInfo(VarKind.PARAM, addr, fn.paramTypes.get(i)));
        }

        visit(ctx.block());

        if (fn.returnType == Type.VOID && !blockContainsExplicitReturn(ctx.block())) {
            emit(OpCode.ret, fn.paramTypes.size());
        }

        popScope();
        currentFunction = null;
        return null;
    }

    @Override
    public Type visitBlock(FavaParser.BlockContext ctx) {
        pushScope();

        int localsInThisBlock = 0;

        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {

                if (child instanceof FavaParser.LocalDeclContext ld) {
                    Type t = declaredType(ld.type());
                    int n = ld.declItem().size();

                    if (n > 0) {
                        emit(OpCode.lalloc, n);
                        localsInThisBlock += n;
                    }

                    for (FavaParser.DeclItemContext item : ld.declItem()) {
                        VarInfo info = new VarInfo(VarKind.LOCAL, nextLocalAddr++, t);
                        declareInCurrentScope(item.ID().getText(), info);

                        if (item.expr() != null) {
                            Type exprType = typeOf(item.expr());
                            visit(item.expr());
                            emitAssignmentConversion(exprType, info.type);
                            emit(OpCode.lstore, info.addr);
                        }
                    }
                } else if (child instanceof FavaParser.StatContext st) {
                    visit(st);
                }
            }
        }

        if (localsInThisBlock > 0) {
            emit(OpCode.pop, localsInThisBlock);
            nextLocalAddr -= localsInThisBlock;
        }

        popScope();
        return null;
    }

    @Override
    public Type visitLocalDecl(FavaParser.LocalDeclContext ctx) {
        return null;
    }

    @Override
    public Type visitPrintStat(FavaParser.PrintStatContext ctx) {
        visit(ctx.expr());

        Type t = typeOf(ctx.expr());
        if (t == Type.ERROR) return Type.ERROR;

        switch (t) {
            case INT -> emit(OpCode.iprint);
            case REAL -> emit(OpCode.dprint);
            case STRING -> emit(OpCode.sprint);
            case BOOL -> emit(OpCode.bprint);
        }
        return null;
    }

    @Override
    public Type visitAssignStat(FavaParser.AssignStatContext ctx) {
        Type exprType = typeOf(ctx.expr());
        if (exprType == Type.ERROR) return Type.ERROR;

        VarInfo info = lookupVar(ctx.ID().getText());
        if (info == null) return Type.ERROR;

        visit(ctx.expr());
        emitAssignmentConversion(exprType, info.type);

        if (info.kind == VarKind.GLOBAL) {
            emit(OpCode.gstore, info.addr);
        } else {
            emit(OpCode.lstore, info.addr);
        }

        return null;
    }

    @Override
    public Type visitBlockStat(FavaParser.BlockStatContext ctx) {
        visit(ctx.block());
        return null;
    }

    @Override
    public Type visitWhileStat(FavaParser.WhileStatContext ctx) {
        int start = currentAddress();

        visit(ctx.expr());
        int jf = emitPlaceholder(OpCode.jumpf);

        visit(ctx.stat());
        emit(OpCode.jump, start);

        int end = currentAddress();
        patchJump(jf, end);

        return null;
    }

    @Override
    public Type visitIfStat(FavaParser.IfStatContext ctx) {
        visit(ctx.expr());
        int jf = emitPlaceholder(OpCode.jumpf);

        visit(ctx.stat());

        int end = currentAddress();
        patchJump(jf, end);

        return null;
    }

    @Override
    public Type visitIfElseStat(FavaParser.IfElseStatContext ctx) {
        visit(ctx.expr());
        int jf = emitPlaceholder(OpCode.jumpf);

        visit(ctx.stat(0));
        int j = emitPlaceholder(OpCode.jump);

        int elseStart = currentAddress();
        patchJump(jf, elseStart);

        visit(ctx.stat(1));

        int end = currentAddress();
        patchJump(j, end);

        return null;
    }

    @Override
    public Type visitReturnStat(FavaParser.ReturnStatContext ctx) {
        if (currentFunction == null) return null;

        if (currentFunction.returnType == Type.VOID) {
            emit(OpCode.ret, currentFunction.paramTypes.size());
            return null;
        }

        if (ctx.expr() != null) {
            Type actual = typeOf(ctx.expr());
            visit(ctx.expr());
            emitAssignmentConversion(actual, currentFunction.returnType);
            emit(OpCode.retval, currentFunction.paramTypes.size());
        }

        return null;
    }

    @Override
    public Type visitCallStat(FavaParser.CallStatContext ctx) {
        visit(ctx.funcCall());
        return null;
    }

    @Override
    public Type visitEmptyStat(FavaParser.EmptyStatContext ctx) {
        return null;
    }

    @Override
    public Type visitCallExpr(FavaParser.CallExprContext ctx) {
        return visit(ctx.funcCall());
    }

    @Override
    public Type visitFuncCall(FavaParser.FuncCallContext ctx) {
        String fname = normalize(ctx.ID().getText());
        FuncInfo fn = functions.get(fname);
        if (fn == null) return Type.ERROR;

        List<FavaParser.ExprContext> actuals = new ArrayList<>();
        if (ctx.argList() != null) {
            actuals.addAll(ctx.argList().expr());
        }

        for (int i = 0; i < actuals.size(); i++) {
            FavaParser.ExprContext arg = actuals.get(i);
            Type actualType = typeOf(arg);
            Type expectedType = fn.paramTypes.get(i);

            visit(arg);
            emitAssignmentConversion(actualType, expectedType);
        }

        emitCall(fname);
        return fn.returnType;
    }

    @Override
    public Type visitIdExpr(FavaParser.IdExprContext ctx) {
        VarInfo info = lookupVar(ctx.ID().getText());
        if (info == null) return Type.ERROR;

        if (info.kind == VarKind.GLOBAL) {
            emit(OpCode.gload, info.addr);
        } else {
            emit(OpCode.lload, info.addr);
        }

        return typeOf(ctx);
    }

    @Override
    public Type visitLiteralExpr(FavaParser.LiteralExprContext ctx) {
        return visit(ctx.literal());
    }

    @Override
    public Type visitParenExpr(FavaParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Type visitIntLiteral(FavaParser.IntLiteralContext ctx) {
        int val = Integer.parseInt(ctx.INTEGER().getText());
        emit(OpCode.iconst, val);
        return Type.INT;
    }

    @Override
    public Type visitRealLiteral(FavaParser.RealLiteralContext ctx) {
        double val = Double.parseDouble(ctx.REAL().getText());
        int idx = addConstant(val);
        emit(OpCode.dconst, idx);
        return Type.REAL;
    }

    @Override
    public Type visitStringLiteral(FavaParser.StringLiteralContext ctx) {
        String text = ctx.STRING().getText();
        String val = text.substring(1, text.length() - 1);
        int idx = addConstant(val);
        emit(OpCode.sconst, idx);
        return Type.STRING;
    }

    @Override
    public Type visitTrueLiteral(FavaParser.TrueLiteralContext ctx) {
        emit(OpCode.tconst);
        return Type.BOOL;
    }

    @Override
    public Type visitFalseLiteral(FavaParser.FalseLiteralContext ctx) {
        emit(OpCode.fconst);
        return Type.BOOL;
    }

    @Override
    public Type visitUnaryExpr(FavaParser.UnaryExprContext ctx) {
        Type t = visit(ctx.expr());
        if (t == Type.ERROR) return Type.ERROR;

        String op = ctx.op.getText().toLowerCase();

        if (op.equals("-")) {
            if (t == Type.INT) {
                emit(OpCode.iuminus);
                return Type.INT;
            }
            if (t == Type.REAL) {
                emit(OpCode.duminus);
                return Type.REAL;
            }
            return Type.ERROR;
        }

        if (t == Type.BOOL) {
            emit(OpCode.not);
            return Type.BOOL;
        }

        return Type.ERROR;
    }

    @Override
    public Type visitAddSubExpr(FavaParser.AddSubExprContext ctx) {
        Type t1 = typeOf(ctx.expr(0));
        Type t2 = typeOf(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) return Type.ERROR;
        if (!isNumeric(t1) || !isNumeric(t2)) return Type.ERROR;

        visitBinaryOperands(ctx.expr(0), t1, ctx.expr(1), t2);
        boolean real = isRealOperation(t1, t2);
        emit(arithmeticOpcode(ctx.op.getText(), real));
        return real ? Type.REAL : Type.INT;
    }

    @Override
    public Type visitMultDivModExpr(FavaParser.MultDivModExprContext ctx) {
        Type l = typeOf(ctx.expr(0));
        Type r = typeOf(ctx.expr(1));
        String op = ctx.op.getText().toLowerCase();

        if (l == Type.ERROR || r == Type.ERROR) return Type.ERROR;

        if (op.equals("mod")) {
            visit(ctx.expr(0));
            visit(ctx.expr(1));
            emit(OpCode.imod);
            return Type.INT;
        }

        visitBinaryOperands(ctx.expr(0), l, ctx.expr(1), r);
        boolean real = isRealOperation(l, r);
        emit(arithmeticOpcode(op, real));
        return real ? Type.REAL : Type.INT;
    }

    @Override
    public Type visitConcatExpr(FavaParser.ConcatExprContext ctx) {
        Type t1 = typeOf(ctx.expr(0));
        Type t2 = typeOf(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) return Type.ERROR;

        visit(ctx.expr(0));
        emitStringConversion(t1);

        visit(ctx.expr(1));
        emitStringConversion(t2);

        emit(OpCode.sconcat);
        return Type.STRING;
    }

    @Override
    public Type visitRelExpr(FavaParser.RelExprContext ctx) {
        Type t1 = typeOf(ctx.expr(0));
        Type t2 = typeOf(ctx.expr(1));
        String op = ctx.op.getText();

        if (t1 == Type.ERROR || t2 == Type.ERROR) return Type.ERROR;

        boolean invert = op.equals(">") || op.equals(">=");
        boolean real = isRealOperation(t1, t2);

        if (invert) {
            visitBinaryOperands(ctx.expr(1), t2, ctx.expr(0), t1);
        } else {
            visitBinaryOperands(ctx.expr(0), t1, ctx.expr(1), t2);
        }

        if (op.equals("<") || op.equals(">")) {
            emit(real ? OpCode.dlt : OpCode.ilt);
        } else {
            emit(real ? OpCode.dleq : OpCode.ileq);
        }

        return Type.BOOL;
    }

    @Override
    public Type visitEqExpr(FavaParser.EqExprContext ctx) {
        Type t1 = typeOf(ctx.expr(0));
        Type t2 = typeOf(ctx.expr(1));
        String op = ctx.op.getText();

        if (t1 == Type.ERROR || t2 == Type.ERROR) return Type.ERROR;

        visitBinaryOperands(ctx.expr(0), t1, ctx.expr(1), t2);
        emit(equalityOpcode(t1, t2, op));
        return Type.BOOL;
    }

    @Override
    public Type visitAndExpr(FavaParser.AndExprContext ctx) {
        Type t1 = typeOf(ctx.expr(0));
        Type t2 = typeOf(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) return Type.ERROR;

        visit(ctx.expr(0));
        visit(ctx.expr(1));
        emit(OpCode.and);
        return Type.BOOL;
    }

    @Override
    public Type visitOrExpr(FavaParser.OrExprContext ctx) {
        Type t1 = typeOf(ctx.expr(0));
        Type t2 = typeOf(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) return Type.ERROR;

        visit(ctx.expr(0));
        visit(ctx.expr(1));
        emit(OpCode.or);
        return Type.BOOL;
    }

    public void emit(OpCode opc) {
        code.add(new Instruction(opc));
    }

    public void emit(OpCode opc, int val) {
        code.add(new Instruction1Arg(opc, val));
    }

    public void dumpInstructions() {
        System.out.println("*** Instructions ***");
        for (int i = 0; i < code.size(); i++) {
            System.out.println(i + ": " + code.get(i));
        }
    }

    public void dumpConstantPool() {
        System.out.println("*** Constant pool ***");
        for (int i = 0; i < constantPool.size(); i++) {
            Object obj = constantPool.get(i);
            if (obj instanceof String) {
                System.out.println(i + ": \"" + obj + "\"");
            } else {
                System.out.println(i + ": " + obj);
            }
        }
    }

    public void saveBytecodes(String filename) throws IOException {
        try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(filename))) {
            dout.writeInt(constantPool.size());

            for (Object obj : constantPool) {
                if (obj instanceof Double d) {
                    dout.writeByte(1);
                    dout.writeDouble(d);
                } else if (obj instanceof String s) {
                    dout.writeByte(2);
                    dout.writeUTF(s);
                }
            }

            for (Instruction inst : code) {
                inst.writeTo(dout);
            }
        }
    }

    public byte[] getBytecodes() throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);

        dout.writeInt(constantPool.size());

        for (Object obj : constantPool) {
            if (obj instanceof Double d) {
                dout.writeByte(1);
                dout.writeDouble(d);
            } else if (obj instanceof String s) {
                dout.writeByte(2);
                dout.writeUTF(s);
            }
        }

        for (Instruction inst : code) {
            inst.writeTo(dout);
        }

        dout.flush();
        return bout.toByteArray();
    }

    private static class VarInfo {
        final VarKind kind;
        final int addr;
        final Type type;

        VarInfo(VarKind kind, int addr, Type type) {
            this.kind = kind;
            this.addr = addr;
            this.type = type;
        }
    }

    private static class FuncInfo {
        final String name;
        final Type returnType;
        final List<Type> paramTypes;
        final List<String> paramNames;
        int address = -1;

        FuncInfo(String name, Type returnType, List<Type> paramTypes, List<String> paramNames) {
            this.name = name;
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.paramNames = paramNames;
        }
    }

    private enum VarKind {
        GLOBAL,
        PARAM,
        LOCAL
    }
}