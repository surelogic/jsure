package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Interface for the EclipsePromiseParser to determine what promise to construct
 */
public interface IPromiseParseRule extends IPromiseRule {
  /** 
   * Returns the name of the promise being unparsed.
   * Assumed to be a constant String
   */
  String name();
  
  /**
   * @param n The IRNode to attach the promises to
   * @param contents The promise excluding the keyword and delimiters like '//@'
   * @param cb Called if the contents were understood as a promise
   * @return true if the contents were understood, otherwise false
   */
  boolean parse(IRNode n, String contents, IPromiseParsedCallback cb);
  
  public static final IPromiseParseRule IGNORE = new IPromiseParseRule() {
  	public String name() { 
  		return "IGNORE"; 
  	} 
    public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
    	return false;
    }
  
		/* (non-Javadoc)
		 * @see edu.cmu.cs.fluid.java.bind.IPromiseRule#getOps(java.lang.Class)
		 */
		public Operator[] getOps(Class type) {
			return PromiseConstants.anyOp;
		}
  };
}
