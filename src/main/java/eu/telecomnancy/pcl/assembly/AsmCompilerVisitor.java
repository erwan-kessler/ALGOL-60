package eu.telecomnancy.pcl.assembly;

import eu.telecomnancy.pcl.Main;
import eu.telecomnancy.pcl.assembly.register.*;
import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.semantics.StaticSemanticException;
import eu.telecomnancy.pcl.symbolTable.*;
import eu.telecomnancy.pcl.symbolTable.Records.ArrayRecord;
import eu.telecomnancy.pcl.symbolTable.Records.ProcedureRecord;
import eu.telecomnancy.pcl.symbolTable.Records.SwitchRecord;

import java.util.*;

import static eu.telecomnancy.pcl.Main.*;

public class AsmCompilerVisitor implements ASTVisitor<AsmCompilerVisitor> {
    private static final boolean debugCompiler = Main.debugCompiler || debugAll;

    private static final String MAIN = "main";

    private final SymbolTable tds;
    private final StringBuffer src;
    private final StringBuffer heap;

    boolean isArrayAssign = false;
    private final AsmWriter writer;
    private final AsmVar varUtils;
    private final AsmLink linkUtils;
    private String reg, regf;

    public AsmCompilerVisitor(SymbolTable tds) {
        this.tds = tds;
        tds.resetTable();

        src = new StringBuffer("\n");
        heap = new StringBuffer("\nheap\nWORD\t\t\t\t\t0\t\t// number of array declared\nWORD\t\t\t\t\t0\t\t// current global offset\n");
        writer = new AsmWriter(src, heap);

        varUtils = new AsmVar(writer);
        linkUtils = new AsmLink(writer);
        registerAllocator.setWriter(writer);
        registerAllocator.setCompilerVisitor(this);
    }

    public void setReg(String reg) {
        this.reg = reg;
    }

    public String uniqueId(ASTNode node) {
        return node.toString().split("@")[1];
    }

    public String uniqueHashCode(String identifier) {
        // java hashcode is signed 32 bits, parodi doesnt like - ...
        return String.valueOf(tds.lookup(identifier).hashCode()).replace("-", "");
    }

    public boolean isReal(ASTNode nodeLeft, ASTNode nodeRight) {
        return nodeLeft.getExprType(tds) == ASTType.REAL || nodeRight.getExprType(tds) == ASTType.REAL;
    }

    public StringBuffer getSrc() {
        return src;
    }

    public StringBuffer getHeap() {
        return heap;
    }

    private void debug(String message) {
        if (debugCompiler)
            writer.comment(message);
    }

    private void tabsInc() {
        if (debugCompiler)
            writer.tabsInc();
    }

    private void tabsDec() {
        if (debugCompiler)
            writer.tabsDec();
    }

    @Override
    public AsmCompilerVisitor visit(ASTAdd node) {
        debug("ADD");
        tabsInc();

        boolean real = node.getExprType(tds) == ASTType.REAL;
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);

        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@add_fix");

