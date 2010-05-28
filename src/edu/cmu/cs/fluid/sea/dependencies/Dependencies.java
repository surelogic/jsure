/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.dependencies;

import java.util.*;

import com.surelogic.promise.PromiseDropStorage;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;

public class Dependencies {
	/**
	 * To avoid cycles and duplication
	 */
	private final Set<Drop> checkedDependents = new HashSet<Drop>();
	private final Set<Drop> checkedDeponents = new HashSet<Drop>();
	/**
	 * The set of CUDrops that need to be reprocessed for promises
	 */
	private final Set<CUDrop> reprocess = new HashSet<CUDrop>();
	/**
	 * The set of changed CUDrops
	 */
	private final Set<CUDrop> changed = new HashSet<CUDrop>();
	
	public void markAsChanged(CUDrop d) {
		if (d == null) {
			return; // Nothing to do
		}
		changed.add(d);
		collect(d);
	}
	
	/**
	 * Collects the CUDrops corresponding to d's dependent drops, 
	 * so we can reprocess the promises on those
	 */	
	private void collect(Drop root) {
		if (checkedDependents.contains(root)) {
			return;
		}
		checkedDependents.add(root);
		
		// Find dependent drops
		for(Drop d : root.getDependents()) {
			//System.out.println(root+" <- "+d);
			findEnclosingCUDrop(d);
			collect(d);
		}				
	}
	
	private void findEnclosingCUDrop(Drop d) {
		if (d instanceof IRReferenceDrop) {
			IRReferenceDrop ird = (IRReferenceDrop) d;
			IRNode cu = VisitUtil.getEnclosingCompilationUnit(ird.getNode());
			CUDrop cud = CUDrop.queryCU(cu);
			if (cud != null) {
				//System.out.println(cud+" <- "+d);
				reprocess.add(cud);
			} else {
				//System.out.println("No CUDrop for "+d);
			}
		} else {
			//System.out.println("Not an IRRefDrop: "+d);
			// TODO ignore these?
		}
	}
	
	/**
	 * Recursively check this drop and its deponents for CUDrops
	 * 
	 * Note: this does what findEnclosingCUDrop() does, and more (too much)
	 */
//	private void findCUDropDeponents(Drop d) {
//		if (checkedDeponents.contains(d)) {
//			return;
//		}
//		checkedDeponents.add(d);
//		if (d instanceof CUDrop) {
//			System.out.println("Reprocessing "+d);
//			reprocess.add((CUDrop) d);				
//			collect(d);
//			/*
//			if (d instanceof PackageDrop) {
//				// I need to reprocess these if the package changed
//				for(Drop dd : d.getDependents()) {
//					if (dd instanceof CUDrop) {
//						reprocess.add((CUDrop) dd);
//					}
//				}
//			}
//			*/
//			return; // No need to look at deponents
//		}
//		for(Drop deponent : d.getDeponents()) {
//			System.out.println(d+" -> "+deponent);
//			findCUDropDeponents(deponent);
//		}
//	}
//	
	/**
	 * Collect CU deponents of promise warnings
	 */
	private void processPromiseWarningDrops() {
		for(Drop d : Sea.getDefault().getDropsOfType(PromiseWarningDrop.class)) {				
			findEnclosingCUDrop(d);
		}
	}
	
	public void finish() {
		processPromiseWarningDrops();
		
		reprocess.removeAll(changed);						
		/*
		for(CUDrop d : changed) {
			System.out.println("Changed:   "+d.javaOSFileName+" "+d.getClass().getSimpleName());
		}		
		for(CUDrop d : reprocess) {
			System.out.println("Reprocess: "+d.javaOSFileName+" "+d.getClass().getSimpleName());
		}
		*/
		IDE.getInstance().setAdapting();
		try {
			for(CUDrop d : reprocess) {
				clearPromiseDrops(d);
				if (d instanceof PackageDrop) {
					final PackageDrop pkg = (PackageDrop) d;						
					for(Drop dependent : pkg.getDependents()) {
						dependent.invalidate();
					}						
					handlePackage(pkg);
					/*
					runVersioned(new AbstractRunner() {
						public void run() {
							parsePackagePromises(pkg);
						}
					});					
					*/
				}
			}
			// Necessary to process these after package drops 
			// to ensure that newly created drops don't invalidated
			for(CUDrop d : reprocess) {
				// Already cleared above
				if (!(d instanceof PackageDrop)) {	
					handleType(d);
					//ConvertToIR.getInstance().registerClass(d.makeCodeInfo());
				}
			}
		} finally {
			IDE.getInstance().clearAdapting();
		}
	}
	
	protected void handlePackage(PackageDrop d) {
		// Nothing to do
	}
	
	protected void handleType(CUDrop d) {
		// Nothing to do
	}
	
	private void clearPromiseDrops(CUDrop d) {
		// Clear out promise drops
		//System.out.println("Reprocessing "+d.javaOSFileName);
		for(IRNode n : JavaPromise.bottomUp(d.cu)) {
			PromiseDropStorage.clearDrops(n);
		}
	}
}
