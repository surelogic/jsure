package edu.cmu.cs.fluid.promise.parse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 */
public class BooleanTagRule extends AbstractParseRule {
  private final SlotInfo<Boolean> si;

  public BooleanTagRule(String name, Operator[] ops, SlotInfo<Boolean> si) {
    super(name, ops);
    this.si = si;
  }
	public BooleanTagRule(String name, Operator[] ops) {
		this(name, ops, null);
	}

  public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
    return parseBoolean(n, contents, cb);
  }
  
  /**
   * @return The Boolean SlotInfo that should be set by this rule.
   */
  @Override
  protected SlotInfo<Boolean> getSI() {
  	return si;
  }
}
