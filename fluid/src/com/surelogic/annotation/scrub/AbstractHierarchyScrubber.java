package com.surelogic.annotation.scrub;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.analysis.*;
import com.surelogic.annotation.AnnotationLocation;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * Designed to scrub annotations/promises hanging off of IRNodes
 * 
 * @author Edwin
 */
public abstract class AbstractHierarchyScrubber<A extends IHasPromisedFor> extends AbstractScrubber {
	final ScrubberType scrubberType;
	
	public AbstractHierarchyScrubber(ScrubberType type, String[] before, String name, ScrubberOrder order, String[] deps) {
		super(before, name, order, deps);
		scrubberType = type;
	}

	// Things to override
	protected abstract Iterable<A> getRelevantAnnotations();			
	protected abstract IAnnotationTraversalCallback<A> getNullCallback();
	protected abstract void finishRun(); 
	protected abstract void processAASTsForType(IAnnotationTraversalCallback<A> cb, IRNode decl, List<A> l);
	protected abstract void finishAddDerived(A clone, PromiseDrop<? extends A> pd);
	
	/**
	 * Use assumptions while scrubbing
	 * @return
	 */
	protected boolean useAssumptions() {
		return true;
	}
	
	protected void startScrubbingType(IRNode decl) {
		// Nothing to do right now		
	}
	
	/**
	 * Called if a method in a supertype has something that methods in subtypes need to be consistent with
	 * Intended to be overridden
	 * 
	 * @return true if okay
	 */
	protected boolean processUnannotatedMethodRelatedDecl(IRNode decl) {
		return true;
	}
	
	/**
	 * Called if a supertype has something that subtypes need to be consistent with
	 * Intended to be overridden
	 * 
	 * @return true if okay
	 */
	protected boolean processUnannotatedType(IJavaSourceRefType dt) {
		return true;
	}
	
	protected void finishScrubbingType(IRNode decl) {
		// Nothing to do right now
	}
	
	public void run() {
		IDE.runAtMarker(new AbstractRunner() {
			public void run() {
				if (SLLogger.getLogger().isLoggable(Level.FINER)) {
					SLLogger.getLogger().finer(
							"Running "
									+ AbstractHierarchyScrubber.this.getClass()
											.getName());
				}
				switch (scrubberType) {
				case UNORDERED:
					/* Eliminated due to @Assume's need to process by type
					scrub(cls);
					return;
					*/
				case BY_TYPE:
					scrubByPromisedFor_Type();
					return;
				case BY_HIERARCHY:
				case INCLUDE_SUBTYPES_BY_HIERARCHY:
				case INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY:
					scrubByPromisedFor_Hierarchy();
					return;
				case DIY:
					scrubAll(getNullCallback(), getRelevantAnnotations());
					return;
				case OTHER:
					throw new UnsupportedOperationException();
				}
				finishRun();
			}
		});	
	}
	
	static boolean isBinary(IRNode n) {
		IRNode cu = VisitUtil.getEnclosingCompilationUnit(n);
		return JavaNode.getModifier(cu, JavaNode.AS_BINARY);
	}
	
	/**
	 * Visits stuff (usually annotations/promises) hanging off the type hierarchy
	 */
	protected final class TypeHierarchyVisitor implements IAnnotationTraversalCallback<A> {	
		// Empty list = a subclass w/o required annos
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		final Map<IRNode, List<IRNode>> methodRelatedDeclsToCheck = new HashMap<IRNode, List<IRNode>>();
		final Set<IRNode> done = new HashSet<IRNode>();
		
		void init() {
			organizeByType(byType);
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
				computeMethodRelatedDeclsToCheck();
				break;
			default:
			}
		}

