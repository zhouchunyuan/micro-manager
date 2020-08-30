// This is modified according to the following links:
// 1. https://imagej.nih.gov/ij/plugins/download/Mouse_Listener.java
// 2. https://docs.oracle.com/javase/tutorial/uiswing/events/mousewheellistener.html
// 3. https://github.com/micro-manager/micro-manager/blob/master/mmAsImageJMacros/src/main/java/MM2_MacroExtensions.java

//import org.micromanager.data.Image;
import org.micromanager.internal.MMStudio;
//import org.micromanager.internal.dialogs.PixelSizeProvider;
import javax.vecmath.Vector2d;

import ij.IJ;
import ij.ImagePlus;
//import ij.macro.ExtensionDescriptor;
//import ij.macro.Functions;
//import ij.macro.MacroExtension;
//import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

//import ij.*;
import ij.plugin.filter.PlugInFilter;
//import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Tracking_Bridge implements PlugInFilter, MouseListener, MouseMotionListener,MouseWheelListener {
    
    		ImagePlus img;
		ImageCanvas canvas;
		static Vector images = new Vector();
                int trackRoiSize;// trackRoiSize is roi size; To be set by img height
                int roiX;
                int roiY;
                MMStudio studio = MMStudio.getInstance();
                MenuBar m = new MenuBar();

        
        public int setup(String arg, ImagePlus img) {
		this.img = img;
		IJ.register(Tracking_Bridge.class);
		return DOES_ALL+NO_CHANGES;
	}
        public void run(ImageProcessor ip) {
		Integer id = new Integer(img.getID());
		if (images.contains(id)) {
			IJ.log("Already listening to this image");
			//return;
		} else {
                        trackRoiSize = img.getHeight()/10;
			ImageWindow win = img.getWindow();
			canvas = win.getCanvas();
			canvas.addMouseListener(this);
			canvas.addMouseMotionListener(this);
                        canvas.addMouseWheelListener(this);
			images.addElement(id);
		}
                
                
	}

    //**************************** event callbacks **************
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    
    // define new target coordinate on click
    @Override
    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        roiX = canvas.offScreenX(x);
        roiY = canvas.offScreenY(y);
        if(m.isTracking()){
                m.set_tracking(roiX,roiY,trackRoiSize);
                img.deleteRoi();
        }else
        if(m.isReady()){
                m.set_tracking(roiX,roiY,trackRoiSize);
                img.deleteRoi();
                Thread acqThread = new Thread(new Runnable() {
                   @Override
                   public void run() {
                        //start tracking
                        double w = studio.core().getImageWidth();
                        double h = studio.core().getImageHeight();
                        double cal = studio.core().getPixelSizeUm();
                        double pixErr = 2;
                        //double maxStepSize = trackRoiSize/10;
                       
                        while(m.tracking && studio.live().getIsLiveModeOn() ){
                            //try{
                             //Thread.sleep(500);
                            String trackingResult =  m.getProperty( "trackingResult");//fail or ok
                            
                            double screen_dx = Double.parseDouble( m.getProperty( "detectedX"))-w/2;
                            double screen_dy = Double.parseDouble( m.getProperty( "detectedY"))-h/2;
                            
                            double PID_Px = m.trackingForce*Math.abs(screen_dx)/w;
                            double PID_Py = m.trackingForce*Math.abs(screen_dy)/h;
                            // scale down to maxStepSize
                            /*
                            if ( Math.abs(dx) > maxStepSize ){
                                dy = maxStepSize/Math.abs(dx) * dy;
                                dx = maxStepSize/Math.abs(dx) * dx;
                            }
                            if ( Math.abs(dy) > maxStepSize){
                                dx = maxStepSize/Math.abs(dy) * dx;
                                dy = maxStepSize/Math.abs(dy) * dy;
                            }
                            */
                            //IJ.log("screen:dx="+dx+", dy="+dy+" PID_P = "+PID_P);
                            try {
                                    if( Math.abs(screen_dx) >pixErr || Math.abs(screen_dy) >pixErr ){
                                        
                                        double dx = screen_dx*cal*PID_Px;
                                        double dy = screen_dy*cal*PID_Py;

                                        Vector2d dv = m.transform(dx, dy);
                                       
                                        studio.core().waitForDevice( studio.core().getXYStageDevice() );
                                        double x0 = studio.core().getXPosition();
                                        double y0 = studio.core().getYPosition();
                                        if(trackingResult.equals( "ok" )){
                                            studio.core().setXYPosition(x0+dv.x, y0+dv.y);
                                            m.dx4draw = screen_dx/w;
                                            m.dy4draw = screen_dy/h;
                                            m.trackingStatusPanel.repaint();
                                        }
                                        //IJ.log(" movex:"+(-dx*cal*PID_P) +" | movey:" + (dy*cal*PID_P));
                                    }
                                   
                                    studio.refreshGUI();
                            } catch (Exception e) {
                                    e.printStackTrace();
                            }

                        }
                        // set menu state to idle and uncheck box
                        m.set_idle();
                        m.trkButton.setSelected(false);
                   }
                });
                acqThread.start();
            }
    }	
    @Override
    public void mouseMoved(MouseEvent e) {
        if(!m.isReady())return;
        
            int x = e.getX();
            int y = e.getY();
            roiX = canvas.offScreenX(x);
            roiY = canvas.offScreenY(y);
            //IJ.log("Mouse dragged: "+offscreenX+","+offscreenY+modifiers(e.getModifiers()));
            img.setRoi(roiX-trackRoiSize/2,roiY-trackRoiSize/2,trackRoiSize,trackRoiSize);
            //IJ.log(m.getProperty( "x position")+","+m.getProperty( "y position"));
        

    }
    //Use mouse wheel to adjust Roi size
     @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
       if(!m.isReady())return;
       
       if (e.getWheelRotation() < 0) {
           if( trackRoiSize < img.getHeight()/4 ) trackRoiSize++;
       } else {
           if( trackRoiSize>10 ) trackRoiSize--;
       }
       img.setRoi(roiX-trackRoiSize/2,roiY-trackRoiSize/2,trackRoiSize,trackRoiSize);
       img.updateAndDraw();
    }
    
}