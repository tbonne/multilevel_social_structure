package multiLevelGroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import jsc.distributions.Beta;
import jsc.distributions.PowerFunction;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import cern.jet.random.Distributions;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactory;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;

public class ModelSetup implements ContextBuilder<Object> 	{

	private static Context mainContext;
	private static Geography geog;

	private static double resAdded;
	public static ArrayList<Cell> allCells;
	public static ArrayList<Cell> cellsToProcess;
	public static ArrayList<Cell> removeCellsToProcess;
	public static ArrayList<OMU> allOMUs;
	public static int id_count;
	public static Normal depletion_dist;


	public Context<Object> build(Context<Object> context){

		System.out.println("Running multi-level group formation model");
		Params p = new Params(); 

		/********************************
		 * 								*
		 * initialize model parameters	*
		 * 								*
		 * ******************************/

		geog=null;
		mainContext = context; //static link to context
		resAdded=0;
		id_count=0;
		allCells = new ArrayList<Cell>();
		cellsToProcess = new ArrayList<Cell>();
		removeCellsToProcess = new ArrayList<Cell>();
		allOMUs = new ArrayList<OMU>();
		depletion_dist = RandomHelper.createNormal(Params.depletionRate/10, Params.depletionSD); //agent internal energy depletion is 10 times less than energy depletion in the environment. Can take one "bite" and it will last the agent 10 steps.



		/****************************
		 * 							*
		 * Building the landscape	*
		 * 							*
		 * *************************/

		//Create Geometry factory; used to create GIS shapes (points=OMU; polygons=resources)
		GeometryFactory fac = new GeometryFactory();

		//Create Geography/GIS 
		GeographyParameters<Object> params= new GeographyParameters<Object>();
		GeographyFactory factory = GeographyFactoryFinder.createGeographyFactory(null);
		geog = factory.createGeography("geog", context, params);
		geog.setCRS("EPSG:32636"); //WGS 84 / UTM zone 36N EPSG:32636


		//x and y dims of the map file
		int xdim = Params.landscapeWidth;
		int ydim = Params.landscapeWidth;


		//adding hexagon grid cells
		Beta beta = new Beta(Params.envHomogen,Params.envHomogen);
		MersenneTwister mt = new MersenneTwister();
		
		double xcoord=0,ycoord=0;
		int offset=0,count=0;

		for (int i = 0; i < ydim; ++i) {
			for (int j = 0; j < xdim; ++j) {

				//double food = 0;
				//double food = beta.random();
				double food = 1/Distributions.nextPowLaw(Params.envHomogen, 10, mt);
				Cell cell=null;

				if(offset==0){
					xcoord=xcoord+(Params.cellSize/2.0)*Math.cos(2*Math.PI/3.0);
					offset=1;
					cell = new Cell(context,xcoord,ycoord,food,count++,geog);
					xcoord=xcoord-(Params.cellSize/2.0);
				}else if (offset==1){
					xcoord=xcoord-(Params.cellSize/2.0)*Math.cos(2*Math.PI/3.0);
					ycoord=ycoord+(Params.cellSize/2.0)*Math.sin(2*Math.PI/3.0);
					offset=0;
					cell = new Cell(context,xcoord,ycoord,food,count++,geog);
					ycoord=ycoord-(Params.cellSize/2.0)*Math.sin(2*Math.PI/3.0);
				}

				resAdded=resAdded+food;
				allCells.add(cell);

				//shift ycoord by cell size value
				xcoord=xcoord+Params.cellSize;
			}

			//Set ycoord back to the start and shift xcoord up by cell size value
			xcoord=0;
			ycoord = ycoord-(Params.cellSize)*Math.sin(2*Math.PI/3.0);
		}

		//to simplify the model all cells record the visible neighbours (within visible range for a primate)
		for (Cell c: allCells){
			c.setVisibleNeigh();
		}
		
		//standardize cell resources to be the same for every run
		setTotalResources(allCells.size());
		
		
		/************************************
		 * 							        *
		 * Adding individuals to the landscape		*
		 * 							        *
		 * *********************************/

		//add individuals
		Collections.shuffle(allCells);
		Cell center = allCells.get(0);
		for (int j = 0; j < Params.numbOMU; j++){

			int randId = RandomHelper.nextIntFromTo(0, allCells.size()-1);
			Coordinate coord = allCells.get(randId).getCoord();
			//Coordinate coord = center.getCoord();
			OMU group = new OMU(coord,id_count);
			id_count++;
			allOMUs.add(group);
			context.add(group);
			Point geom = fac.createPoint(coord);
			geog.move(group, geom);

		}
		
		System.out.println("Done adding cells");

		/************************************
		 * 							        *
		 * create the scheduling			*
		 * 							        *
		 * *********************************/

		//executor takes care of the processing of the schedule
		Executor executor = new Executor();
		createSchedule(executor);
		RunEnvironment.getInstance().endAt(Params.endTime);


		/************************************
		 * 							        *
		 * Adding observer 					*
		 * 							        *
		 * *********************************/

		return context;

	}


