package multiLevelGroups;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.util.ContextUtils;

import jsat.distributions.multivariate.MetricKDE;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.piccolo2d.extras.util.Points;

public class Observer {
	
	
	//////////
	//
	// Methods to record behaviours
	//
	//////////
	
	//this will export all influence patterns between individuals at the end of the simulation
	public static void recordInfluencePatterns(ArrayList<OMU> agents){

		//get all individuals
		/*Iterable<OMU> allAgents = ModelSetup.getContext().getAgentLayer(OMU.class);
		List<OMU> allAgentsOrdered = new ArrayList<OMU>();

		//place it into a list
		for(OMU omu : allAgents){
			allAgentsOrdered.add(omu);
		}*/

		//double[][] asso = new double[allAgentsOrdered.size()][allAgentsOrdered.size()];
		ArrayList<List<String>> edgeList = new ArrayList<List<String>>();

		//for each agent place associations into a vector and add to a matrix
		for(OMU focal: agents){

			//int myIndex = allAgentsOrdered.indexOf(focal);

			//for all agents see if there is an association
			for(OMU other: agents){

				//int otherIndex = allAgentsOrdered.indexOf(other);

				//yes there is an association
				if(focal.familiarOMUs.contains(other)){

					//int otherIndex_in_focal = focal.familiarOMUs.indexOf(other);
					//asso[myIndex][otherIndex] = focal.familiarOMU_values.get(otherIndex_in_focal);
					
					double weight = focal.familiarOMU_values.get(focal.familiarOMUs.indexOf(other));

					edgeList.add(Arrays.asList(focal.name, other.name, Double.toString(weight) ));

					//no association
					//} else {
					//
					//	asso[myIndex][otherIndex] = 0 ;
					//	
					//}

				}

			}
		}

		//print the influence association matrix
		printArrayToCSV(edgeList);

	}


	
	
	//this will export all influence patterns between individuals at the end of the simulation
	public static void recordSpatialPatterns(ArrayList<OMU> agents){  

		//get all individuals
		/*Iterable<OMU> allAgents = ModelSetup.getContext().getAgentLayer(OMU.class);
		List<OMU> allAgentsOrdered = new ArrayList<OMU>();

		//place it into a list
		for(OMU omu : allAgents){
			allAgentsOrdered.add(omu);
		}*/

		//double[][] asso = new double[allAgentsOrdered.size()][allAgentsOrdered.size()];
		ArrayList<List<String>> edgeList = new ArrayList<List<String>>();

		//for each agent place associations into a vector and add to a matrix
		for(OMU focal: agents){

			//int myIndex = allAgentsOrdered.indexOf(focal);

			//for all the focal agents associations
			for(OMU other: agents){

				//int otherIndex = allAgentsOrdered.indexOf(other);
				double dist = Math.pow(Math.pow(focal.getMyCoord().x - other.getMyCoord().x,2)+Math.pow(focal.getMyCoord().y - other.getMyCoord().y,2),0.5);

				//yes there is an association
				if(dist<30){

					//int otherIndex_in_focal = focal.spatialAssoInds.indexOf(other);
					//asso[myIndex][otherIndex] = focal.spatialAssoVal.get(otherIndex_in_focal);
					edgeList.add(Arrays.asList(focal.name, other.name));

					//no association
					//} else {
					//
					//	asso[myIndex][otherIndex] = 0 ;
					//} 

				}

			}
		}

		//print the spatial association matrix
		printSpatialArrayToCSV(edgeList);

	}

	
	
