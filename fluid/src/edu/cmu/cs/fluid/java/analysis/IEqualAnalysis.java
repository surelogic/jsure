/*
 * Created on Jul 17, 2003
 *
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;


public interface IEqualAnalysis {
	/** Return true if the evaluation of the first expression
	 * may return the same non-null reference that the second expression
	 * evaluates to in the context of a single execution of the given
	 * block.  Execution paths through the block that do not evaluate
	 * both expressions can be ignored.
	 * @param expr1 an expression node that is a subtree of the block.
	 * @param expr2 as with expr1
	 * @param block a @{link edu.cmu.cs.fluid.java.oeprator.BlockStatement}
	 *        node that includes both expr1 and expr2 as executable
	 *        expressions.
	 */
	public boolean mayEqual(IRNode expr1, IRNode expr2, IRNode block, IRNode construtorContext);
   
	/** Something useful */
	public boolean mustEqual(IRNode expr1, IRNode expr2, IRNode block, IRNode constructorContext);
}
