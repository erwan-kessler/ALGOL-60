package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.semantics.StaticSemanticException;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import static eu.telecomnancy.pcl.Main.logger;

public class ASTIdiv extends ASTNode {
    private ASTNode nodeLeft;
    private ASTNode nodeRight;

    public ASTIdiv(int sourceLine, int sourceChar, ASTNode nodeLeft,
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
        }
        return ASTType.ERROR;
    }


    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
