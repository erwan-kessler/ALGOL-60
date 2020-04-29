package eu.telecomnancy.pcl.testSyntactic;

import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.debug.Logger;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import eu.telecomnancy.pcl.syntactic.SyntacticChecker;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;

import static eu.telecomnancy.pcl.Main.logger;
import static org.junit.jupiter.api.Assertions.*;

public class testAssign {
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
        (new TableCreation()).create(ast);
        SyntacticChecker syntacticChecker = new SyntacticChecker();
        syntacticChecker.visit(ast);
    }

    @AfterEach
    public void removeLogs() {
        logger.cleanLogs();
    }

    @Test
    @DisplayName("Test if assignation is incorrect on simple scope")
    @Tag("simpleIncorrectAssign.a60")
    public void simpleIncorrectAssign() {
        assertFalse(logger.isCritical());
        assertTrue(logger.isThereLogs());
        assertEquals("ERROR: SyntacticException : Invalid assignment at line 3, character 7\n",
                logger.getInternalLogs());
    }

    @Test
    @DisplayName("Test if assignation is incorrect on multiple scope")
    @Tag("multipleIncorrectAssign.a60")
    public void multipleIncorrectAssign() {
        assertFalse(logger.isCritical());
        assertTrue(logger.isThereLogs());
        assertEquals("ERROR: SyntacticException : Invalid assignment at line 3, character 7\n" +
                        "ERROR: SyntacticException : Invalid assignment at line 7, character 15\n" +
                        "ERROR: SyntacticException : Invalid assignment at line 10, character 15\n",
                logger.getInternalLogs());
    }

    @Test
    @DisplayName("Test if assignation is correct on simple scope")
    @Tag("simpleCorrectAssign.a60")
    public void simpleCorrectAssign() {
        assertFalse(logger.isCritical());
        assertFalse(logger.isThereLogs());
    }

    @Test
    @DisplayName("Test if assignation is correct on multiple scope")
    @Tag("multipleCorrectAssign.a60")
    public void multipleCorrectAssign() {
        assertFalse(logger.isCritical());
        assertFalse(logger.isThereLogs());
    }
}
