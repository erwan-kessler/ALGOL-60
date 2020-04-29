package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTNeq extends ASTNode {
    private ASTNode nodeLeft;
    private ASTNode nodeRight;

    public ASTNeq(int sourceLine, int sourceChar, ASTNode nodeLeft,
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
        //Returns ASTTYPE.BOOLEAN if both memebers are the same type and error otherwise
        ASTType leftType = getNodeLeft().getExprType(currentSymbolTable);
        ASTType rightType = getNodeRight().getExprType(currentSymbolTable);
        if (leftType.equals(rightType)) {
            return ASTType.BOOLEAN;
        }
        return ASTType.ERROR;
    }


    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
