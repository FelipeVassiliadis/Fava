package VM.Instruction;

import VM.OpCode;

import java.io.DataOutputStream;
import java.io.IOException;

public class SConstInstruction extends Instruction {
    private final String value;

    public SConstInstruction(String value) {
        super(OpCode.sconst);
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
        out.writeUTF(value);
    }
}