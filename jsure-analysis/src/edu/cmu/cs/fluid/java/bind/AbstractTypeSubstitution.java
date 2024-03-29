/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractTypeSubstitution.java,v 1.1 2007/11/29 17:45:16 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;

public abstract class AbstractTypeSubstitution implements IJavaTypeSubstitution {
  protected final IBinder binder;
  
  AbstractTypeSubstitution(IBinder b) {
    binder = b;
  }
  
  @Override
  public final ITypeEnvironment getTypeEnv() {
	  return binder.getTypeEnvironment();
  }
  
  protected Iterable<? extends IJavaTypeFormal> getFormals() {
	  throw new UnsupportedOperationException();
  }
  
  @Override
  public boolean involves(Set<? extends IJavaTypeFormal> toCheck) {
	  for(IJavaTypeFormal f : getFormals()) {
		  if (toCheck.contains(f)) {
			  return true;
		  }
	  }
	  return false;
  }
  
  abstract class Process<V> {
	  abstract V process(IJavaTypeFormal jtf, IRNode decl, IJavaType jt);
	  abstract V rawSubst();
  }
  
  /**
   * Search for the substitution corresponding to the given type formal
   * (if any)
   */
  @Override
  public IJavaType get(IJavaTypeFormal jtf) {
	  final Process<IJavaType> processor =  new Process<IJavaType>() {
		  @Override
		  IJavaType process(IJavaTypeFormal jtf, IRNode decl, IJavaType jt) {
			  return captureWildcardType(jtf, decl, jt);
		  }

		  @Override
		  IJavaType rawSubst() {
			  return JavaTypeFactory.voidType;
		  }		  
	  };
	  IJavaType rv = process(jtf, processor);
	  if (rv == processor.rawSubst()) {
		  return null;
	  }
	  if (rv == null) {
		  return jtf;
	  }
	  return rv;
  } 
  
  /**
   * @return null if meant to be raw
   */
  protected abstract <V> V process(IJavaTypeFormal jtf, Process<V> processor);
  
  /**
   * Capture the wildcard type, if any
   * Otherwise, returns the original type
 * @param jtf 
   * 
   * @param decl The declaration for a type formal
   * @param jt   The corresponding actual type
   */
  protected final IJavaType captureWildcardType(IJavaTypeFormal jtf, IRNode decl, IJavaType jt) {
	  if (true) {
		  // This is only called from substitutions -- shouldn't be recursive
		  return jt;
	  }
	  if (decl != jtf.getDeclaration()) {
		  throw new IllegalStateException("Type formal doesn't match decl");
	  }
	  return captureWildcardType(binder, jtf, jt, this);
  }
  
