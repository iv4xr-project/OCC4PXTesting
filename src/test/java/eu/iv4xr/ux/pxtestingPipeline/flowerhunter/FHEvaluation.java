package eu.iv4xr.ux.pxtestingPipeline.flowerhunter;


import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.opencsv.CSVReader;

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
import eu.iv4xr.ux.pxtestingPipeline.CSVlevelImport;
import eu.iv4xr.ux.pxtestingPipeline.Distance;
import eu.iv4xr.ux.pxtestingPipeline.EmotionData;
import eu.iv4xr.ux.pxtestingPipeline.LREvent;
import eu.iv4xr.ux.pxtestingPipeline.PCGEvent;
import eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization;
import eu.iv4xr.ux.pxtestingPipeline.levelsize;
import eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization.EmotionBeliefBase;
import game.LabRecruitsTestServer;
import game.Platform;
import eu.fbk.iv4xr.mbt.execution.labrecruits.LabRecruitsTestSuiteExecutor;

import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.multiAgentSupport.Message;

import static eu.iv4xr.ux.pxtestingPipeline.CSVExport.exportToCSV;
import static eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization.gotAsMuchPointsAsPossible;
import static eu.iv4xr.ux.pxtestingPipeline.PlayerOneCharacterization.questIsCompleted;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import world.BeliefState;
import java.util.*;/**
 * @author sansari
 */
 
// to run this, first you need to create the level model by running MC/SBtest_Generation. 
public class FHEvaluation {


	@Test
    public void emotionevaluate() throws IOException {

        Vec3 position;
	    String rootFolder = new File(System.getProperty("user.dir")).getParent();
        String FHFolder = rootFolder + File.separator + "Flowerhunter";         
        File folder = new File(FHFolder);
        File[] listOfFiles = folder.listFiles();
        //List<List<FHEvent>> Alllevel_events = new ArrayList<List<FHEvent>>();
        Map<String ,List<FHEvent>> levels_events = new HashMap<String,List<FHEvent>>();
        for (File file : listOfFiles) {
            if (file.isFile()) {
            	List<FHEvent> pcgevent_list=CSV_Reader(file.getName(),FHFolder);
            	levels_events.put(file.getName().replace(".csv", ""), pcgevent_list);
            }
        }

            for(Map.Entry<String,List<FHEvent>>  level_events : levels_events.entrySet())
            {                
            	// Create an emotion appraiser, and hook it to the agent:
                 EmotionAppraisalSystem eas = new EmotionAppraisalSystem("Agent1");
                 var beliefbase = new EmotionBeliefBase() ;
                 eas.attachEmotionBeliefBase(beliefbase)
                    .withUserModel(new FHCharacterization()).addGoal(questIsCompleted, 50)
                    .addInitialEmotions();   
                EmotionData emodata=new EmotionData();
                int t=2 ;
            	for(FHEvent e :level_events.getValue())
            	{
            		
                    if (e.name.equals("tick")) {
                        eas.update(new Tick(), t);
                    }
                    else {

                    		 eas.update( e , t);
                    		
                            
                        }
                   
                       emodata.recordNewrow(eas,t);
                        if(eas.newEmotions.stream().anyMatch(c->c.etype==Emotion.EmotionType.Disappointment)||eas.newEmotions.stream().anyMatch(c->c.etype==Emotion.EmotionType.Satisfaction))
                        {
                       	 break;
                        }
                        t++;
                 }
                	//save triggered emotions in the efsm model.
            exportToCSV(emodata.csvData_goalQuestIsCompleted, "OCCEval_"+ level_events.getKey()+".csv");
            //exportToCSV(emodata.csvData_goalGetMuchPoints, "data_goalGetMuchPoints_"+i+".csv");

            
            // run the python script called "mkgraph.py" for drawing graphs according to the saved .csv  
			
			  String path=new File(new
			  File(System.getProperty("user.dir")).getAbsolutePath(),"mkgraph1.py").
			  getAbsolutePath(); ProcessBuilder builder = new ProcessBuilder();
			  ProcessBuilder pb = new ProcessBuilder();
			  
			  //sending the csvfile number, width and heights of the level as parameters.
			  builder.command("python",
			  path,""+ level_events.getKey(),""+53,""+52); Process
			  p=builder.start(); BufferedReader bfr = new BufferedReader(new
			  InputStreamReader(p.getInputStream()));
			  
			  System.out.println(".........start   visualization process........."); String
			  line = ""; while ((line = bfr.readLine()) != null){
			  System.out.println("Python Output: " + line); }
			  
        }


  }


	private List<FHEvent>  CSV_Reader(String Filename, String Path) throws IOException{
		
			List<FHEvent> eventlist=new ArrayList();
			String filepath= Path+ File.separator +Filename;
		    CSVReader reader = new CSVReader(new FileReader(filepath));
		      String [] nextLine;
		      nextLine = reader.readNext();
		      while ((nextLine = reader.readNext()) != null ) {
		              FHEvent e=new FHEvent(nextLine[1]);
		              e.time=Integer.parseInt(nextLine[0]);
		              e.healthpoint=Float.parseFloat(nextLine[2]);
		              e.money=Float.parseFloat(nextLine[3]);
		              e.enemies_killed=Integer.parseInt(nextLine[4]);
		              e.distance_to_objective=Float.parseFloat(nextLine[5]);
		              e.damage_done=Integer.parseInt(nextLine[6]);
			    	  eventlist.add(e);
			    	  
		      }
			return eventlist;
		}
	    

		
	      
}
