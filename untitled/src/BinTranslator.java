import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BinTranslator {
    private final String binFile;


    private int pc;


    public BinTranslator(String binFile) {
        this.binFile = binFile;
    }


    /*
    - Register R9 is B the RAM address register
    - Register R10 is the Accumulator in the AVR for me
    - Register R11 is the tmp Register
    - Register R12 is for the Parameter in AVR
    - Register R13 is the SIO Register

     */
    public void start() {
        Path binFilePath = Paths.get(binFile);
        byte[] buffer;
        try {
            buffer = Files.readAllBytes(binFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("\n;The Translation from the Binary COP421 to the AVR ASM:");

        translate(buffer);


    }

    private void translate(byte[] buffer) {

        for (pc = 0; pc < buffer.length; ) {
            byte opcode1 = buffer[pc];
            byte opcode2 = buffer[pc + 1];
            byte secondOpcode = (byte) (opcode2 >> 4);
            byte param = (byte) (opcode2 & 0xf);
            String pcInHex = String.format("%03X", pc);
            System.out.print(pcInHex+": ");
            switch (opcode1) {
                case 0x00: // CLRA
                    System.out.println("CLR R10 ; clear the Accumulator");
                    pc++;
                    break;
                case 0x10: // CASC
                    System.out.println("ADC R10, R11");
                    emulateFourBitAccumulator();
                    pc++;
                    break;
                case 0x20: //SKC
                    System.out.println("BRCS "+String.format("%03X", pc+2)+" ; Branch if Carry Set");
                    pc++;
                    break;
                case 0x21: //SKE
                    System.out.println("LD 16, R11 ; Load the Mem to the tmp Register");
                    System.out.println("CP R10, R11 ; Compare the accumulator and value");
                    System.out.println("BREQ "+String.format("%X",pc+2) +" ; Branch if equal");
                    pc++;
                    break;
                case 0x22: //SC
                    System.out.println("SEC");
                    pc++;
                    break;
                case 0x30: // ASC
                    System.out.println("LD R11, X ; Load the content of memory at address specified by B register into R0");
                    System.out.println("ADD R10, R11  ; Add the content of R0 to the accumulator (R10)");
                    System.out.println("BRCC "+(pc+2)+"  ; Branch if no carry to skip the next instruction");
                    System.out.println("SBI C 3 ; Set the carry flag according to the carry from bit three");
                    emulateFourBitAccumulator();
                    pc++;
                    break;
                case 0x31: // ADD
                    System.out.println("LD R11");
                    System.out.println("ADD R10, R11");
                    emulateFourBitAccumulator();
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
                case 0x40: //COMP
                    System.out.println("COM R10");
                    System.out.println("ANDI R10, 0x0F"); // To set the highest four bits to zero
                    pc++;
                    break;
                case 0x44:
                    System.out.println("NOP");
                    pc++;
                    break;
                case 0x4A: //ADT
                    System.out.println("ADIW R10, 10");
                    System.out.println("DAAX ; Decimal adjust after addition, adjusts R10 for BCD (if needed)");
                    pc++;
                    break;

                case 0x4F: //XAS
                    System.out.println("XAS noch Fehlerhaft!");

                    //System.out.println("MOV R11, R10 ; Copy accumulator to tmp");
                    //System.out.println("MOV R10, R13 ; Copy SIO register to accumulator");
                    //System.out.println("MOV R13 R11 ; Copy accumulator value in SIO register");

                    pc++;
                    break;
                default:

                    if(checkFirstFourBits(opcode1,opcode2)||checkFirstAndSecondBit(opcode1)){
                        break;
                    }
                    System.err.println(String.format("%02X",opcode1) + " is not known!");
                    throw new IllegalStateException("Error happend");
            }


        }
    }

    private void emulateFourBitAccumulator(){
        System.out.println("SBRC R10, 4"); // skip if bit 4 in register R10 is not set
        System.out.println("SBI C, 1"); // set carry bit
        System.out.println("SBRC SREG, 0"); // skip if carry bit is not set
        System.out.println("BCLR R10, 4"); // clear bit 4 in r10
    }

    private boolean checkFirstFourBits(byte opcode1WithParam, byte param){
        byte opcode1 =  (byte) (opcode1WithParam >> 4);
        byte firstParam = (byte) (opcode1WithParam & 0xf);
        switch (opcode1){
            case 0x5:  //AISC
                System.out.println("ADIW R10, "+ firstParam); // add param to R10
                System.out.println("SBRS R10, 4"); // skip if bit 4 is set
                System.out.println("CALL "+pc+1);
                System.out.println("BCLR R10, 4");
                System.out.println("CALL "+pc+2);
                pc++;
                return true;
            case 0x6: //JSR
                byte threeBitParam = (byte)(firstParam &0x7);
                var address1 = String.format("%01X", threeBitParam);
                var address2 = String.format("%02X", param);
                System.out.println("CALL "+address1+address2 +" ; Go to the Label");
                pc+=2;
                return true;
            default:
                return false;
        }

    }

    private boolean checkFirstAndSecondBit(byte opcode1WithParam){
        boolean isFirstBitSet =  ((byte) 0b10000000 & opcode1WithParam) != 0;
        boolean isSecondBitSet =  ((byte) 0b01000000 & opcode1WithParam) != 0;
        byte param = (byte) ((byte) 0b00111111 & opcode1WithParam);
        if(isFirstBitSet){ //JP
            pc++;
            if(isSecondBitSet){
                System.out.println("RJMP "+String.format("%03X", param));
                return true;
            }
            param = (byte) (param | (byte) 0b01000000);

            System.out.println("RJMP "+String.format("%03X", param));
            return true;
        }

        return false;
    }
}
