package eu.telecomnancy.pcl.assembly.register;

import eu.telecomnancy.pcl.assembly.AsmCompilerVisitor;
import eu.telecomnancy.pcl.assembly.AsmWriter;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.*;

import static eu.telecomnancy.pcl.Main.*;

public class RegisterAllocator {
    private static final boolean debugRegisters = false || debugAll;
    // temporary registers for everyday use
    private final Set<String> availableRegisters = new TreeSet<>(Arrays.asList( "R10", "R11", "R12", "R13"));
    private final List<String> availableRegistersCopyFixed = new ArrayList<>(Arrays.asList("R10", "R11", "R12", "R13"));
    // optimized registers for specific use
    public static String[] FIXED_REGISTERS = {"R3", "R4", "R5","R6", "R7", "R8", "R9"};
    // function return register
    public static final String RET = "R0";
    public static final String RETF = "R1"; /* for real decimal part */
    // register to fetch in stack
    public static final String TEMPORARY_REGISTER = "R2";
    public static final String STACK_POINTER = "SP";
    public static final String BASE_POINTER = "BP";
    // when no fixed register has been attributed (use stack)
    public static final String NO_ATTRIBUTED_REGISTER = "NO_ATTRIBUTED_REGISTER";
    // when all temporary register have been exhausted (use stack)
    public static final String NO_POSSIBLE_REGISTER = "NO_POSSIBLE_REGISTER";

    // actions
    // No fixed register has been allocated, we use exclusively the stack.
    public static final String USE_STACK_ONLY = "STACK_ONLY";
    // The register has changed or need to be instantiated from the stack
    public static final String GET_REGISTER_FROM_STACK = "NEW_REGISTER";
    // The register will change and need to be stored in the stack to not be lost
    // We need 2 states here due to a possible switch between registers approaching the limit of the coloring
    // this will not happen a lot but its necessary to prevent it
    public static final String STORE_REGISTER_IN_STACK_GET_FROM_REGISTRY = "CLEAN_TO_DIRTY_REGISTER";
    public static final String STORE_REGISTER_IN_STACK_GET_FROM_STACK = "DIRTY_TO_DIRTY_REGISTER";
    // the register is the same as the one used before, its clean thus can be used directly
    public static final String GET_REGISTER_FROM_ITSELF = "CLEAN_REGISTER";

    public static final ArrayList<Record> IN_USE_REGISTERS = new ArrayList<>();
    private final SymbolTable tds;
    private final GraphColoringRegister graphColoringRegister;
    private final ArrayList<Integer> countRegisterInStack = new ArrayList<>() {{
        add(0);
    }};
    private int countRegisterIndex = 0;
    private boolean useTemporaryReg = true;
    private AsmWriter writer;
    private AsmCompilerVisitor compilerVisitor;
    public RegisterAllocator(ASTRoot root, SymbolTable TDS, boolean turnOn) {
        if (debugRegisters){
            System.out.println("Fixed registers: "+ Arrays.toString(FIXED_REGISTERS));
            System.out.println("available registers: "+availableRegisters.toString());
        }
        if (!turnOn) {
            availableRegisters.addAll(Arrays.asList(FIXED_REGISTERS));
            availableRegistersCopyFixed.addAll(Arrays.asList(FIXED_REGISTERS));
            FIXED_REGISTERS = new String[0];
        }
        if (debugRegisters){
            System.out.println("Fixed registers: "+ Arrays.toString(FIXED_REGISTERS));
            System.out.println("available registers: "+availableRegisters.toString());
        }
        // this has side effects by adding to the record the actions and registers to use at each step
        graphColoringRegister = new GraphColoringRegister(root, TDS);
        this.tds = TDS;
        this.tds.resetTable();
    }

    public void setWriter(AsmWriter writer) {
        this.writer = writer;
    }

    public void setCompilerVisitor(AsmCompilerVisitor compilerVisitor) {
        this.compilerVisitor = compilerVisitor;
    }

    public List<String> getAvailableRegistersCopyFixed() {
        return availableRegistersCopyFixed;
    }

    public GraphColoringRegister getGraphColoringRegister() {
        return graphColoringRegister;
    }

    private void debug(String message) {
        if (debugRegisters)
            writer.comment(message);
    }

