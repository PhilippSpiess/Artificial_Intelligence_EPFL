import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

interface BiddingInt{
	

	public double Bid(double mc,double mcOp, Task task,Random random, 
   		 boolean useProfit, double mFuturProfit,double mFuturProfitOp,int debug_lvl);
	
	public void results(Long[] bids,int advID,int winner);

	
	
	
}
