package eu.iv4xr.ux.pxtestingPipeline.flowerhunter;

import java.util.List;

import eu.iv4xr.framework.extensions.occ.Event;
import eu.iv4xr.framework.extensions.occ.Emotion.EmotionType;

public class FHState {
    
    //public FHState(String name) { super(name) ; }
	public int time;
    public String distance_closest_enemy;
    public String distance_closest_food_item;
    public String distance_closest_money;
    public String number_enemies_view;
    public String number_food_item_view;
    public String number_money_view;
    public String sum_enemy_values;
    public String sum_food_item_values;

    public String sum_money_values;  
    public String sum_value_slash_distance_enemies;
    public String sum_value_slash_distance_food_item;
    public String value_slash_distance_money;
    public String seconds_since_enemy;
    public String seconds_since_food_item;
    public String seconds_since_money;
    public String distance_to_objective;
    public String healthpoint;
    public String money;
    public String enemies_killed;
    public String damage_done;  
    
    public FHState(int time, String distance_closest_enemy, String distance_closest_food_item, String distance_closest_money,
    		String number_enemies_view, String number_food_item_view,	String sum_money_values,
    		String sum_value_slash_distance_enemies, String sum_value_slash_distance_food_item,
    	    String sum_enemy_values, String sum_food_item_values,String number_money_view, 
    	    String value_slash_distance_money, String seconds_since_enemy, String seconds_since_food_item,
    	    String seconds_since_money,String distance_to_objective,String healthpoint,
    	    String money,String enemies_killed,String damage_done )
    {
    	this.time=time;
    	this.distance_closest_enemy=distance_closest_enemy;
        this.distance_closest_food_item=distance_closest_food_item;
        this. distance_closest_money=distance_closest_money;
        this. number_enemies_view=number_enemies_view;
        this. number_food_item_view=number_food_item_view;
        this. number_money_view=number_money_view;
        this. sum_enemy_values=sum_enemy_values;
        this. sum_food_item_values=sum_food_item_values;

        this. sum_money_values=sum_money_values;  
        this. sum_value_slash_distance_enemies=sum_value_slash_distance_enemies;
        this. sum_value_slash_distance_food_item=sum_value_slash_distance_food_item;
        this. value_slash_distance_money=value_slash_distance_money;
        this. seconds_since_enemy=seconds_since_enemy;
        this. seconds_since_food_item=seconds_since_food_item;
        this. seconds_since_money=seconds_since_money;
        this. distance_to_objective=distance_to_objective;
        this. healthpoint=healthpoint;
        this. money=money;
        this. enemies_killed=enemies_killed;
        this. damage_done=damage_done;  
        
    }
	public FHState previous ;

    
}


