package CodeGenerator;

import Fava.FavaBaseVisitor;
import Fava.FavaParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.*;

public class TypeCheck extends FavaBaseVisitor<Type> {

    private final ParseTreeProperty<Type> types = new ParseTreeProperty<>();

    public static boolean hasError = false;

    private final Scope globals = new Scope(null);
    private Scope currentScope = globals;

    private final Map<String, FuncSymbol> functions = new LinkedHashMap<>();
    private final Set<FavaParser.FuncDeclContext> invalidFuncDecls = new HashSet<>();

    private FuncSymbol currentFunction = null;

    private final List<SemanticError> errors = new ArrayList<>();
    private final Map<String, Integer> globalDeclLine = new LinkedHashMap<>();

    public Type getType(ParseTree node) {
        return types.get(node);
    }

    private void setType(ParseTree node, Type type) {
        types.put(node, type);
    }

    private String normalize(String id) {
        return id.toLowerCase();
    }

    private void addError(ParserRuleContext ctx, String msg) {
        errors.add(new SemanticError(ctx.start.getLine(), msg));
        hasError = true;
    }

    private void addErrorAtLine(int line, String msg) {
        errors.add(new SemanticError(line, msg));
        hasError = true;
    }

    private void printErrors() {
        errors.sort((a, b) -> Integer.compare(a.line, b.line));
        for (SemanticError e : errors) {
            System.out.println("error in line " + e.line + ": " + e.msg);
        }
    }

    private String typeName(Type t) {
        return switch (t) {
            case INT -> "integer";
            case REAL -> "real";
            case STRING -> "string";
            case BOOL -> "bool";
            case VOID -> "void";
            default -> "unknown";
        };
    }

    private boolean isNumeric(Type t) {
        return t == Type.INT || t == Type.REAL;
    }

    private boolean canAssign(Type dest, Type src) {
        if (dest == Type.ERROR || src == Type.ERROR) return false;
        if (dest == src) return true;
        return dest == Type.REAL && src == Type.INT;
    }

    private Type declaredType(FavaParser.TypeContext ctx) {
        if (ctx.BOOLTYPE() != null) return Type.BOOL;
        if (ctx.INTTYPE() != null) return Type.INT;
        if (ctx.REALTYPE() != null) return Type.REAL;
        return Type.STRING;
    }

    private VarSymbol lookupVariable(String rawName) {
        return currentScope.lookupVar(normalize(rawName));
    }

    private FuncSymbol lookupFunction(String rawName) {
        return functions.get(normalize(rawName));
    }

    private boolean declareVariable(ParserRuleContext ctx, String rawName, Type type, VarKind kind) {
        String name = normalize(rawName);
        if (currentScope.symbols.containsKey(name)) {
            addError(ctx, rawName + " already declared");
            return false;
        }
        currentScope.symbols.put(name, new VarSymbol(name, type, kind));
        return true;
    }

    private void preRegisterGlobalNames(FavaParser.GlobalDeclContext ctx) {
        for (FavaParser.DeclItemContext item : ctx.declItem()) {
            String name = normalize(item.ID().getText());
            globalDeclLine.putIfAbsent(name, item.start.getLine());
        }
    }

    @Override
    public Type visitProg(FavaParser.ProgContext ctx) {
        hasError = false;
        errors.clear();
        globalDeclLine.clear();
        functions.clear();
        invalidFuncDecls.clear();
        currentScope = globals;
        globals.symbols.clear();

        for (FavaParser.GlobalDeclContext gd : ctx.globalDecl()) {
            preRegisterGlobalNames(gd);
        }

        for (FavaParser.FuncDeclContext fd : ctx.funcDecl()) {
            registerFunctionHeader(fd);
        }

        for (FavaParser.GlobalDeclContext gd : ctx.globalDecl()) {
            visit(gd);
        }

        for (FavaParser.FuncDeclContext fd : ctx.funcDecl()) {
            visit(fd);
        }

        // apanha statements ao nível do programa

            if (ctx.children != null) {
                for (ParseTree child : ctx.children) {
                    if (child instanceof FavaParser.StatContext) {
                        visit(child);
                    }
                }
            }


        if (!functions.containsKey("main")) {
            addErrorAtLine(ctx.stop.getLine(), "missing main()");
        }

        printErrors();
        return null;
    }

