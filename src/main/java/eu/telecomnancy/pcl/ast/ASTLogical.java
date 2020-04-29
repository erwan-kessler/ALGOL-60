package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTLogical extends ASTNode {
    private boolean value;

    public ASTLogical(int sourceLine, int sourceChar, boolean value) {
        super(sourceLine, sourceChar);

        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.BOOLEAN;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
