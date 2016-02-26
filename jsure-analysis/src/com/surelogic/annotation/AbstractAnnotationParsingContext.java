package com.surelogic.annotation;

import java.lang.annotation.Annotation;
import java.util.*;

import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.annotation.test.*;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Contains code common to parsing contexts
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAnnotationParsingContext implements ITestAnnotationParsingContext {
  private final IBinder binder;
  private final AnnotationSource srcType;
  private final AnnotationOrigin origin;
  protected boolean hadProblem = false;
  protected boolean createdAAST = false;

  protected AbstractAnnotationParsingContext(IBinder b, AnnotationSource src, AnnotationOrigin o) {
	binder = b;
    srcType = src;
    origin = o;
  }

  protected final IBinder getBinder() {
	return binder;
  }
  
  @Override
  public int getModifiers() {
    return JavaNode.ALL_FALSE;
  }

  @Override
  public String getProperty(String key) {
    return null;
  }

  @Override
  public void setProperty(String key, String value) {
    // ignore
  }

  @Override
  public final AnnotationSource getSourceType() {
    return srcType;
  }

  public final AnnotationOrigin getOrigin() {
    return origin;
  }

  public boolean createdAAST() {
    return createdAAST;
  }

  public boolean hadProblem() {
    return hadProblem;
  }

  /**
   * e.g. the annotation being checked for test results
   */
  protected abstract IRNode getAnnoNode();

  public TestResult getTestResult() {
    throw new UnsupportedOperationException();
  }

  public void setTestResultForUpcomingPromise(TestResult r) {
    throw new UnsupportedOperationException();
  }

  public void clearTestResult() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTestResultForUpcomingPromise(TestResultType r, String explan) {
    setTestResultForUpcomingPromise(TestResult.newResult(getAnnoNode(), r, explan));
  }

  @Override
  public void setTestResultForUpcomingPromise(TestResultType r, String context, String explan) {
    setTestResultForUpcomingPromise(TestResult.newResult(getAnnoNode(), r, context, explan));
  }

  /**
   * Returns a default mapping from the offsets seen by the parse rule and the
   * actual source offsets
   */
  @Override
  public int mapToSource(int offset) {
    return offset;
  }

  @Override
  public String getSelectedText(int start, int stop) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getAllText() {
    throw new UnsupportedOperationException();
  }

  /**
   * Convenience method for annotations that will appear on the default
   * declaration
   */
  @Override
  public final <T extends IAASTRootNode> void reportAAST(int offset, T ast) {
    reportAAST(offset, AnnotationLocation.DECL, null, ast);
  }

  @Override
  public final void reportError(int offset, String msg) {
    reportErrorAndProposal(offset, msg, null);
  }

  @Override
  public void reportError(int offset, int number, Object... args) {
    reportErrorAndProposal(offset, I18N.res(number, args), null);
  }

  @Override
  public ProposedPromiseDrop.Builder startProposal(Class<? extends Annotation> anno) {
    return null;
  }

  /**
   * Convenience method for AnnotationLocations that don't provide any context
   */
  @Override
  public final <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, T ast) {
	final IRNode anno = getAnnoNode();
	final IRNode n = computeDeclNode(anno);
	if (JavaNode.getModifier(n, JavaNode.IMPLICIT) && MethodDeclaration.prototype.includes(n)) {
		final IRNode parent = JJNode.tree.getParent(n);
		final IRNode gparent = JJNode.tree.getParent(parent);
		if (AnonClassExpression.prototype.includes(gparent)) {
			ast = remapParametersWithin(gparent, n, ast);
		}
	}
    reportAAST(offset, loc, null, ast);
  }
  
  private <T extends IAASTRootNode> T remapParametersWithin(IRNode ace, IRNode method, T ast) {
	  if (binder == null) {
		  return ast;
	  }
	  // TODO cache this so that it can be reused for multiple annotations?
	  final IJavaType type = binder.getJavaType(AnonClassExpression.getType(ace));
	  final IJavaFunctionType fty = binder.getTypeEnvironment().isFunctionalType(type);
	  final Map<String,String> paramMap = new HashMap<>();
	  IRNode mParams = MethodDeclaration.getParams(fty.getDecl());
	  IRNode params = MethodDeclaration.getParams(method);
	  Iterator<IRNode> it = Parameters.getFormalIterator(params);
	  for(IRNode mp : Parameters.getFormalIterator(mParams)) {
		  IRNode p = it.next();
		  String origName = JJNode.getInfo(mp);
		  String newName = JJNode.getInfo(p);
		  if (!origName.equals(newName)) {
			  paramMap.put(origName, newName);
		  }
	  }
	  if (!paramMap.isEmpty()) {
		  // TODO what if it doesn't change it?		 
		  VarUseModifier mod = new VarUseModifier(paramMap);
		  return (T) ast.modifyTree(mod);		  
	  }
	  return ast;
  }
  
  static class VarUseModifier implements INodeModifier {
	private final Map<String, String> paramMap;

	public VarUseModifier(Map<String, String> map) {
		paramMap = map;
	}

	@Override
	public Status createNewAAST(IAASTNode n) {
		if (n instanceof VariableUseExpressionNode) {
			VariableUseExpressionNode v = (VariableUseExpressionNode) n;
			if (paramMap.containsKey(v.getId())) {
				return Status.MODIFY;
			}
		}
		return Status.KEEP;
	}

	@Override
	public IAASTNode modify(IAASTNode orig) {
		VariableUseExpressionNode v = (VariableUseExpressionNode) orig;
		String newId = paramMap.get(v.getId());
		return new VariableUseExpressionNode(orig.getOffset(), newId);
	}
  }
  
  /**
   * Finds the nearest enclosing declaration from the given node
   */
  protected final IRNode computeDeclNode(IRNode node) {
    final IRNode start = node;
    while (node != null) {
      Operator op = JJNode.tree.getOperator(node);
      if (Declaration.prototype.includes(op)) {
        return node;
      } else if (VariableDeclList.prototype.includes(op)) {
        return node;
      } else if (MethodCall.prototype.includes(op)) {
        return node; // Special case for Cast
      } else if (BlockStatement.prototype.includes(op)) {
        return node;
      }
      node = JJNode.tree.getParentOrNull(node);
    }
    return start;
  }

  /**
   * Finds the appropriate declaration to annotate, based on the parameters
   * 
   * @param decl
   *          The base location to start from
   * @param loc
   *          The relative location
   * @param context
   *          Context info used to find the appropriate declaration
   */
  private IRNode translateDecl(IRNode decl, AnnotationLocation loc, Object context) {
    switch (loc) {
    case DECL:
      return decl;
    case RECEIVER:
      return JavaPromise.getReceiverNodeOrNull(decl);
    case RETURN_VAL:
      return JavaPromise.getReturnNodeOrNull(decl);
    case PARAMETER:
      String id = (String) context;
      return BindUtil.findLV(decl, id);
    case QUALIFIED_RECEIVER:
      IRNode type = (IRNode) context;
      return JavaPromise.getQualifiedReceiverNodeByName(decl, type);
    }
    return null;
  }

  /**
   * Helper method to compute the declarations to annotate from the IRNode where
   * the text of annotation appears
   */
  protected final Iterable<IRNode> computeDeclNode(IRNode node, AnnotationLocation loc, Object o) {
    IRNode decl = computeDeclNode(node);
    if (VariableDeclList.prototype.includes(decl)) {
      IRNode vds = VariableDeclList.getVars(decl);
      return VariableDeclarators.getVarIterator(vds);
    } else {
      return new SingletonIterator<>(translateDecl(decl, loc, o));
    }
  }
}
