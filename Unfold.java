
import java.util.*;

import Jcg.geometry.*;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;

public class Unfold {
	private Polyhedron_3<Point_2> M; // Patron du polyèdre
	private Polyhedron_3<Point_3> S; // Polyèdre original

	public Unfold(String fichier) {
		S = MeshLoader.getSurfaceMesh(fichier);

		// creer le cut tree
		List<Halfedge<Point_3>> cutTree = computeCutTree(S);
		
		//couper le mesh et le mettre à plat		
		M = cutMesh(cutTree,S);		
		
		//mettre dans le OFF
		Mesh2DToOff(M);

	}

	/* Compute BFS or DFS cut tree */
	public static List<Halfedge<Point_3>> computeCutTree(Polyhedron_3<Point_3> S) {
		LinkedList<Halfedge<Point_3>> result = new LinkedList<Halfedge<Point_3>>();
		LinkedList<Vertex<Point_3>> queue = new LinkedList<Vertex<Point_3>>();

		resetTag3D(S); // met les tags à 0 : pas encore visite
		
		if (S.vertices.size() == 0)
			return null;

		queue.addFirst(S.vertices.get(0)); // ajout initial du premier halfedge
											// dans queue
		// ajoute les voisins
		while (!queue.isEmpty()) {
			Vertex<Point_3> v = queue.removeFirst();
			Halfedge<Point_3> h = v.getHalfedge();
			v.tag = 2; // marque le coté comme traité

			Halfedge<Point_3> H = h;
			while (H != h) {
				if (H.opposite.vertex.tag == 0) {
					result.add(H.opposite);
					H.face.tag = 1;
					H.opposite.vertex.tag = 1;// va être traité
					queue.addLast(H.opposite.vertex);// addFirst pour DFS
				}
				H = H.next.opposite;
			}
		}
		return result;
	}
	
	
	/*Cut the mesh according to the cut Tree given*/
	
	public static Polyhedron_3<Point_2>  cutMesh(List<Halfedge<Point_3>> cutTree, Polyhedron_3<Point_3> S){
		return null;
	}
	
	
	
	/*Put a 2D mesh into an OFF file format*/
	public static void Mesh2DToOff(Polyhedron_3<Point_2> M){
		resetTag2D(M);
		resetIndex2D(M);
		
		
		
		
		
	}
	
	/*Reset all the tags to 0 for a new use of tags, 3D mesh*/
	public static void resetTag3D(Polyhedron_3<Point_3> S){
		for (Halfedge<Point_3> h : S.halfedges) {
			h.tag = 0;
		}
		for (Vertex<Point_3> v : S.vertices) {
			v.tag = 0; 
		}
		for (Face<Point_3> f : S.facets) {
			f.tag = 0; 
		}
	}
	
	/*Reset all the tags to 0 for a new use of tags, 2D mesh*/
	public static void resetTag2D(Polyhedron_3<Point_2> S){
		for (Halfedge<Point_2> h : S.halfedges) {
			h.tag = 0;
		}
		for (Vertex<Point_2> v : S.vertices) {
			v.tag = 0;
		}
		for (Face<Point_2> f : S.facets) {
			f.tag = 0;
		}
	}
	
	public static void resetIndex2D(Polyhedron_3<Point_2> S){
		for (Halfedge<Point_2> h : S.halfedges) {
			h.index = 0;
		}
		for (Vertex<Point_2> v : S.vertices) {
			v.index = 0;
		}
	}
	
	

}
