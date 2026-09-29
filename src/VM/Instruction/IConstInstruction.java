package VM.Instruction;


import VM.OpCode;

import java.io.DataOutputStream;
import java.io.IOException;

public class IConstInstruction extends Instruction {
    private final int value;

    public IConstInstruction(int value) {
        super(OpCode.iconst);
        this.value = value;
    }

    @Override
    public int nArgs() { return 1; }

    @Override
    public String toString() {
        return opc + " " + value;
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeInt(value);
    }
}