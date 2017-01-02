import java.util.*;
import Jcg.geometry.Point_2;
import Jcg.geometry.Segment_2;
import Jcg.polyhedron.Face;
import Jcg.polyhedron.Halfedge;
import Jcg.polyhedron.Polyhedron_3;
import Jcg.viewer.old.Fenetre;

import static Jcg.geometry.GeometricOperations_2.intersect;

import java.awt.color.*;

public class RemoveOverlaps {
	
	public static LinkedList<Polyhedron_3<Point_2>> removeOverlaps(ArrayList<ArrayList<Integer>> intersection, Polyhedron_3<Point_2> M){
		LinkedList<LinkedList<Halfedge<Point_2>>> P;
		P = new LinkedList<LinkedList<Halfedge<Point_2>>>();//liste des chemins
		//calculer les chemins
		for(int i = 0; i < intersection.size();i++){
			for(int h : intersection.get(i)){			
				P.add(calculChemin(M.halfedges.get(i),M.halfedges.get(h),M));
			}
		}
		
		//determiner où couper
		
		
		LinkedList<Halfedge<Point_2>> S = new LinkedList<Halfedge<Point_2>>();
		while(!P.isEmpty()){
			Halfedge<Point_2> e = null;
			int max = 0;// cut edge
			for(LinkedList<Halfedge<Point_2>> L : P){//determine le e à choisir
				for(Halfedge<Point_2> h : L){
					int cutted = cutted(h,P);
					if(cutted > max){
						e = h; max = cutted;
					}
				}
			}
			S.add(e);
			LinkedList<LinkedList<Halfedge<Point_2>>> delete = new LinkedList<LinkedList<Halfedge<Point_2>>>();
			for(LinkedList<Halfedge<Point_2>> L : P){
				if(L.contains(e)) delete.add(L);
			}
			for(LinkedList<Halfedge<Point_2>> L : delete){
				P.remove(L);
			}
			//System.out.println(max);
		}
		
		//System.out.println("on va couper  "+S);
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
		System.out.println("ok");
		
		return res;
	}
	
	
	public static LinkedList<Halfedge<Point_2>> calculChemin(Halfedge<Point_2> h1,Halfedge<Point_2> h2,Polyhedron_3<Point_2> M){
		LinkedList<Halfedge<Point_2>> Path = new LinkedList<Halfedge<Point_2>>();//resultat
		for(Face<Point_2> f : M.facets){
			f.tag = -1;//pas visitées
		}
		Face<Point_2> f,Fa;
		
		
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
		if(Path.contains(M.halfedges.get(49))){
			for(Halfedge<Point_2> h: Path){
			}
			
		}
		if(Path.contains(h1)||Path.contains(h2)) throw new Error("ca bug fdp");

		return Path;
	}
	
	/*returns the list of the neighbors of a face F*/
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
	
	/*return the edge between two faces if it exists, throws an error if not*/
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
	
	/*Numbre of paths cutted in P*/
	public static int cutted(Halfedge<Point_2> h,LinkedList<LinkedList<Halfedge<Point_2>>> P){
		int i = 0;
		for(LinkedList<Halfedge<Point_2>> L : P){
			if(L.contains(h)) i++;
		}
		return i;
	}
	
	public static void addFace(Face<Point_2> f, Polyhedron_3<Point_2> P){
		Halfedge<Point_2> h = f.getEdge();
		P.facets.add(f);
		P.halfedges.add(h);
		if(!P.vertices.contains(h.getVertex())){//si non présent ajoute le vertex
			P.vertices.add(h.getVertex());}
		h=h.next;
		int i = 0;
		while(h != f.getEdge()){
			P.halfedges.add(h);
			
			
			if(!P.vertices.contains(h.getVertex())){//si non présent ajoute le vertex
				P.vertices.add(h.getVertex());
				//System.out.println(i++);
				}
			
			h=h.next;
		}
	}
}
