package multiLevelGroups;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.util.ContextUtils;

public class Observer {
	
	//this will export all influence patterns between individuals at the end of the simulation
	public static void recordInfluencePatterns(){

		//get all individuals
		Iterable<OMU> allAgents = ModelSetup.getContext().getAgentLayer(OMU.class);
		List<OMU> allAgentsOrdered = new ArrayList<OMU>();

		//place it into a list
		for(OMU omu : allAgents){
			allAgentsOrdered.add(omu);
		}

		double[][] asso = new double[allAgentsOrdered.size()][allAgentsOrdered.size()];

		//for each agent place associations into a vector and add to a matrix
		for(OMU focal: allAgentsOrdered){

			int myIndex = allAgentsOrdered.indexOf(focal);

			//for all agents see if there is an association
			for(OMU other: allAgentsOrdered){

				int otherIndex = allAgentsOrdered.indexOf(other);

				//yes there is an association
				if(focal.familiarOMUs.contains(other)){

					int otherIndex_in_focal = focal.familiarOMUs.indexOf(other);
					asso[myIndex][otherIndex] = focal.familiarOMU_values.get(otherIndex_in_focal);

				//no association
				} else {

					asso[myIndex][otherIndex] = 0 ;
					
				}

			}

		}

		//print the influence association matrix
		printMatrixToCSV(asso,allAgentsOrdered.size());
		
	}


	public static void printMatrixToCSV(double[][] asso, int size){

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("socialStructure_"+RunEnvironment.getInstance().getCurrentSchedule().getTickCount()+".csv",false));

			//for each row and column
			for(int i = 0; i < size; i++) {
				for (int j=0; j<size; j++) {
					writer.append(String.valueOf(asso[i][j]));
					writer.append(',');
				}
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
	
	
	//this will export all influence patterns between individuals at the end of the simulation
	public static void recordSpatialPatterns(){  

			//get all individuals
			Iterable<OMU> allAgents = ModelSetup.getContext().getAgentLayer(OMU.class);
			List<OMU> allAgentsOrdered = new ArrayList<OMU>();

			//place it into a list
			for(OMU omu : allAgents){
				allAgentsOrdered.add(omu);
			}

			double[][] asso = new double[allAgentsOrdered.size()][allAgentsOrdered.size()];

			//for each agent place associations into a vector and add to a matrix
			for(OMU focal: allAgentsOrdered){

				int myIndex = allAgentsOrdered.indexOf(focal);

				//for all the focal agents associations
				for(OMU other: allAgentsOrdered){

					int otherIndex = allAgentsOrdered.indexOf(other);

					//yes there is an association
					if(focal.spatialAssoInds.contains(other)){
						
						int otherIndex_in_focal = focal.spatialAssoInds.indexOf(other);
						asso[myIndex][otherIndex] = focal.spatialAssoVal.get(otherIndex_in_focal);

					//no association
					} else {

						asso[myIndex][otherIndex] = 0 ;
					} 

				}

			}
			
			//print the spatial association matrix
			printMatrixToCSV_spatial(asso,allAgentsOrdered.size());

		}
	
	public static void printMatrixToCSV_spatial(double[][] asso, int size){

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("spatialStructure_"+RunEnvironment.getInstance().getCurrentSchedule().getTickCount()+".csv",false));

			//for each row and column
			for(int i = 0; i < size; i++) {
				for (int j=0; j<size; j++) {
					writer.append(String.valueOf(asso[i][j]));
					writer.append(',');
				}
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
