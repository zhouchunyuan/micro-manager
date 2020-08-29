

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;

import org.micromanager.internal.MMStudio;
import ij.IJ;

public class MenuBar implements ActionListener{
                public String devName = " no camera";

		private final JFrame frame;
		private final JPanel mainPanel;
                
                public JLabel msgLabel = new JLabel(devName);
                
                private boolean showbox = false;
                private JButton boxButton = new JButton("Press to show box");
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
                
                JCheckBox trkCheckBox = new JCheckBox("Idle", false); 
                JCheckBox swap_xy_CheckBox = new JCheckBox("Swap XY", swap_xy); 
                JCheckBox flip_v_CheckBox = new JCheckBox("Flip V", flip_v); 
                JCheckBox flip_h_CheckBox = new JCheckBox("Flip H", flip_h); 

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
    
                        int rows = 6;
                        int cols = 3;
			mainPanel = new JPanel(new GridLayout(rows,cols));
                        
                        JPanel[][] panelHolder = new JPanel[rows][cols];    
                        for(int m = 0; m < rows; m++) {
                           for(int n = 0; n < cols; n++) {
                              panelHolder[m][n] = new JPanel(new GridLayout(1,1));
                              mainPanel.add(panelHolder[m][n]);
                           }
                        }
			
                        panelHolder[0][1].add(trkCheckBox);

                        panelHolder[1][0].add(swap_xy_CheckBox);
                        panelHolder[1][1].add(flip_v_CheckBox);
                        panelHolder[1][2].add(flip_h_CheckBox);
                        
			//panelHolder[2][0].add(boxButton);
                        panelHolder[2][1].add(up_Button);
                        panelHolder[4][1].add(down_Button);
                        panelHolder[3][0].add(left_Button);
                        panelHolder[3][2].add(right_Button);
                        
                        panelHolder[5][1].add(msgLabel);
                        
                        swap_xy_CheckBox.addActionListener(this);
                        flip_v_CheckBox.addActionListener(this);
                        flip_h_CheckBox.addActionListener(this);
                        trkCheckBox.addActionListener(this);
                        up_Button.addActionListener(this);
                        down_Button.addActionListener(this); 
                        left_Button.addActionListener(this); 
                        right_Button.addActionListener(this); 

			frame.add(mainPanel, BorderLayout.CENTER);
		
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
                        

		}
                
                public void actionPerformed(ActionEvent e) {
                    swap_xy = swap_xy_CheckBox.isSelected();
                    flip_h = flip_h_CheckBox.isSelected();
                    flip_v = flip_v_CheckBox.isSelected();
                    
                    if( up_Button   ==e.getSource() )moveStage(0,-step);
                    if( down_Button ==e.getSource() )moveStage(0,step);
                    if( left_Button ==e.getSource() )moveStage(-step,0);
                    if( right_Button==e.getSource() )moveStage(step,0);
                    if(trkCheckBox  ==e.getSource()){
                        ready = trkCheckBox.isSelected();
                        if (ready) {
                            set_ready();
                        } else {
                            set_idle();
                        }
                    }
		}
                public void set_ready(){
                    trkCheckBox.setText("Ready");
                    //setProperty("trackingReady","true");
                    setProperty("trackingState","Ready");
                    tracking = false;
                }
                public void set_tracking(int x,int y, int size){
                   // define box

                    setProperty("TrackRoiCenterX", Integer.toString(x));

                    setProperty("TrackRoiCenterY", Integer.toString(y));
                    
                    setProperty("TrackRoiSize", Integer.toString(size));
                    
                    trkCheckBox.setText("Tracking");
                    setProperty("trackingState","Tracking");
                    tracking = true;
                }
                public void set_idle(){
                    trkCheckBox.setText("Idle");
                    //setProperty("trackingReady","false");
                    setProperty("trackingState","Idle");
                    tracking = false;
                }
                
                public boolean isReady(){ return trkCheckBox.getText().equals("Ready"); }
                public boolean isTracking(){ return trkCheckBox.getText().equals("Tracking"); }
                public boolean isIdle(){ return trkCheckBox.getText().equals("Idle"); }
                
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
                    if(swap_xy){
                        double tmp = dy;
                        dy = dx;
                        dx = tmp;
                    }
                    if(flip_h)dx = -dx;
                    if(flip_v)dy = -dy;
                    try{
                        studio.core().waitForDevice( studio.core().getXYStageDevice() );
                        double x0 = studio.core().getXPosition();
                        double y0 = studio.core().getYPosition();
                        studio.core().setXYPosition(x0+dx, y0+dy);
                    }catch (Exception e) {
                        e.printStackTrace();
                        IJ.log("warning: stage busy");
                    }
                }
                
}
