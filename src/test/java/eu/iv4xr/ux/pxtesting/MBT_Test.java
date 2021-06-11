package eu.iv4xr.ux.pxtesting;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
//import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agents.LabRecruitsTestAgent;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.fbk.iv4xr.mbt.MBTProperties;
import eu.fbk.iv4xr.mbt.MBTProperties.LR_random_mode;
import eu.fbk.iv4xr.mbt.efsm.EFSM;
import eu.fbk.iv4xr.mbt.efsm.EFSMFactory;
import eu.fbk.iv4xr.mbt.strategy.GenerationStrategy;
//import eu.fbk.iv4xr.mbt.strategy.RandomTestStrategy;
import eu.fbk.iv4xr.mbt.strategy.SearchBasedStrategy;
import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.testcase.MBTChromosome;
import eu.fbk.iv4xr.mbt.testsuite.SuiteChromosome;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import game.LabRecruitsTestServer;
import game.Platform;
import eu.fbk.iv4xr.mbt.execution.labrecruits.LabRecruitsTestSuiteExecutor;

import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import world.BeliefState;
import eu.fbk.iv4xr.mbt.Main;

/**
 * In this test I'll show how to - use mbt to create a Lab Recruits level -
 * generate a test suite on it - serialize the level and the test suite on the
 * disk - load from the disk a test suite - transform a test suite into a goal
 * structure
 * 
 * @author prandi
 *
 */
public class MBT_Test {

	// use a logger to save output execution information
	protected static final Logger logger = LoggerFactory.getLogger(Main.class);

	/**
	 * iv4xr-mbt uses MBTProperties to set properties. In the following method
	 * explains how to use it
	 */
	public void setPropertiesMBT() {
		// to control level creation, there are few parameters
		// seed for random number generation
		MBTProperties.LR_seed = 7652;

		// MBT random level generation has few mode that can be selected with
		// LR_random_mode
		// This mode allows to specify the number of rooms and the mean number of
		// buttons in a room
		MBTProperties.LR_generation_mode = LR_random_mode.N_BUTTONS_DEPENDENT;

		// Expected number of buttons in a room follow a Poisson distribution with
		// parameter
		MBTProperties.LR_mean_buttons = 0.5;

		// Number of room to generate
		MBTProperties.LR_n_rooms = 5;

		// Number of doors (corridors) to connect rooms
		MBTProperties.LR_n_doors = 5;

		// Time budget for test generation
		MBTProperties.SEARCH_BUDGET = 60;
		
		// MBT has a model factory controlled by the SUT_EFSM property
		// "labrecruits.random_default" generate a lab recruits level with parameters
		// specified above
		MBTProperties.SUT_EFSM = "labrecruits.random_default";
		// there are some predefined configuration to pass to MBTProperties.SUT_EFSM
		// "labrecruits.random_simple", "labrecruits.random_medium",
		// "labrecruits.random_large"

	}

	/**
	 * Check if LabRecruits level exists
	 */
	public boolean existsLabRecruitLevel() {
		String efsmString = EFSMFactory.getInstance().getEFSM().getEFSMString();
		boolean out = !efsmString.equalsIgnoreCase("");
		
		return out;
	}
	
	/**
	 * Run test generation
	 */
	public SuiteChromosome runTestGeneration() {

		// initialize the test generator to use search based strategy
		GenerationStrategy generationStrategy = new SearchBasedStrategy<MBTChromosome>();
		// random stategy could also be used
		// GenerationStrategy generationStrategy = new
		// RandomTestStrategy<MBTChromosome>();

		// run test generation
		SuiteChromosome solution = generationStrategy.generateTests();
		return solution;
	}

