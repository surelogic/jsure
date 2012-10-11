/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/AnnotationParsingContextProxy.java,v 1.2 2007/10/16 16:41:37 chance Exp $*/
package com.surelogic.annotation;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public class AnnotationParsingContextProxy extends AbstractAnnotationParsingContext {
  protected final AbstractAnnotationParsingContext context;
  
  public AnnotationParsingContextProxy(AbstractAnnotationParsingContext context) {
    super(context.getSourceType());
    this.context = context;
  }

  @Override
  public boolean createdAAST() {
    return context.createdAAST();
  }
  
  @Override
  public boolean hadProblem() {
    return context.hadProblem();
  }
  
  @Override
  protected IRNode getAnnoNode() {
    return context.getAnnoNode();
  }

  public Operator getOp() {
    return context.getOp();
  }

  public <T extends IAASTRootNode> void reportAAST(int offset,
      AnnotationLocation loc, Object o, T ast) {
    context.reportAAST(offset, loc, o, ast);
  }

  public void reportError(int offset, String msg) {
    context.reportError(offset, msg);
  }

  public void reportException(int offset, Exception e) {
    context.reportException(offset, e);
  }

}
