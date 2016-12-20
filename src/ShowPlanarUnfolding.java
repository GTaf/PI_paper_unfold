import Jcg.geometry.*;
import Jcg.mesh.*;
import Jcg.viewer.old.Fenetre;
import Jcg.polyhedron.*;

/**
 * A class for testing and drawing planar meshes (unfolding of 3d meshes)
 *
 * @author Luca Castelli Aleardi (INF421, Ecole Polytechnique, 2016)
 *
 */
public class ShowPlanarUnfolding {

	
	/**
	 * Draw a planar unfolding
	 */    
	public static void draw2D(String filename) {
    	if(filename.endsWith(".off")==false) {
    		System.out.println("Wrong input format: .off required");
    		System.exit(0);
    	}

    	Polyhedron_3<Point_2> polyhedron2D; // planar mesh
    	polyhedron2D=MeshLoader.getPlanarMesh(filename); // input planar mesh (half-edge representation)
    	polyhedron2D.isValid(true);
    	
    	Fenetre f=new Fenetre(); // a 2d window for drawing the mesh
    	f.addPolyhedronEdges(polyhedron2D);
    }

    public static void main (String[] args) {
    	if(args.length<1) {
    		System.out.println("missing argument: input filename (planar mesh required)");
    		return;
    	}
    	String filename=args[0];
    	//String filename = "OFF/octagon.off";
    	draw2D(filename);
    }

}
