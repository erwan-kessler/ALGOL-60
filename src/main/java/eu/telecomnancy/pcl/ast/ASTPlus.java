package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTPlus extends ASTNode {
    private ASTNode node;

    public ASTPlus(int sourceLine, int sourceChar, ASTNode node) {
        super(sourceLine, sourceChar);

        this.node = node;
    }

    public ASTNode getNode() {
        return node;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        ASTType childType = getNode().getExprType(currentSymbolTable);
        if (childType.equals(ASTType.REAL) || childType.equals(ASTType.INTEGER)) {
            return childType;
        }
        return ASTType.ERROR;
    }


    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
