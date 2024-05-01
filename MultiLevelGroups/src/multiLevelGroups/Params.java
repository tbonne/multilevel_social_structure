package multiLevelGroups;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import repast.simphony.*;

public class Params {

	/********************************
	 * 								*
	 * Model parameters				*
	 * 								*
	 * ******************************/
	
	final static Parameters p = RunEnvironment.getInstance().getParameters();
	
	//Simulation
	public final static int numberOfThreads = 1;
	public static int endTime = 5000*10;
	
	//landscape
	public final static String geog = "geog";
	public static int landscapeWidth = 10;//100;
	public static double envHomogen = 2.5;
	
	//resource cells
	public final static int cellSize = 30;
	public static double regrowthRate = 0.001;
	public static double depletionRate = 0.01;
	public static double foodDensity = 0.50;
	public static double ideal_energy=0;
	
	//OMU population
	public static int numbOMU = 100;
	public static int turnover = 10;
	public static int turnover_time = 5000 ;//#endTime + 1; //i.e., never turnover the population
	public static int juveAge = 500;
	public final static int spatialAssoStartTime = endTime - 1000;
	
	//OMU foraging behaviour
	public static int foodSearchRange = 45;
	public static int visualSearchRange = 100;
	public static int familarRange = cellSize; 
	
	//OMU momvement behaviour
	public static double bearingWeight = 0.00;
	public static double foodWeight = 1.0;
	public static double socialWeight = 0.0;
	public static double homeWeight = 0.0;//0.001;
	public final static int maxDistPerStep = 2;
	
	//Familiarity
	public static double lGrow = 0.1;
	public static double lDecay = 0.0001;
	public static double lDecay_work = 0.001;
	public static double iGrow = 0.1;
	public static double iDecay = 0.009;
	public static double famMinInd = -0.999; //0.001
	public static double famMinCell = 0.001;
	public static double famMaxCell = 0.999;
	public static double famMaxInd = 0.999;
	public static double errorInd = 0.001;
	public static double depletionSD = 0.0;
	public static double diff_IndEat_CellGrow = 10.0;
	
	//Observer
	public static int spatialRangeAsso = 100;
	
	//Constructor: used to set values from batch runs or the GUI
		public Params(){
			
			envHomogen = (Double)p.getValue("envHomogen");
			regrowthRate = (Double)p.getValue("regrow");
			depletionRate = (Double)p.getValue("depletionRate");
			numbOMU = (Integer)p.getValue("NumbOMU");
			foodSearchRange = (Integer)p.getValue("foodSearchRange");
			visualSearchRange = (Integer)p.getValue("visualSearchRange");
			bearingWeight = (Double)p.getValue("bearingWeight");
			foodWeight = (Double)p.getValue("foodWeight");
			socialWeight = (Double)p.getValue("socialWeight");
			homeWeight = (Double)p.getValue("homeWeight");

			juveAge = (Integer)p.getValue("juveAge");
			lGrow= (Double)p.getValue("lGrow");
			lDecay= (Double)p.getValue("lDecay");
			lDecay_work= (Double)p.getValue("lDecay");
			iGrow= (Double)p.getValue("iGrow");
			iDecay= (Double)p.getValue("iDecay");
			famMinInd= (Double)p.getValue("famMinInd");
			famMinCell= (Double)p.getValue("famMinCell");
			famMaxCell= (Double)p.getValue("famMaxCell");
			famMaxInd= (Double)p.getValue("famMaxInd");
			errorInd= (Double)p.getValue("errorInd");
			
			landscapeWidth = (Integer)p.getValue("landWidth");
			
			//Check for input from a param file manually
			try {
				ArrayList<List<String>> mParams = new ArrayList<List<String>>();
				try (BufferedReader br = new BufferedReader(new FileReader("data/params_set.csv"))) {
					String line;
					while ((line = br.readLine()) != null) {
						String[] values = line.split(",");
						mParams.add(Arrays.asList(values));
					}

					List<String> param_list = mParams.get(0);
					
					System.out.println(param_list);
					
					bearingWeight= Double.parseDouble(param_list.get(0));
					foodWeight= Double.parseDouble(param_list.get(1));
					socialWeight = Double.parseDouble(param_list.get(2));
					endTime = Integer.parseInt(param_list.get(3));
					envHomogen = Double.parseDouble(param_list.get(4));
					iDecay = Double.parseDouble(param_list.get(5));
					iGrow = Double.parseDouble(param_list.get(6));
					lDecay = Double.parseDouble(param_list.get(7));
					lDecay_work = Double.parseDouble(param_list.get(8));
					famMaxInd= Double.parseDouble(param_list.get(9));
					famMinInd= Double.parseDouble(param_list.get(10));
					juveAge = (int) Math.round(Double.parseDouble(param_list.get(11)));
					regrowthRate = Double.parseDouble(param_list.get(12));
					turnover = Integer.parseInt(param_list.get(13));
					turnover_time = Integer.parseInt(param_list.get(14));
					
					homeWeight = Double.parseDouble(param_list.get(15));
					depletionRate = Double.parseDouble(param_list.get(16));
					//regrowthRate = Double.parseDouble(param_list.get(17));
					depletionSD = Double.parseDouble(param_list.get(18));
					lGrow = Double.parseDouble(param_list.get(19));

				}
			} catch (FileNotFoundException error) {
				System.out.println("no manuall params file used!");
			} catch (IOException error) {
				System.out.println("no manuall params file used!");
			}
			
			System.out.println("new params? "+ lDecay_work);
		}
	
}
