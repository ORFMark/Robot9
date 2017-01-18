
package Team4450.Robot9;

import java.lang.Math;

import Team4450.Lib.*;
import Team4450.Lib.JoyStick.*;
import Team4450.Lib.LaunchPad.*;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.Relay;

class Teleop
{
	private final Robot 		robot;
	public  JoyStick			rightStick, leftStick, utilityStick;
	public  LaunchPad			launchPad;
	private final FestoDA		shifterValve = new FestoDA(2);
	private final FestoDA		ptoValve = new FestoDA(0);
	private final FestoDA		tiltValve = new FestoDA(1, 0);
	private final FestoDA		climberArmsValve = new FestoDA(1, 2);
	private final FestoDA		defenseArmsValve = new FestoDA(1, 4);
	private boolean				ptoMode = false, limitSwitchEnabled = false;
	private boolean				autoTarget = false, climbPrepEnabled = false, climbPrepInProgress = false;
	private final Shooter		shooter;
	private double				shooterPower;
	private Relay				headLight = new Relay(0, Relay.Direction.kForward);
	//private final RevDigitBoard	revBoard = RevDigitBoard.getInstance();
	//private final DigitalInput	hallEffectSensorDigital = new DigitalInput(9);
	//private final AnalogInput	hallEffectSensorAnalog = new AnalogInput(3);
	private final DigitalInput	climbUpSwitch = new DigitalInput(3);

	// Wheel encoder is plugged into dio port 1 - orange=+5v blue=signal, dio port 2 black=gnd yellow=signal. 
	private Encoder				encoder = new Encoder(1, 2, true, EncodingType.k4X);

	// Encoder ribbon cable to dio ports: ribbon wire 2 = orange, 5 = yellow, 7 = blue, 10 = black

	// Constructor.
	
	Teleop(Robot robot)
	{
		Util.consoleLog();

		this.robot = robot;
		
		shooter = new Shooter(robot, this);
		
		shooterPower = shooter.SHOOTER_HIGH_POWER;
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
		if (tiltValve != null) tiltValve.dispose();
		if (climberArmsValve != null) climberArmsValve.dispose();
		if (defenseArmsValve != null) defenseArmsValve.dispose();
		if (shooter != null) shooter.dispose();
		//if (armMotor != null) armMotor.free();
		if (climbUpSwitch != null) climbUpSwitch.free();
		if (encoder != null) encoder.free();
		if (headLight != null) headLight.free();
		//if (revBoard != null) revBoard.dispose();
		//if (hallEffectSensor != null) hallEffectSensor.free();
	}

