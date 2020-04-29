package eu.telecomnancy.pcl.assembly;

import static eu.telecomnancy.pcl.Main.*;
import static eu.telecomnancy.pcl.assembly.register.RegisterAllocator.*;

import eu.telecomnancy.pcl.assembly.register.RegisterAllocator;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.SymbolTable.Scope;

public class AsmVar {
    private final AsmWriter writer;

    public AsmVar(AsmWriter writer) {
        this.writer = writer;
    }

    public void getVar(String idf, String reg, SymbolTable tds, boolean keepVar) {
        Object[] res = tds.lookupCountGenre(idf, Record.Genre.VARIABLE);
        if (res == null) {
            res = tds.lookupCountGenre(idf, Record.Genre.ARRAY);
            if (res==null) {
                logger.logCritical("Impossible to get " + idf);
                return;
            }
        }
        Record record = (Record) res[0];

        if (record.getSize() != 2) {
            logger.logCritical("Wrong use of getVar for size != 2");
            return;
        }

        int hops = (int) res[1];
        int offset = record.getOffset();

        // Start of fixed register handling
        // warning getCurrentAction and getCurrentRegister have side effects, use with caution
        String action;
        if (!keepVar) {
            action = record.getCurrentAction();
        } else {
            action = record.getFirstAction();
        }
        if (debugCompiler){
            writer.comment("ACTION " + action + ", GET VAR " + record.getId());
        }
        if (action.equals(STORE_REGISTER_IN_STACK_GET_FROM_REGISTRY)) {
            String fixed_reg;
            if (!keepVar) {
                // if we want to clear the entry in the graph
                fixed_reg = record.getCurrentRegister();
            } else {
                // if we want to keep the entry for an unknown period before clearing it
                fixed_reg = record.getFirstRegister();
            }
            writer.ldw(reg, fixed_reg);
            // Store current value in stack because there has been a sudden switch in the fixed registers, this
            // is a rare event
            String regA = registerAllocator.getTemporaryRegister();
            /* static link */
            if (hops == 0) {
                writer.ldw(regA, RegisterAllocator.BASE_POINTER);
            } else {
                writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

                for (int i = 0; i < hops - 1; ++i)
                    writer.ldw(regA, "(" + regA + ")");
            }

            writer.adi(regA, regA, "#" + offset);
            writer.stw(reg, "(" + regA + ")");
            registerAllocator.releaseTemporaryRegister(regA);
            return;
        } else if (action.equals(GET_REGISTER_FROM_ITSELF)) {
            // load fastly the value from a fixed registry as we know the value is there
            String fixed_reg;
            if (!keepVar) {
                // if we want to clear the entry in the graph
                fixed_reg = record.getCurrentRegister();
            } else {
                // if we want to keep the entry for an unknown period before clearing it
                fixed_reg = record.getFirstRegister();
            }
            writer.ldw(reg, fixed_reg);
            return;
        } else if (action.equals(STORE_REGISTER_IN_STACK_GET_FROM_STACK)) {
            // if that happens it means that we had a sequence like that R0 R1 R0 but because R0 was set in the stack
            // getting R0 from the stack into R1 to then store R1 into the stack makes no sense
            // so we just need to load the stack into Rtemp which is equivalent to load the old content of R0
        }
        // end of fixed register handling

        String regA = registerAllocator.getTemporaryRegister();
        /* static link */
        if (hops == 0) {
            writer.ldw(regA, RegisterAllocator.BASE_POINTER);
        } else {
            writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

            for (int i = 0; i < hops - 1; ++i)
                writer.ldw(regA, "(" + regA + ")");
        }

        writer.adi(regA, regA, "#" + offset);
        writer.ldw(reg, "(" + regA + ")");

        registerAllocator.releaseTemporaryRegister(regA);
    }

    public void getVar(String idf, String reg1, String reg2, SymbolTable tds) {
        Object[] res = tds.lookupCountGenre(idf, Record.Genre.VARIABLE);
        if (res == null) {
            res = tds.lookupCountGenre(idf, Record.Genre.ARRAY);
            if (res==null) {
                logger.logCritical("Impossible to get " + idf);
                return;
            }
        }
        Record record = (Record) res[0];

        if (record.getSize() == 2) {
            logger.logCritical("Wrong use of getVar for size == 2");
            return;
        }

        int hops = (int) res[1];
        int offset = record.getOffset();

        String regA = registerAllocator.getTemporaryRegister();
        /* static link */
        if (hops == 0) {
            writer.ldw(regA, RegisterAllocator.BASE_POINTER);
        } else {
            writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

            for (int i = 0; i < hops - 1; ++i)
                writer.ldw(regA, "(" + regA + ")");
        }

        writer.adi(regA, regA, "#" + offset);
        if (offset <= 0) {
            writer.ldw(reg2, "(" + regA + ")-2");
            writer.ldw(reg1, "(" + regA + ")");
        } else {
            writer.ldw(reg2, "(" + regA + ")");
            writer.ldw(reg1, "(" + regA + ")2");
        }

        registerAllocator.releaseTemporaryRegister(regA);
    }

