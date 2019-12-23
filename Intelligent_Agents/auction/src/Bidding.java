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


public class Bidding implements BiddingInt{
	
  List<Long> bidsAdTab; //all bids of the adversary?
  List<Long> bidsTab; //all our bids
  List<Double> marginalCostsAd; //marginal costs Calculated for the adversary
  List<Double> marginalCost; //marginal costs Calculated for us

  private double FirstAdvantage;
  public double DeltaEstimate;
  private Topology topology;
  private Agent agent;
  private boolean oppWonOne;
  private double factor_hSw_tot;
  private double factor_wSw_tot;

	
	public Bidding(Topology topology,Agent agent) {
		this.bidsAdTab = new ArrayList<Long>();
		this.bidsTab = new ArrayList<Long>();
		this.marginalCostsAd = new ArrayList<Double>();
		this.marginalCost = new ArrayList<Double>();
		this.topology=topology;
		this.agent=agent;
		this.oppWonOne=false;
		this.factor_hSw_tot=1;
		this.factor_wSw_tot=1;
	}
	
	public double getDeltaEstimate() {
		return DeltaEstimate;
	}

	public double getFirstAdvantage() {
		return FirstAdvantage;
	}
	
	public void addBid(long bidadv,long bidus) {
		bidsAdTab.add(bidadv);
		bidsTab.add(bidus);
	}
	
	public void addMCost( double mcOp,double mc) {
		marginalCostsAd.add(mcOp);
		marginalCost.add(mc);
	}
	
	
	public void results(Long[] bids,int advID,int winner) {
		addBid(bids[advID],bids[agent.id()]);
		if(winner!=agent.id()) {
			this.oppWonOne=true;
		}
	}

	
	public double Bid(double mc,double mcOp, Task task,Random random, 
   		 boolean useProfit, double mFuturProfit,double mFuturProfitOp,int debug_lvl){
		double deltaEs=0;
    	if (oppWonOne==false) {
			this.estimateAdvantage(agent.vehicles(), task, topology);
			FirstAdvantage = this.getFirstAdvantage();
		}
		else {
			this.correctEstimate();
			deltaEs = this.getDeltaEstimate();
		}
   	
   	
   	double ratioBelow = 1.0 - (random.nextDouble() * 0.1);
   	double ratioAbove = 1.0 + (random.nextDouble() * 0.1);
   	double bid;
   	
   	//First Move: Bid just a bit above the marginal cost + the potential advantage - mFuturProfit
	if (FirstAdvantage !=0) {
		if(mc<mcOp) {
			bid=mc+(mcOp-mc+FirstAdvantage-mFuturProfit)*0.6;
		}
		else {
			bid = mcOp + FirstAdvantage-mFuturProfit;
			bid = ratioAbove * bid;
		}
		FirstAdvantage=0;
	}
	
	else if (mcOp > mc){
   		
		//if (random.nextDouble() < 0.8) {
		bid = mcOp; 
		bid = ratioBelow * bid*Math.cbrt(factor_wSw_tot);
		if(debug_lvl>1) {
			System.out.printf("The wsw factor is %f",factor_wSw_tot);
		}


		//	}
		//else {
		//bid = ratioAbove * mc ;
		//}
	}

	//if mcOp is smaller then our mc: bid above (mc + deltaEs) - mFuturProfit
	else {
		bid = ratioAbove * (mc) - mFuturProfit;
		bid = bid *Math.cbrt(factor_wSw_tot);
		if(debug_lvl>1) {
			System.out.printf("The hsw factor is %f",factor_hSw_tot);		}
	}
	
   	if (bid < 0) {
			bid = 0;
		}

   	addMCost(mcOp,mc);
   	
   	
   	/*
		System.out.println(mc);
		System.out.println(mcOp);
		
		System.out.println(FirstAdvantage);
		System.out.println(deltaEs);
		*/
		return bid;
   }
    
    

