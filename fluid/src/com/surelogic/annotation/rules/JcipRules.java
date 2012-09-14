/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import java.text.MessageFormat;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.java.ClassExpressionNode;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class JcipRules extends AnnotationRules {
	public static final String GUARDED_BY = "GuardedBy";

	private static final AnnotationRules instance = new JcipRules();

	private static final GuardedBy_ParseRule guardedByRule = new GuardedBy_ParseRule();

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
					new String[] { LockRules.LOCK }, noStrings) {
				@Override
				protected PromiseDrop<GuardedByNode> makePromiseDrop(GuardedByNode a) {
//					GuardedByPromiseDrop d = new GuardedByPromiseDrop(a);
					return storeDropIfNotNull(a, scrubGuardedBy(getContext(), a));
				}
			};
		}
	}
	
  private static GuardedByPromiseDrop scrubGuardedBy(
      final IAnnotationScrubberContext context, final GuardedByNode a) {
    // No scrubbing?
    final GuardedByPromiseDrop d = new GuardedByPromiseDrop(a);
    
    final ExpressionNode lock = a.getLock();
    final IRNode fieldDecl = a.getPromisedFor();
    final IRNode classDecl = VisitUtil.getEnclosingType(fieldDecl);
    final String fieldId = VariableDeclarator.getId(fieldDecl);
    final String id = MessageFormat.format("Guard$_{0}", fieldId);
    final RegionNameNode region = new RegionNameNode(a.getOffset(), fieldId);

    // TODO: Clean this up
    if (lock instanceof ThisExpressionNode) {
      final ThisExpressionNode field = (ThisExpressionNode) lock.cloneTree();
      
      final LockDeclarationNode regionLockDecl =
        new LockDeclarationNode(a.getOffset(), id, field, region);
      regionLockDecl.setPromisedFor(classDecl);
      regionLockDecl.setSrcType(a.getSrcType());
      AASTStore.addDerived(regionLockDecl, d);
    } else if (lock instanceof FieldRefNode) {
      final FieldRefNode field = (FieldRefNode) lock.cloneTree();
      
      final LockDeclarationNode regionLockDecl =
        new LockDeclarationNode(a.getOffset(), id, field, region);
      regionLockDecl.setPromisedFor(classDecl);
      regionLockDecl.setSrcType(a.getSrcType());
      AASTStore.addDerived(regionLockDecl, d);
    } else if (lock instanceof ClassExpressionNode) {
      final QualifiedClassLockExpressionNode field = 
        new QualifiedClassLockExpressionNode(lock.getOffset(),
            (NamedTypeNode) ((ClassExpressionNode) lock).getType().cloneTree());
            
      final LockDeclarationNode regionLockDecl =
        new LockDeclarationNode(a.getOffset(), id, field, region);
      regionLockDecl.setPromisedFor(classDecl);
      regionLockDecl.setSrcType(a.getSrcType());
      AASTStore.addDerived(regionLockDecl, d);
    } else if (lock instanceof QualifiedThisExpressionNode) {
      final QualifiedThisExpressionNode field = (QualifiedThisExpressionNode) lock.cloneTree();
      
      final LockDeclarationNode regionLockDecl =
        new LockDeclarationNode(a.getOffset(), id, field, region);
      regionLockDecl.setPromisedFor(classDecl);
      regionLockDecl.setSrcType(a.getSrcType());
      AASTStore.addDerived(regionLockDecl, d);
    }
    return d;
  }
}
