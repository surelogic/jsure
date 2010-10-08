/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.dependencies;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.collections15.*;
import org.apache.commons.collections15.multimap.*;

import com.surelogic.promise.PromiseDropStorage;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * TODO Assumes that all dependencies are the same, and doesn't distinguish between 
 * what different analyses need (which could allow for less reprocessing)
 * 
 * @author Edwin
 */
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

	private Collection<IRNode> findNewAnnotations() {
		// 1. record old decls and what annotations were on them (no easy way to do this?)
		// (esp. w/ scoped promises)
		//    Decls as Strings for field/method decls
		// 2. compare with new decls, eliminating those that didn't appear before
		// 3. compare the annotations on the remaining decls, eliminating those that "existed" before
		return null;
	}
	
	/**
	 * Find uses of the given declarations, and add their CUDrops to the queue to
	 * be reprocessed
	 * 
	 * @param decls A sequence of existing declarations with new annotations
	 */
	public void scanForDependencies(ITypeEnvironment te, Iterable<IRNode> decls) {		
		// Categorize decls by access
		final MultiMap<IRNode,IRNode> packageDecls = new MultiHashMap<IRNode,IRNode>();
		final MultiMap<IRNode,IRNode> protectedDecls = new MultiHashMap<IRNode,IRNode>();
		final Set<IRNode> publicDecls = new HashSet<IRNode>();
		for(IRNode decl : decls) {
			// TODO not quite right for some
			final int mods = JavaNode.getModifiers(decl); 
			if (JavaNode.isSet(mods, JavaNode.PRIVATE)) {
				continue; // Nothing to do?
			}
			else if (JavaNode.isSet(mods, JavaNode.PROTECTED)) {
				final IRNode type = VisitUtil.getEnclosingType(decl);
				final IRNode root = VisitUtil.findCompilationUnit(decl);				
				protectedDecls.put(type, decl);
				packageDecls.put(root, decl);
			}
			else if (JavaNode.isSet(mods, JavaNode.PUBLIC)) {
				publicDecls.add(decl);
			}
			else { // package
				final IRNode root = VisitUtil.findCompilationUnit(decl);
				packageDecls.put(root, decl);
			}
		}
		if (!packageDecls.isEmpty()) {
			scanForPackageDependencies(te, packageDecls);
		}
		if (!protectedDecls.isEmpty()) {
			scanForSubclassDependencies(te, protectedDecls);
		}
		if (!publicDecls.isEmpty()) {
			scanForPublicDependencies(te, publicDecls);
		}
	}
	
	/**
	 * Look for dependencies in the same package as the decl
	 * @param te
	 * @param decls
	 */
	private void scanForPackageDependencies(ITypeEnvironment te, MultiMap<IRNode,IRNode> cu2decls) {
		for(final Entry<IRNode, Collection<IRNode>> e : cu2decls.entrySet()) {
			final Set<IRNode> decls = new HashSet<IRNode>(e.getValue());
			final String name       = VisitUtil.getPackageName(e.getKey());
			// TODO does this have the right info?
			final PackageDrop pd = PackageDrop.findPackage(name);			
			for(CUDrop cud : pd.getCUDrops()) {
				scanCUDropForDependencies(te.getBinder(), cud, decls);
			}
		}
	}

	private void scanForSubclassDependencies(ITypeEnvironment te, MultiMap<IRNode,IRNode> type2decls) {
		for(Entry<IRNode, Collection<IRNode>> e : type2decls.entrySet()) {		
			final IRNode type       = e.getKey();
			final Set<IRNode> decls = new HashSet<IRNode>(e.getValue());
			scanForSubclassDependencies(te, type, decls);
		}
	}
	
	private void scanForSubclassDependencies(ITypeEnvironment te, IRNode type, Set<IRNode> decls) {
		for(IRNode sub : te.getRawSubclasses(type)) {
			// TODO check if already on the list first?
			final IRNode cu  = VisitUtil.findCompilationUnit(sub);
			final CUDrop cud = CUDrop.queryCU(cu);
			scanCUDropForDependencies(te.getBinder(), cud, decls);
			scanForSubclassDependencies(te, sub, decls);
		}	
	}
	
	private void scanForPublicDependencies(ITypeEnvironment te, Set<IRNode> decls) {
		// TODO do i need to check binaries?
		final Set<SourceCUDrop> allCus = Sea.getDefault().getDropsOfExactType(SourceCUDrop.class);
		for(CUDrop cud : allCus) {
			scanCUDropForDependencies(te.getBinder(), cud, decls);
		}
	}
	
	private void scanCUDropForDependencies(IBinder binder, CUDrop cud, Set<IRNode> decls) {
		if (reprocess.contains(cud)) {
			return; // Already on the list
		}
		final boolean present = hasUses(binder, cud.cu, decls);
		if (present) {
			reprocess.add(cud);
		}
	}
	
	/**
	 * Returns true if the CU has any uses of the specified decls
	 */
	private boolean hasUses(IBinder binder, IRNode cu, Set<IRNode> decls) {
		for(final IRNode n : JavaPromise.bottomUp(cu)) {
			final Operator op = JJNode.tree.getOperator(n);
			if (op instanceof IHasBinding) {
				final IBinding b = binder.getIBinding(n);
				if (decls.contains(b.getNode())) {
					return true;
				}
			}
		}
		return false;
	}
}
