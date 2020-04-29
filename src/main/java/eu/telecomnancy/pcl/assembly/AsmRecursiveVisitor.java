package eu.telecomnancy.pcl.assembly;

import eu.telecomnancy.pcl.ast.*;

import java.util.HashMap;
import java.util.Map;

/* XXX: AsmRecursiveVisitor
 * flags all recursive procedures to fix `RegisterAllocator` for recursive flows
 *
 * false positive are possible by design (and should not be a issue)
 *
 * FIXME(low-priority): higher-order recursive flows are not flagged...
 * therefore mutual recursion is broken
 */

public class AsmRecursiveVisitor implements ASTVisitor<AsmRecursiveVisitor> {
    Map<String, Integer> fcns;

    public AsmRecursiveVisitor() {
        fcns = new HashMap<>();
    }

    @Override
    public AsmRecursiveVisitor visit(ASTAdd node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTAnd node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTAssign node) {
        for (ASTNode nodeChild : node.getDesignators())
            nodeChild.accept(this);

        node.getExpression().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTBlock node) {
        for (ASTNode nodeChild : node.getDeclarations())
            nodeChild.accept(this);

        for (ASTNode nodeChild : node.getStatements())
            nodeChild.accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTDecl node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTDeclArray node) {
        for (ASTDeclArrayBound bound : node.getBounds()) {
            bound.getBoundInf().accept(this);
            bound.getBoundSup().accept(this);
        }

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTDeclFcn node) {
        String fcn = node.getIdentifier();

        fcns.put(fcn, 0);

        node.getStatement().accept(this);

        if (fcns.get(fcn) > 0)
            node.setRecursive();

        fcns.remove(fcn);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTDeclSwitch node) {
        for (ASTNode nodeChild : node.getExpressions())
            nodeChild.accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTDiv node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTDummy node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTEq node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTFcn node) {
        String fcn = node.getIdentifier();

        Integer value = fcns.get(fcn);
        if (value != null)
            fcns.replace(fcn, value.intValue() + 1);

        for (ASTNode nodeChild : node.getParameters())
            nodeChild.accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTFor node) {
        for (ASTForIterator iterator : node.getIterators()) {
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
    public AsmRecursiveVisitor visit(ASTGe node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTGoto node) {
        node.getExpression().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTGt node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTIdiv node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTIf node) {
        node.getCondition().accept(this);
        node.getThenStatement().accept(this);
        node.getElseStatement().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTIfe node) {
        node.getCondition().accept(this);
        node.getThenExpression().accept(this);
        node.getElseExpression().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTIff node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTImpl node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTLabel node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTLe node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTLogical node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTLt node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTMinus node) {
        node.getNode().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTMul node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTNeq node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTNot node) {
        node.getNode().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTNumber node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTOr node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTPlus node) {
        node.getNode().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTPow node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTRoot node) {
        for (ASTNode nodeChild : node.getNodes())
            nodeChild.accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTString node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTSub node) {
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);

        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTVar node) {
        return this;
    }

    @Override
    public AsmRecursiveVisitor visit(ASTVarSubscript node) {
        for (ASTNode nodeChild : node.getExpressions())
            nodeChild.accept(this);

        return this;
    }
}
