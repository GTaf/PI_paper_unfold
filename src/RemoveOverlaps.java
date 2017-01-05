import java.util.*;
import Jcg.geometry.Point_2;
import Jcg.polyhedron.Face;
import Jcg.polyhedron.Halfedge;
import Jcg.polyhedron.Polyhedron_3;

public class RemoveOverlaps {
	/**
	 * Removes all intersections in the Polyhedron by cutting it in pieces
	 * @param intersection contains all the intersections between vertices
	 * @param M the unfolding to cut
	 * @return a List of Polyhedron
	 */
	public static LinkedList<Polyhedron_3<Point_2>> removeOverlaps(ArrayList<ArrayList<Integer>> intersection, Polyhedron_3<Point_2> M){
		HashMap<Integer,LinkedList<Halfedge<Point_2>>> P;//LinkedList<LinkedList<Halfedge<Point_2>>> P;
		P = new HashMap<Integer,LinkedList<Halfedge<Point_2>>>();//LinkedList<LinkedList<Halfedge<Point_2>>>();//liste des chemins
		System.out.println("chemins");
		//calculer les chemins
		for(int i = 0; i < intersection.size();i++){
			for(int h : intersection.get(i)){	
				LinkedList<Halfedge<Point_2>> L = calculChemin(M.halfedges.get(i),M.halfedges.get(h),M);
				if(!L.isEmpty())
				P.put(L.hashCode(),L);
			}
		}
		
		System.out.println("decoupe");
		//determiner où couper
		
		
		LinkedList<Halfedge<Point_2>> S = new LinkedList<Halfedge<Point_2>>();
		PriorityQueue<Halfedge<Point_2>> E = new PriorityQueue<Halfedge<Point_2>>(new HalfedgeCutComparator(P));
		for(Halfedge<Point_2> h : M.halfedges){
			E.add(h);
		}
		
		System.out.println("taff   "+P.size());
		while(!P.isEmpty()){
			Halfedge<Point_2> e=null;
			int max = 0;// cut edge
			for(LinkedList<Halfedge<Point_2>> L : P.values()){//determine le e à choisir
				for(Halfedge<Point_2> h : L){
					int cutted = cutted(h,P);//has to be recalculated each time
					if(cutted > max){
						e = h; max = cutted;
					}
				}
			}
			S.add(e);
			LinkedList<LinkedList<Halfedge<Point_2>>> delete = new LinkedList<LinkedList<Halfedge<Point_2>>>();
			for(LinkedList<Halfedge<Point_2>> L : P.values()){
				if(L.contains(e)) delete.add(L);
			}
			if(P.size() == 1) System.out.println(P.values());
			for(LinkedList<Halfedge<Point_2>> L : delete){
				P.remove(L.hashCode());
			}
		}
		
		System.out.println("coupe");
		//couper
		for(Halfedge<Point_2> h : S){
			System.out.println("h : "+h+"  opposé: "+h.opposite);
			Halfedge<Point_2> hp = h.opposite;
			
			h.opposite = new Halfedge<>();
			h.opposite.face = null;
			h.opposite.opposite = h;
			h.opposite.vertex = hp.vertex;
			
			hp.opposite = new Halfedge<>();
			hp.opposite.face = null;
			hp.opposite.opposite = h;
			hp.opposite.vertex = h.vertex;
			
		}
		//reconstruire
		LinkedList<Face<Point_2>> faceList = new LinkedList<Face<Point_2>>();
		for(Face<Point_2> faces : M.facets){
			faceList.add(faces);
			faces.tag = 0;
		}
		
		
		LinkedList<Polyhedron_3<Point_2>> res = new LinkedList<Polyhedron_3<Point_2>>();
		while(!faceList.isEmpty()){
			Polyhedron_3<Point_2> poly = new Polyhedron_3<Point_2>();
			LinkedList<Face<Point_2>> queue = new LinkedList<Face<Point_2>>();
			queue.add(faceList.removeFirst());
			queue.get(0).tag = 1;
			while(!queue.isEmpty()){
				Face<Point_2> face = queue.removeFirst();
				addFace(face, poly);
				for(Face<Point_2> neighbor : neighborFace(face)){
					if(neighbor.tag == 0){
						queue.add(neighbor);
						neighbor.tag = 1;//visited
						faceList.remove(neighbor);
					}
				}
			}
			res.add(poly);
		}
		
		return res;
	}
	
