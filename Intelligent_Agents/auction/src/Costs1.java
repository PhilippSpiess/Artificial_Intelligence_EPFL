import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.task.TaskDistribution;
import logist.task.DefaultTaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;



public class Costs1 implements CostsInt{
    private Set<Task> previousTasks;
    private Set<Task> nextTasks;
    private double oldCost;
    private double newCost;
    private double exFutureProfitOld;
    private double exFutureProfitNew;
    private List<Vehicle> vehicles;
    private double futureDiscount;
    private double exProfitMargin;
    private DefaultTaskDistribution distribution;

    
	
	public Costs1(List<Vehicle> vehicles,double discount, double profit,TaskDistribution distribution) {
		this.previousTasks=new HashSet<Task>();
		this.oldCost=0;
		this.exFutureProfitOld=0;
		this.vehicles=vehicles;
		this.exProfitMargin=profit;
		this.futureDiscount=discount;
		this.distribution=(DefaultTaskDistribution) distribution;
	}
	

	public void wonBid() {
		this.previousTasks=this.nextTasks;
		this.oldCost=this.newCost;
		this.exFutureProfitOld=this.exFutureProfitNew;
	}
	
	
	public void preCalc(Topology topology) {	
		
	}
	
	public void addWeight(int weight,City start,City end) {
		//do nothing here as no need for routes
	}
	

    public double mCost(Task task,long timeAvailable) {
    	SLS.hyperParams quickSLS = new SLS.hyperParams(0.33, 0.66, 1,500,this);
        SLS slsPlanner=new SLS(quickSLS);
        this.nextTasks=new HashSet<Task>(previousTasks);
    	this.nextTasks.add(task);
    	this.newCost=slsPlanner.getBestCost(vehicles, nextTasks,timeAvailable);
    	double mc=(newCost-oldCost);
    	return mc;
    }
    
    public double mfProfit(int nbTests,int nbTasks,long timeAvailable,Task taskNew) {
    	SLS.hyperParams quickSLS = new SLS.hyperParams(0.5, 0.8, 1,100,this);
        SLS slsPlanner=new SLS(quickSLS);

    	Set<Task> futurePrevious=new HashSet<Task>(previousTasks);
    	Set<Task> futureNext=new HashSet<Task>(nextTasks);
    	double bestMargin=0;
    	double summargin=0;
    	for(int test=0; test<nbTests;test++) {
    		for(int task=0; task<nbTasks;task++) {
    			Task added=distribution.createTask();
    			futurePrevious.add(added);
    			futureNext.add(added);
    		}
    		double futureCostPrev=slsPlanner.getBestCost(vehicles, futurePrevious,(long) (timeAvailable/(nbTests*2))); //ISSUE CAUSE all newly generated tasks have ID 0
    		double futureCostNext=slsPlanner.getBestCost(vehicles, futureNext,(long) (timeAvailable/(nbTests*2)));
    		double margin= -futureCostNext+futureCostPrev+newCost-oldCost;
    		summargin=summargin+margin;
    		if(margin<bestMargin) {
    			bestMargin=margin;	
    		}
    	}
    	double avgmargin=summargin/nbTests;
    	
    	return avgmargin*Math.pow(futureDiscount,taskNew.id);
    }	
}