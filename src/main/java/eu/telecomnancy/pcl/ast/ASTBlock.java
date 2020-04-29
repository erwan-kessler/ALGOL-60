package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTBlock extends ASTNode {
    private List<ASTNode> declarations;
    private List<ASTNode> statements;

    public ASTBlock(int sourceLine, int sourceChar, List<ASTNode> declarations,
                    List<ASTNode> statements) {
        super(sourceLine, sourceChar);

        this.declarations = declarations;
        this.statements = statements;
    }

    public List<ASTNode> getDeclarations() {
        return declarations;
    }

    public List<ASTNode> getStatements() {
        return statements;
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
