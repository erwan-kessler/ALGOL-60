package eu.telecomnancy.pcl.testExam;


import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.debug.Logger;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.semantics.SemanticChecker;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import eu.telecomnancy.pcl.syntactic.SyntacticChecker;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;

import static eu.telecomnancy.pcl.Main.logger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class semanticChecks {
    private ASTRoot ast = null;

    @BeforeEach
    void init(TestInfo info) {
        InputStream is = getClass().getResourceAsStream(info.getTags().iterator().next());
        ANTLRParser parser = new ANTLRParser();
        try {
            parser.readSource(is);
            ast = parser.buildAST();
        } catch (IOException | ANTLRTreeException | RuntimeException e) {
            logger.logCritical(e);
            logger.getLogsCritical();
        }
        if (ast == null) {
            logger.logCritical("AST was not initialized");
            logger.getLogsCritical();
        }
        assert ast != null;
        ASTConstantFolder astConstantFolder = new ASTConstantFolder();
        astConstantFolder.visit(ast);
        ast = astConstantFolder.getOptimizedAST();

        SymbolTable TDS = (new TableCreation()).create(ast);

        SyntacticChecker syntacticChecker = new SyntacticChecker();
        syntacticChecker.visit(ast);

        SemanticChecker semanticChecker = new SemanticChecker(TDS);
        semanticChecker.visit(ast);

    }

    @AfterEach
    public void removeLogs() {
        logger.cleanLogs();
    }

    @Test
    @DisplayName("Global semantics check for presentation")
    @Tag("semantic.a60")
    public void parsingSemanticsCheck() {
        assertEquals("ERROR: StaticSemanticException : Explicit division by zero at line 23, character 9\n" +
                        "ERROR: StaticSemanticException : TDS: Variable u is already defined at line 9, character 4\n" +
                        "ERROR: SyntacticException : Invalid assignment at line 17, character 9\n" +
                        "ERROR: StaticSemanticException : Variable \"c\" not defined at line 15, character 16\n" +
                        "ERROR: StaticSemanticException : Type missmatch in assignment: none expected at line 15, character 16\n" +
                        "ERROR: StaticSemanticException : Variable \"p\" not defined at line 18, character 9\n" +
                        "ERROR: StaticSemanticException : Type missmatch in assignment: real expected at line 18, character 4\n" +
                        "ERROR: StaticSemanticException : Subscript is missing for \"A\" at line 19, character 9\n" +
                        "ERROR: StaticSemanticException : Number expected in subscript at line 20, character 25\n" +
                        "ERROR: StaticSemanticException : Type missmatch in assignment: real expected at line 20, character 4\n" +
                        "ERROR: StaticSemanticException : Procedure \"f\" is not defined at line 25, character 4\n" +
                        "ERROR: StaticSemanticException : Procedure \"P\" takes exactly 3 argument(s) at line 26, character 4\n" +
                        "ERROR: StaticSemanticException : Type string expected at parameter 2 at line 27, character 8\n" +
                        "ERROR: StaticSemanticException : array expected at parameter 3 at line 27, character 10\n",
                logger.getInternalLogs());
    }

}