	/**
	 * Save generated test
	 * 
	 * @param solution   solution generated
	 * @param testFolder folder where to save the tests (make sure tests folder
	 *                   exists)
	 */
	private void writeTests(SuiteChromosome solution, String testFolder) {
		// make sure tests folder exists
		File testsFolder = new File(testFolder);
		testsFolder.mkdirs();
		int count = 1;
		for (MBTChromosome testCase : solution.getTestChromosomes()) {
			String dotFileName = testFolder + File.separator + "test_" + count + ".dot";
			String txtFileName = testFolder + File.separator + "test_" + count + ".txt";
			String serFileName = testFolder + File.separator + "test_" + count + ".ser";
			File dotFile = new File(dotFileName);
			File txtFile = new File(txtFileName);
			try {
				FileUtils.writeStringToFile(dotFile, ((AbstractTestSequence) testCase.getTestcase()).toDot(),
						Charset.defaultCharset());
				FileUtils.writeStringToFile(txtFile, testCase.getTestcase().toString(), Charset.defaultCharset());
				TestSerializationUtils.saveTestSequence((AbstractTestSequence) testCase.getTestcase(), serFileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			count++;
		}

	}

	/**
	 * Save EFSM model
	 * 
	 * @param modelFolderName folder where to save the model
	 */
	public void writeModel(String modelFolderName) {
		File modelFolder = new File(modelFolderName);
		modelFolder.mkdirs();

		String modelFileName = modelFolderName + File.separator + "EFSM_model.ser";
		String levelFileName = modelFolderName + File.separator + "LabRecruits_level.csv";
		String modelDotFileName = modelFolderName + File.separator + "EFSM_model.dot";

		File csvFile = new File(levelFileName);
		File dotFile = new File(modelDotFileName);

		EFSM efsm = EFSMFactory.getInstance().getEFSM();
		try {

			FileUtils.writeByteArrayToFile(new File(modelFileName), EFSMFactory.getInstance().getOriginalEFSM());
			FileUtils.writeStringToFile(dotFile, efsm.getDotString(), Charset.defaultCharset());
			// if csv is available
			if (efsm.getEFSMString() != "") {
				FileUtils.writeStringToFile(csvFile, efsm.getEFSMString(), Charset.defaultCharset());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load serialized tests into a SuiteChromosome object
	 * 
	 * @param testsDir
	 * @return
	 */
	private SuiteChromosome parseTests(String testsDir) {
		SuiteChromosome suite = new SuiteChromosome();
		try {
			List<File> files = org.apache.maven.shared.utils.io.FileUtils.getFiles(new File(testsDir), "*.ser", "");
			for (File file : files) {
				AbstractTestSequence test = TestSerializationUtils.loadTestSequence(file.getAbsolutePath());
				MBTChromosome chromosome = new MBTChromosome<>();
				chromosome.setTestcase(test);
				suite.addTest(chromosome);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return suite;
	}

	@Test
	public void runGenerationTestAndConvertToGoals() {
		// set the parameters for the generation
		setPropertiesMBT();

		// check if csv can be generated
		if (!existsLabRecruitLevel()) {
			fail();
		}
		
		// run test generation
		SuiteChromosome solution = runTestGeneration();

		// output folders
		String rootFolder = new File(System.getProperty("user.dir")).getParent();
		String testFolder = rootFolder + File.separator + "MBTtest";
		String modelFolder = testFolder + File.separator + "Model";
		String labRecruitesExeRootDir = rootFolder + File.separator + "iv4xrDemo";
		
		// save generated tests
		File testFolderFile = new File(testFolder);
		if (!testFolderFile.exists()) {
			testFolderFile.mkdirs();
		}
		File modelFolderFile = new File(modelFolder);
		if (!modelFolderFile.exists()) {
			modelFolderFile.mkdirs();
		}

		writeTests(solution, testFolder);
		writeModel(modelFolder);

		/*
		 * The test generation finish here. The following part load the tests from file.
		 */

		// load test from file
		SuiteChromosome loadedSolution = parseTests(testFolder);

		// before converting it is needed a LR test agent

		// open the server
		LabRecruitsTestServer testServer = new LabRecruitsTestServer(false,
				Platform.PathToLabRecruitsExecutable(labRecruitesExeRootDir));

		// set the configuration of the server 
		// level file name is hard coded in writeModel but can be changed
		LabRecruitsConfig lrCfg = new LabRecruitsConfig("LabRecruits_level", modelFolder);

		// start LabRecruits environment
		LabRecruitsEnvironment labRecruitsEnvironment = new LabRecruitsEnvironment(lrCfg);

		// create the agent and attach the goal structure 
		// the random level generator create agent Agent1
		LabRecruitsTestAgent testAgent = new LabRecruitsTestAgent("Agent1").attachState(new BeliefState())
				.attachEnvironment(labRecruitsEnvironment);

		// convert test cases in loadedSolution to goal structure
		
		// use the test case executor to convert
		LabRecruitsTestSuiteExecutor lrExecutor = new LabRecruitsTestSuiteExecutor(rootFolder, testFolder, modelFolder, null);
		
		// iterate over test case and convert to goals
		for (int i = 0; i < loadedSolution.size(); i++) {
			AbstractTestSequence testcase = (AbstractTestSequence) loadedSolution.getTestChromosome(i).getTestcase();
			
			// the system returns a list of goals that can be sequentialized with SEQ
			List<GoalStructure> goals = lrExecutor.convertTestCaseToGoalStructure(testAgent, testcase);
		}
	}

}
