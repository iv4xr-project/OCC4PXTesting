/*
This program has been developed by students from the bachelor Computer Science
at Utrecht University within the Software and Game project course.

©Copyright Utrecht University (Department of Information and Computing Sciences)
*/


package eu.iv4xr.ux.pxtesting;
import agents.EventsProducer;
import agents.LabRecruitsTestAgent;
import eu.iv4xr.ux.pxtesting.PlayerOneCharacterization;
import static eu.iv4xr.ux.pxtesting.PlayerOneCharacterization.*;
import eu.iv4xr.ux.pxtesting.TestSettings;
import agents.tactics.GoalLib;
import agents.tactics.TacticLib;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.extensions.occ.Emotion;
import eu.iv4xr.framework.extensions.occ.EmotionAppraisalSystem;
import eu.iv4xr.framework.extensions.occ.Event;
import eu.iv4xr.framework.extensions.occ.Emotion.EmotionType;
import eu.iv4xr.framework.extensions.occ.Event.Tick;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import helperclasses.datastructures.linq.QArrayList;
import logger.JsonLoggerInstrument;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.multiAgentSupport.Message;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;  
import java.util.*;
import java.util.function.Function;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import game.Platform;
import game.LabRecruitsTestServer;
import world.BeliefState;
import static eu.iv4xr.ux.pxtesting.CSVExport.*;
import static eu.iv4xr.ux.pxtesting.TestSettings.*;
import static nl.uu.cs.aplib.AplibEDSL.*;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class runs an OCC-based Player Experience (PX) evaluation on two
 * variations of the buttons_doors_1 level. (you have to specify which variant
 * to evaluate). This is a small level, but the place is on fire. The player
 * goal is to escape the level by reaching an "exit room" (marked by a goal-flag).
 * The room is guarded by a closed door, so the player has to solve a small puzzle
 * to open this door, and surviving the fire as well.
 * 
 * This will run an iv4xr agent to automatically explore and solve the level. An
 * OCC appraisal system is hooked to that agent that will calculate the
 * emotional state of the agent as it progress through the play. The emotions
 * are recorded in some csv files generated by the evaluation. There is a Python
 * script to visualize the results.
 * @author sansari
 */

public class PX_Testing_SimplerExperiment_1 {

    private static LabRecruitsTestServer labRecruitsTestServer;
    private static String labRecruitesExeRootDir;
    private static Integer level_height;
    private static Integer level_width;
    @BeforeAll
    static void start() {
        // TestSettings.USE_SERVER_FOR_TEST = false ;
        // Uncomment this to make the game's graphic visible:
        TestSettings.USE_GRAPHICS = true; 
        //Changing the root directory
        //It needs to point at the iv4xrDemo directory for the game execution, instead of the current directory. 
        String labRecruitesExeRootParent = new File(System.getProperty("user.dir")).getParent();
        //String labRecruitesExeRootParent = new File(System.getProperty("user.dir")).getPath();
        //String labRecruitesExeRootParent = new File(System.getProperty("user.dir")).getParentFile().getParent();
        System.out.print(labRecruitesExeRootParent);
        labRecruitesExeRootDir= new File(labRecruitesExeRootParent,"iv4xrDemo").getAbsolutePath();
        System.out.print(labRecruitesExeRootDir);
        labRecruitsTestServer = TestSettings.start_LabRecruitsTestServer(labRecruitesExeRootDir);

    }
    

    @AfterAll
    static void close() {
        if (labRecruitsTestServer != null)
            labRecruitsTestServer.close();
    }

    void instrument(Environment env) {
        env.registerInstrumenter(new JsonLoggerInstrument()).turnOnDebugInstrumentation();
    }

    /**
     * Run a Player Experience evaluation on the selected variant of the
     * buttons_doors_1 level. Emotion data are dumped in csv files.
     */
    @Test
    public void runPXEvaluation() throws InterruptedException, IOException {
        // closetReachableTest("buttons_doors_1_setup1") ;

    	// choose your setup here:
    	//set the level size: 
        level_width=12;
        level_height=8;
        runPXEvaluation("buttons_doors_1_setup2");

    }

