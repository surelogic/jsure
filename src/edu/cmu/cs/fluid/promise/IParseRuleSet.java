package edu.cmu.cs.fluid.promise;

import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Encapsulates the storage and handling of IPromiseParseRule
 */
public interface IParseRuleSet {
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("ECLIPSE.fluid.promise");

  IPromiseParseRule addRule(IPromiseParseRule r);
  
  boolean ruleExists(String keyword);
  IPromiseParseRule getRule(String keyword);
  IPromiseParseRule getRule(String keyword, IPromiseParsedCallback cb);
  boolean useRule(IRNode n, String promise, IPromiseParsedCallback cb);

  String getFirstToken(String s);
  
  Iterator<IPromiseParseRule> getRules();
}
