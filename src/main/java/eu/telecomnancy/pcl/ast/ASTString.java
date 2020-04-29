package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public class ASTString extends ASTNode {
    private String value;

    public ASTString(int sourceLine, int sourceChar, String value) {
        super(sourceLine, sourceChar);

        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.STRING;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
