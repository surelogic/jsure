/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/AbstractAASTScrubber.java,v 1.41 2009/06/09 18:56:53 chance Exp $*/
package com.surelogic.annotation.scrub;

import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.aast.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.*;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.test.*;
import com.surelogic.ast.Resolvable;
import com.surelogic.ast.ResolvableToType;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * A superclass specialized to implementing scrubbers that iterate over AASTs.
 * Includes code to check their bindings
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAASTScrubber<A extends IAASTRootNode, P extends PromiseDrop<? super A>>
extends AbstractHierarchyScrubber<A> {
	private final Visitor visitor = new Visitor(Boolean.TRUE);
	
	/**
	 * The annotation being scrubbed
	 */
	private IAASTRootNode current;

	private final Class<A> cls;
	private final IPromiseDropStorage<P> stor;

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
	    IPromiseDropStorage<P> stor, ScrubberType type, String[] before,
			ScrubberOrder order, String... deps) {
		super(type, before, name, order, deps);
		this.stor = stor;
		if (stor == null) {
			System.out.println("Null storage");
		}
		this.cls = c;
	}

	public AbstractAASTScrubber(String name, Class<A> c,
	    IPromiseDropStorage<P> stor, ScrubberType type, String... deps) {
		this(name, c, stor, type, NONE, ScrubberOrder.NORMAL, deps);
	}

	public AbstractAASTScrubber(String name, Class<A> c,
	    IPromiseDropStorage<P> stor, ScrubberType type, ScrubberOrder order,
			String... deps) {
		this(name, c, stor, type, NONE, order, deps);
	}

	/**
	 * Defaults to unordered with no dependencies
	 */
	public AbstractAASTScrubber(String name, Class<A> c,
	    IPromiseDropStorage<P> stor) {
		this(name, c, stor, ScrubberType.UNORDERED, NONE, ScrubberOrder.NORMAL,
				NONE);
	}

	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, P> rule,
			ScrubberType type, String... deps) {
		this(rule, type, NONE, ScrubberOrder.NORMAL, deps);
	}

	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, P> rule,
			ScrubberType type, String[] before, String... deps) {
		this(rule, type, before, ScrubberOrder.NORMAL, deps);
	}

	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, P> rule,
			ScrubberType type, String[] before, ScrubberOrder order,
			String... deps) {
		this(rule.name(), rule.getAASTType(), rule.getStorage(), type, before,
				order, deps);
	}

	/**
	 * Defaults to unordered with no dependencies
	 */
	public AbstractAASTScrubber(ISingleAnnotationParseRule<A, P> rule) {
		this(rule, ScrubberType.UNORDERED, NONE);
	}

	private AbstractAASTScrubber() {
		super(null, null, null, null, (String[])null);
		cls = null;
		stor = null;
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

	private class Visitor extends DescendingVisitor<Boolean> {
		public Visitor(Boolean defaultVal) {
			super(defaultVal);
		}

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
					if (!r.bindingExists() && getContext() != null) {
						String msg = "Couldn't resolve a binding for "+node+" on "+current;
						getContext().reportError(msg, node);		
						if (msg.startsWith("Couldn't resolve a binding for InstanceRegion on RegionEffects Writes test_qualifiedThis.C.this:InstanceRegion")) {
							r.bindingExists();
						}
						rv = false;
					}
				}
				rv = checkForTypeBinding(node, rv);
			}
			return rv;
		}
	}
		
	protected final boolean checkForTypeBinding(AASTNode node, boolean rv) {
		if (node instanceof ResolvableToType) {
			ResolvableToType r = (ResolvableToType) node;
			if (!r.typeExists() && getContext() != null) {					
				getContext().reportError("Couldn't resolve a type for " + node
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
		boolean result = visitor.doAccept((AASTNode) a);
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
			getContext().reportError("No storage allocated", a);
			return false;
		}
		return true;
	}

	/**
	 * Intended to be overridden
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected boolean checkifUndefined(A a) {		
		final IRNode promisedFor = a.getPromisedFor();
		final boolean defined    = stor.isDefined(promisedFor);
		if (defined) {
			PromiseDrop pd = promisedFor.getSlotValue(stor.getSlotInfo());
			A old          = (A) pd.getAST();
			if (/*!pd.isValid() ||*/ old == null) {
				return true;
			}
			String oldS    = old.toString();
			String aS      = a.toString();
			if (!oldS.equals(aS)) {
				//System.out.println(JavaNames.genQualifiedMethodConstructorName(promisedFor));
				getContext().reportError("Conflicting promises: "+oldS+", "+aS, old);				
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
	
	@Override
	protected void finishRun() {
		AASTStore.sync();
	}
	
	@Override
	protected void processAASTsForType(IAnnotationTraversalCallback<A> cb, IRNode decl, List<A> l) {
		if (StorageType.SEQ.equals(stor.type())) {
			// Sort to process in a consistent order
			Collections.sort(l, aastComparator);

			for(A a : preprocessAASTsForSeq(l)) {
		    /*
			if ("MUTEX".equals(a.toString())) {
				System.out.println("Scrubbing: "+a.toString());						
			}
            */
				processAAST(cb, a);
			}
		} else {
			MultiMap<IRNode,A> annos = new MultiHashMap<IRNode, A>();
			for(A a : l) {
				annos.put(a.getPromisedFor(), a);
			}			
			final List<Map.Entry<IRNode,Collection<A>>> nodes = new ArrayList<Map.Entry<IRNode,Collection<A>>>(annos.entrySet());
			Collections.sort(nodes, entryComparator);
			
			for(Map.Entry<IRNode,Collection<A>> e : nodes) {
				processAASTsByNode(cb, e.getValue());
			}  
			/* Unordered among the nodes
			for(Map.Entry<IRNode,Collection<A>> e : annos.entrySet()) {
				processAASTsByNode(e.getValue());
			}
			*/
		}
	}
	
	@Override
	protected void finishAddDerived(A clone, PromiseDrop<? extends A> pd) {
		// Copied from AASTStore
		synchronized (AASTStore.class) {
			AASTStore.setPromiseSource(clone, pd);
			if (pd instanceof ValidatedDropCallback<?>) {
				AASTStore.triggerWhenValidated(clone, (ValidatedDropCallback<?>) pd);
			}
			AASTStore.cloneTestResult(pd.getAST(), clone);
		}
	}
	
	/**
	 * Written for seq promises
	 */
	protected Collection<A> preprocessAASTsForSeq(Collection<A> l) {
		return l;
	}
	
	/**
	 * Written for boolean/node promises that can really only take one AAST per node,
	 * checking for conflicts 
	 * 
	 * @param cb 
	 */
	protected void processAASTsByNode(IAnnotationTraversalCallback<A> cb, Collection<A> l) {
		if (l.size() == 1) {
			processAAST(cb, l.iterator().next());
		} else if (l.isEmpty()) {
			return;
		} else {
			// Sort to process in a consistent order
			List<A> sorted = new ArrayList<A>(l);
			Collections.sort(sorted, aastComparator);			
			
			// There should be at most one valid AAST
			A processedUnsuccessfully = null;
			for(A a : sorted) {
				PromiseDrop<?> pd = AASTStore.getPromiseSource(a);
				if (pd == null) {
					boolean success = processAAST(cb, a);
					if (success) {
						for(A a2 : l) {
							if (a2 != a) {
								PromiseDrop<?> sp = AASTStore.getPromiseSource(a2);
								/*
								if ("Starts nothing".equals(a.toString())) {					
									PromiseDrop<?> sp2 = AASTStore.getPromiseSource(a2);
									if (sp != null) {
										System.out.println("Got Starts nothing: "+DebugUnparser.toString(sp.getNode()));
									}
									if (sp2 != null) {
										System.out.println("Got Starts nothing: "+DebugUnparser.toString(sp2.getNode()));
									}
								}
								*/								
								if (sp != null) {
									getContext().reportError(sp.getMessage()+" ("+JavaNames.getFullName(sp.getNode())+
											") overridden by explicit annotation "+
											a+" ("+JavaNames.getFullName(a.getPromisedFor())+")", a2);
								} else {
									getContext().reportError(a2+" ("+JavaNames.getFullName(a2.getPromisedFor())+
											            ") overridden by explicit annotation "+
														a+" ("+JavaNames.getFullName(a.getPromisedFor())+")", a2);
								}
								markAsUnassociated(a2);
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
						processAAST(cb, a);
						return;
					}
				}
			} else {
				// Create warning for all ASTs
				for(A a : l) {
					getContext().reportError("More than one annotation applies", a);
					markAsUnassociated(a);
				}
			}
		}
	}
	
	private void markAsUnassociated(A a) {
		TestResult expected = AASTStore.getTestResult(a);
		a.markAsUnassociated();
		TestResult.checkIfMatchesResult(expected, TestResultType.UNASSOCIATED);
	}
	
	/**
	 * Intended to be overridden
	 * @param cb 
	 * 
	 * @param a
	 */
	protected boolean processAAST(IAnnotationTraversalCallback<A> cb, A a) {
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
			PromiseDrop<? super A> d = makePromiseDrop(cb, a);
			if (cu != null) {
				if (d != null) {
					d.setAssumed(true);
				}
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
	
	private final IAnnotationTraversalCallback<A> nullCallback = new IAnnotationTraversalCallback<A>() {
		public void addDerived(A c, PromiseDrop<? extends A> pd) {
			getContext().reportWarning("Ignoring derived AAST created by "+pd.getClass().getSimpleName(), c);
		}
	};
	
	@Override
	protected IAnnotationTraversalCallback<A> getNullCallback() {
		return nullCallback;
	}
	
	@Override
	protected Iterable<A> getRelevantAnnotations() {
		return AASTStore.getASTsByClass(cls);
	}
		
	final Comparator<Map.Entry<IRNode,Collection<A>>> entryComparator = new Comparator<Map.Entry<IRNode,Collection<A>>>() {
		public int compare(Map.Entry<IRNode,Collection<A>> o1, Map.Entry<IRNode,Collection<A>> o2) {
			ISrcRef r1 = JavaNode.getSrcRef(o1.getKey());
			ISrcRef r2 = JavaNode.getSrcRef(o2.getKey());
			if (r1 == null || r2 == null) {
				//throw new IllegalStateException(DebugUnparser.toString(o1));
				return min(o1.getValue()) - min(o2.getValue());
			}
			return r1.getOffset() - r2.getOffset();
		}
		private int min(Collection<A> asts) {
			int min = Integer.MAX_VALUE;
			for(A a : asts) { 
				if (a.getOffset() < min) {
					min = a.getOffset();
				}
			}
			return min;
		}
	};

	protected P storeDropIfNotNull(A a, P pd) {
	  return AnnotationRules.storeDropIfNotNull(stor, a, pd);
	}
	
	/**
	 * Meant to be overridden
	 */
	protected PromiseDrop<? super A> makePromiseDrop(IAnnotationTraversalCallback<A> cb, A ast) {
		return makePromiseDrop(ast);
	}
	
	// Only called by the method above
	protected abstract PromiseDrop<? super A> makePromiseDrop(A ast);
}
