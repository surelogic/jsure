package com.surelogic.jsure.core;

import org.xml.sax.Attributes;

import com.surelogic.common.xml.*;

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
	public Entity makeEntity(String name, Attributes a) {
		return new Entity(name, a);
	}
}
