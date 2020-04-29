package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTAssign extends ASTNode {

    private List<ASTNode> designators;
    private ASTNode expression;

    public ASTAssign(int sourceLine, int sourceChar, List<ASTNode> designators,
                     ASTNode expression) {
        super(sourceLine, sourceChar);

        this.designators = designators;
        this.expression = expression;
    }

    public List<ASTNode> getDesignators() {
        return designators;
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
