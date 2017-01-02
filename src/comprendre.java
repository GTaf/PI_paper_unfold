import Jcg.geometry.*;
import Jcg.mesh.*;
import Jcg.polyhedron.*;



public class comprendre {
	
	
	public static void main(String[] args) {
		Polyhedron_3<Point_3> poly;		
		poly=MeshLoader.getSurfaceMesh("OFF/octagon.off");
		
		for(Face<Point_3> e : poly.facets){
			System.out.println(e.degree());
			//System.out.println(e.face);
			//System.out.println(e.next.face);
			
			//System.out.println();
		}
		
		
		//System.out.println(poly.facets.get(1).getEdge().next.tag);
		
	}

}