	/************************************** Model Setup Scehduling ******************************************/

	private void createSchedule(Executor executor){

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		ScheduleParameters agentStepDParamsPrimate = ScheduleParameters.createRepeating(1, 1, 3); //start, interval, priority (high number = higher priority)
		schedule.schedule(agentStepDParamsPrimate,executor,"makeDecisions");

		ScheduleParameters agentStepAParamsPrimate = ScheduleParameters.createRepeating(1, 1, 2); //start, interval, priority (high number = higher priority)
		schedule.schedule(agentStepAParamsPrimate,executor,"makeAMove");

		ScheduleParameters agentStepParams = ScheduleParameters.createRepeating(1, 1, 1);
		schedule.schedule(agentStepParams,executor,"updateCells");
		
		ScheduleParameters agentStepCParams = ScheduleParameters.createRepeating(Params.turnover_time, Params.turnover_time, 1);
		schedule.schedule(agentStepCParams,executor,"populationTurnover");
		
		ScheduleParameters agentStepEParams = ScheduleParameters.createRepeating(Params.spatialAssoStartTime, 100, 1);
		schedule.schedule(agentStepEParams,executor,"spatialAssociations");
		
		ScheduleParameters endModel = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
		schedule.schedule(endModel,executor,"endModel");
	}

	//used to update only the cells which have been modified
	public static void removeCellsUpdated(){
		for(Cell c : removeCellsToProcess){
			cellsToProcess.remove(c);
		}
		removeCellsToProcess.clear();
	}
	
	//used to set total resources in the model
	private void setTotalResources(int numbCells){

		//calculate the percent difference between the total resource level now and the target level
		double targetRes = numbCells*Params.foodDensity;
		double perDiff = resAdded / targetRes;
		System.out.println("conversion = "+perDiff);
		double newTotal=0;
		int count=0;

		//Divide each resource by the percent difference to make the total equal the target resource amount
		for (Cell c : this.getAllCells()){
			c.setMaxResourceLevel(c.getMaxResourceLevel()/perDiff);
			c.setResourceLevel(c.getResourceLevel()/perDiff);
			newTotal = newTotal + c.getResourceLevel();
			count++;
		}

		System.out.println("total amount of food added = "+ resAdded + ",  updated to = " + newTotal);
	}


	/*******************************************get and set methods***********************************************/

	public static ArrayList<OMU> getAllOMUs(){
		Collections.shuffle(allOMUs);
		return allOMUs;		
	}
	
	public static void removeOMU(OMU allind){
		allOMUs.remove(allind);
	}

	public static ArrayList<Cell> getAllCells(){
		Collections.shuffle(allCells);
		return allCells;		
	}

	public static Geography getGeog(){
		return geog;
	}

	public synchronized static void addToCellUpdateList(Cell c){  
		if(cellsToProcess.contains(c)==false)cellsToProcess.add(c);
	}

	public synchronized static void removeCellToUpdate(Cell c){
		removeCellsToProcess.add(c);
	}

	public static ArrayList<Cell> getCellsToProcess(){
		return cellsToProcess;
	}
	public static Context getContext(){
		return mainContext;
	}
	public static double getDepletionRate() {
		return (Math.abs(depletion_dist.nextDouble())); 
	}

}
