// This is modified according to the following links:
// 1. https://imagej.nih.gov/ij/plugins/download/Mouse_Listener.java
// 2. https://docs.oracle.com/javase/tutorial/uiswing/events/mousewheellistener.html
// 3. https://github.com/micro-manager/micro-manager/blob/master/mmAsImageJMacros/src/main/java/MM2_MacroExtensions.java

//import org.micromanager.data.Image;
import org.micromanager.internal.MMStudio;
import org.micromanager.internal.dialogs.PixelSizeProvider;

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
                MenuBar m = new MenuBar(studio.getCMMCore().getCameraDevice());

        
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
                        
                        while(m.tracking && studio.live().getIsLiveModeOn() ){
                            double dx = Double.parseDouble( m.getProperty( "detectedX"))-w/2;
                            double dy = -(Double.parseDouble(m.getProperty( "detectedY"))-h/2);//invert y
                            
                            try {
                                    double x0 = studio.core().getXPosition();
                                    double y0 = studio.core().getYPosition();
                                    studio.core().setXYPosition((x0+dx)*cal, (y0+dy)*cal);
                                    //IJ.log(dx+","+dy);
                                    studio.core().waitForDevice( studio.core().getXYStageDevice() );
                                    Thread.sleep(200);
                                    studio.refreshGUI();
                            } catch (Exception e) {
                                    e.printStackTrace();
                            }

                        }
                        // set menu state to idle and uncheck box
                        m.set_idle();
                        m.trkCheckBox.setSelected(false);
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