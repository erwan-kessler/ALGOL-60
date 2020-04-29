package eu.telecomnancy.pcl.symbolTable.Records;

import eu.telecomnancy.pcl.ast.ASTNode;
import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.symbolTable.Record;

import java.util.ArrayList;

public class ArrayRecord extends Record {
    private int dimension;
    private ArrayList<Integer> strides;
    private ArrayList<ASTNode> bounds;
    private boolean own;
    private boolean unknown;
    /* unknown is set to true if the procedure is a parameter of another procedure,
     * so we can't know any property of this function as it will be treated dynamically */
    private ASTType actualType;

    public ArrayRecord(String id, ASTType type, boolean own) {
        super(id, ASTType.INTEGER, Genre.ARRAY);
        this.own = own;
        strides = new ArrayList<>();
        bounds = new ArrayList<>();
        unknown = false;
        actualType =type;
    }

    public ASTType getActualType() {
        return actualType;
    }

    public boolean getOwn() {
        return own;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public ArrayList<Integer> getStrides() {
        return strides;
    }

    public void setStrides(ArrayList<Integer> strides) {
        this.strides = strides;
    }

    public ArrayList<ASTNode> getBounds() {
        return bounds;
    }

    public void addBound(ASTNode bound) {
        this.bounds.add(bound);
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public boolean isUnknown() {
        return this.unknown;
    }

}
