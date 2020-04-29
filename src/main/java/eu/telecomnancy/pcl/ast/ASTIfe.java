package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTIfe extends ASTNode {
    private ASTNode condition;
    private ASTNode thenExpression;
    private ASTNode elseExpression;

    public ASTIfe(int sourceLine, int sourceChar, ASTNode condition,
                  ASTNode thenExpression, ASTNode elseExpression) {
        super(sourceLine, sourceChar);

        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = elseExpression;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getThenExpression() {
        return thenExpression;
    }

    public ASTNode getElseExpression() {
        return elseExpression;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        ASTType thenType = thenExpression.getExprType(currentSymbolTable);
        ASTType elseType = elseExpression.getExprType(currentSymbolTable);
        if (thenType == elseType) {
            return thenType;
        } else if (thenType == ASTType.INTEGER && elseType == ASTType.REAL) {
            return ASTType.REAL;
        } else if (thenType == ASTType.REAL && elseType == ASTType.INTEGER) {
            return ASTType.REAL;
        }
        return ASTType.ERROR;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