	/**
	 * Computes the shortest path between 2 edges in a Polyhedron using a form of dijkstra 
	 * @param h1 first halfedge
	 * @param h2 second halfedge
	 * @param M the unfolding
	 * @return the list of halfedges crossed in the Path
	 */
	public static LinkedList<Halfedge<Point_2>> calculChemin(Halfedge<Point_2> h1,Halfedge<Point_2> h2,Polyhedron_3<Point_2> M){
		LinkedList<Halfedge<Point_2>> Path = new LinkedList<Halfedge<Point_2>>();//resultat
		for(Face<Point_2> f : M.facets){
			f.tag = -1;//pas visitées
		}
		Face<Point_2> Fa;
		
		
		LinkedList<Face<Point_2>> q = new LinkedList<Face<Point_2>>();//queue des faces visitées
		if(h1.face != null) {//on ajoute la première face si elle existe
			q.add(h1.face);
			Fa = h1.face;
			h1.face.tag = M.facets.size();//pour la reconnaitre avec son tag
		}
		else Fa = h1.opposite.face;//pour être sur que Fa existe
		
		if(h1.opposite.face != null) {//ajoute l'eventuelle 2eme face de h1
			q.add(h1.opposite.face);//
			h1.opposite.face.tag = M.facets.size();//pour la reconnaitre avec son tag
		}
		

		


		
		while(!q.isEmpty() && (Fa.equals(h1.face) || Fa.equals(h1.opposite.face))){
			Face<Point_2> F = q.removeFirst();//enlève la dernière face
			
			for(Face<Point_2> fa : neighborFace(F)){
				//System.out.println(fa);
				if(fa.tag == -1) {fa.tag = M.facets.indexOf(F);q.add(fa);}//si pas encore visité, le visiter et indique parent
				if(fa.equals(h2.face) || fa.equals(h2.opposite.face)){ Fa = fa;break;}//on est arrivés, sort de la boucle
			}
		}
		while(!(Fa.equals(h1.face)||Fa.equals(h1.opposite.face) )){//remonte le trajet avec les tags parents
			Halfedge<Point_2> h = edge(Fa,M.facets.get(Fa.tag));
			Path.add(h);
			Fa = M.facets.get(Fa.tag);
			}
		if(Path.isEmpty()) System.out.println(h1+"         "+h2);

		return Path;
	}
	
	/**
	 * returns the list of the neighbors of a face
	 * @param F Face to get neighbors from
	 * @return list of faces
	 */
	public static LinkedList<Face<Point_2>> neighborFace(Face<Point_2> F){
		LinkedList<Face<Point_2>> res = new LinkedList<Face<Point_2>>();
		Halfedge<Point_2> h = F.getEdge();
		if(h.opposite.face != null)
		res.add(h.opposite.face);
		h=h.next;
		while(h != F.getEdge()){
			if(h.opposite.face !=null)
				res.add(h.opposite.face);
			h=h.next;
		}
		return res;
	}
	
	/**
	 * return the edge between two faces if it exists, throws an error if not
	 * @param f1 first face
	 * @param f2 second face
	 * @return common edge of f1 f2
	 */
	public static Halfedge<Point_2> edge(Face<Point_2> f1,Face<Point_2> f2){
		Halfedge<Point_2> h = f1.getEdge();
		if(h.opposite.getFace() != null && h.opposite.getFace().equals(f2)) return h;
		h=h.next;
		while(h != f1.getEdge()){
			if(f2.equals(h.opposite.getFace())) return h;
			h=h.next;
		}
		
		throw new Error("pas de edge de liaison");
	}
	
	/**
	 * Number of paths cutted in P if we cut on halfedge h
	 * @param h halfedge that will be cut
	 * @param P list oh paths
	 * @return numbre of cu paths
	 */
	public static int cutted(Halfedge<Point_2> h,HashMap<Integer,LinkedList<Halfedge<Point_2>>> P){
		int i = 0;
		for(LinkedList<Halfedge<Point_2>> L : P.values()){
			if(L.contains(h)) i++;
		}
		return i;
	}
	
	/**
	 * Add a face to a Polyhedron
	 * @param f face to add 
	 * @param P Polyhedron in which the face will be added 
	 */
	public static void addFace(Face<Point_2> f, Polyhedron_3<Point_2> P){
		Halfedge<Point_2> h = f.getEdge();
		P.facets.add(f);
		P.halfedges.add(h);
		if(!P.vertices.contains(h.getVertex())){//si non présent ajoute le vertex
			P.vertices.add(h.getVertex());}
		h=h.next;

		while(h != f.getEdge()){
			P.halfedges.add(h);
			
			
			if(!P.vertices.contains(h.getVertex())){//si non présent ajoute le vertex
				P.vertices.add(h.getVertex());
				}
			
			h=h.next;
		}
	}
}
