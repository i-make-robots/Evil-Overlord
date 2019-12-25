package com.marginallyclever.robotOverlord.robots.sixi2;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3d;

import com.marginallyclever.convenience.SpringUtilities;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotOverlord.CollapsiblePanel;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.dhRobot.DHLink;
import com.marginallyclever.robotOverlord.dhRobot.DHRobot;

/**
 * Control Panel for a DHRobot
 * @author Dan Royer
 *
 */
public class Sixi2Panel extends JPanel implements ActionListener, ChangeListener, ItemListener, Observer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	protected Sixi2 robot;
	protected RobotOverlord ro;

	public JButton buttonCalibrate;
	public JButton goHome;
	public JButton goRest;
	public JSlider feedrate, acceleration, gripperOpening;
	public JLabel  feedrateValue, accelerationValue, gripperOpeningValue;

	public JCheckBox immediateDriving;
	public JComboBox<String> frameOfReferenceSelection;

	public JLabel gcodeLabel;
	public JTextField gcodeValue;
	public JPanel ghostPosPanel;
	public JPanel livePosPanel;
	public ReentrantLock sliderLock;
	
	
	public class Pair {
		public JSlider slider;
		public DHLink  link;
		public JLabel  label;
		
		public Pair(JSlider slider0,DHLink link0,JLabel label0) {
			slider=slider0;
			link=link0;
			label=label0;
		}
	}
	
	ArrayList<Pair> liveJoints = new ArrayList<Pair>();
	ArrayList<Pair> ghostJoints = new ArrayList<Pair>();

	// enumerate these?
	String[] framesOfReference = {
			"World", //0
			"Camera", //1
			"Finger tip"//2
			};
	
	
	public Sixi2Panel(RobotOverlord gui,Sixi2 robot) {
		this.robot = robot;
		this.ro = gui;
		sliderLock = new ReentrantLock();
		
		buildPanel();
	}
	
	protected void buildPanel() {
		this.removeAll();

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.weightx=1;
		c.weighty=1;
		c.anchor=GridBagConstraints.NORTHWEST;
		c.fill=GridBagConstraints.HORIZONTAL;

		CollapsiblePanel oiwPanel = new CollapsiblePanel("Sixi2");
		this.add(oiwPanel,c);
		JPanel contents = oiwPanel.getContentPane();		
		
		contents.setBorder(new EmptyBorder(0,0,0,0));
		contents.setLayout(new GridBagLayout());
		GridBagConstraints con1 = new GridBagConstraints();
		con1.gridx=0;
		con1.gridy=0;
		con1.weightx=1;
		con1.weighty=1;
		con1.fill=GridBagConstraints.HORIZONTAL;

		//this.add(toggleATC=new JButton(robot.dhTool!=null?"ATC close":"ATC open"), con1);
		contents.add(buttonCalibrate=new JButton("Calibrate"), con1);
		con1.gridy++;
		buttonCalibrate.addActionListener(this);

		contents.add(goHome=new JButton("Go Home"), con1);
		con1.gridy++;
		goHome.addActionListener(this);

		contents.add(goRest=new JButton("Go Rest"), con1);
		con1.gridy++;
		goRest.addActionListener(this);
		
		contents.add(immediateDriving=new JCheckBox(),con1);
		con1.gridy++;
		immediateDriving.setText("Immediate driving");
		immediateDriving.addItemListener(this);
		immediateDriving.setSelected(robot.immediateDriving);

		contents.add(feedrate=new JSlider(),con1);
		con1.gridy++;
		contents.add(feedrateValue=new JLabel(),con1);
		con1.gridy++;
		feedrate.setMaximum(80);
		feedrate.setMinimum(1);
		feedrate.setMinorTickSpacing(1);
		feedrate.addChangeListener(this);
		feedrate.setValue((int)robot.getFeedrate());
		stateChanged(new ChangeEvent(feedrate));

		contents.add(acceleration=new JSlider(),con1);
		con1.gridy++;
		contents.add(accelerationValue=new JLabel(),con1);
		con1.gridy++;
		acceleration.setMaximum(120);
		acceleration.setMinimum(1);
		acceleration.setMinorTickSpacing(1);
		acceleration.addChangeListener(this);
		acceleration.setValue((int)robot.getAcceleration());
		stateChanged(new ChangeEvent(acceleration));
/*
		contents.add(activeTool=new JLabel("Tool=") ,con1);
		  con1.gridy++; 
		contents.add(gripperOpening=new JSlider(),con1);
		con1.gridy++;
		contents.add(gripperOpeningValue=new JLabel(),con1);
		con1.gridy++;
		gripperOpening.setMaximum(120);
		gripperOpening.setMinimum(90);
		gripperOpening.setMinorTickSpacing(5);
		gripperOpening.addChangeListener(this);
		gripperOpening.setValue((int)robot.dhTool.getAdjustableValue());
		stateChanged(new ChangeEvent(gripperOpening));
*/
/*
		contents.add(new JLabel("Frame of Reference") ,con1);  con1.gridy++;
		contents.add(frameOfReferenceSelection=new JComboBox<String>(Sixi2.Frame.values()),con1);
		frameOfReferenceSelection.addActionListener(this);
		frameOfReferenceSelection.setSelectedIndex(robot.getFrameOfReference());
		con1.gridy++;
*/
		
		contents.add(gcodeLabel=new JLabel("Gcode"), con1);
		con1.gridy++;
		contents.add(gcodeValue=new JTextField(),con1);
		con1.gridy++;
		gcodeValue.setEditable(false);
		Dimension dim = gcodeValue.getPreferredSize();
		dim.width=60;
		gcodeValue.setPreferredSize( dim );
		gcodeValue.setMaximumSize(dim);
		gcodeValue.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
            	StringSelection stringSelection = new StringSelection(gcodeValue.getText());
            	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            	clipboard.setContents(stringSelection, null);
            }
        });
		
		int i;
		JLabel label;

		CollapsiblePanel livePanel = new CollapsiblePanel("Live");
		this.add(livePanel,con1);
		con1.gridy++;
		livePanel.getContentPane().setLayout(new BoxLayout(livePanel.getContentPane(),BoxLayout.PAGE_AXIS));
		
		contents = new JPanel();
		livePanel.getContentPane().add(contents);
		contents.setBorder(new EmptyBorder(0,0,0,0));
		contents.setLayout(new SpringLayout());
		i=0;
		for( DHLink link : robot.live.links ) {
			if(!link.hasAdjustableValue()) continue;
			JSlider newSlider=new JSlider(
					JSlider.HORIZONTAL,
					(int)link.getRangeMin(),
					(int)link.getRangeMax(),
					(int)link.getRangeMin());
			newSlider.setMinorTickSpacing(5);
			newSlider.setEnabled(false);
			contents.add(new JLabel(Integer.toString(i++)));
			contents.add(newSlider);
			contents.add(label=new JLabel("0.000",SwingConstants.RIGHT));
			liveJoints.add(new Pair(newSlider,link,label));
			link.addObserver(this);
			newSlider.setValue((int)link.getAdjustableValue());
			label.setText(StringHelper.formatDouble(link.getAdjustableValue()));
			label.setMinimumSize(new Dimension(50,16));
			label.setPreferredSize(label.getMinimumSize());
		}
		SpringUtilities.makeCompactGrid(contents, i, 3, 5, 5, 5, 5);

		livePosPanel = new JPanel();
		livePosPanel.setBorder(new EmptyBorder(0,0,0,0));
		livePosPanel.setLayout(new SpringLayout());
		livePanel.getContentPane().add(livePosPanel);
		updatePosition(robot.ghost,livePosPanel);
		
		
		// ghost panel
		CollapsiblePanel ghostPanel = new CollapsiblePanel("Ghost");
		this.add(ghostPanel,con1);
		con1.gridy++;
		ghostPanel.getContentPane().setLayout(new BoxLayout(ghostPanel.getContentPane(),BoxLayout.PAGE_AXIS));

		contents = new JPanel();
		ghostPanel.getContentPane().add(contents);
		contents.setBorder(new EmptyBorder(0,0,0,0));
		contents.setLayout(new SpringLayout());
		i=0;
		for( DHLink link : robot.ghost.links ) {
			if(!link.hasAdjustableValue()) continue;
			JSlider newSlider=new JSlider(
					JSlider.HORIZONTAL,
					(int)link.getRangeMin(),
					(int)link.getRangeMax(),
					(int)link.getRangeMin());
			newSlider.setMinorTickSpacing(5);
			contents.add(new JLabel(Integer.toString(i++)));
			contents.add(newSlider);
			contents.add(label=new JLabel("0.000",SwingConstants.RIGHT));
			ghostJoints.add(new Pair(newSlider,link,label));
			link.addObserver(this);
			newSlider.setValue((int)link.getAdjustableValue());
			label.setText(StringHelper.formatDouble(link.getAdjustableValue()));
			label.setMinimumSize(new Dimension(50,16));
			label.setPreferredSize(label.getMinimumSize());
			
			//newSlider.setEnabled(false);
			newSlider.addChangeListener(this);
		}
		SpringUtilities.makeCompactGrid(contents, i, 3, 5, 5, 5, 5);

		ghostPosPanel = new JPanel();
		ghostPosPanel.setBorder(new EmptyBorder(0,0,0,0));
		ghostPosPanel.setLayout(new SpringLayout());
		ghostPanel.getContentPane().add(ghostPosPanel);
		updatePosition(robot.ghost,ghostPosPanel);

		gcodeValue.setText(robot.generateGCode());
	}
	
	protected void updatePosition(DHRobot r, JPanel p) {
		p.removeAll();
		Vector3d pos = new Vector3d();
		r.getPoseIK().get(pos);
		p.add(new JLabel("X"));
		p.add(new JLabel(StringHelper.formatDouble(pos.x)));
		p.add(new JLabel("Y"));
		p.add(new JLabel(StringHelper.formatDouble(pos.y)));
		p.add(new JLabel("Z"));
		p.add(new JLabel(StringHelper.formatDouble(pos.z)));
		SpringUtilities.makeCompactGrid(p, 1, 6, 5, 5, 5, 5);
	}
	
	@Override
	public void stateChanged(ChangeEvent event) {
		Object source = event.getSource();
		if(source == feedrate) {
			int v = feedrate.getValue();
			robot.setFeedrate(v);
			feedrateValue.setText("feedrate = "+StringHelper.formatDouble(v));
		}
		if(source == acceleration) {
			int v = acceleration.getValue();
			robot.setAcceleration(v);
			accelerationValue.setText("acceleration = "+StringHelper.formatDouble(v));
		}
		if(source == gripperOpening) {
			int v = gripperOpening.getValue();
			robot.parseGCode("G0 T"+v);
			gripperOpeningValue.setText("gripper = "+StringHelper.formatDouble(v));
		}
		
		//*
		if(!sliderLock.isLocked()) {
			sliderLock.lock();
				for( Pair p : ghostJoints ) {
					if(p.slider == source) {
						if(!p.link.hasChanged()) {
							System.out.println("slider begins");
							int v = ((JSlider)source).getValue();
							p.link.setAdjustableValue(v);
							p.label.setText(StringHelper.formatDouble(v));
							robot.ghost.refreshPose();
							System.out.println("slider ends");
							break;
						}
					}
				}
			sliderLock.unlock();
		}
		//*/
		/*
		if(false) {
			// test size of labels
			double w=0,h=0;
			for( Pair p : ghostJoints ) {
				Dimension d = p.label.getSize();
				w=Math.max(w,d.getWidth());
				h=Math.max(h,d.getHeight());
			}
			System.out.println("w"+w+"\th"+h);
		}//*/
		// live and ghost joints are not updated by the user
		// because right now that would cause an infinite loop.
		
		if(gcodeValue!=null) {
			gcodeValue.setText(robot.generateGCode());
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == buttonCalibrate) {
			// run the calibration app
			Sixi2Calibrator calibrator = new Sixi2Calibrator(ro.getMainFrame(),robot);
			calibrator.run();
		}
		if(source==goHome) {
			robot.ghost.setPoseFK(robot.homeKey);
		}
		if(source==goRest) {
			robot.ghost.setPoseFK(robot.restKey);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// for checkboxes
		Object source = e.getItemSelectable();
		if(source == immediateDriving) {
			robot.immediateDriving = !robot.immediateDriving;
		}
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		for( Pair p : liveJoints ) {
			if(p.link == arg0) {
				double v = (double)arg1;
				p.slider.setValue((int)v);
				p.label.setText(StringHelper.formatDouble(v));
				break;
			}
		}
		if(!sliderLock.isLocked()) {
			sliderLock.lock();
			for( Pair p : ghostJoints ) {
				if(p.link == arg0) {
					System.out.println("observe begins");
					double v = (double)arg1;
					p.slider.setValue((int)v);
					p.label.setText(StringHelper.formatDouble(v));
					System.out.println("observe ends");
					break;
				}
			}
			sliderLock.unlock();
		}
		updatePosition(robot.live,livePosPanel);
		updatePosition(robot.ghost,ghostPosPanel);
	}
}