/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import java.text.MessageFormat;
import java.util.*;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.java.ClassExpressionNode;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.MethodCallNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.promise.GuardedByNode;
import com.surelogic.aast.promise.ItselfNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.aast.promise.NewRegionDeclarationNode;
import com.surelogic.aast.promise.QualifiedClassLockExpressionNode;
import com.surelogic.aast.promise.RegionNameNode;
import com.surelogic.aast.promise.UniqueInRegionNode;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
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
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class JcipRules extends AnnotationRules {
	public static final String GUARDED_BY = "GuardedBy";

	private static final AnnotationRules instance = new JcipRules();

	private static final GuardedBy_ParseRule guardedByRule = new GuardedBy_ParseRule();

	/**
	 * The IRNode of the lock field (class, or receiver decl)
	 * 
	 * A hack, but it works because we're no longer doing incremental runs
	 */	
	private static final Set<IRNode> declaredLocks = new HashSet<IRNode>();
	
	/**
	 *  Referenced region also declared at the same time
	 */
	private static boolean isLockAlreadyDeclared(IRNode lockNode) {		
		if (lockNode == null) {
			throw new NullPointerException();
		}
		return declaredLocks.contains(lockNode);
	}
			
	public static AnnotationRules getInstance() {
		return instance;
	}

	/*
	public static VouchPromiseDrop getVouchSpec(IRNode decl) {
		return getDrop(vouchRule.getStorage(), decl);
	}
	*/

	/**
	 * Returns the closest vouch applicable for the given IRNode, if any
	 */
	/*
	public static VouchPromiseDrop getEnclosingVouch(final IRNode n) {
		IRNode decl = VisitUtil.getClosestDecl(n);
		while (decl != null) {
			Operator op = JJNode.tree.getOperator(decl);
			if (ClassBodyDeclaration.prototype.includes(op)
					|| TypeDeclaration.prototype.includes(op)) {
				VouchPromiseDrop rv = getVouchSpec(decl);
				if (rv != null) {
					return rv;
				}
			}
			decl = VisitUtil.getEnclosingDecl(decl);
		}
		return null;
	}
	*/

	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, guardedByRule);
	}

	static class GuardedBy_ParseRule
			extends
			DefaultSLAnnotationParseRule<GuardedByNode, GuardedByPromiseDrop> {
		protected GuardedBy_ParseRule() {
			super(GUARDED_BY, fieldDeclOp, GuardedByNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws RecognitionException {
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
			// TODO group similar decls within a type?
			return new AbstractAASTScrubber<GuardedByNode, GuardedByPromiseDrop>(this, ScrubberType.UNORDERED, 
					new String[] { RegionRules.REGION, LockRules.LOCK, RegionRules.SIMPLE_UNIQUE_IN_REGION }, SLUtility.EMPTY_STRING_ARRAY) {
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
      final IAnnotationScrubberContext context, final GuardedByNode a) {
    final GuardedByPromiseDrop d = new GuardedByPromiseDrop(a);
    if (MethodDeclaration.prototype.includes(a.getPromisedFor())) {
    	return handleGuardedByOnMethod(context, d);
    }
    
    final ExpressionNode lock = a.getLock();
    final IRNode fieldDecl = a.getPromisedFor();
    final IRNode classDecl = VisitUtil.getEnclosingType(fieldDecl);
    final String fieldId = VariableDeclarator.getId(fieldDecl);
    final int fieldMods = VariableDeclarator.getMods(fieldDecl);
    final boolean fieldIsStatic = JavaNode.isSet(fieldMods, JavaNode.STATIC);    
    final IRNode lockDecl;
	final String lockId;
    
    // Either of the below might be null to indicate that we shouldn't create a new region/lock decl
    // since they are already declared
    String newRegionId = null;
    final ExpressionNode field;
    if (lock instanceof ThisExpressionNode) {
      if (fieldIsStatic) {
    	  context.reportError(a, "Static field \""+fieldId+"\" cannot be guarded by \"this\"");
    	  return d;
      }
      lockDecl = JavaPromise.getReceiverNode(classDecl);
      lockId = "this";
      
      if (JavaNode.isSet(fieldMods, JavaNode.FINAL)) {
          if (isPrimTyped(fieldDecl)) {
        	  context.reportError(a, "Primitive-typed field \""+fieldId+"\" is final and does not need locking");
        	  return d;
          } else {
        	  // An Object, and thus needs @UniqueInRegion
              newRegionId = makeNewUniqueInRegion(d, lockId);
          }
      }
      if (isLockAlreadyDeclared(lockDecl)) {
    	  field = null;
      } else {
    	  field = (ThisExpressionNode) lock.cloneTree();
      }
    } else if (lock instanceof FieldRefNode) {
      final FieldRefNode ref = (FieldRefNode) lock;
      final IRNode lockField = ref.resolveBinding().getNode();
      final int lockMods = VariableDeclarator.getMods(lockField);
      final boolean lockIsStatic = JavaNode.isSet(lockMods, JavaNode.STATIC);
      if (fieldIsStatic && !lockIsStatic) {
    	  context.reportError(a, "Static field \""+fieldId+"\" cannot be guarded by instance lock \""+ref.getId()+"\"");
    	  return d;
      }
      if (!fieldIsStatic && lockIsStatic) {
    	  // Should this really prevent it from being validated?
    	  context.reportError("Instance field \""+fieldId+"\" should not be guarded by static lock \""+ref.getId()+"\"", a);
    	  return d;
      }
      lockId = VariableDeclarator.getId(lockField);
    	  
      if (isLockAlreadyDeclared(lockField)) {
    	  field = null;
      } else {
          if (JavaNode.isSet(fieldMods, JavaNode.FINAL)) {
              if (isPrimTyped(fieldDecl)) {
            	  context.reportError(a, "Primitive-typed field \""+fieldId+"\" is final and does not need locking");
            	  return d;
              } else {
            	  // An Object, and thus needs @UniqueInRegion            	  
                  newRegionId = makeNewUniqueInRegion(d, lockId);
              }
          }
    	  field = (FieldRefNode) lock.cloneTree();
      }
    } else if (lock instanceof ClassExpressionNode) {
      final ClassExpressionNode classExpr = (ClassExpressionNode) lock;
      if (isLockAlreadyDeclared(classDecl)) {
    	  field = null;
      } else {
    	  field = new QualifiedClassLockExpressionNode(lock.getOffset(),
    			  (NamedTypeNode) classExpr.getType().cloneTree());
      }
      lockId = classExpr.getType()+"_class";  	  
    } else if (lock instanceof QualifiedThisExpressionNode) {
      final QualifiedThisExpressionNode qThis = (QualifiedThisExpressionNode) lock;
      if (isLockAlreadyDeclared(qThis.resolveBinding().getNode())) {
      	  field = null;
      } else {
    	  field = (QualifiedThisExpressionNode) lock.cloneTree();   
      }
      lockId = qThis.getType()+"_this";
    } else if (lock instanceof ItselfNode) {
      if (isPrimTyped(fieldDecl)) {
    	  context.reportError(a, "Primitive-typed field \""+fieldId+"\" cannot guard itself");
    	  return d;
      }
      lockId = fieldId;
      
      if (isLockAlreadyDeclared(fieldDecl)) {
      	  field = null;
      } else {
    	  // An Object, and thus needs @UniqueInRegion
    	  newRegionId = makeNewUniqueInRegion(d, lockId);
    	  field = new FieldRefNode(0, new ThisExpressionNode(0), fieldId);
      }
    //} else if (lock instanceof MethodCallNode) { // no-args method
    } else {
    	context.reportError("Unconverted @GuardedBy: "+lock, a);
    	return d;
    }
    if (field != null) {
    	final RegionNameNode region;
    	if (newRegionId != null) {    	
    		region = new RegionNameNode(a.getOffset(), newRegionId);

    		final NewRegionDeclarationNode regionDecl = 
    				new NewRegionDeclarationNode(0, extractAccessMods(fieldMods), newRegionId, null);
    		regionDecl.setPromisedFor(classDecl, a.getAnnoContext());
    		regionDecl.setSrcType(a.getSrcType());
    		AASTStore.addDerived(regionDecl, d);
    	} else {
    		region = new RegionNameNode(a.getOffset(), fieldId);
    	}
    	// FIX based on the lock
    	final String id = MessageFormat.format("Guard$_{0}", lockId);
    	final LockDeclarationNode regionLockDecl =
    			new LockDeclarationNode(a.getOffset(), id, field, region);
    	regionLockDecl.setPromisedFor(classDecl, a.getAnnoContext());
    	regionLockDecl.setSrcType(a.getSrcType());
    	AASTStore.addDerived(regionLockDecl, d);
    }
    return d;
  }
  
  /* Converts to a RequiresLock */
  private static GuardedByPromiseDrop handleGuardedByOnMethod(IAnnotationScrubberContext context, GuardedByPromiseDrop d) {
	// TODO Auto-generated method stub
	  
	final LockSpecificationNode lsn;
	  
	return d;
  }

private static String makeNewUniqueInRegion(GuardedByPromiseDrop d, String lockId) {
	  final GuardedByNode a = d.getAAST();
	  final IRNode fieldDecl = a.getPromisedFor();
      final String newRegionId = MessageFormat.format("State$_{0}", lockId);
      
      final UniqueInRegionNode uir = new UniqueInRegionNode(0, new RegionNameNode(0, newRegionId), false);
      uir.setPromisedFor(fieldDecl, a.getAnnoContext());
      uir.setSrcType(a.getSrcType());
      AASTStore.addDerived(uir, d);
      return newRegionId;      
  }
  
  private static boolean isPrimTyped(IRNode fieldDecl) {
	  // Check if it's an Object type
      final IRNode type = VariableDeclarator.getType(fieldDecl);
      return PrimitiveType.prototype.includes(type);
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
