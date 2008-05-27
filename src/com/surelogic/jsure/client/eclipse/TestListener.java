package com.surelogic.jsure.client.eclipse;

import java.util.*;

import com.surelogic.jsure.xml.*;
import static com.surelogic.jsure.xml.JSureXMLReader.*;

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
