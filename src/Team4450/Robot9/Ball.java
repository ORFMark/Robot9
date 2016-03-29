package Team4450.Robot9;

import Team4450.Lib.*;
import Team4450.Lib.JoyStick.JoyStickButtonIDs;
import Team4450.Lib.LaunchPad.LaunchPadControlIDs;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.smartdashboard.*;
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
		public final Teleop	teleop;
		public double shooterPower=1.0;
		Thread				autoPickupThread;
		Thread autoShootThread;
		Thread ShootThread;
		
		Ball(Robot robot, Teleop teleop) 
		{
			this.robot=robot;
			this.teleop=teleop;
				if (robot.robotProperties.getProperty("RobotId").equals("comp")) 
				{
					Util.consoleLog();
					PickupMotor = new CANTalon(7);
					robot.InitializeCANTalon((CANTalon) PickupMotor);
				}
				else
					PickupMotor = new Talon(7);
		
			
			PickupUp();
			AngleDown();
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
			sleep(5000);
			BeltIn();
			sleep(1000);
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
				sleep(1000);
			//	teleop.lightOff();
				BeltOff();
			}
			
			catch (InterruptedException e) {} 
			  catch (Throwable e) {e.printStackTrace(Util.logPrintStream);} 
			BeltOff();
			
		
		}
}
}




	
	
		
	


