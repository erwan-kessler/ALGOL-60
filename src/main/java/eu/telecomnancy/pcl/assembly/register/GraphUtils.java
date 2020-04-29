package eu.telecomnancy.pcl.assembly.register;

import eu.telecomnancy.pcl.ast.ASTDummy;
import eu.telecomnancy.pcl.symbolTable.Record;

import java.util.ArrayList;

public class GraphUtils {
    public GraphUtils(){
    }
    public GraphUtils(Graph graph,int nbColor){
        // by default we assume we have a bad graph
        colorGraphPessimistic(graph,nbColor);
    }
    public void colorGraph(Graph graph, int nbColor){
        colorGraphPessimistic(graph, nbColor);
        colorGraphOptimistic(graph,nbColor);
    }
    private static void colorGraphOptimistic(Graph graph, int nbColor) {
        for(Vertex s : graph.getVertices()){
            if(s.getColor() == -1){
                for(int i = 1; i <= nbColor; i++){
                    if(graph.canBeColored(s,i)){
                        s.setColor(i);
                    }
                }
            }
        }
    }

    private void colorGraphPessimistic(Graph graph, int nbColor){

        if(graph.getVertices().size() != 0) {
            boolean found = false;

            Vertex sTrivial = new Vertex(-1,new ASTDummy(-1,-1),"0",new Record(null,null,null));
            for (Vertex s : graph.getVertices()) {
                if (graph.triviallyColoriable(s, nbColor)) {
                    found = true;
                    sTrivial = s;
                    break;
                }
            }
            ArrayList<Vertex> newVertices;
            ArrayList<Edge> newArretesInterferences;
            ArrayList<Edge> newArretesPreferences;

            //If a trivially colorable vertex exists
            if (found) {
                //Then we execute colorGraphPessimistic without this vertex
                newVertices = graph.getNewVertex(sTrivial);
                newArretesInterferences = graph.getNewInterEdges(sTrivial);
                newArretesPreferences = graph.getNewPrefEdges(sTrivial);
                colorGraphPessimistic(new Graph(newVertices, newArretesInterferences, newArretesPreferences), nbColor);

                //Coloring the vertex with available color
                graph.coloringVertex(sTrivial, nbColor);

            } else { //No trivally coloriable vertex

                Vertex verBigestDegree = new Vertex(-2,new ASTDummy(-1,-1),"0",new Record(null,null,null));
                for (Vertex s : graph.getVertices()) {
                    if (graph.interDegre(s) == graph.maxDegree()) {
                        verBigestDegree = s;
                    }
                }

                newVertices = graph.getNewVertex(verBigestDegree);
                newArretesInterferences = graph.getNewInterEdges(verBigestDegree);
                newArretesPreferences = graph.getNewPrefEdges(verBigestDegree);
                colorGraphPessimistic(new Graph(newVertices, newArretesInterferences, newArretesPreferences), nbColor);
            }
        }
    }
}
