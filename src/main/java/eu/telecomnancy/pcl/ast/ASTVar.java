package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTVar extends ASTNode {
    private String identifier;
    private ASTType type;

    public ASTVar(int sourceLine, int sourceChar, String identifier) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.type = ASTType.NONE;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        Record record = currentSymbolTable.lookup(identifier);
        if (record != null) {
            return record.getType();
        }
        return ASTType.ERROR;
    }

    public void setExprType(ASTType type) {
        this.type = type;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
