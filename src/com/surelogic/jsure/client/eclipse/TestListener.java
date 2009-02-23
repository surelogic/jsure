package com.surelogic.jsure.client.eclipse;

import com.surelogic.jsure.xml.*;

public class TestListener extends AbstractXMLResultListener {
	@Override
	protected boolean define(int id, Entity e) {
		// TODO Auto-generated method stub
	  return true;
	}

	@Override
	protected void handleRef(String from, int fromId, Entity to) {
		// TODO handle
		System.out.println("Handled "+to+" ref from "+from+" to "+to.getId());
	}	
	/*
	@Override
	public void done() {
	}
	*/	
}