	//FOR THE FIRST TASK ONLY
	public void estimateAdvantage(List<Vehicle> vehicles, Task task, Topology topology){
		
		double minDistance=Double.MAX_VALUE;
		double Distance=0;
		double TotalDistance=0;
		double AverageDistance=0;
		Vehicle v1 = vehicles.get(0);
		
		for (Vehicle vehicle : vehicles){
			
			if(vehicle.capacity()>task.weight) {
			
				Distance = vehicle.homeCity().distanceTo(task.pickupCity);
				if (Distance < minDistance) {
					minDistance = Distance;
					v1 = vehicle;
				}
			}
		}
		
		for (City city : topology.cities()) {
			
			Distance = city.distanceTo(task.pickupCity);
			TotalDistance += Distance;
	
		}
		AverageDistance = TotalDistance/topology.cities().size();

		FirstAdvantage = (AverageDistance-minDistance)*v1.costPerKm() ;
	}
	
	//Return the difference between average bidding and average estimated bid.
	public void correctEstimate() {
		int nbPastBids= bidsTab.size();
		int i=nbPastBids-1;
			double factor_wSw = 1;//delta we should win (MC)
			double factor_hSw = 1;//delta he should win (MC)
			double advbid=(double) this.bidsAdTab.get(i);
			double ourbid=(double) this.bidsTab.get(i);
			double advMC=this.marginalCostsAd.get(i);
			double ourMC=this.marginalCost.get(i);
			if(ourbid>1) {
			if(advMC>ourMC) { //we should have won it
				if(ourbid<advbid) {//we actually won it... how much could we have increased the offer?
					factor_wSw=advbid/ourbid;
				}
				else if(ourbid>advbid) {//we actually lost it... by how much should we have decreased to win? (Limit is MC)
					if(advbid<ourMC) {
						//do nothing (ignore future for now)
						factor_wSw=advbid/ourbid;
					}
					else {
						factor_wSw=advbid/ourbid;
					}
				}
				factor_wSw_tot=factor_wSw_tot*0.5+factor_wSw*0.5;
			}
			else if(advMC<=ourMC) { //he should have won it
				if(advbid>ourbid) {// but we did, how much could we have increased and still win?
					factor_hSw=advbid/ourbid;
				}
				else if(advbid<ourbid) { //and he did, by how much should we have decreased to win (and still make a profit)
					if(advbid<ourMC) {
						//do nothing (ignore future for now)
						factor_hSw=advbid/ourbid;
					}
					else {
						factor_hSw=advbid/ourbid;
					}
				}
				factor_hSw_tot=factor_hSw_tot*0.5+factor_hSw*0.5;
			}

		}
		
		
		
		//DeltaEstimate  = delta/this.bidsAdTab.size(); //Average of deviation between bid MC estimate and actual bid of the opponent
	}
	
	/*
	 
	public LinearRegression(double[] x, double[] y) {
		
		private final double intercept, slope;
	
	        if (x.length != y.length) {
	            throw new IllegalArgumentException("array lengths are not equal");
	        }
	        int n = x.length;

	        // first pass
	        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
	        for (int i = 0; i < n; i++) {
	            sumx  += x[i];
	            sumx2 += x[i]*x[i];
	            sumy  += y[i];
	        }
	        double xbar = sumx / n;
	        double ybar = sumy / n;

	        // second pass: compute summary statistics
	        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
	        for (int i = 0; i < n; i++) {
	            xxbar += (x[i] - xbar) * (x[i] - xbar);
	            yybar += (y[i] - ybar) * (y[i] - ybar);
	            xybar += (x[i] - xbar) * (y[i] - ybar);
	        }
	        slope  = xybar / xxbar;
	        intercept = ybar - slope * xbar;

	        // more statistical analysis
	        double rss = 0.0;      // residual sum of squares
	        double ssr = 0.0;      // regression sum of squares
	        for (int i = 0; i < n; i++) {
	            double fit = slope*x[i] + intercept;
	            rss += (fit - y[i]) * (fit - y[i]);
	            ssr += (fit - ybar) * (fit - ybar);
	        }

	        int degreesOfFreedom = n-2;
	        r2    = ssr / yybar;
	        double svar  = rss / degreesOfFreedom;
	        svar1 = svar / xxbar;
	        svar0 = svar/n + xbar*xbar*svar1;
	    }

	   */
}

