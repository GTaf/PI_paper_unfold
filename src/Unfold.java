import java.util.*;
import Jcg.geometry.*;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;
import Jcg.viewer.old.Fenetre;
import tc.TC;
import static Jcg.geometry.GeometricOperations_2.intersect;

public class Unfold {
    private Polyhedron_3<Point_2> M; // Patron du polyedre
    private Polyhedron_3<Point_3> S; // Polyedre original
    private DoubleHashMap<Halfedge<Point_2> ,Halfedge<Point_3>> plani ; //correlation entre S et M
    private double epsilon; //numeric tolerance
    private boolean BFS;
    private String filename;
    private ArrayList<ArrayList<Integer>> intersection;


    private Unfold(String fichier) {
    	this.filename = fichier;
        this.S = MeshLoader.getSurfaceMesh("OFF\\"+fichier);
        this.BFS = true;
        this.epsilon = (double) 0.1;
    }
    
    private Unfold(Polyhedron_3<Point_2> M){
    	this.M = M;
    }

    public static void main(String[] args) {
    	String filename = "octagon.off";
        Unfold U = new Unfold(filename);   
        // mettre dans le OFF
        U.computeM();
        Mesh2DToOff(U.M,U.filename);
        U.correspondance();
        System.out.println("Le depliage est isometrique : "+U.isIsometric());        
        
        U.M = MeshLoader.getPlanarMesh("results/2D_"+U.filename);

        ShowPlanarUnfolding.draw2D("results/2D_"+U.filename);
        
        System.out.println("Le depliage est valide : "+U.isValid());
   
        System.out.println("Le depliage contient "+U.isOverlapping()+" recouvrements");
        
        /*for(Halfedge<Point_2> h : U.M.halfedges){
        	if((h.vertex.getPoint().y ==3.7350517958876917 )
        		||(h.opposite.vertex.getPoint().y == 3.7350517958876917 ))
        	System.out.println(h.index+"         "+h);
        }*/
        
        U.removeOverlaps();
        
        System.out.println("done");
    }

    /* Put a 2D mesh into an OFF file format and compute the correspondance betwenn vertices from S and M into an OFF file*/
    private static void Mesh2DToOff(Polyhedron_3<Point_2> M,String file) {
        //compute the unfolding
        resetTag2D(M);
        resetIndex2D(M);

        TC.ecritureDansNouveauFichier("results/2D_"+file);
        TC.println("OFF");//premiere ligne
        TC.println(M.vertices.size()+" "+M.facets.size()+" 0");//nombre de trucs
        int i = 0;
        for(Vertex<Point_2> v : M.vertices){//ajoute les points et leur donne un index
            v.index = i++;
            TC.println(v.getPoint().x+" "+v.getPoint().y+" 0.000000");
        }
        for(Face<Point_2> f : M.facets){//ajoute les faces et leur points
            String S = ""+f.degree();
            int[] t = f.getVertexIndices(M);//tableau des index
            for(int c : t) S = S+" "+c;//ajoute les numero des sommets
            TC.println(S);
        }

    }

    /*computing the unfolding M given the mesh S of the unfold instance*/
    private void computeM() {
        // creer le cut tree
        Hashtable<Integer, Halfedge<Point_3>> cutTree = this.flatSpanning();//this.computeCutTree();

        // couper le mesh et le mettre a  plat
        this.cutMesh(cutTree);
    }

    /* Compute BFS or DFS cut tree */
    private Hashtable<Integer, Halfedge<Point_3>> computeCutTree() {
    	//the cut tree
        Hashtable<Integer, Halfedge<Point_3>> ht = new Hashtable<Integer, Halfedge<Point_3>>();
        LinkedList<Vertex<Point_3>> queue = new LinkedList<Vertex<Point_3>>();

        resetTag3D(this.S); //all tags to 0

        if (this.S.vertices.size() == 0)
            return null;

        //add the first halfedge in the queue
        queue.addFirst(this.S.vertices.get(0));
        
        //add the neighbors
        while (!queue.isEmpty()) {
            Vertex<Point_3> v = queue.removeFirst();
            Halfedge<Point_3> h = v.getHalfedge();
            v.tag = 2; //done for this vertex

            Halfedge<Point_3> H = h;
            while (H.next.opposite != h) {
                if (H.opposite.vertex.tag == 0) {
                    //result.add(H.opposite);
                    ht.put(H.opposite.hashCode(), H.opposite);
                    H.face.tag = 1;
                    H.opposite.vertex.tag = 1;// va etre traite
                    if(this.BFS)//BFS or DFS
                    	queue.addLast(H.opposite.vertex);
                    else
                    	queue.addFirst(H.opposite.vertex);
                }
                H = H.next.opposite;
            }
        }
        return ht;
    }

