package eu.telecomnancy.pcl.symbolTable;

import eu.telecomnancy.pcl.assembly.register.Vertex;
import eu.telecomnancy.pcl.ast.ASTType;

import java.util.ArrayList;

import static eu.telecomnancy.pcl.assembly.register.RegisterAllocator.*;
import static eu.telecomnancy.pcl.symbolTable.TableCreation.UNKNOWN_OFFSET;

public class Record {
    protected String id;
    protected ASTType type;
    protected Genre genre;

    private int line;
    private int source;
    private int offset = UNKNOWN_OFFSET;
    private int size = 0;

    // this is a trust-based mechanism we don't check we are on the correct node
    private final ArrayList<String> actual_register = new ArrayList<>();
    private final ArrayList<String> actions = new ArrayList<>();
    private final ArrayList<Integer> virtualRegisters = new ArrayList<>();
    private final ArrayList<Vertex> vertices = new ArrayList<>();

    private static final boolean DEBUG = false;

    public Record(String id, ASTType type, Genre genre) {
        this.id = id;
        this.type = type;
        this.genre = genre;
    }

    public ArrayList<Vertex> getVertices() {
        return vertices;
    }

    public ArrayList<Integer> getVirtualRegisters() {
        return virtualRegisters;
    }

    public void addVirtual_register(int virtual_register) {
        this.virtualRegisters.add(virtual_register);
    }

    public void addVertex(Vertex vertex) {
        this.vertices.add(vertex);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getCurrentRegister() {
        if (actual_register.isEmpty()) {
            if (DEBUG && FIXED_REGISTERS.length!=0) System.out.println("Not possible register was requested too many times");
            return NO_ATTRIBUTED_REGISTER;
        }
        return actual_register.remove(0);
    }

    public String getLastRegister() {
        return actual_register.isEmpty()?null:actual_register.get(actual_register.size()-1);
    }

    public ArrayList<String> getActual_register() {
        return actual_register;
    }

    public void modifyAction(String action, int index) {
        actions.set(index,action);
    }
    public void modifyLastAction() {
        String last_action=actions.get(actions.size()-1);
        if (last_action.equals(GET_REGISTER_FROM_ITSELF)){
            actions.set(actions.size()-1,STORE_REGISTER_IN_STACK_GET_FROM_REGISTRY);
        }else if (last_action.equals(GET_REGISTER_FROM_STACK)){
            actions.set(actions.size()-1,STORE_REGISTER_IN_STACK_GET_FROM_STACK);
        }
    }

    public ArrayList<String> getActions() {
        return actions;
    }

    public String getCurrentAction() {
        if (actions.isEmpty()) {
            if (DEBUG && FIXED_REGISTERS.length!=0) System.out.println("Not possible action was requested too many times");
            return USE_STACK_ONLY;
        }
        return actions.remove(0);
    }
    public String getFirstAction(){
        if (actions.isEmpty()) {
            if (DEBUG && FIXED_REGISTERS.length!=0) System.out.println("Not possible action was requested too many times");
            return USE_STACK_ONLY;
        }
        return actions.get(0);
    }

    public String getFirstRegister(){
        if (actual_register.isEmpty()) {
            if (DEBUG && FIXED_REGISTERS.length!=0) System.out.println("Not possible register was requested too many times");
            return NO_ATTRIBUTED_REGISTER;
        }
        return actual_register.get(0);
    }
    public void addAction(String action){
        actions.add(action);
    }

    public void addRegister(String register) {
        actual_register.add(register);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public String getId() {
        return this.id;
    }

    public ASTType getType() {
        return this.type;
    }

    public Genre getGenre() {
        return genre;
    }

    @Override
    public String toString() {
        return "Record: " + id + " : " + type + " : " + genre;
    }

    public boolean getOwn() {
        return false;
    }

    public enum Genre {
        PROCEDURE("procedure"),
        ARRAY("array"),
        VARIABLE("variable"),
        SWITCH("switch"),
        LABEL("label");

        private final String text;

        Genre(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
