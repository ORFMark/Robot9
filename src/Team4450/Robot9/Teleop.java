
package Team4450.Robot9;

import java.lang.Math;

import Team4450.Lib.*;
import Team4450.Lib.JoyStick.*;
import Team4450.Lib.LaunchPad.*;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

class Teleop
{
	private final Robot 		robot;
	private double				powerFactor = 1.0;
	private JoyStick			rightStick, leftStick, utilityStick;
	private LaunchPad			launchPad;
	private final FestoDA		shifterValve;
	private final RevDigitBoard	revBoard = new RevDigitBoard();
	//private final DigitalInput	hallEffectSensor = new DigitalInput(0);
	
	// Constructor.
	
	Teleop(Robot robot)
	{
		Util.consoleLog();

		this.robot = robot;
		
		shifterValve = new FestoDA(1);
		
		shifterClose();
	}

	// Free all objects that need it.
	
	void dispose()
	{
		Util.consoleLog();
		
		if (leftStick != null) leftStick.dispose();
		if (rightStick != null) rightStick.dispose();
		if (utilityStick != null) utilityStick.dispose();
		if (launchPad != null) launchPad.dispose();
		if (shifterValve != null) shifterValve.dispose();
		if (revBoard != null) revBoard.dispose();
		//if (hallEffectSensor != null) hallEffectSensor.free();
	}

