package com.surelogic.dropsea.irfree;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.*;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.ResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.DeclFactory;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class DiffHeuristics {
	private static class Computation {
		final String attr;
		final Drop drop;

		Computation(String a, Drop d) {
			attr = a;
			drop = d;
		}

		boolean warnIfTrue(boolean condition, String label, Object src) {
			if (condition) {
				SLLogger.getLogger().warning("No "+attr+" for '"+drop.getMessage()+
						"' due to bad "+label+": "+src);
				return true;
			}
			return false;
		}
		
		boolean isNeg(String valueLabel, int value, Object src) {
			return warnIfTrue(value < 0, valueLabel, src);
		}
		
		<T> boolean isNull(String valueLabel, T value, Object src) {
			return warnIfTrue(value == null, valueLabel, src);
		}
		
		void add(int value) {
			if (value < 0) {
				SLLogger.getLogger().warning("Negative "+attr+" ("+value+") for "+drop.getMessage());
			} 
			drop.addOrReplaceDiffInfo(DiffInfoUtility.getIntInstance(attr, value));
		}
	}

	public static void computeDiffInfo(final Drop drop, final Pair<IJavaRef, IRNode> loc) {
		if (loc == null || loc.first() == null || loc.second() == null) {
			if (!(drop instanceof ResultFolderDrop)) {
				SLLogger.getLogger().warning("Diff info not computed due to missing location: "+drop.getMessage());
			}
			return;
		}
		if (!loc.first().isFromSource()) {
			return; // Not enough info to do anything here
		}
		final IRNode enclosingDecl;	
		if (drop instanceof IPromiseDrop) {
			enclosingDecl = loc.second();
		} else {
			enclosingDecl = DeclFactory.findEnclosingDecl(loc.second());			
		}
		final IJavaRef enclosingRef = JavaNode.getJavaRef(enclosingDecl);    
		if (enclosingDecl == null || enclosingRef == null) {
			SLLogger.getLogger().warning("Diff info not computed due to no enclosing decl: "+drop.getMessage());
			return;
		}
		computeDeclRelativeOffset(new Computation(IDiffInfo.DECL_RELATIVE_OFFSET, drop), 
									loc, enclosingDecl, enclosingRef);
		computeDeclEndRelativeOffset(new Computation(IDiffInfo.DECL_END_RELATIVE_OFFSET, drop), 
				                    loc.first(), enclosingRef);
	}

	/**
	 * Offset from the first statement/expression/decl in the enclosing declaration
	 * (unless it's a parameter, in which case it's from the start of the enclosing
	 * method)
	 */
	private static void computeDeclRelativeOffset(final Computation c, final Pair<IJavaRef,IRNode> loc, 
			final IRNode enclosingDecl, final IJavaRef enclosingRef) {
		final IJavaRef here = loc.first();
		final IRNode start = loc.second() instanceof IPromiseDrop ? enclosingDecl : 
			                 computeFirstInterestingNodeInDecl(enclosingDecl);
		final IJavaRef startRef = (start == enclosingDecl) ? enclosingRef : JavaNode.getJavaRef(start);
		if (c.isNull("start", start, enclosingRef) || 
			c.isNull("start ref", startRef, enclosingRef) ||
			c.isNeg("start offset", startRef.getOffset(), startRef) ||
			c.isNeg("offset", here.getOffset(), here)) {
			return;
		}
		int offset = here.getOffset() - startRef.getOffset();	
		if (offset < 0) {
			// We're actually before the start, which might be ok for certain cases
			final Operator op =  JJNode.tree.getOperator(loc.second());
			if (Annotation.prototype.includes(op) || 
				ParameterDeclaration.prototype.includes(op) || 
				TypeFormal.prototype.includes(op)) {
				if (c.isNeg("enclosing offset", enclosingRef.getOffset(), enclosingRef)) {
					return;
				}
				offset = here.getOffset() - enclosingRef.getOffset();
			}
		}
		c.add(offset);
	}  

	private static IRNode computeFirstInterestingNodeInDecl(IRNode decl) {
		final Operator op = JJNode.tree.getOperator(decl);
		if (AnonClassExpression.prototype.includes(op)) {
			return getFirstChild(decl, AnonClassExpression.getBody(decl));
		}
		Declaration d = (Declaration) op;
		switch (d.getKind()) {
		case ANNOTATION:
			return getFirstChild(decl, AnnotationDeclaration.getBody(decl));
		case CLASS:
			return getFirstChild(decl, ClassDeclaration.getBody(decl));
		case CONSTRUCTOR:
			return getFirstChild(decl, ConstructorDeclaration.getBody(decl));
		case ENUM:
			return getFirstChild(decl, EnumDeclaration.getBody(decl));
		case FIELD:
			if (EnumConstantClassDeclaration.prototype.includes(op)) {
				return decl;
			}
			return getFirstChild(decl, VariableDeclarator.getInit(decl));
		case INITIALIZER:
			return getFirstChild(decl, ClassInitializer.getBlock(decl));
		case INTERFACE:
			return getFirstChild(decl, InterfaceDeclaration.getBody(decl));
		case METHOD:
			if (AnnotationElement.prototype.includes(d)) {
				return decl;
			}
			return getFirstChild(decl, MethodDeclaration.getBody(decl));
		case PACKAGE:
		case PARAMETER:
		case TYPE_PARAMETER:
		default:
		}
		return decl;
	}
	
	private static IRNode getFirstChild(final IRNode gparent, final IRNode parent) {
		if (!JJNode.tree.hasChildren(parent)) {
			return gparent;
		}
		return JJNode.tree.getChild(parent, 0);
	}
	
	private static void computeDeclEndRelativeOffset(final Computation c, final IJavaRef here, final IJavaRef enclosing) {
		if (c.isNeg("enclosing offset", enclosing.getOffset(), enclosing) ||
			c.isNeg("enclosing length", enclosing.getLength(), enclosing) ||
			c.isNeg("offset", here.getOffset(), here)) {
			return;
		}
		final int offset = enclosing.getOffset() + enclosing.getLength() - here.getOffset();	
		c.add(offset);
	}
}
