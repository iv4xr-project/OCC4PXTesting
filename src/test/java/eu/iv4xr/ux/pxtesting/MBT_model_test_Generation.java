package eu.iv4xr.ux.pxtesting;

import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;


import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.fbk.iv4xr.mbt.MBTProperties;
import eu.fbk.iv4xr.mbt.MBTProperties.LR_random_mode;
import eu.fbk.iv4xr.mbt.efsm.EFSM;
import eu.fbk.iv4xr.mbt.efsm.EFSMFactory;
import eu.fbk.iv4xr.mbt.strategy.GenerationStrategy;
import eu.fbk.iv4xr.mbt.strategy.SearchBasedStrategy;
import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.testcase.MBTChromosome;
import eu.fbk.iv4xr.mbt.testsuite.SuiteChromosome;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import eu.fbk.iv4xr.mbt.Main;

/**
 * In this test I'll show how to - use mbt to create a Lab Recruits level -
 * generate a test suite on it - serialize the level and the test suite on the
 * disk
 * 
 * @author prandi
 */
public class MBT_model_test_Generation {

	// use a logger to save output execution information
	protected static final Logger logger = LoggerFactory.getLogger(Main.class);	
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
		MBTProperties.SUT_EFSM = "labrecruits.random_simple";
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
		// OR RandomTestStrategy<MBTChromosome>();
		SuiteChromosome solution = generationStrategy.generateTests();
		return solution;
	}


	@Test
	public void runGenerationTest() {
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
		
		// save generated tests
		File testFolderFile = new File(testFolder);
		if (!testFolderFile.exists()) {
			testFolderFile.mkdirs();
		}
		File modelFolderFile = new File(modelFolder);
		if (!modelFolderFile.exists()) {
			modelFolderFile.mkdirs();
		}
		model_test_IOoperations io=new model_test_IOoperations();
		io.writeTests(solution, testFolder);
		io.writeModel(modelFolder);
	}

}