		/**
		 * Find the decls in overridden methods that we also need to look at
		 */
		private void computeMethodRelatedDeclsToCheck() {
			final Set<IRNode> declsToCheck = new HashSet<IRNode>();
			final Set<IRNode> hasAASTs = new HashSet<IRNode>();
			for (A a : getRelevantAnnotations()) {
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
				else if (QualifiedReceiverDeclaration.prototype.includes(op)) {
					loc = AnnotationLocation.QUALIFIED_RECEIVER;
					// Need to check if this is on a constructor on not?
				}
				else {
					throw new IllegalStateException("Unexpected decl: "+op.name());
				}
				
				// Find the decls to visit
				final IRNode enclosingFunc = VisitUtil.getClosestClassBodyDecl(promisedFor);
				if (!SomeFunctionDeclaration.prototype.includes(enclosingFunc)) {
					if (loc != AnnotationLocation.QUALIFIED_RECEIVER) {
						SLLogger.getLogger().warning("Got a method-related decl within "+
								JavaNames.getFullName(enclosingFunc)+" on a "+loc);
					}
					continue;
				}
				IRNode type = VisitUtil.getEnclosingType(enclosingFunc);
				if (type == null) {
					type = VisitUtil.getEnclosingCompilationUnit(enclosingFunc);
                /*					
				} else if (name().equals(UniquenessRules.CONSISTENCY)) {
					System.out.println("Looking for overrides for "+loc+" on "+JavaNames.getFullName(enclosingFunc));
                */
				}
				final IIRProject p = JavaProjects.getEnclosingProject(type);
				for(IRNode om : p.getTypeEnv().getBinder().findOverridingMethodsFromType(enclosingFunc, type)) {					
					/*
                    if (name().equals(UniquenessRules.CONSISTENCY)) {
						System.out.println("Found override: "+JavaNames.getFullName(om));
					}
                    */
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
					case QUALIFIED_RECEIVER:
					default:
						throw new IllegalStateException("Unexpected location: "+loc);
					}
				}				
			}
            /*
			final Set<IRNode> removed = new HashSet<IRNode>(declsToCheck);
			removed.retainAll(hasAASTs);
			for(IRNode r : removed) {
				IRNode f = VisitUtil.getClosestClassBodyDecl(r);
				System.out.println("Removed "+JavaNames.getFullName(f));
			}
			*/
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
			/*
            if (name().equals(UniquenessRules.CONSISTENCY)) {
				for(IRNode td : methodRelatedDeclsToCheck.keySet()) {
					System.out.println("Got method-related decls to check on "+JavaNames.getFullTypeName(td));
				}
			}
			*/
		}

		/**
		 * Walk ancestors backwards (from roots like java.lang.Object)
		 */
		void walkHierarchy() {
			for (IRNode type : byType.keySet()) {
				IJavaSourceRefType dt = JavaTypeFactory.getMyThisType(type);
				walkHierarchy(dt);
			}
		}

		private void walkHierarchy(final IJavaSourceRefType dt) {
			final IRNode decl = dt.getDeclaration();
			if (done.contains(decl)) {
				return;
			}
			final IIRProject p = JavaProjects.getEnclosingProject(decl);
			// get super types
			for (IJavaType st : p.getTypeEnv().getSuperTypes(dt)) {
				walkHierarchy((IJavaDeclaredType) st);
			}

			boolean cannotSkip = !isPrivateFinalType(p.getTypeEnv(), dt.getDeclaration());
			// process this type
			/*
			final String name = dt.getName();
			if (name().equals(UniquenessRules.CONSISTENCY) && !name.startsWith("java")) {
				System.out.println(name()+" scrubbing promises for "+name);//+" -- "+decl);
			}
			if (name.startsWith("java.util.concurrent.CopyOnWriteArraySet")) {
				System.out.println("Processing CopyOnWriteArraySet");
			}
            */
			List<A> l = byType.get(decl);
			startScrubbingType_internal(decl);
			try {
				final List<IRNode> otherDeclsToCheck = getOtherDeclsToCheck(decl);
				/*
				if (!cannotSkip && otherDeclsToCheck != null && !otherDeclsToCheck.isEmpty()) {
					System.out.println("Ok to skip "+dt);
				}
				*/
				if (l != null && !l.isEmpty()) {							
					processAASTsForType(this, dt.getDeclaration(), l);
					if (cannotSkip && scrubberType == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
						processUnannotatedDeclsForType(dt, otherDeclsToCheck);
					}					
				} else if (cannotSkip) {
					if (scrubberType == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
						processUnannotatedDeclsForType(dt, otherDeclsToCheck);
					} else {
						processUnannotatedType(dt);
					}										
				}
			} finally {
				finishScrubbingType_internal(decl);
			}
			// Mark as done
			done.add(decl);
		}
		
		private List<IRNode> getOtherDeclsToCheck(IRNode decl) {
			if (scrubberType == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
				return methodRelatedDeclsToCheck.get(decl);
		    } else {
		    	return Collections.emptyList();
			}
		}
		
		/**
		 * Changed to use "package" access
		 */
		private boolean isPrivateFinalType(ITypeEnvironment tEnv, IRNode decl) {
			final int mods = JavaNode.getModifiers(decl);
			if (!isTypePrivate(decl, mods)) {
				return false;
			}			
			// Should be private or package-access
			//if (!isBinary(decl)) {
			if (!isPackageBinary(decl)) {
				return false;
			}
			if (JavaNode.getModifier(mods, JavaNode.FINAL)) {
				return true;
			}
			// Check if there are any non-private/"final" subclasses
			for(IRNode sub : tEnv.getRawSubclasses(decl)) {
				if (!isPrivateFinalType(tEnv, sub)) {
					return false;
				}
			}
			return true;
		}
		
		private boolean isTypePrivate(IRNode decl, int mods) {
			if (isPrivate(mods)) {
				return true;
			}
			IRNode enclosingT = VisitUtil.getEnclosingType(decl);
			if (enclosingT == null) {
				// Nothing else to make it inaccessible
				return false;
			}			
			IRNode enclosingD = VisitUtil.getEnclosingClassBodyDecl(decl);
			if (enclosingD != null && enclosingT != enclosingD) {
				// A local type inaccessible to anyone else
				return true;
			}			
			return isTypePrivate(enclosingT, JavaNode.getModifiers(enclosingT));
		}
		
		private boolean isPrivate(int mods) {
			return !JavaNode.getModifier(mods, JavaNode.PUBLIC) && 
			       !JavaNode.getModifier(mods, JavaNode.PROTECTED);
		}
		
		private boolean isPackageBinary(IRNode decl) {
			IRNode cu = VisitUtil.findCompilationUnit(decl);
			String pkg = VisitUtil.getPackageName(cu);
			PackageDrop p = PackageDrop.findPackage(pkg);
			if (p == null) {
				return false; // Unknown
			}
			for(CUDrop cud : p.getCUDrops()) {
				if (cud.isAsSource()) {
					return false;
				}
			}
			return true;
		}
		
		private void processUnannotatedDeclsForType(IJavaSourceRefType dt, List<IRNode> declsToCheck) {
			//int size = declsToCheck == null ? 0 : declsToCheck.size();
			//System.out.println("Looking at "+size+" unannot decls for "+dt);
			if (declsToCheck == null) {
				return;
			}	
			// TODO do I need to sort these? (they're unannotated)		
			// Collections.sort(declsToCheck, nodeComparator);
			for(IRNode decl : declsToCheck) {
				processUnannotatedMethodRelatedDecl(decl);
			}
		}
		
		boolean hasMoreTypes() {
			// Check that we've processed everything
			for (IRNode t : done) {
				byType.remove(t);
			}
			if (!byType.isEmpty()) {
				for (IRNode type : byType.keySet()) {
					final IJavaSourceRefType dt = JavaTypeFactory.getMyThisType(type);
					final IIRProject p = JavaProjects.getEnclosingProject(type);
					// get super types
					for (IJavaType st : p.getTypeEnv().getSuperTypes(dt)) {
						if (st instanceof IJavaDeclaredType) {
							IJavaDeclaredType sdt = (IJavaDeclaredType) st;
							if (!done.contains(sdt.getDeclaration())) {
								throw new Error("Didn't process "+dt+"'s supertype: "+sdt);
							}
						}		
					}
				}
				return true;
			}
			return false;
		}
		
		public void addDerived(A clone, PromiseDrop<? extends A> pd) {
			IRNode ty = VisitUtil.getClosestType(clone.getPromisedFor());
			List<A> l = byType.get(ty);
			if (l == null || l.isEmpty()) {
				l = new ArrayList<A>(1);
				byType.put(ty, l);
			}
			l.add(clone);
			
			finishAddDerived(clone, pd);
		}
	} // end TypeHierarchyVisitor
	
	/**
	 * Loads up byType with the result of getRelevantAnnotations()
	 */
	private void organizeByType(Map<IRNode, List<A>> byType) {
		// Organize by promisedFor
		for (A a : getRelevantAnnotations()) {
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
	
	private void scrubByPromisedFor_Type() {
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		organizeByType(byType);
		
		for(Map.Entry<IRNode, List<A>> e : byType.entrySet()) {
			List<A> l = e.getValue();
			if (l != null && !l.isEmpty()) {
				final IRNode decl = e.getKey();
				startScrubbingType_internal(decl);
				try {
					processAASTsForType(getNullCallback(), decl, l);
				} finally {
					finishScrubbingType_internal(decl);
				}
			}
		}
	}
	
	/**
	 * Scrub the bindings of the specified kind in order of the position of
	 * their promisedFor (assumed to be a type decl) in the type hierarchy
	 */
	private void scrubByPromisedFor_Hierarchy() {
		TypeHierarchyVisitor walk = new TypeHierarchyVisitor();
		walk.init();
		do {
			walk.walkHierarchy();
		} 
		while (walk.hasMoreTypes());
	}
	
	protected void scrubAll(IAnnotationTraversalCallback<A> cb, Iterable<A> all) {
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
}