	void OperatorControl()
	{
		double	rightY, leftY, utilX;
        
        // Motor safety turned off during initialization.
        robot.robotDrive.setSafetyEnabled(false);

		Util.consoleLog();
		
		LCD.printLine(1, "Mode: OperatorControl");
		LCD.printLine(2, "All=%s, Start=%d, FMS=%b", robot.alliance.name(), robot.location, robot.ds.isFMSAttached());
		
		// Initial setting of air valves.

		shifterLow();
		ptoDisable();
		tiltUp();
		climberArmsUp();
		defenseArmsUp();
		shooter.HoodDown();
		
		// Configure LaunchPad and Joystick event handlers.
		
		launchPad = new LaunchPad(robot.launchPad, LaunchPadControlIDs.BUTTON_BLACK, this);
		
		LaunchPadControl lpControl = launchPad.AddControl(LaunchPadControlIDs.ROCKER_LEFT_FRONT);
		lpControl.controlType = LaunchPadControlTypes.SWITCH;
		
		LaunchPadControl lpRLBControl = launchPad.AddControl(LaunchPadControlIDs.ROCKER_LEFT_BACK);
		lpRLBControl.controlType = LaunchPadControlTypes.SWITCH;

		lpControl = launchPad.AddControl(LaunchPadControlIDs.ROCKER_RIGHT);
		lpControl.controlType = LaunchPadControlTypes.SWITCH;
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_YELLOW);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_BLUE);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_GREEN);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_RED);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_RED_RIGHT);
		launchPad.AddControl(LaunchPadControlIDs.BUTTON_BLUE_RIGHT);
        launchPad.addLaunchPadEventListener(new LaunchPadListener());
        launchPad.Start();

		leftStick = new JoyStick(robot.leftStick, "LeftStick", JoyStickButtonIDs.TRIGGER, this);
        leftStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		leftStick.addJoyStickEventListener(new LeftStickListener());
        leftStick.Start();
        
		rightStick = new JoyStick(robot.rightStick, "RightStick", JoyStickButtonIDs.TOP_LEFT, this);
        rightStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		rightStick.AddButton(JoyStickButtonIDs.TRIGGER);
        rightStick.addJoyStickEventListener(new RightStickListener());
        rightStick.Start();
        
		utilityStick = new JoyStick(robot.utilityStick, "UtilityStick", JoyStickButtonIDs.TOP_LEFT, this);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_RIGHT);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_MIDDLE);
		utilityStick.AddButton(JoyStickButtonIDs.TOP_BACK);
		utilityStick.AddButton(JoyStickButtonIDs.TRIGGER);
        utilityStick.addJoyStickEventListener(new UtilityStickListener());
        utilityStick.Start();
        
        // Tighten up dead zone for smoother turrent movement.
        utilityStick.deadZone = .05;

		// Set CAN Talon brake mode by rocker switch setting.
        // We do this here so that the Utility stick thread has time to read the initial state
        // of the rocker switch.
        if (robot.isComp) robot.SetCANTalonBrakeMode(lpRLBControl.latchedState);
        
        // Set gyro to heading 0.
        robot.gyro.reset();

        // Motor safety turned on.
        robot.robotDrive.setSafetyEnabled(true);
        
		// Driving loop runs until teleop is over.

		while (robot.isEnabled() && robot.isOperatorControl())
		{
			// Get joystick deflection and feed to robot drive object
			// using calls to our JoyStick class.

			if (ptoMode)
			{
				rightY = utilityStick.GetY();
				
				if (rightY > 0 && climbUpSwitch.get() && limitSwitchEnabled) rightY = 0;

				leftY = rightY;
			} 
//			else if (invertDrive)
//			{
//    			rightY = rightStick.GetY() * -1.0;		// fwd/back right
//    			leftY = leftStick.GetY() * -1.0;		// fwd/back left
//			}
			else
			{
//				rightY = rightStick.GetY();				// fwd/back right
//    			leftY = leftStick.GetY();				// fwd/back left
				rightY = stickLogCorrection(rightStick.GetY());	// fwd/back right
    			leftY = stickLogCorrection(leftStick.GetY());	// fwd/back left
			}
			
			utilX = utilityStick.GetX();
			
			LCD.printLine(3, "encoder=%d  climbUp=%b", encoder.get(), climbUpSwitch.get());
			LCD.printLine(4, "leftY=%.4f  rightY=%.4f utilX=%.4f", leftY, rightY, utilX);
			LCD.printLine(5, "gyroAngle=%d, gyroRate=%d", (int) robot.gyro.getAngle(), (int) robot.gyro.getRate());
			// encoder rate is revolutions per second.
			LCD.printLine(6, "encoder=%d rpm=%.0f pwr=%.2f pwrR=%.2f", shooter.shooterSpeedSource.get(), 
							shooter.shooterSpeedSource.getRate() * 60, shooter.shooterMotorControl.get(),
							shooter.shooterMotorControl.get());
			LCD.printLine(7, "turretEncoder=%d", shooter.turretEncoder.get());
			
			//LCD.printLine(7, "shooterspeedsource=%.0f", shooter.shooterSpeedSource.pidGet());
			//LCD.printLine(7, "hall effect=%b", hallEffectSensorDigital.get());
			//LCD.printLine(7, "hall effect=%f", hallEffectSensorAnalog.getVoltage());
			// This corrects stick alignment error when trying to drive straight. 
			//if (Math.abs(rightY - leftY) < 0.2) rightY = leftY;
			
			// Set wheel motors.
			// Do not feed JS input to robotDrive if we are controlling the motors in automatic functions.

			if (!autoTarget && !climbPrepInProgress) robot.robotDrive.tankDrive(leftY, rightY);

			// Rotate turret as directed by utility stick left/right deflection.
			
			if (!autoTarget) shooter.rotateTurret(utilX);
			
			// End of driving loop.
			
			Timer.delay(.020);	// wait 20ms for update from driver station.
		}
		
		// End of teleop mode.
		
		Util.consoleLog("end");
	}

	// Map joystick y value of 0.0-1.0 to the motor working power range of approx 0.5-1.0
	
	private double stickCorrection(double joystickValue)
	{
		if (joystickValue != 0)
		{
			if (joystickValue > 0)
				joystickValue = joystickValue / 1.5 + .4;
			else
				joystickValue = joystickValue / 1.5 - .4;
		}
		
		return joystickValue;
	}
	
	// Custom base logrithim.
	// Returns logrithim base of the value.
	
	private double baseLog(double base, double value)
	{
		return Math.log(value) / Math.log(base);
	}

	// Map joystick y value of 0.0 to 1.0 to the motor working power range of approx 0.5 to 1.0 using
	// logrithmic curve.
	
	private double stickLogCorrection(double joystickValue)
	{
		double base = Math.pow(2, 1/3) + Math.pow(2, 1/3);
		
		if (joystickValue > 0)
			joystickValue = baseLog(base, joystickValue + 1);
		else if (joystickValue < 0)
			joystickValue = -baseLog(base, -joystickValue + 1);
			
		return joystickValue;
	}
	
	// Transmission control functions.
	
	//--------------------------------------
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
	
	//--------------------------------------
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
	
	// Misc control functions.
	
	//--------------------------------------
	void tiltUp()
	{
		Util.consoleLog();
		
		tiltValve.SetA();
	}

	void tiltDown()
	{
		Util.consoleLog();
		
		tiltValve.SetB();
	}
	
	//--------------------------------------
	void climberArmsUp()
	{
		Util.consoleLog();
		
		climberArmsValve.SetB();
	}

	void climberArmsDown()
	{
		Util.consoleLog();
		
		climberArmsValve.SetA();
	}
	
	//--------------------------------------
	void defenseArmsUp()
	{
		Util.consoleLog();
		
		defenseArmsValve.SetB();
	}

	void defenseArmsDown()
	{
		Util.consoleLog();
		
		defenseArmsValve.SetA();
	}

	//--------------------------------------
	// Automatically prepare for climb. Drive on batter press black button.
	// We deploy kicker, shift to PTO, disable joystick control of motors,
	// jog motors backwards a bit to facilitate shifting to PTO. Then run
	// climber arms up to near max height. Reenable joystick control of motors.
	void climbPrep()
	{
		Util.consoleLog();
		
		tiltDown();		// deploy foot to hold position.
		
		launchPad.FindButton(LaunchPadControlIDs.BUTTON_GREEN).latchedState = true;	// reset button state for tilt.

		climbPrepInProgress = true;
		
		ptoEnable();	// shift to pto mode.
		
		launchPad.FindButton(LaunchPadControlIDs.BUTTON_BLUE).latchedState = true;	// reset button state for pto.

		robot.robotDrive.setSafetyEnabled(false);

		robot.robotDrive.tankDrive(-.4, -.4);	// jog motors.
		
		Timer.delay(.3);

		robot.robotDrive.tankDrive(.8, .8);		// raise arms.
		
		Timer.delay(.6);
		
		robot.robotDrive.tankDrive(0, 0);		// stop arms.

		robot.robotDrive.setSafetyEnabled(true);

		climbPrepInProgress = false;
	}
	
	//--------------------------------------
