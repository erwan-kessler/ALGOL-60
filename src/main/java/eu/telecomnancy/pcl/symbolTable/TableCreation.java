package eu.telecomnancy.pcl.symbolTable;

import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.semantics.StaticSemanticException;
import eu.telecomnancy.pcl.symbolTable.Records.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static eu.telecomnancy.pcl.Main.logger;

public class TableCreation implements ASTVisitor<TableCreation> {

    public static final int OFFSET_SIZE = 2;
    public static final int UNKNOWN_OFFSET = 1;

    private final SymbolTable symbolTable;
    private int currentCurImbrication;
    private final List<Integer> currentImbrications;
    private boolean debug = false;
    private int offset;

    public TableCreation() {
        this.symbolTable = new SymbolTable();
        currentCurImbrication = -1;
        currentImbrications = new ArrayList<>();
    }

    public void setSize(Record record) {
        switch (record.getType()) {
            case REAL:
                record.setSize(OFFSET_SIZE * 2);
                break;
            case STRING: // a string here can only be a reference to the actual block so we will put a fixed address
            case LABEL: // should a label really have a size?
            case BOOLEAN: //Bool is the same as an int
            case INTEGER:
                record.setSize(OFFSET_SIZE);
                break;
            default:
                break;
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void updateImbricationPlus() {
        if (currentImbrications.size() == currentCurImbrication) {
            currentImbrications.add(0);
        } else if (currentImbrications.size() == currentCurImbrication + 1) {
            currentImbrications.set(currentCurImbrication, currentImbrications.get(currentCurImbrication) + 1);
        } else {
            System.out.println("ERROR" + currentCurImbrication);
            System.out.println("ERROR" + currentImbrications.toString());
        }

    }

    public void updateImbricationMinus() {
        if (currentImbrications.size() - 2 == currentCurImbrication) {
            //we are going back to the parent
        } else if (currentImbrications.size() - 3 == currentCurImbrication) {
            //we are going to the grand parent so we cannot take the parent route again
            currentImbrications.remove(currentCurImbrication + 2);
        } else {
            System.out.println("ERROR" + currentCurImbrication);
            System.out.println("ERROR" + currentImbrications.toString());
        }
    }

    public SymbolTable create(ASTRoot astRoot) {
        this.visit(astRoot);
        this.symbolTable.resetTable();
        return this.symbolTable;
    }

    @Override
    public TableCreation visit(ASTAdd node) {
        if (debug) {
            System.out.println("Encountered Add Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTAnd node) {
        if (debug) {
            System.out.println("Encountered And Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTAssign node) {
        if (debug) {
            System.out.println("Encountered Assign Node, doing nothing");
        }
        for (ASTNode nodeChild : node.getDesignators())
            nodeChild.accept(this);
        node.getExpression().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTBlock node) {
        symbolTable.enterScope();
        offset = -2;
        currentCurImbrication++;
        updateImbricationPlus();
        if (debug) {
            System.out.println("Adding block with imbrication level: " + currentImbrications.toString() + " level: " + currentCurImbrication);
        }
        symbolTable.setCurrentScopeNameAndType(currentImbrications, ScopeTypes.BLOCK.toString());
        for (ASTNode nodeChild : node.getDeclarations())
            nodeChild.accept(this);
        for (ASTNode nodeChild : node.getStatements())
            nodeChild.accept(this);

        symbolTable.exitScope();
        currentCurImbrication--;
        updateImbricationMinus();
        return this;
    }

    @Override
    public TableCreation visit(ASTDecl node) {
        // a declaration can not be a string so size cannot call with type STRING here
        if (debug) {
            System.out.println("Adding declaration with name" + node.getIdentifier());
        }
        VariableRecord record = new VariableRecord(node.getIdentifier(), node.getType(), node.isOwned());
        record.setLine(node.getSourceLine());
        record.setSource(node.getSourceChar());
        record.setOffset(offset);
        this.setSize(record);
        offset -= record.getSize();
        symbolTable.put(node.getIdentifier(), record);
        return this;
    }

    @Override
    public TableCreation visit(ASTDeclArray node) {
        if (debug) {
            System.out.println("Adding array declaration with name" + node.getIdentifier());
        }
        ArrayRecord record = new ArrayRecord(node.getIdentifier(), node.getType(), node.isOwned());
        record.setDimension(node.getBounds().size());
        for (ASTDeclArrayBound bound : node.getBounds()) {
            if (debug) {
                System.out.println("Adding array bound pairs to previous array");
            }
            bound.getBoundInf().accept(this);
            record.addBound(bound.getBoundInf());
            bound.getBoundSup().accept(this);
            record.addBound(bound.getBoundSup());
        }
        record.setLine(node.getSourceLine());
        record.setSource(node.getSourceChar());
        // we dont store any offset since those are dynamics
        symbolTable.put(node.getIdentifier(), record);
        return this;
    }

    @Override
    public TableCreation visit(ASTDeclFcn node) {
        ProcedureRecord record = new ProcedureRecord(node.getIdentifier(), node.getType());
        record.addParameters(node.getParameters());
        record.addValues(node.getValues());
        record.addSpecs(node.getSpecs());
        record.setLine(node.getSourceLine());
        record.setSource(node.getSourceChar());
        symbolTable.put(node.getIdentifier(), record);

        symbolTable.enterScope();
        offset = 6;
        currentCurImbrication++;
        updateImbricationPlus();
        if (debug)
            System.out.println("Adding function declaration with imbrication level: " + currentImbrications.toString() + " level: " + currentCurImbrication);
        symbolTable.setCurrentScopeNameAndType(currentImbrications, ScopeTypes.PROCEDURE.toString());
        symbolTable.getCurrent().setProcedureName(node.getIdentifier());
        for (String param : node.getParameters()) {
            int count = 0;
            for (ASTDeclFcnSpec spec : node.getSpecs()) {
                if (spec.getIdentifier().equals(param)) {
                    count++;
                    Record specRecord;
                    switch (spec.getType().toRecordGenre()) {
                        case PROCEDURE:
                            ProcedureRecord pr = new ProcedureRecord(spec.getIdentifier(), spec.getType().toASTType());
                            pr.setUnknown(true);
                            specRecord = pr;
                            break;
                        case VARIABLE:
                            specRecord = new VariableRecord(spec.getIdentifier(), spec.getType().toASTType(), false);
                            specRecord.setOffset(offset);
                            this.setSize(specRecord);
                            offset += specRecord.getSize();
                            break;
                        case ARRAY:
                            specRecord = new ArrayRecord(spec.getIdentifier(), spec.getType().toASTType(), false);
                            ((ArrayRecord) specRecord).setUnknown(true);
                            specRecord.setOffset(offset);
                            specRecord.setSize(OFFSET_SIZE);  // actually needs to be OFFSET_SIZE but for compatibility we let it as so
                            offset += specRecord.getSize();
                            break;
                        case SWITCH:
                            specRecord = new SwitchRecord(spec.getIdentifier(), spec.getType().toASTType());
                            specRecord.setOffset(offset);
                            this.setSize(specRecord);
                            offset += specRecord.getSize();
                            break;
                        case LABEL:
                            specRecord = new LabelRecord(spec.getIdentifier(), spec.getType().toASTType());
                            specRecord.setOffset(offset);
                            this.setSize(specRecord);
                            offset += specRecord.getSize();
                            break;
                        default:
                            specRecord = new Record(spec.getIdentifier(), spec.getType().toASTType(), spec.getType().toRecordGenre());
                    }
                    specRecord.setSource(node.getSourceChar());
                    specRecord.setLine(node.getSourceLine());
                    symbolTable.put(spec.getIdentifier(), specRecord);

                }
            }
            if (count > 1) {
                logger.log(new StaticSemanticException("One param has match more than one spec, redefinition is not recommended"),node);
            }
        }
        node.getStatement().accept(this);

        symbolTable.exitScope();
        currentCurImbrication--;
        updateImbricationMinus();
        return this;
    }

    @Override
    public TableCreation visit(ASTDeclSwitch node) {
        if (debug) {
            System.out.println("Encountered Switch Node, adding it to TDS");
        }
        SwitchRecord switchRecord = new SwitchRecord(node.getIdentifier(), ASTType.LABEL);
        for (ASTNode nodeChild : node.getExpressions()) {
            nodeChild.accept(this);
        }
        switchRecord.addSpecs(node.getExpressions());
        switchRecord.setSource(node.getSourceChar());
        switchRecord.setLine(node.getSourceLine());
        switchRecord.setOffset(offset);
        this.setSize(switchRecord);
        offset -= switchRecord.getSize();
        symbolTable.put(node.getIdentifier(), switchRecord);
        return this;
    }

    @Override
    public TableCreation visit(ASTDiv node) {
        if (debug) {
            System.out.println("Encountered Div Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTDummy node) {
        if (debug) {
            System.out.println("Encountered Dummy Node, doing nothing");
        }
        return this;
    }

    @Override
    public TableCreation visit(ASTEq node) {
        if (debug) {
            System.out.println("Encountered Eq Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTFcn node) {
        if (debug) {
            System.out.println("Encountered Function Node, doing nothing");
        }
        for (ASTNode nodeChild : node.getParameters()) {
            nodeChild.accept(this);
        }
        return this;
    }

    @Override
    public TableCreation visit(ASTFor node) {
        for (ASTForIterator iterator : node.getIterators()) {
            if (iterator instanceof ASTForIteratorExpr) {
                if (debug) {
                    System.out.println("Encountered Expr Node, doing nothing");
                }
                iterator.getExpression().accept(this);
            } else if (iterator instanceof ASTForIteratorWhile) {
                if (debug) {
                    System.out.println("Encountered While Node, doing nothing");
                }
                iterator.getExpression().accept(this);
                ((ASTForIteratorWhile) iterator).getCondition().accept(this);
            } else if (iterator instanceof ASTForIteratorStepUntil) {
                if (debug) {
                    System.out.println("Encountered Step Node, doing nothing");
                }
                iterator.getExpression().accept(this);
                ((ASTForIteratorStepUntil) iterator).getStepExpression().accept(this);
                ((ASTForIteratorStepUntil) iterator).getUntilExpression().accept(this);
            }
        }
        node.getStatement().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTGe node) {
        if (debug) {
            System.out.println("Encountered Greater or Equal Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTGoto node) {
        if (debug) {
            System.out.println("Encountered Goto Node, doing nothing");
        }
        node.getExpression().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTGt node) {
        if (debug) {
            System.out.println("Encountered Greater Than Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTIdiv node) {
        if (debug) {
            System.out.println("Encountered Idiv Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTIf node) {
        if (debug) {
            System.out.println("Encountered If Node, doing nothing");
        }
        node.getCondition().accept(this);
        node.getThenStatement().accept(this);
        node.getElseStatement().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTIfe node) {
        if (debug) {
            System.out.println("Encountered IFE Node, doing nothing");
        }
        node.getCondition().accept(this);
        node.getThenExpression().accept(this);
        node.getElseExpression().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTIff node) {
        if (debug) {
            System.out.println("Encountered IFF Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTImpl node) {
        if (debug) {
            System.out.println("Encountered Impl Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTLabel node) {
        if (debug) {
            System.out.println("Adding label with name: " + node.getIdentifier());
        }
        LabelRecord record = new LabelRecord(node.getIdentifier(), ASTType.LABEL);
        record.setLine(node.getSourceLine());
        record.setSource(node.getSourceChar());
        record.setOffset(offset);
        this.setSize(record);
        offset -= record.getSize();
        symbolTable.put(node.getIdentifier(), record);
        return this;
    }

    @Override
    public TableCreation visit(ASTLe node) {
        if (debug) {
            System.out.println("Encountered Le Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTLogical node) {
        if (debug) {
            System.out.println("Encountered Logical Node, doing nothing");
        }
        return this;
    }

    @Override
    public TableCreation visit(ASTLt node) {
        if (debug) {
            System.out.println("Encountered Lt Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTMinus node) {
        if (debug) {
            System.out.println("Encountered Minus Node, doing nothing");
        }
        node.getNode().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTMul node) {
        if (debug) {
            System.out.println("Encountered Multiplication Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTNeq node) {
        if (debug) {
            System.out.println("Encountered Neq Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTNot node) {
        if (debug) {
            System.out.println("Encountered Not Node, doing nothing");
        }
        node.getNode().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTNumber node) {
        if (debug) {
            System.out.println("Encountered Number Node, doing nothing");
        }

        return this;
    }

    @Override
    public TableCreation visit(ASTOr node) {
        if (debug) {
            System.out.println("Encountered Or Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTPlus node) {
        if (debug) {
            System.out.println("Encountered Plus Node, doing nothing");
        }
        node.getNode().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTPow node) {
        if (debug) {
            System.out.println("Encountered Pow Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTRoot node) {
        currentCurImbrication++;
        updateImbricationPlus();
        this.symbolTable.setCurrentScopeNameAndType(currentImbrications, ScopeTypes.PROGRAM.toString());
        for (ASTNode nodeChild : node.getNodes()) {
            nodeChild.accept(this);
        }
        ProcedureRecord heapInfo = new ProcedureRecord("heapInfo", ASTType.NONE);
        this.symbolTable.put("heapInfo", heapInfo);
        addIOProcedure("outinteger", ASTDeclFcnSpecType.INTEGER);
        addIOProcedure("outreal", ASTDeclFcnSpecType.REAL);
        addIOProcedure("outstring", ASTDeclFcnSpecType.STRING);

        ProcedureRecord outchar = new ProcedureRecord("outchar", ASTType.NONE);
        outchar.addSpec(new ASTDeclFcnSpec("fd", ASTDeclFcnSpecType.INTEGER, true));
        outchar.addSpec(new ASTDeclFcnSpec("io", ASTDeclFcnSpecType.STRING, true));
        outchar.addSpec(new ASTDeclFcnSpec("n", ASTDeclFcnSpecType.INTEGER, true));
        outchar.addParameter("fd");
        outchar.addParameter("io");
        outchar.addParameter("n");
        this.symbolTable.put("outchar", outchar);

        // To DEFINE
        ProcedureRecord ln = new ProcedureRecord("log", ASTType.REAL);
        ln.addSpec(new ASTDeclFcnSpec("E", ASTDeclFcnSpecType.REAL, true));
        ln.addParameter("E");
        this.symbolTable.put("log", ln);
        ProcedureRecord exp = new ProcedureRecord("exp", ASTType.REAL);
        exp.addSpec(new ASTDeclFcnSpec("E", ASTDeclFcnSpecType.REAL, true));
        exp.addParameter("E");
        this.symbolTable.put("exp", exp);

        //instring doesnt exist at all as we can not store a string anywhere and cannot call a procedure as a lambda
        currentCurImbrication--;
        updateImbricationMinus();
        return this;
    }

    public void addIOProcedure(String name, ASTDeclFcnSpecType paramType) {
        ProcedureRecord io = new ProcedureRecord(name, ASTType.NONE);
        io.addSpec(new ASTDeclFcnSpec("fd", ASTDeclFcnSpecType.INTEGER, true));
        io.addSpec(new ASTDeclFcnSpec("io", paramType, true));
        io.addParameter("fd");
        io.addParameter("io");
        this.symbolTable.put(name, io);
    }

    @Override
    public TableCreation visit(ASTString node) {
        if (debug) {
            System.out.println("Encountered String Node, storing a variable with a fixed size");
        }

        VariableRecord record = new VariableRecord("string_" + symbolTable.getCurrentScopeName().subList(0, symbolTable.getCurrentScopeName().size() - 1).stream().map(Object::toString).collect(Collectors.joining("_")) + "_" + symbolTable.getCurrent().incString++, ASTType.STRING, true);
        record.setLine(node.getSourceLine());
        record.setSource(node.getSourceChar());
        record.setOffset(offset);
        record.setSize(node.getValue().length() + node.getValue().length() % 2);
        offset -= record.getSize();
        symbolTable.put("STRINGSTORAGE_" + node.hashCode(), record);
        return this;
    }

    @Override
    public TableCreation visit(ASTSub node) {
        if (debug) {
            System.out.println("Encountered Sub Node, doing nothing");
        }
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public TableCreation visit(ASTVar node) {
        if (debug) {
            System.out.println("Encountered Var Node, doing nothing");
        }
        return this;
    }

    @Override
    public TableCreation visit(ASTVarSubscript node) {
        if (debug) {
            System.out.println("Encountered Var Subscript Node, doing nothing");
        }
        for (ASTNode nodeChild : node.getExpressions()) {
            nodeChild.accept(this);
        }
        return this;
    }

    public enum ScopeTypes {
        PROGRAM("program"),
        BLOCK("block"),
        PROCEDURE("procedure");

        private final String text;

        ScopeTypes(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
