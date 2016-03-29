package edu.cmu.cs.fluid.java.bind;

import static edu.cmu.cs.fluid.java.bind.IMethodBinder.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.surelogic.ast.java.operator.ITypeFormalNode;
import com.surelogic.common.Pair;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.Iteratable;
import com.surelogic.common.util.PairIterator;
import com.surelogic.common.util.SingletonIterator;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinding.Util;
import edu.cmu.cs.fluid.java.bind.IJavaType.BooleanVisitor;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.CallState;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.ICallState;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.MethodBinding;
import edu.cmu.cs.fluid.java.bind.MethodBinder8.MethodBinding8;
import edu.cmu.cs.fluid.java.bind.MethodBinder8.RefState;
import edu.cmu.cs.fluid.java.bind.MethodInfo.Kind;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.operator.CallInterface.NoArgs;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Triple;

public class TypeInference8 {
  private static final boolean customizeMBForBoundsSet = true;
	
  final MethodBinder8 mb;
  final ITypeEnvironment tEnv;
  final TypeUtils utils;
  final Map<Pair<IRNode, IRNode>, BoundSet> b_2Cache = new ConcurrentHashMap<Pair<IRNode, IRNode>, BoundSet>();

  TypeInference8(MethodBinder8 b) {
    mb = b;
    tEnv = mb.tEnv;
    utils = new TypeUtils(tEnv);
  }

  static String hexToAlpha(String hex) {
    int n = Integer.parseInt(hex, 16);
    return Integer.toString(n, Character.MAX_RADIX);
  }

  interface Dependable {
    int getIndex();

    void setIndex(int i);

    int getLowLink();

    void setLowLink(int i);
  }

  // TODO how to distinguish from each other
  // TODO how to keep from polluting the normal caches?
  static final class InferenceVariable extends JavaReferenceType
      implements IJavaTypeFormal, Comparable<InferenceVariable>, Dependable {
    final IRNode formal;
    final IDebugable origin;

    // For computing the node ordering
    private int index;
    private int lowlink;
    //private final Throwable trace = new Throwable("For stack trace");

    InferenceVariable(IRNode tf, IDebugable orig) {
      formal = tf;
      origin = orig;
    }

    @Override
    void writeValue(IROutput out) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return toSourceText(null);
    }
    
	public String toSourceText(IDebugable context) {
      String rv = super.toString();
      int last = rv.lastIndexOf('@');
      String alpha = hexToAlpha(rv.substring(last + 1));
      if (formal == null) {
        return '@' + alpha;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append(alpha).append("@ ");
      
      final IRNode decl = VisitUtil.getEnclosingDecl(formal);
      if (TypeDeclaration.prototype.includes(decl)) {
        sb.append(JavaNames.getRelativeTypeName(formal).replaceAll(" extends java.lang.Object", ""));
      } else { // Assume it's a method/constructor
    	final IRNode type = VisitUtil.getEnclosingType(decl);
    	sb.append(JavaNames.getRelativeTypeName(type)).append('.');
    	sb.append(JJNode.getInfoOrNull(decl)).append('.'); 
    	sb.append(JJNode.getInfoOrNull(formal));  
      }      
      if (origin != null && !origin.equals(context)) {
    	  sb.append(" @");
    	  sb.append(origin.toSourceText());
    	  /*
      } else {
    	  StackTraceElement e = trace.getStackTrace()[1];
    	  sb.append(" !");    	  
    	  sb.append(e);
    	  */
      }
      return sb.toString();
    }

    public IJavaReferenceType getLowerBound() {
      return null;
    }

    public IJavaReferenceType getUpperBound(ITypeEnvironment te) {
      return null;
    }

    public ITypeFormalNode getNode() {
      throw new UnsupportedOperationException();
    }

    public IRNode getDeclaration() {
      return null;
    }

    public IJavaReferenceType getExtendsBound(ITypeEnvironment te) {
      return null;
    }

    public IJavaReferenceType getExtendsBound() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
      return this == t2;
    }

    @Override
    public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
      return new SingletonIterator<IJavaType>(env.getObjectType());
    }

    @Override
    public int compareTo(InferenceVariable o) {
      if (o.formal == null) {
        if (formal == null) {
          return toString().compareTo(o.toString());
        }
        return 1;
      }
      if (formal == null) {
        return -1;
      }
      return formal.toString().compareTo(o.formal.toString());
    }

    @Override
    public IJavaType subst(final IJavaTypeSubstitution s) {
      if (s == null)
        return this;
      IJavaType rv = s.get(this);
      if (rv != this) {
        return rv;
      }
      return this;
    }

    @Override
    public int getIndex() {
      return index;
    }

    @Override
    public void setIndex(int i) {
      index = i;
    }

    @Override
    public int getLowLink() {
      return lowlink;
    }

    @Override
    public void setLowLink(int i) {
      lowlink = i;
    }
  }

  static boolean isProperType(IJavaType t) {
    BooleanVisitor v = new BooleanVisitor(true) {
      public boolean accept(IJavaType t) {
        if (t instanceof InferenceVariable) {
          result = false;
        }
        return true;
      }
    };
    t.visit(v);
    return v.result;
  }

  static void getReferencedInferenceVariables(final Collection<InferenceVariable> vars, IJavaType t) {
    t.visit(new IJavaType.BooleanVisitor() {
      public boolean accept(IJavaType t) {
        if (t instanceof InferenceVariable) {
          vars.add((InferenceVariable) t);
        }
        return true;
      }
    });
  }

  boolean valueisEquivalent(IJavaType v, IJavaType t) {
    if (v instanceof TypeVariable) {
      TypeVariable tv = (TypeVariable) v;
      if (t instanceof TypeVariable) {
        return tv.isEqualTo(tEnv, t);
      }
      /*
       * if (tv.getLowerBound() != null && !tv.getLowerBound().isSubtype(tEnv,
       * t)) { return false; } IJavaType upper = tv.getUpperBound(tEnv); if
       * (upper != null && !t.isSubtype(tEnv, upper)) { return false; } return
       * true;
       */
    }
    return v.isEqualTo(tEnv, t);
  }

  class TypeVariable extends JavaReferenceType implements IJavaTypeVariable {
    final InferenceVariable var;
    IJavaReferenceType lowerBound, upperBound;

    TypeVariable(InferenceVariable v) {
      var = v;
    }

    public IJavaReferenceType getLowerBound() {
      return lowerBound;
    }

    public IJavaReferenceType getUpperBound(ITypeEnvironment te) {
      return upperBound;
    }

    void setLowerBound(IJavaReferenceType l) {
      if (l == null || lowerBound != null) {
        throw new IllegalStateException();
      }
      lowerBound = l;
    }

    void setUpperBound(IJavaReferenceType l) {
      if (l == null || upperBound != null) {
        throw new IllegalStateException();
      }
      upperBound = l;
    }

    public IJavaType subst(IJavaTypeSubstitution s) {
      if (previouslyVisited) {
        return this; // TODO is this right?
      }
      previouslyVisited = true;
      try {
        IJavaType newLower = lowerBound == null ? null : lowerBound.subst(s);
        IJavaType newUpper = upperBound == null ? null : upperBound.subst(s);
        if (newLower != lowerBound || newUpper != upperBound) {
          TypeVariable v = new TypeVariable(var);
          v.lowerBound = (IJavaReferenceType) newLower;
          v.upperBound = (IJavaReferenceType) newUpper;
          return v;
        }
        return this;
      } finally {
        previouslyVisited = false;
      }
    }

    @Override
    public void visit(Visitor v) {
      if (previouslyVisited && !(v instanceof BoundSet.RecursiveTypeChecker)) { // HACK
        return;
      }
      previouslyVisited = true;
      try {
    	  boolean go = v.accept(this);
    	  if (go) {
    		  if (upperBound != null) {
    			  upperBound.visit(v);
    		  }
    		  if (lowerBound != null) {
    			  lowerBound.visit(v);
    		  }
    		  v.finish(this);
    	  }
      } finally {
        previouslyVisited = false;
      }
    }

    @Override
    void writeValue(IROutput out) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      if (previouslyVisited) {
        return "&" + computeAlpha() + "[...]";
      }
      previouslyVisited = true;
      try {
        StringBuilder sb = new StringBuilder("&");
        sb.append(computeAlpha());
        sb.append('[');
        String lb = computeString(lowerBound);
        String ub = computeString(upperBound);
        if (lb != null || ub != null) {
          sb.append(lb == null ? "..." : lb);
          sb.append(", ");
          sb.append(ub == null ? "..." : ub);
        }
        sb.append(']');
        return sb.toString();
      } finally {
        previouslyVisited = false;
      }
    }

    private String computeString(IJavaReferenceType t) {
      if (t == null) {
        return null;
      }
      String s = t.toString();
      if ("java.lang.Object".equals(s)) {
        return null;
      }
      return t.toString();
    }

    // Used by toString()
    private boolean previouslyVisited = false;

    private String computeAlpha() {
      String rv = super.toString();
      int last = rv.lastIndexOf('@');
      String alpha = hexToAlpha(rv.substring(last + 1));
      return alpha;
    }

    @Override
    public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
      if (t2 instanceof TypeVariable) {
        TypeVariable v2 = (TypeVariable) t2;
        if (previouslyVisited && v2.previouslyVisited) {
        	return true;
        }
        if (previouslyVisited != v2.previouslyVisited) {
        	return false;
        }
        // HACK
        if (lowerBound == null && upperBound == tEnv.getObjectType()) {
          return true;
        }
        if (v2.lowerBound == null && v2.upperBound == tEnv.getObjectType()) {
          return true;
        }
        try {
        	previouslyVisited = true;
        	v2.previouslyVisited = true;
        
        	//System.out.println("Checking bounds for "+this+" vs "+t2);
        	return checkBound(lowerBound, v2.lowerBound, JavaTypeFactory.nullType) &&
        		   checkBound(upperBound, v2.upperBound, tEnv.getObjectType());
        } finally {
        	previouslyVisited = false;
        	v2.previouslyVisited = false;
        }
      }
      /*
       * if (lowerBound != null && !lowerBound.isSubtype(tEnv, t2)) { return
       * false; } if (upperBound != null) { return t2.isSubtype(tEnv,
       * upperBound)) { } return true;
       */
      return false;
    }

    private boolean checkBound(IJavaReferenceType b1, IJavaReferenceType b2, IJavaReferenceType ifNull) {
      if (b1 == b2) {
        return true;
      }
      if (b1 == null) {
        return b2 == ifNull;
      }
      if (b2 == null) {
        return b1 == ifNull;
      }
      return b1.isEqualTo(tEnv, b2);
    }

    public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
      if (upperBound != null) {
        return new SingletonIterator<IJavaType>(upperBound);
      }
      return new SingletonIterator<IJavaType>(tEnv.getObjectType());
    }

    boolean isBound() {
      return upperBound != null;
    }
  }

  /**
   * 18.5.1 Invocation Applicability Inference
   * 
   * Given a method invocation that provides no explicit type arguments, the
   * process to determine whether a potentially applicable generic method m is
   * applicable is as follows:
   * 
   * - Where P 1 , ..., P p (p >= 1) are the type parameters of m , let α 1 ,
   * ..., α p be inference variables, and let θ be the substitution [P 1 :=α 1 ,
   * ..., P p :=α p ] .
   * 
   * - An initial bound set, B 0 , is constructed from the declared bounds of P
   * 1 , ..., P p , as described in Â§18.1.3.
   * 
   * ... see below
   * 
   * @return the bound set B 2 if the method is applicable
   */
  BoundSet inferForInvocationApplicability(ICallState call, MethodBinding m, InvocationKind kind, boolean debug) {
	/*
	final String unparse = call.toString();
	if (unparse.equals("<implicit>.cartesianProduct(Arrays.asList(#))")) {
		System.out.println("Found problematic call");
	}
    */
    // HACK to deal with type variables in the receiver?
    BoundSet hack = null;
    if (m.mkind == Kind.METHOD && call instanceof CallState && call.getReceiverType() != null) {
      final TypeFormalCollector v = new TypeFormalCollector(true/*all formals*/);
      m.getReceiverType().visit(v);

      // Remove any type formals that enclose the call,
      // since they're really constants
      final Iterator<IJavaTypeFormal> formals = v.formals.iterator();
      while (formals.hasNext()) {
    	final IJavaTypeFormal t = formals.next();
    	if (t instanceof InferenceVariable || typeFormalEnclosesCall(t, call)) {
    	  //System.out.println("Not replacing "+t+" for "+call);
    	  formals.remove();
    	}
      }
      
      if (!v.formals.isEmpty()) {
    	// The code below is trying to get more accurate origin info 
    	final IRNode rec = call.getReceiverOrNull();
    	ICallState callForReceiver;
    	if (MethodCall.prototype.includes(rec)) {
    		final IBinding recB = tEnv.getBinder().getIBinding( rec );
    		IRNode targs = MethodCall.getTypeArgs(rec);
    		IRNode args = MethodCall.getArgs(rec);  	  
    		IRNode rec2 = MethodCall.getObject(rec);
    		callForReceiver = new CallState(tEnv.getBinder(), rec, targs, args, recB.getReceiverType(), rec2);
    	} else {
    		callForReceiver = call; // Not quite right
    	}    	
    	
        hack = constructInitialSet(callForReceiver, v.formals, IJavaTypeSubstitution.NULL);
        call = new CallState((CallState) call, Util.subst(call.getReceiverType(), hack.getInitialVarSubst()));

        IBinding b = IBinding.Util.makeMethodBinding(m.bind, (IJavaDeclaredType) Util.subst(m.getContextType(), hack.getInitialVarSubst()), 
        		null, // TODO what should this be
        		call.getReceiverType(), tEnv);
        m = new MethodBinding(b);
      }
    }

    final BoundSet b_0;
    if (ConstructorReference.prototype.includes(call.getNode())) {
      // Special case to handle filling the type's variables, if any
      b_0 = constructInitialSet(call, getTypeFormalsForRef(m), IJavaTypeSubstitution.NULL);
    } else {
      b_0 = constructInitialSet(call, m.typeFormals, m.getSubst());
    }
    if (debug) {
      b_0.debug();
    }

    if (hack != null) {
      b_0.merge(hack);
    }

    /*
     * check if type params appear in throws clause
     * 
     * - For all i (1 <= i <= p), if P i appears in the throws clause of m ,
     * then the bound throws α i is implied. These bounds, if any, are
     * incorporated with B 0 to produce a new bound set, B 1 .
     */
    BoundSet b_1 = null;
    for (final IJavaType thrown : m.getThrownExceptions(tEnv.getBinder())) {
      final InferenceVariable var = b_0.variableMap.get(thrown);
      if (var != null) {
        if (b_1 == null) {
          b_1 = new BoundSet(b_0);
        }
        b_1.addThrown(var);
      }
    }
    if (b_1 == null) {
      b_1 = b_0;
    }

    /*
     * - A set of constraint formulas, C , is constructed as follows.
     * 
     * Let F 1 , ..., F n be the formal parameter types of m , and let e 1 ,
     * ..., e k be the actual argument expressions of the invocation. Then:
     * 
     * - To test for applicability by strict invocation:
     * 
     * If k !=  n, or if there exists an i (1 <= i <= n) such that e i is
     * pertinent to applicability (Â§15.12.2.2) and either i) e i is a
     * standalone expression of a primitive type but F i is a reference type, or
     * ii) F i is a primitive type but e i is not a standalone expression of a
     * primitive type; then the method is not applicable and there is no need to
     * proceed with inference.
     * 
     * Otherwise, C includes, for all i (1 <= i <= k) where e i is pertinent to
     * applicability, < e i -> F i θ>.
     * 
     * - To test for applicability by loose invocation:
     * 
     * If k !=  n, the method is not applicable and there is no need to proceed
     * with inference. Otherwise, C includes, for all i (1 <= i <= k) where e i
     * is pertinent to applicability, < e i -> F i θ>.
     * 
     * - To test for applicability by variable arity invocation:
     * 
     * Let F' 1 , ..., F' k be the first k variable arity parameter types of m
     * (Â§15.12.2.4). C includes, for all i (1 <= i <= k) where e i is pertinent
     * to applicability, < e i -> F' i θ>.
     */
    if (kind != InvocationKind.VARARGS && m.numFormals != call.numArgs()) {
      return null;
    }
    final BoundSet b_2 = new BoundSet(b_1);
    final IJavaTypeSubstitution theta = b_2.getInitialVarSubst();
    /*
	final String unparse = call.toString();
	if (unparse.equals("<implicit>.getVertx.sharedData.<String, String> getClusterWideMap(\"bar\", <implicit>.onSuccess(# -> #))")) {
		System.out.println("Found problematic call");
	}
	*/
    // Include the receiver
    if (call.getReceiverType() != null) {
    	// No need to check for un/boxing
        IJavaType formal_subst = Util.subst(call.getReceiverType(), theta);
        IRNode receiver = call.getReceiverOrNull();
        if (receiver == null || !mb.isPolyExpression(receiver)) {
          // TODO is there anything to do?
          //reduceTypeCompatibilityConstraints(b_2, call.getArgType(i), formal_subst);
        } else {
          final ConstraintFormula f = new ConstraintFormula(receiver, FormulaConstraint.IS_COMPATIBLE, formal_subst);
          reduceConstraintFormula(b_2, f);
        }
    }    
    final IJavaType[] formalTypes = m.getParamTypes(tEnv.getBinder(), call.numArgs(), kind == InvocationKind.VARARGS);
    for (int i = 0; i < call.numArgs(); i++) {
      final IRNode e_i = call.getArgOrNull(i);
      if (mb.isPertinentToApplicability(m, call.getNumTypeArgs() > 0, e_i)) {
        if (kind == InvocationKind.STRICT) {
          final boolean isPoly = mb.isPolyExpression(e_i);
          final IJavaType e_i_Type = isPoly ? null : (e_i == null ? call.getArgType(i) : tEnv.getBinder().getJavaType(e_i));
          if (!isPoly && e_i_Type instanceof IJavaPrimitiveType && formalTypes[i] instanceof IJavaReferenceType) {
            return null;
          }
          if (formalTypes[i] instanceof IJavaPrimitiveType && (isPoly || e_i_Type instanceof IJavaReferenceType)) {
            return null;
          }
        }
        IJavaType formal_subst = Util.subst(formalTypes[i], theta);
        if (e_i == null) {
          reduceTypeCompatibilityConstraints(b_2, call.getArgType(i), formal_subst);
        } else {
          final ConstraintFormula f = new ConstraintFormula(e_i, FormulaConstraint.IS_COMPATIBLE, formal_subst);
          reduceConstraintFormula(b_2, f);
        }
      }
    }
    /*
     * - C is reduced (Â§18.2) and the resulting bounds are incorporated with B
     * 1 to produce a new bound set, B 2 .
     * 
     * Finally, the method m is applicable if B 2 does not contain the bound
     * false and resolution of all the inference variables in B 2 succeeds
     * (Â§18.4).
     */
    final BoundSet result = resolve(b_2, null, false);
    // debug
    if (result == null || result.getInstantiations().isEmpty()) {
      /*
      if (result == null) {
    	  System.out.println("Null result from resolve()");
      }
      */
      resolve(b_2, null, true);
    }
    if (result != null && !result.isFalse && result.getInstantiations().keySet().containsAll(result.variableMap.values())) {
      b_2Cache.put(new Pair<IRNode, IRNode>(call.getNode(), m.bind.getNode()), b_2);
      return b_2;
    }
    return null;
  }

  private boolean typeFormalEnclosesCall(IJavaTypeFormal t, ICallState call) {
	final IRNode enclosingDecl = VisitUtil.getEnclosingDecl(t.getDeclaration());
	if (!TypeDeclaration.prototype.includes(enclosingDecl)) {
	  return false;
	}
	// it's from a type
	for(IRNode td : VisitUtil.getEnclosingTypes(call.getNode(), false)) {
	  if (td == enclosingDecl) {
		return true;
	  }
	}
	return false;
  }

  static class TypeFormalCollector extends IJavaType.BooleanVisitor {
    final Set<IJavaTypeFormal> formals = new HashSet<IJavaTypeFormal>();
    final boolean collectAllFormals;
    
    TypeFormalCollector(boolean collectAll) {
      collectAllFormals = collectAll;
	}

	public boolean accept(IJavaType t) {
	  if (collectAllFormals && t instanceof IJavaTypeFormal) {
		formals.add((IJavaTypeFormal) t);
	  }
	  else if (t instanceof ReboundedTypeFormal) {
        formals.add((ReboundedTypeFormal) t);
      }
      return true;
    }
    
    public IJavaTypeSubstitution getSubst(ITypeEnvironment tEnv) {
		Map<IJavaTypeFormal, IJavaType> map = new HashMap<>();
		for(IJavaTypeFormal f : formals) {
			if (f instanceof ReboundedTypeFormal) {
				ReboundedTypeFormal r = (ReboundedTypeFormal) f;
				map.put(r, r.getExtendsBound(tEnv));
			}
		}
		return TypeInference8.TypeSubstitution.create(tEnv.getBinder(), map);
    }
  }

  static IJavaType eliminateReboundedTypeFormals(final ITypeEnvironment tEnv, final IJavaType t) {
	  final TypeFormalCollector c = new TypeFormalCollector(false);
      t.visit(c);
      if (!c.formals.isEmpty()) {
    	  IJavaType temp = Util.subst(t, c.getSubst(tEnv));
    	  return temp;
      }
      return t;
  }
  
  static class ReboundedTypeFormal extends JavaTypeFormal {
    final IJavaTypeFormal source;
    final IJavaReferenceType bound;
    final ITypeEnvironment tEnv;

    ReboundedTypeFormal(ITypeEnvironment te, IJavaTypeFormal src, IJavaType b) {
      super(src.getDeclaration());
      /*
       * String unparse = src.toString(); if (unparse.contains(
       * "in java.util.List.toArray")) { System.out.println(
       * "Creating bound for "+unparse); }
       */
      tEnv = te;
      bound = (IJavaReferenceType) b;
      source = src;
    }

    @Override
    public IJavaReferenceType getExtendsBound(ITypeEnvironment tEnv) {
      return bound;
    }

    @Override
    public String toString() {
      IRNode decl = VisitUtil.getEnclosingDecl(declaration);
      return JavaNames.getTypeName(declaration) + " (extends " + bound + ") in " + JavaNames.getFullName(decl);
    }

    /**
     * FIX Is this right? Set to compare properly in maps for capture
     */
    @Override
    public int hashCode() {
      return source.hashCode() + bound.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof IJavaTypeFormal) {
        IJavaTypeFormal of = (IJavaTypeFormal) o;
        return source.getDeclaration().equals(of.getDeclaration()) && bound.equals(of.getExtendsBound(tEnv));
      }
      return false;
    }

    @Override
    public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
      if (t2 instanceof ReboundedTypeFormal) {
        ReboundedTypeFormal o = (ReboundedTypeFormal) t2;
        return source.isEqualTo(env, t2) && bound.equals(o.bound);
      }
      return false;
    }
    
    @Override
    public IJavaType subst(final IJavaTypeSubstitution s) {
    	IJavaType subbed = super.subst(s);
    	if (subbed != this && !(subbed instanceof BoundedTypeFormal)) {
    		// There's an exact substitution for this
    		return subbed;
    	}
    	// Check for substitutions involving the bound
    	IJavaType sourceSub = source.subst(s);
    	if (bound.isSubtype(tEnv, sourceSub)) {
    		return bound;
    	}
    	if (sourceSub != source) {
    		return sourceSub;
    	}
    	return this;
    }
  }

  /**
   * 18.5.2 Invocation Type Inference
   * 
   * Given a method invocation that provides no explicit type arguments, and a
   * corresponding most specific applicable generic method m , the process to
   * infer the invocation type (Â§15.12.2.6) of the chosen method is as follows:
   * 
   * - Let θ be the substitution [P 1 :=α 1 , ..., P p :=α p ] defined in
   * Â§18.5.1 to replace the type parameters of m with inference variables. -
   * Let B 2 be the bound set produced by reduction in order to demonstrate that
   * m is applicable in Â§18.5.1. (While it was necessary in Â§18.5.1 to
   * demonstrate that the inference variables in B 2 could be resolved, in order
   * to establish applicability, the instantiations produced by this resolution
   * step are not considered part of B 2 .)
   * 
   * ... see computeB_3 ... - A set of constraint formulas, C , is constructed
   * as follows ...
   * 
   * - While C is not empty, the following process is repeated, starting with
   * the bound set B 3 and accumulating new bounds into a "current" bound set,
   * ultimately producing a new bound set, B 4 :
   * 
   * ... see computeB_4
   * 
   * - Finally, if B 4 does not contain the bound false, the inference variables
   * in B 4 are resolved.
   * 
   * If resolution succeeds with instantiations T 1 , ..., T p for inference
   * variables α 1 , ..., α p , let θ' be the substitution [P 1 := T 1 , ..., P
   * p := T p ] . Then:
   * 
   * - If unchecked conversion was necessary for the method to be applicable
   * during constraint set reduction in Â§18.5.1, then the parameter types of
   * the invocation type of m are obtained by applying θ' to the parameter types
   * of m 's type, and the return type and thrown types of the invocation type
   * of m are given by the erasure of the return type and thrown types of m 's
   * type.
   * 
   * - If unchecked conversion was not necessary for the method to be
   * applicable, then the invocation type of m is obtained by applying θ' to the
   * type of m .
   * 
   * If B 4 contains the bound false, or if resolution fails, then a
   * compile-time error occurs.
   */
  IJavaFunctionType inferForInvocationType(ICallState call, MethodBinding8 m, BoundSet b_2, boolean eliminateTypeVars,
      IJavaType targetType) {
	final String unparse = null;//call.toString();
	BoundSet b_3;
	
	if (unparse != null && unparse.equals("Arrays.stream(#).map((#) -> #.create#).collect(Collectors.toList)")) {
		System.out.println("Inferring invocation type for "+unparse);
	}
	
    /*
     * - If the invocation is not a poly expression, let the bound set B 3 be
     * the same as B 2 .
     */
    if (!mb.isPolyExpression(call.getNode())) {
      b_3 = b_2;
    } else {
      /*
       * if (call.toString().startsWith("Collectors.groupingBy(")) {
       * System.out.println("Computing poly expr target"); }
       */
      final boolean computeTargetType = (targetType == null);
      if (computeTargetType) {
    	// WORKING
    	if (unparse != null && unparse.equals("Arrays.stream(#).map((#) -> #.create#).collect(Collectors.toList)")) {
    		System.out.println("Computing target type for "+unparse);
    	}
        targetType = utils.getPolyExpressionTargetType(call.getNode(), false);
        if (targetType == null) {
          targetType = utils.getPolyExpressionTargetType(call.getNode(), false);
        } else {
          final IJavaType oldTarget = targetType;
          targetType = eliminateReboundedTypeFormals(tEnv, targetType);
          if (targetType == null) {
        	  eliminateReboundedTypeFormals(tEnv, oldTarget);
          }
        }
      }
      //if ("strings.map(#:: <> parseInt).collect(<implicit>.toList)".equals(unparse)) {
      if (unparse != null && unparse.startsWith("futures.toArray(new Future[#.size#])")) {
      //if (unparse.startsWith("strings.map(")) {      
    	  System.out.println("Looking at map()");
    	  if (targetType.toString().equals("vertx.core.Future<?>[]")) {
    		  System.out.println("vertx.core.Future<?>[]");
    		  //utils.getPolyExpressionTargetType(call.getNode(), false);
    	  }
      }
      final IJavaFunctionType ftype = mb.computeMethodType(m, call);
      final IJavaType r = ftype.getReturnType();
      //WAS:
      //final IJavaType r2 = m.getReturnType(tEnv, m.mkind == Kind.CONSTRUCTOR || m.bind.getContextType().isRawType(tEnv));

      b_3 = computeB_3(call, r, b_2, targetType);

      if (b_3.isFalse) {
        if (computeTargetType) {
          utils.getPolyExpressionTargetType(call.getNode());
        }
        //b_2.debug();
        b_3 = computeB_3(call, r, b_2, targetType);
        return null;
      }
    }
    if (unparse != null && unparse.equals("Arrays.stream(#).map((#) -> #.create#).collect(Collectors.toList)")) {
      System.out.println("About to create constraints");
	  if (targetType != null && targetType.toString().equals("java.util.stream.Stream<testVertx.Buffer>")) {
		  System.out.println("Found bad target type");
      }
    }
    final Set<ConstraintFormula> c = createInitialConstraints(call, m, b_3);
    final Set<ConstraintFormula> c2 = new HashSet<>(c);
    if (unparse != null && unparse.equals("crlPaths.stream.map((#) -> #.getAbsolutePath#).map(#.fileSystem:: <> readFileBlocking)")) {
        System.out.println("About to compute B4");
    }
    final BoundSet b_4 = computeB_4(b_3, c);
    if (b_4 == null) {
    	computeB_4(b_3, c2);
    }
    final IJavaFunctionType origType = mb.computeMethodType(m);
    BoundSet result = resolve(b_4, null, false);
    if (result == null) {
    	result = resolve(b_4, null, true);
    }
    final IJavaTypeSubstitution theta_prime = result/* b_4 */.getFinalTypeSubst(eliminateTypeVars, false);
    if (b_4.usedUncheckedConversion()) {
      return mb.substParams_eraseReturn(origType, theta_prime);
    } else {
      return origType.instantiate(origType.getTypeFormals(), theta_prime);
    }
  }

  /**
   * @param t
   *          the target type
   */
  private BoundSet computeB_3(ICallState call, final IJavaType r, BoundSet b_2, final IJavaType targetType) {
    /*
     * If the invocation is a poly expression, let the bound set B 3 be derived
     * from B 2 as follows. Let R be the return type of m , let T be the
     * invocation's target type, and then:
     * 
     * - If unchecked conversion was necessary for the method to be applicable
     * during constraint set reduction in Â§18.5.1, the constraint formula <| R
     * | -> T > is reduced and incorporated with B 2 .
     */
    final BoundSet b_3 = new BoundSet(b_2);
    final IJavaTypeSubstitution theta = b_3.getInitialVarSubst();
    // HACK to substitute variables used by b_3
    final IJavaType t = targetType.subst(theta);
    if (b_3.usedUncheckedConversion) {
      reduceConstraintFormula(b_3, new ConstraintFormula(tEnv.computeErasure(r), FormulaConstraint.IS_COMPATIBLE, t));
      return b_3;
    }
    /*
     * - Otherwise, if R θ is a parameterized type, G<A 1 , ..., A n > , and one
     * of A 1 , ..., A n is a wildcard, then, for fresh inference variables Î² 1
     * , ..., Î² n , the constraint formula < G< Î² 1 , ..., Î² n > -> T > is
     * reduced and incorporated, along with the bound G< Î² 1 , ..., Î² n > =
     * capture( G<A 1 , ..., A n > ), with B 2 .
     */
    final IJavaType r_subst = Util.subst(r, theta);
    final IJavaDeclaredType g = isWildcardParameterizedType(r_subst);
    if (g != null) {
      final int n = g.getTypeParameters().size();
      final List<InferenceVariable> newVars = new ArrayList<InferenceVariable>(n);
      for (int i = 0; i < n; i++) {
        newVars.add(new InferenceVariable(null, call)); // TODO
      }
      // TODO subst?
      final IJavaDeclaredType g_beta = JavaTypeFactory.getDeclaredType(g.getDeclaration(), newVars, g.getOuterType());
      // Connects the new inference variables to the return type
      b_3.addCaptureBound(g_beta, g);
      
      // Connects the new inference variables to the target type
      reduceConstraintFormula(b_3, new ConstraintFormula(g_beta, FormulaConstraint.IS_COMPATIBLE, t));

      // final BoundSet temp = new BoundSet(b_3);
      b_3.addInferenceVariables(newVars);
      
      BoundSet b_3_hack = checkIfHackIsRequiredToB3(b_3, g_beta, g);
      if (b_3_hack != null) {
    	  return b_3_hack;
      }
      return b_3;
    }
    /*
     * - Otherwise, if R θ is an inference variable α, and one of the following
     * is true:
     * 
     * ... see below
     * 
     * then α is resolved in B 2 , and where the capture of the resulting
     * instantiation of α is U , the constraint formula < U -> T > is reduced
     * and incorporated with B 2 .
     */
    if (r_subst instanceof InferenceVariable) {
      final InferenceVariable alpha = (InferenceVariable) r_subst;
      final BoundSubset bounds = b_3.findAssociatedBounds(alpha);
      final IJavaDeclaredType g2;
      BoundCondition cond = null;

      if (t instanceof IJavaReferenceType && isWildcardParameterizedType(t) == null) {
        /*
         * > T is a reference type, but is not a wildcard-parameterized type,
         * and either i) B 2 contains a bound of one of the forms α = S or S <:
         * α, where S is a wildcard-parameterized type, or ii) B 2 contains two
         * bounds of the forms S 1 <: α and S 2 <: α, where S 1 and S 2 have
         * supertypes that are two different parameterizations of the same
         * generic class or interface.
         */
        cond = new BoundCondition() {
          public boolean examineEquality(Equality e) {
            for (IJavaType t : e.values) {
              if (isWildcardParameterizedType(t) != null) {
                return true;
              }
            }
            return false;
          }

          public boolean examineLowerBound(IJavaType t) {
            return false;
          }

          public boolean examineUpperBound(IJavaType t) {
            return isWildcardParameterizedType(t) != null;
            // TODO check for diff parameterizations?
          }
        };
      } else if ((g2 = isParameterizedType(t)) != null) {
        /*
         * > T is a parameterization of a generic class or interface, G , and B
         * 2 contains a bound of one of the forms α = S or S <: α, where there
         * exists no type of the form G< ... > that is a supertype of S , but
         * the raw type | G< ... > | is a supertype of S .
         */
        cond = new BoundCondition() {
          public boolean examineEquality(Equality e) {
            if (e.vars.contains(alpha)) {
              for (IJavaType t : e.values) {
                if (onlyHasRawG_asSuperType(g2.getDeclaration(), t)) {
                  return true;
                }
              }
            }
            return false;
          }

          public boolean examineLowerBound(IJavaType t) {
            return false;
          }

          public boolean examineUpperBound(IJavaType t) {
            return onlyHasRawG_asSuperType(g2.getDeclaration(), t);
          }
        };
      } else if (t instanceof IJavaPrimitiveType) {
        /*
         * > T is a primitive type, and one of the primitive wrapper classes
         * mentioned in Â§5.1.7 is an instantiation, upper bound, or lower bound
         * for α in B 2 .
         */
        final IJavaPrimitiveType pt = (IJavaPrimitiveType) t;
        final IJavaDeclaredType wrapper = JavaTypeFactory.getCorrespondingDeclType(tEnv, pt);
        cond = new BoundCondition() {
          public boolean examineEquality(Equality e) {
            return e.values.contains(wrapper);
          }

          public boolean examineLowerBound(IJavaType t) {
            return t == wrapper;
          }

          public boolean examineUpperBound(IJavaType t) {
            return t == wrapper;
          }
        };
      }
      /* then α is resolved in B 2 , and where the capture of the resulting
       * instantiation of α is U , the constraint formula < U -> T > is reduced
       * and incorporated with B 2 .
       */
      if (cond != null && bounds.examine(cond)) {
    	BoundSet temp = resolve(b_2, Collections.singleton(alpha), false);
        IJavaType u = temp.getInstantiations().get(alpha);
        reduceConstraintFormula(b_3, new ConstraintFormula(u, FormulaConstraint.IS_COMPATIBLE, t));
        return b_3;
      }
    }
    /*
     * - Otherwise, the constraint formula < R θ -> T > is reduced and
     * incorporated with B 2 .
     */
    /*
    if (call.toString().equals("ImmutableList.of(Multisets.immutableEntry(#.checkNotNull#, 1))")) {
    	System.out.println("Found all");
    }
    */
    final BoundSet b_3_before = new BoundSet(b_3);
    reduceConstraintFormula(b_3, new ConstraintFormula(r_subst, FormulaConstraint.IS_COMPATIBLE, t));
    return b_3;
  }

  /**
   * Checks for 
   * @param g The return type of the method in question, substituted
   * @param g_beta g, but replaced with inference variables    
   */
  private BoundSet checkIfHackIsRequiredToB3(BoundSet orig, IJavaDeclaredType g_beta, IJavaDeclaredType g) {
	final BoundSet rv = new BoundSet(orig);
	boolean hacked = false;
	
	final int num = g_beta.getTypeParameters().size();
	for(int i=0; i<num; i++) {
		final IJavaType a_i = g.getTypeParameters().get(i);
		if (a_i instanceof IJavaWildcardType) {
			final IJavaWildcardType w_i = (IJavaWildcardType) a_i;
			if (w_i.getUpperBound() != null) {
				final Set<InferenceVariable> vars = new HashSet<>(); 
				getReferencedInferenceVariables(vars, w_i.getUpperBound());
				// TODO save a copy of vars
				
				if (!vars.isEmpty()) {
					// Check for connection between alpha_i and vars among the bounds involved alpha_i		
					final InferenceVariable alpha_i = (InferenceVariable) g_beta.getTypeParameters().get(i);
					final BoundSubset related = rv.findAssociatedBounds(alpha_i);	
					final Set<InferenceVariable> relatedVars = new HashSet<>();
					related.examine(new BoundCondition() {											
						public boolean examineUpperBound(IJavaType t) {
							getReferencedInferenceVariables(relatedVars, t);
							return false;
						}
						public boolean examineLowerBound(IJavaType t) {
							getReferencedInferenceVariables(relatedVars, t);
							return false;
						}
						public boolean examineEquality(Equality e) {
							relatedVars.addAll(e.vars);
							for(IJavaReferenceType t : e.values) {
								getReferencedInferenceVariables(relatedVars, t);
							}
							return false;
						}
					});
					vars.removeAll(relatedVars);
					if (!vars.isEmpty()) {
						// No connection found, so add constraint
						reduceConstraintFormula(rv, new ConstraintFormula(alpha_i, FormulaConstraint.IS_SUBTYPE, w_i.getUpperBound()));
						hacked = true;
					}
				}
			}
		}
	}
	if (hacked) {
		return rv;
	}
	return null;
  }

