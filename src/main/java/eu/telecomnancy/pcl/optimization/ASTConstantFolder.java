package eu.telecomnancy.pcl.optimization;

import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.semantics.StaticSemanticException;

import java.util.ArrayList;
import java.util.List;

import static eu.telecomnancy.pcl.Main.logger;

public class ASTConstantFolder implements ASTVisitor<ASTConstantFolder> {
    private ASTNode lastNode;

    public ASTConstantFolder() {
        lastNode = null;
    }

    public ASTRoot getOptimizedAST() {
        if (lastNode instanceof ASTRoot)
            return (ASTRoot) lastNode;
        else
            return null;
    }

    @Override
    public ASTConstantFolder visit(ASTAdd node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            float value = ((ASTNumber) nodeLeft).getValue();
            value += ((ASTNumber) nodeRight).getValue();
            boolean real = ((ASTNumber) nodeLeft).getExprType() == ASTType.REAL
                        || ((ASTNumber) nodeRight).getExprType() == ASTType.REAL;
            lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(),
                    value, real);
        } else {
            lastNode = new ASTAdd(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTAnd node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTLogical && nodeRight instanceof ASTLogical) {
            boolean value = ((ASTLogical) nodeLeft).getValue();
            value &= ((ASTLogical) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTAnd(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTAssign node) {
        node.getExpression().accept(this);
        ASTNode nodeExpression = lastNode;
        lastNode = new ASTAssign(node.getSourceLine(), node.getSourceChar(),
                node.getDesignators(), nodeExpression);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTBlock node) {
        List<ASTNode> nodeDeclarations = new ArrayList<ASTNode>();
        for (ASTNode nodeChild : node.getDeclarations()) {
            nodeChild.accept(this);
            nodeDeclarations.add(lastNode);
        }
        List<ASTNode> nodeStatements = new ArrayList<ASTNode>();
        for (ASTNode nodeChild : node.getStatements()) {
            nodeChild.accept(this);
            nodeStatements.add(lastNode);
        }
        lastNode = new ASTBlock(node.getSourceLine(), node.getSourceChar(),
                nodeDeclarations, nodeStatements);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTDecl node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTDeclArray node) {
        List<ASTDeclArrayBound> bounds = new ArrayList<ASTDeclArrayBound>();
        for (ASTDeclArrayBound bound : node.getBounds()) {
            bound.getBoundInf().accept(this);
            ASTNode nodeBoundInf = lastNode;
            bound.getBoundSup().accept(this);
            ASTNode nodeBoundSup = lastNode;
            bounds.add(new ASTDeclArrayBound(nodeBoundInf, nodeBoundSup));
        }
        lastNode = new ASTDeclArray(node.getSourceLine(), node.getSourceChar(),
                node.getIdentifier(), node.getType(),
                node.isOwned(), bounds);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTDeclFcn node) {
        node.getStatement().accept(this);
        ASTNode nodeStatement = lastNode;
        lastNode = new ASTDeclFcn(node.getSourceLine(), node.getSourceChar(),
                node.getIdentifier(), node.getType(),
                node.getParameters(), node.getValues(),
                node.getSpecs(), nodeStatement);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTDeclSwitch node) {
        List<ASTNode> nodeExpressions = new ArrayList<ASTNode>();
        for (ASTNode nodeChild : node.getExpressions()) {
            nodeChild.accept(this);
            nodeExpressions.add(lastNode);
        }
        lastNode = new ASTDeclSwitch(node.getSourceLine(), node.getSourceChar(),
                node.getIdentifier(), nodeExpressions);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTDiv node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            if (((ASTNumber) nodeRight).getValue() == 0) {
                // We have an explicit division by 0
                logger.log(new StaticSemanticException("Explicit division by zero"), node);
                lastNode = new ASTIdiv(node.getSourceLine(), node.getSourceChar(),
                        nodeLeft, nodeRight);
            }
            boolean real = ((ASTNumber) nodeLeft).getExprType() == ASTType.REAL
                        || ((ASTNumber) nodeRight).getExprType() == ASTType.REAL;
            float value = ((ASTNumber) nodeLeft).getValue() / ((ASTNumber) nodeRight).getValue();
            if (!real) value = (int)value;
            lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(), value, real);
        } else {
            lastNode = new ASTDiv(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }

        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTDummy node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTEq node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean value = ((ASTNumber) nodeLeft).getValue() ==
                    ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTEq(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTFcn node) {
        List<ASTNode> nodeParameters = new ArrayList<ASTNode>();
        for (ASTNode nodeChild : node.getParameters()) {
            nodeChild.accept(this);
            nodeParameters.add(lastNode);
        }
        lastNode = new ASTFcn(node.getSourceLine(), node.getSourceChar(),
                node.getIdentifier(), nodeParameters);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTFor node) {
        List<ASTForIterator> iterators = new ArrayList<ASTForIterator>();
        for (ASTForIterator iterator : node.getIterators()) {
            iterator.getExpression().accept(this);
            ASTNode nodeIteratorExpression = lastNode;
            if (iterator instanceof ASTForIteratorWhile) {
                ((ASTForIteratorWhile) iterator).getCondition().accept(this);
                ASTNode nodeIteratorCondition = lastNode;
                iterators.add(new ASTForIteratorWhile(nodeIteratorExpression,
                        nodeIteratorCondition));
            } else if (iterator instanceof ASTForIteratorStepUntil) {
                ((ASTForIteratorStepUntil) iterator).getStepExpression()
                        .accept(this);
                ASTNode nodeIteratorStepExpression = lastNode;
                ((ASTForIteratorStepUntil) iterator).getUntilExpression()
                        .accept(this);
                ASTNode nodeIteratorUntilExpression = lastNode;
                iterators.add(new ASTForIteratorStepUntil(
                        nodeIteratorExpression,
                        nodeIteratorStepExpression,
                        nodeIteratorUntilExpression
                ));
            } else {
                iterators.add(new ASTForIteratorExpr(nodeIteratorExpression));
            }
        }
        node.getStatement().accept(this);
        ASTNode nodeStatement = lastNode;
        lastNode = new ASTFor(node.getSourceLine(), node.getSourceChar(),
                node.getVariable(), iterators, nodeStatement);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTGe node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean value = ((ASTNumber) nodeLeft).getValue() >=
                    ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTGe(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTGoto node) {
        node.getExpression().accept(this);
        ASTNode nodeExpression = lastNode;
        lastNode = new ASTGoto(node.getSourceLine(), node.getSourceChar(),
                nodeExpression);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTGt node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean value = ((ASTNumber) nodeLeft).getValue() >
                    ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTGt(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTIdiv node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            float leftValue = ((ASTNumber) nodeLeft).getValue();
            float rightValue = ((ASTNumber) nodeRight).getValue();
            if ((leftValue != Math.ceil(leftValue) || (rightValue != Math.ceil(rightValue)))) {
                logger.log(new StaticSemanticException("Int division is not defined for reals"), node);
                lastNode = new ASTIdiv(node.getSourceLine(), node.getSourceChar(),
                        nodeLeft, nodeRight);
            }
            if (rightValue == 0) {
                logger.log(new StaticSemanticException("Explicit division by zero"), node);
                lastNode = new ASTIdiv(node.getSourceLine(), node.getSourceChar(),
                        nodeLeft, nodeRight);

            }
            lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(),
                                     (int)(leftValue / rightValue), false);
        } else {
            lastNode = new ASTIdiv(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTIf node) {
        node.getCondition().accept(this);
        ASTNode nodeCondition = lastNode;
        node.getThenStatement().accept(this);
        ASTNode nodeThenStatement = lastNode;
        node.getElseStatement().accept(this);
        ASTNode nodeElseStatement = lastNode;
        if (nodeCondition instanceof ASTLogical) {
            if (((ASTLogical) nodeCondition).getValue()) {
                lastNode = nodeThenStatement;
                logger.logWarning("Error, branch unreachable ", nodeElseStatement);
            } else {
                lastNode = nodeElseStatement;
                logger.logWarning("Error, branch unreachable ", nodeThenStatement);
            }

        } else {
            lastNode = new ASTIf(node.getSourceLine(), node.getSourceChar(),
                    nodeCondition, nodeThenStatement,
                    nodeElseStatement);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTIfe node) {
        node.getCondition().accept(this);
        ASTNode nodeCondition = lastNode;
        node.getThenExpression().accept(this);
        ASTNode nodeThenExpression = lastNode;
        node.getElseExpression().accept(this);
        ASTNode nodeElseExpression = lastNode;
        if (nodeCondition instanceof ASTLogical) {
            if (((ASTLogical) nodeCondition).getValue())
                lastNode = nodeThenExpression;
            else
                lastNode = nodeElseExpression;
        } else {
            lastNode = new ASTIfe(node.getSourceLine(), node.getSourceChar(),
                    nodeCondition, nodeThenExpression,
                    nodeElseExpression);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTIff node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTLogical && nodeRight instanceof ASTLogical) {
            boolean value = ((ASTLogical) nodeLeft).getValue() ==
                    ((ASTLogical) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTIff(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTImpl node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTLogical && nodeRight instanceof ASTLogical) {
            boolean value = !((ASTLogical) nodeLeft).getValue();
            value |= ((ASTLogical) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTImpl(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTLabel node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTLe node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean value = ((ASTNumber) nodeLeft).getValue() <=
                    ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTLe(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTLogical node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTLt node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean value = ((ASTNumber) nodeLeft).getValue() <
                    ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTLt(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTMinus node) {
        node.getNode().accept(this);
        ASTNode nodeUnary = lastNode;
        if (nodeUnary instanceof ASTNumber) {
            boolean real = ((ASTNumber) nodeUnary).getExprType() == ASTType.REAL;
            float value = ((ASTNumber) nodeUnary).getValue();
            value *= -1f;
            lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(),
                                     value, real);
        } else {
            lastNode = new ASTMinus(node.getSourceLine(), node.getSourceChar(),
                    nodeUnary);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTMul node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean real = ((ASTNumber) nodeLeft).getExprType() == ASTType.REAL
                        || ((ASTNumber) nodeRight).getExprType() == ASTType.REAL;
            float value = ((ASTNumber) nodeLeft).getValue();
            value *= ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(),
                                     value, real);
        } else {
            lastNode = new ASTMul(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTNeq node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean value = ((ASTNumber) nodeLeft).getValue() !=
                    ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTNeq(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTNot node) {
        node.getNode().accept(this);
        ASTNode nodeUnary = lastNode;
        if (nodeUnary instanceof ASTLogical) {
            boolean value = !((ASTLogical) nodeUnary).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTNot(node.getSourceLine(), node.getSourceChar(),
                    nodeUnary);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTNumber node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTOr node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTLogical && nodeRight instanceof ASTLogical) {
            boolean value = ((ASTLogical) nodeLeft).getValue();
            value |= ((ASTLogical) nodeRight).getValue();
            lastNode = new ASTLogical(node.getSourceLine(),
                    node.getSourceChar(), value);
        } else {
            lastNode = new ASTOr(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTPlus node) {
        node.getNode().accept(this);
        /* ASTPlus is semantically useless, we don't overwrite lastNode */
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTPow node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            float leftValue = ((ASTNumber) nodeLeft).getValue();
            float rightValue = ((ASTNumber) nodeRight).getValue();
            boolean real = ((ASTNumber) nodeLeft).getExprType() == ASTType.REAL
                        || ((ASTNumber) nodeRight).getExprType() == ASTType.REAL;
            // Section 3.3.4 of the modified report
            if (rightValue != Math.ceil(rightValue)) {
                // The power is not integer
                if (leftValue > 0) {
                    float value = (float) Math.pow(leftValue, rightValue);
                    lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(), value, real);
                } else if (leftValue == 0.0 && rightValue > 0) {
                    lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(), 0, real);
                } else {
                    logger.log(new StaticSemanticException("Undefined power operator"), node);
                }
            } else if (leftValue == Math.ceil(leftValue) && rightValue == Math.ceil(rightValue)) {
                //Both nodes are integer
                if (rightValue < 0 || (leftValue == 0 && rightValue == 0)) {
                    logger.log(new StaticSemanticException("Undefined power operator"), node);
                } else {
                    float value = (float) Math.pow(leftValue, rightValue);
                    real = rightValue < 0;
                    lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(), value, real);
                }
            } else if (rightValue == Math.ceil(rightValue) && leftValue != Math.ceil(leftValue)) {
                if (rightValue == 0 && leftValue == 0.0) {
                    logger.log(new StaticSemanticException("Undefined power operator"), node);
                } else {
                    float value = (float) Math.pow(leftValue, rightValue);
                    lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(), value, true);
                }
            }
        } else {
            lastNode = new ASTPow(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTRoot node) {
        List<ASTNode> nodes = new ArrayList<ASTNode>();
        for (ASTNode nodeChild : node.getNodes()) {
            nodeChild.accept(this);
            nodes.add(lastNode);
        }
        lastNode = new ASTRoot(node.getSourceLine(), node.getSourceChar(),
                nodes);
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTString node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTSub node) {
        node.getNodeLeft().accept(this);
        ASTNode nodeLeft = lastNode;
        node.getNodeRight().accept(this);
        ASTNode nodeRight = lastNode;
        if (nodeLeft instanceof ASTNumber && nodeRight instanceof ASTNumber) {
            boolean real = ((ASTNumber) nodeLeft).getExprType() == ASTType.REAL
                        || ((ASTNumber) nodeRight).getExprType() == ASTType.REAL;
            float value = ((ASTNumber) nodeLeft).getValue();
            value -= ((ASTNumber) nodeRight).getValue();
            lastNode = new ASTNumber(node.getSourceLine(), node.getSourceChar(),
                                     value, real);
        } else {
            lastNode = new ASTSub(node.getSourceLine(), node.getSourceChar(),
                    nodeLeft, nodeRight);
        }
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTVar node) {
        lastNode = node;
        return this;
    }

    @Override
    public ASTConstantFolder visit(ASTVarSubscript node) {
        List<ASTNode> nodeExpressions = new ArrayList<ASTNode>();
        for (ASTNode nodeChild : node.getExpressions()) {
            nodeChild.accept(this);
            nodeExpressions.add(lastNode);
        }
        lastNode = new ASTVarSubscript(node.getSourceLine(),
                node.getSourceChar(),
                node.getIdentifier(), nodeExpressions);
        return this;
    }
}
