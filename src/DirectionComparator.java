import java.util.Comparator;
import java.util.Vector;

import Jcg.geometry.*;
import Jcg.polyhedron.Halfedge;


public class DirectionComparator implements Comparator<Halfedge<Point_3>>{
	Halfedge<Point_3> h;
	
	public DirectionComparator(Halfedge<Point_3> h) {
		this.h = h;
	}
	
	@Override
	public int compare(Halfedge h1,Halfedge h2){
		double l1 = (double) (h.vertex.getPoint().minus(h.opposite.vertex.getPoint())).innerProduct(h1.vertex.getPoint().minus(h1.opposite.vertex.getPoint()));
		double l2 = (double)(h.vertex.getPoint().minus(h.opposite.vertex.getPoint())).innerProduct(h2.vertex.getPoint().minus(h2.opposite.vertex.getPoint()));
		
		
		return Double.compare(Math.abs(l1),Math.abs(l2));//on veut les valeurs absolues
	}
	

}
