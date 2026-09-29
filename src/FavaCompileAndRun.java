import Fava.FavaLexer;
import Fava.FavaParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import CodeGenerator.*;
import VM.vm;

public class FavaCompileAndRun {

    public static void main(String[] args) throws Exception {

        boolean showLexerErrors = false;
        boolean showParserErrors = false;

        CharStream cs;
        if (args.length > 0) {
            cs = CharStreams.fromFileName(args[0]);
        } else {
            cs = CharStreams.fromStream(System.in);
        }

        MyErrorListener errorListener = new MyErrorListener(showLexerErrors, showParserErrors);

        FavaLexer lexer = new FavaLexer(cs);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        FavaParser parser = new FavaParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ParseTree tree = parser.prog();

        if (errorListener.getNumLexerErrors() > 0) {
            System.out.println("Input has lexical errors");
            return;
        }

        if (errorListener.getNumParsingErrors() > 0) {
            System.out.println("Input has parsing errors");
            return;
        }

        TypeCheck.hasError = false;
        TypeCheck typeCheck = new TypeCheck();
        typeCheck.visit(tree);

        if (TypeCheck.hasError) {
            return;
        }

        CodeGen codeGen = new CodeGen(typeCheck);
        codeGen.visit(tree);

        codeGen.dumpConstantPool();
        codeGen.dumpInstructions();
        System.out.println("*** VM output ***");

        codeGen.saveBytecodes("bytecodes.bc");

        byte[] bytecodes = codeGen.getBytecodes();
        vm machine = new vm(bytecodes, false);
        machine.run();
    }
}