    private void registerFunctionHeader(FavaParser.FuncDeclContext ctx) {
        String rawName = ctx.ID().getText();
        String name = normalize(rawName);

        if (functions.containsKey(name) || globalDeclLine.containsKey(name)) {
            addError(ctx, rawName + " already declared");
            invalidFuncDecls.add(ctx);
            return;
        }

        Type retType = Type.VOID;
        if (ctx.returnType() != null) {
            retType = declaredType(ctx.returnType().type());
        }

        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();

        if (ctx.paramList() != null) {
            for (FavaParser.ParamContext p : ctx.paramList().param()) {
                paramTypes.add(declaredType(p.type()));
                paramNames.add(p.ID().getText());
            }
        }

        functions.put(name, new FuncSymbol(name, rawName, retType, paramTypes, paramNames));
    }

    @Override
    public Type visitGlobalDecl(FavaParser.GlobalDeclContext ctx) {
        Type declType = declaredType(ctx.type());

        for (FavaParser.DeclItemContext item : ctx.declItem()) {
            String rawName = item.ID().getText();
            boolean declared = declareVariable(item, rawName, declType, VarKind.GLOBAL);

            if (item.expr() != null) {
                Type exprType = visit(item.expr());
                if (declared && exprType != Type.ERROR && !canAssign(declType, exprType)) {
                    addError(item, "operator := is invalid between " + typeName(declType) + " and " + typeName(exprType));
                }
            }
        }

        return null;
    }

    @Override
    public Type visitFuncDecl(FavaParser.FuncDeclContext ctx) {
        if (invalidFuncDecls.contains(ctx)) {
            return null;
        }

        String fname = normalize(ctx.ID().getText());
        FuncSymbol fn = functions.get(fname);
        currentFunction = fn;

        Scope old = currentScope;
        currentScope = new Scope(globals);

        if (ctx.paramList() != null) {
            for (FavaParser.ParamContext p : ctx.paramList().param()) {
                Type pType = declaredType(p.type());
                String rawName = p.ID().getText();
                declareVariable(p, rawName, pType, VarKind.PARAM);
            }
        }

        visit(ctx.block());

        if (fn != null && fn.returnType != Type.VOID && !blockAlwaysReturns(ctx.block())) {
            addErrorAtLine(ctx.stop.getLine(),
                    "missing return in function " + ctx.ID().getText());
        }

        currentScope = old;
        currentFunction = null;
        return null;
    }

    private boolean blockAlwaysReturns(FavaParser.BlockContext ctx) {
        for (FavaParser.StatContext st : ctx.stat()) {
            if (statAlwaysReturns(st)) {
                return true;
            }
        }
        return false;
    }

    private boolean statAlwaysReturns(FavaParser.StatContext st) {
        if (st instanceof FavaParser.ReturnStatContext rs) {
            if (currentFunction == null) return false;
            if (currentFunction.returnType == Type.VOID) return true;
            return rs.expr() != null;
        }

        if (st instanceof FavaParser.BlockStatContext b) {
            return blockAlwaysReturns(b.block());
        }

        if (st instanceof FavaParser.IfElseStatContext ie) {
            return statAlwaysReturns(ie.stat(0)) && statAlwaysReturns(ie.stat(1));
        }

        return false;
    }

