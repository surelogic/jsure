package com.surelogic.opgen.generator;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import com.surelogic.opgen.syntax.*;


public abstract class AbstractGenerator {
  private static final String FORCE_OUT = "-forceOut";
  /*********************************************************************
   *  Constants
   *********************************************************************/

  protected static final Pattern keywordMatch = Pattern.compile("^\\\"[a-zA-Z]+\\\"");
  protected static final Pattern variableMatch = Pattern.compile("^[+*].*");
  protected static final Pattern tagMatch = Pattern.compile("<.*>");
  
  protected static final Pattern plusOrStarMatch = Pattern.compile("^[+*]$");
  protected static final Pattern plusOrStarDotMatch = Pattern.compile("^[+*]/");
  protected static final Pattern precMatch = Pattern.compile("^\\((.*)\\)$");   
  
  protected static final Pattern javadocMatch = Pattern.compile(
      "/\\*\\*([^\\*])*\\*(\\*|([^\\*/]([^\\*])*\\*))*/");
  
  /*********************************************************************
   *  Generator state
   *********************************************************************/
  
  protected boolean debug = false;
  protected final Map<String,String> typeTable = new HashMap<String, String>();
  protected final Map<String,Set<String>> typeArgsTable = new HashMap<String, Set<String>>();
  {
    typeTable.put("DimInfo", "int");
    typeTable.put("Modifiers", "int");
    typeTable.put("Info", "String");
    typeTable.put("Op", "edu.cmu.cs.fluid.java.JavaOperator");
    typeTable.put("Comment", "String");
    typeTable.put("Code", "Object");
    typeTable.put("IsWrite", "boolean");
    typeTable.put("ConstantNode", "edu.cmu.cs.fluid.ir.IRNode");
    typeTable.put("ConstantInt", "int");
    typeArgsTable.put("Modifiers", 
                      createArgsSet("public", "protected", "private",
                                    "static", "abstract", "final", 
                                    "synchronized", "native", "strictfp"));
  }
  
  /** Maps each attribute name to the Fluid class name that contains its access methods. */
  protected final Map<String, String> accessRcvrTable = new HashMap<String, String>();
  {
    accessRcvrTable.put("DimInfo", "JavaNode");
    accessRcvrTable.put("Modifiers", "JavaNode");
    accessRcvrTable.put("Info", "JJNode");
    accessRcvrTable.put("Op", "JavaNode");
    accessRcvrTable.put("Comment", "JavaNode");
    accessRcvrTable.put("Code", "JavaNode");
    accessRcvrTable.put("IsWrite", "JavaPromise");
    accessRcvrTable.put("ConstantNode", "JavaNode");
    accessRcvrTable.put("ConstantInt", "JavaNode");
  }
  
  /** Maps each attribute name to the Fluid class name that contains its unparse methods. */
  protected final Map<String, String> unparseRcvrTable = new HashMap<String, String>();
  {
    unparseRcvrTable.put("Code", "JavaNode");
    unparseRcvrTable.put("Comment", "JavaNode");
    unparseRcvrTable.put("ConstantInt", "JavaNode");
    unparseRcvrTable.put("ConstantNode", "JavaNode");
    unparseRcvrTable.put("DimInfo", "JavaNode");
    unparseRcvrTable.put("Info", "JavaNode");
    unparseRcvrTable.put("Modifiers", "JavaNode");
    unparseRcvrTable.put("Op", "JavaNode");
    unparseRcvrTable.put("IsWrite", "JavaPromise");
    unparseRcvrTable.put("Promises", "JavaPromise");
  }
  
  
  protected String getType(String name) {
    return typeTable.get(name);
  }
  
  private static Set<String> createArgsSet(String... args) {
    if (args.length == 0) {
      return Collections.emptySet();
    }
    Set<String> s = new HashSet<String>();
    for (String a : args) {
      s.add(a);
    }
    return s;
  }
  
  /*********************************************************************
   *  Table of OpSyntax to its possible parents
   *  -- used to compute the least common super-operator for getParent
   *********************************************************************/
  
  private final Map<String,Set<OpSyntax>> actualParents = new HashMap<String,Set<OpSyntax>>(); 
  private final Map<String,Set<OpSyntax>> parents = new HashMap<String,Set<OpSyntax>>(); 
  private final Map<String,Set<OpSyntax>> leastCommonSuper = new HashMap<String,Set<OpSyntax>>(); 
  
  /**
   * Make a copy of the original, but simplified parents
   */
  private void copyRealParents() {
    actualParents.clear();
    for (Map.Entry<String,Set<OpSyntax>> e : parents.entrySet()) {
      System.err.println(unparseOps("Original parents for "+e.getKey()+": ", e.getValue()));
      Set<OpSyntax> simplified = simplifyParents(e.getValue());
      actualParents.put(e.getKey(), simplified);
    }
  }
  
  protected final Set<OpSyntax> lookupActualParents(String name) {
    return actualParents.get(name);
  }
  
  protected final Set<OpSyntax> lookupParents(String name) {
    return parents.get(name);
  }
  
  /**
   * @param name
   * @return null if never appears in syntax, empty set if no LCS
   */
  protected final Set<OpSyntax> lookupLeastCommonSuper(String name) {
    Set<OpSyntax> rv = leastCommonSuper.get(name);
    return rv;
  }
  
  /**
   * Get the parents of the indicated operator,
   * or create and return an empty set for that operator
   */
  private Set<OpSyntax> ensureParentsEntry(String name) {
    Set<OpSyntax> parents = this.parents.get(name);
    if (parents == null) {
      parents = new HashSet<OpSyntax>();
      this.parents.put(name, parents);
    }
    return parents;
  }
  
  /**
   * Done before we have global information, so just record
   * partial parent/child info
   */
  private void addParents(OpSyntax s) {
    for (Child c : s.children) {
      // add s as a parent
      Set<OpSyntax> parents = ensureParentsEntry(c.type);
      parents.add(s);
    }
  }
  
