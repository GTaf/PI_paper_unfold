import java.util.*;
import Jcg.geometry.*;
import Jcg.polyhedron.*;


public class UnionFind {
    private HashMap<Vertex,Vertex> parent;
    
    public UnionFind( ){
        this.parent = new HashMap<Vertex,Vertex>();
    }
    
    public Vertex find(Vertex src ){
    	if (src == null) return null;
    	if(parent.get(src) == null) parent.put(src, src);
        if(src.equals(parent.get(src))) return src;
        else{
        	Vertex r = find(parent.get(src));
        	parent.put(src, r);
        	return r;
        }
    }
    
    public void union(Vertex v0,Vertex v1 ){
    	if(find(v0) == null) parent.put(v0, v0);
    	if(find(v1) == null) parent.put(v1, v1);
    	Vertex v0rep = find(v0);
    	Vertex v1rep = find(v1);
        parent.put(v0rep, v1rep);
    }
    
    

	public static LinkedList<Halfedge<Point_3>> kruskal(UnionFind u, Polyhedron_3<Point_3> s,Comparator<Halfedge<Point_3>> C) {
		LinkedList<Halfedge<Point_3>> F = new LinkedList<Halfedge<Point_3>>();
    	LinkedList<Halfedge<Point_3>> E = new LinkedList<Halfedge<Point_3>>();
    	for(Halfedge<Point_3> a : s.halfedges)
    		E.add(a);
    	Collections.sort(E, C);
    	for(Halfedge<Point_3> a : E){
    		if(a == null) return null;
    		if(!u.find(a.opposite.vertex).equals(u.find(a.vertex))){
    			F.add(a);
    			u.union(a.opposite.vertex, a.vertex);
    		}
    	}
    	
    	return F;
	}

}
