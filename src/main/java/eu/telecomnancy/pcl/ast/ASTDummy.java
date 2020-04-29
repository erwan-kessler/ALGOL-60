package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTDummy extends ASTNode {
    public ASTDummy(int sourceLine, int sourceChar) {
        super(sourceLine, sourceChar);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.NONE;
    }
}
