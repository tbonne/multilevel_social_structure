package multiLevelGroups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Executor {

	private static final int pThreads = Params.numberOfThreads;
	private static ExecutorService executor;
	private static GeometryFactory fac;

	public Executor(){
		executor = Executors.newFixedThreadPool(pThreads);
		fac = new GeometryFactory();
	}

	/************************************** synchronous scheduling **********************************/

	public static void makeDecisions(){
		//System.out.println("Starting input threads");

		Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
		Iterator<OMU> primateList = ModelSetup.getAllOMUs().iterator();
		while (primateList.hasNext()){
			Runnable worker = new Runnable_OMUs(primateList.next());
			tasks.add(Executors.callable(worker,(Void)null));
		}

		// Wait until all threads are finish
		try {
			for (Future<?> f : executor.invokeAll(tasks)) { //invokeAll() blocks until ALL tasks submitted to executor complete
				f.get(); 
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
			System.out.println("null in inputs");
		}

		//System.out.println("Finished all input threads");

	}

	public static void makeAMove(){

		Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
		Iterator<OMU> primateList = ModelSetup.getAllOMUs().iterator();
		while (primateList.hasNext()){
			Runnable worker = new Runnable_OMUs_move(primateList.next());
			tasks.add(Executors.callable(worker,(Void)null));
		}

		// Wait until all threads are finish
		try {
			for (Future<?> f : executor.invokeAll(tasks)) { //invokeAll() blocks until ALL tasks submitted to executor complete
				f.get(); 
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
			System.out.println("null in inputs");
		}

		//System.out.println("Finished all move threads");

	}

	public static void updateCells(){
		//System.out.println("Starting cell threads");

		Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
		Iterator<Cell> cellList = ModelSetup.getAllCells().iterator();
		while (cellList.hasNext()){
			Runnable worker = new Runnable_Cells(cellList.next());
			tasks.add(Executors.callable(worker,(Void)null));
		}

		// Wait until all threads are finish
		try {
			for (Future<?> f : executor.invokeAll(tasks)) { //invokeAll() blocks until ALL tasks submitted to executor complete
				f.get(); 
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
			System.out.println("null in inputs");
		}

		//System.out.println("Finished all cell threads");

	}
	
	public static void populationTurnover(){
		ArrayList<OMU> allInds = ModelSetup.getAllOMUs();
		ArrayList<OMU> indsToRemove = new ArrayList<OMU>();
		ArrayList<OMU> indsToBirth = new ArrayList<OMU>();
		
		//get individuals to remove (random)
		for(int i = 0; i < Params.turnover ; i++){
			indsToRemove.add(allInds.get(i)); 
		}
		
		//remove those individuals
		for(OMU ind : indsToRemove){
			ModelSetup.getContext().remove(ind);
			ModelSetup.getAllOMUs().remove(ind);
		}
		
		//get female individuals to give birth
		int count = 0, row=0;
		while(count < Params.turnover && row<allInds.size()){
			//if(allInds.get(row).sex == 1){
				indsToBirth.add(allInds.get(row));
				count++;
			//}
			row++;
		}
		
		//selected female individuals reproduce
		for(OMU ind : indsToBirth){
			OMU newInd = new OMU(ind.getMyCoord(),ind, ModelSetup.id_count);
			ModelSetup.id_count++;
			ModelSetup.getContext().add(newInd);
			ModelSetup.getAllOMUs().add(newInd);
			Point geom = fac.createPoint(ind.getMyCoord());
			ModelSetup.getGeog().move(newInd, geom);
		}
		
		//Record network patterns to date
		Observer.recordInfluencePatterns(allInds);
		Observer.recordSpatialPatterns(allInds);
		Observer.recordHomeRangePatterns(allInds);
		
		System.out.println("Turnover");
		
	}
	
	public static void spatialAssociations(){
		
		ArrayList<OMU> allInds = ModelSetup.getAllOMUs();
		
		for(OMU focal : allInds){
			
			for(OMU other: allInds){
				
				if(focal.getMyCoord().distance(other.myCoord)<Params.spatialRangeAsso && focal != other){
					
					if(focal.getSpatialAssoInds().contains(other)){
						
						int indexOfOther = focal.getSpatialAssoInds().indexOf(other);
						int valueOfOther = focal.getSpatialAssoVal().get(indexOfOther);
						
						focal.getSpatialAssoVal().set(indexOfOther,valueOfOther+1);
						
					} else {
						
						focal.getSpatialAssoInds().add(other);
						focal.getSpatialAssoVal().add(1);
						
					}
					
				}
			}
			
			
			
		}
		
		
		
	}

	public static void endModel(){

		//Observer.recordInfluencePatterns();
		//Observer.recordSpatialPatterns();
		//Observer.recordHomeRangePatterns();
		System.exit(0); //kills the program

	}



	/************************************** n - asynchronous scheduling **********************************/


	public static void processPrimates_decision(){

		//System.out.println("processing primates");
		
		//get all primates into an iterator 
		Iterator<OMU> OMUList = ModelSetup.getAllOMUs().iterator();

		//for all primates in order
		while (OMUList.hasNext()){

			//get subsample of primates
			ArrayList<OMU> OMUToProcess = new ArrayList<OMU>();
			for (int i=0; i<Params.numberOfThreads;i++){
				try{
					OMUToProcess.add(OMUList.next());
				} catch (java.util.NoSuchElementException e){

				}
			}

			//process subsample of the primate population

			//inputs prior to action
			Collection<Callable<Void>> tasks_inputs = new ArrayList<Callable<Void>>();
			for (OMU p:OMUToProcess){
				Runnable worker = new Runnable_OMUs(p);
				tasks_inputs.add(Executors.callable(worker,(Void)null));
			}

			try {
				for (Future<?> f : executor.invokeAll(tasks_inputs)) { //invokeAll() blocks until ALL tasks submitted to executor complete
					f.get(); 
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}catch (NullPointerException e){
				e.printStackTrace();
			}
		}

	}

	public static void processPrimates_action(){

		//System.out.println("processing primates");
		
		//get all primates into an iterator 
		Iterator<OMU> OMUList = ModelSetup.getAllOMUs().iterator();

		//for all primates in order
		while (OMUList.hasNext()){

			//get subsample of primates
			ArrayList<OMU> OMUToProcess = new ArrayList<OMU>();
			for (int i=0; i<Params.numberOfThreads;i++){
				try{
					OMUToProcess.add(OMUList.next());
				} catch (java.util.NoSuchElementException e){

				}
			}

			//process subsample of the primate population

			//inputs prior to action
			Collection<Callable<Void>> tasks_inputs = new ArrayList<Callable<Void>>();
			for (OMU p:OMUToProcess){
				Runnable worker = new Runnable_OMUs_move(p);
				tasks_inputs.add(Executors.callable(worker,(Void)null));
			}

			try {
				for (Future<?> f : executor.invokeAll(tasks_inputs)) { //invokeAll() blocks until ALL tasks submitted to executor complete
					f.get(); 
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}catch (NullPointerException e){
				e.printStackTrace();
			}
		}

	}

	public static void envUpdate(){

		//System.out.println("processing primates");
		
		//get all primates into an iterator 
		Iterator<Cell> CellList = ModelSetup.getAllCells().iterator();
		//Iterator<Cell> CellList = ModelSetup.getCellsToProcess().iterator();


		//for all primates in order
		while (CellList.hasNext()){

			//get subsample of primates
			ArrayList<Cell> CellToProcess = new ArrayList<Cell>();
			for (int i=0; i<Params.numberOfThreads;i++){
				try{
					CellToProcess.add(CellList.next());
				} catch (java.util.NoSuchElementException e){

				}
			}

			//process subsample of the primate population

			//inputs prior to action
			Collection<Callable<Void>> tasks_inputs = new ArrayList<Callable<Void>>();
			for (Cell c:CellToProcess){
				Runnable worker = new Runnable_Cells(c);
				tasks_inputs.add(Executors.callable(worker,(Void)null));
			}

			try {
				for (Future<?> f : executor.invokeAll(tasks_inputs)) { //invokeAll() blocks until ALL tasks submitted to executor complete
					f.get(); 
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}catch (NullPointerException e){
				e.printStackTrace();
			}
		}

		ModelSetup.removeCellsUpdated();

	}
	
	
	public void shutdown(){
		executor.shutdown();
	}



}
