package org.team3128;

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;


/**
 * The robot's main class is where all of the hardware objects that represent the robot are stored.
 * It also sets up the control bindings.<br>
 * 
 * Its functions are called only by RobotTemplate.<br>
 * 
 * This class defines the interface for the the robot's main class, 
 * so that we can have different main classes for different robots. 
 * @author Jamie
 *
 */
public abstract class MainClass
{	
	/**
	 * Add the ListenerManagers to RobotTemplate and do anything else that needs to be done as soon as the robot starts.
	 * @param robotTemplate
	 */
	protected abstract void initializeRobot(RobotTemplate robotTemplate);
	
	/**
	 * Add all of the autonomous program CommandGroups for this robot into the chooser. <br>
	 * This is called when the robot starts, after initializeRobot().
	 * @param autoChooser
	 */
	protected abstract void addAutoPrograms(SendableChooser autoChooser);

	/**
	 * Called when the robot switches into disabled mode. <br>
	 * This happens when any other mode is disabled, as well as when the robot starts.
	 */
	protected abstract void initializeDisabled();
	
	/**
	 * Called periodically to print any interesting values to the dashboard.
	 */
	protected abstract void updateDashboard();

	/**
	 * Called when autonomous mode starts, before the auto program is run.
	 */
	protected abstract void initializeAuto();
	
	/**
	 * Called when teleop starts.
	 */
	protected abstract void initializeTeleop();
}
