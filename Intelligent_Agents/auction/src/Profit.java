



public class Profit {
	long winnings;
	public Profit() {
		this.winnings=0;
	}
	
	
	public void won(long bid) {
		winnings=winnings+bid;
	}
	
	public long getWinnings() {
		return winnings;
	}
	
	
}