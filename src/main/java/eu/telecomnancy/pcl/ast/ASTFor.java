package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTFor extends ASTNode {
    private ASTNode variable;
    private List<ASTForIterator> iterators;
    private ASTNode statement;

    public ASTFor(int sourceLine, int sourceChar, ASTNode variable,
                  List<ASTForIterator> iterators, ASTNode statement) {
        super(sourceLine, sourceChar);

        this.variable = variable;
        this.iterators = iterators;
        this.statement = statement;
    }

    public ASTNode getVariable() {
        return variable;
    }

    public List<ASTForIterator> getIterators() {
        return iterators;
    }

    public ASTNode getStatement() {
        return statement;
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
