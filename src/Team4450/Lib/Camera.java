package Team4450.Lib;

import org.opencv.core.Mat;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.CameraServer;

/**
 * Manages USB camera by wrapping WpiLib USB camera class. 
 */
public class Camera
{
	private String 			name;
	private UsbCamera 		cam;
	private int				device;
	private CameraServer	server;
	private CvSink			imageSource;
	private Mat				image;
	private boolean			enabled;

	// Camera settings - Static
	public static final int 	width = 640;
	public static final int 	height = 480;
	//public static final double 	fovH = 48.0;
	//public static final double 	fovV = 32.0;
	public static final int 	fps = 4;

	// Camera settings - Dynamic
	public int whitebalance = 4700;	// Color temperature in K, -1 is auto
	public int brightness = -1;		// 0 - 100, -1 is "do not set"
	public int exposure = -1;		// 0 - 100, -1 is "auto"

	/**
	 * Create USBCamera object.
	 * @param name Camera name you wish to use.
	 * @param decice Usb device number from RoboRio mapping.
	 * of connected Usb cameras.
	 */
	public Camera(String name, int device, CameraServer server) 
	{
		this.name = name;
		this.device = device;
		this.server = server;
	}
	
	void free()
	{
		if (cam != null) cam.free();
		cam = null;
		
		if (imageSource != null) imageSource.free();
		imageSource = null;
	}
	
	/**
	 * Returns the name of the camera.
	 * @return Camera name.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Returns the device number of the camera.
	 * @return Camera device number.
	 */
	public int getDevice()
	{
		return device;
	}
	
	/**
	 * Returns running state of camera.
	 * @return True if running, false if not.
	 */
	public boolean isRunning() 
	{
		return (cam != null);
	}
	
	/**
	 * Returns image capturing state of camera.
	 * @return True if capturing images, false if not.
	 */
	public boolean isEnabled() 
	{
		return (enabled);
	}
	
	/**
	 * Update camera with current settings fields values.
	 */
	public void updateSettings() 
	{
		if (!isRunning()) return;

		cam.setResolution(width, height);
		cam.setFPS(fps);
		
		if (exposure >= 0) 
			cam.setExposureManual(exposure);
		else 
			cam.setExposureAuto();
		
		if (whitebalance >= 0) 
			cam.setWhiteBalanceManual(whitebalance);
		else 
			cam.setWhiteBalanceAuto();
		
		if (brightness >= 0) cam.setBrightness(brightness);
	}
	
	/**
	 * Start the camera capturing images.
	 */
	public void startCapture() 
	{
		if (!isRunning())
		{
			cam = new UsbCamera(name, device);
		
			updateSettings();

			server.startAutomaticCapture(cam);
			
			imageSource = CameraServer.getInstance().getVideo();
		}
		
		if (!isEnabled())
		{
			enabled = true;
			imageSource.setEnabled(enabled);
		}
	}

	/**
	 * Stops camera image capturing.
	 */
	public void stopCapture() 
	{
		enabled = false;
		imageSource.setEnabled(enabled);
	}
	
	/**
	 * Get the last image captured.
	 * @return Last image captured.
	 */
	public Mat getImage() 
	{
		startCapture();		
		
		imageSource.grabFrame(image);
		
		return image;
	}
}
