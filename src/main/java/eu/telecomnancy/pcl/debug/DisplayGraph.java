package eu.telecomnancy.pcl.debug;

import eu.telecomnancy.pcl.Main;
import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.assembly.register.Edge;
import eu.telecomnancy.pcl.assembly.register.GraphColoringRegister;
import eu.telecomnancy.pcl.assembly.register.RegisterAllocator;
import eu.telecomnancy.pcl.assembly.register.Vertex;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.semantics.SemanticChecker;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import eu.telecomnancy.pcl.syntactic.SyntacticChecker;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DisplayGraph {
    protected String styleSheet = "" +
            "node.black {" +
            "	fill-color: black;" +
            "}" +
            "node.blue {" +
            "	fill-color: blue;" +
            "}" +
            "node.red {" +
            "	fill-color: red;" +
            "}" +
            "node.green {" +
            "	fill-color: green;" +
            "}" +
            "node.yellow {" +
            "	fill-color: yellow;" +
            "}" +
            "node.pink {" +
            "	fill-color: pink;" +
            "}" +
            "node.brown {" +
            "	fill-color: brown;" +
            "}" +
            "node.violet {" +
            "	fill-color: violet;" +
            "}" +
            "node.orange {" +
            "	fill-color: orange;" +
            "}" +
            "edge {" +
            "   shape:angle;" +
            "}" +
            "edge.pref {" +
            "   shape:angle;" +
            "	fill-color: blue;" +
            "   size:2px;" +
            "}"+"edge.interf {" +
            "   shape:angle;" +
            "	fill-color: red;" +
            "   size:1px;" +
            "}";

    private String returnColor(int id) {
        String[] strings = {"black", "blue", "red", "green", "yellow", "pink", "brown", "violet","orange"};
        return strings[id];
    }

    public DisplayGraph(ArrayList<Edge> listEdgeInterf, ArrayList<Edge> listEdgePref, ArrayList<Vertex> listVertex) {
        Graph graph = new MultiGraph("Register Graph");
        for (Vertex vertex : listVertex) {
            Node n = graph.addNode(String.valueOf(vertex.getName()));
            n.addAttribute("ui.label", vertex.toString());
            n.addAttribute("ui.class", returnColor(vertex.getColor() == -1 ? 0 : vertex.getColor()));
        }
        for (Edge edge : listEdgeInterf) {
            Vertex[] vertices = edge.getVertices();
            org.graphstream.graph.Edge edge1 = graph.addEdge(String.valueOf(edge.hashCode()), String.valueOf(vertices[0].getName()), String.valueOf(vertices[1].getName()));
            edge1.addAttribute("ui.class", "interf");
        }
        for (Edge edge : listEdgePref) {
            Vertex[] vertices = edge.getVertices();
            org.graphstream.graph.Edge edge1 = graph.addEdge(String.valueOf(edge.hashCode()), String.valueOf(vertices[0].getName()), String.valueOf(vertices[1].getName()));
            edge1.addAttribute("ui.class", "pref");
        }
        graph.addAttribute("ui.stylesheet", styleSheet);
        graph.display();
    }

    public static void main(String[] args) throws IOException, ANTLRTreeException {
        ANTLRParser parser = new ANTLRParser();
        parser.readSource(Main.class.getClassLoader().getResourceAsStream("asm.a60"));
        ASTRoot ast = parser.buildAST();
        assert ast != null;
        ast = (new ASTConstantFolder()).visit(ast).getOptimizedAST();
        SymbolTable TDS = (new TableCreation()).create(ast);
        (new SyntacticChecker()).visit(ast);
        (new SemanticChecker(TDS)).visit(ast);
        GraphColoringRegister graphColoringRegister = new GraphColoringRegister(ast, TDS);
        new DisplayGraph(graphColoringRegister.getListEdgeInterf(), graphColoringRegister.getListEdgePref(), graphColoringRegister.getListVertex());

    }
}