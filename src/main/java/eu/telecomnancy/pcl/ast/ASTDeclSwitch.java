package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTDeclSwitch extends ASTNode {
    private String identifier;
    private List<ASTNode> expressions;

    public ASTDeclSwitch(int sourceLine, int sourceChar, String identifier,
                         List<ASTNode> expressions) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.expressions = expressions;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<ASTNode> getExpressions() {
        return expressions;
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
