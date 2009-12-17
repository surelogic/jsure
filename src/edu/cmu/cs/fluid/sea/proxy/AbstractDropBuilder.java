/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.proxy;

import java.text.MessageFormat;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.*;

public abstract class AbstractDropBuilder {
	final String type;
	private boolean isValid = true;
	private IRNode node;
	private String message;
	private Category category;
	private Drop resultDependUponDrop;
	
	AbstractDropBuilder(String type) {		
		this.type = type;
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	public void invalidate() {
		isValid = false;
	}
	
	public IRNode getNode() {
		return node;
	}
	
	public void setNode(IRNode n) {
		node = n;
	}
	
	public void setMessage(String msg, Object... args) {
		message = (args.length == 0) ? msg : 
			MessageFormat.format(msg, args);
	}
	
	public void setCategory(Category c) {
		category = c;
	}
	
	public void setResultDependUponDrop(Drop drop) {
		resultDependUponDrop = drop;
	}
	
	void buildDrop(IRReferenceDrop d) {
		d.setNode(node);
		d.setMessage(message);
		d.setCategory(category);
		resultDependUponDrop.addDependent(d);
	}
	
	public abstract Drop build();
}
