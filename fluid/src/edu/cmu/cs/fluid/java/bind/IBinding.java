/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IBinding.java,v 1.13 2008/08/22 16:56:34 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

/**
 * The result of binding in Java 5.
 * @author boyland
 */
public interface IBinding {
  /**
   * Return the node bound here
   * @return node bound here
   */
  public IRNode getNode();
  
  /**
   * Return the type of the object from which this declaration is taken, with the
   * appropriate type substitutions (e.g., the declaring type)
   * (Should be the supertype of getReceiverType())
   * 
   * This includes all the actual type parameters.
   * @return type of the object from which this binding is taken, or null for locals/types
   */
  public IJavaDeclaredType getContextType();
  
  /**
   * @return the type of the receiver in the call/field ref being bound
   */
  public IJavaReferenceType getReceiverType();
  
  public ITypeEnvironment getTypeEnvironment();
  
  /**
   * Perform a type substitution on the given type.
   * Essential this call does
   * <pre>
   * ty.subst(JavaTypeSubstitution.create((IJavaDeclaredType)getContextType())
   * </pre>
   * except that if the context type is not a declared type, <tt>ty</tt>
   * is returned unchanged.
   * @param ty type to substitute with
   * @return substitued type.
   */
  public IJavaType convertType(IJavaType ty);
  
  public static class Null implements IBinding {
	  IJavaTypeSubstitution getSubst() {
		  return null;
	  }
	  public IJavaType convertType(IJavaType ty) {
		  return ty;
	  }
	  public IJavaDeclaredType getContextType() {
		  return null;
	  }
	  public IJavaReferenceType getReceiverType() {
		  return null;
	  }
	  public IRNode getNode() {
		  return null;
	  }
	  public ITypeEnvironment getTypeEnvironment() {
		  return null;
	  }
      @Override
      public String toString() {
        if (getNode() == null) return "<none>";
        if (getSubst() == null) return getNode().toString();
        else return getNode() + "@" + getSubst();
      }
      @Override
      public boolean equals(Object o) {
        if (o instanceof IBinding) {
          IBinding binding = (IBinding)o;
          if (getNode() == null) return binding.getNode() == null;
          return getNode().equals(binding.getNode()) && 
                 getContextType() == binding.getContextType();
        }
        return false;
      }
      @Override
      public int hashCode() {
        int h = 0;
        if (getNode() != null) h += getNode().hashCode();
        if (getContextType() != null) h += (getContextType().hashCode())*33;
        return h;
      }
  }
  
  public static final IBinding NULL = new Null();
  
  /**
   * Helper functions for IBinding
   * @author boyland
   */
  public static class Util {
    
    public static IBinding makeBinding(final IRNode n, final IJavaDeclaredType ty, ITypeEnvironment tEnv, 
        final IJavaReferenceType recType) {
      return makeBinding(n, ty, tEnv, recType, null);
    }
    
    public static IBinding makeMethodBinding(IBinding mbind, IJavaTypeSubstitution mSubst) {
      return makeBinding(mbind.getNode(), mbind.getContextType(), 
                         mbind.getTypeEnvironment(), mbind.getReceiverType(), mSubst);
    }
    
    private static IBinding makeBinding(final IRNode n, final IJavaDeclaredType ty, final ITypeEnvironment tEnv, 
                                       final IJavaReferenceType recType, final IJavaTypeSubstitution mSubst) {
      // we might wish to create node classes which satisfy IBinding with null substitution
      if (n instanceof IBinding && ty == null) return (IBinding)n;
      final IJavaTypeSubstitution subst;
      if (ty != null) {
        subst = JavaTypeSubstitution.create(tEnv, ty);
      } else {
        subst = null;
      }
//      if (n == null) {
//        System.out.println("Null");
//      }
      if (mSubst == null) {
          return new PartialBinding(n, ty, tEnv, recType) {
          	@Override
              public IJavaType convertType(IJavaType type) {
                if (type == null) return null;
                return type.subst(subst);
              }
            };  
      }
      return new PartialBinding(n, ty, tEnv, recType) {
    	@Override
        public IJavaType convertType(IJavaType type) {
          if (type == null) return null;
          if (mSubst != null) {
            return type.subst(mSubst).subst(subst);
          }
          return type.subst(subst);
        }
      };
    }
    
    public static class PartialBinding extends NodeBinding {
		private final IJavaDeclaredType ty;
		private final IJavaReferenceType recType;
		private final ITypeEnvironment tEnv;
		
		PartialBinding(IRNode n, IJavaDeclaredType context, ITypeEnvironment te, 
				       IJavaReferenceType receiver) {
			super(n);
			ty      = context;
			recType = receiver;
			tEnv    = te;
		}
        @Override
    	public final IJavaDeclaredType getContextType() {
    		return ty;
    	}
        @Override
    	public final IJavaReferenceType getReceiverType() {
    		return recType;
    	}
        @Override
    	public final ITypeEnvironment getTypeEnvironment() {
    		return tEnv;
    	}
    }
    
	public static IBinding makeBinding(final IRNode binding, final IJavaDeclaredType context,
			                           final ITypeEnvironment typeEnvironment) {
		if (context == null) {
			return makeBinding(binding, typeEnvironment);
		}
		return makeBinding(binding, context, typeEnvironment, null, null);
	}
    
	public static IBinding makeBinding(final IRNode binding,
			                           final ITypeEnvironment typeEnvironment) {
		if (typeEnvironment == null) {
			return makeBinding(binding);
		}
		return new NodeBinding(binding) {
			@Override public ITypeEnvironment getTypeEnvironment() {
				return typeEnvironment;
			}
		};
	}	
	
	public static IBinding makeBinding(final IRNode n) {
		return new NodeBinding(n);
	}
    
	public static class NodeBinding extends Null {
		private final IRNode n;
		
		NodeBinding(IRNode n) {
			this.n = n;
		}

		@Override 
		public final IRNode getNode() { 
			return n; 
		}
	}
	
    public static String debugString(IBinding binding) {
      if (binding == null || binding.getNode() == null) return "<none>";
      String s = DebugUnparser.toString(binding.getNode());
      IJavaDeclaredType ty = binding.getContextType();
      if (ty == null) return s;
      else return s + "@" + ty;
    }
  }
}
