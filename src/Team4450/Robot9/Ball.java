package Team4450.Robot9;

import Team4450.Lib.*;
import Team4450.Lib.JoyStick.JoyStickButtonIDs;
import Team4450.Lib.LaunchPad.LaunchPadControlIDs;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Counter;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
public class Ball {

	
		// pickup piston is on port 6
		//CANtalon=7, PickupBelts
		private final Robot	robot;
		private final FestoDA AnglePiston = new FestoDA(0,4);  
		private final FestoDA PickupPiston = new FestoDA (0,6);
		private final Talon ShooterMotor1 = new Talon(1);
		private final Talon ShooterMotor2 = new Talon(0);
		private final SpeedController 		PickupMotor;
		private final DigitalInput BallLimit = new DigitalInput(0);
		private final PIDController		shooterPidController;
		public static double			SHOOTER_LOW_POWER = .70;
		public ShooterSpeedController	shooterMotorControl = new ShooterSpeedController();
		public ShooterSpeedSource		shooterSpeedSource = new ShooterSpeedSource();
		public final Teleop	teleop;
		public double shooterPower=1.0;
		Thread				autoPickupThread;
		Thread autoShootThread;
		Thread ShootThread;
		public Encoder					encoder = new Encoder(4, 5, true, EncodingType.k4X); 
		Ball(Robot robot, Teleop teleop) 
		{
			this.robot=robot;
			this.teleop=teleop;
			// This is distance per pulse and our distance is 1 revolution since we want to measure
			// rpm. We determined there are 1024 pulses in a rev so 1/1024 = .000976 rev per pulse.
			encoder.setDistancePerPulse(.000976);
			
			// Tells encoder to supply the rate as the input to any PID controller source.
			encoder.setPIDSourceType(PIDSourceType.kRate);

			shooterPidController = new PIDController(0.0, 0.0, 0.0, shooterSpeedSource, shooterMotorControl);
				if (robot.robotProperties.getProperty("RobotId").equals("comp")) 
				{
					Util.consoleLog();
					PickupMotor = new CANTalon(7);
					robot.InitializeCANTalon((CANTalon) PickupMotor);
				}
				else
					PickupMotor = new Talon(7);
			
			
			
		}
		
		void dispose()
		{
			Util.consoleLog();
			if (AnglePiston != null) AnglePiston.dispose();
			if (PickupPiston != null)  PickupPiston.dispose();
			if (ShooterMotor1 != null) ShooterMotor1.free();
			if (ShooterMotor2 != null) ShooterMotor2.free();
			if (PickupMotor != null)  
			{
				if (robot.robotProperties.getProperty("RobotId").equals("comp"))
					((CANTalon)PickupMotor).delete();
				else
					((Talon)PickupMotor).free();
			}
			if (BallLimit != null)  BallLimit.free();
			if (encoder != null) encoder.free(); 
			// this is so nothing has issue
		}
		
		
		void AngleUp() {
			Util.consoleLog();
			AnglePiston.SetA();

			
		}
		void AngleDown() {
			Util.consoleLog();
			AnglePiston.SetB ();
		
		}
		void Fire() {
			Util.consoleLog();
			ShooterMotor1.set(shooterPower);
			ShooterMotor2.set(shooterPower);
			SmartDashboard.putBoolean("ShooterMotor", true);
			}
		void Reload() {
			Util.consoleLog();
			ShooterMotor1.set(0);
			ShooterMotor2.set(0);
			if (teleop != null) teleop.rightStick.FindButton(JoyStickButtonIDs.TRIGGER).latchedState = false;
			SmartDashboard.putBoolean("ShooterMotor", false);
		}
		void PickupDown() {
			Util.consoleLog();
			PickupPiston.SetB();
		}
		void PickupUp() {
			Util.consoleLog();
			PickupPiston.SetA();
		}
		void BeltIn() {
			Util.consoleLog();
			PickupMotor.set(1);
			SmartDashboard.putBoolean("PickupMotor", true);
		}
		void BeltOut() {
			Util.consoleLog();
			PickupMotor.set(-1);
			SmartDashboard.putBoolean("PickupMotor", true);
		}
		void BeltOff() {
			Util.consoleLog();
			PickupMotor.set(0);
			SmartDashboard.putBoolean("PickupMotor", false);
		}
		void StartAutoPickup() {
			Util.consoleLog();
			if (autoPickupThread != null) return;
			autoPickupThread = new AutoPickup();
			autoPickupThread.start();
		}
		void StopAutoPickup() {
			Util.consoleLog();
			if (autoPickupThread != null) autoPickupThread.interrupt();
			autoPickupThread = null;
					
		}
		void StopAutoShoot() {
			Util.consoleLog();
			if (autoShootThread != null) autoShootThread.interrupt();
			autoShootThread = null;
		}
		void StopShoot()
		{
			Util.consoleLog();
			if (ShootThread !=null) ShootThread.interrupt();
			ShootThread = null;
		}
		void ShooterPowerUp()
		{
			Util.consoleLog();
			shooterPower=1.0;
			SmartDashboard.putBoolean("ShooterLowPower", false); 
		}
		