//	void lightOn()
//	{
//		headLight.set(Relay.Value.kOn);
//		SmartDashboard.putBoolean("Light", true);
//	}
//	
//	void lightOff()
//	{
//		headLight.set(Relay.Value.kOff);
//		rightStick.FindButton(JoyStickButtonIDs.TRIGGER).latchedState = false;
//		SmartDashboard.putBoolean("Light", false);
//	}
	
	/**
	 * Rotate the robot by bumping appropriate motors based on the X axis offset
	 * from center of camera image. 
	 * @param value Target offset from center. + value means target is left of center so
	 * run right side motors forward. - value means target is right of center so run
	 * left side motors forward. Currently the magnitude of the offset is not used but
	 * could be if we upgrade the movement function like using PID or ?
	 */
	void bump(int value)
	{
		Util.consoleLog("%d", value);

		if (value > 0)
			robot.robotDrive.tankDrive(.60, -.60);	// + turn left. motor - = forward.
		else
			robot.robotDrive.tankDrive(-.60, .60);	// - turn right.
			
		// larger bump the further off target we are.
		
		if (Math.abs(value) < 100)
			Timer.delay(.1);
		else
			Timer.delay(.25);
		
		robot.robotDrive.tankDrive(0, 0);
	}
	
	/**
	 * Rotate the robot by bumping turret motor based on the X axis offset
	 * from center of camera image. Iteratively finds center of target. 
	 * @param value Target offset from center. + value means target is left of center so
	 * turn turret left. - value means target is right of center so turn turret
	 * right. Currently the magnitude of the offset is used to control how long we run
	 * the motors for a bump.  Motors are run at a fixed power.
	 */
	void bumpTurret(int value)
	{
		Util.consoleLog("%d", value);

		if (value > 0)
			shooter.rotateTurret(-.15);	// +value turn left (minus on rotate).
		else
			shooter.rotateTurret(.15);	// -value turn right (plus on rotate).
			
		// longer bump the further off target we are.
		
		if (Math.abs(value) < 100)
			Timer.delay(.05);
		else
			Timer.delay(.10);
		
		shooter.rotateTurret(0);
	}
	
	/**
	 * Rotate the robot by setting turret motor position based on the X axis offset
	 * from center of camera image. Position is relative to turret current position.
	 * When tuned correctly, should line up on target with or two turrent moves.
	 * @param value Target offset from center. + value means target is left of center so
	 * turn turret left. - value means target is right of center so turn turret
	 * right.
	 */
	void bumpTurret2(int value)
	{
		Util.consoleLog("%d", value);

		value = shooter.pixelsToCounts(value, 0);
		
		// Invert value since setpositionrelative uses opposite sign for direction.
		
		shooter.turretSetPositionRelative(-value);
	}	

	/**
	 * Loops checking camera images for target. Stops when no target found.
	 * If target found, check target X location and if needed bump the bot
	 * in the appropriate direction and then check target location again.
	 * Uses GRIP based vision code running as a standalone program either
	 * on the RoboRio or Raspberry Pi or a PC.
	 */
	void seekTargetGrip()
	{
		int				targetOffset, imageCenter = Grip.IMAGE_WIDTH / 2;
		Grip.Contour	contour;

		// Since the turret camera is incorrectly mounted and points a bit left of 
		// turrent centerline, when we line up the target with the camera centerline
		// the turret is actually pointing right of the target. So we use this value
		// to adjust the turret position to correct this problem. Note that this will
		// make the DS camera centerline to be left of center when we are lined up.
		// This is number of pixels to tweak the location of the target centerline.
		// + value adjusts turret right, - value adjusts left.
		final int		CAMERA_ADJUST = 0;
		
		Util.consoleLog();
		
		autoTarget = true;

		//Grip.suspendGrip(false);	// Only needed when Grip on RoboRio.
		
		SmartDashboard.putBoolean("TargetLocked", false);
		SmartDashboard.putBoolean("AutoTarget", autoTarget);

		contour = Grip.getContoursReport().getContour(0);
		
		robot.robotDrive.setSafetyEnabled(false);
		
		while (robot.isEnabled() && autoTarget && contour != null)
		{
			Util.consoleLog(contour.toString());

			// Image is Grip.IMAGE_WIDTH pixels wide. 5px target zone.
			// targetOffset is number of pixels from center of image to the center
			// of the target in the image. A countour is information about the location
			// of the target in the image. centerX is target center offset from the left
			// edge of the image, ie. zero.
			
			targetOffset = imageCenter - ((int) contour.centerX + CAMERA_ADJUST);
					
			if (Math.abs(targetOffset) > 5)
			{
				// targetOffset will be + if target left of center, - if right of center.

				bumpTurret2(targetOffset);
				
				// Wait for Grip to process an image after bump movement stops.
				// Grip takes about .50-.75sec to process an image and return data.
				// Grip development environment will tell you how much time it takes
				// to process an image on the development PC.
				
				Timer.delay(.50);	
				
				contour = Grip.getContoursReport().getContour(0);
			}
			else
			{
				SmartDashboard.putBoolean("TargetLocked", true);
				contour = null;
			}
		}
		
		//Grip.suspendGrip(true);	// Only needed when Grip on the RoboRio.
		autoTarget = false;
		robot.robotDrive.setSafetyEnabled(true);
		SmartDashboard.putBoolean("AutoTarget", autoTarget);
	}
	
	// Handle LaunchPad control events.
	
	public class LaunchPadListener implements LaunchPadEventListener 
	{
	    public void ButtonDown(LaunchPadEvent launchPadEvent) 
	    {
	    	LaunchPadControl	control = launchPadEvent.control;
	    	
			Util.consoleLog("%s, latchedState=%b", control.id.name(),  control.latchedState);
			
			switch(control.id)
			{
				case BUTTON_YELLOW:
					robot.cameraThread.ChangeCamera();
//    				if (launchPadEvent.control.latchedState)
//    					shooter.HoodUp();
//    				else
//    					shooter.HoodDown();

    				break;
    				
				case BUTTON_BLACK:
    				if (climbPrepEnabled)
    					climbPrep();
    				else
    				{
        				if (launchPadEvent.control.latchedState)
        					shooter.PickupArmDown();
        				else
        					shooter.PickupArmUp();
    				}
    				
    				break;
			
//				case BUTTON_BLUE:
//    				if (launchPadEvent.control.latchedState)
//        				shifterHigh();
//        			else
//        				shifterLow();
//    				
//    				break;

				case BUTTON_GREEN:
    				if (launchPadEvent.control.latchedState)
        				tiltDown();
        			else
        				tiltUp();

    				break;
    				
				case BUTTON_BLUE:
    				if (launchPadEvent.control.latchedState)
    				{
    					shifterLow();
    					ptoEnable();
    				}
        			else
        				ptoDisable();

    				break;
    				
				case BUTTON_BLUE_RIGHT:
    				// Start auto targeting on button push, stop on next button push.
    				if (!autoTarget)
    					seekTargetGrip();
    				else
    					autoTarget = false;

    				break;
    				
				case BUTTON_RED_RIGHT:
    				if (launchPadEvent.control.latchedState)
    					shooter.StartAutoBallSpit();
    				else
    					shooter.StopAutoBallSpit();

    				break;
    				
				case BUTTON_RED:
    				if (launchPadEvent.control.latchedState)
        				defenseArmsDown();
        			else
        				defenseArmsUp();

    				break;
    				
				default:
					break;
			}
	    }
	    
	    public void ButtonUp(LaunchPadEvent launchPadEvent) 
	    {
	    	//Util.consoleLog("%s, latchedState=%b", launchPadEvent.control.name(),  launchPadEvent.control.latchedState);
	    }

	    public void SwitchChange(LaunchPadEvent launchPadEvent) 
	    {
	    	LaunchPadControl	control = launchPadEvent.control;
	    	
	    	Util.consoleLog("%s", control.id.name());

	    	switch(control.id)
	    	{
	    		// Turn PID on/off.
	    		case ROCKER_LEFT_FRONT:
    				if (control.latchedState)
    					SmartDashboard.putBoolean("PIDEnabled", true);
    				else
    					SmartDashboard.putBoolean("PIDEnabled", false);
    				
    				break;
			
				// Set CAN Talon brake mmode.
	    		case ROCKER_LEFT_BACK:
    				if (control.latchedState)
    					robot.SetCANTalonBrakeMode(false);	// coast
    				else
    	    			robot.SetCANTalonBrakeMode(true);	// brake
    				
    				break;
	
				// Set shooter power low/high.
	    		case ROCKER_RIGHT:
    				if (control.latchedState)
    				{
    					shooterPower = shooter.SHOOTER_LOW_POWER;
    					SmartDashboard.putBoolean("ShooterLowPower", true);
    				}
    				else
    				{
    					shooterPower = shooter.SHOOTER_HIGH_POWER;
    					SmartDashboard.putBoolean("ShooterLowPower", false);
    				}

    				break;
    				
				default:
					break;
	    	}
	    }
	}

	// Handle Right JoyStick Button events.
	
	private class RightStickListener implements JoyStickEventListener 
	{
		
	    public void ButtonDown(JoyStickEvent joyStickEvent) 
	    {
	    	JoyStickButton	button = joyStickEvent.button;
	    	
			Util.consoleLog("%s, latchedState=%b", button.id.name(),  button.latchedState);
			
			// Change which USB camera is being served by the RoboRio when using dual usb cameras.
			
//			if (joyStickEvent.button.id.equals(JoyStickButtonIDs.TOP_LEFT))
//				if (joyStickEvent.button.latchedState)
//					((CameraFeed) robot.cameraThread).ChangeCamera(((CameraFeed) robot.cameraThread).cam2);
//				else
//					((CameraFeed) robot.cameraThread).ChangeCamera(((CameraFeed) robot.cameraThread).cam1);			

			switch(button.id)
			{
				case TOP_LEFT:
    				if (button.latchedState)
    					robot.cameraThread.ChangeCamera();
    				else
    					robot.cameraThread.ChangeCamera();			
    
    				break;
				
				case TOP_MIDDLE:
					shooter.StopShoot();
					break;
					
				case TRIGGER:
					shooter.turretSetPosition(-2000);
					
				default:
					break;
			}
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
	    	JoyStickButton	button = joyStickEvent.button;
	    	
			Util.consoleLog("%s, latchedState=%b", button.id.name(),  button.latchedState);
			
			switch(button.id)
			{
				case TRIGGER:
					if (button.latchedState)
	    				shifterHigh();
	    			else
	    				shifterLow();

					break;
					
				case TOP_MIDDLE:
					if (button.latchedState)
	    				climberArmsDown();
	    			else
	    				climberArmsUp();

					break;
					
				default:
					break;
			}
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
	    	JoyStickButton	button = joyStickEvent.button;
	    	
			Util.consoleLog("%s, latchedState=%b", button.id.name(),  button.latchedState);
			
			switch(button.id)
			{
				// Trigger starts shoot sequence.
				case TRIGGER:
    				shooter.StartShoot(false, shooterPower);
    				break;
				
				// Start auto pickup sequence.
				case TOP_RIGHT:
    				if (button.latchedState)
    					shooter.StartAutoPickup();
    				else
    					shooter.StopAutoPickup();

    				break;
    				
    			// Manually turn shooter motor on/off.
				case TOP_LEFT:
    				if (button.latchedState)
    					shooter.ShooterMotorStart(shooterPower);
    				else
    					shooter.ShooterMotorStop();

    				break;
    				
    			// Manually turn pickup motor on/off in the IN direction.
				case TOP_MIDDLE:
    				if (button.latchedState)
    					shooter.PickupMotorIn(1.0);
    				else
    					shooter.PickupMotorStop();

    				break;
    				
    			// Manually turn pickup motor on/off in the OUT direction.
				case TOP_BACK:
    				if (button.latchedState)
    					shooter.PickupMotorOut(1.0);
    				else
    					shooter.PickupMotorStop();
    				
    				break;
    				
				default:
					break;
			}
	    }

	    public void ButtonUp(JoyStickEvent joyStickEvent) 
	    {
	    	//Util.consoleLog("%s", joyStickEvent.button.id.name());
	    }
	}
}
