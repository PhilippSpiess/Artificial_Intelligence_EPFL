import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

interface CostsInt{
	
	void addWeight(int weight,City start,City end); //Need to guarantee to SLS that this will always be there to call, even if it does nothing
	public void preCalc(Topology topology);
	public void wonBid();
	public double mCost(Task task,long timeAvailable);
	public double mfProfit(int nbTests,int nbTasks,long timeAvailable,Task task);
	
}
