package VM;

import VM.Instruction.*;

import java.util.*;
import java.io.*;

public class vm {
    private final boolean trace;
    private final byte[] bytecodes;
    private Instruction[] code;
    private int IP;
    private int FP; // frame pointer

    private final Stack<Object> stack = new Stack<>();
    private Object[] constantPool;
    private Object[] globals;

    public vm(byte[] bytecodes, boolean trace) {
        this.trace = trace;
        this.bytecodes = bytecodes;
        decode(bytecodes);
        this.IP = 0;
        this.FP = -1;
    }

    private void decode(byte[] bytecodes) {
        ArrayList<Instruction> inst = new ArrayList<>();

        try {
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(bytecodes));

            int poolSize = din.readInt();
            constantPool = new Object[poolSize];

            for (int i = 0; i < poolSize; i++) {
                byte type = din.readByte();

                switch (type) {
                    case 0:
                        constantPool[i] = din.readInt();
                        break;
                    case 1:
                        constantPool[i] = din.readDouble();
                        break;
                    case 2:
                        constantPool[i] = din.readUTF();
                        break;
                    default:
                        runtime_error("Unknown constant type: " + type);
                }
            }

            while (true) {
                byte b = din.readByte();
                OpCode opc = OpCode.convert(b);

                switch (opc.nArgs()) {
                    case 0:
                        inst.add(new Instruction(opc));
                        break;
                    case 1:
                        int val = din.readInt();
                        inst.add(new Instruction1Arg(opc, val));
                        break;
                    default:
                        runtime_error("Invalid number of arguments in decode()");
                }
            }

        } catch (EOFException e) {
            this.code = new Instruction[inst.size()];
            inst.toArray(this.code);

            if (trace) {
                System.out.println("Disassembled instructions");
                dumpInstructionsAndBytecodes();
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void dumpInstructionsAndBytecodes() {
        int idx = 0;
        idx += 4;

        try {
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(bytecodes));
            int poolSize = din.readInt();

            for (int i = 0; i < poolSize; i++) {
                byte type = din.readByte();
                idx += 1;

                switch (type) {
                    case 0:
                        din.readInt();
                        idx += 4;
                        break;
                    case 1:
                        din.readDouble();
                        idx += 8;
                        break;
                    case 2:
                        String s = din.readUTF();
                        idx += 2 + s.getBytes("UTF-8").length;
                        break;
                    default:
                        runtime_error("Unknown constant type while dumping");
                }
            }
        } catch (IOException e) {
            runtime_error("Error while dumping bytecodes");
        }

        for (int i = 0; i < code.length; i++) {
            StringBuilder s = new StringBuilder();
            s.append(String.format("%02X ", bytecodes[idx++]));
            if (code[i].nArgs() == 1) {
                for (int k = 0; k < 4; k++) {
                    s.append(String.format("%02X ", bytecodes[idx++]));
                }
            }
            System.out.println(String.format("%5s: %-15s // %s", i, code[i], s));
        }
    }

    public void dumpInstructions() {
        for (int i = 0; i < code.length; i++) {
            System.out.println(i + ": " + code[i]);
        }
    }

    private void runtime_error(String msg) {
        System.out.println("runtime error: " + msg);
        if (trace) {
            System.out.println(String.format("%22s Stack: %s", "", stack));
        }
        System.exit(0);
    }

    private Object popNonNull() {
        Object v = stack.pop();
        if (v == null) {
            runtime_error("accessing a NULL value");
        }
        return v;
    }

    private Object stackAt(int idx) {
        if (idx < 0 || idx >= stack.size()) {
            runtime_error("invalid stack access");
        }
        return stack.get(idx);
    }

    private void setStackAt(int idx, Object value) {
        if (idx < 0 || idx >= stack.size()) {
            runtime_error("invalid stack access");
        }
        stack.set(idx, value);
    }

    private void exec_iconst(Integer v) {
        stack.push(v);
    }

    private void exec_galloc(int n) {
        if (globals == null) {
            globals = new Object[n];
        } else {
            Object[] newGlobals = new Object[globals.length + n];
            System.arraycopy(globals, 0, newGlobals, 0, globals.length);
            globals = newGlobals;
        }
    }

    private void exec_gload(int idx) {
        if (globals == null || idx < 0 || idx >= globals.length) {
            runtime_error("invalid global memory access");
        }
        Object v = globals[idx];
        if (v == null) {
            runtime_error("accessing a NULL value");
        }
        stack.push(v);
    }

    private void exec_gstore(int idx) {
        if (globals == null || idx < 0 || idx >= globals.length) {
            runtime_error("invalid global memory access");
        }
        globals[idx] = stack.pop();
    }

    private void exec_lalloc(int n) {
        for (int i = 0; i < n; i++) {
            stack.push(null);
        }
    }

    private void exec_lload(int addr) {
        int idx = FP + addr;
        Object v = stackAt(idx);
        if (v == null) {
            runtime_error("accessing a NULL value");
        }
        stack.push(v);
    }

    private void exec_lstore(int addr) {
        int idx = FP + addr;
        Object v = stack.pop();
        setStackAt(idx, v);
    }

    private void exec_pop(int n) {
        for (int i = 0; i < n; i++) {
            if (stack.isEmpty()) {
                runtime_error("pop from empty stack");
            }
            stack.pop();
        }
    }

    private void exec_jump(int addr) {
        IP = addr - 1;
    }

    private void exec_jumpf(int addr) {
        Object v = popNonNull();
        boolean cond = (Boolean) v;
        if (!cond) {
            IP = addr - 1;
        }
    }

    private void exec_call(int addr) {
        int oldFP = FP;
        int newFP = stack.size();

        stack.push(oldFP);     // Stack[FP+paramCount]
        stack.push(IP + 1);    // Stack[FP+paramCount+1] = return address

        FP = newFP;
        IP = addr - 1;
    }

    private void exec_ret(int nArgs) {
        int retAddr = (Integer) stackAt(FP + 1);
        int oldFP = (Integer) stackAt(FP);

        while (stack.size() > FP) {
            stack.pop();
        }

        for (int i = 0; i < nArgs; i++) {
            if (stack.isEmpty()) {
                runtime_error("invalid return");
            }
            stack.pop();
        }

        FP = oldFP;
        IP = retAddr - 1;
    }

    private void exec_retval(int nArgs) {
        Object x = stack.pop();

        int retAddr = (Integer) stackAt(FP + 1);
        int oldFP = (Integer) stackAt(FP);

        while (stack.size() > FP) {
            stack.pop();
        }

        for (int i = 0; i < nArgs; i++) {
            if (stack.isEmpty()) {
                runtime_error("invalid return");
            }
            stack.pop();
        }

        FP = oldFP;
        stack.push(x);
        IP = retAddr - 1;
    }

    private void exec_iuminus() {
        int v = (Integer) popNonNull();
        stack.push(-v);
    }

    private void exec_iadd() {
        int right = (Integer) popNonNull();
        int left = (Integer) popNonNull();
        stack.push(left + right);
    }

    private void exec_isub() {
        int right = (Integer) popNonNull();
        int left = (Integer) popNonNull();
        stack.push(left - right);
    }

    private void exec_imult() {
        int right = (Integer) popNonNull();
        int left = (Integer) popNonNull();
        stack.push(left * right);
    }

    private void exec_idiv() {
        int right = (Integer) popNonNull();
        int left = (Integer) popNonNull();
        if (right != 0) {
            stack.push(left / right);
        } else {
            runtime_error("division by 0");
        }
    }

    private void exec_iprint() {
        int v = (Integer) popNonNull();
        System.out.println(v);
    }

    private void exec_not() {
        boolean v = (Boolean) popNonNull();
        stack.push(!v);
    }

    private void exec_imod() {
        int right = (Integer) popNonNull();
        int left = (Integer) popNonNull();
        stack.push(left % right);
    }

    private void exec_ieq() {
        int r = (Integer) popNonNull();
        int l = (Integer) popNonNull();
        stack.push(l == r);
    }

    private void exec_ineq() {
        int r = (Integer) popNonNull();
        int l = (Integer) popNonNull();
        stack.push(l != r);
    }

    private void exec_ilt() {
        int r = (Integer) popNonNull();
        int l = (Integer) popNonNull();
        stack.push(l < r);
    }

    private void exec_ileq() {
        int r = (Integer) popNonNull();
        int l = (Integer) popNonNull();
        stack.push(l <= r);
    }

    private void exec_itod() {
        int v = (Integer) popNonNull();
        stack.push((double) v);
    }

    private void exec_itos() {
        int v = (Integer) popNonNull();
        stack.push(Integer.toString(v));
    }

    private void exec_bprint() {
        boolean v = (Boolean) popNonNull();
        System.out.println(v ? "true" : "false");
    }

    private void exec_dtos() {
        double v = (Double) popNonNull();
        stack.push(Double.toString(v));
    }

    private void exec_dadd() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l + r);
    }

    private void exec_dsub() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l - r);
    }

    private void exec_dmult() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l * r);
    }

    private void exec_ddiv() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l / r);
    }

    private void exec_deq() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l == r);
    }

    private void exec_dneq() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l != r);
    }

    private void exec_dleq() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l <= r);
    }

    private void exec_dlt() {
        double r = (Double) popNonNull();
        double l = (Double) popNonNull();
        stack.push(l < r);
    }

    private void exec_dprint() {
        double v = (Double) popNonNull();
        System.out.println(v);
    }

    private void exec_tconst() {
        stack.push(true);
    }

    private void exec_fconst() {
        stack.push(false);
    }

    private void exec_and() {
        boolean right = (Boolean) popNonNull();
        boolean left = (Boolean) popNonNull();
        stack.push(left && right);
    }

    private void exec_or() {
        boolean right = (Boolean) popNonNull();
        boolean left = (Boolean) popNonNull();
        stack.push(left || right);
    }

    private void exec_sconcat() {
        String right = (String) popNonNull();
        String left = (String) popNonNull();
        stack.push(left + right);
    }

    private void exec_seq() {
        String r = (String) popNonNull();
        String l = (String) popNonNull();
        stack.push(l.equals(r));
    }

    private void exec_sprint() {
        String v = (String) popNonNull();
        System.out.println(v);
    }

    private void exec_btos() {
        boolean v = (Boolean) popNonNull();
        stack.push(v ? "true" : "false");
    }

    private void exec_sneq() {
        String r = (String) popNonNull();
        String l = (String) popNonNull();
        stack.push(!l.equals(r));
    }

    private void exec_beq() {
        boolean r = (Boolean) popNonNull();
        boolean l = (Boolean) popNonNull();
        stack.push(l == r);
    }

    private void exec_bneq() {
        boolean r = (Boolean) popNonNull();
        boolean l = (Boolean) popNonNull();
        stack.push(l != r);
    }

    private void exec_duminus() {
        double v = (Double) popNonNull();
        stack.push(-v);
    }

    private void exec_inst(Instruction inst) {
        if (trace) {
            System.out.println(String.format("%5s: %-15s FP=%-5s Stack: %s", IP, inst, FP, stack));
        }

        OpCode opc = inst.getOpCode();
        int arg;

        switch (opc) {
            case iconst:
                arg = ((Instruction1Arg) inst).getArg();
                exec_iconst(arg);
                break;

            case dconst:
                arg = ((Instruction1Arg) inst).getArg();
                stack.push((Double) constantPool[arg]);
                break;

            case sconst:
                arg = ((Instruction1Arg) inst).getArg();
                stack.push((String) constantPool[arg]);
                break;

            case galloc:
                arg = ((Instruction1Arg) inst).getArg();
                exec_galloc(arg);
                break;

            case gload:
                arg = ((Instruction1Arg) inst).getArg();
                exec_gload(arg);
                break;

            case gstore:
                arg = ((Instruction1Arg) inst).getArg();
                exec_gstore(arg);
                break;

            case lalloc:
                arg = ((Instruction1Arg) inst).getArg();
                exec_lalloc(arg);
                break;

            case lload:
                arg = ((Instruction1Arg) inst).getArg();
                exec_lload(arg);
                break;

            case lstore:
                arg = ((Instruction1Arg) inst).getArg();
                exec_lstore(arg);
                break;

            case pop:
                arg = ((Instruction1Arg) inst).getArg();
                exec_pop(arg);
                break;

            case jump:
                arg = ((Instruction1Arg) inst).getArg();
                exec_jump(arg);
                break;

            case jumpf:
                arg = ((Instruction1Arg) inst).getArg();
                exec_jumpf(arg);
                break;

            case call:
                arg = ((Instruction1Arg) inst).getArg();
                exec_call(arg);
                break;

            case retval:
                arg = ((Instruction1Arg) inst).getArg();
                exec_retval(arg);
                break;

            case ret:
                arg = ((Instruction1Arg) inst).getArg();
                exec_ret(arg);
                break;

            case tconst:
                exec_tconst();
                break;

            case fconst:
                exec_fconst();
                break;

            case itod:
                exec_itod();
                break;

            case itos:
                exec_itos();
                break;

            case dtos:
                exec_dtos();
                break;

            case btos:
                exec_btos();
                break;

            case iuminus:
                exec_iuminus();
                break;

            case iadd:
                exec_iadd();
                break;

            case isub:
                exec_isub();
                break;

            case imult:
                exec_imult();
                break;

            case idiv:
                exec_idiv();
                break;

            case imod:
                exec_imod();
                break;

            case iprint:
                exec_iprint();
                break;

            case duminus:
                exec_duminus();
                break;

            case dadd:
                exec_dadd();
                break;

            case dsub:
                exec_dsub();
                break;

            case dmult:
                exec_dmult();
                break;

            case ddiv:
                exec_ddiv();
                break;

            case dprint:
                exec_dprint();
                break;

            case and:
                exec_and();
                break;

            case or:
                exec_or();
                break;

            case not:
                exec_not();
                break;

            case bprint:
                exec_bprint();
                break;

            case ieq:
                exec_ieq();
                break;

            case ineq:
                exec_ineq();
                break;

            case ilt:
                exec_ilt();
                break;

            case ileq:
                exec_ileq();
                break;

            case deq:
                exec_deq();
                break;

            case dneq:
                exec_dneq();
                break;

            case dlt:
                exec_dlt();
                break;

            case dleq:
                exec_dleq();
                break;

            case seq:
                exec_seq();
                break;

            case sneq:
                exec_sneq();
                break;

            case beq:
                exec_beq();
                break;

            case bneq:
                exec_bneq();
                break;

            case sconcat:
                exec_sconcat();
                break;

            case sprint:
                exec_sprint();
                break;

            case halt:
                return;

            default:
                System.out.println("Unknown opcode: " + opc);
                System.exit(0);
        }
    }

    public void run() {
        if (trace) {
            System.out.println("Trace while running the code");
            System.out.println("Execution starts at instruction " + IP);
        }

        while (IP < code.length) {
            OpCode opc = code[IP].getOpCode();
            exec_inst(code[IP]);

            if (opc == OpCode.halt) {
                break;
            }

            IP++;
        }

        if (trace) {
            System.out.println(String.format("%22s Stack: %s", "", stack));
        }
    }
}