package eu.telecomnancy.pcl.assembly.register;

import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static eu.telecomnancy.pcl.Main.debugAll;
import static eu.telecomnancy.pcl.assembly.register.RegisterAllocator.*;

public class GraphColoringRegister implements ASTVisitor<GraphColoringRegister> {
    // this implements graph coloring allocation by Chaitin et al
    // the process is as follow
    /*
        Renumber: discover live range information in the source program.
        Build: build the interference graph.
        Coalesce: merge the live ranges of non-interfering variables related by copy instructions.
        Spill cost: compute the spill cost of each variable. This assesses the impact of mapping a variable to memory on the speed of the final program.
        Simplify: construct an ordering of the nodes in the inferences graph
        Spill Code: insert spill instructions, i.e loads and stores to commute values between registers and memory.
        Select: assign a register to each variable.
     */
    private static final boolean debugGraph = false || debugAll;
    SymbolTable TDS;
    ArrayList<Edge> listEdgeInterf = new ArrayList<>();
    ArrayList<Edge> listEdgePref = new ArrayList<>();
    ArrayList<Vertex> listVertex = new ArrayList<>();
    // Token to know where I am at for the dispatch
    private final HashMap<Integer,String> specialTokens=new HashMap<>();
    private static final String FOR_BODY_START="FOR_BODY_START";
    private static final String FOR_BODY_END="FOR_BODY_END";
    private static final String FOR_BODY_EMPTY="FOR_BODY_EMPTY";
    private static final String FOR_CONDITION_END="FOR_CONDITION_END";
    private static final String FOR_EMPTY ="FOR_EMPTY";

    private int virtualIndex = 0;
    private final Graph graph;

    public GraphColoringRegister(ASTRoot root, SymbolTable TDS) {
        this.TDS = TDS;
        TDS.resetTable();
        this.visit(root);
        Graph graph = new Graph(listVertex, listEdgeInterf, listEdgePref);
        if (debugGraph) {
            System.out.println("Graph will use: "+ FIXED_REGISTERS.length);
            System.out.println("Graph will use: "+ Arrays.toString(FIXED_REGISTERS));
        }
        new GraphUtils(graph, FIXED_REGISTERS.length);
        this.graph = graph;
        if (debugGraph) System.out.println(graph.toString());
        if (FIXED_REGISTERS.length!=0){
            StoreRegisterActions(graph);
        }
    }

    public void addNewNodeToGraph(ASTNode node, String identifier) {
        Record record = TDS.lookup(identifier);
        // only do it for small variables such as integers
        if (record.getSize() == 2) {
            record.addVirtual_register(virtualIndex++);
            Vertex vertexDecl = new Vertex(record.getVirtualRegisters().get(record.getVirtualRegisters().size() - 1), node, TDS.getFormattedName(), record);
            for (Vertex vertex : record.getVertices()) {
                listEdgePref.add(new Edge(vertex, vertexDecl));
            }
            record.addVertex(vertexDecl);
            for (Vertex vertex : listVertex) {
                if (vertex.getLevel().equals(vertexDecl.getLevel()) && vertex.getRecord() != vertexDecl.getRecord()) {
                    listEdgeInterf.add(new Edge(vertex, vertexDecl));
                }
            }
            listVertex.add(vertexDecl);
        }
    }

    public String getLeftoverActions() {
        StringBuilder stringBuffer = new StringBuilder();
        for (Vertex vertex : graph.getVertices()) {
            Record record = vertex.getRecord();
            String action = record.getCurrentAction();
            String register = record.getCurrentRegister();
            while (!action.equals(USE_STACK_ONLY)) {
                stringBuffer.append(action).append("\n");
                action = record.getCurrentAction();
            }
            while (!register.equals(NO_ATTRIBUTED_REGISTER)) {
                stringBuffer.append(register).append("\n");
                register = record.getCurrentRegister();
            }
        }
        return stringBuffer.toString();
    }

