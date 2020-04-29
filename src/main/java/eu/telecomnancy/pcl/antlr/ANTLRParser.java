package eu.telecomnancy.pcl.antlr;

import eu.telecomnancy.pcl.ast.*;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.tool.ANTLRErrorListener;
import org.antlr.tool.Message;
import org.antlr.tool.ToolMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ANTLRParser {
    private Tree tree;

    public void readSource(InputStream is) throws IOException, ANTLRTreeException {
        ANTLRInputStream ais = new ANTLRInputStream(is);

        algol60Lexer lexer = new algol60Lexer(ais);
        TokenStream tokens = new CommonTokenStream(lexer);
        algol60Parser parser = new algol60Parser(tokens);
        try {
            tree = (Tree) parser.prog().getTree();
        } catch (RecognitionException e) {
            throw new ANTLRTreeException(e.toString()+" at line "+e.line+", character "+e.charPositionInLine);
        }
    }

    /* TODO: Better syntax... */
    private List<ASTNode> buildAST(Tree tree) throws ANTLRTreeException {
        int sourceLine = tree.getLine();
        int sourceChar = tree.getCharPositionInLine();

        List<ASTNode> newNodes = new ArrayList<>();

        List<ASTNode> nodes;
        List<ASTNode> declarations;
        List<ASTNode> statements;
        List<ASTNode> designators;
        List<ASTNode> expressions;
        List<ASTNode> parameters;
        ASTNode node;
        ASTNode nodeLeft;
        ASTNode nodeRight;
        ASTNode variable;
        ASTNode condition;
        ASTNode expression;
        ASTNode thenExpression;
        ASTNode elseExpression;
        ASTNode statement;
        ASTNode thenStatement;
        ASTNode elseStatement;
        List<String> parametersDecl;
        List<String> values;
        List<ASTDeclArrayBound> bounds;
        List<ASTDeclFcnSpec> specs;
        List<ASTForIterator> forIterators;
        String identifier;
        ASTType type;
        boolean owned;
        String label;
        String value;

        switch (tree.getType()) {
            case 0: /* root node */
                nodes = new ArrayList<>();
                for (int i = 0; i < tree.getChildCount(); ++i)
                    nodes.add(buildAST(tree.getChild(i)).get(0));
                newNodes.add(new ASTRoot(sourceLine, sourceChar, nodes));
                break;
            case algol60Parser.ADD:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTAdd(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.AND:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTAnd(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.ASSIGN:
                designators = new ArrayList<>();
                designators.add(buildAST(tree.getChild(0)).get(0));
                for (int i = 1; i < tree.getChildCount() - 1; ++i)
                    designators.add(buildAST(tree.getChild(i).getChild(0))
                            .get(0));
                expression = buildAST(tree.getChild(tree.getChildCount() - 1)
                        .getChild(0)).get(0);
                newNodes.add(new ASTAssign(sourceLine, sourceChar, designators,
                        expression));
                break;
            case algol60Parser.BLOCK:
                declarations = new ArrayList<>();
                statements = new ArrayList<>();
                for (int i = 0; i < tree.getChildCount(); ++i) {
                    switch (tree.getChild(i).getType()) {
                        case algol60Parser.DECL:
                        case algol60Parser.DECL_ARRAY:
                        case algol60Parser.DECL_SWITCH:
                        case algol60Parser.DECL_FCN:
                            declarations.addAll(buildAST(tree.getChild(i)));
                            break;
                        default:
                            statements.addAll(buildAST(tree.getChild(i)));
                            break;
                    }
                }
                newNodes.add(new ASTBlock(sourceLine, sourceChar, declarations,
                        statements));
                break;
            case algol60Parser.DECL:
                owned = tree.getChild(0).getType() == algol60Parser.OWN;
                type = ASTType.getType(tree.getChild(owned ? 1 : 0).getChild(0)
                        .getText());
                for (int i = owned ? 2 : 1; i < tree.getChildCount(); ++i) {
                    identifier = tree.getChild(i).getChild(0).getText();
                    newNodes.add(new ASTDecl(sourceLine, sourceChar, identifier,
                            type, owned));
                }
                break;
            case algol60Parser.DECL_ARRAY:
                owned = tree.getChild(0).getType() == algol60Parser.OWN;
                if (tree.getChild(owned ? 1 : 0).getType() == algol60Parser.TYPE)
                    type = ASTType.getType(tree.getChild(owned ? 1 : 0)
                            .getChild(0).getText());
                else
                    type = ASTType.REAL;
                for (int i = (owned ? 1 : 0) + (type != ASTType.REAL ? 1 : 0);
                     i < tree.getChildCount(); ++i) {
                    Tree treeChild = tree.getChild(i);
                    bounds = new ArrayList<>();
                    Tree treeGrandChild = treeChild.getChild(treeChild
                            .getChildCount() - 1);
                    for (int j = 0; j < treeGrandChild.getChildCount(); ++j) {
                        ASTDeclArrayBound bound = new ASTDeclArrayBound(
                                buildAST(treeGrandChild.getChild(j)
                                        .getChild(0).getChild(0)).get(0),
                                buildAST(treeGrandChild.getChild(j)
                                        .getChild(1).getChild(0)).get(0)
                        );
                        bounds.add(bound);
                    }
                    for (int j = 0; j < treeChild.getChildCount() - 1; ++j) {
                        identifier = treeChild.getChild(j).getChild(0)
                                .getText();
                        newNodes.add(new ASTDeclArray(sourceLine, sourceChar,
                                identifier, type, owned, bounds));
                    }
                }
                break;
            case algol60Parser.DECL_FCN:
                parametersDecl = new ArrayList<>();
                values = new ArrayList<>();
                specs = new ArrayList<>();
                declarations = new ArrayList<>();
                if (tree.getChild(0).getType() == algol60Parser.TYPE)
                    type = ASTType.getType(tree.getChild(0).getChild(0)
                            .getText());
                else
                    type = ASTType.NONE;
                identifier = tree.getChild(type != ASTType.NONE ? 1 : 0)
                        .getChild(0).getText();
                for (int i = 0; i < tree.getChild(type != ASTType.NONE ? 2 : 1)
                        .getChildCount(); ++i) {
                    Tree treeChild = tree.getChild(type != ASTType.NONE ? 2 : 1)
                            .getChild(i);
                    for (int j = i != 0 ? 1 : 0; j < treeChild.getChildCount();
                         ++j)
                        parametersDecl.add(treeChild.getChild(j).getChild(0)
                                .getChild(0).getText());
                }
                for (int i = 0; i < tree.getChild(type != ASTType.NONE ? 3 : 2)
                        .getChildCount(); ++i)
                    values.add(tree.getChild(type != ASTType.NONE ? 3 : 2)
                            .getChild(i).getChild(0).getText());
                for (int i = 0; i < tree.getChild(type != ASTType.NONE ? 4 : 3)
                        .getChildCount(); ++i) {
                    Tree treeChild = tree.getChild(type != ASTType.NONE ? 4 : 3)
                            .getChild(i);
                    String typeA = treeChild.getChild(0).getChild(0).getText();
                    String typeB = "";
                    if (treeChild.getChild(0).getChildCount() != 1)
                        typeB = treeChild.getChild(0).getChild(1).getText();
                    for (int j = 1; j < treeChild.getChildCount(); ++j) {
                        //declarations.add((ASTNode) buildAST(treeChild.getChild(j).getChild(0)));
                        String idf = treeChild.getChild(j).getChild(0).getText();
                        specs.add(new ASTDeclFcnSpec(
                                idf,
                                ASTDeclFcnSpecType.getType(typeA, typeB),
                                values.stream().anyMatch(str -> str.equals(idf))
                        ));
                    }
                }
                statement = buildAST(tree.getChild(type != ASTType.NONE ? 5 : 4))
                        .get(0);
                newNodes.add(new ASTDeclFcn(sourceLine, sourceChar, identifier,
                        type, parametersDecl, values, specs, /*declarations,*/
                        statement));
                break;
            case algol60Parser.DECL_SWITCH:
                identifier = tree.getChild(0).getChild(0).getText();
                expressions = new ArrayList<>();
                for (int i = 1; i < tree.getChildCount(); ++i) {
                    expression = buildAST(tree.getChild(i).getChild(0)).get(0);
                    if (expression instanceof ASTNumber)
                        expression = new ASTVar(sourceLine,
                                sourceChar,
                                ""+(int)((ASTNumber) expression).getValue());
                    expressions.add(expression);
                }
                newNodes.add(new ASTDeclSwitch(sourceLine, sourceChar,
                        identifier, expressions));
                break;
            case algol60Parser.DIV:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTDiv(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.DUMMY:
                newNodes.add(new ASTDummy(sourceLine, sourceChar));
                break;
            case algol60Parser.EQ:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTEq(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.FCN:
                identifier = tree.getChild(0).getChild(0).getText();
                parameters = new ArrayList<>();
                for (int i = 0; i < tree.getChild(1).getChildCount(); ++i) {
                    Tree treeChild = tree.getChild(1).getChild(i);
                    for (int j = i != 0 ? 1 : 0; j < treeChild.getChildCount();
                         ++j) {
                        Tree treeGrandChild = treeChild.getChild(j).getChild(0);
                        if (treeGrandChild.getType() == algol60Parser.EXPR)
                            parameters.add(buildAST(treeGrandChild.getChild(0))
                                    .get(0));
                        else
                            parameters.add(buildAST(treeGrandChild).get(0));
                    }
                }
                newNodes.add(new ASTFcn(sourceLine, sourceChar, identifier,
                        parameters));
                break;
            case algol60Parser.FOR:
                variable = buildAST(tree.getChild(0)).get(0);
                forIterators = new ArrayList<>();
                statement = buildAST(tree.getChild(2)).get(0);
                for (int i = 0; i < tree.getChild(1).getChildCount(); ++i) {
                    Tree treeChild = tree.getChild(1).getChild(i);
                    if (treeChild.getType() == algol60Parser.EXPR) {
                        forIterators.add(new ASTForIteratorExpr(
                                buildAST(treeChild.getChild(0)).get(0)
                        ));
                    } else if (treeChild.getType() == algol60Parser.WHILE) {
                        forIterators.add(new ASTForIteratorWhile(
                                buildAST(treeChild.getChild(0).getChild(0)).get(0),
                                buildAST(treeChild.getChild(1).getChild(0)).get(0)
                        ));
                    } else {
                        forIterators.add(new ASTForIteratorStepUntil(
                                buildAST(treeChild.getChild(0).getChild(0)).get(0),
                                buildAST(treeChild.getChild(1).getChild(0)).get(0),
                                buildAST(treeChild.getChild(2).getChild(0)).get(0)
                        ));
                    }
                }
                newNodes.add(new ASTFor(sourceLine, sourceChar, variable,
                        forIterators, statement));
                break;
            case algol60Parser.GE:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTGe(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.GOTO:
                expression = buildAST(tree.getChild(0)).get(0);
                if (expression instanceof ASTNumber)
                    expression = new ASTVar(sourceLine,
                            sourceChar,
                            ""+(int)((ASTNumber) expression).getValue());
                newNodes.add(new ASTGoto(sourceLine, sourceChar, expression));
                break;
            case algol60Parser.GT:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTGt(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.IDIV:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTIdiv(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.IF:
                condition = buildAST(tree.getChild(0).getChild(0)).get(0);
                thenStatement = buildAST(tree.getChild(1)).get(0);
                if (tree.getChildCount() == 2)
                    elseStatement = new ASTDummy(sourceLine, sourceChar);
                else
                    elseStatement = buildAST(tree.getChild(2)).get(0);
                newNodes.add(new ASTIf(sourceLine, sourceChar, condition,
                        thenStatement, elseStatement));
                break;
            case algol60Parser.IFE:
                condition = buildAST(tree.getChild(0).getChild(0)).get(0);
                thenExpression = buildAST(tree.getChild(1).getChild(0)).get(0);
                elseExpression = buildAST(tree.getChild(2).getChild(0)).get(0);
                newNodes.add(new ASTIfe(sourceLine, sourceChar, condition,
                        thenExpression, elseExpression));
                break;
            case algol60Parser.IFF:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTIff(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.IMPL:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTImpl(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.LABEL:
                label = tree.getChild(0).getChild(0).getText();
                newNodes.add(new ASTLabel(sourceLine, sourceChar, label));
                break;
            case algol60Parser.LE:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTLe(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.LOGICAL:
                value = tree.getChild(0).getText();
                newNodes.add(new ASTLogical(sourceLine, sourceChar, !value.equals("false")));
                break;
            case algol60Parser.LT:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTLt(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.MINUS:
                node = buildAST(tree.getChild(0)).get(0);
                newNodes.add(new ASTMinus(sourceLine, sourceChar, node));
                break;
            case algol60Parser.MUL:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTMul(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.NEQ:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTNeq(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.NOT:
                node = buildAST(tree.getChild(0)).get(0);
                newNodes.add(new ASTNot(sourceLine, sourceChar, node));
                break;
            case algol60Parser.NUMBER:
                value = tree.getChild(0).getText();
                newNodes.add(new ASTNumber(sourceLine, sourceChar,
                                           Float.parseFloat(value),
                                      value.contains(".") || value.contains("e")));
                break;
            case algol60Parser.OR:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTOr(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.PLUS:
                node = buildAST(tree.getChild(0)).get(0);
                newNodes.add(new ASTPlus(sourceLine, sourceChar, node));
                break;
            case algol60Parser.POW:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTPow(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.STRING:
                value = tree.getChild(0).getText();
                newNodes.add(new ASTString(sourceLine, sourceChar, value));
                break;
            case algol60Parser.SUB:
                nodeLeft = buildAST(tree.getChild(0)).get(0);
                nodeRight = buildAST(tree.getChild(1)).get(0);
                newNodes.add(new ASTSub(sourceLine, sourceChar, nodeLeft,
                        nodeRight));
                break;
            case algol60Parser.VAR:
                identifier = tree.getChild(0).getChild(0).getText();
                if (tree.getChildCount() == 1) {
                    newNodes.add(new ASTVar(sourceLine, sourceChar,
                            identifier));
                } else {
                    if (tree.getChild(1).getType() == algol60Parser.SUBSCRIPT) {
                        expressions = new ArrayList<>();
                        for (int i = 0; i < tree.getChild(1).getChildCount();
                             ++i)
                            expressions.add(buildAST(tree.getChild(1)
                                    .getChild(i).getChild(0)).get(0));
                        newNodes.add(new ASTVarSubscript(sourceLine, sourceChar,
                                identifier, expressions));
                    } else {
                        parameters = new ArrayList<>();
                        for (int i = 0; i < tree.getChild(1).getChildCount();
                             ++i) {
                            Tree treeChild = tree.getChild(1).getChild(i);
                            for (int j = i != 0 ? 1 : 0; j < treeChild
                                    .getChildCount(); ++j) {
                                Tree treeGrandChild = treeChild.getChild(j)
                                        .getChild(0);
                                if (treeGrandChild.getType() ==
                                        algol60Parser.EXPR)
                                    parameters.add(buildAST(treeGrandChild
                                            .getChild(0)).get(0));
                                else
                                    parameters.add(buildAST(treeGrandChild)
                                            .get(0));
                            }
                        }
                        newNodes.add(new ASTFcn(sourceLine, sourceChar,
                                identifier, parameters));
                    }
                }
                break;
            default:
                throw new ANTLRMalformedTreeTokenException(tree.getType());
        }

        if (newNodes.size() < 1)
            throw new ANTLRTreeException();

        return newNodes;
    }

    public ASTRoot buildAST() throws ANTLRTreeException {
        List<ASTNode> nodes = buildAST(tree);
        if (nodes.size() < 1){
            throw new ANTLRTreeException();
        }

        ASTNode node = nodes.get(0);
        if (node instanceof ASTRoot)
            return (ASTRoot) node;
        else
            return new ASTRoot(node.getSourceLine(), node.getSourceChar(),
                    Collections.singletonList(node));
    }
}