	//this will export home range sizes of all individuals
	public static void recordHomeRangePatterns(ArrayList<OMU> agents ){  

		//get all individuals
		/*Iterable<OMU> allAgents = ModelSetup.getContext().getAgentLayer(OMU.class);
		List<OMU> allAgentsOrdered = new ArrayList<OMU>();

		//place it into a list
		for(OMU omu : allAgents){
			allAgentsOrdered.add(omu);
		}*/

		//create arrays to fill with information about each agent
		double[] hr = new double[ModelSetup.id_count];
		String[] hr_id = new String[ModelSetup.id_count];
		double[] depletionRates = new double[ModelSetup.id_count];
		int[] inds = new int[ModelSetup.id_count];
		
		int index_i = 0;

		//for each agent place associations into a vector and add to a matrix
		for(OMU focal: agents){

			hr[index_i] = focal.getHomeRangeSize();
			hr_id[index_i] = focal.name;
			depletionRates[index_i] = focal.myDepletionRate;
			inds[index_i] = focal.getSocailAssoInds().size();
			
			
			index_i = index_i + 1;
			
			System.out.println("home range size: "+ focal.getHomeRangeSize());
		}

		//print the spatial association matrix
		printCSV_homeRange(hr,hr_id,depletionRates,inds, agents.size());

	}
	
	
	//this will export home range sizes of all individuals
	public static void recordHomeRangeOverlap(ArrayList<OMU> agents ){  

		//create an edge list
		ArrayList<List<String>> edgeList = new ArrayList<List<String>>();

		//for each agent place associations into a vector and add to a matrix
		for(OMU focal: agents){

			//int myIndex = allAgentsOrdered.indexOf(focal);

			//for all the focal agents associations
			for(OMU other: agents){
				
				if(focal!=other) {

					//estimate the correlation in shared home range between two agents
					//int otherIndex = allAgentsOrdered.indexOf(other);
					//double corr = similarity_in_KDE(focal,other);
					double corr = similarity_in_mem(focal,other);

					//yes there is a positive association
					if(corr>0){

						//add info to edge list
						edgeList.add(Arrays.asList(focal.name, other.name, Double.toString(corr) ));

					}
				}
			}
		} 
		
		//print the spatial association matrix
		printSpatialOverlapArrayToCSV(edgeList);

	}
	
	//takes two agents and calculates similarity in their spatial memory
	private static double similarity_in_mem(OMU a1, OMU a2) {
		
		//get memories of each agent
		ArrayList<Double> mem_a1 = a1.getRemberedCellsValues();
		ArrayList<Double> mem_a2 = a2.getRemberedCellsValues();
		
		//clean the array (get rid of 0,0)
		for(int i =0 ; i< mem_a1.size(); i++ ) {
			
			//if both are non-zero keep that row
			if( (mem_a1.get(i) ==0.0) & (mem_a2.get(i) ==0.0) ) {
				
				//remove the rows that are both zero
				mem_a1.remove(i);
				mem_a2.remove(i);
			}
		}
		
		//assume that there is no correlation in spatial memory
		double corr = 0.0;
		
		//if there are some values left after cleaning
		if( (mem_a1.size()>0)  & (mem_a2.size()>0) ) {
		
			//convert arraylist to double[]
			double[] mem_a1_array = mem_a1.stream().mapToDouble(d -> d).toArray();
			double[] mem_a2_array = mem_a2.stream().mapToDouble(d -> d).toArray();

			//Calculate the correlation between the two memories
			corr = new PearsonsCorrelation().correlation(mem_a1_array, mem_a2_array );
		} 

		//return the correlation
		return corr;
	}
	
	//function to calculate the correlation between KDE estimates of two individual agents
	private static double similarity_in_KDE(OMU a1, OMU a2) {
		
		//Get the kde of the one agent
		MetricKDE kde1 = get_kde(a1);
		
		//Get the kde of the second agent
		MetricKDE kde2 = get_kde(a2);
		
		//Calculate the correlation between KDE
		double corr = kde_correlation(kde1,kde2);
		
		//return correlation
		return corr;
	}
	
	
	//function to calculate a kde from position data
	private static MetricKDE get_kde(OMU agent) {
		
		//get list of x,y points into kde estimate
		List<Vec> dataSet = new ArrayList<Vec>();
		for(int i = 0; i< agent.getPositions().size() ; i++){
			//double[] dd = agent.getPositions().get(i);
			dataSet.add(new DenseVector(agent.getPositions().get(i)));
		}

		//create KDE
		MetricKDE mk = new MetricKDE();
		//mk.setDefaultK(3);
		//mk.setBandwith(100);
		mk.setUsingData(dataSet, 30.0);
		
		//System.out.println(mk.getBandwith());
		
		return mk;
		
	}
	
	
	private static double kde_correlation(MetricKDE kde1, MetricKDE kde2) {
		
		
		double[][] obs_UD = new double[2][Params.landscapeWidth*Params.landscapeWidth];
		int count = 0;
		
		//loop through the landscape and estimate a point every cell size
		for(int x=0; x<Params.landscapeWidth; x++) {
			for(int y=0; y<Params.landscapeWidth; y++) { // assuming square landscape
				
				//convert from x,y of grid cell to location on the map
				double xcoord = -7.5 + 22.5 * x;
				double ycoord = 0 - 25.98076 * y;
				
				//get the UD at each location for each kde
				obs_UD[0][count] = Math.exp(kde1.logPdf(new double[] {xcoord,ycoord} ));//store all x,y 
				obs_UD[1][count] = Math.exp(kde2.logPdf(new double[] {xcoord,ycoord} ));
				
				//increase the count
				count++;
				
			}
		}
		
		
		//create an arraylist
		ArrayList<Double> obs_UD_a = new ArrayList<Double>();
		ArrayList<Double> obs_UD_b = new ArrayList<Double>();
		for(int i = 0; i<Params.landscapeWidth*Params.landscapeWidth;i++) {
			obs_UD_a.add(obs_UD[0][i]);
			obs_UD_b.add(obs_UD[1][i]);
		}
		
		//loop through the density values and keep only those where both are not 0 (i.e., we are interested in overlap)
		for(int i = 0; i <Params.landscapeWidth*Params.landscapeWidth;i++) {
			if(obs_UD_a.get(i) == obs_UD_b.get(i)) {
				obs_UD_a.remove(i);
				obs_UD_b.remove(i);
			}
		}
		
		//convert back to array
		double[] arr_a = obs_UD_a.stream().mapToDouble(d -> d).toArray(); 
		double[] arr_b = obs_UD_b.stream().mapToDouble(d -> d).toArray(); 
		
		//Calculate the correlation between the two KDEs
		double corr = new PearsonsCorrelation().correlation(arr_a,arr_b);
		
		System.out.println(corr);
		
		return corr; 
		
	}
	
	
	//////////
	//
	// Methods for printing to CSV
	//
	//////////
	
