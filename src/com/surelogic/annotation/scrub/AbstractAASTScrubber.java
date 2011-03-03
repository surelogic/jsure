/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/AbstractAASTScrubber.java,v 1.41 2009/06/09 18:56:53 chance Exp $*/
package com.surelogic.annotation.scrub;

import java.util.*;
import java.util.logging.Level;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.aast.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.*;
import com.surelogic.annotation.test.*;
import com.surelogic.ast.Resolvable;
import com.surelogic.ast.ResolvableToType;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ScopedPromiseDrop;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * A superclass specialized to implementing scrubbers that iterate over AASTs.
 * Includes code to check their bindings
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAASTScrubber<A extends IAASTRootNode> extends
		DescendingVisitor<Boolean> implements IAnnotationScrubber<A> {
	protected IAnnotationScrubberContext context;

	/**
	 * The annotation being scrubbed
	 */
	private IAASTRootNode current;

	private final String name;
	private final ScrubberType type;
	private final Class<A> cls;
	private final ScrubberOrder order;
	private final String[] dependencies;
	private final String[] runsBefore;
	@SuppressWarnings("unchecked")
	private final IPromiseDropStorage stor;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            The name of the scrubber
	 * @param c
	 *            The Node class to scrub
	 * @param type
	 *            The type of scrubber, e.g., UNORDERED, HIERARCHY
	 * @param order
	 *            Whether this runs first, last, or in the middle
	 * @param deps
	 *            Any dependencies
	 * @param before
	 *            Any scrubbers that this should run before
	 */
	@SuppressWarnings("unchecked")
	public AbstractAASTScrubber(String name, Class<A> c,
			IPromiseDropStorage stor, ScrubberType type, String[] before,
			ScrubberOrder order, String... deps) {
		super(Boolean.TRUE); // set to default to true
		this.name = name;
		this.stor = stor;
		if (stor == null) {
			System.out.println("Null storage");
		}
		this.type = type;
		this.cls = c;
		this.order = order;
		this.dependencies = deps;
		this.runsBefore = before;
	}
	@SuppressWarnings("unchecked")
	public AbstractAASTScrubber(String name, Class<A> c,
			IPromiseDropStorage stor, ScrubberType type, String... deps) {
		this(name, c, stor, type, NONE, ScrubberOrder.NORMAL, deps);
	}
	@SuppressWarnings("unchecked")
	public AbstractAASTScrubber(String name, Class<A> c,
			IPromiseDropStorage stor, ScrubberType type, ScrubberOrder order,
			String... deps) {
		this(name, c, stor, type, NONE, order, deps);
	}

	/**
	 * Defaults to unordered with no dependencies
	 */
	@SuppressWarnings("unchecked")
	public AbstractAASTScrubber(String name, Class<A> c,
			IPromiseDropStorage stor) {
		this(name, c, stor, ScrubberType.UNORDERED, NONE, ScrubberOrder.NORMAL,
				NONE);
	}

	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, ?> rule,
			ScrubberType type, String... deps) {
		this(rule, type, NONE, ScrubberOrder.NORMAL, deps);
	}

	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, ?> rule,
			ScrubberType type, String[] before, String... deps) {
		this(rule, type, before, ScrubberOrder.NORMAL, deps);
	}

	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, ?> rule,
			ScrubberType type, String[] before, ScrubberOrder order,
			String... deps) {
		this(rule.name(), rule.getAASTType(), rule.getStorage(), type, before,
				order, deps);
	}

	/**
	 * Defaults to unordered with no dependencies
	 */
	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, ?> rule) {
		this(rule, ScrubberType.UNORDERED, NONE);
	}

	protected AbstractAASTScrubber() {
		super(Boolean.TRUE); // set to default to true
		cls = null;
		dependencies = null;
		name = null;
		order = null;
		runsBefore = null;
		stor = null;
		type = null;
	}
	
	/**
	 * Returns the scrubber's name
	 */
	public final String name() {
		return name;
	}

	public ScrubberOrder order() {
		return order;
	}

	/**
	 * Returns a list of strings, each of which is the name of another scrubber
	 * that this scrubber depends on having run before it.
	 */
	public final String[] dependsOn() {
		return dependencies;
	}

	/**
	 * Returns a list of strings, each of which is the name of another scrubber
	 * that this scrubber needs to run before.
	 */
	public final String[] shouldRunBefore() {
		return runsBefore;
	}

	/**
	 * Sets the context for reporting errors, etc.
	 */
	public final void setContext(IAnnotationScrubberContext c) {
		context = c;
	}

	/**
	 * Returns the context
	 * 
	 * @return
	 */
	protected final IAnnotationScrubberContext getContext() {
		return context;
	}

	protected IAASTRootNode getCurrent() {
		return current;
	}
	
	protected void setCurrent(IAASTRootNode n) {
		current = n;
	}
	/***************************************************************************
	 * Code to scrub bindings for an AAST (in combination with code in the
	 * superclass
	 **************************************************************************/

	/**
	 * An AAST is only valid if all of it is valid
	 */
	@Override
	protected final Boolean combineResults(Boolean before, Boolean next) {
		return before && next;
	}

	@Override
	public final Boolean doAccept(AASTNode node) {
		Boolean result = super.doAccept(node);
		boolean rv = result == null ? true : result.booleanValue();
		if (rv) {
			result = customScrubBindings(node);
			if (result != null) {
				return rv && result;
			}
			if (node instanceof Resolvable) {
				Resolvable r = (Resolvable) node;
				if (!r.bindingExists() && context != null) {
					String msg = "Couldn't resolve a binding for "+node+" on "+current;
					context.reportError(msg, node);		
					//r.bindingExists();
					rv = false;
				}
			}
			rv = checkForTypeBinding(node, rv);
		}
		return rv;
	}
	
	protected final boolean checkForTypeBinding(AASTNode node, boolean rv) {
		if (node instanceof ResolvableToType) {
			ResolvableToType r = (ResolvableToType) node;
			if (!r.typeExists() && context != null) {					
				context.reportError("Couldn't resolve a type for " + node
						+ " on " + current, node);
				rv = false;
			}
		}
		return rv;
	}
	
	/**	 
	 * @param node 
	 * @return null if intended to do a normal scrub
	 */
	protected Boolean customScrubBindings(AASTNode node) {
		return null;
	}

	/**
	 * @return true if all bindings are good
	 */
	protected final boolean scrubBindings(A a) {
		current = a;
		if (a == null) {
			System.out.println("No AST to scrub");
			return false;
		}
		boolean result = doAccept((AASTNode) a);
		current = null;
		return result;
	}

	/***************************************************************************
	 * Code to support whatever custom scrubbing is needed for an AAST
	 **************************************************************************/

	/**
	 * Assumes that the AAST needs to have its bindings scrubbed.
	 * 
	 * Intended to be overridden
	 * 
	 * @return true if the AAST is good
	 */
	protected boolean scrub(A a) {
		return scrubBindings(a) && scrubNumberOfDrops(a) && customScrub(a);
	}

	/**
	 * Checks that whether it's ok to define another drop
	 * 
	 * @return true if ok
	 */
	protected final boolean scrubNumberOfDrops(A a) {
		if (stor == null) {
			return true; // Not checked
		}
		switch (stor.type()) {
		case BOOLEAN:
			return checkifUndefined(a);
		case NODE:
			return checkifUndefined(a);
		case SEQ:
			return true; // OK to have multiple
		case NONE:
			context.reportError("No storage allocated", a);
			return false;
		}
		return true;
	}

	/**
	 * Intended to be overridden
	 */
	@SuppressWarnings("unchecked")
	protected boolean checkifUndefined(A a) {		
		final IRNode promisedFor = a.getPromisedFor();
		final boolean defined    = stor.isDefined(promisedFor);
		if (defined) {
			PromiseDrop pd = (PromiseDrop) promisedFor.getSlotValue(stor.getSlotInfo());
			A old          = (A) pd.getAST();
			if (/*!pd.isValid() ||*/ old == null) {
				return true;
			}
			String oldS    = old.toString();
			String aS      = a.toString();
			if (!oldS.equals(aS)) {
				//System.out.println(JavaNames.genQualifiedMethodConstructorName(promisedFor));
				context.reportError("Conflicting promises: "+oldS+", "+aS, old);				
			}
		}
		return !defined;
	}

	/**
	 * Intended to be overridden
	 * 
	 * @return true if a is good
	 */
	protected boolean customScrub(A a) {
		return true;
	}

	/**
	 * Intended to be overridden
	 * 
	 * @return true if okay
	 */
	protected boolean processUnannotatedType(IJavaDeclaredType dt) {
		return true;
	}
	
	protected void processAASTsForType(List<A> l) {
		if (StorageType.SEQ.equals(stor.type())) {
			// Sort to process in a consistent order
			Collections.sort(l, aastComparator);

			for(A a : l) {
		    /*
			if ("MUTEX".equals(a.toString())) {
				System.out.println("Scrubbing: "+a.toString());						
			}
            */
				processAAST(a);
			}
		} else {
			MultiMap<IRNode,A> annos = new MultiHashMap<IRNode, A>();
			for(A a : l) {
				annos.put(a.getPromisedFor(), a);
			}			
			final List<IRNode> nodes = new ArrayList<IRNode>(annos.keySet());
			Collections.sort(nodes, nodeComparator);
			
			for(IRNode n : nodes) {
				processAASTsByNode(annos.get(n));
			}  
			/* Unordered among the nodes
			for(Map.Entry<IRNode,Collection<A>> e : annos.entrySet()) {
				processAASTsByNode(e.getValue());
			}
			*/
		}
	}
	
	protected void processAASTsByNode(Collection<A> l) {
		if (l.size() == 1) {
			processAAST(l.iterator().next());
		} else if (l.isEmpty()) {
			return;
		} else {
			// Sort to process in a consistent order
			List<A> sorted = new ArrayList<A>(l);
			Collections.sort(sorted, aastComparator);			
			
			// There should be at most one valid AAST
			A processedUnsuccessfully = null;
			for(A a : sorted) {
				ScopedPromiseDrop pd = AASTStore.getPromiseSource(a);
				if (pd == null) {
					boolean success = processAAST(a);
					if (success) {
						for(A a2 : l) {
							if (a2 != a) {
								if ("Starts nothing".equals(a.toString())) {
									ScopedPromiseDrop sp = AASTStore.getPromiseSource(a);
									ScopedPromiseDrop sp2 = AASTStore.getPromiseSource(a2);
									if (sp != null) {
										System.out.println("Got Starts nothing: "+DebugUnparser.toString(sp.getNode()));
									}
									if (sp2 != null) {
										System.out.println("Got Starts nothing: "+DebugUnparser.toString(sp2.getNode()));
									}
								}
								context.reportError("@Promise overridden by explicit annotation", a2);
							}
						}
						return;					
					} else {
						processedUnsuccessfully = a;
						break;
					}
				}
			}
			if (processedUnsuccessfully != null && l.size() <= 2) {			
				// At most one other AAST, so use that one instead
				for(A a : l) {
					if (a != processedUnsuccessfully) {
						processAAST(a);
						return;
					}
				}
			} else {
				// Create warning for all ASTs
				for(A a : l) {
					context.reportError("More than one annotation applies", a);
				}
			}
		}
	}
	
	/**
	 * Intended to be overridden
	 * 
	 * @param a
	 */
	protected boolean processAAST(A a) {
		TestResult expected = AASTStore.getTestResult(a);
		boolean result = scrub(a);
		if (result) {
			a.markAsBound();
			TestResult.setAsBound(expected);

			final IRNode cu = AASTStore.checkIfAssumption(a);
			if (cu != null) {
				final PromiseFramework frame = PromiseFramework.getInstance();
				frame.pushTypeContext(cu, true, true); // create one if there isn't one
			}
			PromiseDrop<? super A> d = makePromiseDrop(a);
			if (cu != null) {
				d.setAssumed(true);
				PromiseFramework.getInstance().popTypeContext();				
			}			
			if (d != null) {
				a.markAsValid();
				d.setFromSrc(a.getSrcType().isFromSource());
				d.setAST(a);
				d.dependUponCompilationUnitOf(a.getPromisedFor());
				TestResult.addDrop(expected, d);
				AASTStore.validate(d);
			} else {
				a.markAsUnassociated();
				TestResult.checkIfMatchesResult(expected,
						TestResultType.UNASSOCIATED);
				return false;
			}
		} else {
			a.markAsUnbound();
			TestResult.checkIfMatchesResult(expected, TestResultType.UNBOUND);
		}
		return result;
	}

	public void run() {
		IDE.runAtMarker(new AbstractRunner() {
			public void run() {
				if (SLLogger.getLogger().isLoggable(Level.FINER)) {
					SLLogger.getLogger().finer(
							"Running "
									+ AbstractAASTScrubber.this.getClass()
											.getName());
				}
				switch (type) {
				case UNORDERED:
					/* Eliminated due to @Assume's need to process by type
					scrub(cls);
					return;
					*/
				case BY_TYPE:
					scrubByPromisedFor_Type(cls);
					return;
				case BY_HIERARCHY:
					scrubByPromisedFor_Hierarchy(cls, false);
					return;
				case INCLUDE_SUBTYPES_BY_HIERARCHY:
					scrubByPromisedFor_Hierarchy(cls, true);
					return;
				case DIY:
					scrubAll(AASTStore.getASTsByClass(cls));
					return;
				case OTHER:
					throw new UnsupportedOperationException();
				}
				AASTStore.sync();
			}
		});
	}

	/**
	 * Scrub the bindings of the specified kind in no particular order
	 */
	@SuppressWarnings("unused")
	private void scrub(Class<A> c) {
		for (A a : AASTStore.getASTsByClass(c)) {
			processAAST(a);
		}
	}

	protected void startScrubbingType(IRNode decl) {
		// Nothing to do right now		
	}
	
	protected void finishScrubbingType(IRNode decl) {
		// Nothing to do right now
	}
	/**
	 * Use assumptions while scrubbing
	 * @return
	 */
	protected boolean useAssumptions() {
		return true;
	}
	
	protected void scrubAll(Iterable<A> all) {
		throw new UnsupportedOperationException();
	}
	
	private void startScrubbingType_internal(IRNode decl) {
		startScrubbingType(decl);	
		if (useAssumptions()) {
			final IRNode cu = VisitUtil.getEnclosingCompilationUnit(decl);
			PromiseFramework.getInstance().pushTypeContext(cu);
		}
	}
	
	private void finishScrubbingType_internal(IRNode decl) {
		if (useAssumptions()) {
			PromiseFramework.getInstance().popTypeContext();
		}
		finishScrubbingType(decl);
	}
	
	private void scrubByPromisedFor_Type(Class<A> c) {
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		organizeByType(c, byType);
		
		for(Map.Entry<IRNode, List<A>> e : byType.entrySet()) {
			List<A> l = e.getValue();
			if (l != null && !l.isEmpty()) {
				final IRNode decl = e.getKey();
				startScrubbingType_internal(decl);
				try {
					processAASTsForType(l);
				} finally {
					finishScrubbingType_internal(decl);
				}
			}
		}
	}
	
	private void organizeByType(Class<A> c, Map<IRNode, List<A>> byType) {
		// Organize by promisedFor
		for (A a : AASTStore.getASTsByClass(c)) {
			IRNode promisedFor = a.getPromisedFor();
			IRNode type = VisitUtil.getClosestType(promisedFor);
			if (type == null) {
				type = VisitUtil.getEnclosingCompilationUnit(promisedFor);
			}
			else if (!TypeDeclaration.prototype.includes(type)) {
				throw new IllegalArgumentException("Not a type decl: "
						+ DebugUnparser.toString(type));
			}
			List<A> l = byType.get(type);
			if (l == null) {
				l = new ArrayList<A>();
				byType.put(type, l);
			}
			l.add(a);
		}
	}
	
	private class HierarchyWalk {
		final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv();
		// Empty list = a subclass w/o required annos
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		final Set<IRNode> done = new HashSet<IRNode>();
		final boolean includeSubtypes;
		
		HierarchyWalk(boolean includeSubtypes) {
			this.includeSubtypes = includeSubtypes;
		}

		void init(Class<A> c) {
			organizeByType(c, byType);
			
			if (includeSubtypes) {
				for(IRNode type : new ArrayList<IRNode>(byType.keySet())) {
					for(IRNode sub : tEnv.getRawSubclasses(type)) {
						// Add empty lists for types w/o required annos
						if (!byType.containsKey(sub)) {
							byType.put(sub, Collections.<A>emptyList());
						}
					}
				}
			}
		}

		/**
		 * Walk ancestors backwards (from roots like java.lang.Object)
		 */
		void walkHierarchy() {
			for (IRNode type : byType.keySet()) {
				IJavaDeclaredType dt = JavaTypeFactory.getMyThisType(type);
				walkHierarchy(dt);
			}
		}

		private void walkHierarchy(final IJavaDeclaredType dt) {
			final IRNode decl = dt.getDeclaration();
			if (done.contains(decl)) {
				return;
			}

			// get super types
			for (IJavaType st : tEnv.getSuperTypes(dt)) {
				walkHierarchy((IJavaDeclaredType) st);
			}

			// process this type
			//System.out.println("Scrubbing promises for "+dt+" -- "+decl);
			List<A> l = byType.get(decl);
			if (l != null) {
				startScrubbingType_internal(decl);
				if (l == Collections.emptyList()) {
					processUnannotatedType(dt);
				}
				try {
					processAASTsForType(l);
				} finally {
					finishScrubbingType_internal(decl);
				}
			}
			// Mark as done
			done.add(decl);
		}

		void checkState() {
			// Check that we've processed everything
			for (IRNode t : done) {
				byType.remove(t);
			}
			if (!byType.isEmpty()) {
				for (IRNode type : byType.keySet()) {
					throw new Error("Didn't process "
							+ DebugUnparser.toString(type));
				}
			}
		}
	}

	private static final Comparator<IAASTRootNode> aastComparator = new Comparator<IAASTRootNode>() {
		public int compare(IAASTRootNode o1, IAASTRootNode o2) {
			// TODO What about promisedFor
			return o1.getOffset() - o2.getOffset();
		}
	};
		
	private static final Comparator<IRNode> nodeComparator = new Comparator<IRNode>() {
		public int compare(IRNode o1, IRNode o2) {
			ISrcRef r1 = JavaNode.getSrcRef(o1);
			ISrcRef r2 = JavaNode.getSrcRef(o2);						
			return r1.getOffset() - r2.getOffset();
		}
	};

	/**
	 * Scrub the bindings of the specified kind in order of the position of
	 * their promisedFor (assumed to be a type decl) in the type hierarchy
	 */
	private void scrubByPromisedFor_Hierarchy(Class<A> c, boolean includeSubtypes) {
		HierarchyWalk walk = new HierarchyWalk(includeSubtypes);
		walk.init(c);
		walk.walkHierarchy();
		walk.checkState();
	}
	
	protected abstract PromiseDrop<? super A> makePromiseDrop(A ast);
}
