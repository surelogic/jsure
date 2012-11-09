package com.surelogic.dropsea.irfree;

import org.xml.sax.Attributes;

import com.surelogic.dropsea.irfree.Entity;

public interface IXmlResultListener {
	/**
	 * Called upon seeing the root element
	 */
	void start(String uid, String project);
	
	/**
	 * Called for each top-level entity	 
	 */
	void notify(Entity e);
	
	/**
	 * Called upon seeing the end of the root element
	 */
	void done();
	
	/**
	 * Factory method for creating different Entity subclasses,
	 * depending on the name/attributes
	 */
	Entity makeEntity(String name, Attributes a);
}
