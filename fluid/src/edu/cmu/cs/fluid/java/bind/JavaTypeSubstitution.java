/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/JavaTypeSubstitution.java,v 1.16 2008/07/02 15:45:13 chance Exp $
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;


/**
 * A substitution action to be used when instantiating a parameterized class.
 * Changes (substitutions) may be recorded from the type itself or
 * for the outer class(es).
 * This class is not intended to be cached; it is intended to be 
 * lightweight and emphemeral.
 * @author boyland
 */
public class JavaTypeSubstitution extends AbstractTypeSubstitution {
  private final IJavaDeclaredType declaredType;
  private final List<IJavaType> actuals;
  private final JavaTypeSubstitution context;
  
  private JavaTypeSubstitution(IBinder b, IJavaDeclaredType dt, List<IJavaType> as, JavaTypeSubstitution c) {
    super(b);
    declaredType = dt;
    actuals = as;
    context = c;
  }
  
  public static IJavaTypeSubstitution create(final ITypeEnvironment tEnv, final IJavaDeclaredType jt) {
	  return new IJavaTypeSubstitution() {
		  private IJavaTypeSubstitution realSubst = IJavaTypeSubstitution.NULL;
		  
		  private void ensureSubst() {
			  if (realSubst == IJavaTypeSubstitution.NULL) {
				  /*
				  if (jt.getName().contains("java.util.EnumSet")) {
					  System.out.println("Making subst for "+jt);
				  }
				  */
				  realSubst = JavaTypeSubstitution.createReal(tEnv, jt);
			  }
		  }
		  
		  @Override
      public IJavaType get(IJavaTypeFormal jtf) {
			  ensureSubst();
			  if (realSubst == null) {
				  return jtf;
			  }
			  return realSubst.get(jtf);
		  }

		  @Override
      public List<IJavaType> substTypes(IJavaDeclaredType context, List<IJavaType> types) {
			  if (types.isEmpty()) {
				  return types;
			  }
			  if (jt.equals(context) /* || 
				  jt.getDeclaration().equals(context.getDeclaration()) && jt.getTypeParameters().isEmpty()*/) {
				  return types;
			  }
			  ensureSubst();
			  if (realSubst == null) {
				  return types;
			  }
			  return realSubst.substTypes(context, types);
		  }

		  @Override
      public IJavaTypeSubstitution combine(IJavaTypeSubstitution other) {
			  ensureSubst();
			  if (realSubst == null) {
				  return NULL.combine(other);
			  }
			  return realSubst.combine(other);
		  }

		  @Override
      public ITypeEnvironment getTypeEnv() {
			  ensureSubst();
			  if (realSubst == null) {
				  return null;
			  }
			  return realSubst.getTypeEnv();
		  }
	  };
  }
  
  private static JavaTypeSubstitution createReal(ITypeEnvironment tEnv, IJavaDeclaredType jt) {
    List<IJavaType> tactuals     = jt.getTypeParameters();
    JavaTypeSubstitution nesting = getNesting(tEnv, jt);
    /*
    if (nesting == null) {
      nesting = NULL;
    }
    */
    if (tactuals.isEmpty()) {
      // check if the type is generic
      Operator op = JJNode.tree.getOperator(jt.getDeclaration());
      IRNode types;
      if (ClassDeclaration.prototype.includes(op)) {
        types = ClassDeclaration.getTypes(jt.getDeclaration());
      }
      else if (InterfaceDeclaration.prototype.includes(op)) {
        types = InterfaceDeclaration.getTypes(jt.getDeclaration());
      } 
      else { 
        // Not generic, so nothing to substitute
        return nesting;
      }      
      Iteratable<IRNode> it = TypeFormals.getTypeIterator(types);
      if (it.hasNext() && tEnv != null) {
        tactuals = new ArrayList<IJavaType>(1);
        for(IRNode formal : it) {
          IJavaTypeFormal tf = JavaTypeFactory.getTypeFormal(formal);
          //tactuals.add(tf.getSuperclass(tEnv));
          tactuals.add(tf);
        }
      } else {
        // Not generic, so nothing to substitute
        return nesting;
      }
    } 
    else if (tactuals == null) { 
      return nesting;
    }
    /*
    if (nesting == NULL) {
      nesting = null;
    }
    */
    return new JavaTypeSubstitution(tEnv.getBinder(), jt, tactuals, nesting);
  }
  
