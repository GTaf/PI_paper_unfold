
import java.util.*;

import Jcg.geometry.*;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;
import tc.TC;

public class Unfold {
	private Polyhedron_3<Point_2> M; // Patron du polyedre
	private Polyhedron_3<Point_3> S; // Polyedre original

	public Unfold(String fichier) {
		this.S = MeshLoader.getSurfaceMesh(fichier);
	}
	/*computing the unfolding M given the mesh S of the unfold instance*/
	public void computeM() {
		// creer le cut tree
		Hashtable<Integer, Halfedge<Point_3>> cutTree = computeCutTree(this.S);

		// couper le mesh et le mettre a  plat
		this.M = this.cutMesh(cutTree);
	}

	/* Compute BFS or DFS cut tree */
	public static Hashtable<Integer, Halfedge<Point_3>> computeCutTree(Polyhedron_3<Point_3> S) {
		//LinkedList<Halfedge<Point_3>> result = new LinkedList<Halfedge<Point_3>>();
		Hashtable<Integer, Halfedge<Point_3>> ht = new Hashtable<Integer, Halfedge<Point_3>>();
		LinkedList<Vertex<Point_3>> queue = new LinkedList<Vertex<Point_3>>();

		resetTag3D(S); // met les tags a  0 : pas encore visite

		if (S.vertices.size() == 0)
			return null;
		

		queue.addFirst(S.vertices.get(0)); // ajout initial du premier halfedge
											// dans queue
		// ajoute les voisins
		while (!queue.isEmpty()) {
			Vertex<Point_3> v = queue.removeFirst();
			Halfedge<Point_3> h = v.getHalfedge();
			v.tag = 2; // marque le cote comme traite

			Halfedge<Point_3> H = h;
			while (H.next.opposite != h) {
				if (H.opposite.vertex.tag == 0) {
					//result.add(H.opposite);
					ht.put(H.opposite.hashCode(), H.opposite);
					H.face.tag = 1;
					H.opposite.vertex.tag = 1;// va Ãªtre traitÃ©
					queue.addLast(H.opposite.vertex);// addFirst pour DFS
				}
				H = H.next.opposite;
			}
		}
		return ht;
	}

	/* Cut the mesh according to the cut Tree given TO BE COMPLETED*/

	public Polyhedron_3<Point_2> cutMesh(Hashtable<Integer, Halfedge<Point_3>> cutTree) {
		resetTag3D(this.S);//aucune face visitée
		LinkedList<Face<Point_3>> parcours = new LinkedList<Face<Point_3>>();
		
		//parcours en largeur du graphe pour l'ordre de depliage
		LinkedList<Face<Point_3>> queue = new LinkedList<Face<Point_3>>();		
		queue.addFirst(this.S.facets.get(0));
		while(!queue.isEmpty()){
			Face<Point_3> f = queue.removeFirst();
			parcours.add(f);
			f.tag = 1;
			Halfedge<Point_3> H = f.getEdge();
			while(H.next != f.getEdge()){//fait le tour des cote de la face
				if(!cutTree.containsValue(H) && ! cutTree.containsValue(H.opposite)&& H.opposite.face.tag == 0){//pas encore vue et pas coupé
					queue.add(H.opposite.face);//va etre traitee
					H.opposite.face.tag = 1;
					H.opposite.face.setEdge(H.opposite);//dit quelle est l'endroit d'entrée
				}
				H = H.next;
			}
			if(!cutTree.containsValue(H) && ! cutTree.containsValue(H.opposite)&& H.opposite.face.tag == 0){//pas encore vue et pas coupé
				queue.add(H.opposite.face);//va etre traitee
				H.opposite.face.tag = 1;
				H.opposite.face.setEdge(H.opposite);
			}
		}
		//parcours contient l'ordre dans lequel il faut découper les faces
		
		for(Face<Point_3> f : parcours){
			f.to2D();
		}
		
		
		return null;
	}

	/* Put a 2D mesh into an OFF file format */
	public void Mesh2DToOff() {
		this.computeM();//compute the unfolding
		resetTag2D(this.M);
		resetIndex2D(this.M);

		TC.ecritureDansNouveauFichier("2dmesh.off");
		TC.println("OFF");//premiere ligne
		TC.println(this.M.vertices.size()+" "+this.M.facets.size()+" 0");//nombre de trucs
		int i = 0;
		for(Vertex<Point_2> v : M.vertices){//ajoute les points et leur donne un index
			v.index = i++;
			TC.println(v.getPoint().x+" "+v.getPoint().y+" 0.000000");
		}
		for(Face<Point_2> f : M.facets){//ajoute les faces et leur points
			String S = ""+f.degree();
			int[] t = f.getVertexIndices(this.M);//tableau des index
			for(int c : t) S = S+" "+c;//ajoute les numero des sommets
			TC.println(S);
		}

	}

	/* Reset all the tags to 0 for a new use of tags, 3D mesh */
	public static void resetTag3D(Polyhedron_3<Point_3> S) {
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

	/* Reset all the tags to 0 for a new use of tags, 2D mesh */
	public static void resetTag2D(Polyhedron_3<Point_2> S) {
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

	/* Reset all Indexes for a 2D Mesh */
	public static void resetIndex2D(Polyhedron_3<Point_2> S) {
		for (Halfedge<Point_2> h : S.halfedges) {
			h.index = 0;
		}
		for (Vertex<Point_2> v : S.vertices) {
			v.index = -1;
		}
	}

	public static void main(String[] args) {
		Unfold U = new Unfold("OFF/octagon.off");

		// mettre dans le OFF
		U.Mesh2DToOff();
	}

}
