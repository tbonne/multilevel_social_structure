package multiLevelGroups;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;

public class Cell {

	//geometry
	Context context;
	Geography geog;
	Geometry geom;
	Coordinate coord; //centroid

	//resources
	double resources;
	double maxResources;
	double timeCounter=0;
	ArrayList<Cell> visibleSites;
	int id;
	ArrayList<Double> familiarityValues;
	ArrayList<Double> familiarityValues_work;
	ArrayList<OMU> familiarityIDs;


	/****************************Construct a Cell************************************/

	public Cell(Context con,double x,double y, double r, int i, Geography g){

		//set initial variables
		id=i;
		resources = r;
		maxResources = resources; //maintains initial conditions
		visibleSites = new ArrayList<Cell>();
		con.add(this);

		//place the cell on a gis landscape
		geog = g;
		geom = getHexShape(x,y);

		geog.move(this, geom);//sets the polygon onto the geography surface
		this.setCoord(geom.getCentroid().getCoordinate());
		timeCounter=0;

		familiarityValues = new ArrayList<Double>();
		familiarityValues_work = new ArrayList<Double>();
		familiarityIDs = new ArrayList<OMU>();

	}

	private Geometry getHexShape(double x, double y){

		GeometryFactory fac = new GeometryFactory();
		Geometry shape = null;
		Coordinate[] boundingCoords = new Coordinate[7];
		for(int i=0; i<7; i++) {
			if(i!=6){
				Coordinate c = new Coordinate(x + (Params.cellSize/(2.0))*Math.cos(i*2*Math.PI/6), y + (Params.cellSize/(2.0))*Math.sin(i*2*Math.PI/6));
				boundingCoords[i]=c;
			} else{
				Coordinate c = new Coordinate(x + (Params.cellSize/(2.0))*Math.cos(0*2*Math.PI/6), y + (Params.cellSize/(2.0))*Math.sin(0*2*Math.PI/6));
				boundingCoords[i]=c;
			}
		}
		LinearRing ring = fac.createLinearRing(boundingCoords);
		shape = fac.createPolygon(ring, null);

		return shape;
	}

	/****************************Cell methods************************************/

	public void stepThreaded(){
		regrow();  
		change(); 
	}

	private void regrow(){
		
		//check to see if resource is at max levels
		if (resources!=maxResources){
			//the resource will grow back until it's maximum level is reached
			if (resources<maxResources-Params.regrowthRate){
				resources = (resources+Params.regrowthRate);
			} else {
				resources = (maxResources);
			}
			//will degenerate if above max resource level (seasonality)
			if(resources>maxResources+Params.regrowthRate){
				resources = (resources-Params.regrowthRate);
			}else if (resources>maxResources && resources<maxResources+Params.regrowthRate){
				resources = maxResources;
			}
		} else {
			ModelSetup.removeCellToUpdate(this);
		}

	}

	private void change(){

		ArrayList<OMU> toRemove = new ArrayList<OMU>();

		//decrease all familiarity values
		for(int i =0;i<familiarityValues.size();i++){
			
			//////decrease reference memory/////
			
			double f = familiarityValues.get(i);
			
			if(f<=0.001){
				f=0.001;
				toRemove.add(familiarityIDs.get(i)); //if reference memory is gone... remove the cell from memory
				
			} else {

				f = Math.max( (f - Params.lDecay * (1+f)*f),0.001); //
			
			}
			
			familiarityValues.set(i, f);
			
			
			//////decrease working memory////
			
			double f_work = familiarityValues_work.get(i);
			
			//if(this.id == 8853){
			//if(f_work>0.08) {
			//	System.out.println(this.id + " cell has ref mem = "+ f + " and work mem = "+f_work);
			//}
			
			if(f_work<=0.001){
				f_work=0.001;
			} else {
				f_work = Math.max( (f_work - Params.lDecay_work * (1+f_work)*f_work),0.001); //
			}
			
			familiarityValues_work.set(i, f_work);
			
			
		}
		
		
		

		//remove any at or below familiarity min
		for(OMU omu : toRemove){
			int index = familiarityIDs.indexOf(omu);
			familiarityIDs.remove(index);
			familiarityValues.remove(index);
			familiarityValues_work.remove(index);
			omu.removeFamilarFood(this);
		}
	}

	public void updateFam(OMU omu, Boolean reference_only){

		//if already in the array increase value
		if(familiarityIDs.contains(omu)){

			int  i = familiarityIDs.indexOf(omu);
			familiarityValues.set(i, 0.999); //increase reference memory
			if (reference_only==false)familiarityValues_work.set(i, 0.999); //only update working memory if I'm in the cell

		//add new individual and increase value
		} else {
			
			familiarityIDs.add(omu);
			familiarityValues.add(0.999); //increase reference memory
			familiarityValues_work.add(0.001); //never seen before so working memory is zero
			omu.addFamilarFood(this);
		
		}
	}
	
	public double getMemR(OMU omu) {
		int  i = familiarityIDs.indexOf(omu);
		if(i<0) {
			System.out.println("new cell that i have no memory of!");
			return 0.1;
		} else {
			return familiarityValues.get(i);
		}
		
	}
	
	public double getMemW(OMU omu) {
		int  i = familiarityIDs.indexOf(omu);
		if(i<0) {
			return 0;
		} else {
			return familiarityValues_work.get(i);
		}
	}

	public void setVisibleNeigh(){
		Envelope envelope = new Envelope();
		envelope.init(coord);
		envelope.expandBy(Params.foodSearchRange);
		Iterable neigh = geog.getObjectsWithin(envelope, Cell.class);
		while (neigh.iterator().hasNext()){
			visibleSites.add((Cell)neigh.iterator().next());
		}
		envelope.setToNull();
	}

	public synchronized double eatMe(){

		double depletion = 0;
		if(resources-Params.depletionRate > 0){
			depletion = Params.depletionRate;
			resources = resources - depletion;
		} else{
			depletion = resources;
			resources = 0;
		}

		ModelSetup.addToCellUpdateList(this);

		return depletion;
	}

	/****************************Get and set methods************************************/

	public double getFamiliarity(OMU omu){
		if(familiarityIDs.contains(omu)){
			int index = familiarityIDs.indexOf(omu);
			return familiarityValues.get(index);
		} else {
			return Params.famMinCell;
		}
	}

	public double getResourceLevel(){
		return resources;
	}
	public void setResourceLevel(double r){
		resources = r;
	}
	public double getMaxResourceLevel(){
		return maxResources;
	}
	public void setMaxResourceLevel(double rm){
		maxResources = rm;
	}
	public Coordinate getCoord(){
		return coord;
	}
	private void setCoord(Coordinate c){
		coord = c;
	}
	public int getId(){
		return id;
	}


}