/**
   * From 18.5.2 Invocation Type Inference:
   * 
   * Let e 1 , ..., e k be the actual argument expressions of the invocation. If
   * m is applicable by strict or loose invocation, let F 1 , ..., F k be the
   * formal parameter types of m ; if m is applicable by variable arity
   * invocation, let F 1 , ..., F k the first k variable arity parameter types
   * of m (Â§15.12.2.4). Then:
   * 
   * - For all i (1 <= i <= k), if e i is not pertinent to applicability, C
   * contains < e i -> F i θ>.
   */
  private Set<ConstraintFormula> createInitialConstraints(ICallState call, MethodBinding8 m, BoundSet b_3) {
    final Set<ConstraintFormula> rv = new HashSet<ConstraintFormula>();
    final IJavaTypeSubstitution theta = b_3.getInitialVarSubst();
    final Set<InferenceVariable> preferredVars = b_3.getInitialInferenceVars();
	// Add constraints for the receiver (assumed to be pertinent to applicability?)
    final IRNode receiver = call.getReceiverOrNull();
    if (receiver != null) {
      final IJavaType recType = call.getReceiverType();
      final IJavaType f_subst;
      // TODO are there other special cases?
      if (customizeMBForBoundsSet) {
    	  f_subst = recType;
      }
      else if (receiverBindsToSameGenericMethod(call.getNode(), m, call.getReceiverOrNull())) {
    	  // Remove type formals declared in m, since they should be substituted later
    	  Map<IJavaTypeFormal, InferenceVariable> map = b_3.getInitialVarMap();    	  
    	  for(IRNode tf : JJNode.tree.children(m.typeFormals)) {
    		  map.remove(JavaTypeFactory.getTypeFormal(tf));
    	  }
          f_subst = map.isEmpty() ? recType : Util.subst(recType, TypeSubstitution.create(tEnv.getBinder(), map));
      } else {
    	  f_subst = Util.subst(recType, theta);
      }
      if (!mb.isPertinentToApplicability(m, call.getNumTypeArgs() > 0, receiver)) {
    	rv.add(new ConstraintFormula(receiver, FormulaConstraint.IS_COMPATIBLE, f_subst));
      }
      createAdditionalConstraints(rv, f_subst, receiver, preferredVars);  
    }
    
    final IJavaType[] formalTypes = m.getParamTypes(tEnv.getBinder(), call.numArgs(),
        m.getInvocationKind() == InvocationKind.VARARGS, false);
    for (int i = 0; i < call.numArgs(); i++) {
      final IRNode e_i = call.getArgOrNull(i);
      final IJavaType f_subst = Util.subst(formalTypes[i], theta);
      if (!mb.isPertinentToApplicability(m, call.getNumTypeArgs() > 0, e_i)) {
        rv.add(new ConstraintFormula(e_i, FormulaConstraint.IS_COMPATIBLE, f_subst));
      }
      createAdditionalConstraints(rv, f_subst, e_i, preferredVars);
    }
    return rv;
  }

  private boolean receiverBindsToSameGenericMethod(IRNode call, MethodBinding8 m, IRNode receiver) {
	if (receiver == null || !m.isGeneric()) {
	  return false;
	}
	final Operator callOp = JJNode.tree.getOperator(call);
	final Operator recOp = JJNode.tree.getOperator(receiver);
	if (callOp != recOp) {
	  return false;
	}
 	final IBinding b = tEnv.getBinder().getIBinding(receiver);
	return b.getNode() == m.getNode();
  }

  /**
   * - For all i (1 <= i <= k), additional constraints may be included,
   * depending on the form of e i :
   * 
   * > If e i is a LambdaExpression, C contains <LambdaExpression -> throws F i
   * θ>.
   * 
   * > If e i is a MethodReference, C contains <MethodReference -> throws F i
   * θ>.
   * 
   * > If e i is a poly class instance creation expression (Â§15.9) or a poly
   * method invocation expression (Â§15.12), C contains all the constraint
   * formulas that would appear in the set C generated by Â§18.5.2 when
   * inferring the poly expression's invocation type.
   * 
   * > If e i is a parenthesized expression, these rules are applied recursively
   * to the contained expression.
   * 
   * > If e i is a conditional expression, these rules are applied recursively
   * to the second and third operands.
   * 
   * @param preferredVars Inference variables to use if possible
   */
  private void createAdditionalConstraints(Set<ConstraintFormula> c, IJavaType f_subst, IRNode e_i, Set<InferenceVariable> preferredVars) {
    final Operator op = JJNode.tree.getOperator(e_i);
    if (LambdaExpression.prototype.includes(op)) {
      c.add(new ConstraintFormula(e_i, FormulaConstraint.THROWS, f_subst));
    } else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
      c.add(new ConstraintFormula(e_i, FormulaConstraint.THROWS, f_subst));
    } else if (MethodCall.prototype.includes(op) || NewExpression.prototype.includes(op)) {
      if (mb.isPolyExpression(e_i)) {
        try {
          Triple<CallState, MethodBinding8, BoundSet> result = computeInvocationBounds((CallInterface) op, e_i, f_subst);
          final BoundSet b_3 = result.third();
          // Change to use my preferred variables
          final BoundSet b_3_subst = b_3.substPreferredVars(preferredVars);
          c.addAll(createInitialConstraints(result.first(), result.second(), b_3_subst));
        } catch (NoArgs e1) {
          throw new IllegalStateException("No arguments for " + DebugUnparser.toString(e_i));
        }
      }
    } else if (ParenExpression.prototype.includes(op)) {
      createAdditionalConstraints(c, f_subst, ParenExpression.getOp(e_i), preferredVars);
    } else if (ConditionalExpression.prototype.includes(op)) {
      createAdditionalConstraints(c, f_subst, ConditionalExpression.getIftrue(e_i), preferredVars);
      createAdditionalConstraints(c, f_subst, ConditionalExpression.getIffalse(e_i), preferredVars);
    }
  }

  /**
   * - While C is not empty, the following process is repeated, starting with
   * the bound set B 3 and accumulating new bounds into a "current" bound set,
   * ultimately producing a new bound set, B 4 :
   */
  private BoundSet computeB_4(final BoundSet b_3, final Set<ConstraintFormula> c) {
    final Map<ConstraintFormula, InputOutputVars> io = precomputeIO(c);
    BoundSet current = new BoundSet(b_3);
    BoundSet last = b_3;
    while (!c.isEmpty()) {
      final Set<ConstraintFormula> selected = doComputeB_4(c, io, current);
      if (!current.contains(last)) {
        throw new IllegalStateException();
      }
      if (current.isFalse) {
        BoundSet temp = new BoundSet(last == null ? b_3 : last);
        temp.debug();
        doComputeB_4(c, io, temp);
        return null;
      }
      c.removeAll(selected);
      last = current;
      current = new BoundSet(current);
    }
    return current;
  }

  /**
   * 1. A subset of constraints is selected in C , satisfying the property that,
   * for each constraint, no input variable depends on the resolution (Â§18.4)
   * of an output variable of another constraint in C . (input variable and
   * output variable are defined below.)
   * 
   * (see below)
   * 
   * 2. The selected constraint(s) are removed from C .
   * 
   * 3. The input variables α 1 , ..., α m of all the selected constraint(s) are
   * resolved.
   * 
   * 4. Where T 1 , ..., T m are the instantiations of α 1 , ..., α m , the
   * substitution [ α 1 := T 1 , ..., α m := T m ] is applied to every
   * constraint.
   * 
   * 5. The constraint(s) resulting from substitution are reduced and
   * incorporated with the current bound set.
   */
  private Set<ConstraintFormula> doComputeB_4(final Set<ConstraintFormula> c, final Map<ConstraintFormula, InputOutputVars> io,
      BoundSet current) {
    // Step 2
    ConstraintDependencies deps = new ConstraintDependencies();
    deps.populate(c, io);

    final Set<ConstraintFormula> selected = deps.chooseUninstantiated(c);
    if (selected.isEmpty()) {
      deps.populate(c, io);
      deps.chooseUninstantiated(c);
      throw new IllegalStateException();
    }
    // Hoisted out to computeB_4
    // c.removeAll(selected);

    // Step 3: resolve
    final Set<InferenceVariable> toResolve = collectInputVars(io, selected);
    BoundSet next = resolve(current, toResolve, false);
    if (next == null) {
      next = resolve(current, toResolve, true);
      throw new IllegalStateException();
    }

    // Step 4: apply instantiations
    // Step 5: reduce and incorporate into current
    final Map<InferenceVariable, IJavaType> allInstantiations = next.getInstantiations();
    /* Why only selected? */
    final Map<InferenceVariable, IJavaType> selectedInstantiations = new HashMap<InferenceVariable, IJavaType>(toResolve.size());
    for (InferenceVariable v : toResolve) {
      selectedInstantiations.put(v, allInstantiations.get(v));
    }
    /**/
    final IJavaTypeSubstitution subst = TypeSubstitution.create(tEnv.getBinder(), selectedInstantiations);
    for (ConstraintFormula f : selected) {
      ConstraintFormula f_prime = f.subst(subst);
      reduceConstraintFormula(current, f_prime);
    }
    return selected;
  }

  private Map<ConstraintFormula, InputOutputVars> precomputeIO(Set<ConstraintFormula> c) {
    Map<ConstraintFormula, InputOutputVars> rv = new HashMap<ConstraintFormula, InputOutputVars>(c.size());
    for (ConstraintFormula f : c) {
      InputOutputVars io = new InputOutputVars(f);
      computeInputOutput(io, f);
      rv.put(f, io);
    }
    return rv;
  }

  private Set<InferenceVariable> collectInputVars(Map<ConstraintFormula, InputOutputVars> io, Set<ConstraintFormula> selected) {
    Set<InferenceVariable> inputs = new HashSet<InferenceVariable>();
    for (ConstraintFormula f : selected) {
      InputOutputVars vars = io.get(f);
      inputs.addAll(vars.input);
    }
    return inputs;
  }

  /**
   * 1. A subset of constraints is selected in C , satisfying the property that,
   * for each constraint, no input variable depends on the resolution (Â§18.4)
   * of an output variable of another constraint in C . (input variable and
   * output variable are defined below.)
   * 
   * If this subset is empty, then there is a cycle (or cycles) in the graph of
   * dependencies between constraints. In this case, all constraints are
   * considered that participate in a dependency cycle (or cycles) and do not
   * depend on any constraints outside of the cycle (or cycles). A single
   * constraint is selected from the considered constraints, as follows:
   * 
   * (see below)
   */
  class ConstraintDependencies extends Dependencies<ConstraintFormula> {
	ConstraintDependencies() {
		super(false);
	}
	  
    void populate(Set<ConstraintFormula> c, Map<ConstraintFormula, InputOutputVars> io) {
      // Precompute output mappings
      Multimap<InferenceVariable, ConstraintFormula> outputForFormulas = ArrayListMultimap.create();
      for (ConstraintFormula f : c) {
        InputOutputVars vars = io.get(f);
        for (InferenceVariable v : vars.output) {
          outputForFormulas.put(v, f);
        }
      }
      // Create dependencies
      for (ConstraintFormula f : c) {
        InputOutputVars vars = io.get(f);
        for (InferenceVariable v : vars.input) {
          Collection<ConstraintFormula> formulas = outputForFormulas.get(v);
          if (formulas != null) {
            for (ConstraintFormula fo : formulas) {
              markDependsOn(f, fo);
            }
          }
        }
        markDependsOn(f, f);
      }
    }

    /**
     * - If any of the considered constraints have the form <Expression -> T >,
     * then the selected constraint is the considered constraint of this form
     * that contains the expression to the left (Â§3.5) of the expression of
     * every other considered constraint of this form.
     * 
     * - If no considered constraint has the form <Expression -> T >, then the
     * selected constraint is the considered constraint that contains the
     * expression to the left of the expression of every other considered
     * constraint.
     */
    @Override
    public Set<ConstraintFormula> chooseUninstantiated(final Set<ConstraintFormula> remaining) {
      Set<ConstraintFormula> considered = super.chooseUninstantiated(remaining);
      if (considered.size() <= 1) {
        return considered;
      }
      // Otherwise, there's a cycle of dependencies? TODO
      ConstraintFormula expression_T = null;
      for (ConstraintFormula f : considered) {
        if (f.expr != null && f.constraint == FormulaConstraint.IS_COMPATIBLE) {
          if (expression_T == null) {
            expression_T = f;
          } else if (isToTheLeftOf(f.expr, expression_T.expr)) {
            expression_T = f;
          }
        }
      }
      // TODO is this right?
      return considered;
    }

    private boolean isToTheLeftOf(IRNode e1, IRNode e2) {
      IJavaRef r1 = JavaNode.getJavaRef(e1);
      IJavaRef r2 = JavaNode.getJavaRef(e2);
      if (r1.getOffset() < r2.getOffset()) {
        // TODO what if r1 contains r2?
        return true;
      }
      if (r1.getOffset() == r2.getOffset()) {
        return r1.getLength() < r2.getLength();
      }
      return false;
    }

  }

  /*
   * @return true if there exists no type of the form G< ... > that is a
   * supertype of S , but the raw type | G< ... > | is a supertype of S .
   */
  boolean onlyHasRawG_asSuperType(IRNode g_decl, IJavaType t) {
	Boolean rv = onlyHasRawG_asSuperType(g_decl, false, t);
	if (rv == null) {
		return false;
	}
	return rv.booleanValue();
  }

  private Boolean onlyHasRawG_asSuperType(IRNode g_decl, boolean foundRawG, IJavaType t) {
    if (t instanceof IJavaDeclaredType) {
      IJavaDeclaredType dt = (IJavaDeclaredType) t;
      if (g_decl.equals(dt.getDeclaration())) {
        if (dt.isRawType(tEnv)) {
          foundRawG = true;
        } else {
          return Boolean.FALSE;
        }
      }
    }
    Boolean result = null;
    for (IJavaType st : t.getSupertypes(tEnv)) {
      Boolean temp = onlyHasRawG_asSuperType(g_decl, foundRawG, st);
      if (temp == Boolean.FALSE) {
        return Boolean.FALSE; // Immediate fail
      }
      if (result == null) {
        result = temp;
      }
      // otherwise result is TRUE, and temp is null or TRUE
    }
    if (result == null && foundRawG) {
      return Boolean.TRUE;
    }
    return result;
  }

  private IJavaDeclaredType isParameterizedType(IJavaType t) {
    if (t instanceof IJavaDeclaredType) {
      final IJavaDeclaredType g = (IJavaDeclaredType) t;
      if (!g.getTypeParameters().isEmpty()) {
        return g;
      }
    }
    return null;
  }

  IJavaDeclaredType isWildcardParameterizedType(IJavaType t) {
    final IJavaDeclaredType g = isParameterizedType(t);
    if (g != null) {
      for (IJavaType p : g.getTypeParameters()) {
        if (p instanceof IJavaWildcardType) {
          return g;
        }
      }
    }
    return null;
  }

  static class InputOutputVars {
    final ConstraintFormula f;
    final Set<InferenceVariable> input = new HashSet<InferenceVariable>();
    final Set<InferenceVariable> output = new HashSet<InferenceVariable>();

    InputOutputVars(ConstraintFormula f) {
      this.f = f;
    }

    @Override
    public String toString() {
      return f + " : " + input + " --> " + output;
    }
  }

  /**
   * Invocation type inference may require carefully sequencing the reduction of
   * constraint formulas of the forms <Expression -> T >, <LambdaExpression ->
   * throws T >, and <MethodReference -> throws T >. To facilitate this
   * sequencing, the input variables of these constraints are defined as
   * follows:
   * 
   * - For <LambdaExpression -> T >: - If T is an inference variable, it is the
   * (only) input variable.
   * 
   * - If T is a functional interface type, and a function type can be derived
   * from T (Â§15.27.3), then the input variables include i) if the lambda
   * expression is implicitly typed, the inference variables mentioned by the
   * function type's parameter types; and ii) if the function type's return
   * type, R , is not void , then for each result expression e in the lambda
   * body (or for the body itself if it is an expression), the input variables
   * of < e -> R >.
   * 
   * - Otherwise, there are no input variables.
   * 
   * - For <MethodReference -> T >: - If T is an inference variable, it is the
   * (only) input variable. - If T is a functional interface type with a
   * function type, and if the method reference is inexact (Â§15.13.1), the
   * input variables are the inference variables mentioned by the function
   * type's parameter types. - Otherwise, there are no input variables.
   * 
   * - For <Expression -> T >, if Expression is a parenthesized expression:
   * Where the contained expression of Expression is Expression', the input
   * variables are the input variables of <Expression' -> T >.
   * 
   * - For <ConditionalExpression -> T >: Where the conditional expression has
   * the form e 1 ? e 2 : e 3 , the input variables are the input variables of <
   * e 2 -> T > and < e 3 -> T >.
   * 
   * - For all other constraint formulas, there are no input variables.
   * 
   * The output variables of these constraints are all inference variables
   * mentioned by the type on the right-hand side of the constraint, T , that
   * are not input variables.
   */
  void computeInputOutput(InputOutputVars vars, ConstraintFormula f) {
    getReferencedInferenceVariables(vars.output, f.type);
    computeInputVars(vars.input, f);
    vars.output.removeAll(vars.input);
  }

  private void computeInputVars(Set<InferenceVariable> vars, ConstraintFormula f) {
    switch (f.constraint) {
    case IS_COMPATIBLE:
      if (f.expr != null) {
        final Operator op = JJNode.tree.getOperator(f.expr);
        if (LambdaExpression.prototype.includes(op)) {
          if (f.type instanceof InferenceVariable) {
            vars.add((InferenceVariable) f.type);
          }
          final IJavaFunctionType funcType = tEnv.isFunctionalType(f.type);
          if (funcType != null) {
            if (MethodBinder8.isImplicitlyTypedLambda(f.expr)) {
              for (IJavaType pt : funcType.getParameterTypes()) {
                getReferencedInferenceVariables(vars, pt);
              }
            }
            if (funcType.getReturnType() != JavaTypeFactory.voidType) {
              IRNode body = LambdaExpression.getBody(f.expr);
              for (IRNode e : findResultExprs(body)) {
                computeInputVars(vars, new ConstraintFormula(e, FormulaConstraint.IS_COMPATIBLE, f.type));
              }
            }
          }
        } else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
          if (f.type instanceof InferenceVariable) {
            vars.add((InferenceVariable) f.type);
          }
          final IJavaFunctionType funcType = tEnv.isFunctionalType(f.type);
          if (funcType != null) {
            if (true || !mb.isExactMethodReference(f.expr)) { // TODO?
              for (IJavaType pt : funcType.getParameterTypes()) {
                getReferencedInferenceVariables(vars, pt);
              }
            }
          }
        } else if (ParenExpression.prototype.includes(op)) {
          computeInputVars(vars, new ConstraintFormula(ParenExpression.getOp(f.expr), FormulaConstraint.IS_COMPATIBLE, f.type));
        } else if (ConditionalExpression.prototype.includes(op)) {
          computeInputVars(vars,
              new ConstraintFormula(ConditionalExpression.getIftrue(f.expr), FormulaConstraint.IS_COMPATIBLE, f.type));
          computeInputVars(vars,
              new ConstraintFormula(ConditionalExpression.getIffalse(f.expr), FormulaConstraint.IS_COMPATIBLE, f.type));
        }
      }
      break;
    case THROWS:
      /*
       * - For <LambdaExpression -> throws T >: - If T is an inference variable,
       * it is the (only) input variable. - If T is a functional interface type,
       * and a function type can be derived, as described in Â§15.27.3, the
       * input variables include i) if the lambda expression is implicitly
       * typed, the inference variables mentioned by the function type's
       * parameter types; and ii) the inference variables mentioned by the
       * function type's return type. - Otherwise, there are no input variables.
       * 
       * - For <MethodReference -> throws T >: - If T is an inference variable,
       * it is the (only) input variable. - If T is a functional interface type
       * with a function type, and if the method reference is inexact
       * (Â§15.13.1), the input variables are the inference variables mentioned
       * by the function type's parameter types and the function type's return
       * type. - Otherwise, there are no input variables.
       *
       */
      if (f.type instanceof InferenceVariable) {
        vars.add((InferenceVariable) f.type);
      }
      final IJavaFunctionType funcType = tEnv.isFunctionalType(f.type);
      if (funcType != null) {
        final Operator op = JJNode.tree.getOperator(f.expr);
        if (LambdaExpression.prototype.includes(op)) {
          if (MethodBinder8.isImplicitlyTypedLambda(f.expr)) {
            for (IJavaType pt : funcType.getParameterTypes()) {
              getReferencedInferenceVariables(vars, pt);
            }
          }
          getReferencedInferenceVariables(vars, funcType.getReturnType());
        } else if (true || !mb.isExactMethodReference(f.expr)) {
          getReferencedInferenceVariables(vars, funcType.getReturnType());
          for (IJavaType pt : funcType.getParameterTypes()) {
            getReferencedInferenceVariables(vars, pt);
          }
        }
      }
    default:
    }
  }

  static Iterable<IRNode> findResultExprs(IRNode lambdaBody) {
    if (Expression.prototype.includes(lambdaBody)) {
      return new SingletonIterator<IRNode>(lambdaBody);
    }
    List<IRNode> rv = new ArrayList<IRNode>();
    for (IRNode n : JJNode.tree.topDown(lambdaBody)) {
      if (ReturnStatement.prototype.includes(n)) {
        rv.add(ReturnStatement.getValue(n));
      }
    }
    return rv;
  }

  /**
   * 18.5.3 Functional Interface Parameterization Inference
   * 
   * Where a lambda expression with explicit parameter types P 1 , ..., P n
   * targets a functional interface type F<A 1 , ..., A m > with at least one
   * wildcard type argument, then a parameterization of F may be derived as the
   * ground target type of the lambda expression as follows.
   * 
   * Let Q 1 , ..., Q k be the parameter types of the function type of the type
   * F< α 1 , ..., α m > , where α 1 , ..., α m are fresh inference variables.
   * 
   * If n !=  k, no valid parameterization exists. Otherwise, a set of
   * constraint formulas is formed with, for all i (1 <= i <= n), < P i = Q i >.
   * This constraint formula set is reduced to form the bound set B .
   * 
   * If B contains the bound false, no valid parameterization exists. Otherwise,
   * a new parameterization of the functional interface type, F<A' 1 , ..., A' m
   * > , is constructed as follows, for 1 <= i <= m:
   * 
   * - If B contains an instantiation for α i , T , then A' i = T . - Otherwise,
   * A' i = A i .
   * 
   * If F<A' 1 , ..., A' m > is not a well-formed type (that is, the type
   * arguments are not within their bounds), or if F<A' 1 , ..., A' m > is not a
   * subtype of F<A 1 , ..., A m >, no valid parameterization exists. Otherwise,
   * the inferred parameterization is either F<A' 1 , ..., A' m > , if all the
   * type arguments are types, or the non-wildcard parameterization (Â§9.9) of
   * F<A' 1 , ..., A' m > , if one or more type arguments are still wildcards.
   */
  IJavaType inferForFunctionalInterfaceParameterization(IJavaType targetType, IRNode lambda, boolean checkForSubtype) {
    final IJavaDeclaredType f = isWildcardParameterizedType(targetType);
    if (f == null) {
      return targetType; // Nothing to infer
    }
    // final IJavaFunctionType fType = tEnv.isFunctionalType(f);
    // final int k = fType.getParameterTypes().size();
    final int m = f.getTypeParameters().size();
    final List<InferenceVariable> newVars = new ArrayList<InferenceVariable>(m);
    for (int i = 0; i < m; i++) {
      newVars.add(new InferenceVariable(null, f)); // TODO
    }
    // TODO subst?
    final IJavaDeclaredType f_alpha = JavaTypeFactory.getDeclaredType(f.getDeclaration(), newVars, f.getOuterType());
    IJavaFunctionType funcType = tEnv.isFunctionalType(f_alpha);
    IRNode lambdaParams = LambdaExpression.getParams(lambda);
    final int n = JJNode.tree.numChildren(lambdaParams);
    if (funcType.getParameterTypes().size() != n) {
      return null; // No valid parameterization
    }
    final BoundSet b = new BoundSet(null, getTypeFormalList(InterfaceDeclaration.getTypes(f.getDeclaration())),
        newVars.toArray(new InferenceVariable[newVars.size()]));
    int i = 0;
    for (IRNode paramD : Parameters.getFormalIterator(lambdaParams)) {
      IJavaType paramT = tEnv.getBinder().getJavaType(ParameterDeclaration.getType(paramD));
      reduceConstraintFormula(b, new ConstraintFormula(paramT, FormulaConstraint.IS_SAME, funcType.getParameterTypes().get(i)));
      i++;
    }
    if (b.isFalse) {
      return null; // No valid parameterization
    }
    final BoundSet result = resolve(b, null, false);
    List<IJavaType> a_prime = new ArrayList<IJavaType>(f.getTypeParameters().size());
    for (i = 0; i < f.getTypeParameters().size(); i++) {
      IJavaType t = result.getInstantiations().get(f_alpha.getTypeParameters().get(i));
      a_prime.add(t != null ? t : f.getTypeParameters().get(i));
    }
    IJavaDeclaredType f_prime = JavaTypeFactory.getDeclaredType(f.getDeclaration(), a_prime, f.getOuterType());
    // TODO check if well formed
    if (checkForSubtype && !f_prime.isSubtype(tEnv, f)) {
      // f_prime.isSubtype(tEnv, f);
      return null; // No valid parameterization
    }
    if (isWildcardParameterizedType(f_prime) != null) {
      return computeNonWildcardParameterization(f_prime);
    }
    return f_prime;
  }

  private List<IJavaTypeFormal> getTypeFormalList(IRNode types) {
	if (types == null) {
	  return Collections.emptyList();
	}
    final int n = JJNode.tree.numChildren(types);
    if (n == 0) {
      return Collections.emptyList();
    }
    List<IJavaTypeFormal> rv = new ArrayList<IJavaTypeFormal>(n);
    for (IRNode tf : TypeFormals.getTypeIterator(types)) {
      rv.add(JavaTypeFactory.getTypeFormal(tf));
    }
    return rv;
  }

  /**
   * JLS 8 sec 9.9
   * 
   * • The function type of a parameterized functional interface type I
   * <A1...An>, where one or more of A1...An is a wildcard, is the function type
   * of the non- wildcard parameterization of I, I<T1...Tn>. The non-wildcard
   * parameterization is determined as follows.
   * 
   * Let P1...Pn be the type parameters of I with corresponding bounds B1...Bn.
   * For all i (1 ≤ i ≤ n), Ti is derived according to the form of Ai:
   * 
   * – If Ai is a type, then Ti = Ai. – If Ai is a wildcard, and the
   * corresponding type parameter's bound, Bi, mentions one of P1...Pn, then Ti
   * is undefined and there is no function type. – Otherwise: › If Ai is an
   * unbound wildcard ?, then Ti = Bi. › If Ai is a upper-bounded wildcard ?
   * extends Ui, then Ti = glb(Ui, Bi) (§5.1.10). › If Ai is a lower-bounded
   * wildcard ? super Li, then Ti = Li.
   */
  IJavaType computeNonWildcardParameterization(IJavaDeclaredType iface) {
    final int n = iface.getTypeParameters().size();
    final List<IJavaType> t = new ArrayList<IJavaType>(n);
    final List<IJavaTypeFormal> params = new ArrayList<IJavaTypeFormal>(n);
    for (IRNode tf : JJNode.tree.children(InterfaceDeclaration.getTypes(iface.getDeclaration()))) {
      params.add(JavaTypeFactory.getTypeFormal(tf));
    }

    for (int i = 0; i < n; i++) {
      final IJavaTypeFormal p_i = params.get(i);
      final IJavaReferenceType b_i = p_i.getExtendsBound(tEnv);
      final IJavaType a_i = iface.getTypeParameters().get(i);
      final IJavaType t_i;
      if (a_i instanceof IJavaWildcardType) {
        if (refersTo(b_i, params)) {
          return null;
        }
        final IJavaWildcardType wt = (IJavaWildcardType) a_i;
        if (wt.getUpperBound() != null) {
          t_i = utils.getGreatestLowerBound(b_i, wt.getUpperBound());
        } else if (wt.getLowerBound() != null) {
          t_i = wt.getLowerBound();
        } else {
          t_i = b_i;
        }
      } else {
        t_i = a_i;
      }
      t.add(t_i);
    }
    return JavaTypeFactory.getDeclaredType(iface.getDeclaration(), t, iface.getOuterType()); // TODO
                                                                                             // is
                                                                                             // this
                                                                                             // right?
  }

  private boolean refersTo(IJavaReferenceType t, final Collection<? extends IJavaTypeFormal> params) {
    BooleanVisitor v = new BooleanVisitor(false) {
      public boolean accept(IJavaType t) {
        if (params.contains(t)) {
          result = true;
        }
        return true;
      }
    };
    t.visit(v);
    return v.result;
  }

  /**
   * 18.5.4 More Specific Method Inference
   * 
   * When testing that one applicable method is more specific than another
   * (Â§15.12.2.5), where the second method is generic, it is necessary to test
   * whether some instantiation of the second method's type parameters can be
   * inferred to make the first method more specific than the second.
   * 
   * Let m 1 be the first method and m 2 be the second method. Where m 2 has
   * type parameters P 1 , ..., P p , let α 1 , ..., α p be inference variables,
   * and let θ be the substitution [P 1 :=α 1 , ..., P p :=α p ] .
   * 
   * Let e 1 , ..., e k be the argument expressions of the corresponding
   * invocation. Then:
   * 
   * • If m 1 and m 2 are applicable by strict or loose invocation (§15.12.2.2,
   * §15.12.2.3), then let S 1 , ..., S k be the formal parameter types of m 1 ,
   * and let T 1 , ..., T k be the result of θ applied to the formal parameter
   * types of m 2 .
   * 
   * • If m 1 and m 2 are applicable by variable arity invocation (§15.12.2.4),
   * then let S 1 , ..., S k be the first k variable arity parameter types of m
   * 1 , and let T 1 , ..., T k be the result of θ applied to the first k
   * variable arity parameter types of m 2 .
   * 
   * Note that no substitution is applied to S 1 , ..., S k ; even if m 1 is
   * generic, the type parameters of m 1 are treated as type variables, not
   * inference variables.
   * 
   * The process to determine if m 1 is more specific than m 2 is as follows:
   * 
   * • First, an initial bound set, B , is constructed from the declared bounds
   * of P 1 , ..., P p , as specified in §18.1.3.
   * 
   * • Second, for all i (1 ≤ i ≤ k), a set of constraint formulas or bounds is
   * generated.
   * 
   * ...
   * 
   * • Third, if m 2 is applicable by variable arity invocation and has k+1
   * parameters, then where S k+1 is the k+1'th variable arity parameter type of
   * m 1 and T k+1 is the result of θ applied to the k+1'th variable arity
   * parameter type of m 2 , the constraint ‹ S k+1 <: T k+1 › is generated.
   * 
   * • Fourth, the generated bounds and constraint formulas are reduced and
   * incorporated with B to produce a bound set B' .
   * 
   * If B' does not contain the bound false, and resolution of all the inference
   * variables in B' succeeds, then m 1 is more specific than m 2 .
   * 
   * Otherwise, m 1 is not more specific than m 2 .
   */
  boolean inferToBeMoreSpecificMethod(ICallState call, MethodBinding8 m_1, InvocationKind kind, MethodBinding8 m_2) {
    if (m_2.numTypeFormals <= 0) {
      return true;
    }
    final int k = call.numArgs();
    final IJavaType[] s = m_1.getParamTypes(tEnv.getBinder(), k, kind == InvocationKind.VARARGS, false);
    final IJavaType[] t = m_2.getParamTypes(tEnv.getBinder(), k, kind == InvocationKind.VARARGS, false);

    final BoundSet b = constructInitialSet(call, m_2.typeFormals, IJavaTypeSubstitution.NULL); // TODO
                                                                                         // is
                                                                                         // this
                                                                                         // right?
    final IJavaTypeSubstitution theta = b.getInitialVarSubst();
    for (int i = 0; i < k; i++) {
      t[i] = Util.subst(t[i], theta);
    }
    final BoundSet b_prime = b;
    for (int i = 0; i < k; i++) {
      generateConstraintsFromParameterTypes(b, call.getArgOrNull(i), s[i], t[i]);
    }

    if (kind == InvocationKind.VARARGS && t.length == k + 1) {
      reduceSubtypingConstraints(b_prime, s[k], t[k]);
    }

    return /* !b_prime.isFalse && */resolve(b_prime, null, false) != null;
  }

  /**
   * Originally from JLS 18.5.4
   * 
   * If T i is a proper type, the result is true if S i is more specific than T
   * i for e i (§15.12.2.5), and false otherwise. (Note that S i is always a
   * proper type.)
   * 
   * Otherwise, if T i is not a functional interface type, the constraint
   * formula ‹ S i <: T i › is generated.
   * 
   * Otherwise, T i is a parameterization of a functional interface, I . It must
   * be determined whether S i satisfies the following five constraints:
   * 
   * ...
   * 
   * If all of the above are true, then the following constraint formulas or
   * bounds are generated (where U 1 ... U k and R 1 are the parameter types and
   * return type of the function type of the capture of S i , and V 1 ... V k
   * and R 2 are the parameter types and return type of the function type of T i
   * ):
   * 
   * – If e i is an explicitly typed lambda expression:
   * 
   * › If R 2 is void , true.
   * 
   * › Otherwise, if R 1 and R 2 are functional interface types, and neither
   * interface is a subinterface of the other, then these rules are applied
   * recursively to R 1 and R 2 , for each result expression in e i .
   * 
   * › Otherwise, if R 1 is a primitive type and R 2 is not, and each result
   * expression of e i is a standalone expression (§15.2) of a primitive type,
   * true.
   *
   * › Otherwise, if R 2 is a primitive type and R 1 is not, and each result
   * expression of e i is either a standalone expression of a reference type or
   * a poly expression, true.
   * 
   * › Otherwise, ‹ R 1 <: R 2 ›.
   * 
   * – If e i is an exact method reference:
   * 
   * › For all j (1 ≤ j ≤ k), ‹ U j = V j ›.
   * 
   * › If R 2 is void , true.
   * 
   * › Otherwise, if R 1 is a primitive type and R 2 is not, and the
   * compile-time declaration for e i has a primitive return type, true.
   * 
   * › Otherwise if R 2 is a primitive type and R 1 is not, and the compile-time
   * declaration for e i has a reference return type, true.
   * 
   * › Otherwise, ‹ R 1 <: R 2 ›.
   * 
   * – If e i is a parenthesized expression, these rules are applied recursively
   * to the contained expression.
   * 
   * – If e i is a conditional expression, these rules are applied recursively
   * to each of the second and third operands.
   * 
   * – Otherwise, false.
   * 
   * If the five constraints on S i are not satisfied, the constraint formula ‹
   * S i <: T i › is generated instead.
   */
  private void generateConstraintsFromParameterTypes(BoundSet b, IRNode e_i, IJavaType s_i, IJavaType t_i) {
    if (isProperType(t_i)) {
      if (mb.isMoreSpecific(s_i, t_i, e_i)) {
        b.addTrue();
      } else {
        b.addFalse();
      }
      return;
    }
    if (tEnv.isFunctionalType(t_i) == null) {
      reduceSubtypingConstraints(b, s_i, t_i);
      return;
    }
    final IJavaDeclaredType dt_i = (IJavaDeclaredType) t_i;
    final IRNode i = dt_i.getDeclaration();
    if (satisfiesFiveConstraints(s_i, i)) {
      throw new NotImplemented();
    } else {
      reduceSubtypingConstraints(b, s_i, t_i);
    }
  }

  /**
   * Originally from JLS 18.5.4
   * 
   * – S i is a functional interface type.
   * 
   * – S i is not a superinterface of I , nor a parameterization of a
   * superinterface of I .
   * 
   * – S i is not a subinterface of I , nor a parameterization of a subinterface
   * of I .
   * 
   * – If S i is an intersection type, at least one element of the intersection
   * is not a superinterface of I , nor a parameterization of a superinterface
   * of I .
   * 
   * – If S i is an intersection type, no element of the intersection is a
   * subinterface of I , nor a parameterization of a subinterface of I .
   */
  private boolean satisfiesFiveConstraints(IJavaType s_i, IRNode i) {
    if (tEnv.isFunctionalType(s_i) == null) {
      return false;
    }
    final IJavaType iType = tEnv.convertNodeTypeToIJavaType(i);
    if (tEnv.isRawSubType(iType, s_i) || tEnv.isRawSubType(s_i, iType)) {
      return false;
    }
    if (s_i instanceof IJavaIntersectionType) {
      final IntersectionOperator atLeastOneNonSuperTypeOfI = new IntersectionOperator() {
        public boolean evaluate(IJavaType t) {
          return !tEnv.isRawSubType(iType, t);
        }

        public boolean combine(boolean e1, boolean e2) {
          return e1 | e2;
        }
      };
      final IntersectionOperator noSubTypeOfI = new IntersectionOperator() {
        public boolean evaluate(IJavaType t) {
          return !tEnv.isRawSubType(t, iType);
        }

        public boolean combine(boolean e1, boolean e2) {
          return e1 & e2;
        }
      };
      final IJavaIntersectionType it = (IJavaIntersectionType) s_i;
      if (!flattenIntersectionType(atLeastOneNonSuperTypeOfI, it) || !flattenIntersectionType(noSubTypeOfI, it)) {
        return false;
      }
    }
    return true;
  }

  interface IntersectionOperator {
    boolean evaluate(IJavaType t);

    boolean combine(boolean e1, boolean e2);
  }

  // Needs to be recursive since we only handle the first two elements
  private boolean flattenIntersectionType(IntersectionOperator op, IJavaIntersectionType it) {
    boolean rv1 = handleIntersectionComponentType(op, it.getPrimarySupertype());
    boolean rv2 = handleIntersectionComponentType(op, it.getSecondarySupertype());
    return op.combine(rv1, rv2);
  }

  private boolean handleIntersectionComponentType(IntersectionOperator op, IJavaType t) {
    if (t instanceof IJavaIntersectionType) {
      return flattenIntersectionType(op, (IJavaIntersectionType) t);
    }
    return op.evaluate(t);
  }

  /**
   * 18.1.2 Constraint Formulas
   * 
   * Constraint formulas are assertions of compatibility or subtyping that may
   * involve inference variables. The formulas may take one of the following
   * forms:
   * 
   * - <Expression -> T >: An expression is compatible in a loose invocation
   * context with type T (Â§5.3).
   * 
   * - < S -> T >: A type S is compatible in a loose invocation context with
   * type T (Â§5.3).
   * 
   * - < S <: T >: A reference type S is a subtype of a reference type T
   * (Â§4.10).
   * 
   * - < S <= T >: A type argument S is contained by a type argument T
   * (Â§4.5.1).
   * 
   * -
   * < S = T >: A reference type S is the same as a reference type T (Â§4.3.4),
   * or a type argument S is the same as type argument T .
   * 
   * - <LambdaExpression -> throws T >: The checked exceptions thrown by the
   * body of the LambdaExpression are declared by the throws clause of the
   * function type derived from T .
   * 
   * - <MethodReference -> throws T >: The checked exceptions thrown by the
   * referenced method are declared by the throws clause of the function type
   * derived from T .
   */
  static class ConstraintFormula implements Dependable {
    final IRNode expr;
    final IJavaType stype;
    final FormulaConstraint constraint;
    final IJavaType type;
    private int index;
    private int lowlink;

    ConstraintFormula(IJavaType s, FormulaConstraint c, IJavaType t) {
      if (s == null || t == null) {
    	  throw new NullPointerException();
      }
      expr = null;
      stype = s;
      constraint = c;
      type = t;
    }

    ConstraintFormula(IRNode e, FormulaConstraint c, IJavaType t) {
      if (e == null || t == null) {
    	  throw new NullPointerException();
      }
      /*
      if (!isProperType(t)) {
    	  System.out.println("Not proper? "+t);
      }
      */
      expr = e;
      stype = null;
      constraint = c;
      type = t;
    }

    ConstraintFormula subst(IJavaTypeSubstitution subst) {
      if (expr != null) {
        return new ConstraintFormula(expr, constraint, Util.subst(type, subst));
      }
      return new ConstraintFormula(Util.subst(stype, subst), constraint, Util.subst(type, subst));
    }

    @Override
    public String toString() {
      if (expr == null) {
        return stype + " " + constraint + ' ' + type;
      } else {
        return DebugUnparser.toString(expr) + ' ' + constraint + ' ' + type;
      }
    }

    @Override
    public int getIndex() {
      return index;
    }

    @Override
    public void setIndex(int i) {
      index = i;
    }

    @Override
    public int getLowLink() {
      return lowlink;
    }

    @Override
    public void setLowLink(int i) {
      lowlink = i;
    }
  }

  enum FormulaConstraint {
    IS_COMPATIBLE, IS_SUBTYPE, IS_CONTAINED_BY_TYPE_ARG, IS_SAME, THROWS
  }

  /**
   * 18.1.3 Bounds
   * 
   * During the inference process, a set of bounds on inference variables is
   * maintained. A bound has one of the following forms: - S = T , where at
   * least one of S or T is an inference variable: S is the same as T .
   * 
   * - S <: T , where at least one of S or T is an inference variable: S is a
   * subtype of T .
   * 
   * - false: No valid choice of inference variables exists.
   * 
   * - G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > ): The variables α 1 ,
   * ..., α n represent the result of capture conversion (Â§5.1.10) applied to
   * G<A 1 , ..., A n > (where A 1 , ..., A n may be types or wildcards and may
   * mention inference variables).
   * 
   * - throws α: The inference variable α appears in a throws clause.
   * 
   * A bound is satisfied by an inference variable substitution if, after
   * applying the substitution, the assertion is true. The bound false can never
   * be satisfied.
   * 
   * Some bounds relate an inference variable to a proper type. Let T be a
   * proper type. Given a bound of the form α = T or T = α, we say T is an
   * instantiation of α. Similarly, given a bound of the form α <: T , we say T
   * is a proper upper bound of α, and given a bound of the form T <: α, we say
   * T is a proper lower bound of α.
   * 
   * Other bounds relate two inference variables, or an inference variable to a
   * type that contains inference variables. Such bounds, of the form S = T or S
   * <: T , are called dependencies.
   * 
   * A bound of the form G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > )
   * indicates that α 1 , ..., α n are placeholders for the results of capture
   * conversion. This is necessary because capture conversion can only be
   * performed on a proper type, and the inference variables in A 1 , ..., A n
   * may not yet be resolved.
   * 
   * A bound of the form throws α is purely informational: it directs resolution
   * to optimize the instantiation of α so that, if possible, it is not a
   * checked exception type.
   */
  abstract class Bound<T extends IJavaReferenceType> implements Iterable<T>, Comparable<Bound<T>> {
    final T s, t;

    Bound(T s, T t) {
      if (s == null || t == null) {
        throw new IllegalArgumentException();
      }
      this.s = s;
      this.t = t;
      /*
       * if (s instanceof InferenceVariable || t instanceof InferenceVariable) {
       * // Nothing to do } else { throw new IllegalStateException(); }
       */
    }

    @Override
    public int hashCode() {
      return s.hashCode() + t.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Bound) {
        Bound<?> other = (Bound<?>) o;
        return o.getClass().equals(other.getClass()) && s.isEqualTo(tEnv, other.s) && t.isEqualTo(tEnv, other.t);
      }
      return false;
    }

    @Override
    public String toString() {
      return toSourceText(null);
    }

    public abstract String toSourceText(IDebugable context);
    
    abstract Bound<T> subst(IJavaTypeSubstitution subst);

    public Iterator<T> iterator() {
      return new PairIterator<T>(s, t);
    }

    public int compareTo(Bound<T> other) {
      if (other instanceof EqualityBound) {
        // This should go before all equalities
        return -1;
      }
      return this.hashCode() - other.hashCode();
    }
  }

  EqualityBound newEqualityBound(IJavaType s, IJavaType t) {
    if (t == null) {
      throw new NullPointerException("No type for equality bound");
    }
    final boolean swap;
    if (t instanceof InferenceVariable) {
      if (s instanceof InferenceVariable) {
        InferenceVariable is = (InferenceVariable) s;
        InferenceVariable it = (InferenceVariable) t;
        swap = is.compareTo(it) < 0;
      } else {
        // T = a
        swap = true;
      }
    } else if (s instanceof InferenceVariable) {
      swap = false;
    } else {
      swap = s.toString().compareTo(t.toString()) < 0;
    }
    if (swap) {
      IJavaType temp = s;
      s = t;
      t = temp;
    }
    if (t instanceof IJavaPrimitiveType) {
    	t = JavaTypeFactory.getCorrespondingDeclType(tEnv, (IJavaPrimitiveType) t);
    }
    return new EqualityBound((IJavaReferenceType) s, (IJavaReferenceType) t);
  }

  static String print(IDebugable context, IJavaType t) {
	if (context != null && t instanceof InferenceVariable) {
	  return ((InferenceVariable) t).toSourceText(context);
	}
	return t.toSourceText();
  }
  
  class EqualityBound extends Bound<IJavaReferenceType>implements IEquality {
    EqualityBound(IJavaReferenceType s, IJavaReferenceType t) {
      super(s, t);
    }
    
    @Override
    public String toSourceText(IDebugable context) {
      return print(context, s) + " = " + print(context, t);
    }

    @Override
    EqualityBound subst(IJavaTypeSubstitution subst) {
      return newEqualityBound(Util.subst(s, subst), Util.subst(t, subst));
    }

    public boolean isTrivial() {
      return false;
    }

    public Collection<InferenceVariable> vars() {
      return Collections.emptySet();
    }

    public Collection<IJavaReferenceType> values() {
      List<IJavaReferenceType> rv = new ArrayList<IJavaReferenceType>(2);
      rv.add(s);
      rv.add(t);
      return rv;
    }

    public InferenceVariable getRep() {
      if (s instanceof InferenceVariable) {
        return (InferenceVariable) s;
      }
      return null;
    }

    @Override
    public final int compareTo(Bound<IJavaReferenceType> other) {
      if (other instanceof EqualityBound) {
        if (t instanceof TypeVariable) {
          if (other.t instanceof TypeVariable) {
            return this.hashCode() - other.hashCode();
          } else {
            return 1;
          }
        } else if (other.t instanceof TypeVariable) {
          return -1;
        } else
          return this.hashCode() - other.hashCode();
      }
      return 1; // The other bound should be first
    }
  }

  // S is a subtype of T
  class SubtypeBound extends Bound<IJavaReferenceType> {
    SubtypeBound(IJavaReferenceType s, IJavaReferenceType t) {
      super(s, t);
    }

    @Override
    public String toSourceText(IDebugable context) {
      return print(context, s) + " <: " + print(context, t);
    }

    @Override
    SubtypeBound subst(IJavaTypeSubstitution subst) {
      return new SubtypeBound((IJavaReferenceType) Util.subst(s, subst), (IJavaReferenceType) Util.subst(t, subst));
    }
  }

  class CaptureBound extends Bound<IJavaDeclaredType> {
    CaptureBound(IJavaDeclaredType vars, IJavaDeclaredType needCapture) {
      super(vars, needCapture);
    }

    boolean refersTo(final Set<InferenceVariable> vars) {
      for (IJavaType param : s.getTypeParameters()) {
        if (vars.contains(param)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toSourceText(IDebugable context) {
      return print(context, s) + " = capture(" + print(context, t) + ')';
    }

    @Override
    CaptureBound subst(IJavaTypeSubstitution subst) {
      return new CaptureBound((IJavaDeclaredType) Util.subst(s, subst), (IJavaDeclaredType) Util.subst(t, subst));
    }
  }

  /**
   * From Â§18.1.3:
   * 
   * When inference begins, a bound set is typically generated from a list of
   * type parameter declarations P 1 , ..., P p and associated inference
   * variables α 1 , ..., α p . Such a bound set is constructed as follows. For
   * each l (1 <= l <= p):
   * 
   * - If P l has no TypeBound, the bound α l <: Object appears in the set.
   * 
   * - Otherwise, for each type T delimited by & in the TypeBound, the bound α l
   * <: T[P 1 :=α 1 , ..., P p :=α p ] appears in the set; if this results in no
   * proper upper bounds for α l (only dependencies), then the bound α l <:
   * Object also appears in the set.
   */
  BoundSet constructInitialSet(IDebugable call, IRNode typeFormals, IJavaTypeSubstitution boundSubst, IJavaType... createdVars) {
    return constructInitialSet(call, getTypeFormalList(typeFormals), boundSubst, createdVars);
  }

  BoundSet constructInitialSet(IDebugable call, Collection<IJavaTypeFormal> typeFormals, IJavaTypeSubstitution boundSubst,
      IJavaType... createdVars) {
    // Setup inference variables
    final int numFormals = typeFormals.size();
    final InferenceVariable[] vars = new InferenceVariable[numFormals];
    int i = 0;
    for (IJavaTypeFormal f : typeFormals) {
      vars[i] = createdVars.length > 0 ? (InferenceVariable) createdVars[i] : new InferenceVariable(f.getDeclaration(), call);
      i++;
    }
    final BoundSet set = new BoundSet(call, typeFormals, vars);
    final IJavaTypeSubstitution theta = set.getInitialVarSubst();
    i = 0;
    for (IJavaTypeFormal tf : typeFormals) {
      IRNode bounds = TypeFormal.getBounds(tf.getDeclaration());
      boolean noBounds = true;
      boolean gotProperBound = false;
      for (IRNode bound : MoreBounds.getBoundIterator(bounds)) {
        final IJavaType t = tEnv.getBinder().getJavaType(bound);
        final IJavaType t_subst = AbstractTypeSubstitution.applySubst(t, theta, boundSubst);
        if (t_subst == null) {
        	AbstractTypeSubstitution.applySubst(t, theta, boundSubst);
        }
        noBounds = false;
        set.addSubtypeBound(vars[i], t_subst);
        if (isProperType(t_subst)) {
          gotProperBound = true;
        }
      }
      if (noBounds || !gotProperBound) {
        set.addSubtypeBound(vars[i], tEnv.getObjectType());
      }
      i++;
    }
    // TODO is there anything else to do?
    return set;
  }

  static class BoundSubset {
    private final Set<Equality> equalities = new HashSet<Equality>();
    private final Set<SubtypeBound> upperBounds = new HashSet<SubtypeBound>();
    private final Set<SubtypeBound> lowerBounds = new HashSet<SubtypeBound>();

    boolean examine(BoundCondition cond) {
      for (SubtypeBound b : upperBounds) {
        if (cond.examineUpperBound(b.s)) {
          return true;
        }
      }
      for (SubtypeBound b : lowerBounds) {
        if (cond.examineLowerBound(b.t)) {
          return true;
        }
      }
      for (Equality e : equalities) {
        if (cond.examineEquality(e)) {
          return true;
        }
      }
      return false;
    }
  }

  interface BoundCondition {
    boolean examineEquality(Equality e);

    boolean examineLowerBound(IJavaType t);

    boolean examineUpperBound(IJavaType t);
  }

  class Equalities implements Iterable<IEquality> {
    // used to find its identity for inference variables
    final Map<InferenceVariable, Equality> finder = new HashMap<InferenceVariable, Equality>();
    final Set<EqualityBound> bounds = new HashSet<EqualityBound>();

    Equality find(IJavaReferenceType t) {
      if (!(t instanceof InferenceVariable)) {
        return null;
      }
      Equality e = finder.get(t);
      if (e == null) {
        e = new Equality(t);
        finder.put((InferenceVariable) t, e);
      }
      return e;
    }

    @Override
    public Iterator<IEquality> iterator() {
      Set<IEquality> rv = new HashSet<IEquality>(finder.values());
      rv.addAll(bounds);
      return rv.iterator();
    }

    void addAll(Equalities o) {
      bounds.addAll(o.bounds);
      for (final Equality oe : o.finder.values()) {
        for (InferenceVariable v : oe.vars) {
          Equality e = finder.get(v);
          if (e != null) {
            e.merge(oe);
            continue;
          }
        }
        // Not already present
        final Equality clone = oe.clone();
        for (InferenceVariable v : oe.vars) {
          finder.put(v, clone);
        }
      }
    }

    boolean contains(EqualityBound eb) {
      Equality e1 = finder.get(eb.s);
      Equality e2 = finder.get(eb.t);
      if (e1 != null) {
        if (e2 != null) {
          return e1 == e2;
        } else {
          return e1.values.contains(eb.t);
        }
      }
      return bounds.contains(eb);
    }

    Equality add(EqualityBound eb) {
      // System.out.println("Adding equality: "+eb);
      Equality e1 = find(eb.s);
      Equality e2 = find(eb.t);
      if (e1 != null) {
        if (e2 != null) {
          e1.merge(e2);

          for (InferenceVariable v : e2.vars) {
            finder.put(v, e1);
          }
        } else {
          e1.values.add(eb.t);
        }
        return e1;
      } else {
        bounds.add(eb);
      }
      return null;
    }

    boolean isEmpty() {
      if (!bounds.isEmpty()) {
        return false;
      }
      if (finder.isEmpty()) {
        return true;
      }
      for (Equality e : finder.values()) {
        if (!e.isTrivial()) {
          return false;
        }
      }
      return true;
    }

    Collection<EqualityBound> getBounds() {
      if (isEmpty()) {
        return Collections.emptyList();
      }
      List<EqualityBound> bounds = new ArrayList<EqualityBound>(this.bounds);
      for (Equality e : finder.values()) {
        if (e.isTrivial()) {
          continue;
        }
        for (IJavaType s : e) {
          for (IJavaType t : e) {
            if (s != t) {
              bounds.add(newEqualityBound(s, t));
            }
          }
        }
      }
      return bounds;
    }

    @Override
    public String toString() {
      if (isEmpty()) {
        return "";
      }
      StringBuilder b = new StringBuilder();
      for (IEquality e : this) {
        if (e.isTrivial()) {
          continue;
        }
        b.append(e).append(", \n");
      }
      return b.toString();
    }

    boolean containsAll(Equalities sub) {
      if (!bounds.containsAll(sub.bounds)) {
        return false;
      }
      for (Map.Entry<InferenceVariable, Equality> e : sub.finder.entrySet()) {
        Equality eq = finder.get(e.getKey());
        if (eq == null || !eq.containsAll(e.getValue())) {
          return false;
        }
      }
      return true;
    }
  }

  interface IEquality extends Iterable<IJavaReferenceType> {
    Collection<InferenceVariable> vars();

    Collection<IJavaReferenceType> values();

    boolean isTrivial();

    InferenceVariable getRep();
    
    String toSourceText(IDebugable context);
  }

  class Equality implements IEquality {
    final Set<InferenceVariable> vars = new HashSet<InferenceVariable>();
    final Set<IJavaReferenceType> values = new HashSet<IJavaReferenceType>();
    InferenceVariable rep;

    Equality(IJavaReferenceType t) {
      if (t == null) {
        throw new IllegalStateException();
      } else if (t instanceof InferenceVariable) {
        InferenceVariable v = (InferenceVariable) t;
        if (v.formal != null) {
          rep = v;
        }
        vars.add(v);
      } else {
        values.add(t);
      }
    }

    boolean containsAll(Equality sub) {
      return vars.containsAll(sub.vars) && values.containsAll(sub.values);
    }

    Equality(Equality orig) {
      merge(orig);
    }

    @Override
    public Equality clone() {
      return new Equality(this);
    }

    public Collection<InferenceVariable> vars() {
      return vars;
    }

    public Collection<IJavaReferenceType> values() {
      return values;
    }

    public boolean isTrivial() {
      return vars.size() + values.size() <= 1;
    }

    void merge(Equality o) {
      vars.addAll(o.vars);
      findRep(o.vars);
      values.addAll(o.values);
    }

    private InferenceVariable findRep(Set<InferenceVariable> vars) {
      if (rep == null) {
        for (InferenceVariable v : vars) {
          if (v.formal != null) {
            rep = v;
            break;
          }
        }
      }
      return rep;
    }

    public InferenceVariable getRep() {
      if (rep == null && !vars.isEmpty()) {
        // Just pick one for now
        return vars.iterator().next();
      }
      return rep;
    }

    @Override
    public String toString() {
      return toSourceText(null);
    }
    
    @Override
    public String toSourceText(final IDebugable context) {    
      if (vars.isEmpty()) {
        return toString(context, values, null);
      }
      if (values.isEmpty()) {
        return toString(context, vars, getRep());
      }
      StringBuilder sb = new StringBuilder();
      unparseSet(context, sb, vars);
      sb.append(" = ");
      unparseSet(context, sb, values);
      return sb.toString();
    }

    private void unparseSet(final IDebugable context, StringBuilder sb, Set<? extends IJavaReferenceType> types) {
      if (types.size() == 1) {
        sb.append(print(context, types.iterator().next()));
      } else {
        sb.append('{');
        boolean first = true;
        for (IJavaType t : types) {
          if (first) {
            first = false;
          } else {
            sb.append(", \n\t");
          }
          sb.append(print(context, t));
        }
        sb.append('}');
      }
    }

    private String toString(final IDebugable context, Set<? extends IJavaReferenceType> types, IJavaType startWith) {
      final int n = types.size();
      switch (n) {
      case 0:
        return "? = ?";
      case 1:
        return print(context, types.iterator().next()) + " = ?";
      default:
        StringBuilder sb = new StringBuilder();
        if (startWith != null) {
          sb.append(print(context, startWith));
          if (n == 2) {
            sb.append(" = ");
          } else {
            sb.append(" = {");
          }
        }
        int i = 0;
        for (IJavaType t : types) {
          if (t == startWith) {
        	i++;
            continue; // Already handled
          }
          if (sb.length() == 0) {
            sb.append(print(context, t));
            if (n == 2) {
              sb.append(" = ");
            } else {
              sb.append(" = {");
            }
          } else {
            if (i >= 2) {
              sb.append(", ");
            }
            sb.append(print(context, t));
          }
          i++;
        }
        if (n > 2) {
          sb.append('}');
        }
        return sb.toString();
      }
    }

    @Override
    public Iterator<IJavaReferenceType> iterator() {
      if (vars.isEmpty()) {
        return values.iterator();
      }
      if (values.isEmpty()) {
        return new ArrayList<IJavaReferenceType>(vars).iterator();
      }
      return new AppendIterator<IJavaReferenceType>(vars.iterator(), values.iterator());
    }

    public Map<InferenceVariable, IJavaType> createMapTo(InferenceVariable alpha, IJavaType s) {
      switch (vars.size()) {
      case 0:
        return Collections.emptyMap();
      case 1:
        return Collections.singletonMap(alpha, s);
      default:
        Map<InferenceVariable, IJavaType> rv = new HashMap<InferenceVariable, IJavaType>(vars.size());
        for (InferenceVariable v : vars) {
          rv.put(v, s);
        }
        rv.put(alpha, s);
        return rv;
      }
    }
  }

  /**
   * An important intermediate result of inference is a bound set. It is
   * sometimes convenient to refer to an empty bound set with the symbol true;
   * this is merely out of convenience, and the two are interchangeable
   */
  class BoundSet {
	private boolean debug = false;
    /**
     * Controls whether bounds are actually incorporated or not
     */
    private final boolean isTemp;
    private boolean isFalse = false;
    private boolean usedUncheckedConversion = false;
    private final Set<InferenceVariable> thrownSet = new HashSet<InferenceVariable>();
    // private final Set<EqualityBound> equalities = new
    // HashSet<EqualityBound>();
    private final Equalities equalities = new Equalities();
    private final Set<SubtypeBound> subtypeBounds = new HashSet<SubtypeBound>();
    private final Set<CaptureBound> captures = new HashSet<CaptureBound>();

    /**
     * Queue for bounds that haven't been incorporated yet
     */
    private final PriorityQueue<Bound<?>> unincorporated = new PriorityQueue<Bound<?>>();

    /**
     * The original bound that eventually created this one
     */
    private final BoundSet original;

    private final IDebugable originatingCall;
    
    /**
     * Mapping from the original type variables to the corresponding inference
     * variables
     */
    final Map<IJavaTypeFormal, InferenceVariable> variableMap = new HashMap<IJavaTypeFormal, InferenceVariable>();

    private BoundSet(IDebugable call) {
      originatingCall = call;
      isTemp = true;
      original = null;
    }

    BoundSet(final IDebugable call, final Collection<IJavaTypeFormal> typeFormals, final InferenceVariable[] vars) {
      originatingCall = call;
      original = null;
      isTemp = false;

      int i = 0;
      for (IJavaTypeFormal f : typeFormals) {
        variableMap.put(f, vars[i]);
        i++;
      }
    }

    BoundSet(BoundSet orig) {
      if (orig.isTemp) {
        throw new IllegalStateException();
      }
      originatingCall = orig.originatingCall;
      isTemp = false;
      original = orig.original == null ? orig : orig.original;
      isFalse = orig.isFalse;
      usedUncheckedConversion = orig.usedUncheckedConversion;
      thrownSet.addAll(orig.thrownSet);
      equalities.addAll(orig.equalities);
      subtypeBounds.addAll(orig.subtypeBounds);
      captures.addAll(orig.captures);
      variableMap.putAll(orig.variableMap);
      debug = orig.debug;
    }

    IDebugable getCall() {
      return originatingCall;
    }
    
    void debug() {
      debug = true;
    }
    
    boolean contains(BoundSet sub) {
      return thrownSet.containsAll(sub.thrownSet) && subtypeBounds.containsAll(sub.subtypeBounds)
          && captures.containsAll(sub.captures) && equalities.containsAll(sub.equalities);
    }

    void merge(BoundSet other) {
      merge(other, false);
    }

    void merge(BoundSet other, boolean addBefore) {
      if (isTemp/* || !other.isTemp */) {
        throw new IllegalStateException();
      }
      isFalse |= other.isFalse;
      usedUncheckedConversion |= other.usedUncheckedConversion;
      thrownSet.addAll(other.thrownSet);
      mergeVariableMaps(other);
      if (addBefore) {
        addAllBefore(other.equalities.getBounds());
        addAllBefore(other.subtypeBounds);
        addAllBefore(other.captures);
        addAllBefore(other.unincorporated);
      } else {
        unincorporated.addAll(other.equalities.getBounds());
        unincorporated.addAll(other.subtypeBounds);
        unincorporated.addAll(other.captures);
        unincorporated.addAll(other.unincorporated);
      }
      incorporate();
    }

    private void addAllBefore(Collection<? extends Bound<?>> it) {
      /*
       * for(Bound<?> b : it) { unincorporated.addFirst(b); }
       */
      unincorporated.addAll(it);
    }

    void mergeVariableMaps(BoundSet other) {
    	for(Map.Entry<IJavaTypeFormal, InferenceVariable> e : other.variableMap.entrySet()) {
    		if (variableMap.containsKey(e.getKey())) {
    			// TODO qualify instead?
    			continue;
    		}
    		variableMap.put(e.getKey(), e.getValue());
    	}
    }
    
    void mergeWithSubst(BoundSet other, IJavaTypeSubstitution subst) {
      isFalse |= other.isFalse;
      usedUncheckedConversion |= other.usedUncheckedConversion;
      // TODO no need to subst?
      thrownSet.addAll(other.thrownSet);
      mergeVariableMaps(other);

      for (EqualityBound b : other.equalities.getBounds()) {
        unincorporated.add(b.subst(subst));
      }
      for (SubtypeBound b : other.subtypeBounds) {
        unincorporated.add(b.subst(subst));
      }
      for (CaptureBound b : other.captures) {
        unincorporated.add(b.subst(subst));
      }
      for (Bound<?> b : other.unincorporated) {
        unincorporated.add(b.subst(subst));
      }
      incorporate();
    }

    private boolean isEmpty() {
      return !isFalse && !usedUncheckedConversion && unincorporated.isEmpty() && thrownSet.isEmpty() && equalities.isEmpty()
          && subtypeBounds.isEmpty() && captures.isEmpty();
    }

    @Override
    public String toString() {
      StringBuilder b = new StringBuilder();
      if (isFalse) {
        b.append("FALSE, ");
      }
      if (usedUncheckedConversion) {
        b.append("unchecked,");
      }
      if (!thrownSet.isEmpty()) {
        b.append("throws ");
        for (InferenceVariable v : thrownSet) {
          b.append(v).append(", ");
        }
        b.append('\n');
      }
      for (IEquality e : equalities) {
        if (e.isTrivial()) {
          continue;
        }
        b.append(e.toSourceText(originatingCall)).append(", \n");
      }
      for (Bound<?> bound : subtypeBounds) {
        b.append(bound.toSourceText(originatingCall)).append(", \n");
      }
      for (Bound<?> bound : captures) {
        b.append(bound.toSourceText(originatingCall)).append(", \n");
      }
      for (Bound<?> bound : unincorporated) {
        b.append(bound.toSourceText(originatingCall)).append(", \n");
      }
      return b.toString();
    }

    BoundSet substPreferredVars(Set<InferenceVariable> preferredVars) {
    	final BoundSet subst = new BoundSet(this);
		// Modify variableMap to use my preferred variables
    	outer:
		for(final Entry<IJavaTypeFormal, InferenceVariable> e : subst.variableMap.entrySet()) {
			if (preferredVars.contains(e.getValue())) {
				// Already preferred, so nothing to do
				continue;
			}
			final Equality eq = subst.equalities.find(e.getValue());
			for(InferenceVariable pv : preferredVars) {
				if (eq.vars.contains(pv)) {
					e.setValue(pv);
					break; // TODO is this right?
				}
			}
		}
		// TODO anything else?
		return subst;
	}
    
    IJavaTypeSubstitution getInitialVarSubst() {
      return TypeSubstitution.create(tEnv.getBinder(), variableMap);
    }
    
    Map<IJavaTypeFormal, InferenceVariable> getInitialVarMap() {
      return new HashMap<>(variableMap);
    }

    Set<InferenceVariable> getInitialInferenceVars() {
      return new HashSet<>(variableMap.values());
    }
    
    public Map<InferenceVariable, IJavaType> getInstantiations() {
      final Map<InferenceVariable, IJavaType> instantiations = new HashMap<InferenceVariable, IJavaType>();
      loop:
      for (IEquality e : equalities) {
        if (!e.vars().isEmpty()) {
          IJavaType value = null;
          for (IJavaType t : e.values()) {
            if (isProperType(t)) {
              if (value == null || valueisEquivalent(t, value)) {
                value = t;
              } else if (value instanceof TypeVariable) {
                value = t;
              } else if (t instanceof TypeVariable) {
                // Prefer the non-type variable
              } else if (valueisEquivalent(value, t)) {
                // Nothing to do
              } else {
                valueisEquivalent(t, value);
                valueisEquivalent(value, t);
                //throw new IllegalStateException("Which value to use? " + value + " vs " + t);
                //
                // Probably from an method incompatible with the call
                continue loop;
              }
            }
          }
          if (value == null) {
            continue;
          }
          for (InferenceVariable v : e.vars()) {
            instantiations.put(v, value);
          }
        }
      }
      return instantiations;
    }

    /**
     * 
     * @param eliminateTypeVariables if true, eliminate internally introduced type variables
     * @param useSubstAsBounds if true, reformulate 'sketchy' instantiations as ReboundedTypeFormal
     */
    IJavaTypeSubstitution getFinalTypeSubst(boolean eliminateTypeVariables, boolean useSubstAsBounds) {
      return getFinalTypeSubst(eliminateTypeVariables, useSubstAsBounds, false);
    }
    
    IJavaTypeSubstitution getFinalTypeSubst(boolean eliminateTypeVariables, boolean useSubstAsBounds, boolean ignoreMissing) {
      Map<IJavaTypeFormal, IJavaType> subst = computeTypeSubst(eliminateTypeVariables, useSubstAsBounds, ignoreMissing);
      return TypeSubstitution.create(tEnv.getBinder(), subst);
    }

    Map<IJavaTypeFormal, IJavaType> computeTypeSubst(boolean eliminateTypeVariables, boolean useSubstAsBounds, boolean ignoreMissing) {
      final Map<IJavaTypeFormal, IJavaType> subst = new HashMap<IJavaTypeFormal, IJavaType>();
      final Map<InferenceVariable, IJavaType> instantiations = getInstantiations();
      if (eliminateTypeVariables) {
        // System.out.println("Eliminating type variables");
        for (Entry<InferenceVariable, IJavaType> e : instantiations.entrySet()) {
          try {
        	  if (couldBeRecursiveType(e.getValue())) {
        		  continue;
        	  }
        	  IJavaType elim = eliminateTypeVariables(utils, e.getValue());
        	  if (elim != e.getValue()) {
        		  e.setValue(elim);
        	  }
          } catch(StackOverflowError ex) {
        	  System.err.println("Leaving this type variable, due to stack overflow: "+e.getKey()+" -> "+e.getValue());        	
        	  couldBeRecursiveType(e.getValue());
          }
        }
      }
      for (Entry<IJavaTypeFormal, InferenceVariable> e : variableMap.entrySet()) {
        final IJavaType t = instantiations.get(e.getValue());
        if (t == null && !ignoreMissing) {
          getInstantiations();
          System.err.println("No instantiation for " + e.getKey());
        }
        final IJavaType t_final;
        if (useSubstAsBounds && t instanceof IJavaDeclaredType && t.getName().equals("java.lang.Object")) {
          /*
           * System.out.println("Rebounded: "+e.getKey()); if (
           * "R extends java.lang.Object in java.util.stream.Stream.map(java.util.function.Function <? super T, ? extends R>)"
           * .equals(e.getKey().toString())) { System.out.println(
           * "Found Stream.R"); }
           */
          final IJavaTypeFormal f = e.getKey();
          /*
           * Putting this check in prevents later use of type variables to allow
           * context to determine the right substitution
           * 
           * if (t.equals(f.getExtendsBound(tEnv))) { // TODO Skip substitution?
           * continue; }
           */
          t_final = new ReboundedTypeFormal(tEnv, f, t);
        } else {
          t_final = t;
        }
        subst.put(e.getKey(), t_final);
        final Equality eq = equalities.find(e.getValue());
        for(InferenceVariable v : eq.vars) {
        	subst.put(v, t_final); // Include for completeness!
        }
      }
      return subst;
    }
    
    public boolean couldBeRecursiveType(IJavaType t) {
    	RecursiveTypeChecker v = new RecursiveTypeChecker();
    	t.visit(v);
    	return v.result;
    }

    public class RecursiveTypeChecker extends IJavaType.BooleanVisitor {
    	private final Stack<IJavaType> stack = new Stack<>();
    	
		@Override
		public boolean accept(IJavaType t) {
			if (result) {
				return false;
			}
			if (stack.contains(t)) {
				result = true;
				return false;
			}
			stack.push(t);
			return true;
		}    	
		
		@Override
		public void finish(IJavaType t) {
			IJavaType temp = stack.pop();
			if (temp != t) {
				throw new IllegalStateException();
			}
		}
    }

    private void addInferenceVariables(Collection<InferenceVariable> newVars) {
      // resolution doesn't require us to do anything here
      // variableMap.putAll(newMappings);
    }

    void addFalse() {
      if (debug) {
    	  System.out.println("FALSE: "+this);
      }
      isFalse = true;
    }

    void addTrue() {
      // TODO what is there to do?
    }

    void addEqualityBound(IJavaType s, IJavaType t) {
      incorporate(newEqualityBound(s, t));
    }

    // s <: (is a subtype of) t
    void addSubtypeBound(IJavaType s, IJavaType t) {
      incorporate(new SubtypeBound((IJavaReferenceType) s, (IJavaReferenceType) t));
    }

    // G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > )
    void addCaptureBound(IJavaType s, IJavaType t) {
      incorporate(new CaptureBound((IJavaDeclaredType) s, (IJavaDeclaredType) t));
    }

    void addThrown(IJavaType t) {
      if (t instanceof InferenceVariable) {
        InferenceVariable v = (InferenceVariable) t;
        thrownSet.add(v);
      } else {
        Set<InferenceVariable> vars = new HashSet<InferenceVariable>();
        getReferencedInferenceVariables(vars, t);
        thrownSet.addAll(vars);
      }
    }
    
    /**
     * 18.3 Incorporation
     * 
     * As bound sets are constructed and grown during inference, it is possible
     * that new bounds can be inferred based on the assertions of the original
     * bounds. The process of incorporation identifies these new bounds and adds
     * them to the bound set.
     * 
     * Incorporation can happen in two scenarios. One scenario is that the bound
     * set contains complementary pairs of bounds; this implies new constraint
     * formulas, as specified in §18.3.1. The other scenario is that the bound
     * set contains a bound involving capture conversion; this implies new
     * bounds and may imply new constraint formulas, as specified in §18.3.2. In
     * both scenarios, any new constraint formulas are reduced, and any new
     * bounds are added to the bound set. This may trigger further
     * incorporation; ultimately, the set will reach a fixed point and no
     * further bounds can be inferred.
     * 
     * If incorporation of a bound set has reached a fixed point, and the set
     * does not contain the bound false, then the bound set has the following
     * properties:
     * 
     * • For each combination of a proper lower bound L and a proper upper bound
     * U of an inference variable, L <: U. • If every inference variable
     * mentioned by a bound has an instantiation, the bound is satisfied by the
     * corresponding substitution. • Given a dependency α = β, every bound of α
     * matches a bound of β, and vice versa. • Given a dependency α <: β, every
     * lower bound of α is a lower bound of β, and every upper bound of β is an
     * upper bound of α.
     */
    private void incorporate(Bound<?>... newBounds) {
      for (Bound<?> b : newBounds) {
        if (b.s == b.t) {
          // Ignoring these meaningless bounds
          continue;
        }
        if (unincorporated.contains(b)) {
          continue;
        }
        unincorporated.add(b);
        // System.out.println("Added as unincorporated: "+b);
      }
      if (isTemp) {
        // Don't do anything, since it'll be incorporated when merged
        return;
      }
      final BoundSet temp = this;// new BoundSet();
      // Stop if temp gets false
      while (!temp.isFalse && !unincorporated.isEmpty()) {
        Bound<?> b = unincorporated.remove();
        // String bound = b.toString();
        /*
         * if (bound.contains("@ Collectors.K =") || bound.contains(
         * "@ Collectors.D =")) { System.out.println(
         * "\tFound equality for type variable"); }
         */
        // Check for combos and reduce the resulting constraints

        if (b instanceof SubtypeBound) {
          SubtypeBound sb = (SubtypeBound) b;
          if (subtypeBounds.contains(sb)) {
            continue;
          }
          if (debug) {
        	System.out.println("Incorporating subtypeB "+sb);
          }
          subtypeBounds.add(sb);
          incorporateSubtypeBound(temp, sb);
        } else if (b instanceof EqualityBound) {
          EqualityBound eb = (EqualityBound) b;
          if (equalities.contains(eb)) {
            continue;
          }
          if (debug) {
        	System.out.println("Incorporating equalB "+eb);
          }
          equalities.add(eb);
          incorporateEqualityBound(temp, eb);
        } else {
          CaptureBound cb = (CaptureBound) b;
          if (captures.contains(cb)) {
            continue;
          }
          if (debug) {
            System.out.println("Incorporating captureB "+cb);
          }
          captures.add(cb);
          incorporateCaptureBound(temp, cb);
        }
      }
      if (temp != this && !temp.isEmpty()) {
        // System.out.println("Merging "+temp);
        merge(temp);
      }
    }

    // See incorporateSubtypeBound() for details
    private void incorporateEqualityBound(BoundSet bounds, EqualityBound eb) {
      if (eb.s instanceof InferenceVariable) {
        incorporateEqualityBound(bounds, (InferenceVariable) eb.s, eb.t);
      }
      if (eb.t instanceof InferenceVariable) {
        incorporateEqualityBound(bounds, (InferenceVariable) eb.t, eb.s);
      }
    }

    private void incorporateEqualityBound(BoundSet bounds, final InferenceVariable alpha, IJavaReferenceType s) {
      final Equality alphaEq = equalities.find(alpha);
      final IJavaTypeSubstitution subst = isProperType(s) ? TypeSubstitution.create(tEnv.getBinder(), alphaEq.createMapTo(alpha, s))
          : null;
      /*
      // Make sure that we try all combos? (TODO inefficient)
      for (InferenceVariable v : copy(alphaEq.vars())) {
    	Can't skip myself, since it might be a new equality (bound)
    	BUT originally here to prevent infinite loops
    	if (alpha == v) {
    		continue;
    	}
        incorporateEqualityBound(bounds, v, s, subst);
      }
      */
      incorporateEqualityBound(bounds, alphaEq, s, subst);
    }

    private void incorporateEqualityBound(BoundSet bounds, final Equality alphaEq, /*final InferenceVariable alpha, */IJavaReferenceType s,
        IJavaTypeSubstitution subst) {
      for (final IEquality e : equalities) {
    	final boolean containsAlpha = e == alphaEq; //e.vars().contains(alpha);
        // case 1: α = S and α = T imply ‹S = T›
        if (containsAlpha) {
          for (IJavaType t : copy(e.values())) {
        	if (s == t) {
        		continue; // Skip since this is the equality being introduced
        	}
            reduceTypeEqualityConstraints(bounds, s, t);
          }
        }
        // case 5: α = U and S = T imply ‹S[α:=U] = T[α:=U]›
        if (subst != null) {
          if (containsAlpha) {
            continue; // Skip this equality since it already contains this
                      // variable?
          }
          if (!e.vars().isEmpty()) {
            InferenceVariable beta = e.getRep(); // No subst to do
            for (IJavaType t : copy(e.values())) {
              IJavaType t_subst = Util.subst(t, subst);
              if (t_subst != t) {
            	//System.out.println("EB: "+beta+" == "+t_subst);
                reduceTypeEqualityConstraints(bounds, beta, t_subst);
              }
            }
          }
          for (IJavaType s1 : e.values()) {
            for (IJavaType t : e.values()) {
              if (s1 == t) {
            	  continue; // No need to check against itself
              }
              IJavaType s_subst = Util.subst(s1, subst);
              IJavaType t_subst = Util.subst(t, subst);
              if (s_subst != s1 || t_subst != t) {
                reduceTypeEqualityConstraints(bounds, s_subst, t_subst);
              }
            }
          }
        }
      }
      for (SubtypeBound b : copy(subtypeBounds)) {
        // case 2: α = S and α <: T imply ‹S <: T›
        if (alphaEq.vars.contains(b.s)) {
          reduceSubtypingConstraints(bounds, s, b.t);
        }
        // case 3: α = S and T <: α imply ‹T <: S›
        else if (alphaEq.vars.contains(b.t)) {
          IJavaType t = b.s;
          reduceSubtypingConstraints(bounds, t, s);
        }
        // case 6: α = U and S <: T imply ‹S[α:=U] <: T[α:=U]›
        if (subst != null) {
          IJavaType b_s_subst = Util.subst(b.s, subst);
          IJavaType b_t_subst = Util.subst(b.t, subst);
          if (b_s_subst != b.s || b_t_subst != b.t) {
            reduceSubtypingConstraints(bounds, b_s_subst, b_t_subst);
          }
        }
      }
    }

    /**
     * 18.3.1 Complementary Pairs of Bounds
     *
     * (In this section, S and T are inference variables or types, and U is a
     * proper type. For conciseness, a bound of the form α = T may also match a
     * bound of the form T = α.)
     * 
     * When a bound set contains a pair of bounds that match one of the
     * following rules, a new constraint formula is implied:
     * 
     * 1• α = S and α = T imply ‹S = T› 2• α = S and α <: T imply ‹S <: T› 3• α
     * = S and T <: α imply ‹T <: S› 4• S <: α and α <: T imply ‹S <: T› 5• α =
     * U and S = T imply ‹S[α:=U] = T[α:=U]› 6• α = U and S <: T imply ‹S[α:=U]
     * <: T[α:=U]›
     * 
     * ... see below
     */
    private void incorporateSubtypeBound(BoundSet bounds, SubtypeBound sb) {
      if (sb.s instanceof InferenceVariable) {
        final InferenceVariable alpha = (InferenceVariable) sb.s;
        for (IEquality e : equalities) {
          // case 2: α = S and α <: T imply ‹S <: T›
          if (e.vars().contains(alpha)) {
            for (IJavaType s : copy(e.values())) {
              reduceSubtypingConstraints(bounds, s, sb.t);
            }
          }
        }
        for (SubtypeBound b : copy(subtypeBounds)) {
          if (sb == b) {
        	  continue; 
          }
          if (b.t == alpha) {
            // case 4a: S <: α and α <: T imply ‹S <: T›
            reduceSubtypingConstraints(bounds, b.s, sb.t);
          }
          if (b.s == alpha) {
            /*
             * When a bound set contains a pair of bounds α <: S and α <: T, and
             * there exists a supertype of S of the form G<S1, ..., Sn> and a
             * supertype of T of the form G<T1, ..., Tn> (for some generic class
             * or interface, G), then for all i (1 ≤ i ≤ n), if Si and Ti are
             * types (not wildcards), the constraint formula ‹Si = Ti› is
             * implied.
             */
        	/*
            String unparse = b.toString();      
            if (unparse.contains("? extends testJSure.ModuleAnnotationNode")) {
            	System.out.println("Matched ...");
            }
        	if (unparse.endsWith("VisibilityDrop.T extends VisibilityDrop <? extends #> <: testJSure.VisibilityDrop<? extends testJSure.ModuleAnnotationNode>")) {
        		System.out.println("Looking at wildcard");
        	}
        	*/
            final Map<IRNode, IJavaDeclaredType> sStypes = collectSuperTypes(tEnv, sb.t);
            final Map<IRNode, IJavaDeclaredType> tStypes = collectSuperTypes(tEnv, b.t);
            final Set<IRNode> common = new HashSet<IRNode>(sStypes.keySet());
            common.retainAll(tStypes.keySet());

            for (IRNode n : common) {
              final IJavaDeclaredType g_s = sStypes.get(n);
              final IJavaDeclaredType g_t = tStypes.get(n);
              final List<IJavaType> g_s_params = g_s.getTypeParameters();
              final List<IJavaType> g_t_params = g_t.getTypeParameters();
              final int num = g_s_params.size();
              if (num > 0 && !g_t_params.isEmpty()) { // Both generic
                for (int i = 0; i < num; i++) {
                  final IJavaType s_i =  g_s_params.get(i);
                  final IJavaType t_i = g_t_params.get(i);
                  if (s_i instanceof IJavaWildcardType || t_i instanceof IJavaWildcardType) {
                	  continue;
                  }
                  if (s_i instanceof IJavaCaptureType || t_i instanceof IJavaCaptureType) {
                	  continue; // treated as a wildcard
                  }
                  reduceTypeArgumentEqualityConstraints(bounds, s_i, t_i);
                }
              }
            }
          }
        }
      }
      if (sb.t instanceof InferenceVariable) {
        final InferenceVariable alpha = (InferenceVariable) sb.t;
        for (IEquality e : equalities) {
          // case 3: α = S and T <: α imply ‹T <: S›
          if (e.vars().contains(alpha)) {
            for (IJavaType s : e.values()) {
              reduceSubtypingConstraints(bounds, sb.s, s);
            }
          }
        }
        for (SubtypeBound b : copy(subtypeBounds)) {
          // case 4b
          if (alpha == b.s) {
            reduceSubtypingConstraints(bounds, sb.s, b.t);
          }
        }
      }
      // case 6: α = U and S <: T imply ‹S[α:=U] <: T[α:=U]›
      for (IEquality e : equalities) {
        if (e.vars().isEmpty()) {
          continue;
        }
        for (IJavaType u : e.values()) {
          if (isProperType(u)) {
            // Do the subst for all the equivalent type vars
            Map<InferenceVariable, IJavaType> map = new HashMap<InferenceVariable, IJavaType>(e.vars().size());
            for (InferenceVariable alpha : e.vars()) {
              map.put(alpha, u);
            }
            final IJavaTypeSubstitution s = TypeSubstitution.create(tEnv.getBinder(), map);
            IJavaType sb_s_subst = Util.subst(sb.s, s);
            IJavaType sb_t_subst = Util.subst(sb.t, s);
            if (sb_s_subst != sb.s || sb_t_subst != sb.t) {
              reduceSubtypingConstraints(bounds, sb_s_subst, sb_s_subst);
            }
          }
        }
      }
    }

    private <T> Iterable<T> copy(Collection<T> c) {
      return new ArrayList<T>(c);
    }

    private Map<IRNode, IJavaDeclaredType> collectSuperTypes(ITypeEnvironment tEnv, IJavaReferenceType t) {
      Map<IRNode, IJavaDeclaredType> stypes = new HashMap<IRNode, IJavaDeclaredType>();
      for (IJavaType st : t.getSupertypes(tEnv)) {
        collectSuperTypes(stypes, tEnv, st);
      }
      return stypes;
    }

    private void collectSuperTypes(Map<IRNode, IJavaDeclaredType> stypes, ITypeEnvironment tEnv, IJavaType t) {
      if (t instanceof IJavaDeclaredType) {
        IJavaDeclaredType d = (IJavaDeclaredType) t;
        stypes.put(d.getDeclaration(), d);
      }
      for (IJavaType st : t.getSupertypes(tEnv)) {
        collectSuperTypes(stypes, tEnv, st);
      }
    }

    /**
     * 18.3.2 Bounds Involving Capture Conversion
     * 
     * When a bound set contains a bound of the form G<α1, ..., αn> =
     * capture(G<A1, ..., An>), new bounds are implied and new constraint
     * formulas may be implied, as follows.
     * 
     * Let P1, ..., Pn represent the type parameters of G and let B1, ..., Bn
     * represent the bounds of these type parameters. Let θ represent the
     * substitution [P1:=α1, ..., Pn:=αn]. Let R be a type that is not an
     * inference variable (but is not necessarily a proper type).
     * 
     * ...
     */
    private void incorporateCaptureBound(BoundSet bounds, CaptureBound cb) {
      /*
       * A set of bounds on α1, ..., αn is implied, constructed from the
       * declared bounds of P1, ..., Pn as specified in §18.1.3.
       */
      final IRNode g = cb.s.getDeclaration();
      final IRNode formals;
      if (ClassDeclaration.prototype.includes(g)) {
        formals = ClassDeclaration.getTypes(g);
      } else {
        formals = InterfaceDeclaration.getTypes(g);
      }
      final List<IJavaType> vars = cb.s.getTypeParameters();
      final IJavaType[] varArray = vars.toArray(new IJavaType[vars.size()]);
      final IJavaType[] fBounds = new IJavaType[vars.size()];
      int i = 0;
      for (IRNode f : JJNode.tree.children(formals)) {
        IJavaTypeFormal tf = JavaTypeFactory.getTypeFormal(f);
        fBounds[i] = tf.getExtendsBound(tEnv);
        i++;
      }

      BoundSet newBounds = constructInitialSet(bounds.getCall(), formals, IJavaTypeSubstitution.NULL, varArray);
      IJavaTypeSubstitution theta = newBounds.getInitialVarSubst();
      /* bounds. */merge(newBounds);

      i = 0;
      for (final IJavaType a_i : cb.t.getTypeParameters()) {
        final IJavaType alpha_i = varArray[i];
        final IJavaType b_i = fBounds[i];

        if (a_i instanceof IJavaWildcardType) {
          final IJavaWildcardType wt = (IJavaWildcardType) a_i;

          // Handled together for all 3 cases below
          for (IEquality e : equalities) {
            if (e.vars().contains(alpha_i) && !e.values().isEmpty()) {
              for (IJavaType v : e.values()) {
                if (v instanceof TypeVariable) {
                  TypeVariable t = (TypeVariable) v;
                  if (t.getLowerBound() == null && t.getUpperBound(tEnv) == tEnv.getObjectType()) {
                    continue;
                  }
                }
                bounds.addFalse();
                break;
              }
            }
          }

          if (wt.getUpperBound() != null) {
            /*
             * case 3 • If Ai is a wildcard of the form ? extends T: – αi = R
             * implies the bound false – If Bi is Object, then αi <: R implies
             * the constraint formula ‹T <: R› – If T is Object, then αi <: R
             * implies the constraint formula ‹Bi θ <: R› – R <: αi implies the
             * bound false
             */
        	final IJavaReferenceType t = wt.getUpperBound();
        	  
            for (SubtypeBound sb : copy(subtypeBounds)) {
              if (alpha_i == sb.s && !(sb.t instanceof InferenceVariable)) {
            	final IJavaReferenceType r = sb.t;
                if (b_i == tEnv.getObjectType()) {
                  reduceSubtypingConstraints(bounds, t, r);
                }
                if (t == tEnv.getObjectType()) {
                  reduceSubtypingConstraints(bounds, Util.subst(b_i, theta), r);
                }
              }
              if (alpha_i == sb.t && !(sb.s instanceof InferenceVariable)) {
                bounds.addFalse();
              }
            }
          } else if (wt.getLowerBound() != null) {
            /*
             * case 4 • If Ai is a wildcard of the form ? super T: – αi = R
             * implies the bound false – αi <: R implies the constraint formula
             * ‹Bi θ <: R› – R <: αi implies the constraint formula ‹R <: T›
             */
            for (SubtypeBound sb : subtypeBounds) {
              if (alpha_i == sb.s && !(sb.t instanceof InferenceVariable)) {
                reduceSubtypingConstraints(bounds, Util.subst(b_i, theta), sb.t);
              } else if (alpha_i == sb.t && !(sb.s instanceof InferenceVariable)) {
                reduceSubtypingConstraints(bounds, sb.s, wt.getLowerBound());
              }
            }
          } else {
            /*
             * case 2 • If Ai is a wildcard of the form ?: – αi = R implies the
             * bound false – αi <: R implies the constraint formula ‹Bi θ <: R›
             * – R <: αi implies the bound false
             */
            for (SubtypeBound sb : subtypeBounds) {
              if (alpha_i == sb.s && !(sb.t instanceof InferenceVariable)) {
                reduceSubtypingConstraints(bounds, Util.subst(b_i, theta), sb.t);
              } else if (alpha_i == sb.t && !(sb.s instanceof InferenceVariable)) {
                bounds.addFalse();
              }
            }
          }
        } else {
          // case 1: If Ai is not a wildcard, then the bound αi = Ai is implied.
          bounds.addEqualityBound(alpha_i, a_i);
        }
        i++;
      }
    }

    private void collectVariablesFromBounds(Set<InferenceVariable> vars, Set<? extends Bound<?>> bounds) {
      for (Bound<?> b : bounds) {
        getReferencedInferenceVariables(vars, b.s);
        getReferencedInferenceVariables(vars, b.t);
      }
    }

    private void collectVariablesFromIterable(Set<InferenceVariable> vars, Iterable<? extends Iterable<IJavaReferenceType>> i) {
      for (Iterable<IJavaReferenceType> it : i) {
        for (IJavaReferenceType t : it) {
          getReferencedInferenceVariables(vars, t);
        }
      }
    }

    Set<InferenceVariable> collectVariables() {
      Set<InferenceVariable> vars = new HashSet<InferenceVariable>(thrownSet);
      collectVariablesFromIterable(vars, equalities);
      collectVariablesFromBounds(vars, subtypeBounds);
      collectVariablesFromBounds(vars, captures);
      return vars;
    }

    Set<InferenceVariable> chooseUninstantiated(final Set<InferenceVariable> toResolve, boolean debug) {
      final Set<InferenceVariable> vars = collectVariables();
      final Set<InferenceVariable> uninstantiated = new HashSet<InferenceVariable>(vars);
      final Map<InferenceVariable, IJavaType> instantiations = getInstantiations();
      uninstantiated.removeAll(instantiations.keySet());
      if (uninstantiated.size() < 1) {
        return uninstantiated;
      }
      if (toResolve != null) {
        final Set<InferenceVariable> uninstantiatedAndToResolve = new HashSet<InferenceVariable>(uninstantiated);
        uninstantiatedAndToResolve.retainAll(toResolve);
        if (uninstantiatedAndToResolve.isEmpty()) {
          // toResolve are all resolved already
          return Collections.emptySet();
        }
      }
      VarDependencies deps = computeVarDependencies(debug, instantiations);
      return deps.chooseUninstantiated(uninstantiated);
    }

    private VarDependencies computeVarDependencies(final boolean debug, final Map<InferenceVariable, IJavaType> instantiations) {
      VarDependencies deps = new VarDependencies(debug);
      Set<IJavaType> lhsInCapture = new HashSet<IJavaType>();
      for (CaptureBound b : captures) {
        lhsInCapture.addAll(b.s.getTypeParameters());
        deps.recordDepsForCapture(b);
      }
      deps.recordDepsForEquality(lhsInCapture, equalities);
      deps.recordDependencies(lhsInCapture, subtypeBounds);
      return deps;
    }

    /**
     * Find bounds where a = b
     * 
     * @return
     */
    /*
     * private MultiMap<InferenceVariable,InferenceVariable> collectIdentities()
     * { MultiMap<InferenceVariable,InferenceVariable> rv = new
     * MultiHashMap<InferenceVariable,InferenceVariable> (); for(Bound<?> bound
     * : equalities) { if (bound.s instanceof InferenceVariable && bound.t
     * instanceof InferenceVariable) { InferenceVariable a = (InferenceVariable)
     * bound.s; InferenceVariable b = (InferenceVariable) bound.t; rv.put(a, b);
     * rv.put(b, a); } } return rv; }
     */

    boolean hasNoCaptureBoundInvolvingVars(Set<InferenceVariable> vars) {
      for (CaptureBound b : captures) {
        if (b.refersTo(vars)) {
          return false;
        }
      }
      return true;
    }

    private void removeAssociatedCaptureBounds(Set<InferenceVariable> vars) {
      Iterator<CaptureBound> it = captures.iterator();
      while (it.hasNext()) {
        CaptureBound b = it.next();
        if (b.refersTo(vars)) {
          it.remove();
        }
      }
    }

    /**
     * - If α i has one or more proper lower bounds, L 1 , ..., L k , then T i =
     * lub( L 1 , ..., L k ) (Â§4.10.4).
     * 
     * - Otherwise, if the bound set contains throws α i , and the proper upper
     * bounds of α i are, at most, Exception , Throwable , and Object , then T i
     * = RuntimeException .
     * 
     * - Otherwise, where α i has proper upper bounds U 1 , ..., U k , T i =
     * glb( U 1 , ..., U k ) (Â§5.1.10).
     * 
     * The bounds α 1 = T 1 , ..., α n = T n are incorporated with the current
     * bound set.
     * 
     * If the result does not contain the bound false, then the result becomes
     * the new bound set, and resolution proceeds by selecting a new set of
     * variables to instantiate (if necessary), as described above.
     * 
     * Otherwise, the result contains the bound false, so a second attempt is
     * made to instantiate { α 1 , ..., α n } by performing the step below.
     */
    BoundSet instantiateFromBounds(final Set<InferenceVariable> subset, final Multimap<InferenceVariable, InferenceVariable> equal) {
      final ProperBounds bounds = collectProperBounds(false);
      final BoundSet rv = new BoundSet(this);
      for (InferenceVariable a_i : subset) {
    	final Collection<InferenceVariable> vars = equal.get(a_i);
    	final List<IJavaType> lower = collectBounds(bounds.lowerBounds, a_i, vars);    	  
        if (lower != null && !lower.isEmpty()) {
          rv.addEqualityBound(a_i, utils.getLowestUpperBound(toArray(lower)));
          continue;
        }
        final List<IJavaType> upper = collectBounds(bounds.upperBounds, a_i, vars);
        if (thrownSet.contains(a_i) && qualifiesAsRuntimeException(upper)) {
          rv.addEqualityBound(a_i, tEnv.findJavaTypeByName("java.lang.RuntimeException"));
          continue;
        }
        if (upper != null && !upper.isEmpty()) {
          rv.addEqualityBound(a_i, utils.getGreatestLowerBound(toArray(upper)));
        } else {
          throw new IllegalStateException("what do I do otherwise?"); // TODO
        }
      }
      return rv;
    }

    private boolean qualifiesAsRuntimeException(Collection<IJavaType> upper) {
      if (upper.isEmpty()) {
        return true;
      }
      IJavaType exception = tEnv.findJavaTypeByName("java.lang.Exception");
      IJavaType throwable = tEnv.findJavaTypeByName("java.lang.Throwable");
      IJavaType object = tEnv.getObjectType();
      Set<IJavaType> temp = new HashSet<IJavaType>(upper);
      temp.remove(exception);
      temp.remove(throwable);
      temp.remove(object);
      return temp.isEmpty();
    }

    private IJavaReferenceType[] toArray(Collection<IJavaType> types) {
      IJavaReferenceType[] rv = new IJavaReferenceType[types.size()];
      int i = 0;
      for (IJavaType t : types) {
        rv[i] = (IJavaReferenceType) t;
        i++;
      }
      return rv;
    }

    private ProperBounds collectProperBounds(final boolean onlyProperForLower) {
      final ProperBounds bounds = new ProperBounds();
      for (SubtypeBound b : subtypeBounds) {
        if (b.s instanceof InferenceVariable) {
          if (onlyProperForLower || isProperType(b.t)) {
            bounds.upperBounds.put((InferenceVariable) b.s, b.t);
          }
        } else if (b.t instanceof InferenceVariable) {
          if (isProperType(b.s)) {
            bounds.lowerBounds.put((InferenceVariable) b.t, b.s);
          }
        }
      }
      return bounds;
    }

    // TODO What about duplicates?
    class ProperBounds {
      Multimap<InferenceVariable, IJavaType> lowerBounds = ArrayListMultimap.create();
      Multimap<InferenceVariable, IJavaType> upperBounds = ArrayListMultimap.create();
    }

    /**
     * 
     * @param toInstantiate Modified after return to remove equivalent variables
     * @return
     */
    Multimap<InferenceVariable, InferenceVariable> reduceEquivalences(final Set<InferenceVariable> toInstantiate) {
        final Multimap<InferenceVariable, InferenceVariable> equal = ArrayListMultimap.create();
        for (IEquality e : equalities) {
          if (e.vars().size() > 1) {
            Set<InferenceVariable> matched = new HashSet<InferenceVariable>(e.vars());
            matched.retainAll(toInstantiate);
            if (matched.size() > 1) {
              // Keep only one of the equivalent variables
              final InferenceVariable v = e.getRep();// matched.iterator().next();
              if (!matched.contains(v)) {
                throw new IllegalStateException(); // Not in the same cycle?
              }
              toInstantiate.removeAll(matched);
              toInstantiate.add(v);
              equal.putAll(v, e.vars()); // TODO matched?
            }
          }
        }
        return equal;
    }
    
    /**
     * then let Y 1 , ..., Y n be fresh type variables whose bounds are as
     * follows:
     * 
     * - For all i (1 <= i <= n), if α i has one or more proper lower bounds L 1
     * , ..., L k , then let the lower bound of Y i be lub( L 1 , ..., L k ); if
     * not, then Y i has no lower bound.
     * 
     * - For all i (1 <= i <= n), where α i has upper bounds U 1 , ..., U k ,
     * let the upper bound of Y i be glb( U 1 θ, ..., U k θ), where θ is the
     * substitution [ α 1 := Y 1 , ..., α n := Y n ] .
     * 
     * If the type variables Y 1 , ..., Y n do not have well-formed bounds (that
     * is, a lower bound is not a subtype of an upper bound, or an intersection
     * type is inconsistent), then resolution fails.
     * 
     * Otherwise, for all i (1 <= i <= n), all bounds of the form G< ..., α i ,
     * ... > = capture( G< ... > ) are removed from the current bound set, and
     * the bounds α 1 = Y 1 , ..., α n = Y n are incorporated.
     * 
     * If the result does not contain the bound false, then the result becomes
     * the new bound set, and resolution proceeds by selecting a new set of
     * variables to instantiate (if necessary), as described above.
     * 
     * Otherwise, the result contains the bound false, and resolution fails.
     * @param toInstantiate 
     * @param equal 
     * 
     * @return the new bound set to try to resolve
     */
    BoundSet instantiateViaFreshVars(final Set<InferenceVariable> origSubset, Set<InferenceVariable> toInstantiate, 
    		                         Multimap<InferenceVariable, InferenceVariable> equal, final boolean debug) {
      // HACK use the same type variable for equalities:
      // Remove "duplicate" variables from origSubset
      // Take advantage of incorporation to set the "duplicates" to the same
      // type variable
      final ProperBounds bounds = collectProperBounds(true);
      final Map<InferenceVariable, TypeVariable> y_subst = new HashMap<InferenceVariable, TypeVariable>(toInstantiate.size());
      for (InferenceVariable a_i : toInstantiate) {
        final TypeVariable y_i = new TypeVariable(a_i);// new
                                                       // InferenceVariable(null);
                                                       // // TODO unique?
        y_subst.put(a_i, y_i);

        // HACK also set equivalent variables to the same type variable for
        // substitution
        Collection<InferenceVariable> others = equal.get(a_i);
        if (others != null) {
          for (InferenceVariable v : others) {
            y_subst.put(v, y_i);
          }
        }
      }
      // HACK to handle unresolved variables
      final Map<InferenceVariable, IJavaType> combinedSubst = new HashMap<InferenceVariable, IJavaType>(y_subst);
      combinedSubst.putAll(getInstantiations());
      // Include equalities to canonicalize them?
      for (IEquality e : equalities) {
        InferenceVariable rep;
        if (e.vars().size() > 1 && !combinedSubst.containsKey(rep = e.getRep())) {
          for (InferenceVariable v : e.vars()) {
            if (v == rep) {
              continue;
            }
            combinedSubst.put(v, rep);
          }
        }
      }

      final IJavaTypeSubstitution theta = TypeSubstitution.create(tEnv.getBinder(), combinedSubst);

      final EqualityBound[] newBounds = new EqualityBound[toInstantiate.size()];
      final BoundSet rv = new BoundSet(this);
      int i = 0;
      rv.removeAssociatedCaptureBounds(origSubset);
      // rv.addInferenceVariables(y_subst);

      for (InferenceVariable a_i : toInstantiate) {
        final TypeVariable y_i = y_subst.get(a_i);
        final Collection<InferenceVariable> vars = equal.get(a_i);
        final List<IJavaType> lower = collectBounds(bounds.lowerBounds, a_i, vars);
        IJavaType l_i = null;
        if (lower != null && !lower.isEmpty()) {
          l_i = utils.getLowestUpperBound(toArray(lower));
        }
        final List<IJavaType> upper = collectBounds(bounds.upperBounds, a_i, vars);
        IJavaType u_i = null;
        if (upper != null && !upper.isEmpty()) {
          u_i = utils.getGreatestLowerBound(toArray(theta.substTypes(null, upper)));
        }

        // add new bounds
        if (l_i != null) {
          y_i.setLowerBound((IJavaReferenceType) l_i);
          // rv.addSubtypeBound(l_i, y_i);
        }
        if (u_i != null) {
          y_i.setUpperBound((IJavaReferenceType) u_i);
          // rv.addSubtypeBound(y_i, u_i);
        }
        if (debug) {
          System.out.println("Instantiating "+a_i+" as "+y_i);
        }
        // rv.addEqualityBound(a_i, y_i);
        newBounds[i] = newEqualityBound(a_i, y_i);
        i++;
      }
      for (TypeVariable v : y_subst.values()) {
        if (!v.isBound()) {
          throw new IllegalStateException();
        }

        /* 
         * TODO this isn't right for type formals as bounds?         
        // Check if the bounds are well-formed
        IJavaType l_i = v.getLowerBound();
        IJavaType u_i = v.getUpperBound(tEnv);
        if (l_i != null && u_i != null && !l_i.isSubtype(tEnv, u_i)) {
          l_i.isSubtype(tEnv, u_i);
          return null;
        }
        */
        // TODO how to check for intersection type?
      }
      rv.incorporate(newBounds);
      return rv;
    }

    private List<IJavaType> collectBounds(Multimap<InferenceVariable, IJavaType> bounds, InferenceVariable a_i, Collection<InferenceVariable> vars) {
      if (vars.isEmpty()) {
    	  return new ArrayList<>(bounds.get(a_i));
      }
      final List<IJavaType> rv = new ArrayList<>();
      for(InferenceVariable v : vars) {
    	  Collection<IJavaType> temp = bounds.get(v);
    	  if (temp != null) {
    		  rv.addAll(temp);
    	  }
      }
      return rv;
    }
    
    // a = T, T = a, a <: T, T <: a
    BoundSubset findAssociatedBounds(InferenceVariable a) {
      BoundSubset rv = new BoundSubset();
      Equality e = equalities.find(a);
      if (e != null) {
        rv.equalities.add(e);
      }
      for (SubtypeBound b : subtypeBounds) {
        if (b.s == a) {
          rv.upperBounds.add(b);
        } else if (b.t == a) {
          rv.lowerBounds.add(b);
        }
      }
      return rv;
    }

    void useUncheckedConversion() {
      usedUncheckedConversion = true;
    }

    boolean usedUncheckedConversion() {
      return usedUncheckedConversion;
    }
  }

  @SuppressWarnings("unchecked")
  static <T extends IJavaType> T eliminateTypeVariables(TypeUtils utils, T t) {
    if (t == null) {
      return null;
    }
    if (t instanceof TypeVariable) {
      TypeVariable v = (TypeVariable) t;
      IJavaReferenceType lb = eliminateTypeVariables(utils, v.getLowerBound());
      IJavaReferenceType ub = eliminateTypeVariables(utils, v.getUpperBound(utils.getTypeEnv()));
      return (T) utils.getGreatestLowerBound(lb, ub);
      // IJavaType lub = utils.getLowestUpperBound(v.getLowerBound(),
      // v.getUpperBound(tEnv));
    }
    if (t instanceof IJavaDeclaredType) {
      final IJavaDeclaredType dt = (IJavaDeclaredType) t;
      final int num = dt.getTypeParameters().size();
      if (num == 0) {
        return t;
      }
      List<IJavaType> params = new ArrayList<IJavaType>(num);
      for (IJavaType p : dt.getTypeParameters()) {
        params.add(eliminateTypeVariables(utils, p));
      }
      if (params.equals(dt.getTypeParameters())) {
        return t;
      }
      return (T) JavaTypeFactory.getDeclaredType(dt.getDeclaration(), params, dt.getOuterType());
    }
    // TODO how do I do this?
    return t;
  }
  
  /**
   * 18.4 Resolution
   * 
   * Given a set of inference variables to resolve, let V be the union of this
   * set and all variables upon which the resolution of at least one variable in
   * this set depends.
   * 
   * If every variable in V has an instantiation, then resolution succeeds and
   * this procedure terminates.
   * 
   * Otherwise, let { α 1 , ..., α n } be a non-empty subset of uninstantiated
   * variables in V such that i) for all i (1 <= i <= n), if α i depends on the
   * resolution of a variable Î², then either Î² has an instantiation or there
   * is some j such that Î² = α j ; and ii) there exists no non-empty proper
   * subset of { α 1 , ..., α n } with this property. Resolution proceeds by
   * generating an instantiation for each of α 1 , ..., α n based on the bounds
   * in the bound set:
   * 
   * - If the bound set does not contain a bound of the form G< ..., α i , ... >
   * = capture( G< ... > ) for all i (1 <= i <= n), then a candidate
   * instantiation T i is defined for each α i :
   * 
   * ...
   * 
   * - If the bound set contains a bound of the form G< ..., α i , ... > =
   * capture( G< ... > ) for some i (1 <= i <= n), or;
   * 
   * If the bound set produced in the step above contains the bound false;
   * 
   * ...
   * 
   * Resolve the specified variables, as well as any others that they are
   * dependent on If 'toResolve' is null, keep trying if there are
   * uninstantiated variables
   */
  static BoundSet resolve(final BoundSet bounds, final Set<InferenceVariable> toResolve, boolean debug) {
    if (bounds == null || bounds.isFalse) {
      return null;
    }
    Set<InferenceVariable> lastSubset = null;
    Set<InferenceVariable> subset = bounds.chooseUninstantiated(toResolve, debug);
    BoundSet current = bounds, last = bounds;
    while (!subset.isEmpty()) {
      BoundSet next = resolveVariables(current, subset, debug);
      if (next == null || next.isFalse) {
    	current.debug();
    	resolveVariables(current, subset, true);
        return null;
      }
      last = current;
      current = next;
      lastSubset = subset;
      subset = next.chooseUninstantiated(toResolve, debug);
      if (lastSubset.equals(subset)) {
    	  // Nothing more that we can do?
    	  break;
      }
    }
    return current;
  }

  /**
   * Try to resolve the specific subset of variables.
   */
  static BoundSet resolveVariables(final BoundSet bounds, final Set<InferenceVariable> origSubset, final boolean debug) {
    if (origSubset.isEmpty()) {
      return bounds; // All instantiated
    }
    final Set<InferenceVariable> toInstantiate = new HashSet<InferenceVariable>(origSubset);
    final Multimap<InferenceVariable, InferenceVariable> equal = bounds.reduceEquivalences(toInstantiate);
    if (bounds.hasNoCaptureBoundInvolvingVars(origSubset)) {
      BoundSet fresh = bounds.instantiateFromBounds(toInstantiate, equal);
      // BoundSet rv = resolve(fresh);
      if (fresh != null && !fresh.isFalse) {
        return fresh;
      }
      // Otherwise, try below
      //System.out.println("Couldn't resolve from bounds");
    }
    BoundSet fresh = bounds.instantiateViaFreshVars(origSubset, toInstantiate, equal, debug);
    // return resolve(fresh);
    return fresh;
  }

  static class Dependencies<T extends Dependable> {
    final Multimap<T, T> dependsOn = HashMultimap.create();
	final boolean debug;

    Dependencies(boolean debug) {
    	this.debug = debug;
    }
    
    protected void markDependsOn(T alpha, T beta) {
      if (debug) {
    	  final String unparse = alpha.toString();
    	  if (unparse.contains("Collectors.K") || unparse.contains("Collectors.K")) {
    		  System.out.println("Var depends on "+beta);
    	  }
      }
      dependsOn.put(alpha, beta);
    }

    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	for(T a : dependsOn.keySet()) {
    		sb.append(a).append(" -> ");
    		boolean first = true;
    		for(T b : dependsOn.get(a)) {
    			if (first) {
    				first = false;
    			} else {
    				sb.append('\t');
    			}
    			sb.append(b).append('\n');    			
    		}
    	}
    	return sb.toString();
    }
    
    public Set<T> chooseUninstantiated(final Set<T> uninstantiated) {
      computeStronglyConnectedComponents();

      final List<T> ordering = computeTopologicalSort();
      Collections.reverse(ordering); // FIX to match dependOn
      
      final Set<T> rv = new HashSet<T>();
      boolean foundComponent = false;
      // Find the first component with uninstantiated vars
      for (final T v : ordering) {
        final Collection<T> vars = components.get(v);
        for (final T w : vars) {
          if (uninstantiated.contains(w)) {
            rv.add(w);
            foundComponent = true;
          }
          // TODO do i need to check that all the variables in a component are
          // uninstantiated?
        }
        if (foundComponent) {
          return rv;
        }
      }
      return Collections.emptySet();
    }

    int index = 0;
    final Stack<T> s = new Stack<T>();
    final Multimap<T, T> components = ArrayListMultimap.create();

    // http://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
    private void computeStronglyConnectedComponents() {
      // Reset info
      for (T v : dependsOn.keySet()) {
        v.setIndex(-1);
        v.setLowLink(Integer.MAX_VALUE);
      }
      /*
       * algorithm tarjan is input: graph G = (V, E) output: set of strongly
       * connected components (sets of vertices)
       * 
       * index := 0 S := empty for each v in V do if (v.index is undefined) then
       * strongconnect(v) end if end for
       */
      index = 0;
      s.clear();
      components.clear();

      for (T v : dependsOn.keySet()) {
        if (v.getIndex() < 0) {
          strongConnect(v);
        }
      }
    }

    private void strongConnect(final T v) {
      /*
       * function strongconnect(v) // Set the depth index for v to the smallest
       * unused index v.index := index v.lowlink := index index := index + 1
       * S.push(v)
       */
      v.setIndex(index);
      v.setLowLink(index);
      index++;
      s.push(v);

      /*
       * Consider successors of v for each (v, w) in E do if (w.index is
       * undefined) then // Successor w has not yet been visited; recurse on it
       * strongconnect(w) v.lowlink := min(v.lowlink, w.lowlink) else if (w is
       * in S) then // Successor w is in stack S and hence in the current SCC
       * v.lowlink := min(v.lowlink, w.index) end if end for
       */
      for (T w : dependsOn.get(v)) {
        if (w.getIndex() < 0) {
          strongConnect(w);
          v.setLowLink(Math.min(v.getLowLink(), w.getLowLink()));
        } else if (s.contains(w)) {
          v.setLowLink(Math.min(v.getLowLink(), w.getIndex()));
        }
      }

      /*
       * If v is a root node, pop the stack and generate an SCC if (v.lowlink =
       * v.index) then start a new strongly connected component repeat w :=
       * S.pop() add w to current strongly connected component until (w = v)
       * output the current strongly connected component end if
       */
      if (v.getLowLink() == v.getIndex()) {
        T w = null;
        do {
          w = s.pop();
          components.put(v, w);
        } while (v != w);
      }
      // end function
    }

    // http://en.wikipedia.org/wiki/Topological_sorting
    private List<T> computeTopologicalSort() {
      /*
       * L â†� Empty list that will contain the sorted nodes while there are
       * unmarked nodes do select an unmarked node n visit(n)
       */
      final List<T> l = new LinkedList<T>();
      for (T v : components.keySet()) {
        v.setIndex(-1);
      }
      final Set<T> toVisit = new HashSet<T>(components.keySet());

      while (!toVisit.isEmpty()) {
        T n = toVisit.iterator().next();
        visitForSort(l, toVisit, n);
      }
      return l;
    }

    private void visitForSort(final List<T> l, final Set<T> toVisit, final T n) {
      /*
       * function visit(node n) if n has a temporary mark then stop (not a DAG)
       * if n is not marked (i.e. has not been visited yet) then mark n
       * temporarily for each node m with an edge from n to m do visit(m) mark n
       * permanently unmark n temporarily add n to head of L
       */
      if (n.getIndex() == 0) {
        throw new IllegalStateException("Not a DAG");
      }
      if (n.getIndex() < 0) {
        toVisit.remove(n);
        n.setIndex(0);
        for (T m : dependsOn.get(n)) {
          if (m != n && components.containsKey(m)) { // filter to the component
                                                     // roots
            visitForSort(l, toVisit, m);
          }
        }
        n.setIndex(100);
        l.add(0, n);
      }
    }
  }

  class VarDependencies extends Dependencies<InferenceVariable> {
	VarDependencies(boolean debug) {
		super(debug);
	}
	  
    /**
     * 18.4 Resolution
     * 
     * Given a bound set that does not contain the bound false, a subset of the
     * inference variables mentioned by the bound set may be resolved. This
     * means that a satisfactory instantiation may be added to the set for each
     * inference variable, until all the requested variables have
     * instantiations.
     * 
     * Dependencies in the bound set may require that the variables be resolved
     * in a particular order, or that additional variables be resolved.
     * Dependencies are specified as follows:
     * 
     * - Given a bound of one of the following forms, where T is either an
     * inference variable Î² or a type that mentions Î²: - α = T - α <: T - T =
     * α - T <: α
     * 
     * If α appears on the left-hand side of another bound of the form G< ...,
     * α, ... > = capture( G< ... > ), then Î² depends on the resolution of α.
     * Otherwise, α depends on the resolution of Î².
     * 
     * - An inference variable α appearing on the left-hand side of a bound of
     * the form G< ..., α, ... > = capture( G< ... > ) depends on the resolution
     * of every other inference variable mentioned in this bound (on both sides
     * of the = sign).
     * 
     * - An inference variable α depends on the resolution of an inference
     * variable Î² if there exists an inference variable Î³ such that α depends
     * on the resolution of Î³ and Î³ depends on the resolution of Î².
     * 
     * - An inference variable α depends on the resolution of itself.
     * 
     * @param lhsInCapture
     */
    void recordDependencies(Set<IJavaType> lhsInCapture, Set<? extends Bound<?>> bounds) {
      final Set<InferenceVariable> temp = new HashSet<InferenceVariable>();
      for (Bound<?> b : bounds) {
        if (b.s instanceof InferenceVariable) {
          recordDepsForBound(lhsInCapture, temp, (InferenceVariable) b.s, b.t);
        }
        if (b.t instanceof InferenceVariable) {
          recordDepsForBound(lhsInCapture, temp, (InferenceVariable) b.t, b.s);
        }
      }
    }

    /**
     * • Given a bound of one of the following forms, where T is either an
     * inference variable β or a type that mentions β:
     *
     * – α = T – α <: T – T = α – T <: α
     *
     * If α appears on the left-hand side of another bound of the form G< ...,
     * α, ... > = capture( G< ... > ), then β depends on the resolution of α.
     * Otherwise, α depends on the resolution of β.
     */
    private void recordDepsForBound(Set<IJavaType> lhsInCapture, Set<InferenceVariable> temp, InferenceVariable alpha,
        IJavaReferenceType t) {
      getReferencedInferenceVariables(temp, t);
      if (lhsInCapture.contains(alpha)) {
        for (InferenceVariable beta : temp) {
          markDependsOn(beta, alpha);
        }
      } else {
        for (InferenceVariable beta : temp) {
          markDependsOn(alpha, beta);
        }
      }
      markDependsOn(alpha, alpha);
      temp.clear();
    }

    /**
     * Handles expanding the various equalities encoded
     */
    public void recordDepsForEquality(Set<IJavaType> lhsInCapture, Equalities equalities) {
      final Set<InferenceVariable> temp = new HashSet<InferenceVariable>();
      for (IEquality e : equalities) {
        if (e.isTrivial()) {
          continue;
        }
        for (InferenceVariable v : e.vars()) {
          // Mark the interdependence between the different variables
          for (InferenceVariable v2 : e.vars()) {
            markDependsOn(v, v2);
          }
          for (IJavaReferenceType t : e.values()) {
            recordDepsForBound(lhsInCapture, temp, (InferenceVariable) v, t);
          }
        }
      }
    }

    /**
     * • An inference variable α appearing on the left-hand side of a bound of
     * the form G< ..., α, ... > = capture( G< ... > ) depends on the resolution
     * of every other inference variable mentioned in this bound (on both sides
     * of the = sign).
     */
    void recordDepsForCapture(CaptureBound b) {
      final Set<InferenceVariable> vars = new HashSet<InferenceVariable>();
      getReferencedInferenceVariables(vars, b.s);
      getReferencedInferenceVariables(vars, b.t);

      // For each parameter on the left-hand side
      for (IJavaType param : b.s.getTypeParameters()) {
        if (param instanceof InferenceVariable) {
          InferenceVariable alpha = (InferenceVariable) param;
          for (InferenceVariable beta : vars) {
            markDependsOn(alpha, beta);
          }
        }
      }
    }
  }

  static boolean isInferenceVariable(IJavaType t) {
    return t instanceof InferenceVariable;
  }

  IJavaReferenceType box(IJavaPrimitiveType formalP) {
    IJavaDeclaredType boxedEquivalent = JavaTypeFactory.getCorrespondingDeclType(tEnv, formalP);
    return boxedEquivalent;
  }

  /**
   * @return true if T is a parameterized type of the form G<T 1 , ..., T n > ,
   *         and there exists no type of the form G< ... > that is a supertype
   *         of S , but the raw type G is a supertype of S
   */
  boolean hasRawSuperTypeOf(IJavaType s, IJavaType t) {
    if (t instanceof IJavaDeclaredType) {
      IJavaDeclaredType g = (IJavaDeclaredType) t;
      if (g.getTypeParameters().size() > 0) {
        // g is parameterized
        return onlyHasRawSuperTypeOf(s, g.getDeclaration());
      }
    }
    return false;
  }

  boolean onlyHasRawSuperTypeOf(IJavaType s, IRNode g) {
    if (s instanceof IJavaDeclaredType) {
      IJavaDeclaredType gs = (IJavaDeclaredType) s;
      if (gs.getDeclaration().equals(g) && gs.isRawType(tEnv)) {
        return true;
      }
    }
    for (IJavaType st : s.getSupertypes(tEnv)) {
      if (onlyHasRawSuperTypeOf(st, g)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 18.2 Reduction
   * 
   * Reduction is the process by which a set of constraint formulas (Â§18.1.2)
   * is simplified to produce a bound set (Â§18.1.3).
   * 
   * Each constraint formula is considered in turn. The rules in this section
   * specify how the formula is reduced to one or both of:
   * 
   * - A bound or bound set, which is to be incorporated with the "current"
   * bound set. Initially, the current bound set is empty.
   * 
   * - Further constraint formulas, which are to be reduced recursively.
   * Reduction completes when no further constraint formulas remain to be
   * reduced.
   */
  void reduceConstraintFormula(BoundSet bounds, ConstraintFormula f) {
    switch (f.constraint) {
    case IS_COMPATIBLE:
      if (f.expr != null) {
        reduceExpressionCompatibilityConstraints(bounds, f.expr, f.type);

      } else {
        reduceTypeCompatibilityConstraints(bounds, f.stype, f.type);
      }
      break;
    case IS_SUBTYPE:
      reduceSubtypingConstraints(bounds, f.stype, f.type);
      break;
    case IS_CONTAINED_BY_TYPE_ARG:
      reduceTypeArgContainmentConstraints(bounds, f.stype, f.type);
      break;
    case IS_SAME:
      reduceTypeEqualityConstraints(bounds, f.stype, f.type);
      break;
    case THROWS:
      if (LambdaExpression.prototype.includes(f.expr)) {
        reduceLambdaCheckedExceptionConstraints(bounds, f.expr, f.type);
      } else {
        reduceMethodRefCheckedExceptionConstraints(bounds, f.expr, f.type);
      }
      break;
    default:
      throw new IllegalStateException();
    }
  }

  /**
   * 18.2.1 Expression Compatibility Constraints
   * 
   * A constraint formula of the form <Expression -> T > is reduced as follows:
   * 
   * - If T is a proper type, the constraint reduces to true if the expression
   * is compatible in a loose invocation context with T (Â§5.3), and false
   * otherwise.
   * 
   * - Otherwise, if the expression is a standalone expression (Â§15.2) of type
   * S , the constraint reduces to < S -> T >.
   * 
   * - Otherwise, the expression is a poly expression (Â§15.2). The result
   * depends on the form of the expression:
   * 
   * - If the expression is a parenthesized expression of the form ( Expression'
   * ) , the constraint reduces to <Expression' -> T >.
   * 
   * - If the expression is a class instance creation expression or a method
   * invocation expression, the constraint reduces to the bound set B 3 which
   * would be used to determine the expression's invocation type when targeting
   * T , as defined in Â§18.5.2. (For a class instance creation expression, the
   * corresponding "method" used for inference is defined in Â§15.9.3). This
   * bound set may contain new inference variables, as well as dependencies
   * between these new variables and the inference variables in T .
   * 
   * - If the expression is a conditional expression of the form e 1 ? e 2 : e 3
   * , the constraint reduces to two constraint formulas, < e 2 -> T > and < e 3
   * -> T >.
   * 
   * - If the expression is a lambda expression or a method reference
   * expression, the result is specified below.
   * 
   * @param bounds
   */
  void reduceExpressionCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
    if (isProperType(t)) {
      final String unparse = DebugUnparser.toString(e);
      if ("mc".equals(unparse)) {
    	  System.out.println("Comparing "+unparse+" -> "+t);
      }
      if (mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, t, e,
          null/* tEnv.getBinder().getJavaType(e) */)) {
        bounds.addTrue();
      } else {
    	if (bounds.debug) {
    		System.out.println();
    	}
        mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, t, e,
            null/* tEnv.getBinder().getJavaType(e) */);
        bounds.addFalse();
      }
    } else if (!mb.isPolyExpression(e)) {
      /*
      final String unparse = DebugUnparser.toString(e);
      if ("match.gpe".equals(unparse)) {
    	  System.out.println("Checking "+unparse);
      }
      */
      IJavaType s = tEnv.getBinder().getJavaType(e);
      IJavaType captured = JavaTypeVisitor.captureWildcards(tEnv.getBinder(), s);
      reduceTypeCompatibilityConstraints(bounds, captured, t);
    } else {
      Operator op = JJNode.tree.getOperator(e);
      if (ParenExpression.prototype.includes(op)) {
        reduceExpressionCompatibilityConstraints(bounds, ParenExpression.getOp(e), t);
      } else if (NewExpression.prototype.includes(op) || MethodCall.prototype.includes(op)) {
        try {
          // Need to substitute for inference variables used here
          final Triple<CallState, MethodBinding8, BoundSet> result = computeInvocationBounds((CallInterface) op, e, t);
          final BoundSet b_3 = result.third();
          bounds.mergeWithSubst(b_3, bounds.getInitialVarSubst());
        } catch (NoArgs e1) {
          throw new IllegalStateException("No arguments for " + DebugUnparser.toString(e));
        }
      } else if (ConditionalExpression.prototype.includes(op)) {
        reduceExpressionCompatibilityConstraints(bounds, ConditionalExpression.getIffalse(e), t);
        reduceExpressionCompatibilityConstraints(bounds, ConditionalExpression.getIftrue(e), t);
      } else if (LambdaExpression.prototype.includes(op)) {
        reduceLambdaCompatibilityConstraints(bounds, e, t);
      } else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
        reduceMethodReferenceCompatibilityConstraints(bounds, e, t);
      } else {
        throw new IllegalStateException();
      }
    }
  }

  private Triple<CallState, MethodBinding8, BoundSet> computeInvocationBounds(CallInterface c, final IRNode e, final IJavaType t)
      throws NoArgs {
    // Need to restore the binding to how it looked before I added the type
    // substitution for the method's parameters
    final MethodBinding8 b = (MethodBinding8) tEnv.getBinder().getIBinding(e);
    final CallState call = CallState.create(tEnv.getBinder(), e, b);
    Pair<MethodBinding, BoundSet> pair = recomputeB_2(call, b);
    MethodBinding m = pair.first();
    IJavaType r = m.getReturnType(tEnv, m.mkind == Kind.CONSTRUCTOR);// true); // TODO why
                                                         // no subst?
    BoundSet b_3 = computeB_3(call, r, pair.second(), t);
    return new Triple<CallState, MethodBinding8, BoundSet>(call, b, b_3);
  }
  
  // Note that the method binding returned can be pre-substituted to match the BoundSet?
  // (i.e. set the receiver type / subst based on the inference variables for that call?
  public Pair<MethodBinding, BoundSet> recomputeB_2(ICallState call, MethodBinding8 b) {
	final String unparse = call.toString();
	if (unparse != null && unparse.equals("stream.map(#:: <> create).sorted((# # r1, # # r2) -> -1 * #.compare#)")) {
		System.out.println("Recomputing B2 for "+unparse);
	}	
	final MethodBinding8 reformulatedB = reformulateBindingForInvocationBounds(call.getReceiverOrNull(), b);
    if (b.getInitialBoundSet() != null) {
      return new Pair<MethodBinding, BoundSet>(reformulatedB, b.getInitialBoundSet());
    }
    final MethodBinding8 m;
    if (reformulatedB == b) {
    	/*
        final IBinding newB = IBinding.Util.makeMethodBinding(b, null, JavaTypeSubstitution.create(tEnv, b.getContextType()), null, tEnv);
        final MethodBinding temp = new MethodBinding(newB);
        m = MethodBinding8.create(call, temp, tEnv, null, b.kind);
        */
    	// TODO why is this using the context type, instead of the receiver type?
    	m = MethodBinding8.create(call, b, tEnv, JavaTypeSubstitution.create(tEnv, b.getContextType()), b.kind);
    } else {
    	m = reformulatedB;
    	call = call.replaceReceiverType(m.getReceiverType());
    }
    BoundSet b_2 = b_2Cache.get(new Pair<IRNode, IRNode>(call.getNode(), m.bind.getNode()));
    if (b_2 == null) {
      b_2 = inferForInvocationApplicability(call, m, b.getInvocationKind(), false);
    }
    /*
    if (b_2 == null) {
      System.out.println("Still no boundset?");
      b_2 = inferForInvocationApplicability(call, m, b.getInvocationKind(), true);
    }
    */
    return new Pair<MethodBinding, BoundSet>(m, b_2);
  }

  /*
   * Modifies the receiver type of the given binding to use inference variables
   */
  // TODO do I need to cache this somehow?
  private MethodBinding8 reformulateBindingForInvocationBounds(final IRNode rec, final MethodBinding8 origB) {
	if (rec == null) {
	  return origB;
	}
	final Operator op = JJNode.tree.getOperator(rec);
	if (SomeFunctionCall.prototype.includes(op)) {
	  // Receiver is something that originally checked for invocation applicability
	  IBinding b = tEnv.getBinder().getIBinding(rec);
	  if (!(b instanceof MethodBinding8)) {
		return origB;
	  }
	  MethodBinding8 m = (MethodBinding8) b;
	  CallState recCall = CallState.create(tEnv.getBinder(), rec, m);
	  Pair<MethodBinding, BoundSet> result = recomputeB_2(recCall, m);
	  IJavaTypeSubstitution subst = result.second().getInitialVarSubst();
	  // Substitute in appropriate inference variables
	  return origB.substReceiver(tEnv, subst);
	}
	return origB;
  }
  
  /**
   * A constraint formula of the form <LambdaExpression -> T >, where T mentions
   * at least one inference variable, is reduced as follows:
   * 
   * - If T is not a functional interface type (Â§9.8), the constraint reduces
   * to false.
   * 
   * - Otherwise, let T' be the ground target type derived from T , as specified
   * in Â§15.27.3. If Â§18.5.3 is used to derive a functional interface type
   * which is parameterized, then the test that F<A' 1 , ..., A' m > is a
   * subtype of F<A 1 , ..., A m > is not performed (instead, it is asserted
   * with a constraint formula below). Let the target function type for the
   * lambda expression be the function type of T' . Then:
   * 
   * - If no valid function type can be found, the constraint reduces to false.
   * 
   * (see below)
   */
  void reduceLambdaCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
    final Set<InferenceVariable> vars = bounds.collectVariables();
    if (!refersTo((IJavaReferenceType) t, vars)) {
      return; // TODO is this right?
    }
    IJavaFunctionType ft = tEnv.isFunctionalType(t);
    if (ft == null) {
      bounds.addFalse();
      return;
    }
    /**
     * If Â§18.5.3 is used to derive a functional interface type which is
     * parameterized, then the test that F<A' 1 , ..., A' m > is a subtype of
     * F<A 1 , ..., A m > is not performed (instead, it is asserted with a
     * constraint formula below).
     */
    final IJavaType t_prime = mb.computeGroundTargetType(e, t, false);
    if (t_prime == null) {
      mb.computeGroundTargetType(e, t, false);
    }
    final IJavaFunctionType ft_prime = tEnv.isFunctionalType(t_prime);
    if (ft_prime == null) {
      bounds.addFalse();
      return;
    }
    /*
     * - Otherwise, the congruence of LambdaExpression with the target function
     * type is asserted as follows:
     * 
     * > If the number of lambda parameters differs from the number of parameter
     * types of the function type, the constraint reduces to false.
     * 
     * > If the lambda expression is implicitly typed and one or more of the
     * function type's parameter types is not a proper type, the constraint
     * reduces to false.
     * 
     * > If the function type's result is void and the lambda body is neither a
     * statement expression nor a void-compatible block, the constraint reduces
     * to false.
     * 
     * > If the function type's result is not void and the lambda body is a
     * block that is not value-compatible, the constraint reduces to false.
     */
    final IRNode params = LambdaExpression.getParams(e);
    final int numParams = JJNode.tree.numChildren(params);
    if (numParams != ft_prime.getParameterTypes().size()) {
      bounds.addFalse();
      return;
    }
    final boolean isImplicit = MethodBinder8.isImplicitlyTypedLambda(e);
    if (isImplicit) {
      for (IJavaType pt : ft_prime.getParameterTypes()) {
        if (!isProperType(pt)) {
          bounds.addFalse();
          return;
        }
      }
    }
    final IRNode body = LambdaExpression.getBody(e);
    if (ft_prime.getReturnType() instanceof IJavaVoidType) {
      if (!mb.isVoidCompatible(body)) {
        bounds.addFalse();
        return;
      }
    } else {
      if (!mb.isReturnCompatible(body)) {
        bounds.addFalse();
        return;
      }
    }

    /*
     * > Otherwise, the constraint reduces to all of the following constraint
     * formulas:
     * 
     * Â» If the lambda parameters have explicitly declared types F 1 , ..., F n
     * and the function type has parameter types G 1 , ..., G n , then i) for
     * all i (1 <= i <= n), < F i = G i >, and ii) < T' <: T >.
     * 
     * Â» If the function type's return type is a (non- void ) type R , assume
     * the lambda's parameter types are the same as the function type's
     * parameter types. Then:
     * 
     * - If R is a proper type, and if the lambda body or some result expression
     * in the lambda body is not compatible in an assignment context with R ,
     * then false.
     * 
     * - Otherwise, if R is not a proper type, then where the lambda body has
     * the form Expression, the constraint <Expression -> R >; or where the
     * lambda body is a block with result expressions e 1 , ..., e m , for all i
     * (1 <= i <= m), < e i -> R >.
     */
    if (!isImplicit) {
      int i = 0;
      for (final IJavaType f_i : getLambdaParamTypes(params)) {
        final IJavaType g_i = ft_prime.getParameterTypes().get(i);
        reduceTypeEqualityConstraints(bounds, f_i, g_i);
        i++;
      }
      reduceSubtypingConstraints(bounds, t_prime, t);
    }
    final IJavaType r = ft_prime.getReturnType();
    if (!(r instanceof IJavaVoidType)) {
      checkLambdaReturnExpressions(bounds, body, r, new LambdaCache(tEnv, bounds, e, t_prime, ft_prime));
    }
  }

  /*
   * As above:
   * 
   * - If R is a proper type, and if the lambda body or some result expression
   * in the lambda body is not compatible in an assignment context with R ,
   * then false.
   * 
   * - Otherwise, if R is not a proper type, then where the lambda body has
   * the form Expression, the constraint <Expression -> R >; or where the
   * lambda body is a block with result expressions e 1 , ..., e m , for all i
   * (1 <= i <= m), < e i -> R >.
   */
  private void checkLambdaReturnExpressions(BoundSet bounds, final IRNode body, final IJavaType r, LambdaCache cache) {
    final UnversionedJavaBinder ujb;
    if (tEnv.getBinder() instanceof UnversionedJavaBinder) {
      ujb = (UnversionedJavaBinder) tEnv.getBinder();
    } else {
      throw new NotImplemented();
    }
    final JavaCanonicalizer.IBinderCache old = ujb.setBinderCache(cache);
    try {
      if (isProperType(r)) {
        for (IRNode re : findResultExprs(body)) {
          if (!mb.isAssignCompatible(r, re)) {
            bounds.addFalse();
            return;
          }
        }
      } else {
        for (IRNode re : findResultExprs(body)) {
          reduceExpressionCompatibilityConstraints(bounds, re, r);
        }
      }
    } finally {
      ujb.setBinderCache(old);
    }
  }

  private Iterable<IJavaType> getLambdaParamTypes(IRNode params) {
    List<IJavaType> rv = new ArrayList<IJavaType>();
    for (IRNode pd : Parameters.getFormalIterator(params)) {
      rv.add(tEnv.getBinder().getJavaType(pd));
    }
    return rv;
  }

  /**
   * A constraint formula of the form <MethodReference -> T >, where T mentions
   * at least one inference variable, is reduced as follows:
   * 
   * - If T is not a functional interface type, or if T is a functional
   * interface type that does not have a function type (Â§9.9), the constraint
   * reduces to false.
   * 
   * - Otherwise, if there does not exist a potentially applicable method for
   * the method reference when targeting T , the constraint reduces to false.
   * 
   * - Otherwise, if the method reference is exact (Â§15.13.1), then let P 1 ,
   * ..., P n be the parameter types of the function type of T , and let F 1 ,
   * ..., F k be the parameter types of the potentially applicable method. The
   * constraint reduces to a new set of constraints, as follows:
   * 
   * (see below)
   * 
   * - Otherwise, the method reference is inexact, and: (see below)
   */
  void reduceMethodReferenceCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
    final IRNode recv;
    final String name;
    final IRNode targs;
    if (MethodReference.prototype.includes(e)) {
      recv = MethodReference.getReceiver(e);
      name = MethodReference.getMethod(e);
      targs = MethodReference.getTypeArgs(e);
    } else {
      recv = ConstructorReference.getType(e);
      name = "new";
      targs = ConstructorReference.getTypeArgs(e);
    }
    final IJavaFunctionType ft = mb.methodRefHasPotentiallyApplicableMethods(t, recv, name);
    if (ft == null) {
      bounds.addFalse();
      return;
    }
    final MethodBinding b = mb.getExactMethodReference(e);
    final List<IJavaType> p = ft.getParameterTypes();
    if (b != null) {
      /*
       * - In the special case where n = k+1, the parameter of type P 1 is to
       * act as the target reference of the invocation. The method reference
       * expression necessarily has the form ReferenceType :: [TypeArguments]
       * Identifier. The constraint reduces to < P 1 <: ReferenceType> and, for
       * all i (2 <= i <= n), < P i -> F i-1 >. In all other cases, n = k, and
       * the constraint reduces to, for all i (1 <= i <= n), < P i -> F i >.
       */
      int i;
      if (p.size() == b.numFormals + 1) {
        reduceSubtypingConstraints(bounds, p.get(0), tEnv.getBinder().getJavaType(recv));
        i = 1;
      } else {
        i = 0;
      }
      for (IJavaType f_i : b.getParamTypes(tEnv.getBinder(), b.numFormals, false)) {
        reduceTypeCompatibilityConstraints(bounds, p.get(i), f_i);
        i++;
      }
      /*
       * - If the function type's result is not void , let R be its return type.
       * Then, if the result of the potentially applicable compile-time
       * declaration is void , the constraint reduces to false. Otherwise, the
       * constraint reduces to < R ' -> R >, where R ' is the result of applying
       * capture conversion (Â§5.1.10) to the return type of the potentially
       * applicable compile-time declaration.
       */
      final IJavaType r = ft.getReturnType();
      if (!(r instanceof IJavaVoidType)) {
        final IJavaType r_prime = JavaTypeVisitor.captureWildcards(tEnv.getBinder(), b.getReturnType(tEnv));
        if (r_prime instanceof IJavaVoidType) {
          bounds.addFalse();
        } else {
          reduceTypeCompatibilityConstraints(bounds, r_prime, r);
        }
      }
    } else {
      /*
       * - If one or more of the function type's parameter types is not a proper
       * type, the constraint reduces to false.
       */
      for (IJavaType pt : ft.getParameterTypes()) {
        if (!isProperType(pt)) {
          bounds.addFalse();
          return;
        }
      }
      /*
       * - Otherwise, a search for a compile-time declaration is performed, as
       * specified in Â§15.13.1. If there is no compile-time declaration for the
       * method reference, the constraint reduces to false. Otherwise, there is
       * a compile-time declaration, and:
       */
      final RefState ref = mb.new RefState(ft, e);
      final MethodBinding8 ctd = mb.findCompileTimeDeclForRef(ft, ref);
      if (ctd == null) {
        mb.findCompileTimeDeclForRef(ft, ref);
        bounds.addFalse();
        return;
      }
      // > If the result of the function type is void , the constraint reduces
      // to true.
      if (ft.getReturnType() instanceof IJavaVoidType) {
        bounds.addTrue();
        return;
      }
      /*
       * > Otherwise, if the method reference expression elides TypeArguments,
       * and the compile-time declaration is a generic method, and the return
       * type of the compile-time declaration mentions at least one of the
       * method's type parameters, then the constraint reduces to the bound set
       * B 3 which would be used to determine the method reference's invocation
       * type when targeting the return type of the function type, as defined in
       * Â§18.5.2. B 3 may contain new inference variables, as well as
       * dependencies between these new variables and the inference variables in
       * T .
       */
      final MethodBinding mb = new MethodBinding(ctd);
      final IJavaType rtype = mb.getReturnType(tEnv);
      if (!JJNode.tree.hasChildren(targs) && isGenericMethodRef(mb) && rtype instanceof IJavaReferenceType
          && refersTo((IJavaReferenceType) rtype, getTypeFormalsForRef(mb))) {
        final Pair<MethodBinding, BoundSet> pair = recomputeB_2(ref, ctd);
        final BoundSet b_2 = pair.second();
        final BoundSet b_3 = computeB_3(ref, rtype, b_2, ft.getReturnType());
        bounds.mergeWithSubst(b_3, bounds.getInitialVarSubst());
      }
      /*
       * > Otherwise, let R be the return type of the function type, and let R '
       * be the result of applying capture conversion (Â§5.1.10) to the return
       * type of the invocation type (Â§15.12.2.6) of the compile-time
       * declaration. If R ' is void , the constraint reduces to false;
       * otherwise, the constraint reduces to < R ' -> R >.
       */
      /*
       * From JLS 15.13.2
       * 
       * The result of the function type is R , and the result of applying
       * capture conversion (§5.1.10) to the return type of the invocation type
       * (§15.12.2.6) of the chosen compile-time declaration is R ' (where R is
       * the target type that may be used to infer R '), and neither R nor R '
       * is void , and R ' is compatible with R in an assignment context.
       */
      else {
        final IJavaType r = ft.getReturnType();
        final IJavaFunctionType itype = this.mb.computeInvocationType(ref, ctd, false, r);
        final IJavaType r_prime = JavaTypeVisitor.captureWildcards(tEnv.getBinder(), itype.getReturnType());
        if (r_prime instanceof IJavaVoidType) {
          bounds.addFalse();
        } else {
          reduceTypeCompatibilityConstraints(bounds, r_prime, r);
        }
      }
    }
  }

  // Compensates for the fact that constructors for generic types might not have
  // the usual type parameters
  boolean isGenericMethodRef(MethodBinding mb) {
    if (mb.isGeneric()) {
      return true;
    }
    if (mb.mkind == Kind.CONSTRUCTOR) {
      if (mb.bind.getContextType().getTypeParameters().size() > 0) {
        final IRNode g = mb.bind.getContextType().getDeclaration();
        final IJavaType g_formal = tEnv.convertNodeTypeToIJavaType(g);
        return g_formal.equals(mb.bind.getContextType());
      } else {
        return mb.bind.getContextType().isRawType(tEnv); // From a constructor
                                                         // ref?
      }
    }
    return false;
  }

  List<IJavaTypeFormal> getTypeFormalsForRef(MethodBinding mb) {
    if (mb.mkind != Kind.CONSTRUCTOR) {
      return mb.getTypeFormals();
    }
    List<IJavaTypeFormal> rv = new ArrayList<IJavaTypeFormal>(mb.getTypeFormals());
    // HACK to get formals
    final IJavaDeclaredType declT = (IJavaDeclaredType) tEnv.convertNodeTypeToIJavaType(mb.bind.getContextType().getDeclaration());
    for (IJavaType t : declT.getTypeParameters()) {
      rv.add((IJavaTypeFormal) t);
    }
    return rv;
  }

  /**
   * 18.2.2 Type Compatibility Constraints
   * 
   * A constraint formula of the form < S -> T > is reduced as follows:
   * 
   * - If S and T are proper types, the constraint reduces to true if S is
   * compatible in a loose invocation context with T (Â§5.3), and false
   * otherwise.
   * 
   * - Otherwise, if S is a primitive type, let S' be the result of applying
   * boxing conversion (Â§5.1.7) to S . Then the constraint reduces to < S' -> T
   * >.
   * 
   * - Otherwise, if T is a primitive type, let T' be the result of applying
   * boxing conversion (Â§5.1.7) to T . Then the constraint reduces to
   * < S = T' >.
   * 
   * - Otherwise, if T is a parameterized type of the form G<T 1 , ..., T n > ,
   * and there exists no type of the form G< ... > that is a supertype of S ,
   * but the raw type G is a supertype of S , then the constraint reduces to
   * true.
   * 
   * - Otherwise, if T is an array type of the form G<T 1 , ..., T n >[] k , and
   * there exists no type of the form G< ... >[] k that is a supertype of S ,
   * but the raw type G[] k is a supertype of S , then the constraint reduces to
   * true. (The notation [] k indicates an array type of k dimensions.)
   * 
   * - Otherwise, the constraint reduces to < S <: T >. The fourth and fifth
   * cases are implicit uses of unchecked conversion (Â§5.1.9). These, along
   * with any use of unchecked conversion in the first case, may result in
   * compile-time unchecked warnings, and may influence a method's invocation
   * type (Â§15.12.2.6).
   */
  void reduceTypeCompatibilityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
    // Case 1
    if (isProperType(s) && isProperType(t)) {
      if (mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, t, null, s)) {
        bounds.addTrue();
      } 
      else if (hasRawSuperTypeOf(s, t)) {
    	  bounds.useUncheckedConversion();
      } else {
        mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, t, null, s);
        bounds.addFalse();
      }
    } else if (s instanceof IJavaPrimitiveType) {
      reduceTypeCompatibilityConstraints(bounds, box((IJavaPrimitiveType) s), t);
    } else if (t instanceof IJavaPrimitiveType) {
      reduceTypeCompatibilityConstraints(bounds, s, box((IJavaPrimitiveType) t));
    } else if (hasRawSuperTypeOf(s, t)) {
      bounds.addTrue();
      bounds.usedUncheckedConversion();
    } else if (t instanceof IJavaArrayType && s instanceof IJavaArrayType) {
      IJavaArrayType ta = (IJavaArrayType) t;
      IJavaArrayType sa = (IJavaArrayType) s;
      if (ta.getDimensions() == sa.getDimensions() && hasRawSuperTypeOf(sa.getBaseType(), ta.getBaseType())) {
        bounds.addTrue();
        bounds.usedUncheckedConversion();
      } else {
        reduceSubtypingConstraints(bounds, s, t);
      }
    } else {
      reduceSubtypingConstraints(bounds, s, t);
    }
  }

  /**
   * 18.2.3 Subtyping Constraints
   * 
   * A constraint formula of the form < S <: T > is reduced as follows: 
   * - If S and T are proper types, the constraint reduces to true if S is a subtype of
   *   T (Â§4.10), and false otherwise. 
   * - Otherwise, if S is the null type, the constraint reduces to true. 
   * - Otherwise, if T is the null type, the constraint reduces to false. 
   * - Otherwise, if S is an inference variable, α,
   *   the constraint reduces to the bound α <: T . 
   * - Otherwise, if T is an inference variable, α, the constraint reduces to the bound S <: α. 
   * - Otherwise, the constraint is reduced according to the form of T :
   * 
   * - If T is a parameterized class or interface type, or an inner class type
   * of a parameterized class or interface type (directly or indirectly), let A
   * 1 , ..., A n be the type arguments of T . Among the supertypes of S , a
   * corresponding class or interface type is identified, with type arguments B
   * 1 , ..., B n . If no such type exists, the constraint reduces to false.
   * Otherwise, the constraint reduces to the following new constraints: for all
   * i (1 <= i <= n), < B i <= A i >.
   * 
   * - If T is any other class or interface type, then the constraint reduces to
   * true if T is among the supertypes of S , and false otherwise.
   * 
   * - If T is an array type, T'[] , then among the supertypes of S that are
   * array types, a most specific type is identified, S'[] (this may be S
   * itself). If no such array type exists, the constraint reduces to false.
   * Otherwise:
   * 
   * > If neither S' nor T' is a primitive type, the constraint reduces to < S'
   * <: T' >. > Otherwise, the constraint reduces to true if S' and T' are the
   * same primitive type, and false otherwise.
   * 
   * - If T is a type variable, there are three cases: > If S is an intersection
   * type of which T is an element, the constraint reduces to true. > Otherwise,
   * if T has a lower bound, B , the constraint reduces to < S <: B >. >
   * Otherwise, the constraint reduces to false.
   * 
   * - If T is an intersection type, I 1 & ... & I n , the constraint reduces to
   * the following new constraints: for all i (1 <= i <= n), < S <: I i >.
   */
  void reduceSubtypingConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
    if (isProperType(s) && isProperType(t)) {
      if (s instanceof TypeVariable || t instanceof TypeVariable) {
        // HACK ignore for now?
        return;
      }
      if (tEnv.isSubType(s, t)) {
        bounds.addTrue();
      } 
      else if (hasRawSuperTypeOf(s, t)) {
    	  //bounds.useUncheckedConversion();
      } else {
    	  //System.out.println("F check");
    	  tEnv.isSubType(s, t);
    	  bounds.addFalse();
      }
    } else if (s instanceof IJavaNullType) {
      bounds.addTrue();
    } else if (t instanceof IJavaNullType) {
      bounds.addFalse();
    } else if (isInferenceVariable(s) || isInferenceVariable(t)) {
      bounds.addSubtypeBound(s, t);
    }

    else if (t instanceof IJavaDeclaredType) {
      if (hasRawSuperTypeOf(s, t)) {
    	  //bounds.useUncheckedConversion();
    	  return;
      }
      final IJavaDeclaredType dt = (IJavaDeclaredType) t;
      // TODO subcase 1
      if (s instanceof TypeVariable) {
        // ignore temporarily until fully instantiated
      } else if (dt.getTypeParameters()
          .size() > 0/*
                      * TODO || dt.getOuterType().getTypeParameters().size() > 0
                      */) {
        final IJavaDeclaredType ds = findCorrespondingSuperType(dt.getDeclaration(), s);
        if (ds != null && ds.getTypeParameters().size() > 0) {
          final int n = dt.getTypeParameters().size();
          for (int i = 0; i < n; i++) {
            final IJavaType a_i = dt.getTypeParameters().get(i);
            final IJavaType b_i = ds.getTypeParameters().get(i);
            reduceTypeArgContainmentConstraints(bounds, b_i, a_i);
          }
        } else {
          //findCorrespondingSuperType(dt.getDeclaration(), s);
          bounds.addFalse();
        }
      } else if (tEnv.isSubType(s, t)) {
        bounds.addTrue();
      } else {
        bounds.addFalse();
      }
    } else if (t instanceof IJavaArrayType) {
      final IJavaType s_prime = findMostSpecificArraySuperType(s);
      if (s_prime == null) {
        bounds.addFalse();
      } else {
        final IJavaArrayType t_primeArray = (IJavaArrayType) t;
        final IJavaType t_prime = t_primeArray.getElementType();
        if (s_prime instanceof IJavaPrimitiveType || t_prime instanceof IJavaPrimitiveType) {
          if (!s_prime.equals(t_prime)) {
            bounds.addFalse();
          }
        } else {
          reduceSubtypingConstraints(bounds, s_prime, t_prime);
        }
      }
    }
    // HACK to deal with the lack of simultaneous incorporation
    else if (s instanceof TypeVariable || t instanceof TypeVariable) {
      // the other type is not a proper type, since that case is handled above
      // so ignore for now until we handle the rest of the substitution
    }
    // HACK to deal w/ capture types
    else if (t instanceof IJavaCaptureType && s instanceof IJavaCaptureType) {
      final IJavaCaptureType sc = (IJavaCaptureType) s;
      final IJavaCaptureType tc = (IJavaCaptureType) t;
      reduceSubtypingConstraints(bounds, sc.getUpperBound(), tc.getUpperBound());
      reduceSubtypingConstraints(bounds, tc.getLowerBound(), sc.getLowerBound());
    } else if (t instanceof IJavaTypeVariable) {
      final IJavaTypeVariable tv = (IJavaTypeVariable) t;
      IntersectionOperator hasT = new IntersectionOperator() {
        @Override
        public boolean evaluate(IJavaType t) {
          return t == tv;
        }

        @Override
        public boolean combine(boolean e1, boolean e2) {
          return e1 || e2;
        }

      };
      if (s instanceof IJavaIntersectionType && flattenIntersectionType(hasT, (IJavaIntersectionType) s)) {
        bounds.addTrue();
      } else if (tv.getLowerBound() != null) {
        reduceSubtypingConstraints(bounds, s, tv.getLowerBound());
      } else {
        bounds.addFalse();
      }
    } else if (t instanceof IJavaIntersectionType) {
      IJavaIntersectionType it = (IJavaIntersectionType) t;
      reduceSubtypingConstraints(bounds, s, it.getPrimarySupertype());
      reduceSubtypingConstraints(bounds, s, it.getSecondarySupertype());
    } else {
      throw new NotImplemented(); // TODO
    }
  }

  private IJavaDeclaredType findCorrespondingSuperType(final IRNode decl, IJavaType s) {
    if (s instanceof IJavaDeclaredType) {
      IJavaDeclaredType ds = (IJavaDeclaredType) s;
      if (ds.isSameDecl(decl)) {
        return ds;
      }
    }
    for (IJavaType st : s.getSupertypes(tEnv)) {
      IJavaDeclaredType rv = findCorrespondingSuperType(decl, st);
      if (rv != null) {
        return rv;
      }
    }
    return null;
  }

  // returns S' for S'[]
  private IJavaType findMostSpecificArraySuperType(IJavaType s) {
    if (s instanceof IJavaArrayType) {
      IJavaArrayType as = (IJavaArrayType) s;
      return as.getElementType();
    } else if (s instanceof IJavaDeclaredType) {
      IJavaDeclaredType ds = (IJavaDeclaredType) s;
      if (ds.getDeclaration() == tEnv.getArrayClassDeclaration()) {
    	return ds.getTypeParameters().get(0);
      }
    }
    // What other cases are there?
    return null;
  }

  /**
   * A constraint formula of the form < S <= T >, where S and T are type
   * arguments (Â§4.5.1), is reduced as follows: - If T is a type: - If S is a
   * type, the constraint reduces to
   * < S = T >. - If S is a wildcard, the constraint reduces to false.
   * 
   * - If T is a wildcard of the form ? , the constraint reduces to true.
   * 
   * - If T is a wildcard of the form ? extends T' : - If S is a type, the
   * constraint reduces to < S <: T' >. - If S is a wildcard of the form ? , the
   * constraint reduces to < Object <: T' >. - If S is a wildcard of the form ?
   * extends S' , the constraint reduces to < S' <: T' >. - If S is a wildcard
   * of the form ? super S' , the constraint reduces to < Object = T' >.
   * 
   * - If T is a wildcard of the form ? super T' : - If S is a type, the
   * constraint reduces to < T' <: S >. - If S is a wildcard of the form ? super
   * S' , the constraint reduces to < T' <: S' >. - Otherwise, the constraint
   * reduces to false.
   */
  private void reduceTypeArgContainmentConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
    if (t instanceof IJavaWildcardType) {
      IJavaWildcardType wt = (IJavaWildcardType) t;
      if (wt.getUpperBound() != null) {
        // Case 3: ? extends X
        if (s instanceof IJavaWildcardType) {
          IJavaWildcardType ws = (IJavaWildcardType) s;
          if (ws.getUpperBound() != null) {
            reduceSubtypingConstraints(bounds, ws.getUpperBound(), wt.getUpperBound());
          } else if (ws.getLowerBound() != null) {
            reduceTypeEqualityConstraints(bounds, tEnv.getObjectType(), wt.getUpperBound());
          } else {
            reduceSubtypingConstraints(bounds, tEnv.getObjectType(), wt.getUpperBound());
          }
        } else if (s instanceof IJavaCaptureType) {
          // Is this really the same as the wildcard?
          final IJavaCaptureType cs = (IJavaCaptureType) s;
          if (captureIsWildcard(cs, wt)) {
        	bounds.addTrue();
          } else if (cs.getLowerBound() == JavaTypeFactory.nullType) {
        	reduceSubtypingConstraints(bounds, cs.getUpperBound(), wt.getUpperBound());
          } else {
        	//bounds.addTrue();
        	//
        	// Fall through to below
        	reduceSubtypingConstraints(bounds, s, wt.getUpperBound());
          }
        } else {
          reduceSubtypingConstraints(bounds, s, wt.getUpperBound());
        }
      } else if (wt.getLowerBound() != null) {
        // Case 4: ? super X
        if (s instanceof IJavaWildcardType) {
          IJavaWildcardType ws = (IJavaWildcardType) s;
          if (ws.getLowerBound() != null) {
            reduceSubtypingConstraints(bounds, wt.getLowerBound(), ws.getLowerBound());
          } else {
            bounds.addFalse();
          }
        } else {
          reduceSubtypingConstraints(bounds, wt.getLowerBound(), s);

        }
      } else {
        // Case 2: ?
        bounds.addTrue();
      }
    }
    // NOT needed, due to capture conversion of arguments
    //
    // else if (t instanceof InferenceVariable) {
    // // TODO HACK?
    // if (s instanceof IJavaWildcardType) {
    // // ? <= ?
    // // ? extends U <= ? extends (? super U)
    // // ? super U <= ? super (? extends U)
    // /*
    // IJavaWildcardType ws = (IJavaWildcardType) s;
    // if (ws.getLowerBound() != null) {
    //
    // }
    // */
    // reduceTypeEqualityConstraints(bounds, s, t);
    // } else {
    // reduceTypeEqualityConstraints(bounds, s, t);
    // }
    // }
    // Case 1
    else if (s instanceof IJavaWildcardType) {
      bounds.addFalse();
    } else {
      reduceTypeEqualityConstraints(bounds, s, t);
    }
  }

  private boolean captureIsWildcard(IJavaCaptureType c, IJavaWildcardType w) {
	if (c.getWildcard().equals(w)) {
	  if (c.getLowerBound() != JavaTypeFactory.nullType || w.getLowerBound() != null) {
		return false;
	  }
	  if (c.getUpperBound() == w.getUpperBound()) {
		return true;
	  }
	}
	return false;
  }

  /**
   * 18.2.4 Type Equality Constraints
   * 
   * A constraint formula of the form
   * < S = T >, where S and T are types, is reduced as follows:
   * 
   * - If S and T are proper types, the constraint reduces to true if S is the
   * same as T (Â§4.3.4), and false otherwise.
   * 
   * - Otherwise, if S is an inference variable, α, the constraint reduces to
   * the bound α = T . - Otherwise, if T is an inference variable, α, the
   * constraint reduces to the bound S = α.
   * 
   * - Otherwise, if S and T are class or interface types with the same erasure,
   * where S has type arguments B 1 , ..., B n and T has type arguments A 1 ,
   * ..., A n , the constraint reduces to the following new constraints: for all
   * i (1 <= i <= n), < B i = A i >.
   * 
   * - Otherwise, if S and T are array types, S'[] and T'[] , the constraint
   * reduces to < S' = T' >. - Otherwise, the constraint reduces to false.
   */
  void reduceTypeEqualityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
    if (isProperType(s) && isProperType(t)) {
      if (s instanceof TypeVariable || t instanceof TypeVariable) {
        // HACK ignore for now?
        return;
      }
      if (t.isEqualTo(tEnv, s) || s.isEqualTo(tEnv, t)) {
        bounds.addTrue();
      } else {
    	t.isEqualTo(tEnv, s);
    	s.isEqualTo(tEnv, t);
        bounds.addFalse();
      }
    } else if (isInferenceVariable(s) || isInferenceVariable(t)) {
      bounds.addEqualityBound(s, t);
    } else if (s instanceof IJavaDeclaredType && t instanceof IJavaDeclaredType) {
      IJavaDeclaredType sd = (IJavaDeclaredType) s;
      IJavaDeclaredType td = (IJavaDeclaredType) t;
      if (sd.getDeclaration().equals(td.getDeclaration()) && sd.getTypeParameters().size() == td.getTypeParameters().size()) {
        int i = 0;
        for (IJavaType sp : sd.getTypeParameters()) {
          IJavaType tp = td.getTypeParameters().get(i);
          reduceTypeArgumentEqualityConstraints(bounds, sp, tp);
          i++;
        }
      } else {
        bounds.addFalse(); // TODO
      }
    } else if (s instanceof IJavaArrayType && t instanceof IJavaArrayType) {
      IJavaArrayType sa = (IJavaArrayType) s;
      IJavaArrayType ta = (IJavaArrayType) t;
      reduceTypeEqualityConstraints(bounds, sa.getElementType(), ta.getElementType());
    }
    // HACK to deal with the lack of simultaneous incorporation
    else if (s instanceof TypeVariable || t instanceof TypeVariable) {
      // the other type is not a proper type, since that case is handled above
      // so ignore for now until we handle the rest of the substitution
    }
    else if (t instanceof IJavaTypeVariable) {
      final IJavaTypeVariable v = (IJavaTypeVariable) t;
      final IJavaType vub = getUpperBound(v); 
      if (s instanceof IJavaTypeVariable) {
    	  // Can't just look at one type variable
    	  final IJavaTypeVariable u = (IJavaTypeVariable) s;
    	  final IJavaType uub = getUpperBound(u); 
    	  reduceTypeEqualityConstraints(bounds, uub, vub);
      } else {
    	  reduceTypeEqualityConstraints(bounds, s, vub);
      }
    } else {
      bounds.addFalse();
    }
  }

  IJavaReferenceType getUpperBound(IJavaTypeVariable v) {
	  IJavaReferenceType rv = v.getUpperBound(tEnv);
	  if (rv == null) {
		  return tEnv.getObjectType();
	  }
	  return rv;
  }
  
  /**
   * A constraint formula of the form
   * < S = T >, where S and T are type arguments (Â§4.5.1), is reduced as
   * follows:
   * 
   * - If S and T are types, the constraint is reduced as described above. - If
   * S has the form ? and T has the form ? , the constraint reduces to true. -
   * If S has the form ? and T has the form ? extends T' , the constraint
   * reduces to < Object = T' >. - If S has the form ? extends S' and T has the
   * form ? , the constraint reduces to < S' = Object >.
   * 
   * - If S has the form ? extends S' and T has the form ? extends T' , the
   * constraint reduces to < S' = T' >.
   * 
   * - If S has the form ? super S' and T has the form ? super T' , the
   * constraint reduces to < S' = T' >.
   * 
   * - Otherwise, the constraint reduces to false.
   * 
   * @param bounds
   */
  void reduceTypeArgumentEqualityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
    if (!(s instanceof IJavaWildcardType) && !(t instanceof IJavaWildcardType)) {
      reduceTypeEqualityConstraints(bounds, s, t);
    } else if (s instanceof IJavaWildcardType && t instanceof IJavaWildcardType) {
      IJavaWildcardType sw = (IJavaWildcardType) s;
      IJavaWildcardType tw = (IJavaWildcardType) t;
      if (sw.getUpperBound() != null) {
        if (tw.getUpperBound() != null) {
          // Case 5
          reduceTypeEqualityConstraints(bounds, sw.getUpperBound(), tw.getUpperBound());
        } else if (tw.getLowerBound() == null) {
          // Case 4
          reduceTypeEqualityConstraints(bounds, sw.getUpperBound(), tEnv.getObjectType());
        } else {
          bounds.addFalse();
        }
      } else if (sw.getLowerBound() != null) {
        // case 6
        if (tw.getLowerBound() != null) {
          reduceTypeEqualityConstraints(bounds, sw.getLowerBound(), tw.getLowerBound());
        } else {
          bounds.addFalse();
        }
      } else {
        if (tw.getUpperBound() != null) {
          // Case 3
          reduceTypeEqualityConstraints(bounds, tEnv.getObjectType(), tw.getUpperBound());
        } else if (tw.getLowerBound() == null) {
          // Case 2
          bounds.addTrue();
        } else {
          bounds.addFalse();
        }
      }
    } else if (JavaTypeFactory.isTargetType(t)) {
      bounds.addTrue();
    } else {
      bounds.addFalse();
    }
  }

  /**
   * 18.2.5 Checked Exception Constraints
   * 
   * A constraint formula of the form <LambdaExpression -> throws T > is reduced
   * as follows:
   * 
   * (see below)
   */
  private void reduceLambdaCheckedExceptionConstraints(BoundSet bounds, IRNode lambda, IJavaType t) {
    // - If T is not a functional interface type (Â§9.8), the constraint reduces
    // to false.
    if (tEnv.isFunctionalType(t) == null) {
      bounds.addFalse();
      return;
    }
    /*
     * - Otherwise, let the target function type for the lambda expression be
     * determined as specified in Â§15.27.3. If no valid function type can be
     * found, the constraint reduces to false.
     */
    IJavaType targetType = mb.computeGroundTargetType(lambda, t, false);
    IJavaFunctionType targetFuncType = tEnv.isFunctionalType(targetType);
    if (targetFuncType == null) {
      bounds.addFalse();
      return;
    }
    /*
     * - Otherwise, if the lambda expression is implicitly typed, and one or
     * more of the function type's parameter types is not a proper type, the
     * constraint reduces to false.
     */
    final boolean isImplicitLambda = MethodBinder8.isImplicitlyTypedLambda(lambda);
    if (isImplicitLambda) {
      for (IJavaType pt : targetFuncType.getParameterTypes()) {
        if (!isProperType(pt)) {
          bounds.addFalse();
          return;
        }
      }
    }
    /*
     * - Otherwise, if the function type's return type is neither void nor a
     * proper type, the constraint reduces to false.
     */
    if (!(targetFuncType.getReturnType() instanceof IJavaVoidType) && !isProperType(targetFuncType.getReturnType())) {
      bounds.addFalse();
      return;
    }
    /*
     * - Otherwise, let E 1 , ..., E n be the types in the function type's
     * throws clause that are not proper types. If the lambda expression is
     * implicitly typed, let its parameter types be the function type's
     * parameter types. If the lambda body is a poly expression or a block
     * containing a poly result expression, let the targeted return type be the
     * function type's return type. Let X 1 , ..., X m be the checked exception
     * types that the lambda body can throw (Â§11.2). Then there are two cases:
     */
    final List<IJavaType> improperThrows = new ArrayList<IJavaType>(); // a.k.a
                                                                       // E
    for (IJavaType ex : targetFuncType.getExceptions()) {
      if (!isProperType(ex)) {
        improperThrows.add(ex);
      }
    }
    final Set<IJavaType> x = computeCheckedExceptionsThrownByLambda(bounds, lambda, t, targetFuncType);
    if (improperThrows.isEmpty()) {
      /*
       * - If n = 0 (the function type's throws clause consists only of proper
       * types), then if there exists some i (1 <= i <= m) such that X i is not
       * a subtype of any proper type in the throws clause, the constraint
       * reduces to false; otherwise, the constraint reduces to true.
       * 
       */
      for (IJavaType x_i : x) {
        if (!isPartOfThrowsClause(targetFuncType, x_i)) {
          //computeCheckedExceptionsThrownByLambda(bounds, lambda, t, targetFuncType);
          //isPartOfThrowsClause(targetFuncType, x_i);
          bounds.addFalse();
          break;
        } else {
          bounds.addTrue();
        }
      }
    } else {
      /*
       * - If n > 0 , the constraint reduces to a set of subtyping constraints:
       * for all i (1 <= i <= m), if X i is not a subtype of any proper type in
       * the throws clause, then the constraints include, for all j (1 <= j <=
       * n), < X i <: E j >. In addition, for all j (1 <= j <= n), the
       * constraint reduces to the bound throws E j .
       */
      for (IJavaType x_i : x) {
        if (!isPartOfThrowsClause(targetFuncType, x_i)) {
          for (IJavaType e_j : improperThrows) {
            reduceSubtypingConstraints(bounds, x_i, e_j);
          }
        }
      }
      for (IJavaType e_j : improperThrows) {
        bounds.addThrown(e_j);
      }
    }
  }

  private boolean isPartOfThrowsClause(IJavaFunctionType func, IJavaType x) {
    if (x instanceof IJavaSourceRefType) {
      IJavaSourceRefType xsr = (IJavaSourceRefType) x;
      final IJavaType runtimeEx = tEnv.findJavaTypeByName("java.lang.RuntimeException", xsr.getDeclaration());
      if (x.isSubtype(tEnv, runtimeEx)) {
        return true;
      }
    }
    return isSubTypeOfSomeTypeInList(func.getExceptions(), x);
  }

  private boolean isSubTypeOfSomeTypeInList(Set<IJavaType> types, IJavaType s) {
    for (IJavaType t : types) {
      if (s.isSubtype(tEnv, t)) {
        return true;
      }
    }
    return false;
  }

  /**
   * If the lambda expression is implicitly typed, let its parameter types be
   * the function type's parameter types. If the lambda body is a poly
   * expression or a block containing a poly result expression, let the targeted
   * return type be the function type's return type. Let X 1 , ..., X m be the
   * checked exception types that the lambda body can throw (Â§11.2).
 * @param bounds 
   * 
   * @param t
   */
  private Set<IJavaType> computeCheckedExceptionsThrownByLambda(BoundSet bounds, IRNode lambda, IJavaType t, IJavaFunctionType targetFuncType) {
    final UnversionedJavaBinder ujb;
    if (tEnv.getBinder() instanceof UnversionedJavaBinder) {
      ujb = (UnversionedJavaBinder) tEnv.getBinder();
    } else {
      throw new NotImplemented();
    }
    final JavaCanonicalizer.IBinderCache old = ujb.setBinderCache(new LambdaCache(tEnv, bounds, lambda, t, targetFuncType));
    try {
      final ExceptionCollector c = new ExceptionCollector(ujb);
      return c.doAccept(LambdaExpression.getBody(lambda));
    } finally {
      ujb.setBinderCache(old);
    }
  }

  static IJavaType simplify(IJavaTypeSubstitution subst, TypeUtils utils, IJavaType t) {
  	IJavaType temp = t.subst(subst);
  	/*
  	if (temp != t) {
  	  System.out.println("Simplified types for cache: "+t+" -> "+temp);
  	}
  	*/
  	return eliminateTypeVariables(utils, temp);
  }
  
  static class LambdaCache implements JavaCanonicalizer.IBinderCache {
    final Map<IRNode, IJavaType> map = new HashMap<IRNode, IJavaType>();

    LambdaCache(ITypeEnvironment tEnv, BoundSet bounds, IRNode lambda, IJavaType t, IJavaFunctionType targetFuncType) {
      final IJavaTypeSubstitution subst;
      if (bounds != null) {
    	// Find inference vars to resolve
    	Set<InferenceVariable> toResolve = new HashSet<>();
    	getReferencedInferenceVariables(toResolve, t);
      	getReferencedInferenceVariables(toResolve, targetFuncType.getReturnType());
      	for(IJavaType pt : targetFuncType.getParameterTypes()) {
      	  getReferencedInferenceVariables(toResolve, pt);
      	}    	
    	BoundSet resolved = resolve(bounds, toResolve, false);
    	subst = TypeSubstitution.create(tEnv.getBinder(), resolved.getInstantiations()); // TODO ignore missing?
      } else {
    	subst = IJavaTypeSubstitution.NULL;
      }
      final TypeUtils utils = new TypeUtils(tEnv);
      int i = 0;
      for (IRNode param : Parameters.getFormalIterator(LambdaExpression.getParams(lambda))) {
        IJavaType ptype = targetFuncType.getParameterTypes().get(i);
        /*
         * if (ptype == tEnv.getObjectType()) { System.out.println(
         * "Got Object as type for "+ParameterDeclaration.getId(param)); }
         */
        ptype = simplify(subst, utils, ptype);
        map.put(ParameterDeclaration.getType(param), ptype);
        i++;
      }
      map.put(JJNode.tree.getParent(lambda), simplify(subst, utils, targetFuncType.getReturnType()));
      map.put(lambda, simplify(subst, utils, t));
    }
    
    public void init(IRNode tree) {
      throw new NotImplemented();
    }

    public IBinding checkForBinding(IRNode node) {
      return null;
    }

    public IJavaType checkForType(IRNode node) {
      IJavaType rv = map.get(node);
      if (rv != null) {
        return rv;
      }
      return null;
    }

    public void map(IRNode old, IRNode now) {
      throw new NotImplemented();
    }

    public void addBinding(IRNode node, IBinding b) {
      throw new NotImplemented();
    }

    public void finish(IRNode tree) {
      throw new NotImplemented();
    }
  }

  static class ExceptionCollector extends AbstractLambdaVisitor<Set<IJavaType>> {
    final IPrivateBinder binder;
    final MethodBinder8 mb;

    ExceptionCollector(IPrivateBinder b) {
      super(Collections.<IJavaType>emptySet());
      binder = b;
      mb = new MethodBinder8(b, false);
    }

    Set<IJavaType> collectForCall(IRNode call, IRNode receiver, IRNode args, IRNode targs) {
      // TODO can't bind? (causes cycle)
      final MethodBinding8 b = (MethodBinding8) binder.getIBinding(call);
      final CallState state = CallState.create(binder, call, targs, args, binder.getJavaType(receiver));
      final IJavaFunctionType itype = mb.computeInvocationType(state, b, false);
      return itype.getExceptions();
    }    
    
    /**
     * 11.2.1 Exception Analysis of Expressions
     * 
     * A class instance creation expression (§15.9) can throw an exception class
     * E iff either:
     * 
     * • The expression is a qualified class instance creation expression and
     * the qualifying expression can throw E ; or • Some expression of the
     * argument list can throw E ; or • E is one of the exception types of the
     * invocation type of the chosen constructor (§15.12.2.6); or • The class
     * instance creation expression includes a ClassBody, and some instance
     * initializer or instance variable initializer in the ClassBody can throw E
     * .
     */
    @Override
    public Set<IJavaType> visitNewExpression(IRNode e) {
      return mergeWithChildren(e, collectForCall(e, NewExpression.getType(e), NewExpression.getArgs(e), NewExpression.getTypeArgs(e)));
    }

    /**
     * A method invocation expression (§15.12) can throw an exception class E
     * iff either:
     * 
     * • The method invocation expression is of the form Primary .
     * [TypeArguments] Identifier and the Primary expression can throw E ; or •
     * Some expression of the argument list can throw E ; or • E is one of the
     * exception types of the invocation type of the chosen method (§15.12.2.6).
     */
    @Override
    public Set<IJavaType> visitMethodCall(IRNode call) {
      // Handle receiver and args
      return mergeWithChildren(call, collectForCall(call, MethodCall.getObject(call), MethodCall.getArgs(call), MethodCall.getTypeArgs(call)));
    }

    /**
     * A lambda expression (§15.27) can throw no exception classes.
     */
    
    /*
     * For every other kind of expression, the expression can throw an exception
     * class E iff one of its immediate subexpressions can throw E
     */

    /**
     * 11.2.2 Exception Analysis of Statements
     * 
     * A throw statement (§14.18) whose thrown expression has static type E and
     * is not a final or effectively final exception parameter can throw E or
     * any exception class that the thrown expression can throw.
     * 
     * A throw statement whose thrown expression is a final or effectively final
     * exception parameter of a catch clause C can throw an exception class E
     * iff:
     * 
     * (see below)
     */
    @Override
    public Set<IJavaType> visitThrowStatement(IRNode s) {
      final IRNode v = ThrowStatement.getValue(s);
      final Operator op = JJNode.tree.getOperator(v);
      final IJavaType e = binder.getJavaType(v);
      if (op instanceof IHasBinding) {
        // Check if it's a final exception parameter
        final IBinding b = binder.getIBinding(v);
        // TODO effectively final?
        if (ParameterDeclaration.prototype.includes(b.getNode()) && TypeUtil.isJavaFinal(b.getNode())) {
          final IRNode p = JJNode.tree.getParent(b.getNode());
          if (CatchClause.prototype.includes(p)) {
            /*
             * • E is an exception class that the try block of the try statement
             * which declares C can throw; and
             */
            final IRNode gp = JJNode.tree.getParent(p);
            final Set<IJavaType> fromBlock = doAccept(TryStatement.getBlock(gp));

            if (fromBlock.contains(e) && isAssignmentCompatibleWithCatchClause(e, p)) {
              /*
               * • E is assignment compatible with any of C 's catchable
               * exception classes; and • E is not assignment compatible with
               * any of the catchable exception classes of the catch clauses
               * declared to the left of C in the same try statement.
               */
              boolean addE = true;
              for (IRNode cc : CatchClauses.getCatchClauseIterator(TryStatement.getCatchPart(gp))) {
                if (cc == p) {
                  break;
                }
                if (isAssignmentCompatibleWithCatchClause(e, cc)) {
                  addE = false;
                  break;
                }
              }
              if (addE) {
                return Collections.singleton(e);
              }
            }
            return Collections.emptySet();
          }
        }
      }
      return merge(doAccept(v), e);
    }

    boolean isAssignmentCompatibleWithCatchClause(IJavaType e, IRNode cc) {
      IRNode param = CatchClause.getParam(cc);
      IJavaType ptype = binder.getJavaType(ParameterDeclaration.getType(param));
      return binder.getTypeEnvironment().isAssignmentCompatible(ptype, e, null);
    }

    /**
     * A try statement (§14.20) can throw an exception class E iff either: • The
     * try block can throw E , or an expression used to initialize a resource
     * (in a try -with-resources statement) can throw E , or the automatic
     * invocation of the close() method of a resource (in a try -with-resources
     * statement) can throw E , and E is not assignment compatible with any
     * catchable exception class of any catch clause of the try statement, and
     * either no finally block is present or the finally block can complete
     * normally; or • Some catch block of the try statement can throw E and
     * either no finally block is present or the finally block can complete
     * normally; or • A finally block is present and can throw E .
     */
    @Override
    public Set<IJavaType> visitTryStatement(IRNode t) {
      Set<IJavaType> fromBlock = doAccept(TryStatement.getBlock(t));
      return handleTry(fromBlock, TryStatement.getCatchPart(t), TryStatement.getFinallyPart(t));
    }

    private Set<IJavaType> handleTry(final Set<IJavaType> fromBody, final IRNode catches, final IRNode finallyC) {
      // Remove exceptions handled by the catch clauses
      final Set<IJavaType> exceptions = new HashSet<IJavaType>();
      for (final IRNode cc : CatchClauses.getCatchClauseIterator(catches)) {
        Iterator<IJavaType> it = fromBody.iterator();
        while (it.hasNext()) {
          final IJavaType e = it.next();
          if (isAssignmentCompatibleWithCatchClause(e, cc)) {
            it.remove();
          }
          exceptions.addAll(doAccept(CatchClause.getBody(cc)));
        }
      }
      if (NoFinally.prototype.includes(finallyC)) {
    	  exceptions.addAll(fromBody);
      } else {
        if (!fromBody.isEmpty() || !/*from c2*/exceptions.isEmpty()) {
          // TODO how to complete normally?
          exceptions.addAll(fromBody);
        }
        final Set<IJavaType> c3 = doAccept(finallyC);
        exceptions.addAll(c3);
      }
      return exceptions;
    }

    @Override
    public Set<IJavaType> visitTryResource(IRNode t) {
      Set<IJavaType> local = doAccept(TryResource.getBlock(t));
      local = merge(local, doAccept(TryResource.getResources(t)));
      // TODO what about the close?
      return handleTry(local, TryResource.getCatchPart(t), TryResource.getFinallyPart(t));
    }

    /**
     * An explicit constructor invocation statement (§8.8.7.1) can throw an
     * exception class E iff either: • Some expression of the constructor
     * invocation's parameter list can throw E ; or • E is determined to be an
     * exception class of the throws clause of the constructor that is invoked
     * (§15.12.2.6).
     */
    @Override
    public Set<IJavaType> visitConstructorCall(IRNode e) {
      return mergeWithChildren(e, collectForCall(e, ConstructorCall.getObject(e), ConstructorCall.getArgs(e), ConstructorCall.getTypeArgs(e)));
    }

    /*
     * Any other statement S can throw an exception class E iff an expression or
     * statement immediately contained in S can throw E .
     */

    @Override
    public Set<IJavaType> visitType(IRNode t) {
      // No statements or expressions inside
      return Collections.emptySet();
    }

    private Set<IJavaType> merge(Set<IJavaType> r, IJavaType t) {
      if (r.isEmpty()) {
    	return Collections.singleton(t);
      }
      Set<IJavaType> rv = new HashSet<IJavaType>(r);
      rv.add(t);
      return rv;
    }
    
    Set<IJavaType> merge(Set<IJavaType> r1, Set<IJavaType> r2) {
      return merge(r1, r2, false);
    }
    
    private Set<IJavaType> merge(Set<IJavaType> r1, Set<IJavaType> r2, boolean reuseR1) {
      if (r1.isEmpty()) {
    	  return r2;
      }
      if (r2.isEmpty()) {
    	  return r1;
      }
      Set<IJavaType> rv = reuseR1 ? r1 : new HashSet<IJavaType>(r1);
      rv.addAll(r2);
      return rv;
    }
    
    private Set<IJavaType> mergeWithChildren(IRNode n, Set<IJavaType> localResults) {
      Set<IJavaType> childrenResults = mergeResults(doAcceptForChildrenWithResults(n));
      return merge(childrenResults, localResults, true); 
    }
    
	@Override
	protected Set<IJavaType> mergeResults(List<Set<IJavaType>> results) {
		if (results.isEmpty()) {
			return Collections.emptySet();
		}
		Set<IJavaType> rv = Collections.emptySet();
		for(Set<IJavaType> v : results) {
			if (v == null || v.isEmpty()) {
				continue;
			}
			if (rv.isEmpty()) {
				rv = new HashSet<IJavaType>();
			}
			rv.addAll(v);
		}
		return rv;
	}
  }

  /**
   * A constraint formula of the form <MethodReference -> throws T > is reduced
   * as follows:
   * 
   * - If T is not a functional interface type, or if T is a functional
   * interface type but does not have a function type (Â§9.9), the constraint
   * reduces to false.
   * 
   * - Otherwise, let the target function type for the method reference
   * expression be the function type of T . If the method reference is inexact
   * (Â§15.13.1) and one or more of the function type's parameter types is not a
   * proper type, the constraint reduces to false.
   * 
   * - Otherwise, if the method reference is inexact and the function type's
   * result is neither void nor a proper type, the constraint reduces to false.
   * 
   * (see below)
   */
  private void reduceMethodRefCheckedExceptionConstraints(BoundSet bounds, IRNode ref, final IJavaType t) {
    final IJavaType gt = mb.computeGroundTargetTypeForMethodRef(t);
    final IJavaFunctionType targetFuncType = tEnv.isFunctionalType(gt);

    if (targetFuncType == null) {
      bounds.addFalse();
      return;
    }
    if (!mb.isExactMethodReference(ref)) {
      for (IJavaType pt : targetFuncType.getParameterTypes()) {
        if (!isProperType(pt)) {
          bounds.addFalse();
          return;
        }
      }
      if (!(targetFuncType.getReturnType() instanceof IJavaVoidType) && !isProperType(targetFuncType.getReturnType())) {
        bounds.addFalse();
        return;
      }
    }
    /**
     * - Otherwise, let E 1 , ..., E n be the types in the function type's
     * throws clause that are not proper types. Let X 1 , ..., X m be the
     * checked exceptions in the throws clause of the invocation type of the
     * method reference's compile-time declaration (Â§15.13.2) (as derived from
     * the function type's parameter types and return type). Then there are two
     * cases:
     */
    final Set<IJavaType> e = new HashSet<IJavaType>();
    for (IJavaType ex : targetFuncType.getExceptions()) {
      if (!isProperType(ex)) {
        e.add(ex);
      }
    }
    final IJavaFunctionType invocationType = computeInvocationTypeForRef(targetFuncType, ref);
    if (invocationType == null) {
      computeInvocationTypeForRef(targetFuncType, ref);
      bounds.addFalse();
      return;
    }
    /*
     * - If n = 0 (the function type's throws clause consists only of proper
     * types), then if there exists some i (1 <= i <= m) such that X i is not a
     * subtype of any proper type in the throws clause, the constraint reduces
     * to false; otherwise, the constraint reduces to true.
     */

    if (e.size() == 0) {
      for (IJavaType x_i : invocationType.getExceptions()) {
        for (IJavaType ex : targetFuncType.getExceptions()) {
          if (!x_i.isSubtype(tEnv, ex)) {
            bounds.addFalse();
            return;
          }
        }
      }
    } else {
      /*
       * - If n > 0 , the constraint reduces to a set of subtyping constraints:
       * for all i (1 <= i <= m), if X i is not a subtype of any proper type in
       * the throws clause, then the constraints include, for all j (1 <= j <=
       * n), < X i <: E j >. In addition, for all j (1 <= j <= n), the
       * constraint reduces to the bound throws E j .
       */
      throw new NotImplemented();
    }
  }

  private IJavaFunctionType computeInvocationTypeForRef(IJavaFunctionType targetFuncType, IRNode ref) {
    final RefState state = mb.new RefState(targetFuncType, ref);
    /*
    if (state.toString().startsWith("Grep:: <> getPathStream")) {
      System.out.println("Computing type for Grep:: <> getPathStream");
    }
    */
    MethodBinding8 b = mb.findCompileTimeDeclForRef(targetFuncType, state);
    if (b == null) {
      state.reset();
      mb.findCompileTimeDeclForRef(targetFuncType, state);
      return null;
    }
    return mb.computeInvocationType(state, b, false, targetFuncType.getReturnType());
  }

  static class TypeSubstitution extends AbstractTypeSubstitution {
    final Map<? extends IJavaTypeFormal, ? extends IJavaType> subst;

    public static <T extends IJavaType> IJavaTypeSubstitution create(IBinder b, Map<? extends IJavaTypeFormal, T> s) {
    	if (s.isEmpty()) {
    		return IJavaTypeSubstitution.NULL;
    	}
    	return new TypeSubstitution(b, s);
    }
    
    private <T extends IJavaType> TypeSubstitution(IBinder b, Map<? extends IJavaTypeFormal, T> s) {
      super(b);
      subst = s;
    }

    @Override
    public IJavaType get(IJavaTypeFormal jtf) {
      IJavaType rv = subst.get(jtf);
      if (rv != null) {
        return rv;
      }
      return jtf;
    }

    @Override
    protected <V> V process(IJavaTypeFormal jtf, Process<V> processor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      if (subst.size() <= 1) {
    	return subst.toString();
      } 
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      boolean first = true;
      for(Map.Entry<?, ?> e : subst.entrySet()) {
    	if (first) {
    	  first = false;
    	} else {
    	  sb.append('\t');
    	}
    	sb.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
      }
      return sb.toString();
    }

	@Override
	protected Iterable<? extends IJavaTypeFormal> getFormals() {
		return subst.keySet();
	}
  }
}
