package eu.telecomnancy.pcl.ast;

public class ASTForIteratorWhile extends ASTForIterator {
    private ASTNode condition;

    public ASTForIteratorWhile(ASTNode expression, ASTNode condition) {
        super(expression);

        this.condition = condition;
    }

    public ASTNode getCondition() {
        return condition;
    }
}
