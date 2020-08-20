

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
		private final JPanel upper_panel;// = new JPanel(new GridLayout(2,3));
                private final JPanel lower_panel;
                
                public JLabel msgLabel = new JLabel(devName);
                
                private boolean showbox = false;
                private JButton boxButton = new JButton("Press to show box");
                
                boolean tracking = false;
                boolean ready = false;
                JCheckBox trkCheckBox = new JCheckBox("Idle", false); 

		public MenuBar(String camName) {
                    
                    devName = camName;
                     msgLabel.setText(" Camera : "+devName);
                        
                        
			frame = new JFrame("Tracking Menu");
			mainPanel = new JPanel(new GridLayout(2,1));
                        lower_panel = new JPanel(new GridLayout(1,1));
                        upper_panel = new JPanel(new GridLayout(2,3));
			
                        upper_panel.add(trkCheckBox);
			upper_panel.add(boxButton);
                        
                        trkCheckBox.addActionListener(this);
                        boxButton.addActionListener(this);  
                        
                        lower_panel.add(msgLabel);
                        
			mainPanel.add(upper_panel);
                        mainPanel.add(lower_panel);
			frame.add(mainPanel, BorderLayout.CENTER);
		
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
                        

		}
                
                public void actionPerformed(ActionEvent e) {
                    /*if(trkButton==e.getSource()){
                        tracking = !tracking;
                        if (tracking) {
                            trkButton.setText("Press to stop track");
                        } else {
                            trkButton.setText("Press to enable track");
                        }
                    }*/
                    if(boxButton==e.getSource()){
                        showbox = !showbox;
                        if (showbox) {
                            boxButton.setText("Press to hide box");
                        } else {
                            boxButton.setText("Press to show box");
                        }
                    }
                    if(trkCheckBox==e.getSource()){
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
  
}
