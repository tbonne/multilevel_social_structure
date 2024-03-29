package multiLevelGroups;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.*;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.poi.ss.formula.functions.Now;

import repast.simphony.random.RandomHelper;
import cern.jet.random.VonMises;

import com.thoughtworks.xstream.mapper.SystemAttributeAliasingMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class OMU {

	Coordinate myCoord;
	Coordinate destination;
	RealVector previousBearing;
	RealVector myTravelVector;
	ArrayList<OMU> familiarOMUs;
	ArrayList<Double> familiarOMU_values;
	ArrayList<Cell> homeRange;
	ArrayList<Cell> rememberedFood;
	ArrayList<Double> rememberedFood_maxValues;
	ArrayList<Integer> spatialAssoVal;
	ArrayList<OMU> spatialAssoInds;
	ArrayList<double[]> positions;
	//int safeNeighboursReduction;
	double non_forage_effort;
	double behaviour_adjustment;
	//NormalDistribution nd;
	OMU motherAgent;
	int sex;
	long age;
	int modelEndNear;
	String name;
	int id;
	//double my_hunger;
	double energy;
	double myDepletionRate;
	double distanceTraveled;
	double foodIntake;


	/****************************OMU Construction************************/

	public OMU(Coordinate coord, int id_ini) {
		myCoord = coord;
		previousBearing = new ArrayRealVector(2);
		previousBearing.setEntry(0, RandomHelper.nextDouble()-0.5);
		previousBearing.setEntry(1, RandomHelper.nextDouble()-0.5);
		previousBearing.unitize();

		familiarOMU_values = new ArrayList<Double>();
		familiarOMUs = new ArrayList<OMU>();
		spatialAssoVal = new ArrayList<Integer>();
		spatialAssoInds = new ArrayList<OMU>();

		myTravelVector = new ArrayRealVector(2);

		//nd = new NormalDistribution(0,Params.errorInd);
		motherAgent = null;
		sex = RandomHelper.nextInt();
		age = Params.juveAge+1;
		modelEndNear = 0;
		name="A"+id_ini;
		id=id_ini;
		homeRange = new ArrayList<Cell>();
		//safeNeighboursReduction = 0;
		behaviour_adjustment=0.01;
		non_forage_effort = 1;
		energy = 0.0;
		distanceTraveled=0.0;
		foodIntake=0;
		positions = new ArrayList<double[]>();
		myDepletionRate = ModelSetup.getDepletionRate();
		
		//Initialize starting point for memory
		rememberedFood = new ArrayList<Cell>();
		ArrayList<Cell> food = getVisibleFoodPatches(Params.foodSearchRange);
		for(Cell c : food) {
			c.updateFam(this, true);
		}
		rememberedFood_maxValues = new ArrayList<Double>(); 
		while(rememberedFood_maxValues.size() < Params.landscapeWidth*Params.landscapeWidth) rememberedFood_maxValues.add(0.0);
	}

	public OMU(Coordinate coord, OMU mother, int id_ini) {
		myCoord = coord;
		previousBearing = new ArrayRealVector(2);
		previousBearing.setEntry(0, RandomHelper.nextDouble()-0.5);
		previousBearing.setEntry(1, RandomHelper.nextDouble()-0.5);
		previousBearing.unitize();

		familiarOMU_values = new ArrayList<Double>();
		familiarOMUs = new ArrayList<OMU>();
		spatialAssoVal = new ArrayList<Integer>();
		spatialAssoInds = new ArrayList<OMU>();

		myTravelVector = new ArrayRealVector(2);

		//nd = new NormalDistribution(0,Params.errorInd);
		motherAgent = mother;
		sex = RandomHelper.nextInt();
		age=0;
		modelEndNear = 0;
		name=mother.id +"_"+id_ini;
		id=id_ini;
		homeRange = new ArrayList<Cell>();
		//safeNeighboursReduction = 0;
		behaviour_adjustment=0.01;
		non_forage_effort = 1;
		energy = 0.0;
		distanceTraveled=0.0;
		foodIntake=0;
		positions = new ArrayList<double[]>();
		myDepletionRate = ModelSetup.getDepletionRate();
		
		//Initialize starting point for memory
		rememberedFood = new ArrayList<Cell>();
		ArrayList<Cell> food = getVisibleFoodPatches(Params.foodSearchRange);
		for(Cell c : food) {
			c.updateFam(this, true);
		}
		rememberedFood_maxValues = new ArrayList<Double>(); 
		while(rememberedFood_maxValues.size() < Params.landscapeWidth*Params.landscapeWidth) rememberedFood_maxValues.add(0.0);
	}

	/****************************OMU Behaviour************************/

	public void decision(){
		destination = null;
		
		ArrayList<Cell> food = getVisibleFoodPatches(Params.foodSearchRange);
		ArrayList<OMU> inds = getVisibleOMUs(Params.visualSearchRange);
		updateFam(inds, food);
		calculateMoveCoordVM(food,inds);
		
	}

	public void action(){
		age++;
		eat();
		move();
		setMyCoord(ModelSetup.getGeog().getGeometry(this).getCoordinate());
		if(this.age>Params.juveAge)setPositions(this.myCoord); //keep track of where I'm going... spatial phenotype
	}

	/****************************
	 * 
	 * 
	 * OMU information collection methods
	 * 
	 * 
	 * ************************/

	private ArrayList<Cell> getVisibleFoodPatches(int f){

		Iterable<Cell> objectsInArea = null;
		Envelope envelope = new Envelope();
		envelope.init(this.getMyCoord());
		envelope.expandBy(Params.cellSize+Params.cellSize/2.0);
		objectsInArea = ModelSetup.getGeog().getObjectsWithin(envelope,Cell.class);
		envelope.setToNull();

		ArrayList<Cell> obj = new ArrayList<Cell>();
		while(objectsInArea.iterator().hasNext()){
			Cell ce = objectsInArea.iterator().next();
			if(ce.getCoord().distance(this.getMyCoord())<Params.foodSearchRange)obj.add(ce);
		}
		
		return obj;
	}
	
	
	
	private ArrayList<OMU> getVisibleOMUs(int f){

		Iterable<OMU> objectsInArea = null;
		Envelope envelope = new Envelope();
		envelope.init(this.getMyCoord());
		envelope.expandBy(f);
		objectsInArea = ModelSetup.getGeog().getObjectsWithin(envelope,OMU.class);
		envelope.setToNull();

		ArrayList<OMU> obj = new ArrayList<OMU>();
		while(objectsInArea.iterator().hasNext()){
			OMU omu = objectsInArea.iterator().next();
			if(omu!=this)obj.add(omu);
		}

		return obj;
	}


	/****************************
	 * 
	 * 
	 * OMU movement methods
	 * 
	 * 
	 * ************************/


	private void calculateMoveCoordVM(ArrayList<Cell> foodSites, ArrayList<OMU> inds){
		
		//Add food sites into Set (non-duplicates)
		//Set<Cell> set = new LinkedHashSet<Cell>(foodSites);
		//set.addAll(food_remembered);

		//Convert Set to ArrayList
		//ArrayList<Cell> combinedFood = new ArrayList<Cell>(set);

		/****************initialize direction vector*********************/
		RealVector myVector = new ArrayRealVector(2);


		/********************Bearing effect**********************/
		myVector = myVector.add(getPreviousBearing().mapMultiply(Params.bearingWeight));
		//myVector = myVector.add(getPreviousBearing().mapMultiply(0.1));


		/********************Food effects**********************/
		//calculate the weights of each patch
		ArrayList<Double> weights = new ArrayList<Double>();  
		ArrayList<RealVector> directions = new ArrayList<RealVector>();
		double sum = 0;
		
		//for debugging
		//Params.depletionRate = 0.5;
		//Params.regrowthRate = 0.0002;
		//Params.foodWeight =100.0;
		//Params.socialWeight =0	;
		//Params.socialWeight = 1.0;
		//Params.homeWeight = 1.0;
		//Params.lDecay=0.0001;
		
		//if(this.toString().equals("multiLevelGroups.OMU@75b880f7")){
		//	System.out.println("target ind ");
		//}

		//for each food site
		for(Cell c : foodSites){

			//calculate expected patch utility
			double distance = Math.max(1,  c.getCoord().distance(this.getMyCoord()));
			if(distance < Params.cellSize/2.0)distance = Params.cellSize/2.0;
			double weight = 0;  
			
			//if patch is directly visible 
			if(distance <= (Params.foodSearchRange)) {
				
				 weight = ( c.getResourceLevel()/(distance/(Params.cellSize*2.0) ) );//
				 
				 //Add to utility weight to array
				 weights.add(weight);
				 sum = sum + weight;

				 //calculate the direction
				 RealVector patchVector = new ArrayRealVector(2);
				 double distX = c.getCoord().x-this.getMyCoord().x;
				 double distY = c.getCoord().y-this.getMyCoord().y;
				 patchVector.setEntry(0,distX);
				 patchVector.setEntry(1,distY);
				 if(distX!=0 && distY!=0)patchVector.unitize();
				 directions.add(patchVector);
			}
			
			
		}
		

		//standardize the patch weights to sum to one
		ArrayList<Double> weightsStan = new ArrayList<Double>();
		for(Double d : weights){
			weightsStan.add( Math.pow(d/sum,1) );
		}

		//calculate the avg direction 
		RealVector avgFoodVector = new ArrayRealVector(2);
		for(int i = 0 ; i< weights.size();i++){
			avgFoodVector = avgFoodVector.add(directions.get(i).mapMultiply(weightsStan.get(i))); //weightsStan  weights
		}

		//add to myVector
		//if((avgFoodVector.getEntry(0)==0 && avgFoodVector.getEntry(1)==0)==false)avgFoodVector.unitize();
		avgFoodVector = avgFoodVector.mapMultiply(Params.foodWeight);
		myVector = myVector.add(avgFoodVector);
		
		
		/********************Home effects**********************/
		
		//calculate the weights of each patch
		ArrayList<Double> weights_home = new ArrayList<Double>();  
		ArrayList<RealVector> directions_home = new ArrayList<RealVector>();
		double sum_home = 0;
		
		//if(this.toString().equals("multiLevelGroups.OMU@75b880f7")){
		//	System.out.println("target ind ");
		//}
		
		//get all food remembered
		ArrayList<Cell> food_remembered = getRememberedFood();
		
		//System.out.println("Check mem food sites ");
		
		//for each remembered site beyond my vision
		for(Cell c : food_remembered){

			//calculate expected patch utility
			double distance_home = Math.max(1,  c.getCoord().distance(this.getMyCoord()));
			
			//distance value cannot be less than resolution of cells
			//if(distance_home < Params.cellSize/2.0)distance_home=Params.cellSize/2.0;
			
			//If within visual range use actual value of resources			
			double remembered_food_max =0;
			if(distance_home > Params.foodSearchRange) {
				remembered_food_max = rememberedFood_maxValues.get(c.id);
			} else {
				remembered_food_max = c.getResourceLevel();
			}
			
			double weight = (1-c.getMemW(this))*c.getMemR(this)*( remembered_food_max/(distance_home ) );  /// (Params.cellSize*2.0)
			
			if(distance_home > Params.foodSearchRange) { //
				
				//System.out.println("MEMwork "+c.getMemW(this)+"  MEMref "+ c.getMemR(this)+ "  dist "+distance_home+" remeFood "+remembered_food_max + "  weight "+ weight);
				
				//Add to utility weight to array
				weights_home.add(weight);
				sum_home = sum_home + weight;

				//calculate the direction
				RealVector patchVector = new ArrayRealVector(2);
				double distX = c.getCoord().x-this.getMyCoord().x;
				double distY = c.getCoord().y-this.getMyCoord().y;
				patchVector.setEntry(0,distX);
				patchVector.setEntry(1,distY);
				if(distX!=0 && distY!=0)patchVector.unitize();
				directions_home.add(patchVector);
			}
		}
		
		//Assume they focus on the max utility patch
		//Cell chosenCell = foodSites.get(directions_home.indexOf(Collections.max(weights_home) ) );
		//RealVector chosenDirection = directions_home.get(weights_home.indexOf(Collections.max(weights) ) );
		
		//If there are remembered sites out of vision use these to inform movement
		if(directions_home.size()>0) {

			//standardize the patch weights to sum to one
			ArrayList<Double> weightsStan_home = new ArrayList<Double>();
			for(Double d : weights_home){
				weightsStan_home.add( Math.pow(d/sum_home,1) );
			}

			//ArrayList<Double> weightsMem_sorted = new ArrayList<Double>(weightsStan_home);
			//Collections.sort(weightsMem_sorted);
			//double Max_weight = weightsMem_sorted.get(0); //get top x weight

			//calculate the avg direction 
			RealVector avgHomeVector = new ArrayRealVector(2);
			for(int i = 0 ; i< weights_home.size();i++){
				//if(weightsStan_home.get(i) >= Max_weight) {
					avgHomeVector = avgHomeVector.add(directions_home.get(i).mapMultiply(weightsStan_home.get(i))); //weightsStan  weights
				//}
			}

			//add to myVector
			//if((avgHomeVector.getEntry(0)==0 && avgHomeVector.getEntry(1)==0)==false)avgHomeVector.unitize();
			
			avgHomeVector = avgHomeVector.mapMultiply(Params.homeWeight*non_forage_effort);
			
			
			myVector = myVector.add(avgHomeVector);
		}


		/********************Social effects**********************/
		
		//if i'm not hungry bias movement towards familiar others
		//if(my_hunger==Params.depletionRate) { 
			
			//calculate the weights of each visible individual
			double sumSoc = 0;
			ArrayList<Double> weightsSoc = new ArrayList<Double>();
			ArrayList<RealVector> directionsSoc = new ArrayList<RealVector>();
			
			//Loop through all nearby individuals
			for(OMU ind : inds){
				
				//a known individual is nearby
				if(familiarOMUs.contains(ind)){

					//calculate weight
					double weightSoc = familiarOMU_values.get(familiarOMUs.indexOf(ind));
					sumSoc = sumSoc + weightSoc;

					//calculate the direction and distance
					RealVector indVector = new ArrayRealVector(2);
					double distX = ind.getMyCoord().x-this.getMyCoord().x;
					double distY = ind.getMyCoord().y-this.getMyCoord().y;
					indVector.setEntry(0,distX);
					indVector.setEntry(1,distY);
					
					//get the distance to the individual
					double dist_ind = Math.sqrt(Math.pow(distX, 2)+ Math.pow(distY, 2) );
					
					//If distance is too close reduce influence
					if( dist_ind < Params.cellSize  ) {
						weightsSoc.add(weightSoc * Math.pow(dist_ind/Params.cellSize,2 ));
						//weightsSoc.add(0.0);
					} else {
						weightsSoc.add(weightSoc);
					}
					
					//Unitize the direction vector
					if((distX==0 && distY==0)==false)indVector.unitize();
					directionsSoc.add(indVector);

				//an unknown individual is nearby
				} else {

					//calculate weight
					double weightSoc = Params.famMinInd;
					sumSoc = sumSoc + weightSoc;

					//calculate the direction
					RealVector indVector = new ArrayRealVector(2);
					double distX = ind.getMyCoord().x-this.getMyCoord().x;
					double distY = ind.getMyCoord().y-this.getMyCoord().y;
					indVector.setEntry(0,distX);
					indVector.setEntry(1,distY);
					
					//get the distance to the individual
					double dist_ind = Math.sqrt(Math.pow(distX, 2)+ Math.pow(distY, 2) );
					
					//If distance is too close reduce influence
					if( Math.sqrt(Math.pow(distX, 2)+ Math.pow(distY, 2) ) < Params.cellSize ) {
						weightsSoc.add(weightSoc * Math.pow(dist_ind/Params.cellSize,2 ));
						//weightsSoc.add(0.0);
					} else {
						weightsSoc.add(weightSoc);
					}
					
					//Unitize the direction vector
					if((distX==0 && distY==0)==false)indVector.unitize();
					directionsSoc.add(indVector);
				}
			}

			//If there was at least one visible individual
			if(inds.size()>0){

				//standardize the patch weights to sum to one
				//ArrayList<Double> weightsSocStan = new ArrayList<Double>();
				//for(Double d : weightsSoc){
				//	weightsSocStan.add(d/sumSoc);
				//}
	
				//ensure only the top desired neighbors are considered (i.e., when hungry pay less attention to others).
				/*ArrayList<Double> weightsSoc_sorted = new ArrayList<Double>(weightsSoc);
				Collections.sort(weightsSoc_sorted);
				weightsSoc_sorted.removeIf(i -> i <= 0); //Remove negatives and 0s
				double social_threshold = 0.0;
				//if(safeNeighboursReduction>=0)System.out.println(safeNeighboursReduction+"_"+ weightsSoc+"_"+energy);
				if(safeNeighboursReduction>=weightsSoc_sorted.size()) {
					social_threshold = 1;
					safeNeighboursReduction=weightsSoc_sorted.size();
				} else {
					social_threshold = weightsSoc_sorted.get(safeNeighboursReduction);
				}
				
				
				// go through all weights and remove influence of any positive influences below the threshold
				for(int i =0; i<weightsSoc.size() ; i++) {
					
					//remove influence of ind who fall below the current threshold
					if(weightsSoc.get(i) < social_threshold & weightsSoc.get(i) > 0) {
						weightsSoc.set(i, 0.0);
					}
					
				}*/
				

				//calculate the avg direction (weights*dir to each ind)
				RealVector avgIndVector = new ArrayRealVector(2);
				for(int i = 0 ; i< weightsSoc.size();i++){
					avgIndVector = avgIndVector.add(directionsSoc.get(i).mapMultiply(weightsSoc.get(i)));
				}

				//add to myVector
				//if((avgIndVector.getEntry(0)==0 && avgIndVector.getEntry(1)==0)==false)avgIndVector.unitize();
				avgIndVector = avgIndVector.mapMultiply(Params.socialWeight*non_forage_effort);
				
				myVector = myVector.add(avgIndVector);
			}
		//}


		/********************Effect of uncertainty**********************/

		//estimate uncertainty (length of the resulting movement vector / max possible length of the resulting vector)
		RealVector finalVector = new ArrayRealVector(2);
		double u = Math.atan2(myVector.getEntry(1), myVector.getEntry(0));
		double length = Math.pow(Math.pow(myVector.getEntry(0),2)+Math.pow(myVector.getEntry(1), 2),0.5);
		//double maxLength = 0;
		//if(inds.size()>0){
		//	maxLength = Params.bearingWeight + Params.foodWeight + Params.socialWeight + Params.homeWeight ; 
		//} else{
		//	maxLength = Params.bearingWeight + Params.foodWeight + Params.homeWeight ; 
		//}
		//double k = Math.max(-2*Math.log(length/maxLength),0.00001);
		
		//add uncertainty to the final movement vector
		if(length!=0){
			//u = u + VonMises.staticNextDouble(1/0.1); 
			double deltaX = Math.cos(u);
			double deltaY = Math.sin(u);
			finalVector.setEntry(0, deltaX);
			finalVector.setEntry(1, deltaY);
			//finalVector.unitize();
		} else {
			//nothing
		}

		//multiply by movement scale and set travel information
		myTravelVector = finalVector.mapMultiply(Params.maxDistPerStep);

		//record current vector as previous vector for next movements
		this.setPreviousBearing(myTravelVector);
		
		
	}
	
	


	private void move(){
		if(age<Params.juveAge){
			ModelSetup.getGeog().moveByDisplacement(this, this.motherAgent.myTravelVector.getEntry(0), this.motherAgent.myTravelVector.getEntry(1));
			distanceTraveled = distanceTraveled + Math.sqrt( Math.pow(this.motherAgent.myTravelVector.getEntry(0),2) + Math.pow(this.motherAgent.myTravelVector.getEntry(1),2) );
		} else {
			ModelSetup.getGeog().moveByDisplacement(this, myTravelVector.getEntry(0), myTravelVector.getEntry(1));
			distanceTraveled = distanceTraveled + Math.sqrt( Math.pow(myTravelVector.getEntry(0),2) + Math.pow(myTravelVector.getEntry(1),2) );
		}
	}


	/**************************
	 * 
	 * 
	 * OMU foraging
	 * 
	 * 
	 **************************/

	private void eat(){

		//get the food cell that i'm in.
		ArrayList<Cell> foodSites = new ArrayList<Cell>();
		Cell myCell = null;
		Iterable<Cell> objectsInArea = null;
		Envelope envelope = new Envelope();
		envelope.init(this.getMyCoord());
		envelope.expandBy(Params.cellSize);
		objectsInArea = ModelSetup.getGeog().getObjectsWithin(envelope,Cell.class);
		envelope.setToNull();

		for(Cell c : objectsInArea){
			foodSites.add(c);
		}

		if(foodSites.size()>1){
			double minDist = 99999;
			for(Cell ce : foodSites){
				double di = this.getMyCoord().distance(ce.getCoord());
				if(di<minDist){
					minDist = di;
					myCell = ce;
				}
			}
		} else if (foodSites.size()==1){
			myCell = foodSites.get(0);
		}

		//If i'm in a food cell eat and update familiarity
		if(myCell!=null){
			
			//chance of eating is based on how hungry i am
			double energey_scale = Math.max(Math.min(energy, 5),-5); //times 10 here speeds the changes up... depletion is currently around 0.01
			double prob_eat = 1 - Math.exp(energey_scale)/(1+Math.exp(energey_scale)) ; 
			if(Math.random()<prob_eat) {
				
				//take a bite
				double bite = myCell.eatMe();
				
				//update energy
				energy = energy + bite;
				
				//update total amount of food acquired
				foodIntake = foodIntake + bite;
				
				//update desired number of social partners
				adjust_numb_familiar(bite);
			}
			
			//update memory of this cell
			myCell.updateFam(this, false);
			if(homeRange.contains(myCell)==false) {
				homeRange.add(myCell);
			}
			
			//Adjust energy levels
			energy = energy - myDepletionRate;
			
			//System.out.println("My depletion rate "+ myDepletionRate);
		}

	}

	/**************************
	 * 
	 * 
	 * OMU familiarization methods
	 * 
	 * 
	 **************************/
	
	private void adjust_numb_familiar(double bite) {
		
		//
		//if(energy < 0.0) {

		if(bite<Params.depletionRate | energy < 0.0) {
			//safeNeighboursReduction = Math.max(safeNeighboursReduction+1, familiarOMUs.size()-1);
			non_forage_effort = Math.max(non_forage_effort-behaviour_adjustment, 0);
			//	} //debugging here need to figure out if the neighbour size is changing the way it should

		} else {
			non_forage_effort = Math.min(non_forage_effort+behaviour_adjustment, 1);
			//safeNeighboursReduction = Math.min(safeNeighboursReduction+1,0);
		}

	}

	private void updateFam(ArrayList<OMU> visibleInds, ArrayList<Cell> food){
		
		
		//Update individual familiarity
		
		ArrayList<OMU> withinFamiliarRange = new ArrayList<OMU>();
		for(OMU o: visibleInds){
			if(o.getMyCoord().distance(this.getMyCoord())<=Params.familarRange)withinFamiliarRange.add(o);
		}
		
		//adjust familiarity growth based on adult
		double learning_rate_decrease_adult = 0.0;
		if(this.age>Params.juveAge) {
			learning_rate_decrease_adult = 0.0;//0.90;//0.99;
		}
		
		//increase familiarity with those in range
		for(OMU o : withinFamiliarRange){

			//already on my familiarity list
			if(familiarOMUs.contains(o)){

				int indexO = familiarOMUs.indexOf(o);
				double f = familiarOMU_values.get(indexO);
				if(f<0){
					f = Math.max( (f - ((Params.iGrow - (learning_rate_decrease_adult*Params.iGrow)  ) * (1+f)*f) ),Params.famMinInd);//+nd.sample()
				} else {
					f = Math.min( (f + ((Params.iGrow - (learning_rate_decrease_adult*Params.iGrow)) * (1-f)*f) ),Params.famMaxInd);//+nd.sample()
				}

				familiarOMU_values.set(indexO, f);

				//needing to be added to my familiarity list
			} else {

				//add new individual
				familiarOMUs.add(o);
				double f = Params.famMinInd;
				if(f<0){
					f = Math.max( (f - ((Params.iGrow - (learning_rate_decrease_adult*Params.iGrow)) * (1+f)*f)),Params.famMinInd);//+nd.sample()
				} else {
					f = Math.max( (f + (Params.iGrow - (learning_rate_decrease_adult*Params.iGrow)) * (1-f)*f),Params.famMinInd); //+nd.sample()
				}
				familiarOMU_values.add(f);
			}
		}

		//decrease familiarity 
		ArrayList<OMU> removeFams = new ArrayList<OMU>();
		for(OMU inds: familiarOMUs){
			if(withinFamiliarRange.contains(inds)==false){
				int indexO = familiarOMUs.indexOf(inds);
				double f = familiarOMU_values.get(indexO);
				if(f<0){
					f = Math.max( (f  - Params.iDecay * (1+f)*f),Params.famMinInd);//+nd.sample()
				}else{
					f = Math.max( (f  + Params.iDecay * (1-f)*f),Params.famMinInd);//+nd.sample()
				}
				familiarOMU_values.set(indexO, f);
				if(f <= Params.famMinInd)removeFams.add(inds);
			}
		}

		

		//update list (remove all individuals that are lower or equal to the min familiarity)
		for(OMU omu:removeFams){
			familiarOMU_values.remove(familiarOMUs.indexOf(omu));
			familiarOMUs.remove(omu);
		}
		
		
		//Update cell memory (just seen not necessarily in / used yet)
		for (Cell c : food) {
			
			//update only ref memory (update working memory if i'm eating in this cell)
			c.updateFam(this, true);
			
			//update remembered resource (moving average or experience with the cell) only if working memory is low (i.e., what the food site was like before i eat from it)
			//if(c.getMemW(this)<0.99) {
				rememberedFood_maxValues.set(c.id, ( ((1-c.getMemW(this))*c.resources) + ((c.getMemW(this))*rememberedFood_maxValues.get(c.id)) )/1.0 );
			//}
			//if(c.resources > rememberedFood_maxValues.get(c.id)) {
			//	rememberedFood_maxValues.set(c.id, c.resources);
			//}
		}
		
	}
	
	
	//returns a value of how much the agent remembers the cell (reference mem)
	public ArrayList<Double> getRemberedCellsValues() {
		
		//Initialize an array to hold all the mem values
		//double[] rem = new double[ModelSetup.allCells.size()];
		ArrayList<Double> rem = new ArrayList<Double>();
		
		//loop through and fill the arry with mem values
		for(int c = 0; c < ModelSetup.allCells.size(); c++) {
			
			//get the cell
			Cell cell = ModelSetup.allCells.get(c);
			
			//default is that the agent does not know the cell
			double rem_value = 0.0;
			
			//if the agent does know the cell record the magnitude of the reference memory
			if(cell.familiarityIDs.contains(this)) {
				
				//get the index of the memory
				int  i = cell.familiarityIDs.indexOf(this);
				
				//use the index to get the memory value
				rem_value = cell.familiarityValues.get(i);
			}
			
			//add the value to the array
			rem.add(rem_value);
			
		}
		
		//return the array
		return rem;
	}

	/****************************
	 * 
	 * get and set methods
	 * 
	 * ************************/

	public Coordinate getMyCoord(){
		return myCoord;
	}
	public void setMyCoord(Coordinate c){
		myCoord=c;
	}
	public void setPreviousBearing(RealVector r){
		previousBearing = r;
	}
	public RealVector getPreviousBearing(){
		return previousBearing;
	}

	public ArrayList<Integer> getSpatialAssoVal() {
		return spatialAssoVal;
	}
	public ArrayList<OMU> getSpatialAssoInds() {
		return spatialAssoInds;
	}
	public ArrayList<OMU> getSocailAssoInds(){
		return familiarOMUs;
	}

	//controlling remembered cells
	private ArrayList<Cell> getRememberedFood(){
		return rememberedFood;
	}
	public void removeFamilarFood(Cell cell) {
		rememberedFood.remove(cell);
	}
	public void addFamilarFood(Cell cell) {
		rememberedFood.add(cell);
	}
	public int getNumbCellsRemembered() {
		return rememberedFood.size();
	}
	public int getHomeRangeSize(){
		return homeRange.size();
	}
	public ArrayList<double[]> getPositions() {
		return positions;
	}
	public void setPositions(Coordinate coord) {
		double[] m = {coord.x,coord.y};
		positions.add(m);
	}
	public double getNon_forage_effort() {
		return non_forage_effort;
	}
	public double getEnergy() {
		return energy;
	}
	public double getEfficiency() {
		
		//Calculate efficiency
		double eff = foodIntake/distanceTraveled;
		
		//Return values to zero
		distanceTraveled = 0.0;
		foodIntake = 0.0;
		
		return eff;
	}
	

}
