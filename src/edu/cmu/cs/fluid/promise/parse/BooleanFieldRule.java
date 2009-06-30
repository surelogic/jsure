/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/promise/parse/BooleanFieldRule.java,v 1.3 2007/07/05 18:15:14 aarong Exp $*/
package edu.cmu.cs.fluid.promise.parse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class BooleanFieldRule extends AbstractParseRule {
  protected BooleanFieldRule(String name) {
    super(name, fieldDeclOp);
  }

  public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
    final Operator op = tree.getOperator(n);
    final String msg  = checkPromisedOn(n, op);
    if (msg != null) {
      cb.noteProblem(msg);
      return false;
    }
    IRNode decls  = FieldDeclaration.getVars(n);
    final int num = tree.numChildren(decls);

    if (num == 0) {
      cb.noteProblem("No fields declared for "+DebugUnparser.toString(n));
      return false;
    }        
    boolean first = true;
    for (IRNode vd : VariableDeclarators.getVarIterator(decls)) {
      boolean rv = parseBoolean(vd, first ? contents : null, cb);
      if (!rv) {
        return false;
      }
    }
    return true;
  }
  
  @Override
  protected abstract SlotInfo<Boolean> getSI();
  
  @Override
  protected abstract void parsedSuccessfully(IRNode decl);
}
