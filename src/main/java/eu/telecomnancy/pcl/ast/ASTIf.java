package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTIf extends ASTNode {
    private ASTNode condition;
    private ASTNode thenStatement;
    private ASTNode elseStatement;

    public ASTIf(int sourceLine, int sourceChar, ASTNode condition,
                 ASTNode thenStatement, ASTNode elseStatement) {
        super(sourceLine, sourceChar);

        this.condition = condition;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getThenStatement() {
        return thenStatement;
    }

    public ASTNode getElseStatement() {
        return elseStatement;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.NONE;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
