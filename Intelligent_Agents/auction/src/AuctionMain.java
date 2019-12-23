
//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
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
public class AuctionMain implements AuctionBehavior {
	private static final int DEBUG=1;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private City currentCity;
    private long timeout_setup;
    private long timeout_plan;
    private long timeout_bid;
    private CostsInt costCalc;
    private CostsInt opponentCalc;
    private BiddingInt bidStrat;
    private BiddingInt bidStratAd;
    private List<Vehicle> vehicles;
    private Profit profitCalc;
    private String cCName;
	private String BName;
    private int taskNum;
    
    enum CostCalculators { ROUTES, FUTURE}
    enum BiddingAlgo { NAIVE,CLASSIC,NOLEARN,FUTURENOLEARN}

    
    @Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
        LogistSettings ls = null;
        CostCalculators cC;
        BiddingAlgo B;
        
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
    	// the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        timeout_bid=ls.get(LogistSettings.TimeoutKey.BID);
    	
    	//Get type of estimation and bidding & create the instances.
		cCName = agent.readProperty("costs", String.class, "ROUTES");
		BName = agent.readProperty("bid", String.class, "NAIVE");
		cC = CostCalculators.valueOf(cCName.toUpperCase());
		B = BiddingAlgo.valueOf(BName.toUpperCase());
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.currentCity = vehicles.get(0).homeCity();
		this.profitCalc=new Profit();
		switch (cC) {
		case ROUTES:
			this.costCalc= new Costs(vehicles,0.85,0.1,distribution);
			this.opponentCalc=new Costs(vehicles,0.85,0.1,distribution);
			break;
		case FUTURE:
			this.costCalc= new Costs1(vehicles,0.95,0.1,distribution);
			this.opponentCalc=new Costs1(vehicles,0.95,0.1,distribution);
			break;
		default:
			throw new AssertionError("Should not happen.");
	
		}	
		switch (B) {
		case NAIVE:
			this.bidStrat= new NaiveBidding();
			break;
		case CLASSIC:
			this.bidStrat= new Bidding(topology,agent);
			break;
		case NOLEARN:
			this.bidStrat= new BiddingNoLearn(topology,agent);
			break;
		case FUTURENOLEARN:
			this.bidStrat= new BiddingFutureNoLearn(topology,agent);
			break;
		default:
			throw new AssertionError("Should not happen.");
	
		}	
		
		costCalc.preCalc(topology);
		opponentCalc.preCalc(topology);
		
		
		
		/*
		 * This was here before
		 * -9019554669489983951L
		 */
		long seed = -920934568947420751L * currentCity.hashCode() * agent.id();
		this.random = new Random();
		/*
		 * End
		 */
		
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		int advID=(agent.id()+1)%2;
		if (winner == agent.id()) {
			/*
			 * This was here before
			 */
			currentCity = previous.deliveryCity;
			/*
			 * End
			 */
			costCalc.wonBid();
			profitCalc.won(bids[agent.id()]);
		}
		else {
			opponentCalc.wonBid();
		}
		bidStrat.results(bids,advID,winner);
	}
	
	
	@Override
	public Long askPrice(Task task) {
		if(DEBUG>1) {
			System.out.println(task);
		}
		long start=System.currentTimeMillis();
		double FirstAdvantage=0;
		double deltaEstimate=0;
		//do random search?
		double factor_1=1;
		double factor_2=1;
		boolean useProfit = true;
		

  		double marginalCost=costCalc.mCost(task,(long)(0.002*timeout_bid)); //Use Costs class to find real marginal cost;
  		double marginalCostOp=opponentCalc.mCost(task,(long)(0.002*timeout_bid));
		double marginalFutureProfit=costCalc.mfProfit(10,3,(long)(0.002*timeout_bid),task);
		double marginalFutureProfitOp=opponentCalc.mfProfit(10,3,(long)(0.002*timeout_bid),task);

		
		/*
		 * Phillip
		 */

		
		double bid=bidStrat.Bid(marginalCost,marginalCostOp, task,random, 
				 useProfit, marginalFutureProfit, marginalFutureProfitOp,DEBUG);
		
		if(DEBUG>1) {
			System.out.printf("Agent %d: Marginal Cost Agent:%f Future Profit Agent %f \nAgent %d: Marginal Cost Opponent: %f Future Profit Opponent %f\n",agent.id(),marginalCost,marginalFutureProfit,agent.id(),marginalCostOp,marginalFutureProfitOp);
			System.out.printf("Agent %d: The bid values was %d\n",agent.id(),(int) Math.round(bid));
			long end= System.currentTimeMillis();
	        System.out.printf("Agent %d: Time required for bid computation: %d. Time Available: %d \n",agent.id(), (end-start), timeout_bid);
		}
		taskNum=task.id;
		return (long) Math.round(bid);
	}

	
	
	//From here it is simply SLS implementation to find the final plan. SLS is now a class.
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long start=System.currentTimeMillis();
		SLS.hyperParams optimalSLS = new SLS.hyperParams(0.33, 0.66, 2,3000,costCalc);
        SLS slsPlanner=new SLS(optimalSLS);
        long SLStime=(long) (timeout_plan*0.0095-(System.currentTimeMillis()-start));
        List<Plan> bestPlan= slsPlanner.getBestPlan(vehicles, tasks,SLStime);
        double bestCost=0;
        for(Vehicle vehicle : vehicles) {
            bestCost=bestCost+bestPlan.get(vehicle.id()).totalDistance()*vehicle.costPerKm();
        }
        if(DEBUG>0) {
        	double profit=profitCalc.winnings-bestCost;
        	String cityname=topology.cities().get(0).name;
        	System.out.println(profit);
    	    System.out.printf("Agent: %s , %s, %d. T %s. Prof %f Total Tasks=%d\n",cCName,BName,agent.id(),cityname,profit,taskNum);

	        if(DEBUG>1) {
		        long end= System.currentTimeMillis();
		        System.out.printf("Time required for plan computation: %d. Time Available: %d\n", (end-start),timeout_plan);
	        }
        }
        /*
        if(true) {
        	throw new AssertionError("Should not happen."); //Forces exit to start another sim.
        }*/
        return bestPlan;
    }
}
