package multiLevelGroups;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import repast.simphony.random.RandomHelper;
import cern.jet.random.VonMises;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class OMU {

	Coordinate myCoord;
	int id;
	Coordinate destination;
	RealVector previousBearing;
	RealVector myTravelVector;
	ArrayList<OMU> familiarOMUs;
	ArrayList<Double> familiarOMU_values;
	NormalDistribution nd;


	/****************************OMU Construction************************/

	public OMU(Coordinate coord, int ID) {
		myCoord = coord;
		id = ID;
		previousBearing = new ArrayRealVector(2);
		previousBearing.setEntry(0, RandomHelper.nextDouble()-0.5);
		previousBearing.setEntry(1, RandomHelper.nextDouble()-0.5);
		previousBearing.unitize();

		familiarOMU_values = new ArrayList<Double>();
		familiarOMUs = new ArrayList<OMU>();

		myTravelVector = new ArrayRealVector(2);
		
		nd = new NormalDistribution(0,Params.errorInd);
	}

	/****************************OMU Behaviour************************/

	public void decision(){

		destination = null;
		ArrayList<Cell> food = getVisibleFoodPatches(Params.foodSearchRange);
		ArrayList<OMU> inds = getVisibleOMUs(Params.visualSearchRange);
		calculateMoveCoordVM(food,inds);
		updateFam(inds);
	}

	public void action(){
		move();
		setMyCoord(ModelSetup.getGeog().getGeometry(this).getCoordinate());
		eat();
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
		envelope.expandBy(f);
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
		
		
		/****************initialize direction vector*********************/
		RealVector myVector = new ArrayRealVector(2);


		/********************Bearing effect**********************/
		myVector = myVector.add(getPreviousBearing().mapMultiply(Params.bearingWeight));


		/********************Food effects**********************/
		//calculate the weights of each patch
		ArrayList<Double> weights = new ArrayList<Double>();  
		ArrayList<RealVector> directions = new ArrayList<RealVector>();
		double sum = 0;

		for(Cell c : foodSites){

			//calculate weight
			double distance = Math.max(1,  c.getCoord().distance(this.getMyCoord()));
			double weight = (1-Params.homeWeight)*( c.getResourceLevel()/distance ) +  (Params.homeWeight)*(c.getFamiliarity(this));  
			weights.add(weight);
			sum = sum + weight;

			//calculate the direction
			RealVector patchVector = new ArrayRealVector(2);
			double distX = c.getCoord().x-this.getMyCoord().x;
			double distY = c.getCoord().y-this.getMyCoord().y;
			if(distX!=0 && distY!=0){
				patchVector.setEntry(0,distX);
				patchVector.setEntry(1,distY);
				patchVector.unitize();
			}
			directions.add(patchVector);
		}

		//standardize the patch weights to sum to one
		ArrayList<Double> weightsStan = new ArrayList<Double>();
		for(Double d : weights){
			weightsStan.add(d/sum);
		}

		//calculate the avg direction 
		RealVector avgFoodVector = new ArrayRealVector(2);
		for(int i = 0 ; i< weightsStan.size();i++){
			avgFoodVector = avgFoodVector.add(directions.get(i).mapMultiply(weightsStan.get(i)));
		}

		//add to myVector
		avgFoodVector.unitize();
		avgFoodVector = avgFoodVector.mapMultiply(Params.foodWeight);
		myVector = myVector.add(avgFoodVector);


		/********************Social effects**********************/
		//calculate the weights of each visible individual
		double sumSoc = 0;
		ArrayList<Double> weightsSoc = new ArrayList<Double>();
		ArrayList<RealVector> directionsSoc = new ArrayList<RealVector>();
		for(OMU ind : inds){

			if(familiarOMUs.contains(ind)){

				//calculate weight
				double weightSoc = familiarOMU_values.get(familiarOMUs.indexOf(ind));
				sumSoc = sumSoc + weightSoc;
				weightsSoc.add(weightSoc);

				//calculate the direction
				RealVector indVector = new ArrayRealVector(2);
				double distX = ind.getMyCoord().x-this.getMyCoord().x;
				double distY = ind.getMyCoord().y-this.getMyCoord().y;
				if((distX==0 && distY==0)==false){
					indVector.setEntry(0,distX);
					indVector.setEntry(1,distY);
					indVector.unitize();
					directionsSoc.add(indVector);
				}


			} else {

				//calculate weight
				double weightSoc = Params.famMinInd;
				sumSoc = sumSoc + weightSoc;
				weightsSoc.add(weightSoc);

				//calculate the direction
				RealVector indVector = new ArrayRealVector(2);
				double distX = ind.getMyCoord().x-this.getMyCoord().x;
				double distY = ind.getMyCoord().y-this.getMyCoord().y;
				if((distX==0 && distY==0)==false){
					indVector.setEntry(0,distX);
					indVector.setEntry(1,distY);
					indVector.unitize();
					directionsSoc.add(indVector);
				}
			}
		}

		//If there was a visible individual
		if(inds.size()>0){
			
			//standardize the patch weights to sum to one
			ArrayList<Double> weightsSocStan = new ArrayList<Double>();
			for(Double d : weightsSoc){
				weightsSocStan.add(d/sumSoc);
			}

			//calculate the avg direction (weights*dir to each ind)
			RealVector avgIndVector = new ArrayRealVector(2);
			for(int i = 0 ; i< weightsSocStan.size();i++){
				avgIndVector = avgIndVector.add(directionsSoc.get(i).mapMultiply(weightsSocStan.get(i)));
			}

			//add to myVector
			avgIndVector.unitize();
			avgIndVector = avgIndVector.mapMultiply(Params.socialWeight);
			myVector = myVector.add(avgIndVector);
		}


		/********************Effect of uncertainty**********************/
		
		//estimate uncertainty (length of the resulting movement vector / max possible length of the resulting vector)
		RealVector finalVector = new ArrayRealVector(2);
		double u = Math.atan2(myVector.getEntry(1), myVector.getEntry(0));
		double length = Math.pow(Math.pow(myVector.getEntry(0),2)+Math.pow(myVector.getEntry(1), 2),0.5);
		double maxLength = 0;
		if(inds.size()>0){
			maxLength = Params.bearingWeight + Params.foodWeight + Params.socialWeight ; 
		} else{
			maxLength = Params.bearingWeight + Params.foodWeight ; 
		}
		double k = Math.max(-2*Math.log(length/maxLength),0.0001);
		
		//add uncertainty to the final movement vector
		if(u!=0){
			u = u + VonMises.staticNextDouble(1/k); 
			double deltaX = Math.cos(u);
			double deltaY = Math.sin(u);
			finalVector.setEntry(0, deltaX);
			finalVector.setEntry(1, deltaY);
			//finalVector.unitize();
		}

		//multiply by movement scale and set travel information
		myTravelVector = finalVector.mapMultiply(Params.maxDistPerStep);
		
		//record current vector as previous vector for next movements
		this.setPreviousBearing(myTravelVector);

	}


	private void move(){
		ModelSetup.getGeog().moveByDisplacement(this, myTravelVector.getEntry(0), myTravelVector.getEntry(1));	
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
			myCell.eatMe();
			myCell.updateFam(this);
		}

	}

	/**************************
	 * 
	 * 
	 * OMU familiarization methods
	 * 
	 * 
	 **************************/

	private void updateFam(ArrayList<OMU> visibleInds){

		//increase familiarity with those visible
		for(OMU o : visibleInds){

			//already on my familiarity list
			if(familiarOMUs.contains(o)){

				int indexO = familiarOMUs.indexOf(o);
				double f = familiarOMU_values.get(indexO);
				if(f<0){
					f = Math.max( (f - (Params.iGrow-Params.iDecay) * (1-f)*f)+nd.sample(),Params.famMinInd);
				} else {
					f = Math.max( (f + (Params.iGrow-Params.iDecay) * (1-f)*f)+nd.sample(),Params.famMaxInd);
				}
				
				familiarOMU_values.set(indexO, f);

			//needing to be added to my familiarity list
			} else {
				
				//add new individual
				familiarOMUs.add(o);
				double f = Params.famMinInd;
				if(f<0){
					f = Math.max( (f - (Params.iGrow-Params.iDecay) * (1-f)*f)+nd.sample(),Params.famMinInd);
				} else {
					f = Math.min( (f + (Params.iGrow-Params.iDecay) * (1-f)*f)+nd.sample(),Params.famMaxInd);
				}
				familiarOMU_values.add(f);
			}
		}


		//decrease familiarity with those not visible
		ArrayList<OMU> removeFams = new ArrayList<OMU>();
		for(OMU inds: familiarOMUs){
			if(visibleInds.contains(inds)==false){
				int indexO = familiarOMUs.indexOf(inds);
				double f = familiarOMU_values.get(indexO);
				if(f<0){
					f = Math.max( (f  + Params.iDecay * (1-f)*f),Params.famMinInd);
				}else{
					f = Math.max( (f  - Params.iDecay * (1-f)*f),Params.famMinInd);
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
	public int getMyID(){
		return id;
	}
	public void setMyID(int i){
		id = i;
	}
	public void setPreviousBearing(RealVector r){
		previousBearing = r;
	}
	public RealVector getPreviousBearing(){
		return previousBearing;
	}

}
