import java.util.Comparator;

import Jcg.geometry.Point_3;
import Jcg.polyhedron.Halfedge;

public class lengthComparator implements Comparator<Halfedge<Point_3>>{

	@Override
	public int compare(Halfedge h1,Halfedge h2){
		double l1 = (double)(h1.getVertex().getPoint().squareDistance(h1.opposite.vertex.getPoint()));
		double l2 = (double)(h2.getVertex().getPoint().squareDistance(h2.opposite.vertex.getPoint()));
		
		
		return Double.compare(l1,l2);//we want the smallest
	}

}
