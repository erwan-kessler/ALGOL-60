package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTLe extends ASTNode {
    private ASTNode nodeLeft;
    private ASTNode nodeRight;

    public ASTLe(int sourceLine, int sourceChar, ASTNode nodeLeft,
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
        if (leftType.equals(rightType) || (leftType.equals(ASTType.INTEGER) && rightType.equals(ASTType.REAL))
                || ((leftType.equals(ASTType.REAL) && rightType.equals(ASTType.INTEGER)))) {
            return ASTType.BOOLEAN;
        }
        return ASTType.ERROR;
    }


    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
