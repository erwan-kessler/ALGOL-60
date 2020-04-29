package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTRoot extends ASTNode {
    private List<ASTNode> nodes;

    public ASTRoot(int sourceLine, int sourceChar, List<ASTNode> nodes) {
        super(sourceLine, sourceChar);

        this.nodes = nodes;
    }

    public List<ASTNode> getNodes() {
        return nodes;
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