	public static void printArrayToCSV(ArrayList<List<String>> edges){

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("socialStructure_"+RunEnvironment.getInstance().getCurrentSchedule().getTickCount()+".csv",false));
			
			//header
			writer.append("from , to, weight, time");
			writer.newLine();

			//for each row and column
			int size = edges.size();
			for(int i = 0; i < size; i++) {
				
				List<String> supp = edges.get(i);
				writer.append(String.valueOf(supp.get(0)));
				writer.append(',');
				writer.append(String.valueOf(supp.get(1)));
				writer.append(',');
				writer.append(String.valueOf(supp.get(2)));
				writer.append(',');
				writer.append( Double.toString(RunEnvironment.getInstance().getCurrentSchedule().getTickCount() ) );
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
		}        
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	
	public static void printSpatialArrayToCSV(ArrayList<List<String>> edges){

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("spatialStructure_"+RunEnvironment.getInstance().getCurrentSchedule().getTickCount()+".csv",false));

			//header
			writer.append("from , to, time");
			writer.newLine();
			
			//for each row and column
			int size = edges.size();
			for(int i = 0; i < size; i++) {
				
				List<String> supp = edges.get(i);
				writer.append(String.valueOf(supp.get(0)));
				writer.append(',');
				writer.append(String.valueOf(supp.get(1)));
				writer.append(',');
				writer.append( Double.toString(RunEnvironment.getInstance().getCurrentSchedule().getTickCount() ) );
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
		}        
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	
	
	public static void printSpatialOverlapArrayToCSV(ArrayList<List<String>> edges){

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("spatialOverlap_"+RunEnvironment.getInstance().getCurrentSchedule().getTickCount()+".csv",false));

			//header
			writer.append("from , to, weight");
			writer.newLine();
			
			//for each row and column
			int size = edges.size();
			for(int i = 0; i < size; i++) {
				
				List<String> supp = edges.get(i);
				writer.append(String.valueOf(supp.get(0)));
				writer.append(',');
				writer.append(String.valueOf(supp.get(1)));
				writer.append(',');
				writer.append(String.valueOf(supp.get(2)));
				writer.append(',');
				writer.append( Double.toString(RunEnvironment.getInstance().getCurrentSchedule().getTickCount() ) );
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
		}        
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	
	
	public static void printCSV_homeRange(double[] hr,String[] hr_id,double[] depletionRates, int[] inds, int size){

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("homeRangeStructure_"+RunEnvironment.getInstance().getCurrentSchedule().getTickCount()+".csv",false));

			//for each row and column
			for(int i = 0; i < size; i++) {
					writer.append(String.valueOf(hr[i]));
					writer.append(',');
					writer.append(String.valueOf(hr_id[i]));
					writer.append(',');
					writer.append(String.valueOf(depletionRates[i]) );
					writer.append(',');
					writer.append(String.valueOf(inds[i]) );
					writer.newLine();
			}
			
			writer.flush();
			writer.close();
		}        
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}


}
