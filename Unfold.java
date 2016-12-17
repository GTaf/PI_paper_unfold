
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
		List<Vertex<Point_3>> L = new LinkedList<Vertex<Point_3>>();
		for (Vertex<Point_3> v : S.vertices)
			L.add(v);
		Vertex<Point_3> v = L.remove(0);
		JTree J;

		// remettre sous forme OFF

	}

	/* Creates the first face of M, with face f from S, adding triangles */
	public static void addFirstFace(Polyhedron_3<Point_2> M, Face<Point_3> f) {
		Halfedge<Point_3> h = f.getEdge();

		// ajout des deux premiers points
		M.vertices.add(new Vertex<Point_2>(new Point_2(0, 0))); // premier point
																// de M
		Point_3 p = h.vertex.getPoint(); // stocke le point de h
		M.vertices.add(new Vertex<Point_2>(new Point_2(0, p.distanceFrom(h.next.vertex.getPoint()))));

		// ajout du premier half-edge
		Halfedge<Point_2> H = new Halfedge<Point_2>();

		Face<Point_2> F = new Face<>();
		H.vertex = M.vertices.get(0);

	}

}
