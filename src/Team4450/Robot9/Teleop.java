
package Team4450.Robot9;

import java.lang.Math;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Talon;
import Team4450.Lib.*;
import Team4450.Lib.JoyStick.*;
import Team4450.Lib.LaunchPad.*;
import Team4450.Lib.JoyStick.JoyStickButtonIDs;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.Relay;
@SuppressWarnings("unused")
class Teleop
{
	private final Robot 		robot;
	public JoyStick			rightStick, leftStick, utilityStick;
	public LaunchPad			launchPad;
	private final FestoDA		shifterValve,  ptoValve, armValve; //valve3, valve4;
	private boolean				ptoMode = false, invertDrive=false, climbMode=false, limitSwitchEnabled = true;
	private boolean				autoTarget = false;
	public final Ball			ball;
	public final Climb			climb;
	private final Relay				headLight = new Relay(0, Relay.Direction.kForward); 
	public final DigitalInput ClimbLimitUp = new DigitalInput(3);
	private Vision2016			vision= new Vision2016();
	private Vision2016.ParticleReport 	par;
	//private final RevDigitBoard	revBoard = new RevDigitBoard();
	//private final DigitalInput	hallEffectSensor = new DigitalInput(0);
	//public double				RIGHTY, LEFTY, UTILY; 
	// Constructor.
	// encoder is plugged into dio port 1 - orange=+5v blue=signal, dio port 2 black=gnd yellow=signal. 
		private Encoder				encoder = new Encoder(1, 2, true, EncodingType.k4X);
	Teleop(Robot robot)
	{
		Util.consoleLog();

		this.robot = robot;
		
		shifterValve = new FestoDA(0,2);
		ptoValve = new FestoDA(0,0);
		armValve = new FestoDA(1,4);
		

		//valve3 = new FestoDA(4);
		//valve4 = new FestoDA(6);
		ball = new Ball(robot, this);
		climb = new Climb(robot, this);
		//RIGHTY=rightStick.GetY();
		//LEFTY=leftStick.GetY();
		//UTILY=utilityStick.GetY();
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
		if (ptoValve != null) ptoValve.dispose();
		if (ball != null) ball.dispose();
		if (climb != null) climb.dispose();
		//if (ManipulatorValve != null) ManipulatorValve.dispose();
		if (ClimbLimitUp != null) ClimbLimitUp.free();
		if (headLight != null)	headLight.free();
		if (armValve != null) armValve.dispose();
		//if (valve3 != null) valve3.dispose();
		//if (valve4 != null) valve4.dispose();
		//if (revBoard != null) revBoard.dispose();
		//if (hallEffectSensor != null) hallEffectSensor.free();
	}

