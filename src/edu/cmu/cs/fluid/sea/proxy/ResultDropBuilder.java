package edu.cmu.cs.fluid.sea.proxy;

import java.util.*;

import com.surelogic.analysis.IIRAnalysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Temporary builder to help analyses be parallelized
 *
 * @author Edwin
 */
@SuppressWarnings("unchecked")
public final class ResultDropBuilder extends AbstractDropBuilder {
	private List<Pair<String,IRNode>> supportingInfos =
		new ArrayList<Pair<String,IRNode>>();
	
	private boolean isConsistent = false;
	private Set<PromiseDrop> checks = new HashSet<PromiseDrop>();
	private Set<PromiseDrop> trusted = new HashSet<PromiseDrop>();
	
	private ResultDropBuilder(String type) {		
		super(type);
	}
	
	public static ResultDropBuilder create(IIRAnalysis a, String type) {
		ResultDropBuilder rv = new ResultDropBuilder(type);
		a.handleBuilder(rv);
		return rv;
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
	
	public ResultDrop build() {
		if (!isValid()) {
			return null;
		}
		ResultDrop rd = new ResultDrop(type);		
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
		buildDrop(rd);
		return rd;
	}
}
