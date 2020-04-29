package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTIff extends ASTNode {
    private ASTNode nodeLeft;
    private ASTNode nodeRight;

    public ASTIff(int sourceLine, int sourceChar, ASTNode nodeLeft,
                  ASTNode nodeRight) {
        super(sourceLine, sourceChar);

        this.nodeLeft = nodeLeft;
        this.nodeRight = nodeRight;
    }

    public ASTNode getNodeLeft() {
        return nodeLeft;
    }

    public ASTNode getNodeRight() {
        return nodeRight;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        //Returns ASTTYPE.BOOLEAN if both memebers are Bool type and error otherwise
        ASTType leftType = getNodeLeft().getExprType(currentSymbolTable);
        ASTType rightType = getNodeRight().getExprType(currentSymbolTable);
        if (leftType.equals(ASTType.BOOLEAN) && rightType.equals(ASTType.BOOLEAN)) {
            return ASTType.BOOLEAN;
        }
        return ASTType.ERROR;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