    public void StoreRegisterActions(Graph graph) {
        int index=0;
        HashMap<String,Integer> registerInBody=new HashMap<>();
        HashMap<String,Integer> startingIndexInBody=new HashMap<>();
        boolean isForBody=false;
        boolean isForCondition=false;
        for (Vertex vertex : graph.getVertices()) {
            Record record = vertex.getRecord();
            if (specialTokens.containsKey(index)){
                if (specialTokens.get(index).equals(FOR_BODY_START)){
                    isForBody=true;
                }
                if (specialTokens.get(index).equals(FOR_BODY_EMPTY) || specialTokens.get(index).equals(FOR_BODY_END)){
                    isForBody=false;
                    isForCondition=true;
                }
                if (specialTokens.get(index).equals(FOR_CONDITION_END)|| specialTokens.get(index).equals(FOR_EMPTY)){
                    isForCondition=false;
                    registerInBody.clear();
                    startingIndexInBody.clear();
                }
                if (debugGraph) System.out.println(specialTokens.get(index));
            }
            if (isForBody){
                if (!registerInBody.containsKey(record.getId())){
                    registerInBody.put(record.getId(),vertex.getColor());
                }
                if (!startingIndexInBody.containsKey(record.getId())){
                    startingIndexInBody.put(record.getId(),record.getActual_register().size());
                }
            }
            if (debugGraph){
                System.out.println("Body: "+ isForBody+ " Conditon: "+ isForCondition);
                registerInBody.forEach((key, value) -> System.out.println(key + " " + value));
                startingIndexInBody.forEach((key, value) -> System.out.println(key + " " + value));
                System.out.println(vertex.toString());
            }
            if (vertex.getColor() == -1) {
                // no color has been assigned
                record.addRegister(NO_ATTRIBUTED_REGISTER);
                record.addAction(USE_STACK_ONLY);
            } else {
                // a color has been assigned, using the long term registers
                String last_register = record.getLastRegister();
                String current_register = FIXED_REGISTERS[vertex.getColor() - 1];
                record.addRegister(current_register);
                // check if there is a change of register, if so set appropriate action
                if (last_register==null) {
                    // since a value needs to be instantiated before even being used, this is ok
                    // The value of a variable, not declared own, is undefined from entry into
                    // the block in which it is declared until an assignment is made to it.
                    // CLEAN REGISTER
                    record.addAction(GET_REGISTER_FROM_ITSELF);
                } else if (!last_register.equals(current_register)) {
                    // mark last register as dirty
                    // the register needs to then be stored in the stack
                    // DIRTY REGISTER
                    record.modifyLastAction();
                    // mark new one as new register to use
                    // it needs to get the
                    // NEW REGISTER
                    record.addAction(GET_REGISTER_FROM_STACK);
                } else {
                    // mark new one as old register to use
                    // CLEAN REGISTER
                    record.addAction(GET_REGISTER_FROM_ITSELF);
                }
            }
            if (isForCondition){
                Integer actual_register_used_in_body=registerInBody.get(record.getId());
                if (actual_register_used_in_body==null){
                    // we have an index register, // TODO unassociate it

                }
                else if (actual_register_used_in_body!=vertex.getColor()){
                    if (debugGraph) {
                        System.out.println("Wrong register associated in condition compared to body, defaulting to stack");
                        record.getActions().forEach(e-> System.out.print(e+" "));
                        System.out.println();
                        System.out.println("Modifying action index: "+startingIndexInBody.get(record.getId()));

                    }
                    // modify the action of the head register of the body to get the register from the stack
                    vertex.getRecord().modifyAction(GET_REGISTER_FROM_STACK,startingIndexInBody.get(record.getId()));
                    if (debugGraph){
                        record.getActions().forEach(e-> System.out.print(e+" "));
                        System.out.println();
                    }
                    // modify the action of the for condition to store in stack only
                    record.modifyLastAction();
                    // ok here the only bug that could happen is if the actual graph only color one of the condition element
                    // to a different color, then we need to propagate it to the whole condition (likely with a boolean and at
                    // the end of the condition, this should never happen.
                }

            }

            index++;

        }
    }

    public ArrayList<Edge> getListEdgeInterf() {
        return listEdgeInterf;
    }

