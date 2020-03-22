package com.marginallyclever.robotOverlord.entity.scene.dhRobotEntity.sixi2;

import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import com.marginallyclever.communications.NetworkConnection;
import com.marginallyclever.communications.NetworkConnectionListener;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotOverlord.entity.Entity;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.RemoteEntity;
import com.marginallyclever.robotOverlord.entity.scene.dhRobotEntity.DHKeyframe;
import com.marginallyclever.robotOverlord.entity.scene.modelEntity.ModelEntity;

public class SixiJoystick extends ModelEntity implements NetworkConnectionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9009274404516671409L;
	
	private Sixi2 target;
	
	private RemoteEntity connection = new RemoteEntity();
	
	private ReentrantLock lock = new ReentrantLock();
	
	DHKeyframe keyframe;
	
	public SixiJoystick() {
		setName("Sixi Joystick");
		addChild(connection);
	}
	
	// TODO this is trash.  if robot is deleted this link would do what, exactly?
	// What if there was more than one Sixi?  More than one joystick?
	protected Sixi2 findRobot() {
		for( Entity e : getWorld().getChildren() ) {
			if(e instanceof Sixi2) {
				return (Sixi2)e;
			}
		}
		return null;
	}
	
	@Override
	public void lineError(NetworkConnection arg0, int lineNumber) {}
	
	@Override
	public void sendBufferEmpty(NetworkConnection arg0) {}
	
	@Override
	// data arrives asynchronously, so make sure to 
	public void dataAvailable(NetworkConnection arg0, String data) {
		if(lock.isLocked()) return;
		lock.lock();
		try {
			if(target==null) {
				target = findRobot();
			}
			if(target!=null) {
				if(keyframe == null) {
					 keyframe = target.sim.getIKSolver().createDHKeyframe();
				}
				StringTokenizer tokenizer = new StringTokenizer(data);
				if(tokenizer.countTokens()<6) return;
				
				for(int i=0;i<6;++i) {
					double d = StringHelper.parseNumber(tokenizer.nextToken());
					keyframe.fkValues[i]=Math.max(Math.min(d,180),-180);
				}
				keyframe.fkValues[1]*=-1;
				keyframe.fkValues[4]*=-1;
			}
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void update(double dt) {
		if(target==null) return;
		if(lock.isLocked()) return;

		lock.lock();
		target.sim.setPoseFK(keyframe);
		lock.unlock();
	}
}