	/* Cut the mesh according to the cut Tree given*/

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
        
        //plani is the link between 2D and 3D for edges
        this.plani = new DoubleHashMap<Halfedge<Point_2> ,Halfedge<Point_3>>();

        this.M = new Polyhedron_3<Point_2>();
        
        //especialy for the first face
        this.firstTo2D(parcours.removeFirst(),plani);
        
        //add other faces
        for(Face<Point_3> f : parcours){
            this.to2D(f,plani);
        }

        
        

    }

    /*agit sur this, met la face f dans le mesh 2D. Se repère dans le mesh existant grace a  la table de hachage7
     * plani qui note le lien entre les Halfedge 2D et 3D.*/
    public void to2D(Face<Point_3> f, DoubleHashMap<Halfedge<Point_2> ,Halfedge<Point_3>> plani){
        Point_3 pp1,pp2,pp3; Point_2 p1,p2,p3;
        pp1 = f.getEdge().prev.vertex.getPoint();
        pp2 = f.getEdge().vertex.getPoint();
        pp3 = f.getEdge().next.vertex.getPoint();

        //calcul de l'angle du halfedge de référence psi
        p1 = plani.get(f.getEdge().getOpposite()).getVertex().getPoint();
        p2 = plani.get(f.getEdge().getOpposite()).getOpposite().getVertex().getPoint();
        double psi = Math.acos(costeta(p1,p2,new Point_2(p1.x+1.0,p1.y)));
        if((double)p1.minus(p2).getCartesian(1)<0) psi=-psi;

        //calcul des points
        //premier point
        double teta = Math.acos(costeta(pp1,pp2,pp3));
        teta = teta+psi;
        p3 = new Point_2(Math.cos(teta)*(double)pp3.distanceFrom(pp1), Math.sin(teta)*(double)pp3.distanceFrom(pp1));
        p3.translateOf(p1.minus(new Point_2(0,0)).opposite());
        Halfedge<Point_2> h = plani.get(f.getEdge().getOpposite()).getOpposite();//celui de la face trait�e
        this.M.addTriangleToBorder(h,p3);
        
        //ajouter hachage
        plani.put(h, f.getEdge());
        plani.put(h.next, f.getEdge().next);
        plani.put(h.next.next, f.getEdge().next.next);
        
        //add tag
        h.next.getVertex().tag = f.getEdge().next.vertex.index;//p3
        
        //autres points
        Halfedge<Point_3> H = f.getEdge();
        H = H.next.next; //va au sommet non traite
        
        Face<Point_2> F =plani.get(f.getEdge().getOpposite()).getOpposite().getFace();//face 2D      
        F.setEdge(F.getEdge().next);//change le edge de reference

        
        while(H.next != f.getEdge()){
            Point_3 pp = H.vertex.getPoint(); //point �  ajouter au mesh
            double d = (double)pp.distanceFrom(pp1);//distance entre p et l'origine
            teta = Math.acos(costeta(pp1,pp2,pp));
            teta = teta+psi;
            Point_2 p = new Point_2(Math.cos(teta)*d, Math.sin(teta)*d);//point �  calculer en 2D, connaissant le hlafedge precedent H.previous
            p.translateOf(p1.minus(new Point_2(0,0)).opposite());//translate p of p1
            plani.put(this.splitEdge(F.getEdge().prev.prev, p),H);
            F.getEdge().prev.prev.prev.getVertex().tag = H.vertex.index;//met le tag

            H = H.next;
        }
        plani.put(F.getEdge().getPrev().getPrev(),H);
        
    }
    /*cas particulier pour la premiere face �  ajouter*/
    public void firstTo2D(Face<Point_3> f, DoubleHashMap<Halfedge<Point_2> ,Halfedge<Point_3>> plani){
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
            if (h.vertex.getPoint().x==0 && h.vertex.getPoint().y == 0) h.vertex.tag = f.getEdge().vertex.index; //plani.put(h,f.getEdge());
            else if (h.vertex.getPoint().y == 0) {
            	plani.put(h, f.getEdge().next);
            	h.vertex.tag = f.getEdge().next.vertex.index;
            	}
            else {
            	plani.put(h,f.getEdge().next.next);
            	h.vertex.tag = f.getEdge().next.next.vertex.index;
            }
            
            h=h.next;
        }
        
        Halfedge<Point_3> H = f.getEdge();
        H = H.next.next.next; //va au sommet non traite
        this.M.facets.get(0).setEdge(this.M.facets.get(0).getEdge().next);//change le edge de reference,
        //car l'actuel va être decoupe

        while(H != f.getEdge()){
            Point_3 pp = H.vertex.getPoint(); //point �  ajouter au mesh
            double d = (double)pp.distanceFrom(pp1);//distance entre p et l'origine
            Point_2 p = new Point_2(costeta(pp1,pp2,pp)*d, sinteta(pp1,pp2,pp)*d);//point �  calculer en 2D, connaissant le hlafedge precedent H.previous
            plani.put(this.splitEdge(this.M.facets.get(0).getEdge().prev, p),H);
            
            this.M.facets.get(0).getEdge().prev.prev.getVertex().tag = H.vertex.index;//met le tag
            

            H = H.next;
            //ne pas oublier le hasmap
        }
        plani.put(this.M.facets.get(0).getEdge().getPrev(),H);
    }


    /*cos de l'angle entre pp1 pp2 et pp1 pp3*/
    public static double costeta(Point_3 pp1, Point_3 pp2, Point_3 pp3){
        return((double)pp2.minus(pp1).innerProduct(pp3.minus(pp1)))/((double)pp2.distanceFrom(pp1)*(double)pp3.distanceFrom(pp1));
    }

    public static double costeta(Point_2 pp1, Point_2 pp2, Point_2 pp3){
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
        /*for (Vertex<Point_3> v : S.vertices) {
            v.tag = 0;
        }*/
        for (Face<Point_3> f : S.facets) {
            f.tag = 0;
        }
    }

    /* Reset all the tags to 0 for a new use of tags, 2D mesh */
    public static void resetTag2D(Polyhedron_3<Point_2> S) {
        for (Halfedge<Point_2> h : S.halfedges) {
            h.tag = 0;
        }
        /*for (Vertex<Point_2> v : S.vertices) {
            v.tag = 0;
        }*/
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
    	//Polyhedron_3<Point_2> polyhedron2D = MeshLoader.getPlanarMesh("results/2D_"+this.filename);
        return this.M.isValid(false);
    }


    /*Check the isometry of the unfolding*/
    public boolean isIsometric(){
        for (Halfedge<Point_2> h : this.M.halfedges){

	        if (h.tag ==0 && h.opposite.tag==0){
                Vertex<Point_3> hS = this.S.vertices.get(h.vertex.tag);
                Vertex<Point_3> hoS = this.S.vertices.get(h.opposite.vertex.tag);

	            if (Math.abs((double)h.vertex.getPoint().distanceFrom(h.opposite.vertex.getPoint()) - (double)hS.getPoint().distanceFrom(hoS.getPoint()))>this.epsilon) {	                
	                return false;
                }
                h.tag = 1;
	            h.opposite.tag =1;
            }
        }

        return true ;

        
    }

    /*Check the existence of overlapping edges*/
    public int isOverlapping(){
    	//Fenetre f = new Fenetre();
        resetTag2D(this.M);
        int res = 0;
        this.intersection = new ArrayList<ArrayList<Integer>>();
        for(int i = 0;i < this.M.halfedges.size();i++){
        	this.intersection.add(new ArrayList<Integer>());
        }
        int i = 0;//créer les index des halfedge
        for(Halfedge<Point_2> h : M.halfedges){
        	h.index = i++;
        }

        for (Halfedge<Point_2> h : M.halfedges){
        	if(h.tag == 0){
            for (Halfedge<Point_2> t : M.halfedges){
            	if(t.tag == 0){
            	Point_2 p11,p12,p21,p22;
            	double xMax,xMin,yMax,yMin;
            	p11 = (Point_2)h.getVertex().getPoint();
            	p12 = (Point_2)h.opposite.vertex.getPoint();
            	p21 = (Point_2)t.getVertex().getPoint();
            	p22 = (Point_2)t.opposite.vertex.getPoint();
            	xMax = Math.max(Math.max(p11.x, p21.x),Math.max(p12.x, p22.x));
            	xMin = Math.min(Math.min(p11.x, p21.x),Math.min(p12.x, p22.x));
            	yMax = Math.max(Math.max(p11.y, p21.y),Math.max(p12.y, p22.y));
            	yMin = Math.min(Math.min(p11.y, p21.y),Math.min(p12.y, p22.y));
            	
            	
            	if((!(p11.equals(p21)||p11.equals(p22)||p12.equals(p21)||p12.equals(p22))) &&//segment successifs
            			!(Math.min(p11.x, p12.x)>Math.max(p21.x, p22.x)) &&//segments alignés mais distincts
            			!(Math.min(p21.x, p22.x)>Math.max(p11.x, p12.x)) &&
            			!(Math.min(p11.y, p12.y)>Math.max(p21.y, p22.y)) &&
            			!(Math.min(p21.y, p22.y)>Math.max(p11.y, p12.y)) ){ 
            		Point_2 pI = intersect(new Segment_2((Point_2)t.vertex.getPoint(),(Point_2)t.opposite.vertex.getPoint()), new Segment_2((Point_2)h.vertex.getPoint(),(Point_2)h.opposite.vertex.getPoint())); // intersection entre les deux droites
            		if(!(pI.x >= xMax || pI.x <= xMin || pI.y >= yMax||pI.y <= yMin)){
            			
            			if(!intersection.get(h.opposite.index).contains(t.index) &&
            					!intersection.get(h.index).contains(t.opposite.index)){
            					intersection.get(h.index).add(t.index);//agit que siopposé non traité
            					res++;//on a une intersection
            			}
            			
            		}
            	}
            }}
            h.tag=1;
            h.opposite.tag =1;//don't check twice
        }}
        return res;
    }



    private Halfedge<Point_2> splitEdge(Halfedge<Point_2> h, Point_2 point){
        // create the new edges, faces and vertex to be added
        Halfedge<Point_2> hNewLeft=new Halfedge<>();
        Halfedge<Point_2> hNewRight=new Halfedge<>();
        Vertex<Point_2> newVertex= new Vertex<>(point);

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

        return hNewLeft;
    }

    public void correspondance(){
        TC.ecritureDansNouveauFichier("correspondance.off");
        for (Vertex<Point_2> v : this.M.vertices){
            //System.out.println(v.index+"       "+v.tag+"      "+v);
            TC.println(v.tag);//augmente
        }
    }
	
    /*calculates cut tree with minimum perimeter heuristic*/
    public Hashtable<Integer, Halfedge<Point_3>> minimumParameter(){
    	   LinkedList<Halfedge<Point_3>> L = UnionFind.kruskal(new UnionFind(), this.S, new lengthComparator());
    	   Hashtable<Integer, Halfedge<Point_3>> ht = new Hashtable<>();
    	   for(Halfedge<Point_3> h : L) ht.put(h.hashCode(),h);
    	   return ht;
    }
    
    /*calculates cut tree with flat spanning heuristic*/
    public Hashtable<Integer, Halfedge<Point_3>> flatSpanning(){
    		LinkedList<Halfedge<Point_3>> L = UnionFind.kruskal(new UnionFind(), this.S, new DirectionComparator(this.S.halfedges.get(0)));
 	   		Hashtable<Integer, Halfedge<Point_3>> ht = new Hashtable<>();
 	   		for(Halfedge<Point_3> h : L) ht.put(h.hashCode(),h);
 	   		return ht;
    }
    
    /*Removes overlaps from M using greedy algorithm to minimize cuts*/
    public void removeOverlaps(){
    	int i = 0;
    	for(Polyhedron_3<Point_2> P : RemoveOverlaps.removeOverlaps(this.intersection,this.M)){
    		Mesh2DToOff(P,i+".off");
    		ShowPlanarUnfolding.draw2D("results/2D_"+(i++)+".off");
    	}    	
    }
}
