import java.util.*;

import Jcg.geometry.*;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;
import tc.TC;

import static Jcg.geometry.GeometricOperations_2.doIntersect;

public class Unfold {
    private Polyhedron_3<Point_2> M; // Patron du polyedre
    private Polyhedron_3<Point_3> S; // Polyedre original
    private DoubleHashMap<Halfedge<Point_2> ,Halfedge<Point_3>> plani ; //correlation entre S et M
    private float epsilon ; //numeric tolerance
    private boolean BFS;
    private String filename;


    private Unfold(String fichier) {
    	this.filename = fichier;
        this.S = MeshLoader.getSurfaceMesh("C:\\Users\\Marion letilly\\IdeaProjects\\unfold\\Pi_paper_unfold\\OFF\\"+fichier);
        this.BFS = true;
        this.epsilon = (float)0.1;
    }

    public static void main(String[] args) {
    	String filename = "cube.off";
        Unfold U = new Unfold(filename);   
        // mettre dans le OFF
        U.Mesh2DToOff();
        //U.correspondance();
        ShowPlanarUnfolding.draw2D("results/2D_"+filename);
        System.out.println(U.isValid());
        //System.out.println(U.isIsometric());
        if (!U.isOverlapping())
            System.out.println("The unfolding is not overlapping");
    }

