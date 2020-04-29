package eu.telecomnancy.pcl.ast;

public class ASTForIteratorStepUntil extends ASTForIterator {
    private ASTNode stepExpression;
    private ASTNode untilExpression;

    public ASTForIteratorStepUntil(ASTNode expression, ASTNode stepExpression,
                                   ASTNode untilExpression) {
        super(expression);

        this.stepExpression = stepExpression;
        this.untilExpression = untilExpression;
    }

    public ASTNode getStepExpression() {
        return stepExpression;
    }

    public ASTNode getUntilExpression() {
        return untilExpression;
    }
}
