package eu.telecomnancy.pcl.assembly.register;

import eu.telecomnancy.pcl.ast.ASTNode;
import eu.telecomnancy.pcl.symbolTable.Record;

public class Vertex {
    private int color = -1;
    private final int name;
    private final ASTNode node;
    private final String level;
    private final Record record;

    Vertex(int name, ASTNode node, String level, Record record) {
        this.name = name;
        this.node = node;
        this.level = level;
        this.record = record;
    }

    public String getLevel() {
        return level;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public ASTNode getNode() {
        return node;
    }

    public int getName() {
        return name;
    }

    public Record getRecord() {
        return record;
    }

    @Override
    public String toString() {
        return String.format("Register R%s representing %s of node %s at line %s at character %s at level %s colored with %s",
                getName(), getRecord().getId(), getNode().getClass(), getNode().getSourceLine(), getNode().getSourceChar(), getLevel(), getColor() + 5);
    }
}