    public void setVar(String idf, String reg, SymbolTable tds, boolean keepVar) {
        int cshops = 0;
        Scope cs = tds.getCurrent();

        while (cs != null && (cs.getProcedureName() == null || !cs.getProcedureName().equals(idf))) {
            cs = cs.getParent();
            cshops++;
        }

        if (cs != null && cs.getProcedureName() != null && cs.getProcedureName().equals(idf)) {
            String regA = registerAllocator.getTemporaryRegister();
            /* static link */
            if (cshops == 0) {
                writer.ldw(regA, RegisterAllocator.BASE_POINTER);
            } else {
                writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

                for (int i = 0; i < cshops - 1; ++i)
                    writer.ldw(regA, "(" + regA + ")");
            }

            writer.stw(reg, "(" + regA + ")-2");

            registerAllocator.releaseTemporaryRegister(regA);
            return;
        }

        Object[] res = tds.lookupCountGenre(idf, Record.Genre.VARIABLE);
        if (res == null) {
            logger.logCritical("Impossible to get " + idf);
            return;
        }
        Record record = (Record) res[0];

        if (record.getSize() != 2) {
            logger.logCritical("Wrong use of setVar for size != 2");
            return;
        }

        int hops = (int) res[1];
        int offset = record.getOffset();


        // Start of fixed register handling
        // warning getCurrentAction and getCurrentRegister have side effects, use with caution
        String action;
        if (!keepVar) {
            action = record.getCurrentAction();
        } else {
            action = record.getFirstAction();
        }
        if (debugCompiler) {
            writer.comment("ACTION " + action + ", SET VAR " + record.getId());
        }
        if (action.equals(GET_REGISTER_FROM_STACK) || action.equals(GET_REGISTER_FROM_ITSELF)) {
            // We are using a fixed register
            String fixed_reg;
            if (!keepVar) {
                // if we want to clear the entry in the graph
                fixed_reg = record.getCurrentRegister();
            } else {
                // if we want to keep the entry for an unknown period before clearing it
                fixed_reg = record.getFirstRegister();
            }
            if (action.equals(GET_REGISTER_FROM_STACK)) {
                // if the value was in the stack we still need to instantiate once our fixed register
                String regA = registerAllocator.getTemporaryRegister();
                /* static link */
                if (hops == 0) {
                    writer.ldw(regA, RegisterAllocator.BASE_POINTER);
                } else {
                    writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

                    for (int i = 0; i < hops - 1; ++i)
                        writer.ldw(regA, "(" + regA + ")");
                }

                writer.adi(regA, regA, "#" + offset);
                writer.ldw(fixed_reg, "(" + regA + ")");

                registerAllocator.releaseTemporaryRegister(regA);
            }
            // we can now load from the fixed register the value in the temporary register
            writer.stw(reg, fixed_reg);
            return;
        }
        // end of fixed register handling

        String regA = registerAllocator.getTemporaryRegister();
        /* static link */
        if (hops == 0) {
            writer.ldw(regA, RegisterAllocator.BASE_POINTER);
        } else {
            writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

            for (int i = 0; i < hops - 1; ++i)
                writer.ldw(regA, "(" + regA + ")");
        }

        writer.adi(regA, regA, "#" + offset);
        writer.stw(reg, "(" + regA + ")");

        registerAllocator.releaseTemporaryRegister(regA);
    }

    public void setVar(String idf, String reg1, String reg2, SymbolTable tds) {
        if (reg2 == null) {
            reg2 = registerAllocator.getNextAvailableRegister();
            writer.ldq("0", reg2);
        }

        int cshops = 0;
        Scope cs = tds.getCurrent();

        while (cs != null && (cs.getProcedureName() == null || !cs.getProcedureName().equals(idf))) {
            cs = cs.getParent();
            cshops++;
        }

        if (cs != null && cs.getProcedureName() != null && cs.getProcedureName().equals(idf)) {
            String regA = registerAllocator.getTemporaryRegister();
            /* static link */
            if (cshops == 0) {
                writer.ldw(regA, RegisterAllocator.BASE_POINTER);
            } else {
                writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

                for (int i = 0; i < cshops - 1; ++i)
                    writer.ldw(regA, "(" + regA + ")");
            }

            writer.stw(reg1, "(" + regA + ")-4");
            writer.stw(reg2, "(" + regA + ")-2");

            registerAllocator.releaseTemporaryRegister(regA);
            return;
        }

        Object[] res = tds.lookupCountGenre(idf, Record.Genre.VARIABLE);
        if (res == null) {
            logger.logCritical("Impossible to get " + idf);
            return;
        }
        Record record = (Record) res[0];

        if (record.getSize() == 2) {
            logger.logCritical("Wrong use of setVar for size == 2");
            return;
        }

        int hops = (int) res[1];
        int offset = record.getOffset();
        if (offset > 0) offset += 2;


        String regA = registerAllocator.getTemporaryRegister();
        /* static link */
        if (hops == 0) {
            writer.ldw(regA, RegisterAllocator.BASE_POINTER);
        } else {
            writer.ldw(regA, "(" + RegisterAllocator.BASE_POINTER + ")");

            for (int i = 0; i < hops - 1; ++i)
                writer.ldw(regA, "(" + regA + ")");
        }

        writer.adi(regA, regA, "#" + offset);
        writer.stw(reg2, "(" + regA + ")-2");
        writer.stw(reg1, "(" + regA + ")");

        registerAllocator.releaseTemporaryRegister(regA);
        registerAllocator.releaseRegister(reg2);
    }
}
