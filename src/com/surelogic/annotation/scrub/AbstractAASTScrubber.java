/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/AbstractAASTScrubber.java,v 1.41 2009/06/09 18:56:53 chance Exp $*/
package com.surelogic.annotation.scrub;

import java.util.*;
import java.util.logging.Level;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.aast.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.annotation.*;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.test.*;
import com.surelogic.ast.Resolvable;
import com.surelogic.ast.ResolvableToType;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * A superclass specialized to implementing scrubbers that iterate over AASTs.
 * Includes code to check their bindings
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAASTScrubber<A extends IAASTRootNode, P extends PromiseDrop<? super A>> extends
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
	
	final void processUnannotatedDeclsForType(List<IRNode> declsToCheck) {
		if (declsToCheck == null) {
			return;
		}	
		// TODO do I need to sort these? (they're unannotated)		
		// Collections.sort(declsToCheck, nodeComparator);
		for(IRNode decl : declsToCheck) {
			processUnannotatedMethodRelatedDecl(decl);
		}
	}
	
	/**
	 * Intended to be overridden
	 * 
	 * @return true if okay
	 */
	protected boolean processUnannotatedMethodRelatedDecl(IRNode decl) {
		return true;
	}
	
	protected void processAASTsForType(IRNode decl, List<A> l) {
		if (StorageType.SEQ.equals(stor.type())) {
			// Sort to process in a consistent order
			Collections.sort(l, aastComparator);

			for(A a : preprocessAASTsForSeq(l)) {
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
			final List<Map.Entry<IRNode,Collection<A>>> nodes = new ArrayList<Map.Entry<IRNode,Collection<A>>>(annos.entrySet());
			Collections.sort(nodes, entryComparator);
			
			for(Map.Entry<IRNode,Collection<A>> e : nodes) {
				processAASTsByNode(e.getValue());
			}  
			/* Unordered among the nodes
			for(Map.Entry<IRNode,Collection<A>> e : annos.entrySet()) {
				processAASTsByNode(e.getValue());
			}
			*/
		}
	}
	
	/**
	 * Written for seq promises
	 */
	protected Collection<A> preprocessAASTsForSeq(Collection<A> l) {
		return l;
	}
	
	/**
	 * Written for boolean/node promises that can really only take one AAST per node
	 */
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
				PromiseDrop<?> pd = AASTStore.getPromiseSource(a);
				if (pd == null) {
					boolean success = processAAST(a);
					if (success) {
						for(A a2 : l) {
							if (a2 != a) {
								if ("Starts nothing".equals(a.toString())) {
									PromiseDrop<?> sp = AASTStore.getPromiseSource(a);
									PromiseDrop<?> sp2 = AASTStore.getPromiseSource(a2);
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
				case INCLUDE_SUBTYPES_BY_HIERARCHY:
				case INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY:
					scrubByPromisedFor_Hierarchy(cls, type);
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
			IRNode cu = VisitUtil.getEnclosingCompilationUnit(decl);
			if (cu == null) {
				// Probably already the CU decl
				cu = decl; 
				/*
				IRNode parent = JJNode.tree.getParentOrNull(decl);
				System.out.println("parent = "+DebugUnparser.toString(parent));
				*/
			}
			PromiseFramework.getInstance().pushTypeContext(cu);
		}
	}
	
	private void finishScrubbingType_internal(IRNode decl) {
		if (useAssumptions()) {
			PromiseFramework.getInstance().popTypeContext();
		}
		finishScrubbingType(decl);
	}
	
	void scrubByPromisedFor_Type(Class<A> c) {
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		organizeByType(c, byType);
		
		for(Map.Entry<IRNode, List<A>> e : byType.entrySet()) {
			List<A> l = e.getValue();
			if (l != null && !l.isEmpty()) {
				final IRNode decl = e.getKey();
				startScrubbingType_internal(decl);
				try {
					processAASTsForType(decl, l);
				} finally {
					finishScrubbingType_internal(decl);
				}
			}
		}
	}
	
	void organizeByType(Class<A> c, Map<IRNode, List<A>> byType) {
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
		// Empty list = a subclass w/o required annos
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		final Map<IRNode, List<IRNode>> methodRelatedDeclsToCheck = new HashMap<IRNode, List<IRNode>>();
		final Set<IRNode> done = new HashSet<IRNode>();
		final ScrubberType scrubberType;
		
		HierarchyWalk(ScrubberType type) {
			this.scrubberType = type;
		}

		void init(Class<A> c) {
			organizeByType(c, byType);
			switch (scrubberType) {
			case INCLUDE_SUBTYPES_BY_HIERARCHY:
				for(IRNode type : new ArrayList<IRNode>(byType.keySet())) {
					final IIRProject p = JavaProjects.getEnclosingProject(type);
					/*
					if ("I".equals(JJNode.getInfoOrNull(type))) {
						System.out.println("Looking at my type");
					}
                    */
					for(IRNode sub : p.getTypeEnv().getRawSubclasses(type)) {
						// Add empty lists for types w/o required annos
						if (!byType.containsKey(sub)) {
							// Mark it as a type that we need to look at
							byType.put(sub, Collections.<A>emptyList());
						}
					}
				}		
				break;
			case INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY:
				computeMethodRelatedDeclsToCheck(c);
				break;
			default:
			}
		}

		/**
		 * Find the decls in overridden methods that we also need to look at
		 */
		private void computeMethodRelatedDeclsToCheck(Class<A> c) {
			final Set<IRNode> declsToCheck = new HashSet<IRNode>();
			final Set<IRNode> hasAASTs = new HashSet<IRNode>();
			for (A a : AASTStore.getASTsByClass(c)) {
				final IRNode promisedFor = a.getPromisedFor();		
				hasAASTs.add(promisedFor);
				
				// Find the relative location of the AST
				final Operator op = JJNode.tree.getOperator(promisedFor);
				final AnnotationLocation loc;
				int parameterNum = -1;
				if (SomeFunctionDeclaration.prototype.includes(op)) {
					loc = AnnotationLocation.DECL;
				}
				else if (ReceiverDeclaration.prototype.includes(op)) {
					loc = AnnotationLocation.RECEIVER;
				}
				else if (ParameterDeclaration.prototype.includes(op)) {
					loc = AnnotationLocation.PARAMETER;
					IRLocation irLoc = JJNode.tree.getLocation(promisedFor);
					parameterNum = JJNode.tree.childLocationIndex(promisedFor, irLoc);
				}
				else if (ReturnValueDeclaration.prototype.includes(op)) {
					loc = AnnotationLocation.RETURN_VAL;
				}
				else if (VariableDeclarator.prototype.includes(op) || ClassInitDeclaration.prototype.includes(op)) {
				    // These can't be overridden by anything else
				    continue;
				}
				else {
					throw new IllegalStateException("Unexpected decl: "+op.name());
				}
				
				// Find the decls to visit
				final IRNode enclosingFunc = VisitUtil.getClosestClassBodyDecl(promisedFor);
				IRNode type = VisitUtil.getEnclosingType(enclosingFunc);
				if (type == null) {
					type = VisitUtil.getEnclosingCompilationUnit(enclosingFunc);
				}
				final IIRProject p = JavaProjects.getEnclosingProject(type);
				for(IRNode om : p.getTypeEnv().getBinder().findOverridingMethodsFromType(enclosingFunc, type)) {					
					switch (loc) {
					case DECL:
						declsToCheck.add(om);
						break;
					case RECEIVER:
						declsToCheck.add(ReceiverDeclaration.getReceiverNode(om));
						break;
					case RETURN_VAL:
						declsToCheck.add(ReturnValueDeclaration.getReturnNode(om));
						break;
					case PARAMETER:
						final IRNode params = SomeFunctionDeclaration.getParams(om);
						declsToCheck.add(JJNode.tree.getChild(params, parameterNum));
						break;
					}
				}				
			}
			// Remove decls that we'll already look at
			declsToCheck.removeAll(hasAASTs);
			hasAASTs.clear(); 
			methodRelatedDeclsToCheck.clear();
			
			// Sort by type
			for(IRNode decl : declsToCheck) {
				final IRNode type = VisitUtil.getEnclosingType(decl);
				if (!byType.containsKey(type)) {
					// Mark it as a type that we need to look at
					byType.put(type, Collections.<A>emptyList());
				}
				List<IRNode> mrds = methodRelatedDeclsToCheck.get(type); 
				if (mrds == null) {
					mrds = new ArrayList<IRNode>();
					methodRelatedDeclsToCheck.put(type, mrds);
				}
				mrds.add(decl);
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
			final IIRProject p = JavaProjects.getEnclosingProject(decl);
			// get super types
			for (IJavaType st : p.getTypeEnv().getSuperTypes(dt)) {
				walkHierarchy((IJavaDeclaredType) st);
			}

			// process this type
			//System.out.println("Scrubbing promises for "+dt+" -- "+decl);
			List<A> l = byType.get(decl);
			if (l != null) {
				startScrubbingType_internal(decl);
				try {
					final List<IRNode> otherDeclsToCheck;
					if (type == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
						otherDeclsToCheck = methodRelatedDeclsToCheck.get(decl);
				    } else {
						otherDeclsToCheck = Collections.emptyList();
					}
					if (l == Collections.emptyList()) {
						if (type == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
							processUnannotatedDeclsForType(otherDeclsToCheck);
						} else {
							processUnannotatedType(dt);
						}
					} else {
						processAASTsForType(dt.getDeclaration(), l);
						if (type == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
							processUnannotatedDeclsForType(otherDeclsToCheck);
						}
					}
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

	static final Comparator<IAASTRootNode> aastComparator = new Comparator<IAASTRootNode>() {
		public int compare(IAASTRootNode o1, IAASTRootNode o2) {
			final IRNode p1 = o1.getPromisedFor();
			final IRNode p2 = o2.getPromisedFor();
			if (p1.equals(p2)) {
				return o1.getOffset() - o2.getOffset();
			} else {
				return p1.hashCode() - p2.hashCode();
			}
		}
	};
		
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

	/**
	 * Scrub the bindings of the specified kind in order of the position of
	 * their promisedFor (assumed to be a type decl) in the type hierarchy
	 */
	private void scrubByPromisedFor_Hierarchy(Class<A> c, ScrubberType type) {
		HierarchyWalk walk = new HierarchyWalk(type);
		walk.init(c);
		walk.walkHierarchy();
		walk.checkState();
	}

	protected P storeDropIfNotNull(A a, P pd) {
	  return AnnotationRules.storeDropIfNotNull(stor, a, pd);
	}
	
	protected abstract PromiseDrop<? super A> makePromiseDrop(A ast);
}