  /**
   * @return true if there is no super-operators/interfaces
   */
  private boolean isRoot(OpSyntax s) {
    if (s.isRoot) {
      // Might have some interfaces
      for (String iface : s.superifaces) {
        if (lookupIface(iface) != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return true if it has no sub-operators
   */
  private boolean isLeaf(OpSyntax s) {
    initIsLeaf();
    return !notLeaves.contains(s);
  }
  /**
   * Sets up the cache 'notLeaves'
   */
  private void initIsLeaf() {
    if (!notLeaves.isEmpty()) {
      return;
    }
    for (final Map.Entry<String,OpSyntax> e : iterate()) {
      final OpSyntax s = e.getValue();
      for (final OpSyntax p : getSuperOps(s)) {
        notLeaves.add(p);
      }
    }
  }
  private Set<OpSyntax> notLeaves = new HashSet<OpSyntax>();
  
  protected Collection<OpSyntax> getSuperOps(OpSyntax s) { 
    List<OpSyntax> superOps;
    if (s.isRoot) {
      if (s.superifaces.isEmpty()) {
        return Collections.emptyList();        
      }
      superOps = new ArrayList<OpSyntax>();
    } else {
      superOps = new ArrayList<OpSyntax>();
      OpSyntax parent = lookup(s.parentOperator);
      if (parent != null) {
        superOps.add(parent);
      } else {
        System.err.println("Error:   Couldn't find parent "+s.parentOperator);
      }
    }
    
    for (String iface : s.superifaces) {
      OpSyntax i = lookupIface(iface);      
      if (i != null && i != s) {
        superOps.add(i);      
      }
    }
    //System.err.println(unparseOps("superOps for "+s.name+":", superOps));
    return superOps;
  }
  
  private String unparseOps(String msg, Collection<OpSyntax> ops) {
    StringBuilder sb = new StringBuilder(msg);
    boolean first = true;
    for(OpSyntax o : ops) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(o.name);
    }
    return sb.toString();
  }
  
  /*********************************************************************
   * Code to substitute grandparents for parents that are "fixed children"
   *********************************************************************/
  
  protected enum ParentSubstitutionType {
    ALL, SOME, NONE, UNDEFINED;
    
    ParentSubstitutionType madeSubstitution() {
      switch (this) {
      case UNDEFINED:
      case ALL:
        return ALL;
      case NONE:
      case SOME:
      default:
        return SOME;

      }
    }
    ParentSubstitutionType madeNoSubstitution() {
      switch (this) {
      case UNDEFINED:
      case NONE:
        return NONE;
      case ALL:
      case SOME:
      default:
        return SOME;

      }
    }
    ParentSubstitutionType mergeSubstType(ParentSubstitutionType t2) {
      if (t2.equals(UNDEFINED)) {
        throw new IllegalArgumentException("t2 is undefined");
      }
      switch (this) {
      case UNDEFINED:
        return t2;        
      case NONE:
        return t2.madeNoSubstitution();
      case ALL:
        return t2.madeSubstitution();
      case SOME:
      default:
        return SOME;
      }
    }
  }
  
  private static class LogicalParents {
    static final LogicalParents empty = 
      new LogicalParents(ParentSubstitutionType.NONE, Collections.<OpSyntax>emptySet());
    
    final ParentSubstitutionType type;
    final Set<OpSyntax> ops;
    
    LogicalParents(ParentSubstitutionType t, Set<OpSyntax> p) {
      type = t;
      ops = p;
    }
  }
  
  protected static final boolean substGrandparentForFixedChildren = true;
  private final Map<String,LogicalParents> logicalParents = new HashMap<String,LogicalParents>(); 
  
  private LogicalParents lookupLogicalParents(String name) {
    return logicalParents.get(name);
  }
  
  protected ParentSubstitutionType getLogicalParentStatus(String name) {
    LogicalParents lp = lookupLogicalParents(name);    
    if (lp != null && lp.type != ParentSubstitutionType.UNDEFINED) {
      return lp.type;
    }
    return ParentSubstitutionType.NONE;
  }
  
  private void ensureLogicalParents(String name, LogicalParents subst) {
    if (subst != null) {
      logicalParents.put(name, subst);
    }
  }
  /**
   * 
   * @return true if variable with no other children
   */
  protected final boolean isLogicallyInvisible(OpSyntax s) {
    String invisible = s.props.get(KnownProperty.LOGICALLY_INVISIBLE);
    if (invisible != null && !"false".equals(invisible)) {
      return true;
    }
    return false;
  }
  
  /**
   * 
   * @return true if variable with no other children
   */
  protected final boolean shouldBeLogicallyInvisible(OpSyntax s) {
    if (s.name.endsWith("s")) {
      if (s.isVariable() && s.numChildren == 0) {
        return true;
      }
      else if (s.name.startsWith(OpSyntax.OPT_CHILD_PREFIX)) {
        OpSyntax s2 = lookup(s.name.substring(OpSyntax.OPT_CHILD_PREFIX.length()));
        return shouldBeLogicallyInvisible(s2);
      }
    }
    return false;
  }
  
  /**
   * Compute the logical parents of the node
   * (eliding "fixed children" nodes like Arguments)
   * 
   * @param name The name of the node
   */
  private LogicalParents substituteParents(String name) {
    LogicalParents subst = lookupLogicalParents(name);    
    if (subst == null) {
      Set<OpSyntax> parents = lookupParents(name);    
      subst = substituteParents(name, parents);
      //System.out.println(unparseOps("Subst for "+name+":\t", subst));
      ensureLogicalParents(name, subst);
    }
    return subst;
  }
  
  private LogicalParents substituteParents(String name, Set<OpSyntax> parents) {
    if (parents == null || parents.isEmpty()) {
      return LogicalParents.empty;
    }
    if (substGrandparentForFixedChildren) {
      ParentSubstitutionType type = ParentSubstitutionType.UNDEFINED;
      Set<OpSyntax> subst         = new HashSet<OpSyntax>();
      for (OpSyntax p : parents) {
        if (isLogicallyInvisible(p)) {
          //System.out.println("Getting parents for "+p.name+" (gPs for "+name+")");
          LogicalParents gPs = substituteParents(p.name);
          //System.out.println(unparseOps("Grandparents:                   ", gPs));
          if (gPs.ops.isEmpty()) {
            type = type.madeNoSubstitution();
            subst.add(p); // fallback if no grandparents in AST
          } else {
            type = type.madeSubstitution().mergeSubstType(gPs.type);
            subst.addAll(gPs.ops);
          }
        } else {
          type = type.madeNoSubstitution();
          subst.add(p);
        }
      }
      /*
      Set<OpSyntax> diff = new HashSet<OpSyntax>(subst);
      diff.removeAll(parents);
      if (!diff.isEmpty()) {
        System.out.println(unparseOps("Parents:                        ", parents));
        System.out.println(unparseOps("Diff between parents and subst: ", diff));
        return subst;
      }
      */
      if (type == ParentSubstitutionType.NONE || type == ParentSubstitutionType.UNDEFINED) {
        return new LogicalParents(type, subst);
      }
//      switch (type) {
//      case ALL:
//      case SOME:
//        return new LogicalParents(type, subst);
//      }
    }
    return new LogicalParents(ParentSubstitutionType.NONE, parents);
  }
  
  /*********************************************************************
   * Code to compute the LCS
   *********************************************************************/
  
  protected static final OpSyntax rootOp = OpSyntax.invalid;
  
  /**
   * Try to simplify each set of parents down to 1-2 
   * (preferably not root)
   * 
   * Take a parent and try to find an non-trivial LCS 
   * with the next parent.  If there is, continue 
   * merging subsequent parents with the result.
   * If none, add the result so far to the finished set
   * and start again with a new parent.
   */
  private Set<OpSyntax> simplifyParents(Set<OpSyntax> parents) {
    if (parents == null || parents.isEmpty()) {
      return Collections.emptySet();
    }
    if (parents.size() < 2) { // Singleton
      return parents;
    }
    Set<OpSyntax> simplified = new HashSet<OpSyntax>();
    boolean first = true;
    OpSyntax lcs  = null;
    for (OpSyntax op : parents) {
      if (first) {
        first = false;
        lcs = op; // initialize
      } else {
        Set<OpSyntax> newLCS = computeLeastCommonSuper(lcs, op);
        switch (newLCS.size()) {
        case 1:  // Use the new lcs made by combining op
          lcs = newLCS.iterator().next();
          System.err.println(unparseOps("\tMerged LCS "+lcs.name+" and "+op.name+" => ", newLCS));
          break;
        case 0:  // No commonality, so let's start on a new lcs
        default: // 2 or more, so skip for now
          simplified.add(lcs);
          lcs = op;
        }
      }
    }
    if (!first) {
      simplified.add(lcs);
    }
    if (simplified.size() > 1) {
      simplified = simplifyParents2(simplified);
    }
    //System.err.println(unparseOps("Simplified: ", simplified));
    return simplified;
  }
  
  /**
   * Slower algorithm for combining parents.
   * 
   * Like above, except try merging against 
   * all the parents.
   */
  private Set<OpSyntax> simplifyParents2(Set<OpSyntax> parents) {
    if (parents == null || parents.isEmpty()) {
      return Collections.emptySet();
    }
    if (parents.size() < 2) { // Singleton
      return parents;
    }
    Set<OpSyntax> simplified = new HashSet<OpSyntax>();
    while (!parents.isEmpty()) {
      boolean first = true;
      OpSyntax lcs  = null;
      for (OpSyntax op : new HashSet<OpSyntax>(parents)) {
        if (first) {
          first = false;
          lcs = op; // initialize
          parents.remove(op);
        } else {
          Set<OpSyntax> newLCS = computeLeastCommonSuper(lcs, op);
          switch (newLCS.size()) {
          case 1:  // Use the new lcs made by combining op
            lcs = newLCS.iterator().next();
            System.err.println(unparseOps("\tMerged LCS "+lcs.name+" and "+op.name+" => ", newLCS));
            parents.remove(op);
            break;
          case 0:  // No commonality, so let's start on a new lcs
          default: // 2 or more, so skip for now
          }
        }
      }
      simplified.add(lcs); // assuming there was at least one op
    }
    return simplified;
  }
  
  /**
   * Compute the least common super-operator among the operators in the set
   * 
   * @param ops The set of operators to compute over
   */
  private Set<OpSyntax> computeLeastCommonSuper(Set<OpSyntax> ops) {
    switch (ops.size()) {
    case 0:
      return ops;
    case 1:
      return Collections.singleton(ops.iterator().next());
    default: // 2 or more      
      Iterator<OpSyntax> it = ops.iterator();
      Set<OpSyntax> lcs     = computeLeastCommonSuper(it.next(), it.next());
      while (it.hasNext()) {
        lcs = combineWithLCS(lcs, it.next());
      }
      return eliminateAncestors(lcs);
    }
  }
  
  /**
   * Compute the LCS, given a set of partial results
   * 
   * (A | B) & C ==> (A & C) | (B & C)  
   * 
   * @param lcs The partial results so far
   * @param next The next op to combine
   * @return The updated partial results
   */
  private Set<OpSyntax> combineWithLCS(Set<OpSyntax> lcs, OpSyntax next) {
    if (lcs == null) {
      return null;
    }
    switch (lcs.size()) {
    case 0:
      return lcs;
    case 1:
      return computeLeastCommonSuper(lcs.iterator().next(), next);
    default: // 2 or more      
      boolean first    = true;
      Set<OpSyntax> rv = null;
      for (OpSyntax op : lcs) {
        Set<OpSyntax> temp = computeLeastCommonSuper(op, next);
        if (first) {
          first = false;
          rv    = temp;
        } else {          
          rv = mergeLCS(rv, temp);
        }
      }
      return rv;
    }
  }
  
  /**
   * Return the union of the two original sets
   * (possibly mutating the original sets)
   */
  private Set<OpSyntax> mergeLCS(Set<OpSyntax> s1, Set<OpSyntax> s2) {
    switch (s1.size()) {
    case 0:
      return s2;
    case 2:
      s1.addAll(s2);
      return s1;
    default:   
      switch (s2.size()) {
      case 0:
        return s1;
      case 2:
        s2.addAll(s1);
        return s2;
      default: 
        Set<OpSyntax> temp = new HashSet<OpSyntax>();
        temp.addAll(s1);
        temp.addAll(s2);
        return temp;
      }
    }
  }
  
  /**
   * @return true if p is equal to or the super-operator
   * of s
   */
  private boolean isSuperOpOrEqual(OpSyntax p, OpSyntax s) {
    if (p == null || p == rootOp) {
      return true;
    }
    if (s == null || s == rootOp) {
      return false;
    }
    if (p == s) {
      return true;
    }
    if (isRoot(s)) {
      return false; // No parents to check
    }
    //return isSuperOpOrEqual(p, lookup(s.parentOperator));
    for (OpSyntax superOp : getSuperOps(s)) {
      if (isSuperOpOrEqual(p, superOp)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Internal implementation of computeLeastCommonSuper
   * Only called by computeLeastCommonSuper
   * 
   * Could return more than one, due to multiple inheritance
   * @return A set of LCS for the pair (mutable only if there are 2+ elements)
   */
  private Set<OpSyntax> computeLeastCommonSuper_internal(final OpSyntax s1, final OpSyntax s2) {
    //System.err.println("Computing LCS for "+s1.name+" and "+s2.name);
    if (s1 == null) {
      return Collections.emptySet();
    }
    // Return the more general of the two
    if (isSuperOpOrEqual(s1, s2)) {
      return Collections.singleton(s1);
    }
    if (isRoot(s1)) {
      return Collections.emptySet(); // No parents to check
    }
    boolean first    = true;
    Set<OpSyntax> rv = Collections.emptySet();    
    for (OpSyntax superOp : getSuperOps(s1)) {
      Set<OpSyntax> temp = computeLeastCommonSuper_internal(superOp, s2);
      if (first) {
        first = false;
        rv    = temp;
      } else {    
        // Each superop represents another possible LCS for the two operators       
        rv = mergeLCS(rv, temp);
      }
    }
    return rv;
  }
  
  /**
   * Computes the least common super-operator between s1 and s2
   * 
   * Does not eliminate ancestors of other ops in the set returned
   * Could return more than one, due to multiple inheritance
   * @return A set of LCS for the pair (mutable only if there are 2+ elements) 
   */
  private Set<OpSyntax> computeLeastCommonSuper(OpSyntax s1, OpSyntax s2) {
    if (s1 == null || s2 == null) {
      return Collections.emptySet();
    }
    if (s1 == rootOp || s2 == rootOp) {
      return Collections.emptySet();
    }
    Set<OpSyntax> rv = computeLeastCommonSuper_internal(s1, s2);    
    return eliminateAncestors(rv);
  }
  
  private Set<OpSyntax> eliminateAncestors(Set<OpSyntax> ops) {
    switch (ops.size()) {
    case 0:
    case 1:
      return ops;
    default:
      for (OpSyntax op : new HashSet<OpSyntax>(ops)) {
        if (ops.size() <= 1) {
          return ops;
        } else {
          eliminateAncestors(ops, getSuperOps(op)); 
        }
      }   
      return ops;
    }
  }
  
  private void eliminateAncestors(Set<OpSyntax> ops, Collection<OpSyntax> superOps) {
    if (superOps.isEmpty()) {
      return;
    }
    ops.removeAll(superOps);
    if (ops.size() <= 1) {
      return;
    }
    for (OpSyntax sop : superOps) {
      eliminateAncestors(ops, getSuperOps(sop));          
    }
  }

  /*********************************************************************
   * Code to propagate the LCS down
   *********************************************************************/
  
//  private String getLcsName(OpSyntax s) {
//    return (s == null || s == rootOp) ? "root node" : s.name;
//  }
  
  /*
   * If one branch is null (not appearing anywhere), then any info about
   * how it appears in the tree must come from the other branch.
   * 
   * (A | B) & (C | D) ==> (A & (C|D)) | (B & (C|D))
   */
  private Set<OpSyntax> combineWithLCS(Set<OpSyntax> s1, Set<OpSyntax> s2) {
    if (s1 == null) {
      return s2;
    } 
    if (s2 == null) {
      return s1;
    }
    switch (s1.size()) {
    case 0:
      return s1; // 
    case 1:
      return combineWithLCS(s2, s1.iterator().next());
    default: 
      switch (s2.size()) {
      case 0:
        return s2;
      case 1:
        return combineWithLCS(s1, s2.iterator().next());
      default: // both have 2 or more
        boolean first    = true;
        Set<OpSyntax> rv = null;   
        for (OpSyntax op : s1) {
          Set<OpSyntax> temp = combineWithLCS(s2, op);
          if (first) {
            first = false;
            rv    = temp;
          } else {  
            rv    = mergeLCS(rv, temp);
          }
        }
        return rv;
      }
    }
  }
  
  /**
   * Get LCS from ancestors, setting those that don't have one yet
   */
  private Set<OpSyntax> propagateLeastCommonSuperDown(final OpSyntax op) {
    if (op == null) {
      return null;
    } 
    final Set<OpSyntax> lcs = lookupLeastCommonSuper(op.name);
    /*
    if (isRoot(op)) {
      System.err.println("No need to propagate to "+op.name+", since already root op");
      return lcs;
    }
    */
    Collection<OpSyntax> superOps = getSuperOps(op);
    if (superOps.isEmpty()) {
      System.err.println("Nothing to propagate to "+op.name+", since no super ops");
      return lcs;
    }
    Set<OpSyntax> supersLCS = null; // = propagateLeastCommonSuperDown(superOp);
    boolean first = true;
    for (OpSyntax superOp : superOps) {
      Set<OpSyntax> tempLCS = propagateLeastCommonSuperDown(superOp);
      if (first) {
        first = false;
        supersLCS = tempLCS;
      } else {
        supersLCS = combineWithLCS(supersLCS, tempLCS);
      }
    }
    // A null supersLCS means that nothing was previously specified
    
    if (supersLCS == null) {
      System.err.println("Nothing to propagate for "+op.name+", since no supersLCS");
      return lcs;
    }
    final Set<OpSyntax> newLCS;
    if (lcs != supersLCS) {
      if (lcs == null) { // No LCS/parents here
        if (supersLCS != rootOp) {
          System.err.println(unparseOps(unparseOps("Propagating LCS ", supersLCS)+" from ", superOps)+" down to "+op.name);
        } else {
          System.err.println(unparseOps("Propagating root: ", superOps)+" => "+op.name);
        }
        newLCS = supersLCS;
      } else {
        newLCS = combineWithLCS(supersLCS, lcs);
        System.err.println(unparseOps("Combining LCS ", lcs) + unparseOps(" from "+op.name+" and sLCS ", supersLCS) + 
                           unparseOps(" from ", superOps) + unparseOps(" => ", newLCS));
      }
      leastCommonSuper.put(op.name, newLCS);
      return newLCS;
    }
    return lcs;
  }
  
  private void computeLeastCommonSuperForParents() {
    copyRealParents();
    
    if (substGrandparentForFixedChildren) {
      for (String name : this.parents.keySet()) {
        LogicalParents parents = substituteParents(name);
        this.parents.put(name, parents.ops);
      }
    }    
    // Compute LCS for each op that has parents
    for (String name : this.parents.keySet()) {
      Set<OpSyntax> parents = lookupParents(name);
      Set<OpSyntax> simplified = simplifyParents(parents);
      System.err.println(unparseOps("Simplified parents for "+name+": ", simplified));
      this.parents.put(name, simplified);
        
      Set<OpSyntax> lcs = computeLeastCommonSuper(parents);
      /*
      if (lcs != null) {
        System.err.println("LCS for just "+name+" is "+lcs.name);
      } else {
        System.err.println("LCS for just "+name+" is the root node");
      }
      */
      leastCommonSuper.put(name, lcs);
    }    

    for (String op : syntax.keySet()) {
      propagateLeastCommonSuperDown(lookup(op));
    }
    
    Set<String> done = new HashSet<String>();
  outer:
    for (String op : syntax.keySet()) {
      OpSyntax s        = lookup(op);
      Set<OpSyntax> lcs = lookupLeastCommonSuper(op);
      
      while (op != null) {
        if (done.contains(op)) {
          continue outer;
        }
        OpSyntax parent         = lookup(s.parentOperator);
        Set<OpSyntax> supersLCS = lookupLeastCommonSuper(s.parentOperator);
        if (s.isRoot || lcs != supersLCS) {
          if (lcs == null) {   
            if (!s.packageName.endsWith("promise")) {
              System.err.println("No LCS found for "+op);
            }
          } else if (lcs.isEmpty()) {
            //System.err.println("LCS for "+op+" is the root op");
          } else if (lcs.size() > 1) {
            System.err.println(unparseOps("WARNING: more than one LCS for "+op+": ", lcs));
          } else {
            System.err.println(unparseOps("LCS for "+op+" is ", lcs));
          }
        }
        done.add(op);
        if (s.isRoot) {
          op = null;
        } else {
          op = s.parentOperator;
          s = parent;
          lcs = supersLCS;
        }
      }
    }  
  }
  
  /*********************************************************************
   *  Code for converting OptFoo nodes into Foo
   *********************************************************************/
  
  protected final boolean isNullVariant(String name) {
    OpSyntax s = lookup(name);
    if (s == null) {
      return false;
    }
    return s.props.get(KnownProperty.NULL_VARIANT) != null;
  }
  
  protected final boolean couldBeNullVariant(Child c) {
    if (c.opt) {
      return true;
    }
    OpSyntax s = lookup(c.type);
    if (s == null) {
      return false;
    }
    return s.props.get(KnownProperty.NONNULL_VARIANTS) != null;
  }
  
  /**
   * 
   * @return The nonnull variant of this type, or its own name
   */
  protected final String getNonnullVariant(String name) {
    OpSyntax s = lookup(name);
    if (s == null) {
      return name;
    }
    String variant = s.props.get(KnownProperty.NONNULL_VARIANTS);
    if (variant != null) {
      return variant;
    }
    return s.name;
  }

  /*********************************************************************
   *  Table of FooInterface names to OpSyntax
   *********************************************************************/
  private final Map<String,OpSyntax> ifaceMap = new HashMap<String,OpSyntax>(); 
  
  protected final OpSyntax lookupIface(String name) {
    return ifaceMap.get(name);
  }
  
  /*********************************************************************
   *  Table of OpSyntax objects
   *********************************************************************/
  protected final Map<String,OpSyntax> syntax = new HashMap<String,OpSyntax>(); 
  
  private final void accept(OpSyntax s) {
    syntax.put(s.name, s);
    /*
    System.err.print(s.name+" ");
    s.printProperties(System.err);
    System.err.println();
    */
    if ("Declaration".equals(s.name)) {
      s.printProperties(System.err);
    }
    
    BindingType b = getBindsToName(s);
    if (b != null) {
      System.err.println("Binds to: "+s.name+" => "+b);
    }
    String b2 = s.props.get(KnownProperty.BINDING);
    if (b2 != null) {
      System.err.println(s.name+" implements '"+makeBindingName(b2)+"'");
    }
    
    if (isLogicallyInvisible(s)) {
      System.err.println("Marked as logically invisible: "+s.name);
    }
    
    if (!okToGenerate(s)) {
      System.err.println("Not OK to generate: "+s.name);
    }

    for (String iface : s.superifaces) {      
      // Check if the current operator matches the iface name (which might be shortened)
      if (s.name.startsWith(iface)) {
        // Reject if any extra chars are capitalized
        String extra = s.name.substring(iface.length());
        if (!extra.equals(extra.toLowerCase())) {
          System.err.println("NOT mapping "+iface+"Interface to "+s.name);
          continue;
        }
         
        ifaceMap.put(iface, s);
        System.err.println("Mapping "+iface+"Interface to "+s.name);
        break; // There shouldn't be any more
      }
    }
    addParents(s);
  }

  protected final OpSyntax lookup(String name) {
    return syntax.get(name);
  }
  
  protected final Iterable<Map.Entry<String,OpSyntax>> iterate() {
    return syntax.entrySet();
  }
  
  class OkToGenerateIterable implements Iterable<Map.Entry<String,OpSyntax>> {
    private final String tag;
    OkToGenerateIterable(String t) {
      tag = t;
    }    
    OkToGenerateIterable() {
      this(null);
    }

    private boolean matchesTag(OpSyntax s) {
      return tag == null || isTaggedWith(s, tag);
    }
    
    @Override
    public Iterator<Entry<String, OpSyntax>> iterator() {
      return new Iterator<Entry<String, OpSyntax>>() {
        final Iterator<Entry<String, OpSyntax>> it = syntax.entrySet().iterator();
        Entry<String, OpSyntax> next = null;
        
        private Entry<String, OpSyntax> getNext() {            
          while (it.hasNext()) {
            Entry<String, OpSyntax> rv = it.next();
            OpSyntax s = rv.getValue();
            if (matchesTag(s) && okToGenerate(s)) {
              return rv;
            } else {
              System.err.println("Not OK to generate "+rv.getKey());
            }
          }
          return null;
        }
        @Override
        public boolean hasNext() {
          if (next != null) {
            return true;
          }
          // No element waiting, so
          next = getNext();
          return (next != null);
        }
        @Override
        public Entry<String, OpSyntax> next() {
          Entry<String, OpSyntax> rv = null;
          if (next != null) {
            rv   = next;
            next = null;
          } else {
            // No element waiting, so
            rv = getNext();
            if (rv == null) {
              throw new NoSuchElementException();
            }
          }
          return rv;
        }
        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }         
      };
    }     
  }
  private final Iterable<Map.Entry<String,OpSyntax>> okToGenerateIterable = new OkToGenerateIterable();
  
  protected final Iterable<Map.Entry<String,OpSyntax>> iterateIfOkToGenerate() {
    return okToGenerateIterable;
  }
  
  protected final Iterable<Map.Entry<String,OpSyntax>> iterateIfOkToGenerate(String tag) {
    if (tag == null) {
      throw new IllegalArgumentException("tag is null");
    }
    return new OkToGenerateIterable(tag);
  }
  
  protected final Set<String> packagesAppearing() {
    Set<String> ss = new HashSet<String>();
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate()) {
      OpSyntax s = e.getValue();
      ss.add(s.packageName);
    }
    return ss;
  }
  
  /*********************************************************************
   *  Output stream state
   *********************************************************************/
 
  private boolean forceOut = false;
  protected String outDir = null;
  private PrintStream out = System.out;
  private final Stack<PrintStream> outStack = new Stack<PrintStream>();
  
  private final void resetPrintStream() {
    if (out != System.out) {
      out.close();
    }
    out = System.out;
  }
  
  protected final boolean openPrintStream(String name) {
    try {
      out = new PrintStream(name);
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace(System.err);
      resetPrintStream();
      return false;
    }
  }
  /*
  protected StringBuilder pushBufferStream() {
    StringBuilder sb = new StringBuilder();
    outStack.push(out);
    out = new PrintStream();
    return sb;
  }
  */
  protected void popBufferStream() {
    out = outStack.pop();
  }
  
  /*********************************************************************
   *  Framework code
   *********************************************************************/
  
  private static final FilenameFilter onlyJavaFiles = new FilenameFilter() {
	  @Override
    public boolean accept(File dir, String name) {
      boolean rv = name.endsWith(".java");
      if (!rv) {
        File f = new File(dir, name);
        rv = f.isDirectory();
      }
      //System.err.println("Accepting "+name+": "+rv);
      return rv;
    }
  };
  
  /**
   * Return the earliest last modified time for a
   * file or files in a directory
   */
  private long getEarliestLastModified(FilenameFilter ff, String name) {
    File f = new File(name);
    return getLastModified(ff, f, true);
  }
  
  /**
   * Return the earliest last modified time for a
   * file or files in a directory
   */
  private long getEarliestLastModified(FilenameFilter ff, Iterable<String> paths) {
    long rv = Long.MAX_VALUE;
    for (String path : paths) {
      long temp = getEarliestLastModified(ff, path);
      if (temp < rv) {
        rv = temp;
      }
    }
    return rv;
  }
  
//  /**
//   * Return the last modified time for a
//   * file or files in a directory
//   */
//  private long getLastModified(FilenameFilter ff, String name) {
//    File f = new File(name);
//    return getLastModified(ff, f, false);
//  }
  
  /**
   * Return the last modified time for a
   * collection of ops
   */
  private long getLastModified(List<OpSyntax> ops, boolean earliest) {
    long rv = earliest ? Long.MAX_VALUE : Long.MIN_VALUE;
    for (OpSyntax op : ops) {
      long time = op.lastModified;
      if (earliest) {
        rv = (time < rv) ? time : rv;
      } else {
        rv = (time > rv) ? time : rv;
      }
    }
    return rv;
  }
  
  private long getLastModified(List<OpSyntax> ops) {
    return getLastModified(ops, false);
  }
  
  /**
   * 
   * @param earliest If true, get the earliest time; otherwise, get the latest
   */
  private long getLastModified(FilenameFilter ff, File f, boolean earliest) {
    if (f.exists()) {
      if (f.isFile()) {
        //System.err.println("Looking at "+f.getName()+": "+f.lastModified());
        return f.lastModified();
      } else {
        long rv = earliest ? Long.MAX_VALUE : Long.MIN_VALUE;
        for (File child: f.listFiles(ff)) {
          long time = getLastModified(ff, child, earliest);
          if (earliest) {
            rv = (time < rv) ? time : rv;
          } else {
            rv = (time > rv) ? time : rv;
          }
        }
        return rv;
      }
    }
    return earliest ? Long.MIN_VALUE : Long.MAX_VALUE;
  }
  
  public void generate(String[] args) {
    List<OpSyntax> syntax = populateSyntax(args);
    if (syntax == null) {
      return;
    }
    if (debug) {
      for (OpSyntax s : syntax) {
        s.printState(System.err);
        System.err.println();
      }
    }
    generate();
  }
  
  protected List<OpSyntax> populateSyntax(String[] args) {
    init();
    
    SyntaxBuilder sb = new SyntaxBuilder("");
    List<String> files = parseArgs(args);
    List<OpSyntax> syntax = (files == null) ? sb.parseOpFiles(args) : sb.parseOpFiles(files);

    processProperties(syntax, sb.getGlobalProperties()); 
    
    // Don't do anything if generated code is up to date
    //
    if (!forceOut && outDir != null) {
      long lastOp   = getLastModified(syntax);
      long firstGen = getEarliestLastModified(onlyJavaFiles, computeGeneratedPaths());

      if (lastOp == Long.MIN_VALUE || firstGen == Long.MAX_VALUE) {
        System.err.println("Continuing because there were no ops");
      } 
      else if (lastOp <= firstGen) {  
        System.err.println("Skipped because the last op ("+lastOp+") <= first gen: "+firstGen);
        return null;
      } 
      else {
        System.err.println("Continuing because the last op ("+lastOp+") > first gen: "+firstGen);
      }
    }
    
    for (OpSyntax s : syntax) {
      accept(s);    
    }        
    finishAccept();
    for (OpSyntax s : syntax) {
      check(s);    
    }
    System.out.println();
    return syntax;
  }
  
  protected abstract class PkgStatus {
    /**     
     * @return The tag that this package is in
     */
    public String getName() { return ""; }
    /**     
     * @return The tag that this package extends
     */
    public String getRoot() { return null; }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof PkgStatus) {
        PkgStatus s = (PkgStatus) o;
        return getName().equals(s.getName());
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return getName().hashCode();
    }
    
    boolean reallyEqual(PkgStatus s) {
      if (getRoot() == null) {
        return s.getRoot() == null;
      }
      return getRoot().equals(s.getRoot());
    }
  }
  
  protected final Map<String,PkgStatus> pkgMappings = new HashMap<String,PkgStatus>();
  protected final Map<PkgStatus,PkgStatus> tags = new HashMap<PkgStatus,PkgStatus>();
  
  private static final String IGNORE_PKG = "ignore";
  private static final String EXTEND_PKG = ">>";
  
  private void processProperties(List<OpSyntax> syntax, Properties props) {
    // Collect the possible set of package names
    Set<String> packages = new HashSet<String>();
    for (OpSyntax s : syntax) { 
      packages.add(s.packageName);
    }
    
    // Process options 
    for (String pkg : packages) {
      final String val = (String) props.get(pkg);
      if (val == null || val.equals(IGNORE_PKG)) {
        System.out.println("Ignoring "+pkg);
        continue;
      }
      System.out.println(pkg+" = "+val);
      
      final PkgStatus s;
      if (val.contains(EXTEND_PKG)) {
        StringTokenizer st = new StringTokenizer(val, EXTEND_PKG);
        final String name = st.nextToken();
        final String root = st.nextToken();
        s = new PkgStatus() {
          @Override
          public String getName() { return name; }
          @Override
          public String getRoot() { return root; }
        };        
      } else { // assume to be ROOT
        s = new PkgStatus() {
          @Override
          public String getName() { return val; }
        };
      }
      PkgStatus s2 = tags.put(s,s);
      if (s2 != null && !s2.reallyEqual(s)) {
        throw new Error("Inconsistent with previous mapping: "+val);
      }      
      pkgMappings.put(pkg, s);
    }
    if (pkgMappings.isEmpty()) {
      throw new Error("No package mappings");
    }
  }

  private boolean isTaggedWith(OpSyntax s, String tag) {
    PkgStatus p = pkgMappings.get(s.packageName);
    return p != null && p.getName().equals(tag);
  }
  
  protected Iterable<PkgStatus> getTags() {
    return tags.keySet();
  }
  
  protected void finishAccept() {
    //matchupDeclsAndRefs();
    testLCS();
    computeLeastCommonSuperForParents();
    computeAbstractOps();
  }

  private void testLCS() {
    testLCS("AnonClassExpression", "Declaration");
    testLCS("EnumDeclaration", "NestedClassDeclaration");
  }

  private void testLCS(String name1, String name2) {
    OpSyntax s1  = lookup(name1);
    OpSyntax s2  = lookup(name2);
    Set<OpSyntax> rv1 = computeLeastCommonSuper(s1, s2);
    Set<OpSyntax> rv2 = computeLeastCommonSuper(s2, s1);
    if (!rv1.equals(rv2)) {
      System.err.println("ERROR!!: LCS is not symmetric for AnonClassExpression and Declaration");
      System.err.println(unparseOps("LCS(ACE,D) = ", rv1));
      System.err.println(unparseOps("LCS(D,ACE) = ", rv2));
    }
  }
  
  /*
  private String cutEnding(String s, String ending) {
    if (s.endsWith(ending)) {
      return s.substring(0, s.length() - ending.length());
    }
    return s;
  }
  
  private void matchupDeclsAndRefs() {
    Map<String,OpSyntax> decls = new HashMap<String,OpSyntax>();
    
    for (Map.Entry<String,OpSyntax> e : syntax.entrySet()) {
      OpSyntax s = e.getValue();      
      if (s.name.endsWith("Declaration")) {
        decls.put(cutEnding(s.name, "Declaration"), s);
      }
      else if (s.name.endsWith("Declarator")) {
        decls.put(cutEnding(s.name, "Declarator"), s);
      }
    }
    for (Map.Entry<String,OpSyntax> e : syntax.entrySet()) {
      OpSyntax s = e.getValue();   
      if (s.name.endsWith("Call")) {
        findMatchingDecl(decls, s, "Call");
      } 
      else if (s.name.endsWith("Ref")) {
        findMatchingDecl(decls, s, "Ref");
      } 
      else if (s.name.startsWith("Use")) {
        System.err.println("Leftover ref found:  "+s.name);
      } 
      else if (s.name.endsWith("Name")) {
        findMatchingDecl(decls, s, "Name");
      }
    }
    for (Map.Entry<String,OpSyntax> e : decls.entrySet()) {
      OpSyntax s = e.getValue();   
      System.err.println("Leftover decl found: "+s.name);
    }
  }

  private void findMatchingDecl(Map<String, OpSyntax> decls, OpSyntax s, String ending) {
    String name   = cutEnding(s.name, ending);
    OpSyntax decl = decls.get(name);
    if (decl != null) {
      System.err.println(s.name+" refers to "+decl.name);
      decls.remove(name);
    } else {
      System.err.println("Leftover ref found:  "+s.name);
    }
  }
  */
  private final List<String> parseArgs(String[] args) {
    if (args.length < 3) {
      return null; // Not parsed
    }
    if (args[0].equals("-out") || args[0].equals(FORCE_OUT)) {
      outDir = args[1];
      
      File f = new File(outDir);
      if (!f.exists() || !f.isDirectory()) {
        System.err.println("File '"+outDir+"' does not exist, or is not a directory");
        outDir = null;
      } else {
        forceOut = args[0].equals(FORCE_OUT);
      }
      
      // Copy remaining args
      List<String> l = new ArrayList<String>();
      for(int i=2; i<args.length; i++) {
        l.add(args[i]);  
      }
      return l;
    }
    return null;
  }
  
  private void init() {
    resetPrintStream();
    syntax.clear();
    handledAttrTypes.clear();
    handledChildTypes.clear();
  }
  
  interface SyntaxRunnable {
    void init(OpSyntax s, String name);
    void run(OpSyntax s);
  }
  
  private void generatePaths(SyntaxRunnable r) {
    String outPath = (outDir == null) ? null : outDir + File.separator;
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate()) {
      OpSyntax s = e.getValue();
      if (outPath != null) {
        String name;
        if (hasExtendedPath(s)) {
          String path = makeExtendedPath(s);
          File pathDir = new File(outDir, path);
          if (!pathDir.exists()) {
            pathDir.mkdirs();
          } else if (!pathDir.isDirectory()) {
            System.err.println("Path is not a directory: "+pathDir.getPath());
            continue;
          }
          name = outPath + path + File.separator + makeFilename(s);
        } else {
          name = outPath + makeFilename(s);
        }
        r.init(s, name);
      }
      r.run(s);
    }
  }
  
  protected final Set<String> computeGeneratedPaths() {
    class R implements SyntaxRunnable {
      final Set<String> paths = new HashSet<String>();
      
      @Override
      public void init(OpSyntax s, String name) {
        int lastSeparator = name.lastIndexOf(File.separatorChar);
        if (lastSeparator >= 0) {
          paths.add(name.substring(0, lastSeparator));
        }
      }
      @Override
      public void run(OpSyntax s) {
    	  // Nothing to do
      }      
    }
    R r = new R();
    generatePaths(r);
    return r.paths;
  }
  
  protected final void generate() {
    generatePaths(new SyntaxRunnable() {
        @Override
      public void init(OpSyntax s, String name) {
        if (openPrintStream(name)) {
          System.err.println("Generating: "+name);
        } 
      }
        @Override
      public void run(OpSyntax s) {
        initEach(s);
        generateEach(s);    
      }      
    });
    generateForAll();
    
    resetPrintStream();
  }
  
  protected boolean okToGenerate(OpSyntax s) {
    return true;
  }

  protected void generateForAll() {
	  // NOthing to do
  }

  protected boolean hasExtendedPath(OpSyntax s) {
    return s.packageName != null;
  }
  
  protected String makeExtendedPath(OpSyntax s) {
    if (s.packageName == null) {
      return "";
    }
    //System.err.println("Package name = "+s.packageName);
    String path  = s.packageName.replace('.', File.separatorChar);
    return path;
  }
  
  protected static String computePath(String pathPrefix, String pkgSuffix) {
    if (pathPrefix == null) {
      return pkgSuffix.replace('.', File.separatorChar);
    }
    return pathPrefix + File.separator + pkgSuffix.replace('.', File.separatorChar);
  }
  
  /**
   * Generate the file name to be created for s
   */
  protected String makeFilename(OpSyntax s) {
    return s.name+".java";
  }
  
  /**
   * Generates the Java name for s
   */
  protected String makeInterfaceName(String name) {
    return name;
  }
  
  protected void initEach(OpSyntax s) {
	  // Nothing to do
  }
  protected abstract void generateEach(OpSyntax s);

  /*********************************************************************
   *  Sanity checking code
   *********************************************************************/
  
  private final Set<String> handledAttrTypes = new HashSet<String>();
  private final Set<String> handledChildTypes = new HashSet<String>();
  
  protected void check(final OpSyntax s) {    
    checkAttributes(s);
    checkChildren(s);
    
    if (!s.isRoot) {
      // Check if parent is defined
      final OpSyntax op = lookup(s.parentOperator);
      if (op == null) {
        if (!handledChildTypes.contains(s.parentOperator)) {      
          System.err.println("Warning: no operator defined for "+s.parentOperator);
        }
        handledChildTypes.add(s.parentOperator);
      } else {              
        checkParent(s, op);
      }
    }
  }
  
  protected void checkParent(OpSyntax s, OpSyntax parent) {
    if (s.syntax.isEmpty()) {
      // No children in current operator: inherits everything from parent
      return;
    }
    
    // Confirm that the parent 
    if (!checkChildren(parent)) {
      System.err.println("Warning: discontinued checks on parent, due to previous error");
      return;
    }
    
    // Check if I have all the attributes of my parent
    for (Attribute a : parent.attributes) {
      if (!s.attributes.contains(a)) {
        System.err.println("Error:  "+parent.name+" defines an attribute "+a.name+" that's not part of "+s.name); 
      }
    }
    // Check if I have more children than my parent
    if (s.children.size() < parent.children.size()) {
      System.err.println("Warning: "+s.name+" has fewer children than its parent "+parent.name); 
    }
    
    checkParentVariability(s, parent);
    checkParentChildren(s, parent);    
  }
  
  /**
   * Check if the variability of the parent and child match
   */
  protected void checkParentVariability(OpSyntax s, OpSyntax parent) {
    if (s.variability == null) {
      if (parent.variability != null) {
        System.err.println("Error:  "+s.name+" is not variable, but its parent "+parent.name+" ("+parent.variability+") is."); 
      }
    } else if (parent.variability == null) {
      if (parent.isConcrete) {
        System.err.println("Error:  "+s.name+" ("+s.variability+") is variable, but its parent "+parent.name+" is not."); 
      }
      // otherwise the parent is completely abstract
    } else if (!s.variability.equals(parent.variability)) {
      System.err.println("Error:  "+s.name+" ("+s.variability+") does not have the same variability as its parent "+
                         parent.name+" ("+parent.variability+")"); 
    }
  }
  
  /**
   * Check that the following hold:
   * 1. the concrete children of the parent form a prefix of this operator's children
   * 2. the abstract children of the parent appear in order in this operator's remaining children
   * 
   * If the parent has C1 C2 A3, then a child with C1 C2 C3 matches,
   * as does C1 C2 
   */
  protected void checkParentChildren(OpSyntax s, OpSyntax parent) {
    Iterator<Child> parentChildren = parent.children.iterator();
    Iterator<Child> sChildren      = s.children.iterator();
    
    while (parentChildren.hasNext()) {
      if (!sChildren.hasNext()) {
        System.err.println("Warning: "+s.name+" has fewer children than "+parent.name);
        // check if rest of parent's children are optional
        do { 
          Child parentC = parentChildren.next();
          
          if (!parentC.type.startsWith(OpSyntax.OPT_CHILD_PREFIX)) {
            System.err.println("Error:   "+parent.name+" has a child "+parentC.name+" that is not optional");
            return;
          }
        }
        while (parentChildren.hasNext());

        return;
      }
      Child parentC = parentChildren.next();
      Child sC      = sChildren.next();
      
      if (parentC.isAbstract()) {
        while (!parentC.equals(sC)) {
          if (!sChildren.hasNext()) {
            System.err.println("Error:   Couldn't match abstract child "+parentC.name+" in "+s.name);
            return;
          }
          sC = sChildren.next();
        }
      } 
      else { // concrete child
        if (!parentC.equals(sC)) {
          System.err.println("Error:   concrete child "+parentC.name+" of parent doesn't match "+
                             " child "+sC.name);
        }
      }      
    }
  }
  
  protected void checkAttributes(OpSyntax s) {
    for (Attribute a : s.attributes) {
      // Check if type is defined
      if (!handledAttrTypes.contains(a.type)) {
        String t = typeTable.get(a.type);
        if (t == null) {
          System.err.println("Warning: no type defined for "+a.type);
        }
        handledAttrTypes.add(a.type);
      }
      // Check if args are legal
      if (!a.args.isEmpty()) {
        Set<String> legalArgs = typeArgsTable.get(a.type);
        if (legalArgs == null) {
          System.err.println("Error:   no type args defined for "+a.type);
        } else {
          for (String arg : a.args) {
            if (!legalArgs.contains(arg)) {
              System.err.println("Error:   arg "+arg+" is not defined for "+a.type);
            }
          }
        }
      }
    }
  }
  
  protected boolean checkChildren(OpSyntax s) {
    boolean ok = true;
    
    // Ref to the first abstract child
    Child firstAbstract = null; 

    for (Child c : s.children) {
      // Check if type is defined
      if (!handledChildTypes.contains(c.type)) {
        OpSyntax op = lookup(c.type);
        if (op == null) {
          if (c.type.endsWith("s")) {
            //System.err.println("Info:    will create operator "+c.type);
            System.err.println("Warning: no operator defined for "+c.type);
          } else {
            System.err.println("Warning: no operator defined for "+c.type);
          }
          ok = false;
        }
        handledChildTypes.add(c.type);
      }
      // Check if the children match the pattern C*A*
      if (c.isAbstract()) {
        if (firstAbstract == null) {
          firstAbstract = c;
        }
        // otherwise, we're in A*, so it's OK
      } else { // we're a concrete child
        if (firstAbstract != null) {
          System.err.println("Error:   got a concrete child "+c.name+
                             " after seeing abstract child "+firstAbstract.name);
          ok = false;
        }
        // otherwise, we're in C*, so it's OK
      }
    }
    return ok;
  }
  
  /*********************************************************************
   *  Functions on Strings
   *********************************************************************/
  
  protected static String capitalize(String name) {
    if (name.length() == 0) {
      return "";
    }
    char[] temp = name.toCharArray();
    temp[0] = Character.toUpperCase(temp[0]);
    return new String(temp);
  }
  
  protected static boolean matches(Pattern p, String s) {
    return p.matcher(s).matches();
  }
  
  private static String makeSpaces(int i) {
    char[] temp = new char[i];
    Arrays.fill(temp, ' ');    
    return new String(temp);
  }
  static final String oneHundredSpaces = makeSpaces(100);
  public static final String[] noStrings = new String[0];
  
  protected static String getSpaces(int i) {
    if (i > 100) {
      return makeSpaces(i);
    }
    return oneHundredSpaces.substring(0, i);
  }
  
  private static String generateBindingName(String prefix, String name) {
    return prefix+name+"Binding";
  }
  
  private static String generateTypeName(String prefix, String name) {
    if (name.contains("Type")) {
      return prefix+name;
    }
    return prefix+name+"Type";
  }
  
  protected static String makeBindingName(String name) {
    return generateBindingName("I", name);
  }
  
  protected static String makeTypeName(String name) {
    return generateTypeName("I", name);
  }
  
  protected static String makeHasBindingName(String name) {
    return generateBindingName("IHas", name);
  }
  
  protected static String makeHasTypeName(String name) {
    return generateTypeName("IHas", name);
  }

  /*********************************************************************
   *  Generator code
   *********************************************************************/
  
  protected void printfJava(String format, Object... args) {
    out.printf(format, args);
  }
  
  protected void printJava(String msg) {
    out.print(msg);
  }  
  
  protected static abstract class MethodDescriptor {
    private final String sig, noVal;
    MethodDescriptor(String sig, String noVal) {
      this.sig = sig;
      this.noVal = noVal;
    }
    final String getSignature() { return sig; }
    final String noValue() { return noVal; }
  }
  
  protected static abstract class ChildMethodDescriptor extends MethodDescriptor {
    protected ChildMethodDescriptor(String sig, String noVal) {
      super(sig, noVal);
    }
    protected abstract String childValue(OpSyntax s, Child c);
    protected abstract String variableValue(OpSyntax s);
  }
  
  protected static abstract class InfoMethodDescriptor extends MethodDescriptor {
    protected InfoMethodDescriptor(String sig, String noVal) {
      super(sig, noVal);
    }
    protected abstract String infoValue(OpSyntax s, Attribute a);
  }
  
  protected void generateMethod(OpSyntax s, InfoMethodDescriptor md) {
    printJava("  @Override\n");
    printJava("  public "+md.getSignature()+" {\n");
    if (s.attributes.size() == 0) {
      printJava("    return "+md.noValue()+";\n");
    } else {
      printJava("    switch (i) {\n");
      for (Attribute attr : s.attributes) {
        printJava("    case "+attr.index+": return "+md.infoValue(s, attr)+";\n");       
      }
      printJava("    default: return "+md.noValue()+";\n");
      printJava("    }\n");
    }
    printJava("  }\n\n");
  }
  
  protected void generateMethod(OpSyntax s, ChildMethodDescriptor md) {
    printJava("  @Override\n");
    printJava("  public "+md.getSignature()+" {\n");
    if (s.numChildren == 0) {
      if (s.isVariable()) {
        printJava("    return "+md.variableValue(s)+";\n");
      } else {
        printJava("    return "+md.noValue()+";\n");
      }
    } else {
      printJava("    switch (i) {\n");
      int i = 0;
      for (Child c : s.children) {
        if (c == s.variableChild) {
          break; // this will be handled by the default
        } 
        printJava("    case "+i+": return "+md.childValue(s, c)+";\n");
        i++;
      }
      if (s.isVariable()) {
        printJava("    default: return "+md.variableValue(s)+";\n");
      } else {
        printJava("    default: return "+md.noValue()+";\n");
      }
      printJava("    }\n");
    }
    printJava("  }\n\n");
  }
  
  /*********************************************************************
   *  Code to compute if an operator should be considered abstract
   *********************************************************************/  

  /**
   * Returns true if it'there is an abstract child in s
   * or if s has no parent and is empty
   */
  private final boolean isAbstractOp(OpSyntax s) {
    // A root that is otherwise empty
    if (isRoot(s) && canInheritAbstractness(s)) {
      return true;
    } 
    
    for (Child c : s.children) {
      if (c.isAbstract()) {
        return true;
      }
    }
    return false;
  }
  
  private Set<OpSyntax> abstractOps = new HashSet<OpSyntax>();
  
  private final void setAbstract(OpSyntax s) {
    abstractOps.add(s);
  }
  
  protected final boolean isAbstract(OpSyntax s) {
    return abstractOps.contains(s);
  }
  
  /**
   * If the parent is considered abstract, 
   * then this should also be if it has no children
   * and all its attributes are abstract
   */
  private boolean canInheritAbstractness(OpSyntax s) {
    if (isLeaf(s)) {
      return false;
    }
    if (s.children.isEmpty()) {
      for (Attribute a : s.attributes) {
        if (!a.isAbstract()) {
          return false;
        }        
      }
      return true;
    }
    return false;
  }
  
  // True only if all parents are abstract
  protected boolean hasAbstractParents(OpSyntax s) {
    for (OpSyntax p : getSuperOps(s)) {
      if (!isAbstract(p)) {
        //System.err.println(s.name+" not considered abstract because of parent "+p.name);
        return false;
      }
    }
    return true;
  }
  
  /**
   * Iterate looking for 
   *
   */
  private void computeAbstractOps() {
    boolean changed;
    do {
      changed = false;
      for (Map.Entry<String,OpSyntax> e : iterate()) {
        OpSyntax s         = e.getValue();
        boolean isAbstract = isAbstract(s);
        if (isAbstract) {
          continue; // No need to look at it again
        }
        
        if (hasAbstractParents(s) &&  canInheritAbstractness(s)) {      
          setAbstract(s);
          changed = true;
          System.err.println("Set "+s.name+unparseOps(" as abstract, because of its parents ", getSuperOps(s)));
        } 
        else if (isAbstractOp(s)) {        
          setAbstract(s);
          changed = true;
          System.err.println("Set "+s.name+" as abstract");
        }
      }
    } while (changed);
    
    for (Map.Entry<String,OpSyntax> e : iterate()) {
      OpSyntax s = e.getValue();
      if (!isAbstract(s)) {
        // Nothing to do
      }
    }
  }

  /*********************************************************************
   *  getBindingType
   *********************************************************************/

  protected class BindingType {
    private static final String SEQ_PREFIX = "seq:";
    
    public final String name;
    public final boolean isSequence;
    private final boolean isType;
    
    private BindingType(String s, boolean isType) {
      this.isType = isType;
      if (s.startsWith(SEQ_PREFIX)) {
        name = s.substring(SEQ_PREFIX.length());
        isSequence = true;
      } else {
        name = s;
        isSequence = false;
      }
    }
    @Override
    public String toString() {
      if (isSequence) {
        return "seq<"+name+">";
      }
      return name;
    }
    
    public String getBindingName() {
      if (isType) {
        throw new UnsupportedOperationException("This is a type");
      }
      return isSequence ? "Iterable<"+makeBindingName(name)+">" : makeBindingName(name);
    }
    
    public String getTypeName() {
      if (!isType) {
        throw new UnsupportedOperationException("This isn't a type");
      }
      return isSequence ? "Iterable<"+makeTypeName(name)+">" : makeTypeName(name);
    }
    public String getHasBindingName() {
      if (isType) {
        throw new UnsupportedOperationException("This is a type");
      }
      //return isSequence ? makeHasBindingName(name+"Seq") : makeHasBindingName(name);
      return isSequence ? "IHasBindingSeq" : "IHasBinding";

    }
    public String getHasTypeName() {
      if (!isType) {
        throw new UnsupportedOperationException("This isn't a type");
      }
      //return isSequence ? makeHasTypeName(name+"Seq") : makeHasTypeName(name);
      return isSequence ? "IHasTypeSeq" : "IHasType";
    }
  }
  
  protected final BindingType getBindsToName(final OpSyntax s) {    
    String t = s.props.get(KnownProperty.BINDS_TO);
    if (t != null) {
      return new BindingType(t, false);
    }
    /*
    // Check if it matches certain patterns
    Matcher m  = null;
    String val = null;
    if ((m = nameTypeMatch.matcher(s.name)).matches()) {
      val = m.group(1);
    } 
    else if ((m = callTypeMatch.matcher(s.name)).matches()) {
      val = m.group(1);
    }
    if (val != null) {      
      for (String b : bindings) {
        if (b.equals(val)) {
          System.out.println("Heuristic: binding for "+s.name);
          return b;
        }
      }
    }
    */
    return null;
  }
 
  protected final BindingType getBindsToTypeName(final OpSyntax s) {    
    String t = s.props.get(KnownProperty.BINDS_TO_TYPE);
    if (t != null) {
      return new BindingType(t, true);
    }
    return null;
  }
  
  protected final boolean implementsBinding(OpSyntax s) {
    String binding  = s.props.get(KnownProperty.BINDING);
    String tBinding = s.props.get(KnownProperty.TYPE_BINDING);
    return binding != null || tBinding != null;
  }
  
  protected final String getTypeBindingName(final OpSyntax s) {    
    String t = s.props.get(KnownProperty.TYPE_BINDING);
    if (t != null) {
      if (t.contains("Type")) {
        return t;
      }
      return t+"Type";
    }
    return null;
  }
  
  protected final String getBridgesToName(final OpSyntax s) {    
    String t = s.props.get(KnownProperty.BRIDGES_TO);
    if (t != null) {
      return t;
    }
    return null;
  }
}
