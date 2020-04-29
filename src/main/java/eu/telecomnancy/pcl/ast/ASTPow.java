package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTPow extends ASTNode {
    private ASTNode nodeLeft;
    private ASTNode nodeRight;

    public ASTPow(int sourceLine, int sourceChar, ASTNode nodeLeft,
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
        ASTType leftType = getNodeLeft().getExprType(currentSymbolTable);
        ASTType rightType = getNodeRight().getExprType(currentSymbolTable);
        if (leftType == ASTType.INTEGER && rightType == ASTType.INTEGER) {
            return ASTType.INTEGER;
        } else if (leftType == ASTType.REAL && (rightType == ASTType.REAL || rightType == ASTType.INTEGER)
                || rightType == ASTType.REAL && (leftType == ASTType.REAL || leftType == ASTType.INTEGER)) {
            return ASTType.REAL;
        }
        return ASTType.ERROR;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