    public ArrayList<Edge> getListEdgePref() {
        return listEdgePref;
    }

    public ArrayList<Vertex> getListVertex() {
        return listVertex;
    }

    @Override
    public GraphColoringRegister visit(ASTAdd node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTAnd node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTAssign node) {
        // needs 2 registers, one for temporary action when needing to get an external variable
        // One to store the variable
        node.getExpression().accept(this);
        for (ASTNode nodeChild : node.getDesignators()) {
            nodeChild.accept(this);
        }
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTBlock node) {
        TDS.enterScope();
        for (ASTNode nodeChild : node.getDeclarations()) {
            nodeChild.accept(this);
        }
        for (ASTNode nodeChild : node.getStatements()) {
            nodeChild.accept(this);
        }


        TDS.exitScope();
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTDecl node) {
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTDeclArray node) {
        for (ASTDeclArrayBound bound : node.getBounds()) {
            bound.getBoundInf().accept(this);
            bound.getBoundSup().accept(this);
        }
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTDeclFcn node) {
        TDS.enterScope();
        for (String parameter : node.getParameters()) {
        }
        for (String value : node.getValues()) {
        }
        for (ASTDeclFcnSpec spec : node.getSpecs()) {
        }
        node.getStatement().accept(this);
        TDS.exitScope();
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTDeclSwitch node) {
        for (ASTNode nodeChild : node.getExpressions())
            nodeChild.accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTDiv node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTDummy node) {
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTEq node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTFcn node) {
        for (ASTNode nodeChild : node.getParameters())
            nodeChild.accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTFor node) {

        specialTokens.put(listVertex.size(),FOR_BODY_START);
        node.getStatement().accept(this);
        if (specialTokens.containsKey(listVertex.size())){
            specialTokens.put(listVertex.size(),FOR_BODY_EMPTY);
        }else{
            specialTokens.put(listVertex.size(),FOR_BODY_END);
        }
        if (node.getVariable() instanceof ASTVar) {
            addNewNodeToGraph(node, ((ASTVar) node.getVariable()).getIdentifier());
        } else {
            // TODO
        }

        for (ASTForIterator iterator : node.getIterators()) {
            if (iterator instanceof ASTForIteratorExpr) {
                iterator.getExpression().accept(this);
            } else if (iterator instanceof ASTForIteratorWhile) {
                iterator.getExpression().accept(this);
                ((ASTForIteratorWhile) iterator).getCondition().accept(this);
            } else if (iterator instanceof ASTForIteratorStepUntil) {
                iterator.getExpression().accept(this);
                ((ASTForIteratorStepUntil) iterator).getStepExpression().accept(this);
                ((ASTForIteratorStepUntil) iterator).getUntilExpression().accept(this);
            }
        }
        if (specialTokens.containsKey(listVertex.size())){
            specialTokens.put(listVertex.size(), FOR_EMPTY);
        }else{
            specialTokens.put(listVertex.size(),FOR_CONDITION_END);
        }

        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTGe node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTGoto node) {
        node.getExpression().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTGt node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTIdiv node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTIf node) {
        node.getCondition().accept(this);
        node.getThenStatement().accept(this);
        node.getElseStatement().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTIfe node) {
        node.getCondition().accept(this);
        node.getThenExpression().accept(this);
        node.getElseExpression().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTIff node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTImpl node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTLabel node) {
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTLe node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTLogical node) {
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTLt node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTMinus node) {
        node.getNode().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTMul node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTNeq node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTNot node) {
        node.getNode().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTNumber node) {
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTOr node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTPlus node) {
        node.getNode().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTPow node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTRoot node) {
        for (ASTNode nodeChild : node.getNodes()) {
            nodeChild.accept(this);
        }
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTString node) {
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTSub node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTVar node) {
        addNewNodeToGraph(node, node.getIdentifier());
        return this;
    }

    @Override
    public GraphColoringRegister visit(ASTVarSubscript node) {
        for (ASTNode nodeChild : node.getExpressions())
            nodeChild.accept(this);
        return this;
    }
}
