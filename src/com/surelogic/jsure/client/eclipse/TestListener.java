package com.surelogic.jsure.client.eclipse;

import com.surelogic.jsure.xml.*;

public class TestListener extends AbstractXMLResultListener {
	@Override
	protected void define(Entity e) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void handleDanglingRef(String from, Entity to) {
		// TODO handle
		System.out.println("Handled "+to+" ref from "+from+" to "+to.getId());
	}	
	/*
	@Override
	public void done() {
	}
	*/	
}
