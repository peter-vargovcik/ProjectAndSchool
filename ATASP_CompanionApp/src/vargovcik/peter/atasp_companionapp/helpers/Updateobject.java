package vargovcik.peter.atasp_companionapp.helpers;

public class Updateobject {
	public static int CONNECTING = 1;
	public static int CONNECTING_SUCESS = 2;
	public static int CONNECTING_FALED = 3;
	public static int CONNECTION_TERMINATED = 10;
	public static int STARTING_LIVE_FEED = 4;
	public static int STARTING_LIVE_FEED_SUCESS = 5;
	public static int STARTING_LIVE_FEED_FAILED = 6;
	public static int REQUESTING_REMOTE_CONTROL = 7;
	public static int REQUESTING_REMOTE_CONTROL_SUCESS = 8;
	public static int REQUESTING_REMOTE_CONTROL_FALED = 9;
	
	private int state;

	
	public Updateobject() {
	}

	public Updateobject(int state) {
		super();
		this.state = state;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
	
	

}
