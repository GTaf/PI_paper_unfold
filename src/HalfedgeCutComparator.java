import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import Jcg.geometry.Point_2;
import Jcg.polyhedron.Halfedge;
import Jcg.polyhedron.Polyhedron_3;

public class HalfedgeCutComparator implements Comparator<Halfedge<Point_2>>{
	private HashMap<Integer,LinkedList<Halfedge<Point_2>>> P;
	private Hashtable<Halfedge<Point_2>, Integer> H;
	public HalfedgeCutComparator(HashMap<Integer,LinkedList<Halfedge<Point_2>>> P){
		this.P = P;
		this.H = new Hashtable<Halfedge<Point_2>, Integer> ();
	}
	
	
	public int compare(Halfedge<Point_2> h1,Halfedge<Point_2> h2){
		Integer c1 = H.get(h1);
		Integer c2 = H.get(h2);
		
		if(c1 == null){
			c1 = RemoveOverlaps.cutted(h1, P);
			H.put(h1, c1);
		}
		if(c2 == null){
			c2 = RemoveOverlaps.cutted(h2, P);
			H.put(h2, c2);
		}
		return -Integer.compare(c1, c2);
	}

}
