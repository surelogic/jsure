/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/IAnnotationParsingContext.java,v 1.9 2007/08/22 20:07:51 chance Exp $ */
package com.surelogic.annotation;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * The context used when parsing AAST.
 * 
 * It mediates how IAnnotationParseRules interact with their environment.
 * For example, they report all AASTs and/or errors via callbacks on the 
 * context.  It is maintained by the promise framework
 * 
 * @author Edwin.Chan
 */
public interface IAnnotationParsingContext {
  static final int UNKNOWN = -1;

  /**
   * @param offset The index to be mapped
   * @return the offset in the original source that corresponds 
   *         to the given offset
   */
  int mapToSource(int offset);
  
  String getSelectedText(int start, int stop);
  
  String getAllText();
  
  int getModifiers();
  
  String getProperty(String key);
  
  /**   
   * @return The kind of source the text is originally from
   */
  AnnotationSource getSourceType();
  
  /**
   * @return The type of node that we were called on
   */
  Operator getOp();
  
  /**
   * Report that an AAST was created from parsing the text
   * 
   * @param offset The offset into the text being parsed (unmapped)
   */
  <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, Object o, T ast);
  
  <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, T ast);
  
  <T extends IAASTRootNode> void reportAAST(int offset, T ast);

  /**
   * Report an error in the text
   * 
   * @param offset The offset into the text being parsed (unmapped)
   */
  void reportError(int offset, String msg);

  /**
   * Report an exception thrown while parsing the text
   * 
   * @param offset The offset into the text being parsed (unmapped)
   */
  void reportException(int offset, Exception e);
  
  IAnnotationParsingContext nullPrototype = 
    new AbstractAnnotationParsingContext(AnnotationSource.JAVADOC) {
    public void reportError(int offset, String msg) {
      System.out.println(msg);
    }
    public <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, Object o, T ast) {
    	// Nothing to do
    }
    public void reportException(int offset, Exception e) {
      e.printStackTrace();
    }
    public Operator getOp() {
      return null;
    }
    @Override
    protected IRNode getNode() {
      return null;
    }
	@Override
	public String getAllText() {
		return null;
	}
  };
}