            writer.ldw(regf, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("4", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.add(regA, reg, reg);
            registerAllocator.releaseRegister(regA);
            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTAnd node) {
        debug("AND");
        tabsInc();

        node.getNodeLeft().accept(this);
        String regA = reg;

        node.getNodeRight().accept(this);
        registerAllocator.preventRegisterExhaustion();
        writer.and(regA, reg, reg);
        registerAllocator.releaseRegister(regA);

        tabsDec();
        return this;

    }

    @Override
    public AsmCompilerVisitor visit(ASTAssign node) {
        debug("ASSIGN");
        tabsInc();

        node.getExpression().accept(this);

        // rounding
        if (node.getDesignators().get(0).getExprType(tds) == ASTType.INTEGER) {
            if (node.getExpression().getExprType(tds) == ASTType.REAL && regf != null) {
                writer.tst(regf);
                writer.bge("2");
                writer.adq("1", reg);
                writer.ldq("0", regf);
            }
        }
        for (ASTNode nodeChild : node.getDesignators()) {
            registerAllocator.preventRegisterExhaustion();
            if (nodeChild instanceof ASTVar) {
                if (nodeChild.getExprType(tds) == ASTType.REAL) {
                    varUtils.setVar(((ASTVar) nodeChild).getIdentifier(), reg, regf, tds);
                } else {
                    varUtils.setVar(((ASTVar) nodeChild).getIdentifier(), reg, tds, false);
                }
            } else if (nodeChild instanceof ASTVarSubscript) {
                isArrayAssign = true;
                // we will free the reg inside that call
                nodeChild.accept(this);
            } else {
                System.out.println("/!\\ TODO instanceof != ASTVar");
            }
        }
        if (!isArrayAssign) {
            registerAllocator.releaseRegister(reg);
            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }
        }
        isArrayAssign = false;
        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTBlock node) {
        debug("BLOCK");
        tabsInc();

        writer.ldw(RegisterAllocator.RET, RegisterAllocator.BASE_POINTER);

        linkUtils.linkFrame(tds);

        for (ASTNode nodeChild : node.getDeclarations())
            nodeChild.accept(this);

        for (ASTNode nodeChild : node.getStatements())
            nodeChild.accept(this);

        linkUtils.unlinkFrame(tds);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTDecl node) {
        debug("DECL");
        tabsInc();
        debug(node.getIdentifier() + " offset : " + tds.lookup(node.getIdentifier()).getOffset());
        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTDeclArray node) {
        debug("DECL_ARRAY");
        tabsInc();
        if (node.getBounds().size() > 127) {
            logger.logCritical("You have used too many dimensions, you will have unexpected behavior", node);
        }
        String lbl_array = "array_" + uniqueHashCode(node.getIdentifier());
        String lbl_bound_error = "bound_error_" + uniqueHashCode(node.getIdentifier());
        // putting in place the static table
        writer.writeHeap(lbl_array);
        writer.writeHeap("WORD\t\t\t\t\t" + (node.getType() == ASTType.REAL ? 4 : 2) + "\t\t// type of the array (2 for boolean and integer, 4 for real)");
        writer.writeHeap("WORD\t\t\t\t\t" + node.getBounds().size() + "\t\t// dimensions of the array");
        writer.writeHeap("WORD\t\t\t\t\t" + 0 + "\t\t// offset of the array");
        writer.writeHeap("WORD\t\t\t\t\t" + 0 + "\t\t// size of the array");


        List<ASTDeclArrayBound> bounds = node.getBounds();
        Collections.reverse(bounds);
        for (ASTDeclArrayBound bound : bounds) {
            bound.getBoundInf().accept(this);
            // stacking the inf bound first
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            bound.getBoundSup().accept(this);
            // stacking the inf bound second
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
        }
        writer.writeStatic(lbl_bound_error);
        writer.writeStatic("STRING\t\t\t\t\" for the array at line: " + node.getSourceLine() + " character: " + node.getSourceChar() + "\"");


        String temp_reg = registerAllocator.getTemporaryRegister();
        // stacking the error message
        writer.ldw(temp_reg, "#" + lbl_bound_error);
        writer.stw(temp_reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
        // stacking the address of said array
        writer.ldw(temp_reg, "#" + lbl_array);
        writer.stw(temp_reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
        registerAllocator.releaseTemporaryRegister(temp_reg);

        writer.jsr("@allocate_array");
        writer.adq("-" + (2 + bounds.size()) * 2, "SP");
        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTDeclFcn node) {
        debug("DECL_FCN");
        debug("function name: " + node.getIdentifier());
        tabsInc();

        String lblFcn = "fcn_" + uniqueHashCode(node.getIdentifier());
        String lblFcnExit = "fcnexit_" + uniqueHashCode(node.getIdentifier());

        /* TODO: obviously not the best method... */
        writer.jmp("#" + lblFcnExit + " - $ - 2");

        writer.label(lblFcn);

        linkUtils.linkFrame(tds);

        if (node.getType() == ASTType.BOOLEAN || node.getType() == ASTType.INTEGER) {
            writer.adq("-2", RegisterAllocator.STACK_POINTER);
        } else if (node.getType() == ASTType.REAL) {
            writer.adq("-4", RegisterAllocator.STACK_POINTER);
        }

        /* XXX: fixes `RegisterAllocator` for recursive flows */
        if (node.isRecursive()) {
            writer.comment("Fixing RegisterAllocator for recursive flows");
            for (int i = 2; i <= 13; ++i)
                writer.stw("R" + i, "-(" + RegisterAllocator.STACK_POINTER + ")");
        }

        node.getStatement().accept(this);

        if (node.isRecursive()) {
            writer.comment("Fixing RegisterAllocator for recursive flows");
            for (int i = 13; i >= 2; --i)
                writer.ldw("R" + i, "(" + RegisterAllocator.STACK_POINTER + ")+");
        }

        if (node.getType() == ASTType.BOOLEAN || node.getType() == ASTType.INTEGER) {
            writer.ldw(RegisterAllocator.RET, "(" + RegisterAllocator.BASE_POINTER + ")-2");
        } else if (node.getType() == ASTType.REAL) {
            writer.ldw(RegisterAllocator.RET, "(" + RegisterAllocator.BASE_POINTER + ")-4");
            writer.ldw(RegisterAllocator.RETF, "(" + RegisterAllocator.BASE_POINTER + ")-2");
        }

        linkUtils.unlinkFrame(tds);

        writer.rts();

        writer.label(lblFcnExit);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTDeclSwitch node) {
        debug("DECL_SWITCH");
        tabsInc();

        String lbl = "switch_" + uniqueHashCode(node.getIdentifier());
        writer.writeStatic(lbl);

        int i = 0;
        for (ASTNode nodeChild : node.getExpressions()) {
            writer.writeStatic("  WORD 0");
            if (nodeChild instanceof ASTNumber) {
                // special case : numbers alone can't tell us if they are labels or not
                // so we have to take them by the hand
                reg = registerAllocator.getNextAvailableRegister();
                ASTNumber labelNb = (ASTNumber) nodeChild;
                writer.ldw(reg, "#lbl_" + tds.lookup(String.valueOf((int) labelNb.getValue())).hashCode());
            } else {
                nodeChild.accept(this);
            }
            writer.stw(reg, "@" + lbl + " + " + i);
            i += 2;
            // TODO REGISTER EXHAUSTION
            registerAllocator.releaseRegister(reg);
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTDiv node) {
        debug("DIV");
        tabsInc();

        boolean real = node.getExprType(tds) == ASTType.REAL;
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        /* Error message if division by zero */
        String lblMsg = "divzero_" + uniqueId(node);
        writer.writeStatic(lblMsg);
        writer.writeStatic("STRING \"/!\\ Division by zero at line "
                + node.getSourceLine() + ", character "
                + node.getSourceChar() + " \"");
        String lblNonZero = "nonzero_" + uniqueId(node);

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            /* Check zero */
            writer.tst(reg);
            writer.jne("#" + lblNonZero + " - $ - 2");
            writer.tst(regf);
            writer.jne("#" + lblNonZero + " - $ - 2");
            String regC = registerAllocator.getTemporaryRegister();
            writer.ldw(regC, "#" + lblMsg);
            writer.stw(regC, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.jsr("@print_string");
            writer.adq("2", RegisterAllocator.STACK_POINTER);
            writer.jsr("@print_newline");
            registerAllocator.releaseTemporaryRegister(regC);
            writer.jsr("@exit");
            writer.label(lblNonZero);

            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@div_fix");

            writer.ldw(regf, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("4", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            /* Check zero */
            writer.tst(reg);
            writer.jne("#" + lblNonZero + " - $ - 2");
            String regC = registerAllocator.getTemporaryRegister();
            writer.ldw(regC, "#" + lblMsg);
            writer.stw(regC, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.jsr("@print_string");
            writer.adq("2", RegisterAllocator.STACK_POINTER);
            writer.jsr("@print_newline");
            registerAllocator.releaseTemporaryRegister(regC);
            writer.jsr("@exit");
            writer.label(lblNonZero);

            writer.div(regA, reg, reg);

            registerAllocator.releaseRegister(regA);

            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTDummy node) {
        debug("DUMMY");
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTEq node) {
        debug("EQ");
        tabsInc();

        boolean real = isReal(node.getNodeLeft(), node.getNodeRight());
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@eq_fix");

            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("6", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.cmp(regA, reg);
            writer.beq("4");
            writer.ldq("0", regA);
            writer.bmp("2");
            writer.ldq("1", regA);
            registerAllocator.releaseRegister(reg);
            reg = regA;
        }

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTFcn node) {
        debug("FCN");
        tabsInc();

        switch (node.getIdentifier()) {

            case "exp":

            node.getParameters().get(0).accept(this);

            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }

            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");


            writer.jsr("@fcn_exp");
            writer.ldw(reg,RegisterAllocator.RET);
            writer.ldw(regf,RegisterAllocator.RETF);
            writer.adq("4", RegisterAllocator.STACK_POINTER);

            registerAllocator.releaseRegister(reg);
            registerAllocator.releaseRegister(regf);
            break;

            case "log":
                node.getParameters().get(0).accept(this);

                if (regf == null) {
                    regf = registerAllocator.getNextAvailableRegister();
                    writer.ldq("0", regf);
                }

                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");


                writer.jsr("@fcn_log");
                writer.ldw(reg,RegisterAllocator.RET);
                writer.ldw(regf,RegisterAllocator.RETF);
                writer.adq("4", RegisterAllocator.STACK_POINTER);

                registerAllocator.releaseRegister(reg);
                registerAllocator.releaseRegister(regf);

            break;


            case "outinteger":
                node.getParameters().get(1).accept(this);

                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.jsr("@print_short");
                writer.adq("2", RegisterAllocator.STACK_POINTER);

                registerAllocator.releaseRegister(reg);

                writer.jsr("@print_newline");

                if (regf != null) {
                    registerAllocator.releaseRegister(regf);
                    regf = null;
                }

                break;
            case "outreal":
                node.getParameters().get(1).accept(this);

                if (regf == null) {
                    regf = registerAllocator.getNextAvailableRegister();
                    writer.ldq("0", regf);
                }

                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.jsr("@print_fix");
                writer.adq("4", RegisterAllocator.STACK_POINTER);

                registerAllocator.releaseRegister(reg);
                registerAllocator.releaseRegister(regf);

                writer.jsr("@print_newline");

                break;
            case "outstring":
                node.getParameters().get(1).accept(this);
                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.jsr("@print_string");
                writer.adq("2", RegisterAllocator.STACK_POINTER);

                registerAllocator.releaseRegister(reg);

                writer.jsr("@print_newline");

                break;
            case "outchar":
                node.getParameters().get(1).accept(this);
                String regA = reg;
                node.getParameters().get(2).accept(this);
                writer.add(reg, regA, reg);
                writer.ldb(regA, "(" + reg + ")");
                writer.stw(regA, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.ldq("0", regA);
                writer.stb(regA, "(" + reg + ")");
                writer.adq("-1", reg);

                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.jsr("@print_string");
                writer.adq("2", RegisterAllocator.STACK_POINTER);

                writer.ldw(regA, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.adq("1", reg);
                writer.stb(regA, "(" + reg + ")");

                registerAllocator.releaseRegister(regA);
                registerAllocator.releaseRegister(reg);

                break;
            case "heapInfo":
                writer.jsr("@heap_information");
                break;
            default:
                ProcedureRecord fcnRec = (ProcedureRecord) tds.lookup(node.getIdentifier(), Record.Genre.PROCEDURE);
                String lblFcn = "fcn_" + String.valueOf(fcnRec.hashCode()).replace("-", "");

                int size = 0;

                List<ASTNode> parameters = node.getParameters();
                Collections.reverse(parameters);

                List<ASTDeclFcnSpec> specifications = fcnRec.getSpecifications();
                Collections.reverse(specifications);

                for (int i = 0; i < parameters.size(); i++) {
                    ASTNode nodeChild = parameters.get(i);
                    ASTDeclFcnSpecType specType = specifications.get(i).getType();

                    if (specType == ASTDeclFcnSpecType.REAL)
                        size += 4;
                    else
                        size += 2;

                    nodeChild.accept(this);

                    // real rounding and integer -> real conversion
                    if (specType == ASTDeclFcnSpecType.INTEGER
                            && nodeChild.getExprType(tds) == ASTType.REAL
                            && regf != null) {
                        writer.tst(regf);
                        writer.bge("2");
                        writer.adq("1", reg);
                        writer.ldq("0", regf);
                    } else if (specType == ASTDeclFcnSpecType.REAL && nodeChild.getExprType(tds) == ASTType.INTEGER) {
                        if (regf == null)
                            regf = registerAllocator.getNextAvailableRegister();
                        writer.ldq("0", regf);
                    }

                    writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");

                    if (specType == ASTDeclFcnSpecType.REAL)
                        writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

                    registerAllocator.releaseRegister(reg);
                    if (regf != null) {
                        registerAllocator.releaseRegister(regf);
                        regf = null;
                    }
                }

                Object[] res = tds.lookupCountGenre(node.getIdentifier(), Record.Genre.PROCEDURE);
                Record record = (Record) res[0];

                int hops = (int) res[1];

                if (hops == 0) {
                    writer.ldw(RegisterAllocator.RET, RegisterAllocator.BASE_POINTER);
                } else {
                    writer.ldw(RegisterAllocator.RET, "(" + RegisterAllocator.BASE_POINTER + ")");
                    for (int i = 0; i < hops - 1; ++i)
                        writer.ldw(RegisterAllocator.RET, "(" + RegisterAllocator.RET + ")");
                }

                writer.jsr("@" + lblFcn);

                if (size != 0) {
                    writer.adi(RegisterAllocator.STACK_POINTER, RegisterAllocator.STACK_POINTER, "#" + size);
                }

                if (record.getType() != ASTType.NONE) {
                    regA = registerAllocator.getNextAvailableRegister();
                    writer.ldw(regA, RegisterAllocator.RET);
                    reg = regA;
                }

                if (record.getType() == ASTType.REAL) {
                    String regB = registerAllocator.getNextAvailableRegister();
                    writer.ldw(regB, RegisterAllocator.RETF);
                    regf = regB;
                }
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTFor node) {
        debug("FOR");
        tabsInc();

        String regA, regAf;

        String lblFor = "for_" + uniqueId(node);

        ASTNode variable = node.getVariable();
        ASTType vartype = variable.getExprType(tds);

        writer.adq("-2", RegisterAllocator.STACK_POINTER);

        writer.jmp("#" + "foriter0_" + uniqueId(node) + " - $ - 2");

        writer.label(lblFor);

        node.getStatement().accept(this);

        regA = registerAllocator.getTemporaryRegister();
        writer.ldw(regA, "(" + RegisterAllocator.STACK_POINTER + ")");
        /* XXX: `jpa` opcode is broken
         * smart workaround
         */
        writer.stw(regA, "-(" + RegisterAllocator.STACK_POINTER + ")");
        writer.rts();
        registerAllocator.releaseTemporaryRegister(regA);

        int cnt = 0;
        for (ASTForIterator iterator : node.getIterators()) {
            writer.label("foriter" + cnt + "_" + uniqueId(node));

            iterator.getExpression().accept(this);
            ASTType itertype = iterator.getExpression().getExprType(tds);

            registerAllocator.preventRegisterExhaustion();

            if (variable instanceof ASTVar) {
                if (vartype != ASTType.INTEGER) {
                    varUtils.setVar(((ASTVar) variable).getIdentifier(), reg, regf, tds);
                    registerAllocator.releaseRegister(reg);
                    regf = null;    // already released by setVar
                } else {
                    // round if the iterator is real
                    if (itertype == ASTType.REAL && regf != null) {
                        writer.tst(regf);
                        writer.bge("2");
                        writer.adq("1", reg);
                        writer.ldq("0", regf);
                        registerAllocator.releaseRegister(regf);
                        regf = null;
                    }
                    varUtils.setVar(((ASTVar) variable).getIdentifier(), reg, tds, true);
                    registerAllocator.releaseRegister(reg);
                }
            } else {
                logger.logCritical("Array in for iterator is not allowed, please load it in a variable first");
            }

            if (iterator instanceof ASTForIteratorExpr) {
                regA = registerAllocator.getTemporaryRegister();

                writer.ldw(regA, "#foriter" + (cnt + 1) + "_" + uniqueId(node));
                writer.stw(regA, "(" + RegisterAllocator.STACK_POINTER + ")");

                writer.jmp("#" + lblFor + " - $ - 2");

                registerAllocator.releaseTemporaryRegister(regA);
            } else if (iterator instanceof ASTForIteratorWhile) {
                regA = registerAllocator.getTemporaryRegister();

                writer.ldw(regA, "#foriter" + cnt + "_cond_" + uniqueId(node));
                writer.stw(regA, "(" + RegisterAllocator.STACK_POINTER + ")");

                registerAllocator.releaseTemporaryRegister(regA);

                writer.label("foriter" + cnt + "_cond_" + uniqueId(node));

                ((ASTForIteratorWhile) iterator).getCondition().accept(this);

                writer.tst(reg);

                registerAllocator.releaseRegister(reg);

                writer.jne("#" + lblFor + " - $ - 2");

                writer.jmp("#foriter" + (cnt + 1) + "_" + uniqueId(node) + " - $ - 2");

            } else if (iterator instanceof ASTForIteratorStepUntil) {
                // TODO check memory exhaustion side effects here

                regA = registerAllocator.getTemporaryRegister();

                writer.ldw(regA, "#foriter" + cnt + "_next_" + uniqueId(node));
                writer.stw(regA, "(" + RegisterAllocator.STACK_POINTER + ")");

                registerAllocator.releaseTemporaryRegister(regA);

                writer.label("foriter" + cnt + "_cond_" + uniqueId(node));

                regA = registerAllocator.getNextAvailableRegister();
                regAf = registerAllocator.getNextAvailableRegister();

                // load the iterator
                if (variable instanceof ASTVar) {
                    if (variable.getExprType(tds) != ASTType.INTEGER) {
                        varUtils.getVar(((ASTVar) variable).getIdentifier(), regA, regAf, tds);
                    } else {
                        varUtils.getVar(((ASTVar) variable).getIdentifier(), regA, tds, true);
                        writer.ldq("0", regAf);
                    }
                } else {
                    logger.logCritical("Array in for iterator is not allowed, please load it in a variable first");
                }


                // save the value of iterator for later
                writer.stw(regA, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regAf, "-(" + RegisterAllocator.STACK_POINTER + ")");


                ((ASTForIteratorStepUntil) iterator).getUntilExpression().accept(this);

                if (regf == null) {
                    regf = registerAllocator.getNextAvailableRegister();
                    writer.ldq("0", regf);
                }

                // until - iterator -> A
                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regA, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regAf, "-(" + RegisterAllocator.STACK_POINTER + ")");

                writer.jsr("@neg_fix");
                writer.jsr("@add_fix");

                writer.ldw(regAf, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.ldw(regA, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.adq("4", RegisterAllocator.STACK_POINTER);

                // free registers
                registerAllocator.releaseRegister(reg);
                registerAllocator.releaseRegister(regf);
                regf = null;

                ((ASTForIteratorStepUntil) iterator).getStepExpression().accept(this);

                if (regf == null) {
                    regf = registerAllocator.getNextAvailableRegister();
                    writer.ldq("0", regf);
                }

                // save the value of step for later
                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

                // step < 0 => -A -> A
                writer.tst(reg);
                writer.bge("12");
                writer.stw(regA, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regAf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.jsr("@neg_fix");
                writer.ldw(regAf, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.ldw(regA, "(" + RegisterAllocator.STACK_POINTER + ")+");

                // re-put what we need on the top for the next iteration
                writer.ldw(regAf, "(" + RegisterAllocator.STACK_POINTER + ")8");
                writer.stw(regAf, "-(" + RegisterAllocator.STACK_POINTER + ")");

                // A >= 0 => next for-iteration
                writer.tst(regA);
                writer.jge("#" + lblFor + " - $ - 2");
                writer.jmp("#foriter" + (cnt + 1) + "_" + uniqueId(node) + " - $ - 2");
                writer.label("foriter" + cnt + "_next_" + uniqueId(node));

                // recover the value of step and iterator
                writer.adq("2", RegisterAllocator.STACK_POINTER);
                writer.ldw(regf, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.ldw(regAf, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.ldw(regA, "(" + RegisterAllocator.STACK_POINTER + ")+");

                // iterator + step -> iterator
                writer.stw(regA, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regAf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

                writer.jsr("@add_fix");

                writer.ldw(regAf, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.ldw(regA, "(" + RegisterAllocator.STACK_POINTER + ")+");
                writer.adq("4", RegisterAllocator.STACK_POINTER);


                // save iterator
                if (variable instanceof ASTVar) {
                    if (variable.getExprType(tds) != ASTType.INTEGER) {
                        varUtils.setVar(((ASTVar) variable).getIdentifier(), regA, regAf, tds);
                    } else {
                        // rounding
                        writer.tst(regAf);
                        writer.bge("2");
                        writer.adq("1", regA);
                        writer.ldq("0", regAf);

                        varUtils.setVar(((ASTVar) variable).getIdentifier(), regA, tds, true);
                    }
                } else {
                    logger.logCritical("Array in for iterator is not allowed, please load it in a variable first");
                }


                // else : go to the next for-condition
                writer.jmp("#foriter" + cnt + "_cond_" + uniqueId(node) + " - $ - 2");

                // free registers
                registerAllocator.releaseRegister(reg);
                registerAllocator.releaseRegister(regf);
                regf = null;
                registerAllocator.releaseRegister(regA);
                registerAllocator.releaseRegister(regAf);
            }
            cnt++;
        }

        writer.label("foriter" + cnt + "_" + uniqueId(node));

        writer.adq("2", RegisterAllocator.STACK_POINTER);

        // Clearing the for variable from graph
        if (variable instanceof ASTVar) {
            registerAllocator.getAction(((ASTVar) node.getVariable()).getIdentifier());
            registerAllocator.getRegister(((ASTVar) node.getVariable()).getIdentifier());
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTGe node) {
        debug("GE");
        tabsInc();

        boolean real = isReal(node.getNodeLeft(), node.getNodeRight());
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@ge_fix");

            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("6", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.cmp(regA, reg);
            writer.bge("4");
            writer.ldq("0", regA);
            writer.bmp("2");
            writer.ldq("1", regA);
            registerAllocator.releaseRegister(reg);
            reg = regA;
        }

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTGoto node) {
        debug("GOTO");
        tabsInc();

        if (node.getExpression() instanceof ASTNumber) {
            // special case : numbers alone can't tell us if they are labels or not
            // so we have to take them by the hand
            reg = registerAllocator.getNextAvailableRegister();
            ASTNumber labelNb = (ASTNumber) node.getExpression();
            String lbl = "lbl_" + tds.lookup(String.valueOf((int) labelNb.getValue())).hashCode();
            writer.ldw(reg, "#" + lbl);
        } else {
            node.getExpression().accept(this);
        }
        // JPA doesn't work with some registers ¯\_(ツ)_/¯
        writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
        writer.rts();

        registerAllocator.releaseRegister(reg);
        // TODO: unstack environments if the label is outside the current one

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTGt node) {
        debug("GT");
        tabsInc();

        boolean real = isReal(node.getNodeLeft(), node.getNodeRight());
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@gt_fix");

            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("6", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.cmp(regA, reg);
            writer.bgt("4");
            writer.ldq("0", regA);
            writer.bmp("2");
            writer.ldq("1", regA);
            registerAllocator.releaseRegister(reg);
            reg = regA;
        }

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTIdiv node) {
        debug("IDIV");
        tabsInc();

        node.getNodeLeft().accept(this);
        String regA = reg;

        node.getNodeRight().accept(this);
        String regB = reg;
        registerAllocator.preventRegisterExhaustion();

        /* Check zero */
        String lblMsg = "divzero_" + uniqueId(node);
        writer.writeStatic(lblMsg);
        writer.writeStatic("STRING \"/!\\ Division by zero at line "
                + node.getSourceLine() + ", character "
                + node.getSourceChar() + " \"");
        String lblNonZero = "nonzero_" + uniqueId(node);
        writer.tst(regB);
        writer.jne("#" + lblNonZero + " - $ - 2");

        String regC = registerAllocator.getTemporaryRegister();

        writer.ldw(regC, "#" + lblMsg);
        writer.stw(regC, "-(" + RegisterAllocator.STACK_POINTER + ")");
        writer.jsr("@print_string");
        writer.adq("2", RegisterAllocator.STACK_POINTER);
        writer.jsr("@print_newline");

        registerAllocator.releaseTemporaryRegister(regC);

        writer.jsr("@exit");
        writer.label(lblNonZero);

        writer.div(regA, regB, regB);

        registerAllocator.releaseRegister(regA);
        reg = regB;

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }
        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTIf node) {
        debug("IF");
        tabsInc();

        String lblElse = "else_" + uniqueId(node);
        String lblEnd = "ifend_" + uniqueId(node);

        node.getCondition().accept(this);
        // TODO check here
        registerAllocator.preventRegisterExhaustion();
        writer.tst(reg);
        registerAllocator.releaseRegister(reg);

        writer.jeq("#" + lblElse + " - $ - 2");

        node.getThenStatement().accept(this);
        writer.jmp("#" + lblEnd + " - $ - 2");

        writer.label(lblElse);
        node.getElseStatement().accept(this);
        writer.label(lblEnd);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTIfe node) {
        debug("IFE");
        tabsInc();

        String lblElse = "else_" + uniqueId(node);
        String lblEnd = "ifend_" + uniqueId(node);
        String regA = registerAllocator.getNextAvailableRegister();

        node.getCondition().accept(this);
        // TODO test indepth
        registerAllocator.preventRegisterExhaustion();
        writer.tst(reg);
        writer.jeq("#" + lblElse + " - $ - 2");
        registerAllocator.releaseRegister(reg);

        node.getThenExpression().accept(this);
        registerAllocator.preventRegisterExhaustion();
        writer.ldw(regA, reg);
        registerAllocator.releaseRegister(reg);
        writer.jmp("#" + lblEnd + " - $ - 2");

        writer.label(lblElse);
        node.getElseExpression().accept(this);
        registerAllocator.preventRegisterExhaustion();
        writer.ldw(regA, reg);
        registerAllocator.releaseRegister(reg);
        writer.label(lblEnd);

        reg = regA;

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTIff node) {
        debug("IFF");
        tabsInc();

        node.getNodeLeft().accept(this);
        String regA = reg;

        node.getNodeRight().accept(this);
        registerAllocator.preventRegisterExhaustion();
        String regB = reg;

        writer.xor(regA, regB, regA);
        writer.not(regA, regA);
        writer.ani(regA, regA, "#0001");

        reg = regA;

        registerAllocator.releaseRegister(regB);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTImpl node) {
        debug("IMPL");
        tabsInc();

        node.getNodeLeft().accept(this);
        String regA = reg;

        node.getNodeRight().accept(this);
        registerAllocator.preventRegisterExhaustion();
        String regB = reg;

        writer.not(regA, regA);
        writer.ani(regA, regA, "#0001");
        writer.or(regA, regB, regA);

        reg = regA;

        registerAllocator.releaseRegister(regB);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTLabel node) {
        debug("LABEL");
        writer.label("lbl_" + uniqueHashCode(node.getIdentifier()));
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTLe node) {
        debug("LE");
        tabsInc();

        boolean real = isReal(node.getNodeLeft(), node.getNodeRight());
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            // lower or equal <=> not greater than
            writer.jsr("@gt_fix");

            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("6", RegisterAllocator.STACK_POINTER);
            writer.not(reg, reg);
            writer.ani(reg, reg, "#0001");
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.cmp(regA, reg);
            writer.ble("4");
            writer.ldq("0", regA);
            writer.bmp("2");
            writer.ldq("1", regA);
            registerAllocator.releaseRegister(reg);
            reg = regA;
        }

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTLogical node) {
        debug("LOGICAL");
        tabsInc();

        reg = registerAllocator.getNextAvailableRegister();
        writer.ldq(node.getValue() ? "1" : "0", reg);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTLt node) {
        debug("LT");
        tabsInc();

        boolean real = isReal(node.getNodeLeft(), node.getNodeRight());
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            // lower than <=> not greater or equal
            writer.jsr("@ge_fix");

            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("6", RegisterAllocator.STACK_POINTER);
            writer.not(reg, reg);
            writer.ani(reg, reg, "#0001");
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.cmp(regA, reg);
            writer.blw("4");
            writer.ldq("0", regA);
            writer.bmp("2");
            writer.ldq("1", regA);
            registerAllocator.releaseRegister(reg);
            reg = regA;
        }

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTMinus node) {
        debug("MINUS");
        tabsInc();

        node.getNode().accept(this);
        if (node.getNode().getExprType(tds) == ASTType.REAL) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@neg_fix");

            writer.ldw(regf, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
        } else {
            writer.neg(reg, reg);
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTMul node) {
        debug("MUL");
        tabsInc();

        boolean real = node.getExprType(tds) == ASTType.REAL;
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@mul_fix");

            writer.ldw(regf, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("4", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.mul(regA, reg, reg);
            registerAllocator.releaseRegister(regA);
            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTNeq node) {
        debug("NEQ");
        tabsInc();

        boolean real = isReal(node.getNodeLeft(), node.getNodeRight());
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@eq_fix");

            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("6", RegisterAllocator.STACK_POINTER);
            writer.not(reg, reg);
            writer.ani(reg, reg, "#0001");
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.cmp(regA, reg);
            writer.beq("4");
            writer.ldq("0", regA);
            writer.bmp("2");
            writer.ldq("1", regA);
            registerAllocator.releaseRegister(reg);
            reg = regA;
        }

        if (regf != null) {
            registerAllocator.releaseRegister(regf);
            regf = null;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTNot node) {
        debug("NOT");
        tabsInc();

        node.getNode().accept(this);

        writer.not(reg, reg);
        writer.ani(reg, reg, "#0x0001");

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTNumber node) {
        debug("NUMBER");
        tabsInc();

        if (node.getExprType(tds) == ASTType.INTEGER) {
            reg = registerAllocator.getNextAvailableRegister();

            int i = (int) node.getValue();

            if (i >= -128 && i < 128)
                writer.ldq("" + i, reg);
            else
                writer.ldw(reg, "#" + i);

            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }
        } else {
            reg = registerAllocator.getNextAvailableRegister();

            int i = (int) node.getValue();
            int f = Math.round(node.getValue() % 1f * 65536);

            if (f != 0) {
                regf = registerAllocator.getNextAvailableRegister();

                if (f < 0) {
                    f = 65536 + f;
                    i -= 1;
                }
                if (f >= -128 && f < 128)
                    writer.ldq("" + f, regf);
                else
                    writer.ldw(regf, "#" + f);
            }

            if (i >= -128 && i < 128)
                writer.ldq("" + i, reg);
            else
                writer.ldw(reg, "#" + i);
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTOr node) {
        debug("OR");
        tabsInc();

        node.getNodeLeft().accept(this);
        String regA = reg;

        node.getNodeRight().accept(this);
        registerAllocator.preventRegisterExhaustion();
        writer.or(regA, reg, reg);
        registerAllocator.releaseRegister(regA);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTPlus node) {
        debug("PLUS");
        tabsInc();

        node.getNode().accept(this);

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTPow node) {
        debug("POW");
        tabsInc();

        node.getNodeRight().accept(this);

        ASTType exponentType = node.getNodeRight().getExprType(tds);

        writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");

        if (exponentType.equals(ASTType.REAL)) {
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
            }
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(regf);
        }

        node.getNodeLeft().accept(this);
        registerAllocator.preventRegisterExhaustion();

        ASTType numberType = node.getNodeLeft().getExprType(tds);

        writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
        registerAllocator.releaseRegister(reg);

        if (numberType.equals(ASTType.REAL)) {
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
            }
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(regf);
        }


        //This part is based on the section 3.3.4 of the modified report
        if (exponentType.equals(ASTType.REAL)) {
            writer.jsr("@fcn_realpowreal");
            writer.ldw(reg, RegisterAllocator.RET);
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.ldw(regf, RegisterAllocator.RETF);
            writer.adq("8", RegisterAllocator.STACK_POINTER);


        } else if (numberType.equals(ASTType.INTEGER) && exponentType.equals(ASTType.INTEGER)) {
            writer.jsr("@fcn_intpowint");
            writer.ldw(reg, RegisterAllocator.RET);
            writer.adq("4", RegisterAllocator.STACK_POINTER);
        } else if (numberType.equals(ASTType.REAL) && (exponentType.equals(ASTType.INTEGER))) {
            writer.jsr("@fcn_realpowint");
            writer.ldw(reg, RegisterAllocator.RET);
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.ldw(regf, RegisterAllocator.RETF);
            writer.adq("6", RegisterAllocator.STACK_POINTER);
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTRoot node) {
        debug("ROOT");
        tabsInc();

        writer.label(MAIN);

        for (ASTNode nodeChild : node.getNodes())
            nodeChild.accept(this);

        writer.ldq("0", RegisterAllocator.RET);

        writer.rts();

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTString node) {
        debug("STRING");
        reg = registerAllocator.getNextAvailableRegister();

        String lbl = "str_" + uniqueId(node);

        writer.writeStatic(lbl);
        String str = node.getValue().substring(1, node.getValue().length() - 1);
        writer.writeStatic("STRING \"" + str + "\"");
        writer.ldw(reg, "#" + lbl);
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTSub node) {
        debug("SUB");
        tabsInc();

        boolean real = node.getExprType(tds) == ASTType.REAL;
        String regA = "";

        node.getNodeLeft().accept(this);
        if (real) {
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            registerAllocator.releaseRegister(reg);
            if (regf == null) {
                regf = registerAllocator.getTemporaryRegister();
                writer.ldq("0", regf);
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseTemporaryRegister(regf);
            } else {
                writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(regf);
            }
        } else {
            regA = reg;
        }

        node.getNodeRight().accept(this);
        registerAllocator.preventRegisterExhaustion();
        if (real) {
            if (regf == null) {
                regf = registerAllocator.getNextAvailableRegister();
                writer.ldq("0", regf);
            }
            writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");

            writer.jsr("@neg_fix");
            writer.jsr("@add_fix");

            writer.ldw(regf, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.ldw(reg, "(" + RegisterAllocator.STACK_POINTER + ")+");
            writer.adq("4", RegisterAllocator.STACK_POINTER);
        } else {
            registerAllocator.preventRegisterExhaustion();
            writer.sub(regA, reg, reg);
            registerAllocator.releaseRegister(regA);
            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTVar node) {
        debug("VAR");
        tabsInc();

        reg = registerAllocator.getNextAvailableRegister();
        if (tds.lookup(node.getIdentifier()).getGenre()== Record.Genre.ARRAY ){
            if (!((ArrayRecord)tds.lookup(node.getIdentifier())).isUnknown()) {
                writer.ldw(reg, "#array_" + uniqueHashCode(node.getIdentifier()));
            }else{
                logger.logCritical(new StaticSemanticException("You can not use a raw pointer like that"),node);
            }
            return this;
        }
        switch (node.getExprType(tds)) {
            case BOOLEAN:
            case INTEGER:
                varUtils.getVar(node.getIdentifier(), reg, tds, false);
                if (regf != null) {
                    registerAllocator.releaseRegister(regf);
                    regf = null;
                }
                break;
            case REAL:
                // this register is temporary and will be freed in the subsequent call
                regf = registerAllocator.getNextAvailableRegister();
                varUtils.getVar(node.getIdentifier(), reg, regf, tds);
                break;
            case LABEL:
                writer.ldw(reg, "#lbl_" + uniqueHashCode(node.getIdentifier()));
            default:
                break;
        }

        tabsDec();
        return this;
    }

    @Override
    public AsmCompilerVisitor visit(ASTVarSubscript node) {
        debug("VAR_SUBSCRIPT");
        tabsInc();
        if (node.getExprType(tds) == ASTType.LABEL) {
            // TODO check exhaustion
            String regA = registerAllocator.getNextAvailableRegister();
            registerAllocator.preventRegisterExhaustion();

            SwitchRecord swrec = (SwitchRecord) tds.lookup(node.getIdentifier(), Record.Genre.SWITCH);
            String swlbl = "switch_" + uniqueHashCode(node.getIdentifier());

            writer.ldw(regA, "#" + swlbl);

            node.getExpressions().get(0).accept(this);

            // still possible that the expression is real
            if (regf != null) {
                registerAllocator.releaseRegister(regf);
                regf = null;
            }

            /* Error message if negative */
            String lblMsg = "outbounds_" + uniqueId(node);
            writer.writeStatic(lblMsg);
            writer.writeStatic("STRING \"/!\\ Index out of bounds at line "
                    + node.getSourceLine() + ", character "
                    + node.getExpressions().get(0).getSourceChar() + " \"");
            String lblPositive = "positive_" + uniqueId(node);
            String lblInRange = "inrange_" + uniqueId(node);

            writer.adq("-1", reg);

            // check positive
            writer.jge("#" + lblPositive + " - $ - 2");
            String regC = registerAllocator.getTemporaryRegister();
            writer.ldw(regC, "#" + lblMsg);
            writer.stw(regC, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.jsr("@print_string");
            writer.adq("2", RegisterAllocator.STACK_POINTER);
            writer.jsr("@print_newline");
            registerAllocator.releaseTemporaryRegister(regC);
            writer.jsr("@exit");
            writer.label(lblPositive);

            // check if not too high
            regC = registerAllocator.getTemporaryRegister();
            writer.ldq("" + swrec.getSwitchList().size(), regC);
            writer.cmp(reg, regC);
            writer.jlw("#" + lblInRange + " - $ - 2");
            writer.ldw(regC, "#" + lblMsg);
            writer.stw(regC, "-(" + RegisterAllocator.STACK_POINTER + ")");
            writer.jsr("@print_string");
            writer.adq("2", RegisterAllocator.STACK_POINTER);
            writer.jsr("@print_newline");
            registerAllocator.releaseTemporaryRegister(regC);
            writer.jsr("@exit");
            writer.label(lblInRange);

            writer.add(reg, reg, reg);

            writer.add(regA, reg, regA);
            writer.ldw(reg, "(" + regA + ")");

            registerAllocator.releaseRegister(regA);

        } else {
            boolean isReal = node.getExprType(tds) == ASTType.REAL;
            if (isArrayAssign) {
                // assign number
                if (isReal) {
                    if (regf == null) {
                        regf = registerAllocator.getTemporaryRegister();
                        writer.ldq("0", regf);
                        writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                        registerAllocator.releaseTemporaryRegister(regf);
                    } else {
                        writer.stw(regf, "-(" + RegisterAllocator.STACK_POINTER + ")");
                        registerAllocator.releaseRegister(regf);
                    }
                }
                // store the assign number
                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(reg);
            }
            for (ASTNode nodeChild : node.getExpressions()) {
                nodeChild.accept(this);
                // store the access path
                writer.stw(reg, "-(" + RegisterAllocator.STACK_POINTER + ")");
                registerAllocator.releaseRegister(reg);
            }

            String lbl_error = "modify_error_" + uniqueId(node);
            writer.writeStatic(lbl_error);
            writer.writeStatic("STRING\t\t\t\t\", for the array at line: " + node.getSourceLine() + " character: " + node.getSourceChar() + "\"");


            String lbl_array = "array_" + uniqueHashCode(node.getIdentifier());

            String tempreg = registerAllocator.getTemporaryRegister();
            // store dimensions
            writer.ldq(String.valueOf(node.getExpressions().size()), tempreg);
            writer.stw(tempreg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            // store the error label
            writer.ldw(tempreg, "#" + lbl_error);
            writer.stw(tempreg, "-(" + RegisterAllocator.STACK_POINTER + ")");
            // store the array static address
            if (((ArrayRecord)tds.lookup(node.getIdentifier())).isUnknown()){
                varUtils.getVar(node.getIdentifier(), tempreg, tds, false);
            }else {
                writer.ldw(tempreg, "#" + lbl_array);
            }
            writer.stw(tempreg, "-(" + RegisterAllocator.STACK_POINTER + ")");

            registerAllocator.releaseTemporaryRegister(tempreg);
            if (isArrayAssign) {
                writer.jsr("@set_var_array");
            } else {
                writer.jsr("@get_var_array");
            }
            writer.adq(String.valueOf((3 + node.getExpressions().size() + (isArrayAssign ? (isReal ? 2 : 1) : 0)) * 2), "SP");
            if (!isArrayAssign) {
                reg = registerAllocator.getNextAvailableRegister();
                writer.ldw(reg, RegisterAllocator.RET);
                if (isReal) {
                    regf = registerAllocator.getTemporaryRegister();
                    writer.ldw(regf, RegisterAllocator.RETF);
                }
            }
        }


        tabsDec();
        return this;
    }
}
