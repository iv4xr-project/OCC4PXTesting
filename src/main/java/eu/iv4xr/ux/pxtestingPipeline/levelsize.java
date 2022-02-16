package eu.iv4xr.ux.pxtestingPipeline;

public  class levelsize {
    private int height;
    private int width;
    
	public levelsize(int[] s)
	{
		this.height=s[0];
		this.width=s[1];
	}

    public int getheight() {
        return this.height;
    }

    public void setheight(int num) {
        this.height = num;
    }
    
    public int getwidth() {
        return this.width;
    }

    public void setwidth(int num) {
        this.width = num;
    }

}
