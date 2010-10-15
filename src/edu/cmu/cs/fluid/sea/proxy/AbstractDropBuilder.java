/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.proxy;

import java.text.MessageFormat;
import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.util.Pair;
import edu.cmu.cs.fluid.util.Triple;

public abstract class AbstractDropBuilder {
	final String type;
	private boolean isValid = true;
	private IRNode node;
	private String message;
	private int messageNum;
	private Object[] args;
	private Category category;
	private List<Drop> dependUponDrops = new ArrayList<Drop>();
	private List<Pair<String,IRNode>> supportingInfos =
		new ArrayList<Pair<String,IRNode>>();	
	private List<Triple<IRNode,Integer,Object[]>> supportingInfos2 =
		new ArrayList<Triple<IRNode,Integer,Object[]>>();
	private List<ProposedPromiseBuilder> proposals = 
		new ArrayList<ProposedPromiseBuilder>();
	
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
	
	public void setNodeAndCompilationUnitDependency(IRNode node) {
		setNode(node);
	}
	
	public void setMessage(String msg, Object... args) {
		message = (args.length == 0) ? msg : 
			MessageFormat.format(msg, args);
		messageNum = -1;
	}
	
	public void setResultMessage(int num, Object... args) {
		messageNum = num;
		this.args = args;
	}
	
	public void setCategory(Category c) {
		category = c;
	}
	
	public void addDependUponDrop(Drop drop) {
		if (drop == null) {
			throw new IllegalArgumentException();
		}
		dependUponDrops.add(drop);
	}
	
	public void addSupportingInformation(String msg, IRNode context) {
		supportingInfos.add(new Pair<String,IRNode>(msg, context));
	}
	
	public void addSupportingInformation(IRNode context, int num, Object... args) {
		supportingInfos2.add(new Triple<IRNode,Integer,Object[]>(context, num, args));
	}
	
	public void addProposal(ProposedPromiseBuilder p) {
		proposals.add(p);
	}
	
	int buildDrop(IRReferenceDrop d) {
		int num = 1;
		//System.out.println("Making: "+message);
		d.setNodeAndCompilationUnitDependency(node);
		if (messageNum < 0) {
			d.setMessage(message);
		} else {
			d.setResultMessage(messageNum, args);
		}
		d.setCategory(category);
		for(Drop deponent : dependUponDrops) {
			deponent.addDependent(d);
		}
		for(Pair<String,IRNode> p : supportingInfos) {
			d.addSupportingInformation(p.first(), p.second());
		}
		for(Triple<IRNode,Integer,Object[]> p : supportingInfos2) {
			d.addSupportingInformation(p.first(), p.second(), p.third());
		}
		for(ProposedPromiseBuilder p : proposals) {
			d.addProposal(p.build());
			num++;
		}
		return num;
	}
	
	public abstract int build();
}
