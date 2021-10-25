
# OCC4PXTesting

# master branch
This is basically the implemented codes of the paper called "An Appraisal Transition System for Event-driven Emotions in Agent-based Player Experience Testing
" accepted on EMAS2021@AMAAS(in press) which can be accessed via https://arxiv.org/abs/2105.05589

The appriasal system is used for multiple Lab recruits levels including Lab1. Using the written python script, emotional data is visualized in terms of timeline and heatmaps and got stored. 
The default setting is to run Lab1.
to run the test of every level, you only need to set the name of key door (final door) of your chosen level:

	to run SimplerExperiment_1 ...> KeyDoor = "door3"  in main/PlayerOnecharacterization 
	to run Experiment_2 ....> KeyDoor = "d1"  in main/PlayerOnecharacterization 
	to run Lab1 ...> KeyDoor="d_finish" in main/PlayerOnecharacterization 

Graphs, when the test is terminated succesfully, are stored in the project directory.