    public String getNextAvailableRegister() {

        if (!availableRegisters.isEmpty()) {
            String reg = availableRegisters.iterator().next();
            availableRegisters.remove(reg);
            countRegisterInStack.set(countRegisterIndex, countRegisterInStack.get(countRegisterIndex) + 1);
            DEBUG("Register requested",reg);
            return reg;
        }
        // there is a register exhaustion, saving current register frame to the stack
        debug("TEMPORARY REGISTERS WERE EXHAUSTED, NEED TO USE THE STACK");

        for (String availableRegister : registerAllocator.getAvailableRegistersCopyFixed()) {
            writer.stw(availableRegister, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(availableRegister);
        }
        countRegisterIndex++;
        countRegisterInStack.add(0);

        return getNextAvailableRegister();
    }

    public String getRegister(Record record) {
        IN_USE_REGISTERS.add(record);
        return record.getCurrentRegister();
    }

    public String getAction(Record record) {
        return record.getCurrentAction();
    }

    public String getRegister(String identifier) {
        Record record = this.tds.lookup(identifier);
        IN_USE_REGISTERS.add(record);
        return record.getCurrentRegister();
    }

    public String getAction(String identifier) {
        Record record = this.tds.lookup(identifier);
        return record.getCurrentAction();
    }


    public void releaseRegister(Record record, String register) {
        if (IN_USE_REGISTERS.contains(record)) {
            IN_USE_REGISTERS.remove(record);
        } else {
            availableRegisters.add(register);
        }
    }


    public void releaseRegister(String register) {
        if (register.equals(RET) || register.equals(BASE_POINTER) || register.equals(STACK_POINTER)) {
            logger.logCritical("[releaseRegister] "+ register+" can not be released");
            return;
        }
        if (register.equals(TEMPORARY_REGISTER)){
            debug("TEMP REGISTER RELEASED");
            if (countRegisterIndex!=0 || countRegisterInStack.get(countRegisterIndex)!=0){
                debug("WRONG OFFSET, NEED CORRECTION");
            }
            return;
        }
        if (countRegisterIndex==0){
            // dirty fix for floating point register spamming
            if (!availableRegisters.contains(register)) {
                countRegisterInStack.set(countRegisterIndex, countRegisterInStack.get(countRegisterIndex) - 1);
            }else{
                debug("You are freeing an already freed register "+register);
            }
        }
        availableRegisters.add(register);
        DEBUG("Register released",register);
    }

    /*
    XXX -> The patterns to prevent memory exhaustion are as follow
        For left-Right:
            public AsmCompilerVisitor visit(ASTNode node) {
                String regA;

                node.getNodeLeft().accept(this);
                regA = reg;

                node.getNodeRight().accept(this);
                registerAllocator.preventRegisterExhaustion(); // always before anything using reg
                ...
                writer.op(regA, reg, reg);
                registerAllocator.releaseRegister(regA);

                // result of op is in reg
                return this;
             }
        For expression-children:
            public AsmCompilerVisitor visit(ASTNode node) {
                node.getExpression().accept(this);
                for (ASTNode nodeChild : node.getDesignators())
                    nodeChild.accept(this);
                    registerAllocator.preventRegisterExhaustion();

                return this;
            }

         If there is a temporary register requested even with fixed number calls prefer to use those:
             getNextAvailableRegister() -> getTemporaryRegister()
             ...                        -> ...
             releaseAvailableRegister() -> releaseTemporaryRegister()



     */

    private void DEBUG(String type,String register){
        debug (String.format("%s : %s, availables are: %s, counter is: %s, in stack is: %s, available length: %s, sum is: %s",type,register,availableRegisters.toString(),countRegisterIndex,countRegisterInStack,availableRegisters.size(),countRegisterInStack.get(countRegisterIndex)+availableRegisters.size()));
    }
    // This has the side effect to potentially change the reg to the return register
    public void preventRegisterExhaustion(){
        // there is no register exhaustion
        if (countRegisterIndex==0){
            return;
        }
        // the prelease help in case of register exhaustion, it will use the temporary register as the result register
        // and will release step by step the saved frame of each registers
        if (useTemporaryReg){
            if (countRegisterInStack.get(countRegisterIndex)-1>=registerAllocator.getAvailableRegistersCopyFixed().size()){
                debug("HUGE INDEX ERROR, YOU ARE MISSING CALLS");
                return;
            }
            writer.ldw(RegisterAllocator.TEMPORARY_REGISTER,registerAllocator.getAvailableRegistersCopyFixed().get(countRegisterInStack.get(countRegisterIndex)-1));
            useTemporaryReg =false;
        }
        compilerVisitor.setReg(RegisterAllocator.TEMPORARY_REGISTER);
        if (countRegisterIndex > 0 && countRegisterInStack.get(countRegisterIndex)==1) {
            for (int i = registerAllocator.getAvailableRegistersCopyFixed().size()-1; i >-1; i--) {
                writer.ldw(registerAllocator.getAvailableRegistersCopyFixed().get(i), "(" + RegisterAllocator.STACK_POINTER + ")+");
                availableRegisters.remove(registerAllocator.getAvailableRegistersCopyFixed().get(i));
                DEBUG("Register Exhaustion release",registerAllocator.getAvailableRegistersCopyFixed().get(i));
            }
            countRegisterInStack.remove(countRegisterIndex);
            countRegisterIndex--;
        } else {
            countRegisterInStack.set(countRegisterIndex, countRegisterInStack.get(countRegisterIndex) - 1);
        }
       DEBUG("global boolean",useTemporaryReg?"true":"false");
        if (countRegisterIndex==0){
            useTemporaryReg =true;
        }
    }

    public String getTemporaryRegister(){
        if (!useTemporaryReg) {
            writer.stw(TEMPORARY_REGISTER, "-(" + STACK_POINTER + ")");
        }
        DEBUG("Temporary register requested",TEMPORARY_REGISTER);
        return TEMPORARY_REGISTER;
    }

    public void releaseTemporaryRegister(String register){
        if (!useTemporaryReg){
            writer.ldw(TEMPORARY_REGISTER,"("+STACK_POINTER+")+");
        }
        DEBUG("Temporary register released",TEMPORARY_REGISTER);
        if (!register.equals(TEMPORARY_REGISTER)){
            debug("WRONG REGISTER RELEASED");
            releaseRegister(register);
        }
    }
}