  public static final IJavaType captureWildcardType(IBinder binder, IJavaTypeFormal jtf, IJavaType jt, IJavaTypeSubstitution subst) {
    if (jt instanceof IJavaWildcardType) {
      IJavaWildcardType wt = (IJavaWildcardType) jt;
 
      /* 5.1.10 Capture Conversion
      Let G name a generic type declaration with n formal type parameters A1 ... An with 
      corresponding bounds U1 ... Un. There exists a capture conversion from G<T1 ... Tn> 
      to G<S1 ... Sn>, where, for 1in:

          (see below)

          * Otherwise, Si = Ti. 

      Capture conversion on any type other than a parameterized type (�4.5) acts as an identity conversion (�5.1.1). Capture conversions never require a special action at run time and therefore never throw an exception at run time.

      Capture conversion is NOT applied recursively.
      */
      if (wt.getUpperBound() != null) {
    	  // If Ti is a wildcard type argument of the form ? extends Bi, 
    	  // then Si is a fresh type variable whose upper bound is glb(Bi, Ui[A1 := S1, ..., An := Sn]) 
    	  // and whose lower bound is the null type, where glb(V1,... ,Vm) is V1 & ... & Vm. 
    	  // It is a compile-time error if for any two classes (not interfaces) Vi and Vj,Vi is not a subclass of Vj or vice versa.
    	  
    	  // Note that the code below considers the type formals' bounds separately when computing the greatest lower bound
    	  IRNode irBounds        = TypeFormal.getBounds(jtf.getDeclaration());   
    	  IJavaReferenceType glb = JavaTypeFactory.computeGreatestLowerBound(binder, wt.getUpperBound(), irBounds, subst); // TODO subst
    	  return JavaTypeFactory.getCaptureType(wt, JavaTypeFactory.nullType, glb); // 
      } else {
    	  IJavaReferenceType formalBound = (IJavaReferenceType) jtf.getSuperclass(binder.getTypeEnvironment());
    	  IJavaReferenceType fBoundSubst = formalBound;
    	  /*
    	  if (formalBound.equals(jtf)) {
    		  // This would otherwise cause an infinite loop
    		  //fBoundSubst = JavaTypeFactory.selfReference;
    	  } else {
    		  // TODO doesn't handle deeper cycles -- extend the substitution?
    		  // How to get a ref to the eventual capture type?
        	  try {
        		  System.out.println("Trying to substitute "+formalBound+" after "+jtf+" -> "+jt);
        		  fBoundSubst = (IJavaReferenceType) formalBound.subst(subst);
        	  } catch(StackOverflowError e) {
        		  throw e;
        	  }
    	  }
    	  */
    	  if (wt.getLowerBound() != null) {      
    		  // If Ti is a wildcard type argument of the form ? super Bi, 
    		  // then Si is a fresh type variable whose upper bound is Ui[A1 := S1, ..., An := Sn] and whose lower bound is Bi.
    		  return JavaTypeFactory.getCaptureType(wt, wt.getLowerBound(), fBoundSubst); 
    	  } else {
    		  // If Ti is a wildcard type argument (�4.5.1) of the form ? 
    		  // then Si is a fresh type variable whose upper bound is Ui[A1 := S1, ..., An := Sn] and whose lower bound is the null type.
    		  return JavaTypeFactory.getCaptureType(wt, JavaTypeFactory.nullType, fBoundSubst); // 
    	  }
      }
    }
    return jt;
  }
  
  /**
   * Apply this substitution to all types in the input list
   * (which is not modified).  If there are no changes, the result
   * <em>may</em> be identical to the input.  The result should
   * be considered immutable.
   * @param types list of types to substitute
   * @return immutable list of substituted types.
   */
  @Override
  public final List<IJavaType> substTypes(IJavaDeclaredType context, List<IJavaType> types) {
    if (types.isEmpty()) return types;
    boolean changed = false; // FIX unused?
    List<IJavaType> res = new ArrayList<IJavaType>();
    for (IJavaType jt : types) {
      try {
    	IJavaType jtp = jt.subst(this);
    	if (jtp != jt) changed = true;
    	res.add(jtp);
      } catch(StackOverflowError e) {
    	System.out.println("SOE2: "+jt);
      }
    }
    if (!changed) return types;
    return res; //? new ImmutableList(res.toArray())
  }
  
  @Override
  public IJavaTypeSubstitution combine(final IJavaTypeSubstitution other) {
	  if (other == null) {
		  return this;
	  }
	  final IJavaTypeSubstitution me = this;
	  return new AbstractTypeSubstitution(binder) {
		@Override
		public IJavaType get(IJavaTypeFormal jtf) {
			// TODO is this in the right order?
			IJavaType rv = me.get(jtf);
			if (rv == null) {
				// Handle the raw case 
				//rv = jtf.getExtendsBound(binder.getTypeEnvironment()); 
				return null;
			}
			return rv.subst(other);
		}

		@Override
		protected <V> V process(IJavaTypeFormal jtf, Process<V> processor) {
			return null;
		}
		
		@Override
		public String toString() {
			return me.toString()+" + "+other.toString();
		}

		@Override
		public boolean involves(Set<? extends IJavaTypeFormal> formals) {
			return me.involves(formals) || other.involves(formals);
		}
	  };
  }
  
  /**
   * See IBinding.Util.subst() if only using one substitution
   */
  public static IJavaType applySubst(final IJavaType t, IJavaTypeSubstitution... subst) {
	  if (subst.length <= 0) {
		  return t;
	  }
	  IJavaType rv = t;
	  for(IJavaTypeSubstitution s : subst) {
		  rv = rv.subst(s);
	  }
	  if (rv != null) {
		  return rv;
	  }
	  if (t instanceof IJavaTypeFormal) {
		  IJavaTypeFormal f = (IJavaTypeFormal) t;
		  return f.getExtendsBound(subst[0].getTypeEnv());
	  }
	  throw new IllegalStateException("Got null subst on "+t);
  }
}
