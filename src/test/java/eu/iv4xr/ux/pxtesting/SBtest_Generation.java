package eu.iv4xr.ux.pxtesting;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.fbk.iv4xr.mbt.MBTProperties;
import eu.fbk.iv4xr.mbt.MBTProperties.LR_random_mode;
import eu.fbk.iv4xr.mbt.MBTProperties.ModelCriterion;
import eu.fbk.iv4xr.mbt.efsm.EFSM;
import eu.fbk.iv4xr.mbt.efsm.EFSMFactory;
import eu.fbk.iv4xr.mbt.efsm.EFSMState;
import eu.fbk.iv4xr.mbt.efsm.EFSMTransition;
import eu.fbk.iv4xr.mbt.efsm.labRecruits.LabRecruitsRandomEFSM;
import eu.fbk.iv4xr.mbt.efsm.labRecruits.LabRecruitsRandomEFSM.StateType;
import eu.fbk.iv4xr.mbt.efsm.modelcheckingInterface.InterfaceToIv4xrModelCheker;
import eu.fbk.iv4xr.mbt.efsm.modelcheckingInterface.InterfaceToIv4xrModelCheker.EFSMTransitionWrapper;
import eu.fbk.iv4xr.mbt.strategy.GenerationStrategy;
import eu.fbk.iv4xr.mbt.strategy.SearchBasedStrategy;
import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.testcase.MBTChromosome;
import eu.fbk.iv4xr.mbt.testsuite.SuiteChromosome;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker;
import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.ITransition;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;
import nl.uu.cs.aplib.utils.Pair;
import eu.fbk.iv4xr.mbt.Main;

/**
 * In this test I'll show how to - use mbt to create a Lab Recruits level -
 * generate a test suite on it - serialize the level and the test suite on the
 * disk
 * 
 * @author prandi
 */
public class SBtest_Generation {

	// use a logger to save output execution information
	protected static final Logger logger = LoggerFactory.getLogger(Main.class);	
	protected List <EFSMState> goalstates = new ArrayList<EFSMState>();
	public void setPropertiesMBT() {
		
		MBTProperties.LR_seed = 7652;
		MBTProperties.LR_generation_mode = LR_random_mode.N_BUTTONS_DEPENDENT;
		MBTProperties.LR_mean_buttons = 0.5;
		MBTProperties.LR_n_rooms = 5;
		MBTProperties.LR_n_doors = 5;
		MBTProperties.SEARCH_BUDGET = 60;
		MBTProperties.SUT_EFSM = "labrecruits.random_simple";
		// there are some predefined configuration to pass to MBTProperties.SUT_EFSM
		// "labrecruits.random_simple", "labrecruits.random_medium",
		// "labrecruits.random_large"
		MBTProperties.LR_n_goalFlags = 1 ;

		MBTProperties.MODELCRITERION = new ModelCriterion[] {
				ModelCriterion.STATE 
		};
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
	public List<MBTChromosome> runSBTestGeneration(Set<String> targetSet) {

		// initialize the test generator to use search based strategy
		SearchBasedStrategy sbStrategy = new SearchBasedStrategy<>();
		SuiteChromosome generatedTests = sbStrategy.generateTests(targetSet);
		List<MBTChromosome> testChromosomes = generatedTests.getTestChromosomes();
		return testChromosomes;
	}
	
	//run a test for testgeneration using SBT developed by FBK
	@Test
	public void runSBTGenerationTest() {
		
		// set the parameters for the generation
		setPropertiesMBT();
		
		if (!existsLabRecruitLevel()) {
			fail();
		}
	
		//get goal state	
		EFSM efsm=EFSMFactory.getInstance().getEFSM();
		for(var state : efsm.getStates()) {
			EFSMState state_ = (EFSMState) state ;
			if(LabRecruitsRandomEFSM.getStateType(state_) == StateType.GoalFlag) {
				goalstates.add(state_) ;
			}
		} ;
		assertTrue(goalstates != null) ;
		String goalid = goalstates.get(0).getId() ;

		
		List<AbstractTestSequence> absTestsuite = new ArrayList<AbstractTestSequence>() ;
	
		for(var efsmtr : efsm.getTransitons())
		{
			Set<String> targetSet = new HashSet<>();
			targetSet.add(goalid);
			targetSet.add(((EFSMTransition)efsmtr).getSrc().getId().toString());
			targetSet.add(((EFSMTransition)efsmtr).getTgt().getId().toString());
			System.out.println("Source"+((EFSMTransition)efsmtr).getSrc().getId().toString());
			System.out.println("Target"+((EFSMTransition)efsmtr).getTgt().getId().toString());
			
			var starttime = System.currentTimeMillis() ;
		
			List<MBTChromosome> testChromosomes =runSBTestGeneration(targetSet);
		
			float duration = ((float) (System.currentTimeMillis() - starttime)) / 1000f ; 
	
			System.out.println("\nGenerated "+testChromosomes.size()+" test cases");
			for(MBTChromosome chr : testChromosomes) {
				AbstractTestSequence testcase = (AbstractTestSequence) chr.getTestcase();
				if(!absTestsuite.contains(testcase))
				{
					absTestsuite.add(testcase);
				}
			}
			break;
		}
		
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
		io.writeTests(absTestsuite, testFolder);
		io.writeModel(modelFolder);
	}
	
	
}
