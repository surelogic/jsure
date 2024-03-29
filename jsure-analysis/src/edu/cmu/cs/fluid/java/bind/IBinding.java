/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IBinding.java,v 1.13 2008/08/22 16:56:34 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.Nullable;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * The result of binding in Java 5.
 * 
 * Consider:
 * <pre>
 *	class Map<K,V> { 
 *	    public K first();
 *	    ... // no "iterator" method 
 *      }
 *
 *	class Set<E> extends Map<E,Boolean> implements Cloneable {
 *	    public Iterator<E> iterator() { ... }
 *	    ... // no "first" method 
 *	}
 *
 *
 *	Set<String> s = ...
 *
 *	... s.iterator() ...
 *	... s.first() ...
 * </pre>
 * For the first method call, the context type is indeed the receiver type
 *	 Set<String>    E -> String
 * For the second method call, the context type is the relevant supertype
 *	 Map<String,Boolean>   K -> String, V -> Boolean
 * 
 * @author boyland
 */
public interface IBinding {
  /**
   * Return the node bound here
   * @return node bound here
   */
  public IRNode getNode();
  
  /**
   * This returns the correctly substituted supertype relevant to the 
   * *declaration* returned by getNode() (e.g. of the method being called).
   * 
   * Return the type of the object from which this declaration is taken, with the
   * appropriate type substitutions (e.g., the declaring type)
   * (Should be a (perhaps improper) supertype of getReceiverType())
   * This includes all the actual type parameters.
   * 
   * @return type of the object from which this binding is taken, or null for locals/types
   */
  @Nullable
  public IJavaDeclaredType getContextType();
  
  /**
   * @return the type of the receiver in the call/field ref being bound
   */
  @Nullable
  public IJavaReferenceType getReceiverType();
  
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
  public IJavaType convertType(IBinder binder, IJavaType ty);
  
  public IJavaTypeSubstitution getSubst();
  
  public static class Null implements IBinding {
	  @Override
	  public IJavaTypeSubstitution getSubst() {
		  return null;
	  }
	  @Override
	  public IJavaType convertType(IBinder binder, final IJavaType ty) {
		  return ty;
	  }
	  
