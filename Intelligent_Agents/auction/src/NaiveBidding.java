//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class NaiveBidding implements BiddingInt{

	List<Long> bidsAdTab;
	
	public NaiveBidding() { //constructor

	}
	
	
	public void Bid(long bid) {

	}

	
	public double Bid(double mc,double mcOp, Task task,Random random, 
    	boolean useProfit, double mFuturProfit,double mFuturProfitOp,int debug_lvl) {
		double bid=0;
		if(mc<mcOp) {
    		bid=0.99*mcOp;
    	}
		else if(mc>=mcOp) {
			bid=mc*1.05;
		}
		return bid;
    }
    
	public void results(Long[] bids,int advID,int winner) {
		
	}
  

	
}

