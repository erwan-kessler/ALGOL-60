package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

public abstract class ASTNode {
    private int sourceLine;
    private int sourceChar;

    public ASTNode(int sourceLine, int sourceChar) {
        this.sourceLine = sourceLine;
        this.sourceChar = sourceChar;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public int getSourceChar() {
        return sourceChar;
    }

    public abstract ASTType getExprType(SymbolTable currentSymbolTable);

    public abstract void accept(ASTVisitor visitor);

    public String getName() {
        return this.getClass().getName();
    }
}
