package com.marginallyclever.robotOverlord.entity.scene;

import java.util.Observable;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.convenience.PrimitiveSolids;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.DoubleEntity;
import com.marginallyclever.robotOverlord.swingInterface.InputManager;
import com.marginallyclever.robotOverlord.swingInterface.view.ViewPanel;
import com.jogamp.opengl.GL2;

/**
 * Camera in the world.  Has no physical presence.  Has location and direction.
 * @author Dan Royer
 */
public class CameraEntity extends PoseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2042647908029220648L;
		
	// angles
	protected DoubleEntity pan = new DoubleEntity("Pan",0);
	protected DoubleEntity tilt = new DoubleEntity("Tilt",0);
	protected DoubleEntity zoom = new DoubleEntity("Zoom",100);

	// snap system
	protected DoubleEntity snapDeadZone = new DoubleEntity("Snap dead zone",100);
	protected DoubleEntity snapDegrees = new DoubleEntity("Snap degrees",45);
	protected transient boolean hasSnappingStarted=false;
	protected transient double sumDx;
	protected transient double sumDy;
	
	
	public CameraEntity() {
		super();
		setName("Camera");
		
		addChild(snapDeadZone);
		addChild(snapDegrees);
		
		pan.addObserver(this);
		tilt.addObserver(this);
	}
	
	protected Matrix3d buildPanTiltMatrix(double panDeg,double tiltDeg) {
		Matrix3d a = new Matrix3d();
		Matrix3d b = new Matrix3d();
		Matrix3d c = new Matrix3d();
		a.rotZ(Math.toRadians(panDeg));
		b.rotX(Math.toRadians(-tiltDeg));
		c.mul(b,a);
		c.transpose();
		
		return c;
	}
	
	@Override
	public void update(Observable o, Object arg) {
		setRotation(buildPanTiltMatrix(pan.get(),tilt.get()));
		super.update(o, arg);
	}

	@Override
	public void update(double dt) {
		// Move the camera		
        double dz = InputManager.getRawValue(InputManager.Source.MOUSE_Z);
        if(dz!=0) { 
        	double oldZoom = zoom.get();
        	double newZoom = oldZoom;
        	
        	newZoom -= dz*3;
        	newZoom = Math.max(1,newZoom);

        	if(oldZoom!=newZoom) {
        		zoom.set(newZoom);
				// adjust the camera position to orbit around a point 'zoom' in front of the camera
				Vector3d oldZ = MatrixHelper.getZAxis(pose);
				Vector3d newZ = new Vector3d(oldZ); 

				oldZ.scale(oldZoom);
				newZ.scale(zoom.get());
	
				Vector3d p = getPosition();
				p.sub(oldZ);
				p.add(newZ);
				setPosition(p);
        	}
        	//Log.message(dz+"\t"+zoom);
        }
        
		if (InputManager.isOn(InputManager.Source.MOUSE_MIDDLE)) {
			double scale = 1;
	        double dx = InputManager.getRawValue(InputManager.Source.MOUSE_X);
	        double dy = InputManager.getRawValue(InputManager.Source.MOUSE_Y);
	        dx = dx *scale;
	        dy = dy *scale;

			if(dx!=0 || dy!=0) {
				// snap system
		        boolean isSnapHappeningNow=
		        		(InputManager.isOn(InputManager.Source.KEY_LALT) || InputManager.isOn(InputManager.Source.KEY_RALT));
		        if(isSnapHappeningNow) {
		        	if(!hasSnappingStarted) {
						sumDx=0;
						sumDy=0;
						hasSnappingStarted=true;
					}
				}
		        hasSnappingStarted = isSnapHappeningNow;
		        //Log.message("Snap="+isSnapHappeningNow);
				
		        //
				if( InputManager.isOn(InputManager.Source.KEY_LSHIFT) ||
					InputManager.isOn(InputManager.Source.KEY_RSHIFT) ) {
					// translate relative to camera's current orientation
					Vector3d vx = MatrixHelper.getXAxis(pose);
					Vector3d vy = MatrixHelper.getYAxis(pose);
					Vector3d p = getPosition();
					double zSq = Math.sqrt(zoom.get())*0.01;
					vx.scale(zSq*-dx);
					vy.scale(zSq* dy);
					p.add(vx);
					p.add(vy);
					setPosition(p);
					
					//Log.message(dx+"\t"+dy+"\t"+zoom+"\t"+zSq);
				} else if(InputManager.isOn(InputManager.Source.KEY_LCONTROL) ||
						  InputManager.isOn(InputManager.Source.KEY_RCONTROL) ) {
					// up and down to fly forward and back
					Vector3d zAxis = MatrixHelper.getZAxis(pose);
					zAxis.scale(dy);
					
					Vector3d p = getPosition();
					p.add(zAxis);
					setPosition(p);
				} else if( isSnapHappeningNow ) {
					sumDx+=dx;
					sumDy+=dy;
					if(Math.abs(sumDx)>snapDeadZone.get() || Math.abs(sumDy)>snapDeadZone.get()) {
						double degrees = snapDegrees.get();
						if(Math.abs(sumDx) > Math.abs(sumDy)) {
							double a=getPan();
							if(sumDx>0)	a+=degrees;	// snap CCW
							else		a-=degrees;	// snap CW
							setPan(Math.round(a/degrees)*degrees);
						} else {
							double a=getTilt();
							if(sumDy>0)	a-=degrees;	// snap down
							else		a+=degrees;	// snap up
							setTilt(Math.round(a/degrees)*degrees);
						}
						
						Matrix3d rot = buildPanTiltMatrix(pan.get(),tilt.get());
						setRotation(rot);
						sumDx=0;
						sumDy=0;
					}
				} else {
					double z = zoom.get();
					
					// adjust the camera position to orbit around a point 'zoom' in front of the camera
					// relies on the Z axis of the matrix BEFORE any rotations are applied.
					Vector3d oldZ = MatrixHelper.getZAxis(pose);
					oldZ.scale(z);
					
					// orbit around the focal point
					setPan(getPan()+dx);
					setTilt(getTilt()-dy);

					// do updateMatrix() but keep the rotation matrix
					Matrix3d rot = buildPanTiltMatrix(pan.get(),tilt.get());
					setRotation(rot);

					// get the new Z axis
					Vector3d newZ = new Vector3d(rot.m02,rot.m12,rot.m22);
					newZ.scale(z);

					// adjust position according to zoom (aka orbit) distance.
					Vector3d p = getPosition();
					p.sub(oldZ);
					p.add(newZ);
					setPosition(p);
					
					//Log.message(dx+"\t"+dy+"\t"+pan+"\t"+tilt+"\t"+oldZ+"\t"+newZ);
				}
			}
		} 
			// CONTROLLER
		if(!InputManager.isOn(InputManager.Source.STICK_X) && !InputManager.isOn(InputManager.Source.STICK_CIRCLE)) {
			double rawxl = InputManager.getRawValue(InputManager.Source.STICK_LX);
			double rawyl = InputManager.getRawValue(InputManager.Source.STICK_LY);
			double rawzl = InputManager.getRawValue(InputManager.Source.STICK_L2);
			
			double rawxr = InputManager.getRawValue(InputManager.Source.STICK_RX);
			double rawyr = InputManager.getRawValue(InputManager.Source.STICK_RY);
			
			double scale = 50.0*dt;  // TODO something better?
			double dxl = rawxl * -scale;
			double dyl = rawyl * -scale;
			double dzl = rawzl * scale;
			
			double dxr = rawxr * scale;
			double dyr = rawyr * scale;
			
			Vector3d vx = MatrixHelper.getXAxis(pose);
			Vector3d vy = MatrixHelper.getYAxis(pose);
			Vector3d vz = MatrixHelper.getZAxis(pose);
			Vector3d p = getPosition();
			
			// orbit around the focal point
			setPan(getPan()+dxr);
			setTilt(getTilt()-dyr);
			// do updateMatrix() but keep the rotation matrix
			Matrix3d rot = buildPanTiltMatrix(pan.get(),tilt.get());
			setRotation(rot);

			// adjust the camera position to orbit around a point 'zoom' in front of the camera
			Vector3d oldZ = MatrixHelper.getZAxis(pose);
			oldZ.scale(zoom.get());
			Vector3d newZ = new Vector3d(rot.m02,rot.m12,rot.m22);
			newZ.scale(zoom.get());

			p.sub(oldZ);
			p.add(newZ);
			
//			double zSq = Math.sqrt(zoom.get())*0.01;
			double zSq = 1;
			vx.scale(zSq*-dxl);
			vy.scale(zSq* dyl);
			vz.scale(dzl);
			p.add(vx);
			p.add(vy);
			p.add(vz);
			setPosition(p);
		}
	}
	
	// OpenGL camera: -Z=forward, +X=right, +Y=up
	@Override
	public void render(GL2 gl2) {
		gl2.glPushMatrix();
			MatrixHelper.applyMatrix(gl2, pose);
			PrimitiveSolids.drawStar(gl2, 10);
		gl2.glPopMatrix();
	}
	
	public double getPan() {
		return pan.get();
	}
	
	public double getTilt() {
		return tilt.get();
	}
	
	public void setPan(double arg0) {
		//arg0 = Math.min(Math.max(arg0, 0), 360);
		pan.set(arg0);
	}
	
	public void setTilt(double arg0) {
		arg0 = Math.min(Math.max(arg0, 1), 179);
		tilt.set(arg0);
	}
	
	public void setZoom(double arg0) {
		arg0 = Math.min(Math.max(arg0, 0), 500);
		zoom.set(arg0);
	}
	
	public double getZoom() {
		return zoom.get();
	}
	
	@Override
	public void getView(ViewPanel view) {
		view.pushStack("Ca", "Camera");
		view.add(snapDeadZone);
		view.add(snapDegrees);
		view.add(pan).setReadOnly(true);
		view.add(tilt).setReadOnly(true);
		view.add(zoom).setReadOnly(true);
		view.popStack();
		super.getView(view);
	}
}
