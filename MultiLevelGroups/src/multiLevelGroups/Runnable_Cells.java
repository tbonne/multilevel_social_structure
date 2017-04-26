package multiLevelGroups;

public class Runnable_Cells implements Runnable {
	
	
Cell cell;
	
	Runnable_Cells(Cell c){
		cell = c;
	}
	
	@Override
	public void run(){
		Throwable thrown = null;
	    try {
		cell.stepThreaded();
	    } catch (Throwable e) {
	        thrown = e;
	        System.out.println("Problem lies in cell code" + thrown);
	    } finally {
	    	return;
	    }
	}
	    

}
