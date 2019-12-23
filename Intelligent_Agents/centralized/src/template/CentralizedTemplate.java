package template;

import java.awt.font.NumericShaper;
import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;
import java.util.*;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

	private static final double THRESHOLD = 0.2;
	private static final double THRESHOLD2 = 0.4;
	private static final int TRIALS = 5;
	private static final int NONE = -1;
	private static final int MAXITER = 10000;
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private int numTasks;
    private int numVehicles;
    
    public class Assignment {
    	private int [] nextTask;
    	private int [] time;
    	private int [] vehicle;
    	
    	public Assignment(int[] nT,int[] t,int [] v){ //Initial Constructor 
    		setNextTask(nT);
    		setTime(t);
    		setVehicle(v);
    	}
    	
    	public Assignment(Assignment copyAssignment) { //Copy Constructor
    		setNextTask(copyAssignment.getNextTask());
    		setTime(copyAssignment.getTime());
    		setVehicle(copyAssignment.getVehicle());
    	}
    	
    	
    	public void setNextTask(int [] nT) {
    		nextTask=new int[nT.length];
    		for(int i=0; i<nT.length;i++) {
    			nextTask[i]=nT[i];
    		}
    	}
    	public void setTime(int [] t) {
    		time=new int[t.length];
    		for(int i=0; i<t.length;i++) {
    			time[i]=t[i];
    		}
    	}
    	public void setVehicle(int [] v) {
    		vehicle=new int[v.length];
    		for(int i=0; i<v.length;i++) {
    			vehicle[i]=v[i];
    		}
    	}
    	
    	public int[] getNextTask() {
    		return this.nextTask;
    	}
    	
    	public int[] getTime() {
    		return this.time;
    	}
    	public int[] getVehicle() {
    		return this.vehicle;
    	}
    	
    }
    
    
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        numTasks = tasks.size();
        numVehicles = vehicles.size();
        
        
    	Task [] taskArray = new Task[tasks.size()];
    	for (Task tsk : tasks) {
    		taskArray[tsk.id]=tsk;
    	}
        //SLS Algorithm

        //Initial solution
    	Assignment Ainit= SelectInitialSolution(vehicles,numTasks,numVehicles);
    	Assignment A=Ainit;
    	
    	int resetMax=MAXITER;
    	int reset=0;
    	double minCost=getCost(Ainit,vehicles,taskArray);
    	Assignment minA=Ainit;
    	for( int i=0; i<TRIALS;i++) {
    		int j=0;
    		A=Ainit;
    		double smallestCost=getCost(A,vehicles,taskArray);
        	Assignment bestOverallA=A;
    		while(reset<resetMax) {
    			Assignment Aold= new Assignment(A);
    			List<Assignment> N= ChooseNeighbours(Aold,vehicles,taskArray);
    			
    			A=LocalChoice(Aold,N,vehicles,taskArray,i);
    			double cost=getCost(A,vehicles,taskArray);
    			if(cost<smallestCost) {
    				bestOverallA=A;
    				smallestCost=cost;
    				reset=0;
    			}
    			if(j%1000==0) {
    				System.out.println(reset);
    				System.out.println(i);
    				System.out.println(smallestCost);
    			}
    			if(reset>resetMax) {
    				A=Ainit;
    				reset=0;
    			}
    			reset=reset+1;
    			j=j+1;
    		}
    		if(smallestCost<minCost) {
    			minCost=smallestCost;
    			minA=bestOverallA;
    		}
    		
    		reset=0;
    	}
    	System.out.println(minCost);
    	List<Plan> PlanList = new ArrayList<Plan>(); 
    	for(Vehicle vehicle : vehicles) {
    		PlanList.add(makePlan(vehicle,taskArray, minA));
    	}
    	 
        return PlanList;
        
    }

    
    
    
    private Plan makePlan(Vehicle vehicle, Task[] tasks,Assignment A) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current); 
        int[] nextTask=A.getNextTask();
        int[] time=A.getTime();
        int[] vehicles=A.getVehicle();
        int v = vehicle.id();
        int actual_task = nextTask[numTasks*2 + v];
        int pickup_deliver = actual_task%2; // if pair: pickup, impair: delivery
        
        if (actual_task == NONE) {
        	 return Plan.EMPTY;
        }
        else {
        	while (actual_task != NONE){
        		pickup_deliver = actual_task%2; 
        		Task task=tasks[actual_task/2];
        		if (pickup_deliver == 0) {
        			// move: current city => pickup location
        			for (City city : current.pathTo(task.pickupCity)){
        				plan.appendMove(city);
        			}
        			plan.appendPickup(task);
        			current = task.pickupCity;
        		}

        		if (pickup_deliver == 1) {

        			// move: pickup location => delivery location
        			for (City city : current.pathTo(task.deliveryCity)) {
        				plan.appendMove(city);
        			}
        			plan.appendDelivery(task);

        			// set current city
        			current = task.deliveryCity;
        		}
        		actual_task = nextTask[actual_task];
        	}
            		
            	}
        		
        	return plan;
    }

    
    
    private List<Assignment> ChooseNeighbours(Assignment Aold, List<Vehicle> vehicles, Task[] tasks) {
	    double ACost=0;
	    double newCost=0;
	    int vi;
	    List<Assignment> N= new ArrayList<Assignment>();
	 
	    //repeat until vehicle ok selected
	    Random r=new Random();
	    vi=r.nextInt(numVehicles);
	    while (Aold.nextTask[vi+numTasks*2] == NONE) {
	        vi=r.nextInt(numVehicles);
	    }
	    int maxweight=vehicles.get(vi).capacity();
	    
        for (int vj=0; vj<numVehicles;vj++){//adds first task of chosen vehicle to all others as new solution states
		    if (vi != vj) {	
		    	for(int taskSwap=0;taskSwap<numTasks*2;taskSwap++) {
	    			if(taskSwap%2==0 && Aold.getVehicle()[taskSwap]==vi) {
		    			if(tasks[taskSwap/2].weight<vehicles.get(vj).capacity()) {
		    				N.add(ChangingVehicle(Aold,vi+numTasks*2,vj+numTasks*2,taskSwap));
		    			}
	    			}

		    	}

		 	}
	    }
	    
	    //Look at all possible Tasks order swap for given vehicles
	    
	    //ADD all constraints
        int length=0;
        int t=vi+numTasks*2;
	    while(t!=NONE) {
	    	t=Aold.nextTask[t];
	    	length=length+1;
	    }
	    length=length-1;
	    if(length >=4) {
	    	for(int tI1=0; tI1<length-1;tI1++) { //CAREFUL tI1 and tI2 represent the task# carried by the vehicle, not the task# overall
	    		for(int tI2=tI1+1; tI2<length;tI2++) {
	    			Assignment An=ChangingTaskOrder(Aold,vi+numTasks*2,tI1,tI2, tasks,maxweight);
	    			if(An!=null) {
	    				N.add(An);
	    			}
	    		}
	    	}
	    }
	    return N;
    }
    
  
    private Assignment LocalChoice(Assignment Aold, List<Assignment> N,List<Vehicle> vehicles, Task[] tasks,int i){
    	double smallestCost=Double.MAX_VALUE;
    	Assignment Abestoverall;
    	Assignment Arandom = Aold;
    	int [] num_element;
    	num_element=new int[N.size()];
    	for(int ite=0;ite<N.size();ite++) {
    		num_element[ite] = ite;
    	}
    	int random = getRandom(num_element);
    	
    	int k=0;
    	List<Assignment> bestList= new ArrayList<CentralizedTemplate.Assignment>();
    	for(Assignment A: N) {
    		k=k+1;
    		if (k==random) {
    			 Arandom = A;
    		}
    		double cost = getCost(A,vehicles,tasks);
    		if(cost<smallestCost) {
    			smallestCost=cost;
    			bestList.clear();
    			bestList.add(A);
    		}
    		else if(cost==smallestCost) {
    			bestList.add(A);
    		}	
    	}
	    Random r=new Random();
	    Assignment Abest=bestList.get(r.nextInt(bestList.size()));
	    if(smallestCost<getCost(Aold,vehicles,tasks)) {
	    	
	    }
	    double proba = Math.random();
	
	    
	    if (proba < THRESHOLD) {
	    	return Abest;
	    }
	    else if (proba < THRESHOLD2) {
	    	return Arandom;
	    }	
	    else {
	    	return Aold;
	    }
	    
    }
    
    public static int getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }
     
    //How to get the cost for each tasks of a vehicle plan
    private double getCost(Assignment A,List<Vehicle> vehicles, Task[] tasks){
    	double totalCost=0;
    	for(int i=0;i<numTasks*2;i++) {
    		int nt=A.getNextTask()[i];
    		if(nt==NONE) {
    			continue;
    		}
    		if(i%2==0) { //i is a pickup task
    			if(nt%2==0) {
    				totalCost=totalCost+tasks[i/2].pickupCity.distanceTo(tasks[nt/2].pickupCity)*vehicles.get(A.getVehicle()[i]).costPerKm();
    			}
    			else {
    				totalCost=totalCost+tasks[i/2].pickupCity.distanceTo(tasks[nt/2].deliveryCity)*vehicles.get(A.getVehicle()[i]).costPerKm();
    			}
    		}
    		else {
    			if(nt%2==0) {
    				totalCost=totalCost+tasks[i/2].deliveryCity.distanceTo(tasks[nt/2].pickupCity)*vehicles.get(A.getVehicle()[i]).costPerKm();
    			}
    			else {
    				totalCost=totalCost+tasks[i/2].deliveryCity.distanceTo(tasks[nt/2].deliveryCity)*vehicles.get(A.getVehicle()[i]).costPerKm();
    			}
    		}
    	}
    	for(int k=0; k<numVehicles;k++) {
    		int nt=A.getNextTask()[k+numTasks*2];
    		if(nt==NONE) {
    			continue;
    		}
    		totalCost=totalCost+vehicles.get(k).homeCity().distanceTo(tasks[nt/2].pickupCity)*vehicles.get(k).costPerKm();
    	}
	    return totalCost;
    }
    
    

    public int getRandomElement(List<Integer> list) 
    { 
        Random rand = new Random(); 
        return list.get(rand.nextInt(list.size())); 
    } 
	 
    public Assignment SelectInitialSolution(List<Vehicle> vehicles,int numTasks, int numVehicles) {
    	int[] nextTask = new int[numTasks*2+numVehicles]; // [null, 2, 3, 4, ..., 60, 61; 1(the first vehicle here), 0, 0, 0]
        int [] time= new int[numTasks*2]; // [ 1, 2, 3, 4 ...]
        int [] vehicle = new int[numTasks*2]; //[ 0, 0, 0, 0, 0, ...]
    	
        //find largest vehicle
        int maxcapa=0;
        int vehicleNmbr=0;
        for(int veh=0;veh<numVehicles;veh++) {
        	int capa = vehicles.get(veh).capacity();
        	if(capa>maxcapa) {
        		maxcapa=capa;
        		vehicleNmbr=veh;
        	}
        }
        
        //The first task start at task = 1
    	for (int i = 0; i < numTasks*2; i++) {
    		
    		if (i < numTasks*2-1){
    			nextTask[i] = i+1;
    			time[i]=i;
    			vehicle[i] = vehicleNmbr;	
    		}
    		else if(i==numTasks*2-1) {
    			nextTask[i] = NONE ;
    			time[i]=i;
    			vehicle[i] = vehicleNmbr;	
    		}
    	}
    	for(int i = 0; i < numVehicles; i++) {
    		int j=i+numTasks*2;
    		if (i == vehicleNmbr){
    			nextTask[j] = 0;
    		}
    		else {
    			nextTask[j] = NONE;
    		}
    	}
    
        
        
    	return new Assignment(nextTask, time, vehicle);
    }

    private Assignment ChangingVehicle(Assignment A,int v1,int v2,int taskSwap) {
    	Assignment A1=new Assignment(A);
    	int[] nT=A1.getNextTask();
    	int Pret=v1;
    	int t=nT[v1];
    	while(t!=taskSwap) {
    		Pret=t;
    		t=nT[Pret];
    	}
    	nT[Pret]=nT[t];
    	nT[t]=nT[v2];
    	nT[v2]=t;
    	int twin=t+1; //has to be a pickup, first task of vehicle
    	int twP=v1;
    	while(nT[twP]!=twin) {
    		twP=nT[twP];
    	}
    	nT[twP]=nT[twin];
    	nT[twin]=nT[t];
    	nT[t]=twin;
    	
    	A1.setNextTask(nT);
    	UpdateTime(A1,v1);
    	UpdateTime(A1,v2);
    	int[] veh=A1.getVehicle();
    	veh[t]=v2-numTasks*2;
    	veh[twin]=v2-numTasks*2;
    	return A1;
    }
    
	private Assignment ChangingTaskOrder(Assignment A, int vi, int tI1, int tI2, Task[] tasks, int maxweight) {
		Assignment A1=new Assignment(A);
		int tPre1=vi;
		int t1=A1.getNextTask()[tPre1];
		int count=0;
		boolean swapBoth=false;
		while(count<tI1) {
			tPre1=t1;
			t1=A1.getNextTask()[t1];
			count=count+1;
		}
		int tPost1=A1.getNextTask()[t1];
		int tPre2=t1;
		int t2=A1.getNextTask()[tPre2];
		count=count+1;
		while(count<tI2) { 
			tPre2=t2;
			t2=A1.getNextTask()[t2];
			count=count+1;
		}
		int tPost2=A1.getNextTask()[t2];
		
		if(t1%2==0) { //task is pickup, the deliver task needs to be after the proposed new location
			if(A1.getTime()[t2]>=A1.getTime()[t1+1]) {
				if(t2%2==0) {
					swapBoth=true;
				}
				else {
					return null;
				}
			}
				
		}
		if(t2%2==1) {// task is deliver, pickup task needs to be before proposed new location
			if(A1.getTime()[t1]<=A1.getTime()[t2-1])
				return null;
		}
		

		
		if(tPost1==t2) {
			int nT[]=A1.getNextTask();
			nT[tPre1]=t2;
			nT[t2]=t1;
			nT[t1]=tPost2;
			A1.setNextTask(nT);
		}
		else {
			int nT[]=A1.getNextTask();
			nT[tPre1]=t2;
			nT[tPre2]=t1;
			nT[t2]=tPost1;
			nT[t1]=tPost2;
			A1.setNextTask(nT);
		}
		
		if(swapBoth==true) {
			int tw1=A1.getNextTask()[vi];
			int twPre1=vi;
			while(tw1!=t1+1) {
				twPre1=tw1;
				tw1=A1.getNextTask()[tw1];
			}
			int twPost1=A1.getNextTask()[tw1];

			int tw2=A1.getNextTask()[vi];
			int twPre2=vi;
			while(tw2!=t2+1) {
				twPre2=tw2;
				tw2=A1.getNextTask()[tw2];
			}
			int twPost2=A1.getNextTask()[tw2];
			if(twPost1==tw2) {
				int nT[]=A1.getNextTask();
				nT[twPre1]=tw2;
				nT[tw2]=tw1;
				nT[tw1]=twPost2;
				A1.setNextTask(nT);
			}
			else {
				int nT[]=A1.getNextTask();
				nT[twPre1]=tw2;
				nT[twPre2]=tw1;
				nT[tw2]=twPost1;
				nT[tw1]=twPost2;
				A1.setNextTask(nT);
			}
		}

		
		tPre1=vi;
		t1=A1.getNextTask()[tPre1];
		int weight=0;
		while(t1!=NONE) {
			if(t1%2==0) {
				weight=weight+tasks[t1/2].weight;
			}
			else {
				weight=weight-tasks[t1/2].weight;
			}
			
			if(weight>maxweight) {
				return null;
			}
			tPre1=t1;
			t1=A1.getNextTask()[t1];
		}
			
		UpdateTime(A1,vi);
		
		
		return A1;
	}
	
	public void UpdateTime(Assignment A, int vi) {
		int ti=A.getNextTask()[vi];
		if(ti!=NONE) {
			int[] timelocal=A.getTime();
			timelocal[ti]=1;
			int tj=A.getNextTask()[ti];
			while(tj!=NONE) {
				timelocal[tj]=timelocal[ti]+1;
				ti=tj;
				tj=A.getNextTask()[ti];
			}
			A.setTime(timelocal);
		}
		
	}

}