		void ShooterPowerDown()
		{
			Util.consoleLog();
			shooterPower=0.65;
			SmartDashboard.putBoolean("ShooterLowPower", true); 
		}
		public void ShooterMotorStart(double speed)
		{
			Util.consoleLog("%f", speed);
		
			if (speed == SHOOTER_LOW_POWER)
			{
				// When shooting a low power, we will attempt to maintain a constant wheel speed (rpm)
				// using pid controller measuring rpm via the encoder. RPM determined experimentally.
				// This call starts the pid controller and turns shooter motor control over to it.
				// The pid will run the motors on its own until disabled.
				holdShooterRPM(5000);
			}
			else
			{
				shooterMotorControl.set(speed);
				
//				shooterMotor1.set(speed);
//				shooterMotor2.set(speed);
			}
			
			SmartDashboard.putBoolean("ShooterMotor", true);
		}
		//----------------------------------------
		public void ShooterMotorStop()
		{
			Util.consoleLog();
			
			shooterPidController.disable();

			shooterMotorControl.set(0);
			
			//shooterMotor1.set(0);
			//shooterMotor2.set(0);
			
			if (teleop != null) teleop.rightStick.FindButton(JoyStickButtonIDs.TOP_LEFT).latchedState = false;
			SmartDashboard.putBoolean("ShooterMotor", false);
		}
		//----------------------------------------

		
	private class AutoPickup extends Thread {
		AutoPickup()
		{
			Util.consoleLog();
			this.setName("AutoPickup");
		}
	public void run() {
		Util.consoleLog();
		try {
			Util.consoleLog();
			PickupDown();
			BeltIn();
		while(!isInterrupted() && !BallLimit.get())
		{
			sleep(50);
		} 
		}
		
		catch (InterruptedException e) {} 
		catch (Throwable e) {e.printStackTrace(Util.logPrintStream);} 
		BeltOff();
			PickupUp();
			autoPickupThread = null;
					
	}
	}
public void StartAutoShoot(boolean angleUp) {
	Util.consoleLog();
	if(angleUp)
		AngleUp();
	else
		AngleDown();
	Util.consoleLog();
	if (autoShootThread != null) return;
	autoShootThread = new AutoShoot();
	autoShootThread.start();

}
private class AutoShoot extends Thread {
	AutoShoot(){
		this.setName("AutoShoot");
		Util.consoleLog();
	}
	public void run()
	{
		try {
			Util.consoleLog();
			Fire();
			sleep(6000);
			BeltIn();
			teleop.ArmDown();
			sleep(500);
			teleop.ArmUp();
			sleep(500);
			//teleop.lightOff();
		} 
		catch (InterruptedException e) {} 
		  catch (Throwable e) {e.printStackTrace(Util.logPrintStream);} 
		Util.consoleLog();
		BeltOff();
		Reload();
		StopAutoShoot();
	}
}
	public void StartShoot(boolean angleUp) {
		Util.consoleLog();
		if(angleUp)
			AngleUp();
		else
			AngleDown();
		Util.consoleLog();
		if (ShootThread != null) return;
		ShootThread = new Shoot();
		ShootThread.start();

	}
	private class Shoot extends Thread {
		Shoot()
		{
			Util.consoleLog();
			this.setName("Shoot");
		}
			public void run()
			{
			Util.consoleLog();
			try {
				Util.consoleLog();
				//Fire();
				//sleep(3000);
				BeltIn();
				if (teleop != null)  
						    		{  
						    			if (teleop.launchPad.FindButton(LaunchPadControlIDs.ROCKER_RIGHT).latchedState)	teleop.ArmDown();  
						    		}  

				if (teleop != null)
	    		{
	    			if (teleop.launchPad.FindButton(LaunchPadControlIDs.ROCKER_RIGHT).latchedState)	
	    			{
	    	    		sleep(500);
	    				teleop.ArmUp();
	    	    		sleep(500);
	    			}
	    			else
	    				sleep(1000);
	    		}
	    		else
	    			sleep(1000);
				sleep(500);
			//	teleop.lightOff();
				BeltOff();
			}
			
			catch (InterruptedException e) {} 
			  catch (Throwable e) {e.printStackTrace(Util.logPrintStream);} 
			BeltOff();
			Reload();
			
		
		}
			// Automatically hold shooter motor speed (rpm). Starts PID controller to
			// manage motor power to maintain rpm target.
			void holdShooterRPM(double rpm)
			{
				Util.consoleLog("%.0f", rpm);
				
				// p,i,d values are a guess.
				// f value is the base motor speed, which is where (power) we start.
				// setpoint is target rpm converted to rev/sec.
				// The idea is that we apply power to get rpm up to set point and then maintain.
				shooterPidController.setPID(0.001, 0.001, 0.0, 0.0); 
				shooterPidController.setSetpoint(rpm / 60);
				shooterPidController.setPercentTolerance(1);	// 5% error.
				//encoder.reset();
				shooterSpeedSource.reset();
				shooterPidController.enable();
			}
			
