
import java.util.*;

import Jcg.geometry.*;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;
import tc.TC;

public class Unfold {
	private Polyhedron_3<Point_2> M; // Patron du polyedre
	private Polyhedron_3<Point_3> S; // Polyedre original
	private Hashtable<Halfedge<Point_3>, Halfedge<Point_2>> plani ; //corrélation entre S et M

		
	public Unfold(String fichier) {
		this.S = MeshLoader.getSurfaceMesh(fichier);
	}
	
	public static void main(String[] args) {
		Unfold U = new Unfold("OFF/octagon.off");

		// mettre dans le OFF
		U.Mesh2DToOff();
		ShowPlanarUnfolding.draw2D("2dmesh.off");
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
			v.index = ++i;
			TC.println(v.getPoint().x+" "+v.getPoint().y+" 0.000000");
		}
		for(Face<Point_2> f : M.facets){//ajoute les faces et leur points
			String S = ""+f.degree();
			int[] t = f.getVertexIndices(this.M);//tableau des index
			for(int c : t) S = S+" "+c;//ajoute les numero des sommets
			TC.println(S);
		}
	}	
	
	/*computing the unfolding M given the mesh S of the unfold instance*/
	public void computeM() {
		// creer le cut tree
		Hashtable<Integer, Halfedge<Point_3>> cutTree = computeCutTree(this.S);

		// couper le mesh et le mettre a  plat
		this.cutMesh(cutTree);
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

	public void cutMesh(Hashtable<Integer, Halfedge<Point_3>> cutTree) {
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
					H.opposite.face.tag = 1;//vistee
					H.opposite.face.setEdge(H.opposite);//dit quelle est l'endroit d'entrée
				}
				H = H.next;
			}
			if(!cutTree.containsValue(H) && ! cutTree.containsValue(H.opposite)&& H.opposite.face.tag == 0){//pas encore vue et pas coupé
				queue.add(H.opposite.face);//va etre traitee
				H.opposite.face.tag = 1;
				H.opposite.face.setEdge(H.opposite);//dit quelle est l'endroit d'entrée 
			}
		}
		//parcours contient l'ordre dans lequel il faut découper les faces
		this.plani = new Hashtable<Halfedge<Point_3>, Halfedge<Point_2>>();
		
		this.M = new Polyhedron_3<Point_2>();
		//traite à part la première face
		this.firstTo2D(parcours.removeFirst(),plani);
		System.out.println(this.M.facesToString());
		
		for(Face<Point_3> f : parcours){
			this.to2D(f,plani);
		}

	}
	
	/*agit sur this, met la face f dans le mesh 2D. Se repère dans le mesh existant grace à la table de hachage7
	 * plani qui note le lien entre les Halfedge 2D et 3D.*/
	public void to2D(Face<Point_3> f, Hashtable<Halfedge<Point_3>, Halfedge<Point_2>> plani){
		
	}
	 /*cas particulier pour la premiere face à ajouter*/
	public void firstTo2D(Face<Point_3> f, Hashtable<Halfedge<Point_3>, Halfedge<Point_2>> plani){
		Point_3 pp1,pp2,pp3; Point_2 p1,p2,p3;
		pp1 = f.getEdge().vertex.getPoint();
		pp2 = f.getEdge().next.vertex.getPoint();
		pp3 = f.getEdge().next.next.vertex.getPoint();
		p1 = new Point_2(0, 0);
		p2 = new Point_2(0,pp2.distanceFrom(pp1));
		double costeta = ((double)pp2.minus(pp1).innerProduct(pp3.minus(pp1)))/((double)pp2.distanceFrom(pp1)*(double)pp3.distanceFrom(pp1));
		double sinteta = (double) Math.sqrt(1-costeta*costeta);
		p3 = new Point_2(costeta*(double)pp3.distanceFrom(pp1), sinteta*(double)pp3.distanceFrom(pp1));
		this.M.makeTriangle(p1, p2, p3);//initialise notre mesh avec 3 points
		
		//remplir la table de hachage
		Halfedge<Point_2> h = this.M.facets.get(0).getEdge();
		for(int i = 0; i <3; i++){
			if (h.vertex.getPoint().x==0 && h.vertex.getPoint().y == 0) plani.put(f.getEdge(), h);
			else if (h.vertex.getPoint().y == 0) plani.put(f.getEdge().next, h);
			else plani.put(f.getEdge().next.next, h);			
			h=h.next;
		}
		
		Halfedge<Point_3> H = f.getEdge();
		H = H.next; H = H.next;
		while(H != f.getEdge()){
			Point_3 pp = H.vertex.getPoint(); //point à ajouter au mesh
			Point_2 p;//point à calculer en 2D, connaissant le hlafedge precedent H.previous
			
			H = H.next;
			//ne pas oublier le hasmap
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

	

}
