package com.surelogic.annotation.scrub;

import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.annotation.AnnotationLocation;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class AbstractHierarchyScrubber<A extends IHasPromisedFor> extends AbstractScrubber {
	final ScrubberType scrubberType;
	
	public AbstractHierarchyScrubber(ScrubberType type, String[] before, String name, ScrubberOrder order, String[] deps) {
		super(before, name, order, deps);
		scrubberType = type;
	}

	// Things to override
	protected abstract Iterable<A> getRelevantAnnotations(Class<A> c);	
	protected abstract void organizeByType(Class<A> c, Map<IRNode, List<A>> byType);
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
	protected boolean processUnannotatedType(IJavaDeclaredType dt) {
		return true;
	}
	
	protected void finishScrubbingType(IRNode decl) {
		// Nothing to do right now
	}
	
	/**
	 * Visits stuff (usually annotations/promises) hanging off the type hierarchy
	 */
	protected final class TypeHierarchyVisitor implements IAnnotationTraversalCallback<A> {	
		// Empty list = a subclass w/o required annos
		final Map<IRNode, List<A>> byType = new HashMap<IRNode, List<A>>();
		final Map<IRNode, List<IRNode>> methodRelatedDeclsToCheck = new HashMap<IRNode, List<IRNode>>();
		final Set<IRNode> done = new HashSet<IRNode>();
		
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
			for (A a : getRelevantAnnotations(c)) {
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
					if (scrubberType == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
						otherDeclsToCheck = methodRelatedDeclsToCheck.get(decl);
				    } else {
						otherDeclsToCheck = Collections.emptyList();
					}
					if (l == Collections.emptyList()) {
						if (scrubberType == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
							processUnannotatedDeclsForType(otherDeclsToCheck);
						} else {
							processUnannotatedType(dt);
						}
					} else {
						processAASTsForType(this, dt.getDeclaration(), l);
						if (scrubberType == ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
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
		
		private void processUnannotatedDeclsForType(List<IRNode> declsToCheck) {
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
					final IJavaDeclaredType dt = JavaTypeFactory.getMyThisType(type);
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
	
	protected final void startScrubbingType_internal(IRNode decl) {
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
	
	protected final void finishScrubbingType_internal(IRNode decl) {
		if (useAssumptions()) {
			PromiseFramework.getInstance().popTypeContext();
		}
		finishScrubbingType(decl);
	}
}
