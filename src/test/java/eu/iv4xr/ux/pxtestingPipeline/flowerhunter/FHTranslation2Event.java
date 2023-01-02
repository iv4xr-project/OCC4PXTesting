package eu.iv4xr.ux.pxtestingPipeline.flowerhunter;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import agents.EventsProducer;
import agents.LabRecruitsTestAgent;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.fbk.iv4xr.mbt.MBTProperties;
import eu.fbk.iv4xr.mbt.MBTProperties.LR_random_mode;
import eu.fbk.iv4xr.mbt.efsm.EFSM;
import eu.fbk.iv4xr.mbt.efsm.EFSMFactory;
import eu.fbk.iv4xr.mbt.efsm.EFSMOperation;
import eu.fbk.iv4xr.mbt.efsm.EFSMState;
import eu.fbk.iv4xr.mbt.efsm.EFSMTransition;
import eu.fbk.iv4xr.mbt.efsm.exp.Assign;
import eu.fbk.iv4xr.mbt.efsm.exp.Var;
import eu.fbk.iv4xr.mbt.efsm.exp.integer.IntSum;
import eu.fbk.iv4xr.mbt.efsm.labRecruits.LabRecruitsRandomEFSM;
import eu.fbk.iv4xr.mbt.efsm.labRecruits.LabRecruitsRandomEFSM.StateType;
import eu.fbk.iv4xr.mbt.efsm.exp.Const;
import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.testcase.MBTChromosome;
import eu.fbk.iv4xr.mbt.testsuite.SuiteChromosome;
import eu.fbk.iv4xr.mbt.testcase.Path;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import eu.iv4xr.framework.extensions.occ.Emotion;
import eu.iv4xr.framework.extensions.occ.EmotionAppraisalSystem;
import eu.iv4xr.framework.extensions.occ.Emotion.EmotionType;
import eu.iv4xr.framework.extensions.occ.Event.Tick;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.ux.pxtestingPipeline.CSVExport;
import eu.iv4xr.ux.pxtestingPipeline.CSVlevelImport;
import eu.iv4xr.ux.pxtestingPipeline.Distance;
import eu.iv4xr.ux.pxtestingPipeline.LREvent;
import eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization;
import eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization.EmotionBeliefBase;
import game.LabRecruitsTestServer;
import game.Platform;
import eu.fbk.iv4xr.mbt.execution.labrecruits.LabRecruitsTestSuiteExecutor;
import  eu.iv4xr.ux.pxtestingPipeline.flowerhunter.FHCharacterization;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.multiAgentSupport.Message;

import static eu.iv4xr.ux.pxtestingPipeline.CSVExport.exportToCSV;
import static eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization.gotAsMuchPointsAsPossible;
import static eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization.questIsCompleted;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import world.BeliefState;
import java.util.*;
/**
 * @author sansari
 */
 
public class FHTranslation2Event {

	@Test
    public void emotionevaluate() throws IOException {

        Vec3 position;
	    String rootFolder = new File(System.getProperty("user.dir")).getParent();
        String EventFolder = rootFolder + File.separator + "PAD_emotion_game-main"+ File.separator+ "First_Study"+ File.separator+ "Arousal";         
        File folder = new File(EventFolder);
        File[] listOfFiles = folder.listFiles();
        Map<String ,List<FHState>> level_perceptions = new HashMap<String,List<FHState>>();
        for (File file : listOfFiles) {
            if (file.isFile() & file.getName().startsWith("Traces_Perceptor")) {
            	List<FHState> FHState_list=File_Reader(EventFolder+File.separator+ file.getName(),EventFolder);
            	level_perceptions.put(file.getName().replaceFirst("Traces_Perceptor_", "").replaceFirst(".txt",""), FHState_list);
            }
        }
        Translate2Events(level_perceptions);
        
         }


	private List<FHState> File_Reader(String Fileaddress, String Path) throws IOException{
		
			List<FHState> perceptionlist=new ArrayList();
			 BufferedReader reader
	            = new BufferedReader(new FileReader(Fileaddress));
		      String nextLine;
          	FHState previous = null ;
          	int r=0;
		      while ((nextLine = reader.readLine()) != null ) {
		    	  		if (r==0) {
          					r++ ;
          					continue ;            			
		    	  		}
		              String [] splited = nextLine.toString().split("_");

		             Arrays.asList(splited).stream().filter(c->c.equals("inf")).forEach(c->splited[Arrays.asList(splited).indexOf(c)]=c.replace("inf", String.valueOf(Integer.MAX_VALUE)));

		              
		              FHState perception=new FHState(r,splited[0], splited[1], splited[2], splited[3],splited[4], splited[5], splited[6],splited[7],splited[8],splited[9], splited[10],
		            		  splited[11],splited[12],splited[13],splited[14] ,splited[15], splited[16],splited[17], splited[18],splited[19]);
		              perception.previous=previous;
		              if(r>1)
		              {
			              perceptionlist.add(perception);
		            	  
		              }
		              previous= perception;
		              r++;
		      }
			return perceptionlist;
		}
	    

