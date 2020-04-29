package eu.telecomnancy.pcl.testTDS;

import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.debug.Logger;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.symbolTable.Records.ArrayRecord;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;

import static eu.telecomnancy.pcl.symbolTable.TableCreation.ScopeTypes.BLOCK;
import static eu.telecomnancy.pcl.symbolTable.TableCreation.ScopeTypes.PROCEDURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static eu.telecomnancy.pcl.Main.logger;

public class testGetRecord {
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
    }

    @AfterEach
    public void removeLogs() {
        logger.cleanLogs();
    }

    @Test
    @DisplayName("Get record on same scope")
    @Tag("recordSameScope.a60")
    public void parsingRecordSameScope() {
        SymbolTable TDS = (new TableCreation()).create(ast);
        assertFalse(logger.isCritical());
        TDS.enterScope();
        assertEquals(9, TDS.getCurrent().getRecords().size());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("a").getType());
        assertEquals(ASTType.REAL, TDS.getCurrent().lookup("b").getType());
        assertEquals(ASTType.BOOLEAN, TDS.getCurrent().lookup("c").getType());
        assertEquals(ASTType.REAL, ((ArrayRecord)TDS.getCurrent().lookup("d")).getActualType());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("e").getType());
        assertEquals(ASTType.BOOLEAN, ((ArrayRecord)TDS.getCurrent().lookup("f")).getActualType());
        assertEquals(ASTType.NONE, TDS.getCurrent().lookup("g").getType());
        assertEquals(ASTType.LABEL, TDS.getCurrent().lookup("h").getType());
        assertEquals(ASTType.LABEL, TDS.getCurrent().lookup("i").getType());
    }

    @Test
    @DisplayName("Get record on different scopes")
    @Tag("recordDifferentScope.a60")
    public void parsingDifferentScope() {
        SymbolTable TDS = (new TableCreation()).create(ast);
        assertFalse(logger.isCritical());
        TDS.enterScope();
        assertEquals(BLOCK.toString(), TDS.getCurrent().getScopeType());
        assertEquals(9, TDS.getCurrent().getRecords().size());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("a").getType());
        assertEquals(ASTType.REAL, TDS.getCurrent().lookup("b").getType());
        assertEquals(ASTType.BOOLEAN, TDS.getCurrent().lookup("c").getType());
        assertEquals(ASTType.REAL, ((ArrayRecord)TDS.getCurrent().lookup("d")).getActualType());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("e").getType());
        assertEquals(ASTType.BOOLEAN, ((ArrayRecord)TDS.getCurrent().lookup("f")).getActualType());
        assertEquals(ASTType.NONE, TDS.getCurrent().lookup("g").getType());
        assertEquals(ASTType.LABEL, TDS.getCurrent().lookup("h").getType());
        assertEquals(ASTType.LABEL, TDS.getCurrent().lookup("i").getType());
        TDS.enterScope();
        assertEquals(0, TDS.getCurrent().getRecords().size());
        assertEquals(PROCEDURE.toString(), TDS.getCurrent().getScopeType());
        TDS.exitScope();
        TDS.enterScope();
        assertEquals(BLOCK.toString(), TDS.getCurrent().getScopeType());
        assertEquals(9, TDS.getCurrent().getRecords().size());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("a").getType());
        assertEquals(ASTType.REAL, TDS.getCurrent().lookup("b").getType());
        assertEquals(ASTType.BOOLEAN, TDS.getCurrent().lookup("c").getType());
        assertEquals(ASTType.REAL, ((ArrayRecord)TDS.getCurrent().lookup("d")).getActualType());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("e").getType());
        assertEquals(ASTType.BOOLEAN, ((ArrayRecord)TDS.getCurrent().lookup("f")).getActualType());
        assertEquals(ASTType.NONE, TDS.getCurrent().lookup("g").getType());
        assertEquals(ASTType.LABEL, TDS.getCurrent().lookup("h").getType());
        assertEquals(ASTType.LABEL, TDS.getCurrent().lookup("i").getType());
    }

    @Test
    @DisplayName("Get same record on different scopes")
    @Tag("recordMultipleScope.a60")
    public void parsingMultipleScope() {
        SymbolTable TDS = (new TableCreation()).create(ast);
        assertFalse(logger.isCritical());
        TDS.enterScope();
        assertEquals(BLOCK.toString(), TDS.getCurrent().getScopeType());
        assertEquals(1, TDS.getCurrent().getRecords().size());
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("a").getType());
        TDS.enterScope();
        assertEquals(ASTType.REAL, TDS.getCurrent().lookup("a").getType());
        TDS.enterScope();
        assertEquals(ASTType.INTEGER, TDS.getCurrent().lookup("a").getType());
        TDS.exitScope();
        TDS.enterScope();
        assertEquals(ASTType.BOOLEAN, TDS.getCurrent().lookup("b").getType());
        assertEquals(ASTType.REAL, TDS.getCurrent().lookup("a").getType());
    }
}
