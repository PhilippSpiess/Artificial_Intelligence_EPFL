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
import logist.task.TaskDistribution;
import logist.task.DefaultTaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class Costs implements CostsInt{
    private Set<Task> previousTasks;
    private Set<Task> nextTasks;
    private double oldCost;
    private double newCost;
    private double exFutureProfitOld;
    private double exFutureProfitNew;
    private List<Vehicle> vehicles;
    Map<CityPair,Route> routes ;
    private double futureDiscount;
    private double exProfitMargin;
    private TaskDistribution distribution;

    static class Route{
    	private City start;
    	private double length;
    	private City end;
    	private double prob;
    	private int weight;
    	
    	public Route(City start, City end) {
    		this.start=start;
    		this.end=end;
    		this.length=start.distanceTo(end);
    		this.weight=0;
    	}
    	
    	public City getStart() {
    		return start;
    	}
    	public City getEnd() {
    		return end;
    	}
    	public void addProb(double newProb) {
    		prob=prob+newProb;
    	}
    	public void addWeight(int newWeight) {
    		weight=weight+newWeight;
    	}
    	public double getExpProfit() {
    		return length*weight*prob;
    	}
    	public void resetWeight() {
    		weight=0;
    	}
    }
    
    static class CityPair{
        public City key1;
        public City key2;
    	public CityPair(City s,City e) {
            this.key1 = s;
            this.key2 = e;
    	}
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
     
            CityPair key = (CityPair) o;
     
            if (key1 != null ? !key1.equals(key.key1) : key.key1 != null) return false;
            if (key2 != null ? !key2.equals(key.key2) : key.key2 != null) return false;
     
            return true;
        }
        @Override
        public int hashCode() {
            int result = key1 != null ? key1.hashCode() : 0;
            result = 31 * result + (key2 != null ? key2.hashCode() : 0);
            return result;
        }
    }
	
	
	
	public Costs(List<Vehicle> vehicles,double discount, double profit,TaskDistribution distribution) {
		this.previousTasks=new HashSet<Task>();
		this.oldCost=0;
		this.exFutureProfitOld=0;
		this.vehicles=vehicles;
		this.routes = new HashMap<CityPair,Route>();
		this.exProfitMargin=profit;
		this.futureDiscount=discount;
		this.distribution=distribution;
	}
	

	public void wonBid() {
		this.previousTasks=this.nextTasks;
		this.oldCost=this.newCost;
		this.exFutureProfitOld=this.exFutureProfitNew;
	}
	
	
	public void preCalc(Topology topology) {	
		for(City s :topology) {
			for(City e : s.neighbors()) {
				routes.put(new CityPair(s,e), new Route(s,e));
			}
		}	
		for(City s :topology) {
			for(City e : topology) {
				List<City> path=s.pathTo(e);
				City prev=s;
				for(City current:path) {
					Route route=routes.get(new CityPair(prev,current));
					route.addProb(distribution.probability(s, e)/topology.size());
					prev=current;
				}
				double p=distribution.probability(e, s);
			}
		}
		long end=System.currentTimeMillis();
	}
	
	public void addWeight(int weight,City start,City end) {
		Route route=routes.get(new CityPair(start, end));
		route.addWeight(weight);
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
    
    public double mfProfit(int nbTests,int nbTasks,long timeAvailable,Task task) {
    	exFutureProfitNew=0;
    	for(Route route:routes.values()) {
    		exFutureProfitNew=exFutureProfitNew+route.getExpProfit();
    		route.resetWeight();
    	}

    	double mF=(exFutureProfitNew-exFutureProfitOld)*Math.pow(futureDiscount,task.id)*exProfitMargin;
    	
    	if(mF<0) {
    		mF=0;
    	}
    	return mF;
    }

	
}