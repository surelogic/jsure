/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import java.text.MessageFormat;
import java.util.*;

import org.antlr.runtime.RecognitionException;

import com.surelogic.NonNull;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
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
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
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
      final IAnnotationScrubberContext context, final GuardedByNode a) {
    final LockVisitor v = new LockVisitor(context, a);
    if (v.doAccept(a.getLock())) {
      return new GuardedByPromiseDrop(a);
    } else {
      return null;
    }
  }
  
  // Returning null means that there was a problem and we're not creating anything
  static class LockVisitor extends DescendingVisitor<Boolean> {
    final IAnnotationScrubberContext context;
    final GuardedByNode anno;
    final IRNode target; // being protected
    final IRNode enclosingTypeDecl;
    final boolean isFieldNotMethod;
    final String targetId;
    final int targetMods;
    final boolean targetIsStatic;

    public LockVisitor(
        final IAnnotationScrubberContext context, final GuardedByNode a) {
      super(true);
      this.context = context;
      anno = a;
      target = a.getPromisedFor();
      enclosingTypeDecl = VisitUtil.getEnclosingType(target);
      isFieldNotMethod = VariableDeclarator.prototype.includes(target);
      if (isFieldNotMethod) {
        targetId = VariableDeclarator.getId(target);
        targetMods = VariableDeclarator.getMods(target);
      } else {
        targetId = SomeFunctionDeclaration.getId(target);
        targetMods = SomeFunctionDeclaration.getModifiers(target);
      }
      targetIsStatic = JavaNode.isSet(targetMods, JavaNode.STATIC);
    }

//    private Result makeResultForField(final ExpressionNode lock,
//        final IRNode lockDecl, final String lockId) {
//      // Always need to create an Unique/InRegion, but not Region decl
//      String newRegionId = null;
//      if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
//        if (isPrimTyped(target)) {
//          context.reportError(anno, "Primitive-typed field \"" + targetId
//              + "\" is final and does not need locking");
//          return null;
//        } else {
//          // WRONG: An Object, and thus needs @UniqueInRegion
//          // The JCIP spec for @GuardedBy has no notion of aggregation
//          newRegionId = makeNewInRegion(lockId, false);
//        }
//      } else {
//        newRegionId = makeNewInRegion(lockId, false);
//      }
//      final ExpressionNode field;
//      if (isLockAlreadyDeclared(lockDecl)) {
//        field = null;
//      } else {
//        field = (ExpressionNode) lock.cloneTree();
//      }
//      return new Result(lockDecl, lockId, newRegionId, field);
//    }

//    private Result makeResultForMethod(final ExpressionNode lock,
//        final IRNode lockDecl, final String lockId) {
//      final ExpressionNode field;
//      if (isLockAlreadyDeclared(lockDecl)) {
//        field = null;
//      } else {
//        field = (ExpressionNode) lock.cloneTree();
//      }
//      return new Result(lockDecl, lockId, null, field);
//    }

    @Override
    public Boolean visit(final ThisExpressionNode lock) {
      if (isFieldNotMethod) {
        if (targetIsStatic) {
          context.reportError(anno, "Static field \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportError(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
      } else {
        if (targetIsStatic) {
          context.reportError(anno, "Static method \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
        if (ConstructorDeclaration.prototype.includes(target)) {
          context.reportError(anno, "Constructor \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
      }
      return true;
    }

//    @Override
//    public Result visit(FieldRefNode lock) {
//      final IRNode lockField = lock.resolveBinding().getNode();
//      final int lockMods = VariableDeclarator.getMods(lockField);
//      final boolean lockIsStatic = JavaNode.isSet(lockMods, JavaNode.STATIC);
//      final String targetLabel = isFieldNotMethod ? "field" : "method";
//      if (targetIsStatic && !lockIsStatic) {
//        context.reportError(anno, "Static " + targetLabel + " \"" + targetId
//            + "\" cannot be guarded by instance lock \"" + lock.getId() + "\"");
//        return null;
//      }
//      if (!targetIsStatic && lockIsStatic) {
//        // Should this really prevent it from being validated?
//        context.reportError("Instance " + targetLabel + " \"" + targetId
//            + "\" should not be guarded by static lock \"" + lock.getId()
//            + "\"", anno);
//        return null;
//      }
//      final String lockId = VariableDeclarator.getId(lockField);
//      if (isFieldNotMethod) {
//        return makeResultForField(lock, lockField, lockId);
//      } else {
//        return makeResultForMethod(lock, lockField, lockId);
//      }
//    }

    @Override
    public Boolean visit(final ClassExpressionNode lock) {
      if (isFieldNotMethod) {
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportError(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
      } 
      return true;
    }

    @Override
    public Boolean visit(final QualifiedThisExpressionNode lock) {
      if (isFieldNotMethod) {
        if (targetIsStatic) {
          context.reportError(anno, "Static field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportError(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
      } else {
        if (targetIsStatic) {
          context.reportError(anno, "Static method \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
        if (ConstructorDeclaration.prototype.includes(target)) {
          context.reportError(anno, "Constructor \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
      }
      return true;
    }

    @Override
    public Boolean visit(final ItselfNode lock) {
      if (!isFieldNotMethod) {
        context.reportError(anno,
            "Method/constructor \"" + targetId + 
            "\" cannot be annotated to guard itself");
        return false;
      }
      if (isPrimitiveTyped(target)) {
        context.reportError(anno,
            "Field \"" + targetId + 
            "\" cannot guard itself because it does not have a reference type");
        return false;
      }
      if (!JavaNode.isSet(targetMods, JavaNode.FINAL)) {
        context.reportError(anno, "Field \"" + targetId
            + "\" cannot guard itself becaust it must be final");
        return false;
      }
      return true;
    }

//    @Override
//    public Result visit(MethodCallNode lock) { // no-args method
//      context.reportError(
//          "Unconverted @GuardedBy: currently unable to handle method " + lock
//              + "() as a lock", anno);
//      return null;
//    }

//    public Result visit(ExpressionNode lock) {
//      context.reportError("Unconverted @GuardedBy: " + lock, anno);
//      return null;
//    }

//    private String makeNewInRegion(String lockId, boolean alsoUnique) {
//      final String newRegionId = MessageFormat.format("State$_{0}", lockId);
//      return newRegionId;
//    }
//
    private static boolean isPrimitiveTyped(final IRNode fieldDecl) {
      final IRNode type = VariableDeclarator.getType(fieldDecl);
      return PrimitiveType.prototype.includes(type);
    }
  }
}