  /**
   * @return the substitution corresponding to jt's outer type (if any)
   */
  private static JavaTypeSubstitution getNesting(ITypeEnvironment tEnv, IJavaDeclaredType jt) {
    if (jt instanceof IJavaNestedType) {
      return createReal(tEnv, ((IJavaNestedType)jt).getOuterType());
    } else {
      return null;
    }
  }

  public boolean isNull() {
	  return false; // FIX?
  }
  
  @Override
  protected <V> V process(IJavaTypeFormal jtf, Process<V> processor) {
	// Not right for generic methods/constructors
	final IRNode decl = jtf.getDeclaration();
    final IRNode parent = JJNode.tree.getParent(decl);
    
    final IRNode enclosingDecl = JJNode.tree.getParent(parent);
    final IRNode enclosingType;
    if (TypeDeclaration.prototype.includes(enclosingDecl)) {
    	enclosingType = enclosingDecl;
    } else {
    	enclosingType = VisitUtil.getEnclosingType(enclosingDecl);
    }
    final IRNode typeFormals;
    Operator typeOp = JJNode.tree.getOperator(enclosingType);
    if (InterfaceDeclaration.prototype.includes(typeOp)) {
    	typeFormals = InterfaceDeclaration.getTypes(enclosingType);
    } 
    else if (ClassDeclaration.prototype.includes(typeOp)) {
    	typeFormals = ClassDeclaration.getTypes(enclosingType);
    }   
    else { // Cannot be a generic type
    	//System.out.println("Not a generic type: "+JavaNames.getFullName(enclosingType));
    	return null;
    }
    for (JavaTypeSubstitution s = this; s != null; s = s.context) {
      if (s.declaredType.getDeclaration().equals(enclosingType)) {
    	// Try to match up with the right formal/actual pair
        Iterator<IRNode> ch = JJNode.tree.children(typeFormals); 
        for (IJavaType jt : s.actuals) {
          if (decl.equals(ch.next())) {
            return processor.process(jtf, decl, jt);
          }
        }
      }
    }
    // TODO what if we need to substitute for the supertype?
    return null;
  }
  
  /*public static JavaTypeSubstitution combine(JavaTypeSubstitution around, JavaTypeSubstitution inner) {
    // XXX this is wrong!
    if (inner == null) return null;
    return inner.subst(around);
  }*/
  
  /**
   * Combine two substitutions: we run the parameter substitution on the output of
   * this substitution.
   * @param s substitution to apply to our output
   * @return a (possibly) new substitution with the same domain
   * @deprecated this function has uncertain semantics.
   * Better to form a substitution on the IJavaType underlying the substitution
   */
  @Deprecated
  public JavaTypeSubstitution subst(JavaTypeSubstitution s) {
    if (s == null) return this;
    List<IJavaType> newActuals = s.substTypes(declaredType, actuals);
    JavaTypeSubstitution newContext = context;
    if (newContext != null) newContext = newContext.subst(s);
    if (newActuals == actuals && newContext == context) return this;
    return new JavaTypeSubstitution(this.binder, declaredType, newActuals, newContext);
  }
  
  @Override
  public String toString() {
    StringBuilder args = new StringBuilder();
    args.append('[');
    boolean first = true;
    for (IJavaType t : actuals) {
      if (first) {
        first = false; 
      } else {
        args.append(',');
      }
      args.append(t);
    }
    args.append(']');
    
    final String name = JJNode.getInfo(declaredType.getDeclaration());
    if (context == null) {
      return name + args;
    }
    return context + "." + name + args;
  }
}