    @Override
    public Type visitBlock(FavaParser.BlockContext ctx) {
        Scope old = currentScope;
        currentScope = new Scope(currentScope);

        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof FavaParser.LocalDeclContext) {
                    visit(child);
                } else if (child instanceof FavaParser.StatContext) {
                    visit(child);
                }
            }
        }

        currentScope = old;
        return null;
    }

    @Override
    public Type visitLocalDecl(FavaParser.LocalDeclContext ctx) {
        Type declType = declaredType(ctx.type());

        for (FavaParser.DeclItemContext item : ctx.declItem()) {
            String rawName = item.ID().getText();
            boolean declared = declareVariable(item, rawName, declType, VarKind.LOCAL);

            if (item.expr() != null) {
                Type exprType = visit(item.expr());
                if (declared && exprType != Type.ERROR && !canAssign(declType, exprType)) {
                    addError(item, "operator := is invalid between " + typeName(declType) + " and " + typeName(exprType));
                }
            }
        }

        return null;
    }

    @Override
    public Type visitPrintStat(FavaParser.PrintStatContext ctx) {
        visit(ctx.expr());
        return null;
    }

    @Override
    public Type visitAssignStat(FavaParser.AssignStatContext ctx) {
        String rawName = ctx.ID().getText();
        VarSymbol var = lookupVariable(rawName);
        Type exprType = visit(ctx.expr());

        if (var == null) {
            if (lookupFunction(rawName) != null) {
                addError(ctx, rawName + " is not a variable");
            } else {
                addError(ctx, rawName + " not declared");
            }
            return null;
        }

        if (exprType != Type.ERROR && !canAssign(var.type, exprType)) {
            addError(ctx, "operator := is invalid between " + typeName(var.type) + " and " + typeName(exprType));
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
        Type condType = visit(ctx.expr());
        if (condType != Type.ERROR && condType != Type.BOOL) {
            addError(ctx, "while expression must be of type bool");
        }
        visit(ctx.stat());
        return null;
    }

    @Override
    public Type visitIfStat(FavaParser.IfStatContext ctx) {
        Type condType = visit(ctx.expr());
        if (condType != Type.ERROR && condType != Type.BOOL) {
            addError(ctx, "if expression must be of type bool");
        }
        visit(ctx.stat());
        return null;
    }

    @Override
    public Type visitIfElseStat(FavaParser.IfElseStatContext ctx) {
        Type condType = visit(ctx.expr());
        if (condType != Type.ERROR && condType != Type.BOOL) {
            addError(ctx, "if expression must be of type bool");
        }
        visit(ctx.stat(0));
        visit(ctx.stat(1));
        return null;
    }

    @Override
    public Type visitReturnStat(FavaParser.ReturnStatContext ctx) {
        if (currentFunction == null) {
            addError(ctx, "return outside function");
            return null;
        }

        Type expected = currentFunction.returnType;

        if (ctx.expr() == null) {
            if (expected == Type.VOID) {
                return null;
            }
            return null;
        }

        Type actual = visit(ctx.expr());

        if (expected == Type.VOID) {
            addError(ctx, "function " + currentFunction.rawName + " does not return a value");
            return null;
        }

        if (actual != Type.ERROR && !canAssign(expected, actual)) {
            addError(ctx, "function " + currentFunction.rawName +
                    " must return a value of type " + typeName(expected));
        }

        return null;
    }

    @Override
    public Type visitCallStat(FavaParser.CallStatContext ctx) {
        Type t = visit(ctx.funcCall());
        if (t != Type.ERROR && t != Type.VOID) {
            addError(ctx, "value of function " + ctx.funcCall().ID().getText() + " must be assigned to a variable");
        }
        return null;
    }

    @Override
    public Type visitEmptyStat(FavaParser.EmptyStatContext ctx) {
        return null;
    }

    @Override
    public Type visitCallExpr(FavaParser.CallExprContext ctx) {
        Type t = visit(ctx.funcCall());

        if (t == Type.VOID) {
            addError(ctx, "function " + ctx.funcCall().ID().getText() + " does not return a value");
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        setType(ctx, t);
        return t;
    }

    @Override
    public Type visitFuncCall(FavaParser.FuncCallContext ctx) {
        String rawName = ctx.ID().getText();
        FuncSymbol fn = lookupFunction(rawName);

        if (fn == null) {
            addError(ctx, rawName + " not declared");
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        List<FavaParser.ExprContext> actuals = new ArrayList<>();
        if (ctx.argList() != null) {
            actuals.addAll(ctx.argList().expr());
        }

        if (actuals.size() != fn.paramTypes.size()) {
            addError(ctx, "function " + rawName + " expects " + fn.paramTypes.size() + " arguments");
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        for (int i = 0; i < actuals.size(); i++) {
            Type actualType = visit(actuals.get(i));
            Type expectedType = fn.paramTypes.get(i);

            if (actualType != Type.ERROR && !canAssign(expectedType, actualType)) {
                addError(actuals.get(i),
                        "expecting an expression of type " + typeName(expectedType) +
                                " for argument of function " + rawName);
            }
        }

        setType(ctx, fn.returnType);
        return fn.returnType;
    }

    @Override
    public Type visitIdExpr(FavaParser.IdExprContext ctx) {
        String rawName = ctx.ID().getText();
        VarSymbol var = lookupVariable(rawName);

        if (var == null) {
            if (lookupFunction(rawName) != null) {
                addError(ctx, rawName + " is not a variable");
            } else {
                addError(ctx, rawName + " not declared");
            }
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        setType(ctx, var.type);
        return var.type;
    }

    @Override
    public Type visitLiteralExpr(FavaParser.LiteralExprContext ctx) {
        Type t = visit(ctx.literal());
        setType(ctx, t);
        return t;
    }

    @Override
    public Type visitParenExpr(FavaParser.ParenExprContext ctx) {
        Type t = visit(ctx.expr());
        setType(ctx, t);
        return t;
    }

    @Override
    public Type visitIntLiteral(FavaParser.IntLiteralContext ctx) {
        setType(ctx, Type.INT);
        return Type.INT;
    }

    @Override
    public Type visitRealLiteral(FavaParser.RealLiteralContext ctx) {
        setType(ctx, Type.REAL);
        return Type.REAL;
    }

    @Override
    public Type visitStringLiteral(FavaParser.StringLiteralContext ctx) {
        setType(ctx, Type.STRING);
        return Type.STRING;
    }

    @Override
    public Type visitTrueLiteral(FavaParser.TrueLiteralContext ctx) {
        setType(ctx, Type.BOOL);
        return Type.BOOL;
    }

    @Override
    public Type visitFalseLiteral(FavaParser.FalseLiteralContext ctx) {
        setType(ctx, Type.BOOL);
        return Type.BOOL;
    }

    @Override
    public Type visitUnaryExpr(FavaParser.UnaryExprContext ctx) {
        Type t = visit(ctx.expr());

        if (t == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        String op = ctx.op.getText().toLowerCase();

        if (op.equals("-")) {
            if (t == Type.INT || t == Type.REAL) {
                setType(ctx, t);
                return t;
            }
            addError(ctx, "operator - is invalid for type " + typeName(t));
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (op.equals("not")) {
            if (t == Type.BOOL) {
                setType(ctx, Type.BOOL);
                return Type.BOOL;
            }
            addError(ctx, "operator not is invalid for type " + typeName(t));
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitMultDivModExpr(FavaParser.MultDivModExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        String op = ctx.op.getText().toLowerCase();

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (op.equals("mod")) {
            if (t1 == Type.INT && t2 == Type.INT) {
                setType(ctx, Type.INT);
                return Type.INT;
            }
            addError(ctx, "operator mod is invalid between " + typeName(t1) + " and " + typeName(t2));
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (isNumeric(t1) && isNumeric(t2)) {
            Type result = (t1 == Type.REAL || t2 == Type.REAL) ? Type.REAL : Type.INT;
            setType(ctx, result);
            return result;
        }

        addError(ctx, "operator " + ctx.op.getText() + " is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitAddSubExpr(FavaParser.AddSubExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (isNumeric(t1) && isNumeric(t2)) {
            Type result = (t1 == Type.REAL || t2 == Type.REAL) ? Type.REAL : Type.INT;
            setType(ctx, result);
            return result;
        }

        addError(ctx, "operator " + ctx.op.getText() + " is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitConcatExpr(FavaParser.ConcatExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        boolean ok =
                (t1 == Type.BOOL && t2 == Type.STRING) ||
                        (t1 == Type.STRING && t2 == Type.BOOL) ||
                        (t1 == Type.INT && t2 == Type.STRING) ||
                        (t1 == Type.STRING && t2 == Type.INT) ||
                        (t1 == Type.REAL && t2 == Type.STRING) ||
                        (t1 == Type.STRING && t2 == Type.REAL) ||
                        (t1 == Type.STRING && t2 == Type.STRING);

        if (ok) {
            setType(ctx, Type.STRING);
            return Type.STRING;
        }

        addError(ctx, "operator || is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitRelExpr(FavaParser.RelExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (isNumeric(t1) && isNumeric(t2)) {
            setType(ctx, Type.BOOL);
            return Type.BOOL;
        }

        addError(ctx, "operator " + ctx.op.getText() + " is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitEqExpr(FavaParser.EqExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        boolean ok =
                (t1 == Type.BOOL && t2 == Type.BOOL) ||
                        (t1 == Type.INT && t2 == Type.INT) ||
                        (t1 == Type.INT && t2 == Type.REAL) ||
                        (t1 == Type.REAL && t2 == Type.INT) ||
                        (t1 == Type.REAL && t2 == Type.REAL) ||
                        (t1 == Type.STRING && t2 == Type.STRING);

        if (ok) {
            setType(ctx, Type.BOOL);
            return Type.BOOL;
        }

        addError(ctx, "operator " + ctx.op.getText() + " is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitAndExpr(FavaParser.AndExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (t1 == Type.BOOL && t2 == Type.BOOL) {
            setType(ctx, Type.BOOL);
            return Type.BOOL;
        }

        addError(ctx, "operator and is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    @Override
    public Type visitOrExpr(FavaParser.OrExprContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));

        if (t1 == Type.ERROR || t2 == Type.ERROR) {
            setType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        if (t1 == Type.BOOL && t2 == Type.BOOL) {
            setType(ctx, Type.BOOL);
            return Type.BOOL;
        }

        addError(ctx, "operator or is invalid between " + typeName(t1) + " and " + typeName(t2));
        setType(ctx, Type.ERROR);
        return Type.ERROR;
    }

    private static class SemanticError {
        final int line;
        final String msg;

        SemanticError(int line, String msg) {
            this.line = line;
            this.msg = msg;
        }
    }

    private static class Scope {
        final Scope parent;
        final Map<String, VarSymbol> symbols = new LinkedHashMap<>();

        Scope(Scope parent) {
            this.parent = parent;
        }

        VarSymbol lookupVar(String name) {
            Scope s = this;
            while (s != null) {
                VarSymbol sym = s.symbols.get(name);
                if (sym != null) return sym;
                s = s.parent;
            }
            return null;
        }
    }

    private static class VarSymbol {
        final String name;
        final Type type;
        final VarKind kind;

        VarSymbol(String name, Type type, VarKind kind) {
            this.name = name;
            this.type = type;
            this.kind = kind;
        }
    }

    private static class FuncSymbol {
        final String name;
        final String rawName;
        final Type returnType;
        final List<Type> paramTypes;
        final List<String> paramNames;

        FuncSymbol(String name, String rawName, Type returnType, List<Type> paramTypes, List<String> paramNames) {
            this.name = name;
            this.rawName = rawName;
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