/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.AnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberOrder;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.common.SLUtility;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

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
    final LockVisitor v = new LockVisitor(context, a);
    if (v.doAccept(a.getLock())) {
      final GuardedByPromiseDrop guardedByPromiseDrop = new GuardedByPromiseDrop(a);
      v.addHint(guardedByPromiseDrop);
      return guardedByPromiseDrop;
    } else {
      return null;
    }
  }
  
  // Returning null means that there was a problem and we're not creating anything
  static class LockVisitor extends DescendingVisitor<Boolean> {
    final AnnotationScrubberContext context;
    final GuardedByNode anno;
    final IRNode target; // being protected
    final IRNode enclosingTypeDecl;
    final boolean isFieldNotMethod;
    final String targetId;
    final int targetMods;
    final boolean targetIsStatic;

    private int hintMessage = -1;
    
    
    
    public LockVisitor(
        final AnnotationScrubberContext context, final GuardedByNode a) {
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

    
    
    public void addHint(final Drop drop) {
      if (hintMessage != -1) {
        final HintDrop hint = HintDrop.newInformation(target);
        hint.setMessage(hintMessage);
        drop.addDependent(hint);
      }
    }
    
    
    
    @Override
    public Boolean visit(final ThisExpressionNode lock) {
      if (isFieldNotMethod) {
        if (targetIsStatic) {
          context.reportModelingProblem(anno, "Static field \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportModelingProblem(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
      } else {
        if (targetIsStatic) {
          context.reportModelingProblem(anno, "Static method \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
        if (ConstructorDeclaration.prototype.includes(target)) {
          context.reportModelingProblem(anno, "Constructor \"" + targetId
              + "\" cannot be guarded by \"this\"");
          return false;
        }
      }
      return true;
    }

    @Override
    public Boolean visit(final FieldRefNode lock) {
      final IRNode lockField = lock.resolveBinding().getNode();
      final int lockMods = VariableDeclarator.getMods(lockField);
      
      if (!JavaNode.isSet(lockMods, JavaNode.FINAL)) {
        context.reportModelingProblem(anno,
            "Lock field \"" + lock + "\" must be final");
        return false;
      }
      if (isPrimitiveTyped(lockField)) {
        context.reportModelingProblem(anno,
            "Lock field \"" + lock + "\" must have a reference type");
        return false;
      }
      
      if (isFieldNotMethod) {
        final boolean lockFieldIsNotStatic = !JavaNode.isSet(lockMods, JavaNode.STATIC);
        if (lockFieldIsNotStatic) {
          final IRNode fieldInType = VisitUtil.getEnclosingType(lockField);
          final boolean isFieldFromAncestor = TypeUtil.isAncestorOf(
              context.getBinder(fieldInType).getTypeEnvironment(),
              JavaTypeFactory.getMyThisType(fieldInType),
              JavaTypeFactory.getMyThisType(enclosingTypeDecl));
          if (!isFieldFromAncestor) {
            context.reportModelingProblem(anno,
                "Lock field \"" + lock + 
                "\" must be declared in an ancestor of the annotated class");
            return false;
          }
        }
        
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportModelingProblem(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
        if (targetIsStatic && lockFieldIsNotStatic) {
          context.reportModelingProblem(anno, "Static field \"" + targetId +
              "\" cannot be guarded by the non-static field \"" + lock + "\"");
          return false;
        }
      } else { // annotate method or constructor
        // check named parameters, etc
      }
      
      return true;
    }

    @Override
    public Boolean visit(final ClassExpressionNode lock) {
      if (isFieldNotMethod && JavaNode.isSet(targetMods, JavaNode.FINAL)) {
        context.reportModelingProblem(anno, "Final field \"" + targetId
            + "\" cannot be guarded by \"" + lock + "\"");
        return false;
      } 
      return true;
    }

    @Override
    public Boolean visit(final QualifiedThisExpressionNode lock) {
      if (isFieldNotMethod) { // Field
        if (targetIsStatic) {
          context.reportModelingProblem(anno, "Static field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportModelingProblem(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
      } else { // Method or Construtor
        if (targetIsStatic) {
          context.reportModelingProblem(anno, "Static method \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
        if (ConstructorDeclaration.prototype.includes(target)) { // constructor
          // Can use on constructor as long as it isn't really longhand for "this"
          final IRNode qualifierTypeDecl = lock.getType().resolveType().getNode();
          if (qualifierTypeDecl.equals(enclosingTypeDecl)) {
            context.reportModelingProblem(anno, "Constructor \"" + targetId
                + "\" cannot be guarded by \"" + lock + "\"; it is the same as \"this\"");
            return false;
          }
        }
      }
      
      hintMessage = 280;
      return true;
    }

    @Override
    public Boolean visit(final ItselfNode lock) {
      if (!isFieldNotMethod) { // Constructor or method
        context.reportModelingProblem(anno,
            "Method/constructor \"" + targetId + 
            "\" cannot be annotated to guard itself");
        return false;
      } else { // Field
        if (isPrimitiveTyped(target)) {
          context.reportModelingProblem(anno,
              "Field \"" + targetId + 
              "\" cannot guard itself because it does not have a reference type");
          return false;
        }
        if (!JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportModelingProblem(anno, "Field \"" + targetId
              + "\" cannot guard itself becaust it must be final");
          return false;
        }
      }
      return true;
    }

    @Override
    public Boolean visit(final MethodCallNode lock) {
      // cannot be a constructor
      // must be nil-ary
      // must return a  reference type
      // treat like a field look up
      // field must be mutable
      // requireslock is also like a field (so wait for e-mail response)
      
      final IRNode methodDecl = lock.resolveBinding().getNode();
      final int methodMods = SomeFunctionDeclaration.getModifiers(methodDecl);
      
      /* Cannot name a constructor.  Binder actually chokes on this first,
       * but check it here in case things change in the future.
       */
      if (ConstructorDeclaration.prototype.includes(methodDecl)) {
        context.reportModelingProblem(anno,
            "Constructor \"" + lock + "\"cannot be used as a lock");
        return false;
      } else { // Definitely a method declaration
        /* Must return a reference type */
        final IRNode returnType = MethodDeclaration.getReturnType(methodDecl);
        if (!ReferenceType.prototype.includes(returnType)) {
          context.reportModelingProblem(anno,
              "Lock method \"" + lock + "\" must return a reference type");
          return false;
        }
        
        /* Must have no arguments. Binder actually chokes on this first,
         * but check it here in case things change in the future.
         */
        if (JJNode.tree.numChildren(MethodDeclaration.getParams(methodDecl)) > 0) {
          context.reportModelingProblem(anno,
              "Lock method \"" + lock + "\" must have no formal parameters");
          return false;
        }
      }
      
      if (isFieldNotMethod) {
        final boolean methodIsNotStatic = !JavaNode.isSet(methodMods, JavaNode.STATIC);
        if (methodIsNotStatic) {
          final IRNode methodInType = VisitUtil.getEnclosingType(methodDecl);
          final boolean isMethodFromAncestor = TypeUtil.isAncestorOf(
              context.getBinder(methodInType).getTypeEnvironment(),
              JavaTypeFactory.getMyThisType(methodInType),
              JavaTypeFactory.getMyThisType(enclosingTypeDecl));
          if (!isMethodFromAncestor) {
            context.reportModelingProblem(anno,
                "Lock method \"" + lock + 
                "\" must be declared in an ancestor of the annotated class");
            return false;
          }
        }
        
        if (JavaNode.isSet(targetMods, JavaNode.FINAL)) {
          context.reportModelingProblem(anno, "Final field \"" + targetId
              + "\" cannot be guarded by \"" + lock + "\"");
          return false;
        }
        if (targetIsStatic && methodIsNotStatic) {
          context.reportModelingProblem(anno, "Static field \"" + targetId +
              "\" cannot be guarded by the non-static method \"" + lock + "\"");
          return false;
        }
      } else { // annotate method or constructor
        // check named parameters, etc
      }
      
      return true;
    }

    private static boolean isPrimitiveTyped(final IRNode fieldOrMethodDecl) {
      final IRNode type = VariableDeclarator.getType(fieldOrMethodDecl);
      return PrimitiveType.prototype.includes(type);
    }
  }
}
