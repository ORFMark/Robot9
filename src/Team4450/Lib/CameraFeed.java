
package Team4450.Lib;

import org.opencv.core.Mat;

import edu.wpi.cscore.CvSource;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Timer;

/**
 * USB camera feed task. Runs as a thread separate from Robot class.
 * Manages one or more usb cameras feeding their images to the 
 * CameraServer class to send to the DS.
 * We create one or more usb of our camera objects and start them capturing
 * images. We then loop on a thread getting the current image from
 * the currently selected camera and pass the image to the camera
 * server which passes the image to the driver station.
 */

public class CameraFeed extends Thread
{
	public	double				frameRate = 30;		// frames per second
	private Camera				currentCamera, cam1, cam2;
	private Mat 				image;
	private CameraServer 		server;
	private static CameraFeed	cameraFeed;
	private boolean				isCompetitionRobot;
	private CvSource			imageOutputStream;
	
	// Create single instance of this class and return that single instance to any callers.
	
	/**
	 * Get a reference to global CameraFeed2 object.
	 * @return Reference to global CameraFeed2 object.
	 */
	  
	public static CameraFeed getInstance(boolean isCompetitionRobot) 
	{
		Util.consoleLog();
		
		if (cameraFeed == null) cameraFeed = new CameraFeed(isCompetitionRobot);
	    
	    return cameraFeed;
	}

	// Private constructor means callers must use getInstance.
	// This is the singleton class model.
	
	private CameraFeed(boolean isCompetitionRobot)
	{
		try
		{
    		Util.consoleLog();
    
    		this.setName("CameraFeed2");
    		
    		this.isCompetitionRobot = isCompetitionRobot;
    		
            // camera Server that we'll give the images to.
            server = CameraServer.getInstance();

            // Create output image stream.
            
            imageOutputStream = server.putVideo("4450", 640, 480);
            
            // Create cameras.
            // Using one camera at this time.

            if (isCompetitionRobot)
    			cam1 = new Camera("cam1", 1, server);
    		else
    			cam1 = new Camera("cam0", 0, server);
    			
            cam2 = cam1;
            
            // Open cameras when using 2 cameras.
            
//            if (isCompetitionRobot)
//            {
//    			cam1 = new UsbCamera("cam1", 1, server);
//    			cam2 = new UsbCamera("cam0", 0, server);
//            }
//            else
//            {
//            	cam1 = new UsbCamera("cam0", 0, server);
//    			cam2 = new UsbCamera("cam1", 1, server);
//            }
            
            // Set starting camera.

            currentCamera = cam1;
		}
		catch (Throwable e) {Util.logException(e);}
	}
	
	// Run thread to read and feed camera images. Called by Thread.start().
	public void run()
	{
		Util.consoleLog();
		
		try
		{
			Util.consoleLog();

			while (!isInterrupted())
			{
				UpdateCameraImage();
		
				Timer.delay(1 / frameRate);
			}
		}
		catch (Throwable e) {Util.logException(e);}
	}
	
	/**
	 * Get last image read from camera.
	 * @return Mat Last image from camera.
	 */
	public Mat CurrentImage()
	{
		Util.consoleLog();
		
		return image;
	}
	
	/**
	 * Stop image feed, ie close camera stream stop feed thread.
	 */
	public void EndFeed()
	{
		try
		{
    		Util.consoleLog();

    		Thread.currentThread().interrupt();
    		
    		cam1.stopCapture();
    		cam1.free();
    		//cam2.stopCapture();
    		//cam2.free();
    		
    		currentCamera = cam1 = cam2 =  null;
    		server = null;
		}
		catch (Throwable e)	{Util.logException(e);}
	}
	
	/**
	 * Change the camera to get images from the other camera. 
	 */
	public void ChangeCamera()
    {
		Util.consoleLog();

		currentCamera.stopCapture();
		
		if (currentCamera.equals(cam1))
			currentCamera = cam2;
		else
			currentCamera = cam1;
		
		currentCamera.startCapture();
    }
    
	// Get an image from current camera and give it to the server.
    private void UpdateCameraImage()
    {
    	try
    	{
    		if (currentCamera != null)
    		{	
    			image = currentCamera.getImage();
    				
            	imageOutputStream.putFrame(image);
    		}
		}
		catch (Throwable e) {Util.logException(e);}
    }
}
