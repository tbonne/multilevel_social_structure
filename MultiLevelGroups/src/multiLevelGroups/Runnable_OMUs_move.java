package multiLevelGroups;

public class Runnable_OMUs_move implements Runnable  {
	
OMU omu;
	
	Runnable_OMUs_move(OMU o){
		omu = o;
	}
	
	@Override
	public void run(){
		Throwable thrown = null;
	    try {
		omu.action();
	    } catch (Throwable e) {
	        thrown = e;
	        System.out.println("Problem lies in OMU action code" + thrown);
	    } finally {
	    	return;
	    }
	}
	    

}
