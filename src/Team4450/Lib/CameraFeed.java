
package Team4450.Lib;

import org.opencv.core.Mat;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
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
	private UsbCamera			currentCamera, cam1, cam2;
	private Mat 				image = new Mat();
	private static CameraFeed	cameraFeed;
	private boolean				isCompetitionRobot, initialized;
	private MjpegServer			mjpegServer;
	private CvSink				imageSource;
	private CvSource			imageOutputStream;
	private boolean				changingCamera;

	// Camera settings - Static
	public static final int 	width = 320; //640;
	public static final int 	height = 240; //480;
	//public static final double 	fovH = 48.0;
	//public static final double 	fovV = 32.0;
	public static final	double	frameRate = 20;		// frames per second
	public static final int		whitebalance = 4700;	// Color temperature in K, -1 is auto
	public static final int		brightness = 50;		// 0 - 100, -1 is "do not set"
	public static final int		exposure = 50;		// 0 - 100, -1 is "auto"

	// Create single instance of this class and return that single instance to any callers.
	
	/**
	 * Get a reference to global CameraFeed object.
	 * @return Reference to global CameraFeed object.
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

            // Create Mjpeg stream server.
            
            mjpegServer = CameraServer.getInstance().addServer("4450-mjpegServer", 1181);

            // Create image source.
            
            imageSource = new CvSink("4450-CvSink");
            
            // Create output image stream.
            
            imageOutputStream = new CvSource("4450-CvSource", VideoMode.PixelFormat.kMJPEG, width, height, (int) frameRate);
            
            mjpegServer.setSource(imageOutputStream);
            
            // Create cameras.
            // Using one camera at this time.
            // You have to look at the RoboRio web page to see what
            // number the camera 1s assigned to. The name here is
            // whatever you would like it to be.

//            if (this.isCompetitionRobot)
//    			cam1 = new UsbCamera("cam0", 0);
//    		else
//    			cam1 = new UsbCamera("cam0", 0);
//    			
//            updateCameraSettings(cam1);
//            
//            cam2 = cam1;
            
            // Open cameras when using 2 cameras.
            // You have to look at the RoboRio web page to see what
            // numbers the cameras are assigned to. The name here is
            // whatever you would like it to be.
            
            if (this.isCompetitionRobot)
            {
    			cam1 = new UsbCamera("cam0", 0);
    			cam2 = new UsbCamera("cam1", 1);
            }
            else
            {
            	cam1 = new UsbCamera("cam0", 0);
    			cam2 = new UsbCamera("cam1", 1);
            }

            updateCameraSettings(cam1);
            updateCameraSettings(cam2);
            
            initialized = true;
            
            // Set starting camera.

            ChangeCamera();
		}
		catch (Throwable e) {Util.logException(e);}
	}
	
	/**
	 * Update camera with current settings fields values.
	 */
	public void updateCameraSettings(UsbCamera camera) 
	{
		Util.consoleLog();

		camera.setResolution(width, height);
		camera.setFPS((int) frameRate);
		camera.setExposureManual(exposure);
		camera.setWhiteBalanceManual(whitebalance);
		camera.setBrightness(brightness);
	}

	// Run thread to read and feed camera images. Called by Thread.start().
	
	public void run()
	{
		Util.consoleLog();
		
		if (!initialized) return;
		
		try
		{
			Util.consoleLog();

			while (!isInterrupted())
			{
				if (!changingCamera) UpdateCameraImage();
		
				Timer.delay(1 / frameRate);
			}
		}
		catch (Throwable e) {Util.logException(e);}
	}
	
	/**
	 * Get last image read from camera.
	 * @return Mat Last image from camera.
	 */
	public Mat getCurrentImage()
	{
		Util.consoleLog();
		
	    synchronized (this) 
	    {
	    	return image;
	    }
	}
	
	/**
	 * Stop image feed, ie close cameras stop feed thread, release the
	 * singleton cameraFeed object.
	 */
	public void EndFeed()
	{
		if (!initialized) return;

		try
		{
    		Util.consoleLog();

    		Thread.currentThread().interrupt();
    		
    		cam1.free();
    		cam2.free();
    		
    		currentCamera = cam1 = cam2 =  null;

    		mjpegServer = null;
	
    		cameraFeed = null;
		}
		catch (Throwable e)	{Util.logException(e);}
	}
	
	/**
	 * Change the camera to get images from the other camera. 
	 */
	public void ChangeCamera()
    {
		Util.consoleLog();
		
		if (!initialized) return;
		
		changingCamera = true;
		
		if (currentCamera == null || currentCamera.equals(cam2))
			currentCamera = cam1;
		else
			currentCamera = cam2;

		Util.consoleLog("current=%s", currentCamera.getName());
		
	    synchronized (this) 
	    {
	    	imageSource.setSource(currentCamera);
	    }
	    
	    changingCamera = false;
	    
	    Util.consoleLog("end");
    }
    
	// Get an image from current camera and give it to the server.
    
	private void UpdateCameraImage()
    {
		if (currentCamera != null)
		{	
		    synchronized (this) 
		    {
		    	imageSource.grabFrame(image);
		    	
		    	imageOutputStream.putFrame(image);
		    }
		}
    }
}
