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
 * @author sansari
 * 
 */

// to run this, first you need to create the level model by running MBT_model_test_Generation. 
public class Model_based_pxtesting {

	protected EFSM efsm_copy;
    // context variables
	public Var<Integer> hope = new Var<Integer>("Hope", 0);
	public Var<Integer> fear = new Var<Integer>("Fear", 0);
	public Var<Integer> joy = new Var<Integer>("Joy", 0);
	public Var<Integer> distress = new Var<Integer>("Distress", 0);
	public Var<Integer> satisfaction = new Var<Integer>("Satisfaction", 0);
	public Var<Integer> disappointment = new Var<Integer>("Disappointment", 0);
	public HashMap<Long,HashSet<Emotion>> map=new HashMap<Long,HashSet<Emotion>>();
	

	@Test
    public void runGeneratedTests() throws IOException {

	    String rootFolder = new File(System.getProperty("user.dir")).getParent();
        String testFolder = rootFolder + File.separator + "MBTtest";
        String modelFolder = testFolder + File.separator + "Model";
        String UdpateFolder = testFolder + File.separator + "UpdatedModel";
        String labRecruitesExeRootDir = rootFolder + File.separator + "iv4xrDemo";
        
        model_test_IOoperations set=new model_test_IOoperations();
        efsm_copy=set.loadModel(modelFolder);
        
        // load tests from file
        SuiteChromosome loadedSolution = set.parseTests(testFolder);
        // open the server
        LabRecruitsTestServer testServer = new LabRecruitsTestServer(false,
                Platform.PathToLabRecruitsExecutable(labRecruitesExeRootDir));
        // set the configuration of the server 
        // level file name is hard coded in writeModel but can be changed
        LabRecruitsConfig lrCfg = new LabRecruitsConfig("LabRecruits_level", modelFolder);
        levelsize lrsize= new levelsize(CSVImport.ImportFromCSV("LabRecruits_level", modelFolder));    
        
        // convert test cases in loadedSolution to goal structure: use the test case executor to convert
        LabRecruitsTestSuiteExecutor lrExecutor = new LabRecruitsTestSuiteExecutor(rootFolder, testFolder, modelFolder, null);
        
        // iterate over test case and convert to goals
        String results = "" ;
        for (int i = 0; i < loadedSolution.size(); i++) {
            AbstractTestSequence testcase = (AbstractTestSequence) loadedSolution.getTestChromosome(i).getTestcase();
            // the system returns a list of goals that can be sequentialized with SEQ
            LabRecruitsEnvironment labRecruitsEnvironment = new LabRecruitsEnvironment(lrCfg);
            var dataCollector = new TestDataCollector();
            // run the testing task:
            LabRecruitsTestAgent testAgent = new LabRecruitsTestAgent("Agent1") ; 
            testAgent.attachState(new BeliefState())
                     .setTestDataCollector(dataCollector)
                     .attachEnvironment(labRecruitsEnvironment) ;

            List<GoalStructure> goals = lrExecutor.convertTestCaseToGoalStructure(testAgent, testcase);
            // Add instrumentation here:
            goals = set.instrumentTestCase(testAgent,testcase,goals) ;
            GoalStructure g =SEQ(goals.toArray(new GoalStructure[goals.size()]));
            testAgent.setGoal(g) ;
            System.out.println(">> Testing task: " + i + ", #=" + goals.size()) ;
            System.out.println(">> " + testcase ) ;    
            
            // add an event-producer to the test agent so that it produce events for
            // emotion appraisals:
            EventsProducer eventsProducer = new EventsProducer().attachTestAgent(testAgent);
            	
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
                 
                 eventsProducer.generateCurrentEvents();
                 if (eventsProducer.currentEvents.isEmpty()) {
                     eas.update(new Tick(), t);
                 }
                 else {
                     for (Message m : eventsProducer.currentEvents) {
                         eas.update( new LREvent(m.getMsgName()), t);
                         if(!eas.newEmotions.isEmpty())
                         {
                         map.put(testAgent.getState().worldmodel.timestamp, eas.newEmotions);
                         }
                     }
                 }

                 if (position != null) {
                    
                     Float score = (float) testAgent.getState().worldmodel.score;
                     System.out.println("*** score=" + score);
                     emodata.recordNewrow(score,eas,position,t);
                     
                 }
                if (t>10000) {
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
            update_EFSMmodel(map,testAgent);
            exportToCSV(emodata.csvData_goalQuestIsCompleted, "data_goalQuestCompleted_"+i+".csv");
            exportToCSV(emodata.csvData_goalGetMuchPoints, "data_goalGetMuchPoints_"+i+".csv");

        }
        //set.writeModel(UdpateFolder); //fix its problem with efsm get instance
        System.out.println("" + results) ;
        testServer.close();
        
	}

	private void update_EFSMmodel(HashMap<Long, HashSet<Emotion>> activated_emotions, TestAgent agent) throws IOException {

		HashMap<String,Long[]> tr_timestamp=new HashMap<String,Long[]>();
		for(var e : agent.getTestDataCollector().getTestAgentTrace(agent.getId()))
		{	
			if(e.getFamilyName().startsWith("MBT-transition") &&e.getFamilyName().contains("START"))
			{
				ArrayList<String> info = 
						new  ArrayList<String>(Arrays.asList(e.getInfo().split(":")));
				
				//find the the time that the same transition ends and return the timestamp.
				String t_end=agent.getTestDataCollector().getTestAgentTrace(agent.getId()).stream()
															.filter(t->(t.getInfo().contains(info.get(0))&&t.getFamilyName().contains("END")))
															.map(t-> t.getInfo().substring(t.getInfo().lastIndexOf(":")+1))
															.findFirst().orElse(null);																						
				// add to have all transition names (tr_id+ t.string) with their start & ending time.
				if(t_end!=null)
				{
					tr_timestamp.put(info.get(0), new Long[] {Long.parseLong(info.get(1)),Long.parseLong(t_end)});	
				}
			}
		}
		
		Assign<Boolean> assign=null;
		EFSMTransition tr=null;
		for (Long key : activated_emotions.keySet()) {		
		    String transition_name=null;
			HashSet<Emotion> value = activated_emotions.get(key);
		    //check the time the emotion is triggered is in range for any transition 
		    transition_name=tr_timestamp.entrySet().stream().filter(p->p.getValue()[0]<=key&& key<=p.getValue()[1])
		    												.map(k->k.getKey())
		    												.findFirst().orElse(null);		    
		    if(transition_name!=null) {
		    	 tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));}
		    
	    	//just a dummy code from here for creating an assign for each emotion type and add it to the transition operation
		    if(tr!=null)
		    {
			    
		    	boolean hope_match =value.stream().anyMatch(x ->x.etype==EmotionType.Hope);
			    if(hope_match)
			    {
			    	assign = new Assign(hope , new IntSum(hope,new Const(1)));
			    	
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    	
			    }
			    boolean fear_match=value.stream().anyMatch(x ->x.etype==EmotionType.Fear);
			    if(fear_match)
			    {
			    	assign = new Assign(fear , new IntSum(fear,new Const(1)));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean joy_match=value.stream().anyMatch(x ->x.etype==EmotionType.Joy);
			    if(joy_match)
			    {
			    	assign = new Assign(joy , new IntSum(joy,new Const(1)));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean distress_match=value.stream().anyMatch(x ->x.etype==EmotionType.Distress);
			    if(distress_match)
			    {
			    	assign = new Assign(distress , new IntSum(distress,new Const(1)));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean satf_match=value.stream().anyMatch(x ->x.etype==EmotionType.Satisfaction);
			    if(satf_match)
			    {
			    	assign = new Assign(satisfaction , new IntSum(satisfaction,new Const(1)));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean disap_match=value.stream().anyMatch(x ->x.etype==EmotionType.Disappointment);
			    if(disap_match)
			    {
			    	assign = new Assign(disappointment , new IntSum(disappointment,new Const(1)));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
				break;
		    }
		}
	}
}
