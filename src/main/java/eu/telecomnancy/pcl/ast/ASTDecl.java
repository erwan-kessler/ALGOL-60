package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTDecl extends ASTNode {
    private String identifier;
    private ASTType type;
    private boolean owned;

    public ASTDecl(int sourceLine, int sourceChar, String identifier,
                   ASTType type, boolean owned) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.type = type;
        this.owned = owned;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ASTType getType() {
        return type;
    }

    public boolean isOwned() {
        return owned;
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
