

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;

import javax.vecmath.Vector2d;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.BorderFactory;
import javax.swing.border.LineBorder;
import javax.swing.JSlider;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.micromanager.internal.MMStudio;
import ij.IJ;
import java.awt.Color;

public class MenuBar implements ActionListener,ChangeListener{
                public String devName = " no camera";

		private final JFrame frame;
		private final JPanel mainPanel;
                JPanel trackingStatusPanel;
                
                public JLabel msgLabel = new JLabel(devName);
                
                public double trackingForce = 0.8;
                private JLabel lblForce = new JLabel("tracking force:"+trackingForce);
               
                //private JButton boxButton = new JButton("Press to show box");
                private JButton up_Button = new JButton("up");
                private JButton down_Button = new JButton("down");
                private JButton left_Button = new JButton("left");
                private JButton right_Button = new JButton("right");
                
                boolean tracking = false;
                boolean ready = false;
                boolean swap_xy = false;
                boolean flip_h = false;
                boolean flip_v = false;
                
                double step = 10;
                double dx4draw = 0;
                double dy4draw = 0;
                
                JToggleButton trkButton = new JToggleButton("Idle");
                
                JCheckBox swap_xy_CheckBox = new JCheckBox("Swap XY", swap_xy); 
                JCheckBox flip_v_CheckBox = new JCheckBox("Flip V", flip_v); 
                JCheckBox flip_h_CheckBox = new JCheckBox("Flip H", flip_h); 
                
                public JSlider trkSlideForce = new JSlider(0,100);
                
		public MenuBar() {
                    
                    devName = MMStudio.getInstance().getCMMCore().getCameraDevice();
                    msgLabel.setText(" Camera : "+devName);
                        
                        
			frame = new JFrame("Tracking Menu");
                        //            panel layout
                        //-------------------------------------
                        //1             |   tracking |
                        //-------------------------------------
                        //2    swap xy  |   flip y   | flip y
                        //-------------------------------------
                        //3                    up
                        //4        left       step       right
                        //5                   down
                        //-------------------------------------
                        //6
                        

			mainPanel = new JPanel(new GridLayout(3,1));
                        
                        JPanel trackingControlPanel = new JPanel(new GridLayout(1,1));
                        trackingControlPanel.add(trkButton);
                        trackingControlPanel.add(trkSlideForce);
                        trackingControlPanel.add(lblForce);
                        trkSlideForce.setValue((int)(trackingForce*100));
                        trkSlideForce.setOrientation(JSlider.VERTICAL);
                        trkButton.setBorder(new LineBorder(Color.darkGray,10));
                        trkButton.setFont(new Font("Arial", Font.PLAIN, 40));
                        mainPanel.add(trackingControlPanel);
                        
                        int rows = 6;
                        int cols = 3;                 
                        JPanel trackingConfigPanel = new JPanel(new GridLayout(rows,cols));
                        JPanel[][] panelHolder = new JPanel[rows][cols];    
                        for(int m = 0; m < rows; m++) {
                           for(int n = 0; n < cols; n++) {
                              panelHolder[m][n] = new JPanel(new GridLayout(1,1));
                              trackingConfigPanel.add(panelHolder[m][n]);
                           }
                        }
			
                        //panelHolder[0][1].add(trkButton);

                        panelHolder[1][0].add(swap_xy_CheckBox);
                        panelHolder[1][1].add(flip_v_CheckBox);
                        panelHolder[1][2].add(flip_h_CheckBox);
                        
			//panelHolder[2][0].add(boxButton);
                        panelHolder[2][1].add(up_Button);
                        panelHolder[4][1].add(down_Button);
                        panelHolder[3][0].add(left_Button);
                        panelHolder[3][2].add(right_Button);
                        
                        panelHolder[5][1].add(msgLabel);
                        //panelHolder[5][0].add(lblForce);
                        
                        mainPanel.add(trackingConfigPanel);
                        
                        trackingStatusPanel = new JPanel(new GridLayout(1,1)){
                            @Override
                            public void paint(java.awt.Graphics g){
                                super.paint(g);
                                drawStageMap(this,g);
                            }
                        };
                        trackingStatusPanel.setPreferredSize(new java.awt.Dimension(100,200));
                        mainPanel.add(trackingStatusPanel);                        
                        
                        
                   
                        swap_xy_CheckBox.addActionListener(this);
                        flip_v_CheckBox.addActionListener(this);
                        flip_h_CheckBox.addActionListener(this);
                        trkButton.addActionListener(this);
                        up_Button.addActionListener(this);
                        down_Button.addActionListener(this); 
                        left_Button.addActionListener(this); 
                        right_Button.addActionListener(this); 
                        
                        trkSlideForce.addChangeListener(this);

			frame.add(mainPanel, BorderLayout.CENTER);
		
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
                        

		}
                public void stateChanged(ChangeEvent e){
                    if( trkSlideForce ==e.getSource()){
                        trackingForce = (double)trkSlideForce.getValue()/100.0;
                        lblForce.setText("tracking force:"+trackingForce);
                    }
                }
                public void actionPerformed(ActionEvent e) {
                    swap_xy = swap_xy_CheckBox.isSelected();
                    flip_h = flip_h_CheckBox.isSelected();
                    flip_v = flip_v_CheckBox.isSelected();
                    
                    if( up_Button   ==e.getSource() )moveStage(0,-step);
                    if( down_Button ==e.getSource() )moveStage(0,step);
                    if( left_Button ==e.getSource() )moveStage(-step,0);
                    if( right_Button==e.getSource() )moveStage(step,0);
                    if(trkButton  ==e.getSource()){
                        ready = trkButton.isSelected();
                        if (ready) {
                            set_ready();
                        } else {
                            set_idle();
                        }
                    }

		}
                public void set_ready(){
                    trkButton.setText("Ready");
                    trkButton.setBorder(new LineBorder(Color.GREEN,10));
                    setProperty("trackingState","Ready");
                    tracking = false;
                }
                public void set_tracking(int x,int y, int size){
                   // define box

                    setProperty("TrackRoiCenterX", Integer.toString(x));

                    setProperty("TrackRoiCenterY", Integer.toString(y));
                    
                    setProperty("TrackRoiSize", Integer.toString(size));
                    
                    trkButton.setText("Tracking");
                    trkButton.setBorder(new LineBorder(Color.MAGENTA,10));
                    setProperty("trackingState","Tracking");
                    tracking = true;
                }
                public void set_idle(){
                    trkButton.setText("Idle");
                    trkButton.setBorder(new LineBorder(Color.darkGray,10));
                    setProperty("trackingState","Idle");
                    tracking = false;
                }
                
