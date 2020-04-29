package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTGoto extends ASTNode {
    private ASTNode expression;

    public ASTGoto(int sourceLine, int sourceChar, ASTNode expression) {
        super(sourceLine, sourceChar);

        this.expression = expression;
    }

    public ASTNode getExpression() {
        return expression;
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