	void OperatorControl()
	{
		double	rightY, leftY;
        
        // Motor safety turned off during initialization.
        robot.robotDrive.setSafetyEnabled(false);

		Util.consoleLog();
		
		LCD.printLine(1, "Mode: OperatorControl");
		LCD.printLine(2, "All=%s, Start=%d, FMS=%b", robot.alliance.name(), robot.location, robot.ds.isFMSAttached());

		SmartDashboard.putNumber("Power Factor", powerFactor * 100);
		
		// Configure LaunchPad and Joystick event handlers.
		
		launchPad = new LaunchPad(robot.launchPad, LaunchPadControlIDs.BUTTON_SIX, this);
		LaunchPadControl lpControl = launchPad.AddControl(LaunchPadControlIDs.BUTTON_FOUR);
		lpControl.controlType = LaunchPadControlTypes.SWITCH;
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_ONE);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_EIGHT);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_YELLOW);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_BLUE);
        launchPad.addLaunchPadEventListener(new LaunchPadListener());
        launchPad.Start();

		leftStick = new JoyStick(robot.leftStick, "LeftStick", JoyStickButtonIDs.TOP_LEFT, this);
		leftStick.AddButton(JoyStickButtonIDs.TOP_RIGHT);
		leftStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		leftStick.AddButton(JoyStickButtonIDs.TOP_BACK);
        leftStick.addJoyStickEventListener(new LeftStickListener());
        leftStick.Start();
        
		rightStick = new JoyStick(robot.rightStick, "RightStick", JoyStickButtonIDs.TOP_LEFT, this);
        rightStick.addJoyStickEventListener(new RightStickListener());
        rightStick.Start();
        
		utilityStick = new JoyStick(robot.utilityStick, "UtilityStick", JoyStickButtonIDs.TOP_LEFT, this);
        utilityStick.addJoyStickEventListener(new UtilityStickListener());
        utilityStick.Start();
        
        // Motor safety turned on.
        robot.robotDrive.setSafetyEnabled(true);
        
        robot.RFTalon.setEncPosition(0);
        
		// Driving loop runs until teleop is over.

		while (robot.isEnabled() && robot.isOperatorControl())
		{
			// Get joystick deflection and feed to robot drive object.
			// using calls to our JoyStick class.

			rightY = rightStick.GetY();		// fwd/back right
			leftY = leftStick.GetY();		// fwd/back left

			LCD.printLine(4, "leftY=%.4f  rightY=%.4f, power=%f", leftY, rightY, powerFactor);

			// This corrects stick alignment error when trying to drive straight. 
			if (Math.abs(rightY - leftY) < 0.2) rightY = leftY;
			
			// Set motors.

			robot.robotDrive.tankDrive(leftY * powerFactor, rightY * powerFactor);

			//LCD.printLine(7, "penc=%d  pv=%d", robot.RFTalon.getPulseWidthPosition(), robot.RFTalon.getPulseWidthVelocity());
			//LCD.printLine(8, "aenc=%d  av=%d", robot.RFTalon.getAnalogInPosition(), robot.RFTalon.getAnalogInVelocity());
			LCD.printLine(9, "qenc=%d  qv=%d", robot.RFTalon.getEncPosition(), robot.RFTalon.getEncVelocity());
			
			LCD.printLine(10, "A=%b  B=%b  pot=%d  potv=%f  hes=%b", revBoard.getButtonA(), revBoard.getButtonB(), 
					revBoard.getPotValue(), revBoard.getPot().getVoltage(), false); //, !hallEffectSensor.get());
			
			// End of driving loop.
			
			Timer.delay(.020);	// wait 20ms for update from driver station.
		}
		
		// End of teleop mode.
		
		Util.consoleLog("end");
	}

	void shifterClose()
	{
		Util.consoleLog();
		
		shifterValve.Close();
	}

	void shifterOpen()
	{
		Util.consoleLog();
		
		shifterValve.Open();
	}

	// Handle LaunchPad control events.
	
	public class LaunchPadListener implements LaunchPadEventListener 
	{
	    public void ButtonDown(LaunchPadEvent launchPadEvent) 
	    {
			Util.consoleLog("%s, latchedState=%b", launchPadEvent.control.id.name(),  launchPadEvent.control.latchedState);
			
			// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			
			if (launchPadEvent.control.id.equals(LaunchPad.LaunchPadControlIDs.BUTTON_SIX))
				if (launchPadEvent.control.latchedState)
					robot.cameraThread.ChangeCamera(robot.cameraThread.cam2);
				else
					robot.cameraThread.ChangeCamera(robot.cameraThread.cam1);
			
			if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_ONE)
			{
				((Teleop) launchPadEvent.getSource()).powerFactor = 1.0;
				SmartDashboard.putNumber("Power Factor", ((Teleop) launchPadEvent.getSource()).powerFactor * 100);
			}
			
			if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_EIGHT)
			{
				((Teleop) launchPadEvent.getSource()).powerFactor = 0.5;
				SmartDashboard.putNumber("Power Factor", ((Teleop) launchPadEvent.getSource()).powerFactor * 100);
			}

			if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_YELLOW)
			{
				//revBoard.display("");
				
				if (launchPadEvent.control.latchedState)
    				shifterOpen();
    			else
    				shifterClose();
			}

			if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_BLUE)
			{
				//revBoard.blink(true);
				//revBoard.displayTestPattern();
				//revBoard.display("ZX 9");
			}
	    }
	    
	    public void ButtonUp(LaunchPadEvent launchPadEvent) 
	    {
	    	//Util.consoleLog("%s, latchedState=%b", launchPadEvent.control.name(),  launchPadEvent.control.latchedState);
	    }

	    public void SwitchChange(LaunchPadEvent launchPadEvent) 
	    {
	    	Util.consoleLog("%s", launchPadEvent.control.id.name());

	    	// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			
			if (launchPadEvent.control.id.equals(LaunchPadControlIDs.BUTTON_FOUR))
				if (launchPadEvent.control.latchedState)
					robot.cameraThread.ChangeCamera(robot.cameraThread.cam2);
				else
					robot.cameraThread.ChangeCamera(robot.cameraThread.cam1);
	    }
	}

	// Handle Right JoyStick Button events.
	
	private class RightStickListener implements JoyStickEventListener 
	{
	    public void ButtonDown(JoyStickEvent joyStickEvent) 
	    {
			Util.consoleLog("%s, latchedState=%b", joyStickEvent.button.id.name(),  joyStickEvent.button.latchedState);
			
			// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_LEFT))
				if (joyStickEvent.button.latchedState)
					((CameraFeed) robot.cameraThread).ChangeCamera(((CameraFeed) robot.cameraThread).cam2);
				else
					((CameraFeed) robot.cameraThread).ChangeCamera(((CameraFeed) robot.cameraThread).cam1);			
	    }

	    public void ButtonUp(JoyStickEvent joyStickEvent) 
	    {
	    	//Util.consoleLog("%s", joyStickEvent.button.name());
	    }
	}

	// Handle Left JoyStick Button events.
	
	private class LeftStickListener implements JoyStickEventListener 
	{
	    public void ButtonDown(JoyStickEvent joyStickEvent) 
	    {
			Util.consoleLog("%s, latchedState=%b", joyStickEvent.button.id.name(),  joyStickEvent.button.latchedState);
			
			// Change the power factor setting.

			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_LEFT)) ((Teleop) joyStickEvent.getSource()).powerFactor = 1.0;
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_RIGHT)) ((Teleop) joyStickEvent.getSource()).powerFactor = 0.5;
			
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_MIDDLE))
			{
				if (((Teleop) joyStickEvent.getSource()).powerFactor == 1.0)
					((Teleop) joyStickEvent.getSource()).powerFactor = .75;
				else if (((Teleop) joyStickEvent.getSource()).powerFactor == .75)
					((Teleop) joyStickEvent.getSource()).powerFactor = .50;
				else if (((Teleop) joyStickEvent.getSource()).powerFactor == .50)
					((Teleop) joyStickEvent.getSource()).powerFactor = .25;
			}
			
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_BACK))
			{
				if (((Teleop) joyStickEvent.getSource()).powerFactor == .25)
					((Teleop) joyStickEvent.getSource()).powerFactor = .50;
				else if (((Teleop) joyStickEvent.getSource()).powerFactor == .50)
					((Teleop) joyStickEvent.getSource()).powerFactor = .75;
				else if (((Teleop) joyStickEvent.getSource()).powerFactor == .75)
					((Teleop) joyStickEvent.getSource()).powerFactor = 1.0;
			}

			SmartDashboard.putNumber("Power Factor", ((Teleop) joyStickEvent.getSource()).powerFactor * 100);
	    }

	    public void ButtonUp(JoyStickEvent joyStickEvent) 
	    {
	    	//Util.consoleLog("%s", joyStickEvent.button.name());
	    }
	}

	// Handle Utility JoyStick Button events.
	
	private class UtilityStickListener implements JoyStickEventListener 
	{
	    public void ButtonDown(JoyStickEvent joyStickEvent) 
	    {
			Util.consoleLog("%s, latchedState=%b", joyStickEvent.button.id.name(),  joyStickEvent.button.latchedState);
			
			// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_LEFT))
				if (joyStickEvent.button.latchedState)
					((CameraFeed) robot.cameraThread).ChangeCamera(((CameraFeed) robot.cameraThread).cam2);
				else
					((CameraFeed) robot.cameraThread).ChangeCamera(((CameraFeed) robot.cameraThread).cam1);
	    }

	    public void ButtonUp(JoyStickEvent joyStickEvent) 
	    {
	    	//Util.consoleLog("%s", joyStickEvent.button.id.name());
	    }
	}
}
