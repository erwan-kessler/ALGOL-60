package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.Records.ArrayRecord;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTVarSubscript extends ASTNode {
    private String identifier;
    private ASTType type;
    private List<ASTNode> expressions;

    public ASTVarSubscript(int sourceLine, int sourceChar, String identifier,
                           List<ASTNode> expressions) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.type = ASTType.NONE;
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

        Record record = currentSymbolTable.lookup(this.identifier);
        if (record == null) {
            return ASTType.ERROR;
        }

        //Checks if all expressions in the subscript are integer and returns the type of the array if its the case
        for (ASTNode node : expressions) {
            ASTType type =node.getExprType(currentSymbolTable);
            if (type != ASTType.INTEGER && type != ASTType.REAL) {
                return ASTType.ERROR;
            }
        }
        if (record.getGenre()== Record.Genre.ARRAY){
            return ((ArrayRecord)record).getActualType();
        }
        return currentSymbolTable.lookup(this.identifier).getType();
    }

    public void setExprType(ASTType type) {
        this.type = type;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
