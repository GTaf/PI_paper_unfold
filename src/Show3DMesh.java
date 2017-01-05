import processing.core.*;

import java.io.File;

/**
 * A simple 3d viewer for visualizing surface meshes (based on Processing)
 * 
 * @author Luca Castelli Aleardi (INF555, 2012)
 *
 */
public class Show3DMesh extends PApplet {

	SurfaceMesh mesh; // 3d surface mesh
	int renderType=0; // choice of type of rendering
	int renderModes=3; // number of rendering modes
	public static String filename; // input file (OFF)
	
	public void setup() {
		  size(800,600,P3D);
		  ArcBall arcball = new ArcBall(this);
		  
		  this.mesh=new SurfaceMesh(this, filename);
	}
		 
		public void draw() {
		  background(255);
		  //this.lights();
		  directionalLight(101, 204, 255, -1, 0, 0);
		  directionalLight(51, 102, 126, 0, -1, 0);
		  directionalLight(51, 102, 126, 0, 0, -1);
		  directionalLight(102, 50, 126, 1, 0, 0);
		  directionalLight(51, 50, 102, 0, 1, 0);
		  directionalLight(51, 50, 102, 0, 0, 1);
		 
		  translate(width/2.f,height/2.f,-1*height/2.f);
		  this.strokeWeight(1);
		  stroke(150,150,150);
		  
		  this.mesh.draw(renderType);
		  this.drawOptions();
		}
		
		public void drawOptions() {
			int hF=12;
			fill(255);
			this.textMode(this.SCREEN);
			this.text("'i' or 'o' for zooming", 10, hF*1);
			this.text("'r' for chaning rendering mode", 10, hF*2);
		}
		
		public void keyPressed(){
			  switch(key) {
			    case('i'):case('I'): this.zoomIn(); break;
			    case('o'):case('O'): this.zoomOut(); break;
			    case('r'):this.renderType=(this.renderType+1)%this.renderModes; break;
			  }
		}
		
		public void zoomIn() {
			this.mesh.zoom=this.mesh.zoom*1.5;
		}

		public void zoomOut() {
			this.mesh.zoom=this.mesh.zoom*0.75;
		}

		public static void setInputFile(String input) {
			if(input==null) { 
				System.out.println("Error: wrong input file "+input);
				System.exit(0);
			}
			File file = new File(input);
			if(file.exists()==false) { 
				System.out.println("Wrong input file: "+input+" not found");
				System.exit(0);
			}
			if(input.endsWith(".off")==false) { 
				System.out.println("Error: wrong input format "+input+" (.off required)");
				System.exit(0);
			}
			filename=input;
		}

		/**
		 * For running the PApplet as Java application
		 */
		public static void main(String args[]) {
			/*if(args.length!=1) {
				System.out.println("Wrong number of input parameters: required one file .off");
				System.exit(0);
			}
			Show3DMesh.setInputFile(args[0]);*/
			
			Show3DMesh.setInputFile("OFF/bunny_small.off");
			
			PApplet.main(new String[] { "Show3DMesh" });
		}

}
