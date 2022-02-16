package eu.iv4xr.ux.pxtesting;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import eu.iv4xr.ux.pxtesting.PlayerOneCharacterization.EmotionBeliefBase;
import game.LabRecruitsTestServer;
import game.Platform;
import eu.fbk.iv4xr.mbt.execution.labrecruits.LabRecruitsTestSuiteExecutor;

import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.multiAgentSupport.Message;

import static eu.iv4xr.ux.pxtesting.CSVExport.exportToCSV;
import static eu.iv4xr.ux.pxtesting.PlayerOneCharacterization.gotAsMuchPointsAsPossible;
import static eu.iv4xr.ux.pxtesting.PlayerOneCharacterization.questIsCompleted;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import world.BeliefState;

/**
 *  load all test cases from the disk as a test suite - transform a test suite into a goal structure.

 * Using this along with the appraisal system we could record emotional states of different numbers of generated test cases.  
 * 
 * @author sansari
 */

// to run this, first you need to create the level model by running MC/SBtest_Generation. 
public class Model_based_pxtesting {

	protected EFSM efsm_copy;
    // context variables
	public HashMap<Long,HashSet<Emotion>> map=new HashMap<Long,HashSet<Emotion>>();
	

	@Test
    public void runGeneratedTests() throws IOException {

	    String rootFolder = new File(System.getProperty("user.dir")).getParent();
        String testFolder = rootFolder + File.separator + "Combinedtest";
        String modelFolder = testFolder + File.separator + "Model";
        String UdpateFolder = testFolder + File.separator + "UpdatedModel";
        String labRecruitesExeRootDir = rootFolder + File.separator + "iv4xrDemo";
        
		//get goal state			
        model_test_IOoperations set=new model_test_IOoperations();
        efsm_copy=set.loadModel(modelFolder);
        EFSMState goalstate = null;
		for(var state : efsm_copy.getStates()) {
			EFSMState state_ = (EFSMState) state ;
			if(LabRecruitsRandomEFSM.getStateType(state_) == StateType.GoalFlag) {
				goalstate=state_ ;
			}
		} ;
		
        // load tests from file
        SuiteChromosome loadedSolution = set.parseTests(testFolder);
        
		// choose a distance metric from Jaccard, Jaro- Winkler or Leveneshtien.
        Distance dis=new Distance("jaccard");
		double totaldistance= dis.distance(loadedSolution);
		System.out.println("MC-testsuite size is: "+ loadedSolution.size());
		System.out.println("total Distance: "+totaldistance);
		
        // open the server
        LabRecruitsTestServer testServer = new LabRecruitsTestServer(false,
                Platform.PathToLabRecruitsExecutable(labRecruitesExeRootDir));
        // set the configuration of the server 
        // level file name is hard coded in writeModel but can be changed
        LabRecruitsConfig lrCfg = new LabRecruitsConfig("LabRecruits_level", modelFolder);
        levelsize lrsize= new levelsize(CSVlevelImport.ImportFromCSV("LabRecruits_level", modelFolder));    
        
        // convert test cases in loadedSolution to goal structure: use the test case executor to convert
        LabRecruitsTestSuiteExecutor lrExecutor = new LabRecruitsTestSuiteExecutor(rootFolder, testFolder, modelFolder, null);
        
        // iterate over test case and convert to goals
        String results = "" ;
        for (int i = 1; i <= loadedSolution.size(); i++) {
            AbstractTestSequence testcase = (AbstractTestSequence) loadedSolution.getTestChromosome(i-1).getTestcase();
            // the system returns a list of goals that can be sequentialized with SEQ
            LabRecruitsEnvironment labRecruitsEnvironment = new LabRecruitsEnvironment(lrCfg);
            var dataCollector = new TestDataCollector();
            // run the testing task:
            LabRecruitsTestAgent testAgent = new LabRecruitsTestAgent("Agent1") ; 
            testAgent.attachState(new BeliefState())
                     .setTestDataCollector(dataCollector)
                     .attachEnvironment(labRecruitsEnvironment)
                     .attachSyntheticEventsProducer(new EventsProducer());
            testAgent.getSyntheticEventsProducer().idOfLevelEnd="gf0";
            List<GoalStructure> goals = lrExecutor.convertTestCaseToGoalStructure(testAgent, testcase);
            // Add instrumentation here:
            goals = set.instrumentTestCase(testAgent,testcase,goals) ;
            GoalStructure g =SEQ(goals.toArray(new GoalStructure[goals.size()]));
            testAgent.setGoal(g) ;
            System.out.println(">> Testing task: " + i + ", #=" + goals.size()) ;
            System.out.println(">> " + testcase ) ;    
            
            // add an event-producer to the test agent so that it produce events for
            // emotion appraisals:
            //EventsProducer eventsProducer = new EventsProducer().attachTestAgent(testAgent);
            	
            // Create an emotion appraiser, and hook it to the agent:
            EmotionAppraisalSystem eas = new EmotionAppraisalSystem(testAgent.getId());
            eas.attachEmotionBeliefBase(new EmotionBeliefBase().attachFunctionalState(testAgent.getState()))
                    .withUserModel(new PlayerOneCharacterization()).addGoal(questIsCompleted, 50)
                    .addGoal(gotAsMuchPointsAsPossible, 50).addInitialEmotions();       
            labRecruitsEnvironment.startSimulation();
            // goal not achieved yet
            assertFalse(testAgent.success());
            
            //collect data
            EmotionData emodata=new EmotionData();
            int t=0 ;
            while (g.getStatus().inProgress()) {
            	 Vec3 position = testAgent.getState().worldmodel.position;
                 System.out.println("*** " + t + ", " + testAgent.getState().id + " @" + position);
                 
                 
                 if (testAgent.getSyntheticEventsProducer().currentEvents.isEmpty()) {
                     eas.update(new Tick(), t);
                 }
                 else {
                     for (Message m : testAgent.getSyntheticEventsProducer().currentEvents) {
                         eas.update( new LREvent(m.getMsgName()), t);
                         if(!eas.newEmotions.isEmpty())
                         {
                        	 
                         map.put(testAgent.getState().worldmodel.timestamp, eas.newEmotions);
                         }
                         
                     }
                 }

                 if (position != null) {
                    
                     Float score = (float) testAgent.getState().worldmodel().score;
                     Float healthlost = (float) testAgent.getState().worldmodel().healthLost;
                     Float health = (float) testAgent.getState().worldmodel().health;
                     System.out.println("*** score=" + score);
                     emodata.recordNewrow(score,healthlost,health,eas,position,t);
                     if(eas.newEmotions.stream().anyMatch(c->c.etype==Emotion.EmotionType.Disappointment)||eas.newEmotions.stream().anyMatch(c->c.etype==Emotion.EmotionType.Satisfaction))
                     {
                    	 break;
                     }
                 }
                if (t>1000) {
                    break ;
                }
                try {
                    Thread.sleep(200);
                    testAgent.update();
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                t++ ;
             }
            
            //save triggered emotions in the efsm model.
            exportToCSV(emodata.csvData_goalQuestIsCompleted, "data_goalQuestCompleted_"+i+".csv");
            exportToCSV(emodata.csvData_goalGetMuchPoints, "data_goalGetMuchPoints_"+i+".csv");

            
            // run the python script called "mkgraph.py" for drawing graphs according to the saved .csv  
            String path=new File(new File(System.getProperty("user.dir")).getAbsolutePath(),"mkgraph.py").getAbsolutePath();
			ProcessBuilder builder = new ProcessBuilder(); 
			ProcessBuilder pb = new ProcessBuilder();
			
			//sending the csvfile number, width and heights of the level as parameters.
			builder.command("python", path,""+i,""+lrsize.getheight(),""+lrsize.getwidth());
			Process p=builder.start();
			BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
	
			System.out.println(".........start   visualization process.........");  
		    String line = "";     
		    while ((line = bfr.readLine()) != null){
			      System.out.println("Python Output: " + line);
		    }
            
            g.printGoalStructureStatus();
            labRecruitsEnvironment.close() ;           
            results += ">> tc-" + i + ", Duration: "+ t + ", goalstatus: " + g.getStatus() 
               + ", #fail: " + testAgent.getTestDataCollector().getNumberOfFailVerdictsSeen()
               + ", #success: " + testAgent.getTestDataCollector().getNumberOfPassVerdictsSeen() + "\n" ;
          
        }
        //set.writeModel(UdpateFolder); //fix its problem with efsm get instance
        System.out.println("" + results) ;
        testServer.close();
        
	}

}