    /**
     * Run a Player Experience evaluation on specified variant of the
     * buttons_doors_1 level. Emotion data are dumped in csv files.
     */
    public void runPXEvaluation(String button_doors1_setup) throws InterruptedException, IOException {

        // var buttonToTest = "button1" ;
        // var doorToTest = "door1" ;

        // Ctreate an environment
    	// again the root need to get changed. The overloaded config (had existed) is used.
        //var config = new LabRecruitsConfig(button_doors1_setup);
    	var config = new LabRecruitsConfig(button_doors1_setup, Paths.get(labRecruitesExeRootDir, "src", "test", "resources", "levels").toAbsolutePath().toString());
        config.light_intensity = 0.45f;
		
        var environment = new LabRecruitsEnvironment(config);
        if (USE_INSTRUMENT)
            instrument(environment);

        try {
            if (TestSettings.USE_GRAPHICS) {
                System.out.println("You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.");
                new Scanner(System.in).nextLine();
            }

            // create a test agent
            var testAgent = new LabRecruitsTestAgent("agent1") // matches the ID in the CSV file
                    .attachState(new BeliefState()).attachEnvironment(environment);
            
            // we will use the following fiunctional testing task to steer the agent
            // to solve the level. The final goal is to get the agent into a goal-room.
            // This this room as an escape room. 
            var testingTask = SEQ(GoalLib.entityInteracted("button1"), GoalLib.entityStateRefreshed("door1"),
                    GoalLib.entityInvariantChecked(testAgent, "door1", "door1 should be open",
                            (WorldEntity e) -> e.getBooleanProperty("isOpen")),
                    GoalLib.entityInteracted("button3"), GoalLib.entityStateRefreshed("door2"),
                    GoalLib.entityInvariantChecked(testAgent, "door2", "door2 should be open",
                            (WorldEntity e) -> e.getBooleanProperty("isOpen")),
                    GoalLib.entityInteracted("button4"),
                    // GoalLib.entityIsInRange("button3").lift(),
                    // GoalLib.entityIsInRange("door1").lift(),
                    GoalLib.entityStateRefreshed("door1"),
                    GoalLib.entityInvariantChecked(testAgent, "door1", "door1 should be open",
                            (WorldEntity e) -> e.getBooleanProperty("isOpen")),
                    // GoalLib.entityIsInRange("button1").lift(),
                    GoalLib.entityStateRefreshed("door3"),
                    GoalLib.entityInvariantChecked(testAgent, "door3", "door3 should be open",
                            (WorldEntity e) -> e.getBooleanProperty("isOpen")),
                    // move close to the door guarding the goal-room:
                    GoalLib.entityInCloseRange("door3"), 
                    // move into goal room:
                    GoalLib.positionsVisited(new Vec3(11.3f, 0, 4f)));
            
            // attaching the goal and testdata-collector
            var dataCollector = new TestDataCollector();
            testAgent.setTestDataCollector(dataCollector).setGoal(testingTask);

            // add an event-producer to the test agent so that it produce events for
            // emotion appraisals:
            EventsProducer eventsProducer = new EventsProducer().attachTestAgent(testAgent);

            // Create an emotion appraiser, and hook it to the agent:
            EmotionAppraisalSystem eas = new EmotionAppraisalSystem(testAgent.getId());
            eas.attachEmotionBeliefBase(new EmotionBeliefBase().attachFunctionalState(testAgent.getState()))
                    .withUserModel(new PlayerOneCharacterization()).addGoal(questIsCompleted, 50)
                    .addGoal(gotAsMuchPointsAsPossible, 50).addInitialEmotions();

            // some lists for collecting experiment data:
            List<String[]> csvData_goalQuestIsCompleted = new LinkedList<>();
            String[] csvRow = { "t", "x", "y", "hope", "joy", "satisfaction", "fear" };
            csvData_goalQuestIsCompleted.add(csvRow);
            List<String[]> csvData_goalGetMuchPoints = new LinkedList<>();
            csvData_goalGetMuchPoints.add(csvRow);
            Function<Emotion, Float> normalizeIntensity = e -> e != null ? (float) e.intensity / 800f : 0f;

            environment.startSimulation(); // this will press the "Play" button in the game for you
            // goal not achieved yet
            assertFalse(testAgent.success());

            int i = 0;
            // ok, let's now run the agent:
            while (testingTask.getStatus().inProgress()) {
                Vec3 position = testAgent.getState().worldmodel.position;
                System.out.println("*** " + i + ", " + testAgent.getState().id + " @" + position);
                eventsProducer.generateCurrentEvents();
                if (eventsProducer.currentEvents.isEmpty()) {
                    eas.update(new Tick(), i);
                }
                else {
                    for (Message m : eventsProducer.currentEvents) {
                        eas.update( new LREvent(m.getMsgName()), i);
                        
                    }
                }
                
                if (position != null) {
                    Vec3 p_ = position.copy();
                    //p_.z = 8 - p_.z;
                    Float score = (float) testAgent.getState().worldmodel.score;
                    System.out.println("*** score=" + score);

                    float hope_completingQuest = normalizeIntensity
                            .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Hope));
                    float joy_completingQuest = normalizeIntensity
                            .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Joy));
                    float satisfaction_completingQuest = normalizeIntensity
                            .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Satisfaction));
                    float fear_completingQuest = normalizeIntensity
                            .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Fear));

                    float hope_getMuchPoints = normalizeIntensity
                            .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Hope));
                    float joy_getMuchPoints = normalizeIntensity
                            .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Joy));
                    float satisfaction_getMuchPoints = normalizeIntensity
                            .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Satisfaction));
                    float fear_getMuchPoints = normalizeIntensity
                            .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Fear));

                    String[] csvRow1 = { "" + i, "" + p_.x, "" + p_.z, "" + hope_completingQuest,
                            "" + joy_completingQuest, "" + satisfaction_completingQuest, "" + fear_completingQuest };

                    String[] csvRow2 = { "" + i, "" + p_.x, "" + p_.z, "" + hope_getMuchPoints, "" + joy_getMuchPoints,
                            "" + satisfaction_getMuchPoints, "" + fear_getMuchPoints };

                    csvData_goalQuestIsCompleted.add(csvRow1);
                    csvData_goalGetMuchPoints.add(csvRow2);
                }
                Thread.sleep(70);
                i++;
                testAgent.update();
                if (i > 200) {
                    break;
                }
            }
            testingTask.printGoalStructureStatus();

            // check that we have passed both tests above:
            assertTrue(dataCollector.getNumberOfPassVerdictsSeen() == 4);
            // goal status should be success
            assertTrue(testAgent.success());
            // close
            testAgent.printStatus();

            // done. Dump the collected data to csv files:
            exportToCSV(csvData_goalQuestIsCompleted, "data_goalQuestCompleted.csv");
            exportToCSV(csvData_goalGetMuchPoints, "data_goalGetMuchPoints.csv");
            
            // run the python script called "mkgraph.py" for drawing graphs according to the saved .csv  
            String path=new File(new File(System.getProperty("user.dir")).getAbsolutePath(),"mkgraph.py").getAbsolutePath();
			ProcessBuilder builder = new ProcessBuilder(); 
			ProcessBuilder pb = new ProcessBuilder();
			//sending width and heights of the level as parameters.
			builder.command("python", path,""+level_width,""+level_height);
			Process p=builder.start();
			BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
	
			System.out.println(".........start   visualization process.........");  
		    String line = "";     
		    while ((line = bfr.readLine()) != null){
			      System.out.println("Python Output: " + line);
			  }
        } finally {
            environment.close();


        }
    }
}
