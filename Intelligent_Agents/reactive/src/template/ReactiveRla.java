package template;

import java.util.Arrays;
//import java.util.Random;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveRla implements ReactiveBehavior {
	
	private static final int TAKETASKACTION=0;

	private int numActions;
	private Agent myAgent;

	private double[][][] Transition_Table;
	
	private double[][] Reward_Table;
	
	private double[] Strategy_Table;
	private double[] Strategy_Table_Previous;
	private double[] Q_values;
	private int[] Policy_Table;
	private double discount=0;
	private int NumCities;
	
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		int costperkm=agent.vehicles().get(0).costPerKm();
		System.out.printf("Discount factor is %f",discount);
		this.numActions = 0;
		this.myAgent = agent;
		int state=0;
		int stateP=0;

		
		NumCities=topology.size();
		Transition_Table= new double[NumCities*NumCities][NumCities+1][NumCities*NumCities];
		Reward_Table=new double[NumCities*NumCities][NumCities+1]; 
		//fill the reward table
		for(City cityLocation : topology){
			for(City cityTarget : topology) {//iterate over states
				state=cityLocation.id*NumCities+cityTarget.id;
				
				Reward_Table[state][TAKETASKACTION]=td.reward(cityLocation, cityTarget)-cityLocation.distanceTo(cityTarget)*costperkm; //assuming that r(ij) has a value of 0 for the reward
				for(City cityLocationP : topology){
					for(City cityTargetP : topology) {//iterate over states
						stateP=(cityLocationP.id*NumCities)+cityTargetP.id;
						if(cityLocationP==cityTarget){
							Transition_Table[state][TAKETASKACTION][stateP]=td.probability(cityTarget, cityTargetP);
							if(cityTarget==cityTargetP) {
								Transition_Table[state][TAKETASKACTION][stateP]=td.probability(cityTarget, null);
							}
						}
						else{
							Transition_Table[state][TAKETASKACTION][stateP]=0;
						}
					}
				}
				
				//iterate over actions not taking a task
				for(City actionCity : topology) { 
					Reward_Table[state][actionCity.id+1]=-cityLocation.distanceTo(actionCity)*costperkm; //cost of kilometers if carrying empty load. Also include non-neighboring as will be dealt with later
					for(City cityLocationP : topology){
						for(City cityTargetP : topology) {
							stateP=cityLocationP.id*NumCities+cityTargetP.id;
							if(cityLocationP==actionCity){
								Transition_Table[state][actionCity.id+1][stateP]=td.probability(actionCity, cityTargetP);
								if(actionCity==cityTargetP) {
									Transition_Table[state][actionCity.id+1][stateP]=td.probability(actionCity, null);
								}
							}
							else{
								Transition_Table[state][actionCity.id+1][stateP]=0;
							}
						}
					}
				}	
			}
		}
		Strategy_Table=new double[NumCities*NumCities];
		Strategy_Table_Previous=new double[NumCities*NumCities];
		Arrays.fill(Strategy_Table_Previous, 1);
		Policy_Table=makeStrategy(topology);
	}
	

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		int actionID;
		int stateID;

		City currentCity = vehicle.getCurrentCity();
		City destinationCity;
		
		if(availableTask != null) {
			destinationCity = availableTask.deliveryCity;
			stateID = currentCity.id*NumCities+destinationCity.id;
		} else {
			destinationCity = null;
			stateID=currentCity.id*NumCities+currentCity.id;
			
		}
		//come up with no task situation

		actionID = Policy_Table[stateID];
		
		if (actionID == 0){
			action = new Pickup(availableTask);;
		} else{
			action = new Move(currentCity.neighbors().get(actionID-1));
		}
	
		if (numActions == 1000) {
			System.out.println(myAgent.name() + ": The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	
		
	//implement V value Table
	public int[] makeStrategy(Topology topology){
		int state=0;
		int j=0;
		while(checkConvergence() != true){
			j++;
			System.arraycopy(Strategy_Table, 0, Strategy_Table_Previous, 0, Strategy_Table.length);
			//iterate to change the strategy table until good enough
			for(City cityLocation : topology){
				for(City cityTarget : topology){
					state=cityLocation.id*NumCities+cityTarget.id;

					//first action Q value
					if(cityLocation!=cityTarget) {
						Q_values=new double[cityLocation.neighbors().size()+1];
						double sum = 0;
						for(int futur = 0; futur < Strategy_Table.length; futur++){
							
							sum = sum + Transition_Table[state][TAKETASKACTION][futur]*Strategy_Table_Previous[futur];
						}
						Q_values[TAKETASKACTION] = Reward_Table[state][TAKETASKACTION] + discount * sum;
					
						//remaining actions Q values (
						int nth_neighbor=1;
						for(City neighbors : cityLocation) {
							sum = 0;
							for(int futur = 0; futur < Strategy_Table.length; futur++){

								sum = sum + Transition_Table[state][neighbors.id+1][futur]*Strategy_Table_Previous[futur];
							}
							Q_values[nth_neighbor] = Reward_Table[state][neighbors.id+1] + discount * sum;
							nth_neighbor++;
						}
						//increase best V value
						Arrays.sort(Q_values);
						Strategy_Table[state] = Q_values[Q_values.length-1];
					}
					else {
						int nth_neighbor=0;
						double sum = 0;
						Q_values=new double[cityLocation.neighbors().size()];
						for(City neighbors : cityLocation) {
							sum = 0;
							for(int futur = 0; futur < Strategy_Table.length; futur++){

								sum = sum + Transition_Table[state][neighbors.id+1][futur]*Strategy_Table_Previous[futur];
							}
							Q_values[nth_neighbor] = Reward_Table[state][neighbors.id+1] + discount * sum;
							nth_neighbor++;
						}
						//increase best V value
						Arrays.sort(Q_values);
						Strategy_Table[state] = Q_values[Q_values.length-1];
					}
				}
			}
		}
		System.out.printf("Iterations for finding strategy table: %d",j);
		//Create policy table
		int pt[]=new int[NumCities*NumCities];
		for(City cityLocation : topology){
			for(City cityTarget : topology){
				state=cityLocation.id*NumCities+cityTarget.id;
				
				if(cityLocation!=cityTarget) {
					Q_values=new double[cityLocation.neighbors().size()+1];
					//first action Q value
					double sum = 0;
					for(int futur = 0; futur < Strategy_Table.length; futur++){
						
						sum = sum + Transition_Table[state][TAKETASKACTION][futur]*Strategy_Table_Previous[state];
					}
					Q_values[TAKETASKACTION] = Reward_Table[state][TAKETASKACTION] + discount * sum;
					
					
					//remaining actions Q values (
					int nth_neighbor=1;
					for(City neighbors : cityLocation) {
						sum = 0;
						for(int futur = 0; futur < Strategy_Table.length; futur++){
							
							sum = sum + Transition_Table[state][neighbors.id+1][futur]*Strategy_Table_Previous[state];
						}
						Q_values[nth_neighbor] = Reward_Table[state][neighbors.id+1] + discount * sum;
						nth_neighbor++;
					}
					//choose max Q
					pt[state]=argmax(Q_values);	
				}
				else {
					Q_values=new double[cityLocation.neighbors().size()];
					//first action Q value
					double sum = 0;

					
					
					//remaining actions Q values (
					int nth_neighbor=0;
					for(City neighbors : cityLocation) {
						sum = 0;
						for(int futur = 0; futur < Strategy_Table.length; futur++){
							
							sum = sum + Transition_Table[state][neighbors.id+1][futur]*Strategy_Table_Previous[state];
						}
						Q_values[nth_neighbor] = Reward_Table[state][neighbors.id+1] + discount * sum;
						nth_neighbor++;
					}
					//choose max Q
					pt[state]=argmax(Q_values)+1;	
				}
				
			}
		}
		return pt;
	}
	
	
	public boolean checkConvergence(){
		boolean converged = false;
		
		double epsilon = Math.pow(10, -5);
		double maxValue = 0;
		for(int i = 0; i < Strategy_Table.length; i++) {
			double currentVal = Math.abs(Strategy_Table[i]-Strategy_Table_Previous[i]);
			if(currentVal > maxValue) {
				maxValue = currentVal;
			}
		}
		if(maxValue <= epsilon) {
			converged = true;
		}
		return converged;
	}
	
	
	
	public int argmax(double[] array){
	
      int max_index = -1;
      double best_confidence = -Double.MAX_VALUE;

      for(int i = 0;i < array.length;i++){

          double value = array[i];

          if (value > best_confidence){

              best_confidence = value;
             max_index = i;
          }
      }

      return max_index;
	
	}
	
}
