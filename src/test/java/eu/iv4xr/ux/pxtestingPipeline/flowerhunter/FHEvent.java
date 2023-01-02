package eu.iv4xr.ux.pxtestingPipeline.flowerhunter;

import eu.iv4xr.framework.extensions.occ.Event;

public class FHEvent extends Event {
    
    public FHEvent(String name) { super(name) ; }
    public int time;
    public float distance_to_objective;
    public float healthpoint;
    public float money;
    public int enemies_killed;
    public int damage_done;
}
