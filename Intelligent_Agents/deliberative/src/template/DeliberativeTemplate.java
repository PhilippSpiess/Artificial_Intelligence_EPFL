package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;
import logist.plan.Action.Move;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {
	private static final int CARRY = -1;
	private static final int DELIVERED = -2;
	enum Algorithm { BFS, ASTAR, UBC }
	private static final int HEURISTIC_REGULATOR = 10; 
	//to have the heuristic always smaller than 40 (min possible distance)
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	TaskSet carriedTasks;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		if(vehicle.color().getGreen()==255) {
			int teststop=0;
		}
		
		if(carriedTasks!=null) {
			tasks=TaskSet.union(tasks, carriedTasks);
		}
		
		
		long startTime = System.nanoTime();
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = AstarPlan(vehicle, tasks);
			break;
		case UBC:
			plan = UbcPlan(vehicle, tasks);
			break;
		case BFS:
			plan = BFSplan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		long endTime = System.nanoTime();
		long timeElapsed = endTime - startTime;

		System.out.println("Execution time in nanoseconds  : " + timeElapsed);

		System.out.println("Execution time in milliseconds : " + 
								timeElapsed / 1000000);
		
		if(vehicle.color().getGreen()==255) {
			int teststop=0;
		}
		return plan;
	}
	
	class State implements Comparable<State>{
		private City vehicleLocation;
		private int[] taskLocations;
		private ArrayList<Action> actionList;
		private double cost;
		private int carriedWeight;
		private double heuristic;
		
		public double getHeuristic() {
			return heuristic;
		}
		
		public void setHeuristic(double newH) {
			this.heuristic=newH;
		}
		
		
		public double getCost() {
			return cost;
		}
		
		public void setCost(double newCost) {
			this.cost=newCost;
		}
		
		@SuppressWarnings("unchecked")
		State(State prev_state, City moveToCity){//move constructor
			this.vehicleLocation=moveToCity;
			this.taskLocations=new int[prev_state.taskLocations.length];
			for(int i=0;i<prev_state.taskLocations.length;i++) {
				this.taskLocations[i]=prev_state.taskLocations[i];
			}
			this.actionList=(ArrayList<Action>) prev_state.actionList.clone();
			this.actionList.add(new Move(moveToCity));
			this.cost=prev_state.getCost()+prev_state.vehicleLocation.distanceTo(moveToCity);
			this.heuristic=0;
		}
		State(State prev_state, Task pdTask){ //task constructor
			this.vehicleLocation=prev_state.vehicleLocation;
			this.taskLocations=new int[prev_state.taskLocations.length];
			for(int i=0;i<prev_state.taskLocations.length;i++) {
				this.taskLocations[i]=prev_state.taskLocations[i];
			}
			this.actionList=(ArrayList<Action>) prev_state.actionList.clone();
			
			if(this.taskLocations[pdTask.id]==CARRY) {//deliver task
				this.taskLocations[pdTask.id]=DELIVERED;
				this.actionList.add(new Delivery(pdTask));
				this.carriedWeight=this.carriedWeight-pdTask.weight;
			}
			else { //Pickup task
				this.taskLocations[pdTask.id]=CARRY;
				this.actionList.add(new Pickup(pdTask));
				this.carriedWeight=this.carriedWeight+pdTask.weight;
			}
			this.cost=prev_state.getCost();
			this.heuristic=0;
		}
		State(City vL,TaskSet tL,int weight,TaskSet carriedTasks){//initial constructor
			this.vehicleLocation=vL;
			int tableSize=0;
			for (Task task : tL) {
				if (task.id>tableSize) {
					tableSize=task.id;
				}
			}
			this.taskLocations= new int[tableSize+1];
			for(int i=0;i<tableSize+1;i++) {
				this.taskLocations[i]=DELIVERED;
			}
			for (Task task : tL) {
				this.taskLocations[task.id]=task.pickupCity.id;
				if(carriedTasks!=null) {
					if(carriedTasks.contains(task)) {
						this.taskLocations[task.id]=CARRY;
					}
				}
			}
			this.actionList=new ArrayList<Action>();
			this.cost=0;
			this.carriedWeight=weight;
			this.heuristic=0;
		}
		//END CONSTRUCTORS
		public boolean isGoal() {
			boolean yes=true;
			for(int testLoc : this.taskLocations) {
				if(testLoc!=DELIVERED) {
					yes=false;
				}
			}
			return yes;
		}
		public boolean compareStates(State otherstate) {
			if(this.vehicleLocation.id!=otherstate.vehicleLocation.id) {
				return false;
			}
			for(int i=0; i<this.taskLocations.length;i++) {
				if(this.taskLocations[i]!=otherstate.taskLocations[i]) {
					return false;
				}
			}
			if(this.cost<otherstate.getCost()) {
				return false;
			}
			return true;
		}
		
		@Override
	    public int compareTo(State comparestate) {
	        if (comparestate.getCost()+comparestate.getHeuristic() < this.getCost()+this.getHeuristic()) return 1;
	        if (comparestate.getCost()+comparestate.getHeuristic() > this.getCost()+this.getHeuristic()) return -1;
	        return 0;

	    }
	}

	private Plan UbcPlan(Vehicle vehicle, TaskSet tasks) { //smallest path to any futur city

		City current = vehicle.getCurrentCity();
		State startstate=new State(current,tasks,0,carriedTasks);
		
		ArrayList<State> Q = new ArrayList<State>(); // all last layer states
		ArrayList<State> C = new ArrayList<State>(); // all finite states (here only one)
		//ArrayList<State> N = new ArrayList<State>();
		State n=startstate;
		Q.add(startstate);
		while(!Q.isEmpty()) {
			n= Q.remove(0); //try the first element of the list each time
			if(n.isGoal()) {
				break;
			}
			if(!listContains(C,n)) {
				/*
				 * Add successors to Q
				 */
				for(City neighborsCity : n.vehicleLocation.neighbors()) { //new locations for agent
					State newstate=new State(n,neighborsCity);//crash
					Q.add(newstate); 
				}
				
				for(Task taskno:tasks) { 
					//deliver carried tasks
					if(n.taskLocations[taskno.id]==CARRY && taskno.deliveryCity.equals(n.vehicleLocation)) {
						//second condition is questionable if you want to be able to drop tasks and come back later.
						State newstate=new State(n,taskno);
						Q.add(newstate);
					}
					//pickup task at location
					if(n.taskLocations[taskno.id]==n.vehicleLocation.id && n.carriedWeight+taskno.weight<vehicle.capacity()) {
						State newstate=new State(n,taskno);
						Q.add(newstate);
					}
				}
				Collections.sort(Q); 
				C.add(n);
				
				/*
				 * End
				 */
			}
		}
		Plan finished= new Plan(current);
		for(Action plannedAct : n.actionList) {
			finished.append(plannedAct);
		}
		System.out.println(finished.totalDistance());
		return finished;
	}
	
	private Plan AstarPlan(Vehicle vehicle, TaskSet tasks) {

		City current = vehicle.getCurrentCity();
		State startstate=new State(current,tasks,0,carriedTasks);
		
		ArrayList<State> Q = new ArrayList<State>(); //all last layer states
		ArrayList<State> C = new ArrayList<State>(); //last present state (here only one)
		//ArrayList<State> N = new ArrayList<State>();
		State n=startstate;
		Q.add(startstate);
		
		while(!Q.isEmpty()) {
			n= Q.remove(0);
			if(n.isGoal()) {
				break;
			}
			if(!listContains(C,n)) {
				/*
				 * Add successors to Q
				 */
				for(City neighborsCity : n.vehicleLocation.neighbors()) { //new locations for agent
					State newstate=new State(n,neighborsCity);//crash
					newstate.setHeuristic(computeHeuristic(newstate,tasks));
					Q.add(newstate);
				}
				for(Task taskno:tasks) { 
					//deliver carried tasks
					if(n.taskLocations[taskno.id]==CARRY && taskno.deliveryCity.equals(n.vehicleLocation)) {
						//second condition is questionable if you want to be able to drop tasks and come back later.
						State newstate=new State(n,taskno);
						newstate.setHeuristic(computeHeuristic(newstate,tasks));
						Q.add(newstate);
					}
					//pickup task at location
					if(n.taskLocations[taskno.id]==n.vehicleLocation.id && n.carriedWeight+taskno.weight<vehicle.capacity()) {
						State newstate=new State(n,taskno);
						newstate.setHeuristic(computeHeuristic(newstate,tasks));
						Q.add(newstate);
					}
				}
				
				Collections.sort(Q);
				C.add(n);
				
			}
		}
		Plan finished= new Plan(current);
		for(Action plannedAct : n.actionList) { //The plan is a set of actions following one another
			finished.append(plannedAct);
		}
		System.out.println(finished.totalDistance());
		return finished;
	}
	
	private Plan BFSplan(Vehicle vehicle, TaskSet tasks) {
		//End of state class
				City current = vehicle.getCurrentCity();
				State startstate=new State(current,tasks,0,carriedTasks);
				
				ArrayList<State> Q = new ArrayList<State>();
				ArrayList<State> C = new ArrayList<State>();
				ArrayList<State> N = new ArrayList<State>();
				Q.add(startstate);
				while(!Q.isEmpty()) {
					State n= Q.remove(0);	
					if(!listContains(C,n)) {
						/*
						 * Add successors to Q
						 */
						for(City neighborsCity : n.vehicleLocation.neighbors()) { //new locations for agent
							State newstate=new State(n,neighborsCity);//crash
							if(newstate.isGoal()) {
								N.add(newstate);
							}		
							else Q.add(newstate);
						}
						for(Task taskno:tasks) { 
							//deliver carried tasks
							if(n.taskLocations[taskno.id]==CARRY && taskno.deliveryCity.equals(n.vehicleLocation)) {
								//second condition is questionable if you want to be able to drop tasks and come back later.
								State newstate=new State(n,taskno);
								if(newstate.isGoal()) {
									N.add(newstate);
								}		
								else Q.add(newstate);
							}
							//pickup task at location
							if(n.taskLocations[taskno.id]==n.vehicleLocation.id && n.carriedWeight+taskno.weight<vehicle.capacity()) {
								State newstate=new State(n,taskno);
								if(newstate.isGoal()) {
									N.add(newstate);
								}		
								else Q.add(newstate);
							}
						}
						C.add(n);
						
						/*
						 * End
						 */
					}
				}
				Plan finished= new Plan(current);
				for(Action plannedAct : N.remove(0).actionList) {
					finished.append(plannedAct);
				}
				while(!N.isEmpty())
				{
					Plan testPlan= new Plan(current);
					for(Action plannedAct : N.remove(0).actionList) {
						testPlan.append(plannedAct);
					}
					if(testPlan.totalDistance()<finished.totalDistance()) {
						finished=testPlan;
					}
				}
				System.out.println(finished.totalDistance());
				return finished;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) { 
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed
			this.carriedTasks = carriedTasks;
		}
		else {
			this.carriedTasks=null;
		}
		
	}
	
	public boolean listContains(ArrayList<State> listofStates,State testState) {
		for(State testState2 : listofStates) {
			if(testState.compareStates(testState2)) {
				return true;
			}	
		}
		return false;
	}
	
	
	public double computeHeuristic(State newstate, TaskSet tasks) {
		double sumdist= 0;
		double dist=0;
		int N=0;
		for(Task taskno:tasks) { 
			N=N+1;
			if(newstate.taskLocations[taskno.id]==CARRY) {
				double distx = newstate.vehicleLocation.xPos - taskno.deliveryCity.xPos;
				double disty = newstate.vehicleLocation.yPos - taskno.deliveryCity.yPos;
				dist=Math.sqrt(Math.pow(distx, 2)+Math.pow(disty, 2));
			}
			else if(newstate.taskLocations[taskno.id]!=DELIVERED) {
				double distx = newstate.vehicleLocation.xPos - taskno.pickupCity.xPos;
				double disty = newstate.vehicleLocation.yPos - taskno.pickupCity.yPos;
				dist=Math.sqrt(Math.pow(distx, 2)+Math.pow(disty, 2));
			}
			sumdist=sumdist+dist;
		}
		return sumdist/N;
	}
}
