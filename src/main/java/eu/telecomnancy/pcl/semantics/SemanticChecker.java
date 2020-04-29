package eu.telecomnancy.pcl.semantics;

import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.Records.ArrayRecord;
import eu.telecomnancy.pcl.symbolTable.Records.ProcedureRecord;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import static eu.telecomnancy.pcl.Main.logger;

public class SemanticChecker implements ASTVisitor<SemanticChecker> {
    private final SymbolTable symbolTable;
    private final TypeChecker typeChecker;
    private final DefinitionChecker definitionChecker;
    private boolean arrayTracker = false;

    public SemanticChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        typeChecker = new TypeChecker(symbolTable);
        definitionChecker = new DefinitionChecker();

        symbolTable.resetTable();
    }

    @Override
    public SemanticChecker visit(ASTAdd node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTAnd node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTAssign node) {
        try {
            for (ASTNode nodeChild : node.getDesignators())
                nodeChild.accept(this);
            node.getExpression().accept(this);
            typeChecker.checkTypeAssign(node);
        } catch (StaticSemanticException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public SemanticChecker visit(ASTBlock node) {
        symbolTable.enterScope();
        for (ASTNode nodeChild : node.getDeclarations())
            nodeChild.accept(this);
        for (ASTNode nodeChild : node.getStatements())
            nodeChild.accept(this);
        symbolTable.exitScope();
        return this;
    }

    @Override
    public SemanticChecker visit(ASTDecl node) {
        return this;
    }

    @Override
    public SemanticChecker visit(ASTDeclArray node) {
        arrayTracker = true;
        for (ASTDeclArrayBound bound : node.getBounds()) {
            bound.getBoundInf().accept(this);
            bound.getBoundSup().accept(this);
        }
        arrayTracker = false;
        return this;
    }

    @Override
    public SemanticChecker visit(ASTDeclFcn node) {
        symbolTable.enterScope();
        for (String value : node.getValues()) {
            if (node.getParameters().stream().noneMatch(str -> str.equals(value)))
                logger.log(new StaticSemanticException("Value \"" + value + "\" is not in parameters"), node);
        }
        for (ASTDeclFcnSpec spec : node.getSpecs()) {
            if (node.getParameters().stream().noneMatch(str -> str.equals(spec.getIdentifier()))) {
                logger.log(new StaticSemanticException("Variable \"" + spec.getIdentifier()
                        + "\" is not in parameters"), node);
            }
        }
        node.getStatement().accept(this);
        symbolTable.exitScope();
        return this;
    }

    @Override
    public SemanticChecker visit(ASTDeclSwitch node) {
        for (ASTNode nodeChild : node.getExpressions()) {
            if (!(nodeChild instanceof ASTNumber || nodeChild instanceof ASTVar)) {
                // Les cas ASTNumber et ASTVar sont vérifiés par le definitionChecker
                nodeChild.accept(this);
            }
            definitionChecker.checkLabel(symbolTable, nodeChild);
        }
        return this;
    }

    @Override
    public SemanticChecker visit(ASTDiv node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTDummy node) {
        return this;
    }

    @Override
    public SemanticChecker visit(ASTEq node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTFcn node) {
        ProcedureRecord r = (ProcedureRecord) symbolTable.lookup(node.getIdentifier(), Record.Genre.PROCEDURE);
        /* Checking if procedure exists */
        if (r == null) {
            logger.log(new StaticSemanticException("Procedure \"" + node.getIdentifier() + "\" is not defined"), node);
        }
        /* Counting parameters */
        else if (!r.isUnknown() && r.getParameters().size() != node.getParameters().size()) {
            logger.log(new StaticSemanticException("Procedure \"" + node.getIdentifier() +
                    "\" takes exactly " + r.getParameters().size() + " argument(s)"), node);
        }
        /* Checking arguments types */
        else {
            for (int i = 0; i < r.getParameters().size(); i++) {
                ASTNode nodeChild = node.getParameters().get(i);
                /* Searching for the corresponding specification */
                ASTDeclFcnSpec spec = null;
                for (ASTDeclFcnSpec s : r.getSpecifications()) {
                    if (s.getIdentifier().equals(r.getParameters().get(i))) spec = s;
                }
                if (spec == null) continue;
                ASTDeclFcnSpecType expected = spec.getType();
                /* Checking genre */
                Record.Genre foundGenre = typeChecker.getNodeGenre(nodeChild);
                if (foundGenre != expected.toRecordGenre()) {
                    logger.log(new StaticSemanticException(expected.toRecordGenre() + " expected at parameter " + (i + 1)), nodeChild);
                }
                if (foundGenre == Record.Genre.VARIABLE) {
                    if (spec.isValue()) nodeChild.accept(this);
                    else if (!(nodeChild instanceof ASTVar
                            || nodeChild instanceof ASTVarSubscript
                            || nodeChild instanceof ASTString))
                        logger.log(new StaticSemanticException("Parameter " + (i + 1) + " must not be a value"), nodeChild);
                }
                /* Checking type */
                ASTType foundType = nodeChild.getExprType(symbolTable);
                if (expected.toASTType() == ASTType.INTEGER && foundType == ASTType.REAL) {
                    if (nodeChild instanceof ASTNumber) typeChecker.realToInt((ASTNumber) nodeChild);
                    else {
                        logger.logWarning("Real value will be rounded to integer", nodeChild);
                    }
                } else if (expected.toASTType() == ASTType.REAL && foundType == ASTType.INTEGER) {
                    // raise a warning ? (do nothing)
                } else if (foundType != expected.toASTType()) {
                    logger.log(new StaticSemanticException("Type " + expected.toASTType() + " expected at parameter "
                            + (i + 1)), nodeChild);
                }
            }
        }
        return this;
    }

    @Override
    public SemanticChecker visit(ASTFor node) {
        for (ASTForIterator iterator : node.getIterators()) {
            if (node.getVariable() instanceof ASTVar) {
                node.getVariable().accept(this);
            }
            if (iterator instanceof ASTForIteratorExpr) {
                iterator.getExpression().accept(this);
            } else if (iterator instanceof ASTForIteratorWhile) {
                iterator.getExpression().accept(this);
                ((ASTForIteratorWhile) iterator).getCondition().accept(this);
            } else if (iterator instanceof ASTForIteratorStepUntil) {
                iterator.getExpression().accept(this);
                ((ASTForIteratorStepUntil) iterator).getStepExpression()
                        .accept(this);
                ((ASTForIteratorStepUntil) iterator).getUntilExpression()
                        .accept(this);
            }
        }
        node.getStatement().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTGe node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTGoto node) {
        // Les labels définis dans des scopes parents sont valables (section 4.3.4)
        node.getExpression().accept(this);
        definitionChecker.checkLabel(symbolTable, node.getExpression());
        return this;
    }

    @Override
    public SemanticChecker visit(ASTGt node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTIdiv node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTIf node) {
        if (node.getCondition().getExprType(symbolTable) != ASTType.BOOLEAN) {
            logger.log(new StaticSemanticException("Boolean expression expected for if statement but " +
                    node.getCondition().getExprType(symbolTable) + " found"), node.getCondition());
        }
        if (node.getExprType(symbolTable) == ASTType.ERROR) {
            if (node.getThenStatement().getExprType(symbolTable) != ASTType.ERROR) {
                logger.log(new StaticSemanticException(node.getThenStatement().getExprType(symbolTable) +
                        " expression found for else statement but " +
                        node.getThenStatement().getExprType(symbolTable) + " expected"), node.getElseStatement());
            } else {
                logger.log(new StaticSemanticException("Error in 'then' expression"), node.getThenStatement());
            }
        }
        node.getCondition().accept(this);
        node.getThenStatement().accept(this);
        node.getElseStatement().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTIfe node) {
        if (node.getCondition().getExprType(symbolTable) != ASTType.BOOLEAN) {
            logger.log(new StaticSemanticException("Boolean expression expected for if statement but " +
                    node.getCondition().getExprType(symbolTable) + " found"), node.getCondition());
        }
        if (node.getExprType(symbolTable) == ASTType.ERROR) {
            if (node.getThenExpression().getExprType(symbolTable) != ASTType.ERROR) {
                logger.log(new StaticSemanticException(node.getElseExpression().getExprType(symbolTable) +
                        " expression found for else statement but " +
                        node.getThenExpression().getExprType(symbolTable) + " expected"), node.getElseExpression());
            } else {
                logger.log(new StaticSemanticException("Error in 'then' expression"), node.getThenExpression());
            }
        }
        node.getCondition().accept(this);
        node.getThenExpression().accept(this);
        node.getElseExpression().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTIff node) {

        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTImpl node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTLabel node) {
        return this;
    }

    @Override
    public SemanticChecker visit(ASTLe node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTLogical node) {
        return this;
    }

    @Override
    public SemanticChecker visit(ASTLt node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTMinus node) {
        node.getNode().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTMul node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTNeq node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTNot node) {
        node.getNode().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTNumber node) {
        return this;
    }

    @Override
    public SemanticChecker visit(ASTOr node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTPlus node) {
        node.getNode().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTPow node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTRoot node) {
        for (ASTNode nodeChild : node.getNodes())
            nodeChild.accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTString node) {
        return this;
    }

    @Override
    public SemanticChecker visit(ASTSub node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        return this;
    }

    @Override
    public SemanticChecker visit(ASTVar node) {
        /* Searching for the variable type in the symbol table : */
        Record r = symbolTable.lookup(node.getIdentifier());
        if (arrayTracker) {
            if ((int) symbolTable.lookupCount(node.getIdentifier())[1] == 0) {
                logger.log(new StaticSemanticException("Array can not use a local defined variable as a bound"), node);
            }
        }
        // if it's a label then we make sure it's defined into the current scope
        if (r != null && r.getGenre() == Record.Genre.LABEL)
            r = symbolTable.lookup(node.getIdentifier(), Record.Genre.LABEL);
        if (r == null) {
            logger.log(new StaticSemanticException("Variable \"" + node.getIdentifier() + "\" not defined"), node);
        } else if (r.getGenre() != Record.Genre.VARIABLE && !typeChecker.inProcedureScope(node.getIdentifier())) {
            // S'il ne s'agit pas d'une variable et qu'on ne se trouve pas dans le scope d'une procédure du même nom
            if (r.getGenre() == Record.Genre.ARRAY || r.getGenre() == Record.Genre.SWITCH)
                logger.log(new StaticSemanticException("Subscript is missing for \"" + node.getIdentifier() + "\""), node);
                // S'il s'agit d'une procédure devant posséder des paramètres
            else if (r.getGenre() == Record.Genre.PROCEDURE) {
                if (((ProcedureRecord) r).getParameters().size() > 0)
                    logger.log(new StaticSemanticException("Procedure \"" + node.getIdentifier() + "\" takes exactly " +
                            ((ProcedureRecord) r).getParameters().size() + " arguments"), node);
            }
            // Cas inconnus
            else if (r.getGenre() != Record.Genre.LABEL)
                logger.log(new StaticSemanticException("\"" + node.getIdentifier() + "\" is not a variable"), node);
        } else {
            node.setExprType(symbolTable.lookup(node.getIdentifier()).getType());
        }
        return this;
    }

    @Override
    public SemanticChecker visit(ASTVarSubscript node) {
        for (ASTNode nodeChild : node.getExpressions()) {
            nodeChild.accept(this);
            ASTType type = nodeChild.getExprType(symbolTable);
            if (type == ASTType.REAL) {
                // S'il s'agit d'un nombre, on effectue un arrondi
                if (nodeChild instanceof ASTNumber) typeChecker.realToInt((ASTNumber) nodeChild);
                else logger.logWarning("Real value will be rounded to integer", nodeChild);
            } else if (type != ASTType.INTEGER) {
                logger.log(new StaticSemanticException("Number expected in subscript"), nodeChild);
            }
        }
        Record r = symbolTable.lookup(node.getIdentifier());
        // if it's a switch then we make sure that it's defined inside the current scope
        if (r != null && r.getGenre() == Record.Genre.SWITCH)
            r = symbolTable.lookup(node.getIdentifier(), Record.Genre.SWITCH);
        if (r == null) {
            if (node.getExpressions().size() > 1)
                logger.log(new StaticSemanticException("Array \"" + node.getIdentifier() + "\" not defined"), node);
            else
                logger.log(new StaticSemanticException("Array or switch \"" + node.getIdentifier() + "\" not defined"), node);
        } else {
            if (r.getGenre() == Record.Genre.SWITCH && node.getExpressions().size() != 1) {
                logger.log(new StaticSemanticException("Switch takes exactly one integer"), node);
            }
            if (r.getGenre() == Record.Genre.ARRAY && node.getExpressions().size() != ((ArrayRecord) r).getDimension() && !((ArrayRecord) r).isUnknown()) {
                logger.log(new StaticSemanticException("Array \"" + node.getIdentifier() + "\" is of dimension " + ((ArrayRecord) r).getDimension()), node);
            }
        }
        return this;
    }
}
