package VM.Instruction;
import VM.OpCode;

import java.io.DataOutputStream;
import java.io.IOException;

public class DConstInstruction extends Instruction {
    private final double value;

    public DConstInstruction(double value) {
        super(OpCode.dconst);
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
        out.writeDouble(value);
    }
}