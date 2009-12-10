package edu.cmu.cs.fluid.sea.proxy;

import java.text.MessageFormat;
import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Temporary builder to help analyses be parallelized
 *
 * @author Edwin
 */
@SuppressWarnings("unchecked")
public class ResultDropBuilder {
	private final String type;
	private boolean isValid = true;
	private IRNode node;
	private String message;
	private Category category;
	private List<Pair<String,IRNode>> supportingInfos =
		new ArrayList<Pair<String,IRNode>>();
	
	private boolean isConsistent = false;
	private Set<PromiseDrop> checks = new HashSet<PromiseDrop>();
	private Set<PromiseDrop> trusted = new HashSet<PromiseDrop>();
	private Drop resultDependUponDrop;
	
	public ResultDropBuilder(String type) {
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
	
	public void addSupportingInformation(String msg, IRNode context) {
		supportingInfos.add(new Pair<String,IRNode>(msg, context));
	}
	
	public void setConsistent() {
		isConsistent = true;
	}
	
	public void setInconsistent() {
		isConsistent = false;
	}
	
	/**
	 * Adds a promise to the set of promises this result establishes, or
	 * <i>checks</i>.
	 * 
	 * @param promise
	 *            the promise being supported by this result
	 */
	public void addCheckedPromise(PromiseDrop promise) {
		checks.add(promise);
	}
	
	/**
	 * @return the set of promise drops established, or checked, by this result.
	 *         All members of the returned set will are of the PromiseDrop type.
	 */
	public Set<? extends PromiseDrop> getChecks() {
		return checks;
	}

	public void addTrustedPromise(PromiseDrop promise) {
		trusted.add(promise);
	}

	public void setResultDependUponDrop(Drop drop) {
		resultDependUponDrop = drop;
	}
	
	public ResultDrop build() {
		if (!isValid()) {
			return null;
		}
		ResultDrop rd = new ResultDrop(type);
		rd.setNode(node);
		rd.setMessage(message);
		rd.setCategory(category);
		for(Pair<String,IRNode> p : supportingInfos) {
			rd.addSupportingInformation(p.first(), p.second());
		}
		
		rd.setConsistent(isConsistent);
		for(PromiseDrop check : checks) {
			rd.addCheckedPromise(check);
		}
		for(PromiseDrop t : trusted) {
			rd.addTrustedPromise(t);
		}		
		resultDependUponDrop.addDependent(rd);
		return rd;
	}
}
