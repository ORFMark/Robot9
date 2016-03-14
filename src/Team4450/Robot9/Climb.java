package Team4450.Robot9;

import Team4450.Lib.*;
import edu.wpi.first.wpilibj.*;

public class Climb 
{
	private final Robot robot;
	private final FestoDA ClimbAnglePiston = new FestoDA(1,0);
	private final FestoDA ArmPiston = new FestoDA (1, 2);
	Thread	AutoClimbThread;
	public final Teleop teleop;
	
	Climb(Robot robot, Teleop teleop)
	{
		Util.consoleLog();
		this.robot=robot;
		this.teleop=teleop;
	}
	void dispose() {
		Util.consoleLog();
		if (ClimbAnglePiston != null) ClimbAnglePiston.dispose();
	}
	void ClimbUp()
	{
		Util.consoleLog();
		ClimbAnglePiston.SetA();
	}
	void ClimbDown()
	{
		Util.consoleLog();
		ClimbAnglePiston.SetB();
	}
	void armsOut()
	{
		Util.consoleLog();
		ArmPiston.SetB();
	}
	void armsIn()
	{
		Util.consoleLog();
		ArmPiston.SetA();
	}
	void StartAutoClimb()
	{
		Util.consoleLog();
		if (AutoClimbThread != null) return;
		AutoClimbThread = new AutoClimb();
		AutoClimbThread.start();
	}
	void StopAutoClimb()
	{
		Util.consoleLog();
		if (AutoClimbThread != null) AutoClimbThread.interrupt();
		AutoClimbThread = null;
	}
	
	
	private class AutoClimb extends Thread
	{
		AutoClimb()
		{
			Util.consoleLog();
			this.setName("AutoClimb");
		}
		public void run() {
			Util.consoleLog();
			try {
				Util.consoleLog();
				ClimbDown();
				sleep(50);
				armsOut();
				robot.robotDrive.tankDrive(-.64, -.60);
			while (!isInterrupted() && !teleop.ClimbLimitUp.get())
				sleep(50);
			}
			catch (InterruptedException e) {} 
			catch (Throwable e) {e.printStackTrace(Util.logPrintStream);} 
			robot.robotDrive.tankDrive(0, 0);
			AutoClimbThread=null;
		}
}
}



	