	  @Override
    public IJavaDeclaredType getContextType() {
		  return null;
	  }
	  @Override
    public IJavaReferenceType getReceiverType() {
		  return null;
	  }
	  @Override
    public IRNode getNode() {
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
    //private static final Logger LOG = SLLogger.getLogger("fluid.java.bind");
	  
	/**
	 * A wrapper to handle null results at the outermost level
	 */
    public static IJavaType subst(IJavaType orig, IJavaTypeSubstitution subst) {
    	IJavaType substituted = orig.subst(subst);
    	if (substituted == null) {
    		if (orig instanceof IJavaTypeFormal) {
    			IJavaTypeFormal tf = (IJavaTypeFormal) orig;
    			return tf.getExtendsBound(subst.getTypeEnv()).subst(subst);
    		} 
    		throw new IllegalStateException();
    	}
    	return substituted;
    }
	  
    public static IBinding makeMethodBinding(IBinding mbind, IJavaDeclaredType context, IJavaTypeSubstitution mSubst, IJavaType recType, ITypeEnvironment tEnv) {
      if (context == null && mSubst == null && (recType == null || recType == mbind.getReceiverType())) {
    	  // No need to create a new binding
    	  return mbind;
      }
      /*
      if (recType != null && mbind.getReceiverType() != null && recType != mbind.getReceiverType() 
    		              && mbind.getContextType() != mbind.getReceiverType()) {    	  
    	  System.out.println("Replacing "+mbind.getReceiverType()+" with "+recType);
      }
      */
      return makeBinding(mbind.getNode(), 
    		             context == null ? mbind.getContextType() : context, 
    		             tEnv,
                         recType == null ? mbind.getReceiverType() : (IJavaReferenceType) recType, 
                         mSubst == null ? mbind.getSubst() : mSubst);
    }
    
    /**
     * Generate a binding where the recType and the context type are the same.
     * (e.g. for constructors)
     * 
     * @param tenv type environment
     * @param mdecl method declaration
     * @param recType receiver type
     * @param mSubst substitution for the method's type formals.
     * @return
     */
    public static IBinding makeMethodBinding(final ITypeEnvironment tenv, final IRNode mdecl, final IJavaDeclaredType recType, final IJavaTypeSubstitution mSubst) {
    	IRNode tdecl = recType.getDeclaration();
    	List<IJavaTypeFormal> tFormals = new ArrayList<IJavaTypeFormal>();
    	for (IRNode tf : JJNode.tree.children(TypeUtils.getParametersForType(tdecl))) {
    		tFormals.add(JavaTypeFactory.getTypeFormal(tf));
    	}
    	final IJavaTypeSubstitution tSubst = new SimpleTypeSubstitution(tenv.getBinder(),tFormals,recType.getTypeParameters());    	
    	final IJavaTypeSubstitution subst = tSubst.combine(mSubst);
    	return new PartialBinding(mdecl,recType,recType,tenv) {
    		@Override
    		public IJavaType convertType(IBinder binder, IJavaType type) {
    	          if (type == null) return null;
    	          
    	          type = super.convertType(binder, type);
                  return Util.subst(type, subst);
    		}
			@Override
			public IJavaTypeSubstitution getSubst() {
				return subst;
			}
    	};
    }
    
    /**
     * @param n
     * @param ty
     * @param tEnv
     * @param recType
     * @param mSubst
     * @return
     */
    private static IBinding makeBinding(final IRNode n, final IJavaDeclaredType ty, final ITypeEnvironment tEnv, 
                                        IJavaReferenceType recType, final IJavaTypeSubstitution mSubst) {
      // we might wish to create node classes which satisfy IBinding with null substitution
      if (n instanceof IBinding && ty == null) return (IBinding)n;
      /* 
       * Note that this relationship may not be true due to a lack of substitutions
       * 
      if (recType != null && !recType.isSubtype(tEnv, ty)) {
    	  System.err.println("Receiver type "+recType+" isn't a subtype of the context type "+ty);
    	  recType.isSubtype(tEnv, ty);
      }
      */
      if (recType == null) {
    	  recType = ty;
      }
      
      final IJavaTypeSubstitution subst;
      if (ty != null && tEnv.getMajorJavaVersion() >= 5) {
        subst = JavaTypeSubstitution.create(tEnv, ty).combine(mSubst);
      } else {
        subst = mSubst;
      }
//      if (n == null) {
//        System.out.println("Null");
//      }
      /*
      final String unparse = DebugUnparser.toString(n);
      if ("final native public binary java.lang.Class <?> getClass() <compiled>;".equals(unparse)) {
    	System.out.println("Got getClass() with recType "+recType);  
      }
      */
      return new PartialBinding(n, ty, recType, tEnv) {
    	  @Override
    	  public IJavaType convertType(IBinder binder, IJavaType type) {
    		  if (type == null) return null;
    		  return Util.subst(super.convertType(binder, type), subst);
    	  }

    	  @Override
    	  public IJavaTypeSubstitution getSubst() {
    		  return subst;
    	  }
      };  
    }
    
    public static abstract class PartialBinding extends NodeBinding {
		private final IJavaDeclaredType contextType;
		private final IJavaReferenceType recType;
		private final ITypeEnvironment tEnv;
		
		PartialBinding(IRNode n, IJavaDeclaredType context,
				       IJavaReferenceType receiver, ITypeEnvironment te) {
			super(n);
			contextType = context;
			recType = receiver;
			tEnv = te;
		}
        @Override
    	public final IJavaDeclaredType getContextType() {
    		return contextType;
    	}
        @Override
    	public final IJavaReferenceType getReceiverType() {
    		return recType;
    	}
        @Override
        public IJavaType convertType(IBinder binder, final IJavaType ty) {
        	if (!TypeUtil.isStatic(getNode()) && contextType.isRawType(tEnv)) {
        		return tEnv.computeErasure(ty);
        	}
        	return ty;
        }
        @Override
        public abstract IJavaTypeSubstitution getSubst();
    }
    
	public static IBinding makeBinding(final IRNode binding, final IJavaDeclaredType context,
			                           final ITypeEnvironment typeEnvironment) {
		if (context == null) {
			return makeBinding(binding);
		}
		return makeBinding(binding, context, typeEnvironment, null, null);
	}
	
	public static IBinding makeBinding(final IRNode binding, final IJavaDeclaredType context,
            final ITypeEnvironment typeEnvironment, final IJavaType recType) {
		if (recType == null) {
			return makeBinding(binding, context, typeEnvironment);
		}
		return makeBinding(binding, context, typeEnvironment, (IJavaReferenceType) recType, null);
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
		
        @Override
        public String toString() {
      	  return DebugUnparser.toString(n);
        }
	}
	
    public static String debugString(IBinding binding) {
      if (binding == null || binding.getNode() == null) return "<none>";
      String s = DebugUnparser.toString(binding.getNode());
      IJavaDeclaredType ty = binding.getContextType();
      if (ty == null) return s;
      else return s + "@" + ty;
    }

    /**
     * Create a binding that includes the given receiver type
     * 
     * TODO need to fix the context type to match?
     */
	public static IBinding addReceiverType(IBinding bind, IJavaType receiverType, ITypeEnvironment tEnv) {		
		return makeBinding(bind.getNode(), bind.getContextType(), tEnv,  
				(IJavaReferenceType) receiverType, null);
	} 
  }
}
