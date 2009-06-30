package edu.cmu.cs.fluid.promise;

import java.util.*;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.FilterIterator;

public class ParseRuleMap extends HashMap<String,IPromiseParseRule> implements IParseRuleSet {
  private static final class RenamedRule implements IPromiseParseRule {
    private final String            name;

    private final IPromiseParseRule r;

    private RenamedRule(String name, IPromiseParseRule r) {
      this.name = name;
      this.r = r;
    }

    public String name() {
      return name;
    }

    public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
      if (AbstractPromiseAnnotation.inStrictMode) {           
        cb.noteProblem("@"+name+" is deprecated; use @"+r.name()+" instead");
      }
      return r.parse(n, contents, cb);
    }

    public Operator[] getOps(Class type) {
      return r.getOps(type);
    }
  }

  private static final String[] prefixesToIgnore = {"ejb.", "jmx."};
  
  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * @see edu.cmu.cs.fluid.eclipse.promise.IParseRuleSet#addRule(IPromiseParseRule)
   */
  public IPromiseParseRule addRule(final IPromiseParseRule r) {
    final String tag = r.name();
    if (tag == null || tag.equals("")) {
      LOG.severe("Ignoring invalid rule: "+r);
      return null;
    }
    final char first = tag.charAt(0);
    final String realTag, altTag;
    if (Character.isUpperCase(first)) {
      realTag = tag;
      altTag  = Character.toLowerCase(first) + tag.substring(1);
    } else if (Character.isLowerCase(first)) {      
      realTag = Character.toUpperCase(first) + tag.substring(1);
      altTag  = tag;      
    } else {
      realTag = tag;
      altTag  = null;
    }
    IPromiseParseRule o = put(realTag, r);
    if (o != null) {
      LOG.warning("Replaced parse rule for @" + o.name() + " with " + r);
      return o;
    } else {
      LOG.fine("Added rule for @" + r.name());      
      
      if (altTag != null) {
        put(altTag, new RenamedRule(altTag, r));
        
        /**
         * Adding a tentative ("?") version of every annotation
         */
        final String newName = altTag+"?";      
        put(newName, new RenamedRule(newName, r));
      }
    }
    return null;
  }
  
  public boolean ruleExists(String keyword) {
    return containsKey(keyword);
  }
  
  public IPromiseParseRule getRule(String keyword) {
    return getRule(keyword, null);  
  }
  
  /**
   * @see edu.cmu.cs.fluid.eclipse.promise.IParseRuleSet#getRule(String)
   */
  public IPromiseParseRule getRule(String keyword, IPromiseParsedCallback cb) {
    IPromiseParseRule o = get(keyword);
    if (o == null) {
      for (String ignoredPrefix : prefixesToIgnore) {
        if (keyword.startsWith(ignoredPrefix)) {
          return IPromiseParseRule.IGNORE;
        }
      }
      if (keyword.startsWith("-")) {
        if (cb != null) {
          cb.noteWarning("Deactivated promise: @"+keyword);
        }
      } else {
        LOG.warning("getRule() ignored -- No rule for @" + keyword);
        
        if (cb != null) {
          cb.noteWarning("No rule for @"+keyword);
        }
      }
      return IPromiseParseRule.IGNORE;
    }
    return o;
  }

  /**
   * @see edu.cmu.cs.fluid.eclipse.promise.IParseRuleSet
   */
  public boolean useRule(IRNode n, String promise, IPromiseParsedCallback cb) {
    final String keyword = getFirstToken(promise);
    final String rest = getRest(promise, keyword);
    return getRule(keyword, cb).parse(n, rest, cb);
  }

  public String getFirstToken(String s) {
    final StringTokenizer st = new StringTokenizer(s);
    if (!st.hasMoreTokens()) {
      return null;
    }
    return st.nextToken();
  }

  private static String getRest(String s, String first) {
    return s.substring(s.indexOf(first) + first.length());
  }
  
  public Iterator<IPromiseParseRule> getRules() {
    return new FilterIterator<Map.Entry<String,IPromiseParseRule>,IPromiseParseRule>(entrySet().iterator()) {
      @Override
      protected Object select(Map.Entry<String,IPromiseParseRule> e) {
        return e.getValue();
      }
    };
  }
}
