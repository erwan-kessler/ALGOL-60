package eu.telecomnancy.pcl.semantics;

import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.Records.ArrayRecord;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.HashSet;

import static eu.telecomnancy.pcl.Main.logger;

public class TypeChecker {

    SymbolTable symbolTable;

    public TypeChecker(SymbolTable currentSymboleTable) {
        this.symbolTable = currentSymboleTable;
    }

    public void checkTypeAssign(ASTAssign astAssign) throws StaticSemanticException {

        ASTType assignTypeTarget = ASTType.NONE;
        HashSet<ASTType> typeSet = new HashSet<>();

        //This is where we have to accept int := float := ...
        for (ASTNode nodeChild : astAssign.getDesignators()) {

            // S'il s'agit d'une variable sans subscript
            if (nodeChild instanceof ASTVar) {
                // Si cette variale est en réalité la valeur de retour d'une procédure dans laquelle on se trouve
                if (inProcedureScope(((ASTVar) nodeChild).getIdentifier())) {
                    Record currentRecord = symbolTable.lookup(((ASTVar) nodeChild).getIdentifier(), Record.Genre.PROCEDURE);
                    if (currentRecord != null) {
                        typeSet.add(currentRecord.getType());
                    }
                    // Sinon, s'il s'agit d'une variable normale
                } else {
                    Record currentRecord = symbolTable.lookup(((ASTVar) nodeChild).getIdentifier(), Record.Genre.VARIABLE);
                    if (currentRecord != null) {
                        typeSet.add(currentRecord.getType());
                    }
                }
                // Si la variable est avec subscript
            } else if (nodeChild instanceof ASTVarSubscript) {
                ArrayRecord currentRecord = (ArrayRecord) symbolTable.lookup(((ASTVarSubscript) nodeChild).getIdentifier(), Record.Genre.ARRAY);
                if (currentRecord != null) {
                    typeSet.add(currentRecord.getActualType());
                }
            }

        }
        for (ASTType type : ASTType.values()) {
            if (typeSet.contains(type)) {
                assignTypeTarget = type;
                break; // Not necessary but its useless keep on searching once we found one
            }
        }

        //TODO : match the spec on integer assign to reals

        ASTType expressionType = astAssign.getExpression().getExprType(symbolTable);

        if (assignTypeTarget.equals(ASTType.REAL)) {
            if (!(expressionType.equals(ASTType.REAL) || expressionType.equals(ASTType.INTEGER))) {
                logger.log(new StaticSemanticException("Type missmatch in assignment: " + assignTypeTarget + " expected"), astAssign);
            }
        } else if (assignTypeTarget.equals(ASTType.INTEGER) && expressionType.equals(ASTType.REAL)) {
            if (astAssign.getExpression() instanceof ASTNumber) realToInt((ASTNumber) astAssign.getExpression());
            else {
                // raise a warning ?
            }
        } else if (!expressionType.equals(assignTypeTarget)) {
            logger.log(new StaticSemanticException("Type missmatch in assignment: " + assignTypeTarget + " expected"), astAssign);
        }
    }

    public Record.Genre getNodeGenre(ASTNode node) {
        if (node instanceof ASTVar) {
            Record r = symbolTable.lookup(((ASTVar) node).getIdentifier());
            if (r != null) return r.getGenre();
            logger.log(new StaticSemanticException("Undefined symbol \"" + ((ASTVar) node).getIdentifier() + "\""), node);
            return null;
        } else if (node instanceof ASTVarSubscript) {
            Record r = symbolTable.lookup(((ASTVarSubscript) node).getIdentifier());
            if (r != null) {
                /* S'il s'agit d'un tableau, sachant qu'il possède un subscript,
                 * on sait que l'on obtiendra une valeur, dont on attribue le genre à VARIABLE.
                 * Illustration :
                 *      real array A[1:10, -5:5];
                 *      --> A est du genre ARRAY
                 *      --> A[1,0] est du genre VARIABLE
                 * TODO: Gérer le cas A[1] (doit-on renvoyer une erreur ou bien un ARRAY ?)
                 */
                if (r.getGenre() == Record.Genre.ARRAY) return Record.Genre.VARIABLE;
                else return Record.Genre.LABEL;
            }
            logger.log(new StaticSemanticException("Undefined symbol \"" + ((ASTVarSubscript) node).getIdentifier() + "\""), node);
            return null;
        }
        return Record.Genre.VARIABLE;
    }

    public boolean inProcedureScope(String identifier) {
        SymbolTable.Scope scope = symbolTable.getCurrent();
        while (scope != null) {
            if (scope.getProcedureName() != null && scope.getProcedureName().equals(identifier))
                return true;
            scope = scope.getParent();
        }
        return false;
    }

    public void realToInt(ASTNumber node) {
        float value = node.getValue();
        node.setValue((int) (value + 0.5));
        logger.logWarning("Real value " + value + " has been rounded to integer " + (int) node.getValue(), node);
    }
}
