/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/AbstractAASTScrubber.java,v 1.41 2009/06/09 18:56:53 chance Exp $*/
package com.surelogic.annotation.scrub;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.aast.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.*;
import com.surelogic.annotation.test.*;
import com.surelogic.ast.Resolvable;
import com.surelogic.ast.ResolvableToType;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * A superclass specialized to implementing scrubbers that iterate over AASTs.
 * Includes code to check their bindings
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAASTScrubber<A extends IAASTRootNode> extends
		DescendingVisitor<Boolean> implements IAnnotationScrubber<A> {
	private IAnnotationScrubberContext context;

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

	public AbstractAASTScrubber(String name, Class<A> c,
			IPromiseDropStorage stor, ScrubberType type, String... deps) {
		this(name, c, stor, type, NONE, ScrubberOrder.NORMAL, deps);
	}

	public AbstractAASTScrubber(String name, Class<A> c,
			IPromiseDropStorage stor, ScrubberType type, ScrubberOrder order,
			String... deps) {
		this(name, c, stor, type, NONE, order, deps);
	}

	/**
	 * Defaults to unordered with no dependencies
	 */
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
		if (node instanceof Resolvable) {
			Resolvable r = (Resolvable) node;
			if (!r.bindingExists()) {
				context.reportError("Couldn't resolve a binding for " + node
						+ " on " + current, node);
				rv = false;
			}
		}
		if (node instanceof ResolvableToType) {
			ResolvableToType r = (ResolvableToType) node;
			if (!r.typeExists()) {
				context.reportError("Couldn't resolve a type for " + node
						+ " on " + current, node);
				rv = false;
			}
		}
		return rv;
	}

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
	 */
	protected boolean scrub(A a) {
		return scrubBindings(a) && scrubNumberOfDrops(a) && customScrub(a);
	}

	/**
	 * Checks that whether it's ok to define another drop
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
			if (old == null) {
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
	 */
	protected boolean customScrub(A a) {
		return true;
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

			PromiseDrop<? super A> d = makePromiseDrop(a);
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
					scrub(cls);
					return;
				case BY_HIERARCHY:
					scrubByPromisedFor(cls);
					return;
				case OTHER:
					throw new UnsupportedOperationException();
				}
			}
		});
	}

	/**
	 * Scrub the bindings of the specified kind in no particular order
	 */
	private void scrub(Class<A> c) {
		for (A a : AASTStore.getASTsByClass(c)) {
			processAAST(a);
		}
	}

	private class HierarchyWalk {
		final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv();
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		final Set<IRNode> done = new HashSet<IRNode>();

		void init(Class<A> c) {
			// Organize by promisedFor
			for (A a : AASTStore.getASTsByClass(c)) {
				IRNode type = a.getPromisedFor();
				if (!TypeDeclaration.prototype.includes(type)) {
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
			List<A> l = byType.get(decl);
			if (l != null) {
				if (l.size() > 1) {
					Collections.sort(l, aastComparator);
					/*
					 * for(A a : l) { System.out.println("After sort: "+a); }
					 * System.out.println();
					 */
				}
				for (A a : l) {
					processAAST(a);
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
			return o1.getOffset() - o2.getOffset();
		}
	};

	/**
	 * Scrub the bindings of the specified kind in order of the position of
	 * their promisedFor (assumed to be a type decl) in the type hierarchy
	 */
	private void scrubByPromisedFor(Class<A> c) {
		HierarchyWalk walk = new HierarchyWalk();
		walk.init(c);
		walk.walkHierarchy();
		walk.checkState();
	}

	protected abstract PromiseDrop<? super A> makePromiseDrop(A ast);
}
