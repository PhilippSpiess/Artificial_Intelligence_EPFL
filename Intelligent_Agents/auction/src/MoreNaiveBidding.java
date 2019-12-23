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


public class MoreNaiveBidding implements BiddingInt{

	List<Long> bidsAdTab;
	
	public MoreNaiveBidding() { //constructor

	}
	
	
	public void Bid(long bid) {

	}

	
	public double Bid(double mc,double mcOp, Task task,Random random, 
    	boolean useProfit, double mFuturProfit,double mFuturProfitOp,int debug_lvl) {
		double bid=1.5*mc;
		return bid;
    }
    
	public void results(Long[] bids,int advID,int winner) {
		
	}
  

	
}

