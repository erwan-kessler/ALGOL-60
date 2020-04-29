package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTNumber extends ASTNode {
    private float value;
    private boolean real;

    public ASTNumber(int sourceLine, int sourceChar, float value, boolean real) {
        super(sourceLine, sourceChar);

        this.value = value;
        this.real = real;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value){
        this.value = value;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return this.getExprType();
    }

    public ASTType getExprType() {
        if (real)
            return ASTType.REAL;
        else
            return ASTType.INTEGER;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
