package eu.telecomnancy.pcl.ast;

public abstract class ASTForIterator {
    private ASTNode expression;

    public ASTForIterator(ASTNode expression) {
        this.expression = expression;
    }

    public ASTNode getExpression() {
        return expression;
    }
}