    /* Put a 2D mesh into an OFF file format and compute the correspondance betwenn vertices from S and M into an OFF file*/
    private void Mesh2DToOff() {
        this.computeM();//compute the unfolding
        resetTag2D(this.M);
        resetIndex2D(this.M);

        TC.ecritureDansNouveauFichier("C:\\Users\\Marion letilly\\IdeaProjects\\unfold\\Pi_paper_unfold\\results/2D_"+this.filename);
        TC.println("OFF");//premiere ligne
        TC.println(this.M.vertices.size()+" "+this.M.facets.size()+" 0");//nombre de trucs
        int i = 0;
        for(Vertex<Point_2> v : M.vertices){//ajoute les points et leur donne un index
            v.index = i++;
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
    private void computeM() {
        // creer le cut tree
        Hashtable<Integer, Halfedge<Point_3>> cutTree = this.computeCutTree();

        // couper le mesh et le mettre a  plat
        this.cutMesh(cutTree);
    }

    /* Compute BFS or DFS cut tree */
    private Hashtable<Integer, Halfedge<Point_3>> computeCutTree() {
        Hashtable<Integer, Halfedge<Point_3>> ht = new Hashtable<Integer, Halfedge<Point_3>>();
        LinkedList<Vertex<Point_3>> queue = new LinkedList<Vertex<Point_3>>();

        resetTag3D(this.S); // met les tags a  0 : pas encore visite

        if (this.S.vertices.size() == 0)
            return null;


        queue.addFirst(this.S.vertices.get(0)); // ajout initial du premier halfedge
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
                    H.opposite.vertex.tag = 1;// va etre traite
                    if(this.BFS)//BFS ou DFS
                    	queue.addLast(H.opposite.vertex);
                    else
                    	queue.addFirst(H.opposite.vertex);
                }
                H = H.next.opposite;
            }
        }
        return ht;
    }

	/* Cut the mesh according to the cut Tree given DONE*/

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
        this.plani = new DoubleHashMap<Halfedge<Point_2> ,Halfedge<Point_3>>();
        System.out.println(parcours);

        this.M = new Polyhedron_3<Point_2>();
        //traite  part la première face
        System.out.println("Face n0 : "+parcours.getFirst());
        this.firstTo2D(parcours.removeFirst(),plani);

        
        for(Face<Point_3> f : parcours){
            this.to2D(f,plani);
            //System.out.println(f.getEdge());
        }
        /*
        for(int i = 0; i <6;i++){
        	this.to2D(parcours.get(i), plani);
        	//System.out.println("Face n"+(i+1)+" : "+parcours.get(i));
        }*/
        
        //plani.values();
        
        

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
        //System.out.println(p1.minus(p2).getCartesian(1));

        //calcul des points
        //premier point
        double teta = Math.acos(costeta(pp1,pp2,pp3));
        teta = teta+psi;
        //System.out.println("tetat : "+teta+"    psi : "+psi+"     teta"+(teta-psi));
        //System.out.println(p1+"           "+p2);
        p3 = new Point_2(Math.cos(teta)*(double)pp3.distanceFrom(pp1), Math.sin(teta)*(double)pp3.distanceFrom(pp1));
        p3.translateOf(p1.minus(new Point_2(0,0)).opposite());
        Halfedge<Point_2> h = plani.get(f.getEdge().getOpposite()).getOpposite();//celui de la face trait�e
        System.out.println("\n"+ h);
        System.out.println(h);
        this.M.addTriangleToBorder(h,p3);
        //ajouter hachage
        plani.put(h, f.getEdge());
        plani.put(h.next, f.getEdge().next);
        plani.put(h.next.next, f.getEdge().next.next);
                
        //autres points
        Halfedge<Point_3> H = f.getEdge();
        H = H.next.next; //va au sommet non traite
        System.out.println("H : "+H);
        
        Face<Point_2> F =plani.get(f.getEdge().getOpposite()).getOpposite().getFace();//face 2D
        //System.out.println(F+"           "+F.getEdge());
        
        
        F.setEdge(F.getEdge().next);//change le edge de reference
        //System.out.println(F+"           "+F.getEdge());
        
        
        //this.Mesh2DToOff2();
        //System.out.println(U.M.edgesToString());
        //ShowPlanarUnfolding.draw2D("2dmesh.off");
        
        while(H.next != f.getEdge()){
            Point_3 pp = H.vertex.getPoint(); //point �  ajouter au mesh
            System.out.println(pp1);
            double d = (double)pp.distanceFrom(pp1);//distance entre p et l'origine
            teta = Math.acos(costeta(pp1,pp2,pp));
            teta = teta+psi;
            Point_2 p = new Point_2(Math.cos(teta)*d, Math.sin(teta)*d);//point �  calculer en 2D, connaissant le hlafedge precedent H.previous
            p.translateOf(p1.minus(new Point_2(0,0)).opposite());
            //System.out.println(F.getEdge());
            plani.put(this.splitEdge(F.getEdge().prev.prev, p),H);

            H = H.next;
            //ne pas oublier le hasmap
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
            if (h.vertex.getPoint().x==0 && h.vertex.getPoint().y == 0); //plani.put(h,f.getEdge());
            else if (h.vertex.getPoint().y == 0) plani.put(h, f.getEdge().next);
            else plani.put(h,f.getEdge().next.next);
            h=h.next;
        }

        Halfedge<Point_3> H = f.getEdge();
        H = H.next.next.next; //va au sommet non traite
        this.M.facets.get(0).setEdge(this.M.facets.get(0).getEdge().next);//change le edge de reference

        while(H != f.getEdge()){
            Point_3 pp = H.vertex.getPoint(); //point �  ajouter au mesh
            double d = (double)pp.distanceFrom(pp1);//distance entre p et l'origine
            Point_2 p = new Point_2(costeta(pp1,pp2,pp)*d, sinteta(pp1,pp2,pp)*d);//point �  calculer en 2D, connaissant le hlafedge precedent H.previous
            plani.put(this.splitEdge(this.M.facets.get(0).getEdge().prev, p),H);

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
	 * pour chaque ar�te, je vais chercher le sommet correspondant dans S
	 * si la distance est fausse, je  sors
	 * sinon je traite tout*/
    public boolean isIsometric(){
        resetTag2D(this.M);
        this.correspondance();

        for (Vertex<Point_2> v : this.M.vertices ){
            Halfedge<Point_2> h = v.getHalfedge();
            Halfedge<Point_2> H = h.next;
            int s1 = 0 ; //index of the corresponding vertex in S
            for (int i = 0 ; i < v.index + 1 ; i++ ){
                TC.lectureDansFichier("correspondance.off");
                s1=TC.lireInt();
            }
            Vertex<Point_3> vS = this.S.vertices.get(s1);

            //cas initial
            if ( H.tag == 0){
                int s2 = 0 ;
                for (int i = 0 ; i < H.vertex.index + 1 ; i++ ){
                    TC.lectureDansFichier("correspondance.off");
                    s2=TC.lireInt();
                }
                Vertex<Point_3> HS = this.S.vertices.get(s2);
                if (Math.abs((double)v.getPoint().distanceFrom(H.vertex.getPoint()) - (double)vS.getPoint().distanceFrom(HS.getPoint()))>this.epsilon)
                    return false;
                H.tag=1;
                H.opposite.tag=1; //avoid checking a length twice
                H=H.opposite.next;
            }
            while ( H != h ) {
                if ( H.tag == 0){
                    int s2 = 0;
                    for (int i = 0 ; i < H.vertex.index + 1 ; i++ ){
                        TC.lectureDansFichier("correspondance.off");
                        s2=TC.lireInt();
                    }
                    Vertex<Point_3> HS = S.vertices.get(s2);
                    if (Math.abs((double)v.getPoint().distanceFrom(H.vertex.getPoint()) - (double)vS.getPoint().distanceFrom(HS.getPoint()))>this.epsilon)
                        return false;
                    H.tag=1;
                    H.opposite.tag=1; //avoid checking a length twice
                }
                H=H.opposite.next;
            }
        }

        return true;//each edges has been checked
    }

    /*Check the existence of overlapping edges*/
    public boolean isOverlapping(){
        resetTag2D(this.M);
        for (Halfedge h : M.halfedges){
            for (Halfedge t : M.halfedges){
                if (t.tag != 0){
                    if (doIntersect(new Segment_2((Point_2)h.vertex.getPoint(),(Point_2)h.opposite.vertex.getPoint()), new Segment_2((Point_2)t.vertex.getPoint(),(Point_2)t.opposite.vertex.getPoint())));
                    return true;
                }
            }
            h.tag=0; //don't check twice
        }
        return false;
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
    
    
    
    /* Put a 2D mesh into an OFF file format and compute the correspondance betwenn vertices from S and M into an OFF file*/
    public void Mesh2DToOff2() {
        resetTag2D(this.M);
        resetIndex2D(this.M);

        TC.ecritureDansNouveauFichier("2dmesh.off");
        TC.println("OFF");//premiere ligne
        TC.println(this.M.vertices.size()+" "+this.M.facets.size()+" 0");//nombre de trucs
        int i = 0;
        for(Vertex<Point_2> v : M.vertices){//ajoute les points et leur donne un index
            v.index = i++;
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
            //TC.println(this.plani.get(v.getHalfedge()).vertex.index);
        }

    }

    public void correspondance(){
        TC.ecritureDansNouveauFichier("correspondance.off");
        for (Vertex<Point_2> v : this.M.vertices){
            TC.println(this.plani.get2(v.getHalfedge()).vertex.index);
        }
    }





}