
import java.util.*;

import Jcg.geometry.*;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;
import tc.TC;

public class Unfold {
	private Polyhedron_3<Point_2> M; // Patron du polyedre
	private Polyhedron_3<Point_3> S; // Polyedre original
	private Hashtable<Halfedge<Point_2>, Halfedge<Point_3>> plani ; //correlation entre S et M
	private float epsilon ; //numeric tolerance

		
	public Unfold(String fichier) {
		this.S = MeshLoader.getSurfaceMesh(fichier);
	}
	
	public static void main(String[] args) {
		Unfold U = new Unfold("OFF/cube_trunc.off");

		// mettre dans le OFF
		U.Mesh2DToOff();
		System.out.println(U.M.edgesToString());
		ShowPlanarUnfolding.draw2D("2dmesh.off");
	}
	
	/* Put a 2D mesh into an OFF file format and compute the correspondance betwenn vertices from S and M into an OFF file*/
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
		TC.ecritureDansNouveauFichier("correspondance.off");
		for (Vertex<Point_2> v : M.vertices){
			TC.println(this.plani.get(v.getHalfedge()).vertex.index);
		}

	}	
	
	/*computing the unfolding M given the mesh S of the unfold instance*/
	public void computeM() {
		// creer le cut tree
		Hashtable<Integer, Halfedge<Point_3>> cutTree = computeCutTree(this.S);

		// couper le mesh et le mettre aÂ  plat
		this.cutMesh(cutTree);
	}

	/* Compute BFS or DFS cut tree */
	public static Hashtable<Integer, Halfedge<Point_3>> computeCutTree(Polyhedron_3<Point_3> S) {
		//LinkedList<Halfedge<Point_3>> result = new LinkedList<Halfedge<Point_3>>();
		Hashtable<Integer, Halfedge<Point_3>> ht = new Hashtable<Integer, Halfedge<Point_3>>();
		LinkedList<Vertex<Point_3>> queue = new LinkedList<Vertex<Point_3>>();

		resetTag3D(S); // met les tags aÂ  0 : pas encore visite

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
					H.opposite.vertex.tag = 1;// va ÃƒÂªtre traitÃƒÂ©
					queue.addLast(H.opposite.vertex);// addFirst pour DFS
				}
				H = H.next.opposite;
			}
		}
		return ht;
	}

	/* Cut the mesh according to the cut Tree given TO BE COMPLETED*/

	public void cutMesh(Hashtable<Integer, Halfedge<Point_3>> cutTree) {
		resetTag3D(this.S);//aucune face visitÃ©e
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
				if(!cutTree.containsValue(H) && ! cutTree.containsValue(H.opposite)&& H.opposite.face.tag == 0){//pas encore vue et pas coupÃ©
					queue.add(H.opposite.face);//va etre traitee
					H.opposite.face.tag = 1;//vistee
					H.opposite.face.setEdge(H.opposite);//dit quelle est l'endroit d'entrÃ©e
				}
				H = H.next;
			}
			if(!cutTree.containsValue(H) && ! cutTree.containsValue(H.opposite)&& H.opposite.face.tag == 0){//pas encore vue et pas coupÃ©
				queue.add(H.opposite.face);//va etre traitee
				H.opposite.face.tag = 1;
				H.opposite.face.setEdge(H.opposite);//dit quelle est l'endroit d'entrÃ©e 
			}
		}
		//parcours contient l'ordre dans lequel il faut dÃ©couper les faces
		this.plani = new Hashtable<Halfedge<Point_2>, Halfedge<Point_3>>();
		
		this.M = new Polyhedron_3<Point_2>();
		//traite Ã  part la premiÃ¨re face
		this.firstTo2D(parcours.removeFirst(),plani);
		
		
		for(Face<Point_3> f : parcours){
			this.to2D(f,plani);
		}

	}
	
	/*agit sur this, met la face f dans le mesh 2D. Se repÃ¨re dans le mesh existant grace Ã  la table de hachage7
	 * plani qui note le lien entre les Halfedge 2D et 3D.*/
	public void to2D(Face<Point_3> f, Hashtable<Halfedge<Point_2>, Halfedge<Point_3>> plani){
		
	}
	 /*cas particulier pour la premiere face Ã  ajouter*/
	public void firstTo2D(Face<Point_3> f, Hashtable<Halfedge<Point_2>, Halfedge<Point_3>> plani){
		Point_3 pp1,pp2,pp3; Point_2 p1,p2,p3;
		pp1 = f.getEdge().vertex.getPoint();
		pp2 = f.getEdge().next.vertex.getPoint();
		pp3 = f.getEdge().next.next.vertex.getPoint();
		p1 = new Point_2(0, 0);
		p2 = new Point_2(pp2.distanceFrom(pp1),0);
		p3 = new Point_2(costeta(pp1,pp2,pp3)*(double)pp3.distanceFrom(pp1), sinteta(pp1,pp2,pp3)*(double)pp3.distanceFrom(pp1));
		this.M.makeTriangle(p1, p2, p3);//initialise notre mesh avec 3 points
		
		//remplir la table de hachage
		Halfedge<Point_2> h = this.M.facets.get(0).getEdge();
		for(int i = 0; i <3; i++){
			if (h.vertex.getPoint().x==0 && h.vertex.getPoint().y == 0) plani.put(h,f.getEdge()); 
			else if (h.vertex.getPoint().y == 0) plani.put(h, f.getEdge().next);
			else plani.put(h,f.getEdge().next.next);			
			h=h.next;
		}
		
		Halfedge<Point_3> H = f.getEdge();
		H = H.next.next.next; //va au sommet non traité
		this.M.facets.get(0).setEdge(this.M.facets.get(0).getEdge().next);//change le edge de référence
		System.out.println(f.degree());
		System.out.println(f);
		
		while(H != f.getEdge()){
			Point_3 pp = H.vertex.getPoint(); //point Ã  ajouter au mesh
			double d = (double)pp.distanceFrom(pp1);//distance entre p et l'origine
			Point_2 p = new Point_2(costeta(pp1,pp2,pp)*d, sinteta(pp1,pp2,pp)*d);//point Ã  calculer en 2D, connaissant le hlafedge precedent H.previous
			
			this.splitEdge(this.M.facets.get(0).getEdge().prev, p);
			
			H = H.next;
			//ne pas oublier le hasmap
		}
	}
	/*cos de l'angle entre pp1 pp2 et pp1 pp3*/
	public static double costeta(Point_3 pp1, Point_3 pp2, Point_3 pp3){
		return((double)pp2.minus(pp1).innerProduct(pp3.minus(pp1)))/((double)pp2.distanceFrom(pp1)*(double)pp3.distanceFrom(pp1));
	}
	/*cos de l'angle entre pp1 pp2 et pp1 pp3*/
	public static double sinteta(Point_3 pp1, Point_3 pp2, Point_3 pp3){
		return (double) Math.sqrt(1-costeta(pp1,pp2,pp3)*costeta(pp1,pp2,pp3));
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
	public static void resetIndex2D(Polyhedron_3<Point_2> M) {
		for (Halfedge<Point_2> h : M.halfedges) {
			h.index = 0;
		}
		for (Vertex<Point_2> v : M.vertices) {
			v.index = -1;
		}
	}
	
	
	
	/*Check combinatorial validity*/
	public boolean isValid(){
		return this.M.isValid(false); //version je m'emmerde pas a voir si on met true ou false
	}
	
	
	/*Check the isometry of the unfolding*/
	/*en gros, je vais pour chaque noeud, je regarde la distance de toutes ses arretes
	 * pour chaque arête, je vais chercher le sommet correspondant dans S
	 * si la distance est fausse, je  sors
	 * sinon je traite tout*/
	public boolean isIsometric(){
		resetTag2D(this.M);
		
		for (Vertex<Point_2> v : this.M.vertices ){
			Halfedge<Point_2> h = v.getHalfedge();
			Halfedge<Point_2> H = h.next;
			int s1; //index of the corresponding vertex in S
			for (int i = 0 ; i < v.index + 1 ; i++ ){
				s1=TC.readInt("correspondance.OFF");
			}
			Vertex<Point_3> vS = this.S.get(s1);
			
			//cas initial
			if ( H.tag == 0){
				int s2; 
				for (int i = 0 ; i < H.vertex.index + 1 ; i++ ){
					s2=TC.readInt("correspondance.OFF");
				}
				Vertex<Point_3> HS = this.S.get(s2);
				if (Math.abs(v.squareDistance(H.vertex) - vS.squareDistance(HS))>this.epsilon)
					return false;
				H.tag=1;
				H.opposite.tag=1; //avoid checking a length twice
				H=H.opposite.next;

			
			while ( H != h ) {
				if ( H.tag == 0){
					int s2; 
					for (int i = 0 ; i < H.vertex.index + 1 ; i++ ){
						s2=TC.readInt("correspondance.OFF");
					}
					Vertex<Point_3> HS = S.get(s2);
					if (Math.abs(v.squareDistance(H.vertex) - vS.squareDistance(HS))>this.epsilon)
						return false;
					H.tag=1;
					H.opposite.tag=1; //avoid checking a length twice
				}
				H=H.opposite.next;
			}	
			
		}
		
		return true;//each edges has been checked
	}
	
	public void splitEdge(Halfedge<Point_2> h, Point_2 point){
		// create the new edges, faces and vertex to be added
    	Halfedge<Point_2> hNewLeft=new Halfedge<Point_2>();
		Halfedge<Point_2> hNewRight=new Halfedge<Point_2>();
		Vertex<Point_2> newVertex=new Vertex<Point_2>(point);
		
		// set the vertex incidence relations
		newVertex.setEdge(hNewLeft);
		hNewLeft.setVertex(newVertex);

		Vertex<Point_2> u=h.getOpposite().getVertex();
		hNewRight.setVertex(u);
		u.setEdge(hNewRight);
		
		h.opposite.setVertex(newVertex);
		
		// set incidence relations for the first new halfedge
		hNewLeft.setFace(h.face);
		hNewLeft.setPrev(h.prev);
		hNewLeft.setNext(h);
		hNewLeft.setOpposite(hNewRight);
		// warning: the order in the updates does count here
		h.prev.setNext(hNewLeft); 
		h.setPrev(hNewLeft);
		
		// set incidence relations for the second new halfedge
		hNewRight.setFace(h.opposite.face);
		hNewRight.setPrev(h.opposite);
		hNewRight.setNext(h.opposite.next);
		hNewRight.setOpposite(hNewLeft);
		// warning: the order in the updates does count here
		h.opposite.next.setPrev(hNewRight);
		h.opposite.setNext(hNewRight);
		
		// add new cells to the polyhedron
		this.M.vertices.add(newVertex);
		this.M.halfedges.add(hNewLeft);
		this.M.halfedges.add(hNewRight);
		
		//return hNewLeft;
	}
	
	

	

}