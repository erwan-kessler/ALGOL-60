package eu.telecomnancy.pcl.assembly.register;

public class Edge {
    Vertex[] vertices = new Vertex[2];

    Edge(Vertex s1, Vertex s2){
        this.vertices[0] = s1;
        this.vertices[1] = s2;
    }

    public boolean concern(Vertex s) {
        return (vertices[0].equals(s)|| vertices[1].equals(s));
    }

    public Vertex[] getVertices() {
        return vertices;
    }

    @Override
    public String toString() {
        return "This edge concerns the vertex " + vertices[0].toString() + " and the vertex " + vertices[1].toString();
    }
}