                public boolean isReady(){ return trkButton.getText().equals("Ready"); }
                public boolean isTracking(){ return trkButton.getText().equals("Tracking"); }
                public boolean isIdle(){ return trkButton.getText().equals("Idle"); }
                
                public void setProperty(String propName, String val){
                    MMStudio studio = MMStudio.getInstance();
                        try {
                            studio.core().setProperty(devName, propName, val);
                            studio.refreshGUI();
                        }catch(Exception ex) {
                            ex.printStackTrace();
                        }
                }
                public String getProperty(String propName){
                    MMStudio studio = MMStudio.getInstance();
                    String str="";
                        try {
                            str = studio.core().getProperty(devName, propName);
                            studio.refreshGUI();
                        }catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    return str;
                }
                
                private void moveStage(double dx,double dy){
                    //IJ.log("moveStage"+dx+","+dy);
                    MMStudio studio = MMStudio.getInstance();

                    Vector2d transformed_v = transform(dx,dy);
                    
                    try{
                        studio.core().waitForDevice( studio.core().getXYStageDevice() );
                        double x0 = studio.core().getXPosition();
                        double y0 = studio.core().getYPosition();
                        studio.core().setXYPosition(x0+transformed_v.x, y0+transformed_v.y);
                    }catch (Exception e) {
                        e.printStackTrace();
                        IJ.log("warning: stage busy");
                    }
                    trackingStatusPanel.repaint();
                }
                
                void drawStageMap(JPanel panel,java.awt.Graphics g){
                    int W = panel.getSize().width;
                    int H = panel.getSize().height;
                    int cx = W/2;
                    int cy = H/2;
                    
                    // stage boader
                    g.setColor(Color.green);
                    g.drawRect(0, 0, W-1, H-1);
                    g.drawOval(cx-2, cy-2, 4, 4);
                    
                    // FOV boader
                    int FOV_W = W/3;
                    int FOV_H = H/3;
                    
                    Vector2d transformed_v = transform(dx4draw*100,dy4draw*100);
                    
                    MMStudio studio = MMStudio.getInstance();
                    try{
                        int x0 =(int)( studio.core().getXPosition());
                        int y0 =(int)( studio.core().getYPosition());
                        int rectCx = x0+cx;
                        int rectCy = y0+cy;
                        if( rectCx>W-FOV_W/2)rectCx= W-FOV_W/2;
                        if( rectCx<  FOV_W/2)rectCx= FOV_W/2;
                        if( rectCy>H-FOV_H/2)rectCy= H-FOV_H/2;
                        if( rectCy<  FOV_H/2)rectCy= FOV_H/2;
                        g.drawRect(rectCx-FOV_W/2,rectCy-FOV_H/2,FOV_W,FOV_H);
                        g.drawLine(rectCx, rectCy, rectCx+(int)transformed_v.x, rectCy+(int)transformed_v.y);
                        
                        }catch (Exception e) {
                            e.printStackTrace();
                            IJ.log("warning: stage busy");
                    }
                }
                
                // transform coordinate system
                // according to swap_xy,flip_h,flip_v
                Vector2d transform(double dx, double dy){
                    if(swap_xy){
                        double tmp = dy;
                        dy = dx;
                        dx = tmp;
                    }
                    if(flip_h)dx = -dx;
                    if(flip_v)dy = -dy;
                    return new Vector2d(dx,dy);
                }
                
}