	private void Translate2Events(Map<String, List<FHState>> level_perceptions) throws IOException {

		for(Map.Entry<String, List<FHState>> perceptions : level_perceptions.entrySet())
		{	
			List<FHEvent> event_list=new ArrayList<FHEvent>();
			boolean objectiveInsight=false;
			for(FHState p :perceptions.getValue())
			{
				boolean flag=false;
				FHEvent e=null;
				if(Float.parseFloat(p.healthpoint)> Float.parseFloat(p.previous.healthpoint))
				{ 
					e=new FHEvent("gethealed"); 
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);	
					flag=true;
				}	
				if(Float.parseFloat(p.healthpoint)< Float.parseFloat(p.previous.healthpoint))
				{	
					e=new FHEvent("getdamaged");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				}
				if(Float.parseFloat(p.money)> Float.parseFloat(p.previous.money))
				{	
					e=new FHEvent("gainmoney");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				}	
				if(Float.parseFloat(p.enemies_killed)> Float.parseFloat(p.previous.enemies_killed))
				{	
					e=new FHEvent("enemykilled");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				}
				if(Float.parseFloat(p.damage_done)> Float.parseFloat(p.previous.damage_done))
				{	
					e=new FHEvent("hurtenemies");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				}
				if(Float.parseFloat(p.number_enemies_view)> Float.parseFloat(p.previous.number_enemies_view))
				{	
					e=new FHEvent("moreenemiesInsight");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				}

				//if(Float.parseFloat(p.sum_value_slash_distance_enemies)> Float.parseFloat(p.previous.sum_value_slash_distance_enemies))
				//	player.getdamage(null);
				if(!objectiveInsight&&Float.parseFloat(p.distance_to_objective)< Float.parseFloat(p.previous.distance_to_objective))
				{	
					objectiveInsight=true;
					e=new FHEvent("closertoobjective");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				};
				if(Float.parseFloat(p.distance_to_objective)> Float.parseFloat(p.previous.distance_to_objective))
				{	
					e=new FHEvent("fartherfromobjective");
					e.time=p.time;
					e.healthpoint=Float.parseFloat(p.healthpoint);
					e.money=Float.parseFloat(p.money);
					e.enemies_killed=Integer.parseInt(p.enemies_killed);
					e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
					e.damage_done=Integer.parseInt(p.damage_done);
					event_list.add(e);
					flag=true;
				
				}
				if(Float.parseFloat(p.distance_closest_enemy)< Float.parseFloat(p.previous.distance_closest_enemy))
				{
						  float diff = Float.parseFloat(p.distance_closest_enemy)- Float.parseFloat(p.previous.distance_closest_enemy);
						  if(diff< 10)
							{	
							  e=new FHEvent("soclosetoenemy");
								e.time=p.time;
								e.healthpoint=Float.parseFloat(p.healthpoint);
								e.money=Float.parseFloat(p.money);
								e.enemies_killed=Integer.parseInt(p.enemies_killed);
								e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
								e.damage_done=Integer.parseInt(p.damage_done);
								event_list.add(e);
								flag=true;
							
							}
						  else {	
							  e=new FHEvent("closertoenemy");
									e.time=p.time;
									e.healthpoint=Float.parseFloat(p.healthpoint);
									e.money=Float.parseFloat(p.money);
									e.enemies_killed=Integer.parseInt(p.enemies_killed);
									e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
									e.damage_done=Integer.parseInt(p.damage_done);
									event_list.add(e);
									flag=true;
								
								}
						  }
				if(!flag) {	
				e=new FHEvent("tick");
				e.time=p.time;
				e.healthpoint=Float.parseFloat(p.healthpoint);
				e.money=Float.parseFloat(p.money);
				e.enemies_killed=Integer.parseInt(p.enemies_killed);
				e.distance_to_objective=Float.parseFloat(p.distance_to_objective);
				e.damage_done=Integer.parseInt(p.damage_done);
				event_list.add(e);}
					
				}
			if(Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).distance_to_objective)<=40)
			{	FHEvent e=new FHEvent("goalachieved");
				e.time=perceptions.getValue().get(perceptions.getValue().size()-1).time;
				if(event_list.stream().anyMatch(c->c.time==e.time))
				{event_list.removeAll(event_list.stream().filter(c->c.time==e.time).collect(Collectors.toList()));} 
				
				e.healthpoint=Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).healthpoint);
				e.money=Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).money);
				e.enemies_killed=Integer.parseInt(perceptions.getValue().get(perceptions.getValue().size()-1).enemies_killed);
				e.distance_to_objective=Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).distance_to_objective);
				e.damage_done=Integer.parseInt(perceptions.getValue().get(perceptions.getValue().size()-1).damage_done);
				event_list.add(e);
			}	
			else
			{	FHEvent e=new FHEvent("goalfailed");
				e.time=perceptions.getValue().get(perceptions.getValue().size()-1).time;
				if(event_list.stream().anyMatch(c->c.time==e.time))
				{event_list.removeAll(event_list.stream().filter(c->c.time==e.time).collect(Collectors.toList()));}
				e.healthpoint=Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).healthpoint);
				e.money=Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).money);
				e.enemies_killed=Integer.parseInt(perceptions.getValue().get(perceptions.getValue().size()-1).enemies_killed);
				e.distance_to_objective=Float.parseFloat(perceptions.getValue().get(perceptions.getValue().size()-1).distance_to_objective);
				e.damage_done=Integer.parseInt(perceptions.getValue().get(perceptions.getValue().size()-1).damage_done);
				event_list.add(e);
			}	
				Save_Events(perceptions.getKey(),event_list);
				
			}
	}


	private void Save_Events(String key, List<FHEvent> event_list) throws IOException {
	        List<String[]> stringlist=new ArrayList();
	        String[] header = { "time","Event Name","healthpoint","money","enemies_killed","distance_to_objective","damage_done"};
	        stringlist.add(header);
	       for(FHEvent e : event_list)
	       {
	    	   
		        String[] csvRow={ "" + e.time, ""+e.name, "" + e.healthpoint,"" + e.money,"" + e.enemies_killed,""+ e.distance_to_objective,""+ e.damage_done};
		        stringlist.add(csvRow);
	       }
	       
	        CSVExport.exportToCSV(stringlist, key+".csv");
	    
	
		
	}
		
}

	      