			public class shooterSpeedController implements SpeedController
			{
				private boolean	inverted, disabled;
			
				@Override
				public void pidWrite(double output)
				{
					this.set(output);
				}

				@Override
				public double get()
				{
					return ShooterMotor1.get();
				}

				@Override
				public void set(double speed, byte syncGroup)
				{
					this.set(speed);
				}

				@Override
				public void set(double speed)
				{
					if (!disabled)
					{
		    			ShooterMotor1.set(speed);
		    			ShooterMotor2.set(speed);
					}
				}

				@Override
				public void setInverted(boolean isInverted)
				{
					inverted = isInverted;
				}

				@Override
				public boolean getInverted()
				{
					return inverted;
				}

				public void enable()
				{
					disabled = false;
				}
				
				@Override
				public void disable()
				{
					disabled = true;
				}

				@Override
				public void stopMotor()
				{
					this.set(0);
				}
			}
			
			// Encapsulate the encoder so we could modify the rate returned to
			// the PID controller.
			private class shooterSpeedSource implements PIDSource
			{
				@Override
				public void setPIDSourceType(PIDSourceType pidSource)
				{
					encoder.setPIDSourceType(pidSource);
				}

				@Override
				public PIDSourceType getPIDSourceType()
				{
					return encoder.getPIDSourceType();
				}

				@Override
				public double pidGet()
				{
					// TODO: Some sort of smoothing could be done to damp out the
					// fluctuations in encoder rate (rpm).
					return encoder.getRate();
				}
				
				public void reset()
				{
					encoder.reset();
				}
			}
		}
		
}





	
	
		
	


