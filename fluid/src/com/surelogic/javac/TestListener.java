package com.surelogic.javac;

import org.xml.sax.Attributes;

import com.surelogic.common.xml.*;
import com.surelogic.dropsea.irfree.AbstractXmlResultListener;

public class TestListener extends AbstractXmlResultListener {
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
	@Override
  public Entity makeEntity(String name, Attributes a) {
		return new Entity(name, a);
	}
}
