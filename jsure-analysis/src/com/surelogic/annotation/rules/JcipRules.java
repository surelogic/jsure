/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import java.text.MessageFormat;
import java.util.*;

import org.antlr.runtime.RecognitionException;

import com.surelogic.NonNull;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.AnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberOrder;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.common.SLUtility;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class JcipRules extends AnnotationRules {
	public static final String GUARDED_BY = "GuardedBy";

	private static final AnnotationRules instance = new JcipRules();

	private static final GuardedBy_ParseRule guardedByRule = new GuardedBy_ParseRule();
	private static final IAnnotationScrubber methodScrubber = 
		new AbstractAASTScrubber<MethodGuardedByNode, GuardedByPromiseDrop>("methodGuardedBy", MethodGuardedByNode.class, 
				guardedByRule.getStorage(), ScrubberType.UNORDERED, 
				new String[] { LockRules.REQUIRES_LOCK }, ScrubberOrder.NORMAL, GUARDED_BY) {
		@Override
		protected PromiseDrop<GuardedByNode> makePromiseDrop(MethodGuardedByNode a) {
			return storeDropIfNotNull(a, scrubGuardedBy(getContext(), a));
		}
	};
	
	/**
	 * The IRNode of the lock field (class, or receiver decl)
	 * 
	 * A hack, but it works because we're no longer doing incremental runs
	 */	
	private static final Map<IRNode,LockDeclarationNode> declaredLocks = new HashMap<IRNode,LockDeclarationNode>();
	
	/**
	 *  Referenced region also declared at the same time
	 *  
	 *  Also adds the node to the set
	 */
	private static boolean isLockAlreadyDeclared(IRNode lockNode) {		
		if (lockNode == null) {
			throw new NullPointerException();
		}
		return declaredLocks.containsKey(lockNode);
	}
			
	public static AnnotationRules getInstance() {
		return instance;
	}

	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, guardedByRule);
		registerScrubber(fw, methodScrubber);
	}

	static class GuardedBy_ParseRule
			extends
			DefaultSLAnnotationParseRule<GuardedByNode, GuardedByPromiseDrop> {
		protected GuardedBy_ParseRule() {
			super(GUARDED_BY, fieldMethodDeclOps, GuardedByNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws RecognitionException {
			if (MethodDeclaration.prototype.includes(context.getOp())) {
				return parser.methodGuardedBy().getTree();
			}
			return parser.guardedBy().getTree();
		}

		@Override
		protected IPromiseDropStorage<GuardedByPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					GuardedByPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			// Run this before Lock to create virtual declarations
			return new AbstractAASTScrubber<GuardedByNode, GuardedByPromiseDrop>(this, ScrubberType.UNORDERED, 
					new String[] { RegionRules.REGION, LockRules.LOCK, RegionRules.IN_REGION, RegionRules.SIMPLE_UNIQUE_IN_REGION }, 
					SLUtility.EMPTY_STRING_ARRAY) {
				@Override
				protected PromiseDrop<GuardedByNode> makePromiseDrop(GuardedByNode a) {
					return storeDropIfNotNull(a, scrubGuardedBy(getContext(), a));
				}
			};
		}
	}
	
  /**
   * If using the lock to create names for region/lock decls,
   * -- no need to create a "field" expr if DUPLICATE
   */
  private static GuardedByPromiseDrop scrubGuardedBy(
      final AnnotationScrubberContext context, final GuardedByNode a) {
    final GuardedByPromiseDrop d = new GuardedByPromiseDrop(a);
	final LockVisitor v = new LockVisitor(context, d); 
	final Result rv = v.doAccept(a.getLock());
    if (rv == null) {
    	d.invalidate();
    	return null;
    }
    if (v.isFieldNotMethod) {
        if (rv.lockField != null) {        
        	// Changed to create a new region for every lock decl (instead of depending on the type of field)
        	final RegionNameNode region = new RegionNameNode(a.getOffset(), rv.newRegionId);

        	final NewRegionDeclarationNode regionDecl = 
        			new NewRegionDeclarationNode(0, extractAccessMods(v.targetMods), rv.newRegionId, null);
        	regionDecl.copyPromisedForContext(v.enclosingTypeDecl, a, AnnotationOrigin.SCOPED_ON_TYPE);
        	AASTStore.addDerived(regionDecl, d);

        	final LockDeclarationNode regionLockDecl =
        			new LockDeclarationNode(a.getOffset(), rv.lockId, rv.lockField, region);
        	regionLockDecl.copyPromisedForContext(v.enclosingTypeDecl, a, AnnotationOrigin.SCOPED_ON_TYPE);
        	AASTStore.addDerived(regionLockDecl, d);
        	declaredLocks.put(rv.lockNode, regionLockDecl);
        } else {
        	// Link the @GuardedBy drop to the lock model when it's created
        	final LockDeclarationNode previousDecl = declaredLocks.get(rv.lockNode);
        	AASTStore.triggerWhenValidated(previousDecl, new ValidatedDropCallback<PromiseDrop<?>>() {
				@Override
				public void validated(PromiseDrop<?> lm) {				
					d.addDependent(lm);
				}
			});
        }
    } else {
    	// Create a policy lock if there isn't already a lock decl
    	if (rv.lockField != null) {
    		final PolicyLockDeclarationNode lockDecl = new PolicyLockDeclarationNode(0, rv.lockId, rv.lockField);
    		lockDecl.copyPromisedForContext(v.enclosingTypeDecl, a, AnnotationOrigin.SCOPED_ON_TYPE);
        	AASTStore.addDerived(lockDecl, d);
    	}    	
    	/* Converts to a RequiresLock for a method */
    	final LockSpecificationNode spec = new SimpleLockNameNode(0, rv.lockId);  
    	final RequiresLockNode req = new RequiresLockNode(0, Collections.singletonList(spec));
    	req.copyPromisedForContext(d.getPromisedFor(), a, AnnotationOrigin.GENERATED_FOR_DECL);
    	AASTStore.addDerived(req, d);
    }
    return d;
  }
  
  // Returning null means that there was a problem and we're not creating anything
  static class LockVisitor extends DescendingVisitor<Result> {		
	final AnnotationScrubberContext context;
	final GuardedByPromiseDrop drop;
	final GuardedByNode anno;
    final IRNode target; // being protected
    final IRNode enclosingTypeDecl;
	final boolean isFieldNotMethod;
    final String targetId;
    final int targetMods;
    final boolean targetIsStatic;  
	
	public LockVisitor(final AnnotationScrubberContext context, final GuardedByPromiseDrop d) {
		super(null);
		this.context = context;
		drop = d;
		anno = d.getAAST();
		target = d.getPromisedFor();		
		enclosingTypeDecl = VisitUtil.getEnclosingType(target);
		isFieldNotMethod = VariableDeclarator.prototype.includes(target);
		if (isFieldNotMethod) {
			targetId = VariableDeclarator.getId(target);
			targetMods = VariableDeclarator.getMods(target);
		} else {
			targetId = MethodDeclaration.getId(target);
			targetMods = MethodDeclaration.getModifiers(target);
		}
		targetIsStatic = JavaNode.isSet(targetMods, JavaNode.STATIC);    
	}

	private Result makeResultForField(final ExpressionNode lock, final IRNode lockDecl, final String lockId) {
		// Always need to create an Unique/InRegion, but not Region decl
		String newRegionId = null;		
		if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
			if (isPrimTyped(target)) {
				context.reportModelingProblem(anno, "Primitive-typed field \""+targetId+"\" is final and does not need locking");
				return null;
			} else {
				// WRONG: An Object, and thus needs @UniqueInRegion
				// The JCIP spec for @GuardedBy has no notion of aggregation
				newRegionId = makeNewInRegion(lockId, false);
			}
		} else {
			newRegionId = makeNewInRegion(lockId, false);
		}
		final ExpressionNode field;
		if (isLockAlreadyDeclared(lockDecl)) {
			field = null;
		} else {
			field = (ExpressionNode) lock.cloneTree();
		}
		return new Result(lockDecl, lockId, newRegionId, field);
	}
	
	private Result makeResultForMethod(final ExpressionNode lock, final IRNode lockDecl, final String lockId) {
		final ExpressionNode field;
		if (isLockAlreadyDeclared(lockDecl)) {
			field = null;
		} else {
			field = (ExpressionNode) lock.cloneTree();
		}
		return new Result(lockDecl, lockId, null, field);
	}
	
	public Result visit(ThisExpressionNode lock) {
		if (targetIsStatic) {
			context.reportModelingProblem(anno, "Static field \""+targetId+"\" cannot be guarded by \"this\"");
			return null;
		}
		final IRNode lockDecl = JavaPromise.getReceiverNode(enclosingTypeDecl);
		final String lockId = "this";
		if (isFieldNotMethod) {
			return makeResultForField(lock, lockDecl, lockId);
		} else {
			return makeResultForMethod(lock, lockDecl, lockId);
		}
	}
	
	public Result visit(FieldRefNode lock) {
		final IRNode lockField = lock.resolveBinding().getNode();
		final int lockMods = VariableDeclarator.getMods(lockField);
		final boolean lockIsStatic = JavaNode.isSet(lockMods, JavaNode.STATIC);
		final String targetLabel = isFieldNotMethod ? "field" : "method";
		if (targetIsStatic && !lockIsStatic) {
			context.reportModelingProblem(anno, "Static "+targetLabel+" \""+targetId+"\" cannot be guarded by instance lock \""+lock.getId()+"\"");
			return null;
		}
		if (!targetIsStatic && lockIsStatic) {
			// Should this really prevent it from being validated?
			context.reportError("Instance "+targetLabel+" \""+targetId+"\" should not be guarded by static lock \""+lock.getId()+"\"", anno);
			return null;
		}
		final String lockId = VariableDeclarator.getId(lockField);		
		if (isFieldNotMethod) {
			return makeResultForField(lock, lockField, lockId);
		} else {
			return makeResultForMethod(lock, lockField, lockId);
		}
	}
	
	public Result visit(ClassExpressionNode lock) {
		final String targetLabel = isFieldNotMethod ? "field" : "method";
		final String id = lock.getType().toString().replace('.', '$')+"_class";
		final ExpressionNode field;
		String newRegionId = isFieldNotMethod ? makeNewInRegion(id, false) : null;
		if (!targetIsStatic) {
			context.reportError("Instance "+targetLabel+" \""+targetId+"\" should not be guarded by static lock \""+lock+"\"", anno);
			return null;
		}
		if (isLockAlreadyDeclared(enclosingTypeDecl)) {
			field = null;
		} else {
			field = new QualifiedClassLockExpressionNode(lock.getOffset(),
					(NamedTypeNode) lock.getType().cloneTree());
		}
		return new Result(enclosingTypeDecl, id, newRegionId, field);
	}
	
	public Result visit(QualifiedThisExpressionNode lock) {		
		if (true) {
			context.reportError("Unconverted @GuardedBy: currently unable to handle qualified receiver "+lock+" as a lock", anno);
			return null;
		}
		final IRNode lockNode = lock.resolveBinding().getNode();
		final String id = lock.getType().toString().replace('.', '$')+"_this";
		if (isFieldNotMethod) {
			return makeResultForField(lock, lockNode, id);
		}
		return makeResultForMethod(lock, lockNode, id);
	}
	
	public Result visit(ItselfNode lock) {
		if (!isFieldNotMethod) {
			context.reportModelingProblem(anno, "A method cannot be annotated to guard itself");
			return null;
		}
		if (isPrimTyped(target)) {
			context.reportModelingProblem(anno, "Primitive-typed field \""+targetId+"\" cannot guard itself");
			return null;
		}
		String newRegionId = makeNewInRegion(targetId, true);
		final ExpressionNode field;
		if (isLockAlreadyDeclared(target)) {
			field = null;
		} else {
			// An Object, and thus needs @UniqueInRegion	
			field = new FieldRefNode(0, new ThisExpressionNode(0), targetId);
		}
		return new Result(target, targetId, newRegionId, field);
	}
	
	public Result visit(MethodCallNode lock) { // no-args method
    	context.reportError("Unconverted @GuardedBy: currently unable to handle method "+lock+"() as a lock", anno);
		return null;
	}
	
	public Result visit(ExpressionNode lock) {
    	context.reportError("Unconverted @GuardedBy: "+lock, anno);
		return null;
	}
	
	private String makeNewInRegion(String lockId, boolean alsoUnique) {
		final String newRegionId = MessageFormat.format("State$_{0}", lockId);
		final RegionNameNode name = new RegionNameNode(0, newRegionId);
		final AASTRootNode root;
		if (alsoUnique) {
			root = new UniqueInRegionNode(0, name);
		} else {
			root = new InRegionNode(0, name);
		}
		root.copyPromisedForContext(target, anno, AnnotationOrigin.GENERATED_FOR_DECL);
		AASTStore.addDerived(root, drop);
		return newRegionId;      
	}
	
	private static boolean isPrimTyped(IRNode fieldDecl) {
		// Check if it's an Object type
		final IRNode type =	VariableDeclarator.getType(fieldDecl);
		return PrimitiveType.prototype.includes(type);
	}
  }
  
  static class Result {
	@NonNull
	final IRNode lockNode;
	  
	@NonNull
	final String lockId;	  
    
    // Either of the below might be null to indicate that we shouldn't create a new region/lock decl
    // since they are already declared
	// 
	// Only used if it's for a field
	final String newRegionId;
    final ExpressionNode lockField;
    
	Result(IRNode n, String id, String rid, ExpressionNode f) {
		lockNode = n;
		lockId = MessageFormat.format("Guard$_{0}", id);
		newRegionId = rid;
		lockField = f;
	}
	Result(IRNode n, String id) {
		this(n, id, null, null);
	}
  }

  private static int extractAccessMods(final int mods) {
	  final boolean isStatic = JavaNode.isSet(mods, JavaNode.STATIC);
	  if (JavaNode.isSet(mods, JavaNode.PRIVATE)) {
		  if (isStatic) {
			  return JavaNode.PRIVATE | JavaNode.STATIC;
		  }
		  return JavaNode.PRIVATE;
	  }
	  if (JavaNode.isSet(mods, JavaNode.PROTECTED)) {
		  if (isStatic) {
			  return JavaNode.PROTECTED | JavaNode.STATIC;
		  }
		  return JavaNode.PROTECTED;
	  }
	  if (JavaNode.isSet(mods, JavaNode.PUBLIC)) {
		  if (isStatic) {
			  return JavaNode.PUBLIC | JavaNode.STATIC;
		  }
		  return JavaNode.PUBLIC;
	  }	 
	  return isStatic ? JavaNode.STATIC : JavaNode.ALL_FALSE;
  }
}