	void OperatorControl()
	{
		double	rightY, leftY, utilY = 0;
        
        // Motor safety turned off during initialization.
        robot.robotDrive.setSafetyEnabled(false);

		Util.consoleLog();
		
		LCD.printLine(1, "Mode: OperatorControl");
		LCD.printLine(2, "All=%s, Start=%d, FMS=%b", robot.alliance.name(), robot.location, robot.ds.isFMSAttached());
		
		// Initial setting of air valves.
		shifterLow();
		ptoDisable();
		climb.ClimbUp();
		//ManipulatorUp();
		climb.armsIn();
		ArmUp();
		ball.PickupUp();
		ball.AngleUp();
		
		
		//valve3.SetA();
		//valve4.SetA();
		
		// Configure LaunchPad and Joystick event handlers.
		
		launchPad = new LaunchPad(robot.launchPad, LaunchPadControlIDs.BUTTON_BLACK, this);
		LaunchPadControl lpControl = launchPad.AddControl(LaunchPadControlIDs.ROCKER_LEFT_FRONT);
		lpControl.controlType = LaunchPadControlTypes.SWITCH;
		launchPad.AddControl(LaunchPadControlIDs.ROCKER_RIGHT);
		lpControl.controlType = LaunchPadControlTypes.SWITCH;
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_YELLOW);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_BLUE);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_GREEN);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_RED);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_RED_RIGHT);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_BLUE_RIGHT);
		launchPad.AddControl(LaunchPadControlIDs.ROCKER_RIGHT);
        launchPad.addLaunchPadEventListener(new LaunchPadListener());
        launchPad.Start();

		leftStick = new JoyStick(robot.leftStick, "LeftStick", JoyStickButtonIDs.TRIGGER, this);
		leftStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		leftStick.addJoyStickEventListener(new LeftStickListener());
        leftStick.Start();
        
		rightStick = new JoyStick(robot.rightStick, "RightStick", JoyStickButtonIDs.TRIGGER, this);
        rightStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		rightStick.addJoyStickEventListener(new RightStickListener());
        rightStick.Start();
        
		utilityStick = new JoyStick(robot.utilityStick, "UtilityStick", JoyStickButtonIDs.TRIGGER, this);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_LEFT);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_RIGHT);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_BACK);
		utilityStick.AddButton(JoyStickButtonIDs.TRIGGER);
        utilityStick.addJoyStickEventListener(new UtilityStickListener());
        utilityStick.Start();
        
        // Motor safety turned on.
        robot.robotDrive.setSafetyEnabled(true);
        
      
        
		// Driving loop runs until teleop is over.

		while (robot.isEnabled() && robot.isOperatorControl())
		{
			
			// Get joystick deflection and feed to robot drive object.
			// using calls to our JoyStick class.

			if (ptoMode)
			{
			
				rightY = utilityStick.GetY();
				leftY = rightY;
				if (rightY > 0 && ClimbLimitUp.get()&& limitSwitchEnabled) rightY = 0;
				utilY = 0;
			}
			else if (invertDrive)
			{
					rightY =StickMath(rightStick.GetY()) *-1.0;
					leftY = StickMath(leftStick.GetY()) * -1.0; 
					utilY = 0.75*utilityStick.GetY();
			}
			else
			{
    			rightY = StickMath(rightStick.GetY());		// fwd/back right
    			leftY = StickMath(leftStick.GetY());		// fwd/back left\
    			utilY=0.75*utilityStick.GetY();
			}

			LCD.printLine(3, "encoder=%d  climbUp=%b", encoder.get(), ClimbLimitUp.get()); 
			LCD.printLine(4, "leftY=%.4f  rightY=%.4f", leftY, rightY);
			LCD.printLine(6, "shooter encoder=%d  rate=%.3f", ball.encoder.get(), ball.encoder.getRate() * 60);  
			      

			
			// This corrects stick alignment error when trying to drive straight. 
			//if (Math.abs(rightY - leftY) < 0.2) rightY = leftY;
			//commented in order to fix jerky drive
			// Set motors.

			if (!autoTarget) robot.robotDrive.tankDrive(leftY, rightY);
			
			//LCD.printLine(7, "penc=%d  pv=%d", robot.RFTalon.getPulseWidthPosition(), robot.RFTalon.getPulseWidthVelocity());
			//LCD.printLine(8, "aenc=%d  av=%d", robot.RFTalon.getAnalogInPosition(), robot.RFTalon.getAnalogInVelocity());
			
			
			

			// End of driving loop.
			
			Timer.delay(.020);	// wait 20ms for update from driver station.
		}
		
		// End of teleop mode.
		
		Util.consoleLog("end");
	}

	// Transmission control functions.
	
	void shifterLow()
	{
		Util.consoleLog();
		
		shifterValve.SetA();

		SmartDashboard.putBoolean("Low", true);
		SmartDashboard.putBoolean("High", false);
	}

	void shifterHigh()
	{
		Util.consoleLog();
		
		shifterValve.SetB();

		SmartDashboard.putBoolean("Low", false);
		SmartDashboard.putBoolean("High", true);
	}

	void ptoDisable()
	{
		Util.consoleLog();
		
		ptoMode = false;
		
		ptoValve.SetA();

		SmartDashboard.putBoolean("PTO", false);
	}

	void ptoEnable()
	{
		Util.consoleLog();
		
		ptoValve.SetB();

		ptoMode = true;
		
		SmartDashboard.putBoolean("PTO", true);
	}
	/*
	void ManipulatorUp()
	{
		Util.consoleLog();
		ManipulatorValve.SetB();
	}
	void ManipulatorDown()
	{
		Util.consoleLog();
		ManipulatorValve.SetA();
	}
	*/
	double StickMath(double x)
	{
		if (x==0)
			return 0;
		else if (0<x && x<0.5)
		return (x/5+.40);
		else if (0.5 < x && x <= 1)
		return x;
		else if (-.5<x && x<0)
			return (x/5-0.4);
		else if (-1<=x && -0.5>x)
			return x;
		else
			return 0;
		
	}
	void ArmUp()
	{
		Util.consoleLog();
		armValve.SetA();
	}
	void ArmDown()
	{
		Util.consoleLog();
		armValve.SetB();
	}
	/*
	void lightOn()  
	 	{  
	 		headLight.set(Relay.Value.kOn);  
	 		SmartDashboard.putBoolean("Light", true);  
	 	}  
	 	  
	 	void lightOff()  
	 	{  
	 		headLight.set(Relay.Value.kOff);  
	 		rightStick.FindButton(JoyStickButtonIDs.TRIGGER).latchedState = false;  
	 		SmartDashboard.putBoolean("Light", false);  
	 	}  
*/ 
	/**
	 * Rotate the robot by bumping appropriate motors based on the X offset
	 * from center of camera image. 
	 * @param value Target offset. + value means target is right of center so
	 * run right side motors backwards. - value means target is left of ceneter so run
	 * left side motors backwards.
	 */
	void bump(int value)
	{
		Util.consoleLog("%d", value);

		if (value > 0)
			robot.robotDrive.tankDrive(.60, 0);
		else
			robot.robotDrive.tankDrive(0, .60);
			
		Timer.delay(.10);
	}
	
    /**
     * Check current camera image for the target. 
     * @return A particle report for the target.
     */
	Vision2016.ParticleReport findTarget()
	{
		Util.consoleLog();
		
		par = vision.CheckForTarget(robot.cameraThread.CurrentImage());
		
		if (par != null) Util.consoleLog("Target=%s", par.toString());
		
		return par;
	}
	
	/**
	 * Loops checking camera images for target. Stops when no target found.
	 * If target found, check target X location and if needed bump the bot
	 * in the appropriate direction and then check target location again.
	 */
	void seekTarget()
	{
		Vision2016.ParticleReport par;
		
		Util.consoleLog();

		SmartDashboard.putBoolean("TargetLocked", false);
		SmartDashboard.putBoolean("AutoTarget", true);

		par = findTarget();

		autoTarget = true;
		
		while (robot.isEnabled() && autoTarget && par != null)
		{
			if (Math.abs(320 - par.CenterX) > 10)
			{
				bump(320 - par.CenterX);
				
				par = findTarget();
			}
			else
			{
				SmartDashboard.putBoolean("TargetLocked", true);
				par = null;
			}
		}
		
		autoTarget = false;

		SmartDashboard.putBoolean("AutoTarget", false);
	}
	// Handle LaunchPad control events.
	
	public class LaunchPadListener implements LaunchPadEventListener 
	{
	    public void ButtonDown(LaunchPadEvent launchPadEvent) 
	    {
			Util.consoleLog("%s, latchedState=%b", launchPadEvent.control.id.name(),  launchPadEvent.control.latchedState);
			
			// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			
			if (launchPadEvent.control.id.equals(LaunchPad.LaunchPadControlIDs.BUTTON_GREEN))
				if (launchPadEvent.control.latchedState)
					climb.ClimbDown();
				else
					climb.ClimbUp();
				
			if (launchPadEvent.control.id.equals(LaunchPad.LaunchPadControlIDs.BUTTON_BLACK))
				if (launchPadEvent.control.latchedState)
					ArmDown();
				else
					ArmUp();
	
			if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_BLUE_RIGHT)  
				 	{  
						Util.consoleLog();
						if(!autoTarget)
							seekTarget();
						else
							autoTarget=false;
				 		  
				 	}  

			//if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_BLUE)
			//{
			//	Util.consoleLog();
			// if (launchPadEvent.control.latchedState)
    		//		shifterHigh();
    		//	else
    		//		shifterLow();
			//}

			if (launchPadEvent.control.id == LaunchPadControlIDs.BUTTON_BLUE)
			{
				Util.consoleLog();
				if (launchPadEvent.control.latchedState)
				{
					shifterLow();
					ptoEnable();
				}
    			else
    				ptoDisable();
			}
			
			if (launchPadEvent.control.id == (LaunchPadControlIDs.BUTTON_YELLOW)) 
			{
				
			//	if (climbMode=false)
				//{
				Util.consoleLog();
				if (launchPadEvent.control.latchedState) 
				
					ball.AngleUp();
				
				else 
					ball.AngleDown();
				/*
				}
				else
				{
					Util.consoleLog();
					climb.StartAutoClimb();
				}
			*/
			}
	    	
			
			if (launchPadEvent.control.id == (LaunchPadControlIDs.BUTTON_BLACK)) 
			{
				Util.consoleLog();
	    		if (launchPadEvent.control.latchedState)
	    			ball.PickupDown();
	    		else
	    			ball.PickupUp();
	    		
			}
			
			if (launchPadEvent.control.id == (LaunchPadControlIDs.BUTTON_RED_RIGHT))
			{
			//	if (climbMode = false)
				//{
					Util.consoleLog();
				limitSwitchEnabled=!limitSwitchEnabled;
					SmartDashboard.putBoolean("LSOverride", limitSwitchEnabled);
				//}
//				else
//				{
//					Util.consoleLog();
//					climb.StopAutoClimb();
//				}
//				
			}
	    }
	    
		public void ButtonUp (LaunchPadEvent launchPadEvent) 
		{
			    	//Util.consoleLog("%s, latchedState=%b", launchPadEvent.control.name(),  launchPadEvent.control.latchedState);
		}
	   
	    public void SwitchChange(LaunchPadEvent launchPadEvent) 
	    {
	    	Util.consoleLog("%s", launchPadEvent.control.id.name());

	    	// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			/*
			if (launchPadEvent.control.id.equals(LaunchPadControlIDs.ROCKER_LEFT_FRONT))
				if (launchPadEvent.control.latchedState)
					robot.cameraThread.ChangeCamera(robot.cameraThread.cam2);
				else
					robot.cameraThread.ChangeCamera(robot.cameraThread.cam1);
			*/
			if (launchPadEvent.control.id.equals(LaunchPadControlIDs.ROCKER_LEFT_FRONT))
				if (launchPadEvent.control.latchedState)
					climbMode=true;
				else
					climbMode=false;
			if 	(launchPadEvent.control.id.equals(LaunchPadControlIDs.ROCKER_RIGHT))
				if (launchPadEvent.control.latchedState)
					ball.ShooterPowerDown();
			
				else
					ball.ShooterPowerUp();
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
	    
/*
	    if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TRIGGER))  
	    {
	    	if (joyStickEvent.button.latchedState)
	    	lightOn();  
	    	else
	    	lightOff();
	    }
	    */  
	if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_MIDDLE))  
		ball.StopShoot();  
	    }
	    public void ButtonUp(JoyStickEvent joyStickEvent) 
	    {
	    	//Util.consoleLog("%s", joyStickEvent.button.name());
	    }
	}

	// Handle Left JoyStick Button events.
	
	//@SuppressWarnings("unused")
	private class LeftStickListener implements JoyStickEventListener 
	{
	    public void ButtonDown(JoyStickEvent joyStickEvent) 
	    {
			Util.consoleLog("%s, latchedState=%b", joyStickEvent.button.id.name(),  joyStickEvent.button.latchedState);
	  /*  	
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_MIDDLE))  
			 	if (joyStickEvent.button.latchedState)
			 		//ManipulatorDown();
			 	else
			 		//ManipulatorUp();
		*/	   
		if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TRIGGER))  
			if (joyStickEvent.button.latchedState)
				shifterLow();
			else
				shifterHigh();
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
				ball.Fire();  
						else  
			 				ball.Reload();  
			 		  
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_RIGHT))  
					if (joyStickEvent.button.latchedState)  
			 					ball.StartAutoPickup();  
						else  
			 					ball.StopAutoPickup();  
			 			  
			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_MIDDLE))  
					if (joyStickEvent.button.latchedState)  
			 					ball.BeltIn();  
						else  
			 					ball.BeltOff();  
			 			  
			 		if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_BACK))  
			 			if (joyStickEvent.button.latchedState)  
			 				ball.BeltOut();  
			 			else    
			 				ball.BeltOff();  
			 		if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TRIGGER))
			 				ball.StartShoot(false);
	    }
	    
	    public void ButtonUp(JoyStickEvent joyStickEvent) 
	    {
	    	//Util.consoleLog("%s", joyStickEvent.button.id.name());
	    }
	}
}
