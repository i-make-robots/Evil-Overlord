package com.marginallyclever.robotOverlord.RotaryStewartPlatform2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import com.jogamp.opengl.GL2;
import javax.swing.JPanel;
import javax.vecmath.Vector3f;

import com.marginallyclever.robotOverlord.*;
import com.marginallyclever.robotOverlord.communications.AbstractConnection;

public class RotaryStewartPlatform2
extends RobotWithConnection
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1816224308642132316L;
	
	// machine ID
	protected long robotUID;
	protected final static String hello = "HELLO WORLD! I AM STEWART PLATFORM V4.2";
	public static final String ROBOT_NAME = "Stewart Platorm 2";
	
	// machine dimensions
	static final float BASE_TO_SHOULDER_X   =( 8.093f);  // measured in solidworks, relative to base origin
	static final float BASE_TO_SHOULDER_Y   =( 2.150f);
	static final float BASE_TO_SHOULDER_Z   =( 6.610f);
	static final float BICEP_LENGTH         =( 5.000f);
	static final float FOREARM_LENGTH       =(16.750f);
	static final float WRIST_TO_FINGER_X    =( 7.635f);
	static final float WRIST_TO_FINGER_Y    =( 0.553f);
	static final float WRIST_TO_FINGER_Z    =(-0.870f);  // measured in solidworks, relative to finger origin
	
	// calibration settings
	protected float HOME_X;
	protected float HOME_Y;
	protected float HOME_Z;
	protected float HOME_A;
	protected float HOME_B;
	protected float HOME_C;
	protected float HOME_RIGHT_X;
	protected float HOME_RIGHT_Y;
	protected float HOME_RIGHT_Z;
	protected float HOME_FORWARD_X;
	protected float HOME_FORWARD_Y;
	protected float HOME_FORWARD_Z;
	
	static final float LIMIT_U=15;
	static final float LIMIT_V=15;
	static final float LIMIT_W=15;
	
	// volumes for collision detection (not being used yet)
	protected Cylinder [] volumes;

	// networking information
	protected boolean isPortConfirmed=false;

	// visual models of robot
	protected transient Model modelTop;
	protected transient Model modelArm;
	protected transient Model modelBase;
	
	// this should be come a list w/ rollback
	protected RotaryStewartPlatform2MotionState motionNow = new RotaryStewartPlatform2MotionState();
	protected RotaryStewartPlatform2MotionState motionFuture = new RotaryStewartPlatform2MotionState();

	// convenience
	boolean hasArmMoved;
	
	// keyboard history
	protected float xDir = 0.0f;
	protected float yDir = 0.0f;
	protected float zDir = 0.0f;
	protected float uDir = 0.0f;
	protected float vDir = 0.0f;
	protected float wDir = 0.0f;
	
	private boolean just_testing_dont_get_uid=false;

	// visual model for controlling robot
	protected transient RotaryStewartPlatform2ControlPanel rspPanel;


	public RotaryStewartPlatform2() {
		super();
		setDisplayName(ROBOT_NAME);

		// load the visuals
		setupModels();
		
		// set up bounding volumes
		volumes = new Cylinder[6];
		for(int i=0;i<volumes.length;++i) {
			volumes[i] = new Cylinder();
		}
		volumes[0].setRadius(3.2f);
		volumes[1].setRadius(3.0f*0.575f);
		volumes[2].setRadius(2.2f);
		volumes[3].setRadius(1.15f);
		volumes[4].setRadius(1.2f);
		volumes[5].setRadius(1.0f*0.575f);

		// set up the initial state of the machine
		motionNow.rotateBase(0,0);
		motionNow.updateIK();
		motionFuture.set(motionNow);
		moveIfAble();
		hasArmMoved = false;
	}
	
	
	public Vector3f getHome() {  return new Vector3f(HOME_X,HOME_Y,HOME_Z);  }
	
	
	public void setHome(Vector3f newhome) {
		HOME_X=newhome.x;
		HOME_Y=newhome.y;
		HOME_Z=newhome.z;
		motionFuture.rotateBase(0f,0f);
		motionFuture.moveBase(new Vector3f(0,0,0));
		moveIfAble();
	}

	protected void loadCalibration() {
		HOME_X = 0.0f;
		HOME_Y = 0.0f;
		HOME_Z = 0.0f;
		HOME_A = 0.0f;
		HOME_B = 0.0f;
		HOME_C = 0.0f;
		
		HOME_RIGHT_X = 0;
		HOME_RIGHT_Y = 0;
		HOME_RIGHT_Z = -1;

		HOME_FORWARD_X = 1;
		HOME_FORWARD_Y = 0;
		HOME_FORWARD_Z = 0;
	}
	

	protected void setupModels() {
		modelTop = Model.loadModel("/StewartPlatform.zip:top.STL",0.1f);
		modelArm = Model.loadModel("/StewartPlatform.zip:arm.STL",0.1f);
		modelBase = Model.loadModel("/StewartPlatform.zip:base.STL",0.1f);
	}

    private void readObject(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException
    {
    	setupModels();
        inputStream.defaultReadObject();
    }

	public void moveX(float dir) {
		xDir=dir;
	}

	public void moveY(float dir) {
		yDir=dir;
	}

	public void moveZ(float dir) {
		zDir=dir;
	}

	public void moveU(float dir) {
		uDir=dir;
	}

	public void moveV(float dir) {
		vDir=dir;
	}

	public void moveW(float dir) {
		wDir=dir;
	}
	 

	public void setSpeed(float newSpeed) {
		motionNow.setSpeed(newSpeed);
	}
	public float getSpeed() {
		return motionNow.getSpeed();
	}
	
	public void updateFK(float delta) {}


	protected void updateIK(float delta) {
		boolean changed=false;
		motionFuture.set(motionNow);

		// lateral moves
		if (xDir!=0) {  motionFuture.fingerPosition.x += xDir * motionFuture.getSpeed();  changed=true;  xDir=0;  }		
		if (yDir!=0) {  motionFuture.fingerPosition.y += yDir * motionFuture.getSpeed();  changed=true;  yDir=0;  }
		if (zDir!=0) {  motionFuture.fingerPosition.z += zDir * motionFuture.getSpeed();  changed=true;  zDir=0;  }
		// rotation		
		if(uDir!=0) {	motionFuture.iku += motionFuture.getSpeed() * uDir;	changed=true;  uDir=0;  }
		if(vDir!=0) {	motionFuture.ikv += motionFuture.getSpeed() * vDir;	changed=true;  vDir=0;  }
		if(wDir!=0) {	motionFuture.ikw += motionFuture.getSpeed() * wDir;	changed=true;  wDir=0;  }

		if(changed==true) {
			moveIfAble();
		}
	}
	
	public void moveIfAble() {
		rotateFinger();	
		
		if(motionFuture.movePermitted()) {
			hasArmMoved=true;
			finalizeMove();
		} else {
			motionFuture.set(motionNow);
		}
	}
	
	
	public void rotateFinger() {
		Vector3f forward = new Vector3f(HOME_FORWARD_X,HOME_FORWARD_Y,HOME_FORWARD_Z);
		Vector3f right = new Vector3f(HOME_RIGHT_X,HOME_RIGHT_Y,HOME_RIGHT_Z);
		Vector3f up = new Vector3f();
		
		up.cross(forward,right);
		
		Vector3f of = new Vector3f(forward);
		Vector3f or = new Vector3f(right);
		Vector3f ou = new Vector3f(up);
		
		Vector3f result;

		result = rotateAroundAxis(forward,of,Math.toRadians(motionFuture.iku));  // TODO rotating around itself has no effect.
		result = rotateAroundAxis(result,or,Math.toRadians(motionFuture.ikv));
		result = rotateAroundAxis(result,ou,Math.toRadians(motionFuture.ikw));
		motionFuture.finger_forward.set(result);

		result = rotateAroundAxis(right,of,Math.toRadians(motionFuture.iku));
		result = rotateAroundAxis(result,or,Math.toRadians(motionFuture.ikv));
		result = rotateAroundAxis(result,ou,Math.toRadians(motionFuture.ikw));
		motionFuture.finger_left.set(result);
		
		motionFuture.finger_up.cross(motionFuture.finger_forward,motionFuture.finger_left);
	}
	
		
	/**
	 * Rotate the point xyz around the line passing through abc with direction uvw
	 * http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/ArbitraryAxisRotation.html
	 * Special case where abc=0
	 * @param vec
	 * @param axis
	 * @param angle_radians in radians
	 * @return
	 */
	static public Vector3f rotateAroundAxis(Vector3f vec,Vector3f axis,double angle_radians) {
		float C = (float)Math.cos(angle_radians);
		float S = (float)Math.sin(angle_radians);
		float x = vec.x;
		float y = vec.y;
		float z = vec.z;
		float u = axis.x;
		float v = axis.y;
		float w = axis.z;
		
		// (a*( v*v + w*w) - u*(b*v + c*w - u*x - v*y - w*z))(1.0-C)+x*C+(-c*v + b*w - w*y + v*z)*S
		// (b*( u*u + w*w) - v*(a*v + c*w - u*x - v*y - w*z))(1.0-C)+y*C+( c*u - a*w + w*x - u*z)*S
		// (c*( u*u + v*v) - w*(a*v + b*v - u*x - v*y - w*z))(1.0-C)+z*C+(-b*u + a*v - v*x + u*y)*S
		// but a=b=c=0 so
		// x' = ( -u*(- u*x - v*y - w*z)) * (1.0-C) + x*C + ( - w*y + v*z)*S
		// y' = ( -v*(- u*x - v*y - w*z)) * (1.0-C) + y*C + ( + w*x - u*z)*S
		// z' = ( -w*(- u*x - v*y - w*z)) * (1.0-C) + z*C + ( - v*x + u*y)*S
		
		float a = (-u*x - v*y - w*z);

		return new Vector3f( (-u*a) * (1.0f-C) + x*C + ( -w*y + v*z)*S,
							 (-v*a) * (1.0f-C) + y*C + (  w*x - u*z)*S,
							 (-w*a) * (1.0f-C) + z*C + ( -v*x + u*y)*S);
	}

	
	@Override
	public void prepareMove(float delta) {
		updateFK(delta);
		updateIK(delta);
	}

	@Override
	public void finalizeMove() {
		// copy motion_future to motion_now
		motionNow.set(motionFuture);
		
		if(hasArmMoved) {
			if(motionNow.isHomed && motionNow.isFollowMode ) {
				hasArmMoved=false;
				sendChangeToRealMachine();
				updateGUI();
			}
			
			// TODO: update text fields when the cursor moves.  right now this causes inter-thread damage and crashes the app
			/*
			if(view_px!=null) {	        
				view_px.getField().setText(Float.toString(motion_now.finger_tip.x));
				viewPy.getField().setText(Float.toString(motion_now.finger_tip.y));
				viewPz.getField().setText(Float.toString(motion_now.finger_tip.z));
				viewRx.getField().setText(Float.toString(motion_now.iku));
				viewRy.getField().setText(Float.toString(motion_now.ikv));
				viewRz.getField().setText(Float.toString(motion_now.ikw));
			}*/
		}
	}
	
	
	public void render(GL2 gl2) {
		int i;

		boolean draw_finger_star=false;
		boolean draw_base_star=false;
		boolean draw_shoulder_to_elbow=false;
		boolean draw_shoulder_star=false;
		boolean draw_elbow_star=false;
		boolean draw_wrist_star=false;
		boolean draw_stl=true;

		//RebuildShoulders(motion_now);
		
		gl2.glPushMatrix();
		gl2.glTranslated(position.x, position.y, position.z);
		
		if(draw_stl) {
			// base
			gl2.glPushMatrix();
			gl2.glColor3f(1, 0.8f, 0.6f);
			gl2.glTranslatef(0, 0, BASE_TO_SHOULDER_Z+0.6f);
			gl2.glRotatef(90, 0, 0, 1);
			gl2.glRotatef(90, 1, 0, 0);
			modelBase.render(gl2);
			gl2.glPopMatrix();
			
			// arms
			for(i=0;i<3;++i) {
				gl2.glColor3f(0.9f,0.9f,0.9f);
				gl2.glPushMatrix();
				gl2.glTranslatef(motionNow.arms[i*2+0].shoulder.x,
						         motionNow.arms[i*2+0].shoulder.y,
						         motionNow.arms[i*2+0].shoulder.z);
				gl2.glRotatef(120.0f*i, 0, 0, 1);
				gl2.glRotatef(90, 0, 1, 0);
				gl2.glRotatef(180-motionNow.arms[i*2+0].angle,0,0,1);
				modelArm.render(gl2);
				gl2.glPopMatrix();
	
				gl2.glColor3f(0.9f,0.9f,0.9f);
				gl2.glPushMatrix();
				gl2.glTranslatef(motionNow.arms[i*2+1].shoulder.x,
						         motionNow.arms[i*2+1].shoulder.y,
						         motionNow.arms[i*2+1].shoulder.z);
				gl2.glRotatef(120.0f*i, 0, 0, 1);
				gl2.glRotatef(90, 0, 1, 0);
				gl2.glRotatef(+motionNow.arms[i*2+1].angle,0,0,1);
				modelArm.render(gl2);
				gl2.glPopMatrix();
			}
			//top
			gl2.glPushMatrix();
			gl2.glColor3f(1, 0.8f, 0.6f);
			gl2.glTranslatef(motionNow.fingerPosition.x,motionNow.fingerPosition.y,motionNow.fingerPosition.z+motionNow.relative.z);
			gl2.glRotatef(motionNow.iku, 1, 0, 0);
			gl2.glRotatef(motionNow.ikv, 0, 1, 0);
			gl2.glRotatef(motionNow.ikw, 0, 0, 1);
			gl2.glRotatef(90, 0, 0, 1);
			gl2.glRotatef(180, 1, 0, 0);
			modelTop.render(gl2);
			gl2.glPopMatrix();
		}
		
		// draw the forearms
		
		Cylinder tube = new Cylinder();
		gl2.glColor3f(0.8f,0.8f,0.8f);
		tube.setRadius(0.15f);
		for(i=0;i<6;++i) {
			//gl2.glBegin(GL2.GL_LINES);
			//gl2.glColor3f(1,0,0);
			//gl2.glVertex3f(motion_now.arms[i].wrist.x,motion_now.arms[i].wrist.y,motion_now.arms[i].wrist.z);
			//gl2.glColor3f(0,1,0);
			//gl2.glVertex3f(motion_now.arms[i].elbow.x,motion_now.arms[i].elbow.y,motion_now.arms[i].elbow.z);
			//gl2.glEnd();
			tube.SetP1(motionNow.arms[i].wrist);
			tube.SetP2(motionNow.arms[i].elbow);
			PrimitiveSolids.drawCylinder(gl2, tube);
		}

		gl2.glDisable(GL2.GL_LIGHTING);
		// debug info
		gl2.glPushMatrix();
		for(i=0;i<6;++i) {
			gl2.glColor3f(1,1,1);
			if(draw_shoulder_star) PrimitiveSolids.drawStar(gl2, motionNow.arms[i].shoulder,5);
			if(draw_elbow_star) PrimitiveSolids.drawStar(gl2, motionNow.arms[i].elbow,3);			
			if(draw_wrist_star) PrimitiveSolids.drawStar(gl2, motionNow.arms[i].wrist,1);

			if(draw_shoulder_to_elbow) {
				gl2.glBegin(GL2.GL_LINES);
				gl2.glColor3f(0,1,0);
				gl2.glVertex3f(motionNow.arms[i].elbow.x,motionNow.arms[i].elbow.y,motionNow.arms[i].elbow.z);
				gl2.glColor3f(0,0,1);
				gl2.glVertex3f(motionNow.arms[i].shoulder.x,motionNow.arms[i].shoulder.y,motionNow.arms[i].shoulder.z);
				gl2.glEnd();
			}
		}
		gl2.glPopMatrix();
		
		if(draw_finger_star) {
	 		// draw finger orientation
			float s=2;
			gl2.glBegin(GL2.GL_LINES);
			gl2.glColor3f(1,1,1);
			gl2.glVertex3f(motionNow.fingerPosition.x, motionNow.fingerPosition.y, motionNow.fingerPosition.z);
			gl2.glVertex3f(motionNow.fingerPosition.x+motionNow.finger_forward.x*s,
					       motionNow.fingerPosition.y+motionNow.finger_forward.y*s,
					       motionNow.fingerPosition.z+motionNow.finger_forward.z*s);
			gl2.glVertex3f(motionNow.fingerPosition.x, motionNow.fingerPosition.y, motionNow.fingerPosition.z);
			gl2.glVertex3f(motionNow.fingerPosition.x+motionNow.finger_up.x*s,
				       motionNow.fingerPosition.y+motionNow.finger_up.y*s,
				       motionNow.fingerPosition.z+motionNow.finger_up.z*s);
			gl2.glVertex3f(motionNow.fingerPosition.x, motionNow.fingerPosition.y, motionNow.fingerPosition.z);
			gl2.glVertex3f(motionNow.fingerPosition.x+motionNow.finger_left.x*s,
				       motionNow.fingerPosition.y+motionNow.finger_left.y*s,
				       motionNow.fingerPosition.z+motionNow.finger_left.z*s);
			
			gl2.glEnd();
		}

		if(draw_base_star) {
	 		// draw finger orientation
			float s=2;
			gl2.glDisable(GL2.GL_DEPTH_TEST);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glColor3f(1,0,0);
			gl2.glVertex3f(motionNow.base.x, motionNow.base.y, motionNow.base.z);
			gl2.glVertex3f(motionNow.base.x+motionNow.baseForward.x*s,
					       motionNow.base.y+motionNow.baseForward.y*s,
					       motionNow.base.z+motionNow.baseForward.z*s);
			gl2.glColor3f(0,1,0);
			gl2.glVertex3f(motionNow.base.x, motionNow.base.y, motionNow.base.z);
			gl2.glVertex3f(motionNow.base.x+motionNow.baseUp.x*s,
				       motionNow.base.y+motionNow.baseUp.y*s,
				       motionNow.base.z+motionNow.baseUp.z*s);
			gl2.glColor3f(0,0,1);
			gl2.glVertex3f(motionNow.base.x, motionNow.base.y, motionNow.base.z);
			gl2.glVertex3f(motionNow.base.x+motionNow.finger_left.x*s,
				       motionNow.base.y+motionNow.finger_left.y*s,
				       motionNow.base.z+motionNow.finger_left.z*s);
			
			gl2.glEnd();
			gl2.glEnable(GL2.GL_DEPTH_TEST);
		}
		
		gl2.glEnable(GL2.GL_LIGHTING);
		
		gl2.glPopMatrix();
	}
	

	public void setModeAbsolute() {
		if(connection!=null) this.sendLineToRobot("G90");
	}
	
	
	public void setModeRelative() {
		if(connection!=null) this.sendLineToRobot("G91");
	}
	

	@Override
	// override this method to check that the software is connected to the right type of robot.
	public void dataAvailable(AbstractConnection arg0,String line) {
		if(line.contains(hello)) {
			isPortConfirmed=true;
			//finalizeMove();
			setModeAbsolute();
			this.sendLineToRobot("R1");
			
			String ending = line.substring(hello.length());
			String uidString=ending.substring(ending.indexOf('#')+1).trim();
			System.out.println(">>> UID="+uidString);
			try {
				long uid = Long.parseLong(uidString);
				if(uid==0) {
					robotUID = getNewRobotUID();
				} else {
					robotUID = uid;
				}
				updateGUI();
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			setDisplayName(ROBOT_NAME+" #"+robotUID);
		}
	}
	

	/**
	 * based on http://www.exampledepot.com/egs/java.net/Post.html
	 */
	private long getNewRobotUID() {
		long new_uid = 0;

		if(just_testing_dont_get_uid==true) {
			try {
				// Send data
				URL url = new URL("https://marginallyclever.com/stewart_platform_getuid.php");
				URLConnection conn = url.openConnection();
				try (
						final InputStream connectionInputStream = conn.getInputStream();
						final Reader inputStreamReader = new InputStreamReader(connectionInputStream);
						final BufferedReader rd = new BufferedReader(inputStreamReader)
						) {
					String line = rd.readLine();
					new_uid = Long.parseLong(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
	
			// did read go ok?
			if (new_uid != 0) {
				// make sure a topLevelMachinesPreferenceNode node is created
				// tell the robot it's new UID.
				this.sendLineToRobot("UID " + new_uid);
			}
		}
		return new_uid;
	}
	
	
	public boolean isPortConfirmed() {
		return isPortConfirmed;
	}
	
	
	public BoundingVolume [] getBoundingVolumes() {
		// TODO finish me
		return volumes;
	}
	
	
	Vector3f getWorldCoordinatesFor(Vector3f in) {
		Vector3f out = new Vector3f(motionFuture.base);
		
		Vector3f tempx = new Vector3f(motionFuture.baseForward);
		tempx.scale(in.x);
		out.add(tempx);

		Vector3f tempy = new Vector3f(motionFuture.baseRight);
		tempy.scale(-in.y);
		out.add(tempy);

		Vector3f tempz = new Vector3f(motionFuture.baseUp);
		tempz.scale(in.z);
		out.add(tempz);
				
		return out;
	}

	
	@Override
	public ArrayList<JPanel> getControlPanels() {
		ArrayList<JPanel> list = super.getControlPanels();
		
		if(list==null) list = new ArrayList<JPanel>();

		rspPanel = new RotaryStewartPlatform2ControlPanel(this);
		
		list.add(rspPanel);
		updateGUI();
		
		return list;
	}
	

	protected float roundOff(float v) {
		float SCALE = 1000.0f;
		
		return Math.round(v*SCALE)/SCALE;
	}
	
	
	public void updateGUI() {
		// TODO rotate fingerPosition before adding position

		rspPanel.xPos.setText(Float.toString(roundOff(motionNow.fingerPosition.x)));
		rspPanel.yPos.setText(Float.toString(roundOff(motionNow.fingerPosition.y)));
		rspPanel.zPos.setText(Float.toString(roundOff(motionNow.fingerPosition.z)));

		rspPanel.uPos.setText(Float.toString(roundOff(motionNow.iku)));
		rspPanel.vPos.setText(Float.toString(roundOff(motionNow.ikv)));
		rspPanel.wPos.setText(Float.toString(roundOff(motionNow.ikw)));

		//if( tool != null ) tool.updateGUI();
	}
	
	private void sendChangeToRealMachine() {
		if(isPortConfirmed()==false) return;
		
		this.sendLineToRobot("G0 X"+roundOff(motionNow.fingerPosition.x)
		          +" Y"+roundOff(motionNow.fingerPosition.y)
		          +" Z"+roundOff(motionNow.fingerPosition.z)
		          +" U"+roundOff(motionNow.iku)
		          +" V"+roundOff(motionNow.ikv)
		          +" W"+roundOff(motionNow.ikw)
		          );
	}
	
	
	public void goHome() {
		motionFuture.isHomed=false;
		this.sendLineToRobot("G28");
		motionFuture.fingerPosition.set(HOME_X,HOME_Y,HOME_Z);  // HOME_* should match values in robot firmware.
		motionFuture.iku=0;
		motionFuture.iku=0;
		motionFuture.iku=0;
		motionFuture.isHomed=true;
		motionFuture.updateIK();
		motionNow.set(motionFuture);
		motionNow.updateIK();
		updateGUI();
		
		//finalizeMove();
		//this.sendLineToRobot("G92 X"+HOME_X+" Y"+HOME_Y+" Z"+HOME_Z);
	}
}
