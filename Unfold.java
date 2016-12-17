
import java.util.*;

import javax.swing.JTree;

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
		
		cutMesh(S,cutTree);

	}

	/* Compute BFS or DFS cut tree */
	public static List<Halfedge<Point_3>> computeCutTree(Polyhedron_3<Point_3> S) {
		LinkedList<Halfedge<Point_3>> result = new LinkedList<Halfedge<Point_3>>();
		LinkedList<Vertex<Point_3>> queue = new LinkedList<Vertex<Point_3>>();

		for (Halfedge<Point_3> h : S.halfedges) {
			h.vertex.tag = 0; // signifie pas encore visité
		}
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
	
	public static Polyhedron_3<Point_2> 

}
