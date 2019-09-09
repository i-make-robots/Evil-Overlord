package com.marginallyclever.robotOverlord.gridEntity;

import java.util.ArrayList;

import javax.swing.JPanel;

import com.jogamp.opengl.GL2;
import com.marginallyclever.convenience.PrimitiveSolids;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.entity.EntityControlPanel;
import com.marginallyclever.robotOverlord.physicalObject.PhysicalObject;

public class GridEntity extends PhysicalObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public double width=100;
	public double height=100;
	
	protected transient GridEntityControlPanel gridEntityControlPanel;

	public GridEntity() {
		super();
		setDisplayName("Grid");
	}
	
	
	/**
	 * Get the {@link EntityControlPanel} for this class' superclass, then the EntityPanel for this class, and so on.
	 * 
	 * @param gui the main application instance.
	 * @return the list of EntityPanels 
	 */
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = new ArrayList<JPanel>();
		
		gridEntityControlPanel = new GridEntityControlPanel(gui,this);
		list.add(gridEntityControlPanel);

		return list;
	}

	@Override
	public void render(GL2 gl2) {
		PrimitiveSolids.drawGrid(gl2,(int)width,(int)height,1);
	}
}