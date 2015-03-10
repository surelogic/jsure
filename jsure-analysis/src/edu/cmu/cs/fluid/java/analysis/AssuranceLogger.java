package edu.cmu.cs.fluid.java.analysis;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

public class AssuranceLogger {
  protected final String analysisName;

  Map<IRNode, Set<String>> positive;

  Map<IRNode, Set<String>> negative;

  boolean dont_collect;

  public static final Logger LOG = SLLogger.getLogger("AssuranceLogger");

  public AssuranceLogger(final String aName) {
    analysisName = aName;
    positive = new HashMap<IRNode, Set<String>>();
    negative = new HashMap<IRNode, Set<String>>();
    dont_collect = true;
  }

  public void stopCollection() {
    dont_collect = true;
  }

  public void startCollection() {
    dont_collect = false;
  }

  public void reportPositiveAssurance(String message, IRNode locale) {
    if (dont_collect) {
      return;
    }
    Set<String> s = positive.get(locale);
    if (s == null) {
      s = new HashSet<String>();
    }
    s.add(message);
    positive.put(locale, s);
  }

  public Set<String> getPositiveAssurancesFor(IRNode locale) {
    Set<String> s = positive.get(locale);
    if (s != null) {
      return s;
    }
    return Collections.emptySet();
  }

  public Set<Pair<IRNode, String>> getPositiveAssurances() {
    Set<Pair<IRNode, String>> ret = new HashSet<Pair<IRNode, String>>();
    Iterator<IRNode> i = positive.keySet().iterator();
    while (i.hasNext()) {
      IRNode locale = i.next();
      Iterator<String> j = getPositiveAssurancesFor(locale).iterator();
      while (j.hasNext()) {
        ret.add(new Pair<IRNode, String>(locale, j.next()));
      }
    }
    return ret;
  }

  public Map<IRNode,Set<String>> getPositiveAssuranceMap() {
    if (!dont_collect) {
      LOG.warning("Map leaked while still collecting results");
    }
    return positive; // called only when done, we hope
  }

  public void reportNegativeAssurance(String message, IRNode locale) {
    if (dont_collect) {
      return;
    }
    Set<String> s = negative.get(locale);
    if (s == null) {
      s = new HashSet<String>();
    }
    s.add(message);
    negative.put(locale, s);
  }

  public Set<String> getNegativeAssurancesFor(IRNode locale) {
    Set<String> s = negative.get(locale);
    if (s != null) {
      return s;
    }
    return Collections.emptySet();
  }

  public Set<Pair<IRNode, String>> getNegativeAssurances() {
    Set<Pair<IRNode, String>> ret = new HashSet<Pair<IRNode, String>>();
    Iterator<IRNode> i = negative.keySet().iterator();
    while (i.hasNext()) {
      IRNode locale = i.next();
      Iterator<String> j = getNegativeAssurancesFor(locale).iterator();
      while (j.hasNext()) {
        ret.add(new Pair<IRNode, String>(locale, j.next()));
      }
    }
    return ret;
  }

  public Map<IRNode,Set<String>> getNegativeAssuranceMap() {
    if (!dont_collect) {
      LOG.warning("Map leaked while still collecting results");
    }
    return negative;
  }

  public void clearAssurances() {
    positive = new HashMap<IRNode, Set<String>>();
    negative = new HashMap<IRNode, Set<String>>();
  }

  public String analysisName() {
    return analysisName;
  }
}
