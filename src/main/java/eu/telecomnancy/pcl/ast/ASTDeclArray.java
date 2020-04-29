package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTDeclArray extends ASTNode {
    private String identifier;
    private ASTType type;
    private boolean owned;
    private List<ASTDeclArrayBound> bounds;

    public ASTDeclArray(int sourceLine, int sourceChar, String identifier,
                        ASTType type, boolean owned,
                        List<ASTDeclArrayBound> bounds) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.type = type;
        this.owned = owned;
        this.bounds = bounds;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.NONE;
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

    public List<ASTDeclArrayBound> getBounds() {
        return bounds;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
