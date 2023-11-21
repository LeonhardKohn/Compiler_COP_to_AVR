import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BinTranslator {
    private final String binFile;
    private final String outputFile;





    public BinTranslator(String binFile, String outputFile) {
        this.binFile = binFile;
        this.outputFile = outputFile;
    }


    /*
    - Register R10 is the Accumulator in the AVR for me
    - Register R12 is for the Parameter in AVR
     */
    public void start() {
        Path binFilePath = Paths.get(binFile);
        byte[] buffer = new byte[0];
        try {
            buffer = Files.readAllBytes(binFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("\nThe Translation from the Binary COP421 to the AVR ASM:");

        translate(buffer);


    }

    private void translate(byte[] buffer) {
        int pc;
        for (pc = 0; pc < buffer.length; ) {
            byte opcode1 = buffer[pc];
            byte opcode2 = buffer[pc + 1];
            byte secondOpcode = (byte) (opcode2 >> 4);
            byte param = (byte) (opcode2 & 0xf);
            String pcInHex = String.format("%03x", pc);
            System.out.print(pcInHex+": ");
            switch (opcode1) {
                case 0x00: // CLRA
                    System.out.println("CLR R10 ; clear the Accumulator");
                    pc++;
                    break;
                case 0x32: //RC
                    System.out.println("CLC ; Clear Carry Flag");
                    pc++;
                    break;
                case 0x33: // 2-byte Command

                    switch (secondOpcode) {
                        case 0x5: // OGI
                            System.out.println("LDI R11 " + param);
                            System.out.println("OUT $18,R11 ; output the param on the Port B (0-3)");
                            pc += 2;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + opcode2);

                    }
                    break;
                case opcode1 > 0x6: // JSK
                    byte threeBitParam = (byte)(opcode1 &0xe);
                    System.out.println("CALL "+threeBitParam+secondOpcode+param); // JMP or CALL ???
                    pc+=2;
                    break;
                default:

                    System.err.println(String.format("%02x",opcode1) + " is not known!");
                    throw new IllegalStateException("Error happend");
            }


        }
    }

    private void secondChance(){

    }
}
