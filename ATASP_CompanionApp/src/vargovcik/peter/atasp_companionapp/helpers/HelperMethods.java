package vargovcik.peter.atasp_companionapp.helpers;

public enum HelperMethods {
	instance;
	
	private HelperMethods()	{
		
	}
	
	public long map(long x, long in_min, long in_max, long out_min, long out_max)
	{
	  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
	
	public int map(int x, int in_min, int in_max, int out_min, int out_max)
	{
		long res = ((long)x - (long)in_min) * ((long)out_max - (long)out_min) / ((long)in_max -(long) in_min) + (long)out_min;
		return (int) res;
	}

}
