/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaScope.java,v 1.28 2008/08/26 20:45:53 chance Exp $
 */
package edu.cmu.cs.fluid.java.bind;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AppendIterator;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.Iteratable;


/**
 * A representation of the scope when doing binding.
 * @author boyland
 */
public interface IJavaScope {
  public static final Logger LOG = SLLogger.getLogger("FLUID.java");

  public static final EmptyIterator<IBinding> EMPTY_BINDINGS_ITERATOR = new EmptyIterator<IBinding>();
  
  /**
   * Look in the scope to find a declaration that matches the given selector.
   * @param name name to look up in the scope
   * @param useSite use site to mark for incrementality.  We mark the use site as changed
   *        whenever the lookup results might be different.
   * @param selector control over what nodes to return
    * @return an IR node or null
   */
  public IBinding lookup(String name, IRNode useSite, IJavaScope.Selector selector);
  
  /**
   * Look in the scope to find all declarations that match the given selector.
   * This method is used for overloading resolution of methods and constructors.
   * It is not a generic ``find all'' implementation.  In particular, it will ignore
   * scopes that are <em>known</em> not to include method declarations, and it uses
   * Java's rules on method overloading: inherited methods join the ``fun,'' but
   * not methods from different nesting levels.
   * @param name name to look up in the scope
   * @param useSite use site to mark for incrementality.  We mark the use site as changed
   *        whenever the lookup results might be different.
   * @param selector control over what nodes to return
   * @return a non-null iterator of IR nodes
   */
  public Iteratable<IBinding> lookupAll(String name, IRNode useSite, IJavaScope.Selector selector);
  
  public void printTrace(PrintStream out, int indent);
  
  public static final IJavaScope nullScope = new IJavaScope() {
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      return null;
    }
    public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      return EMPTY_BINDINGS_ITERATOR;
    }
    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[null]");
    }
  };
  
  /**
   * Selection criteria for nodes to be found with lookup.  A selector is
   * used to (for example) restrict a search to only find type declarations.
   * @author boyland
   */
  public static interface Selector {
    /**
     * Return true if this node matches the selection criteria of this class.
     * @param node IR node in a Java AST, usually a declaration node
     * @return whether the node matches the cirtieria
     */
    public boolean select(IRNode node);

    public String label();
  }
  
  public static abstract class AbstractSelector implements Selector {
    private final String label;
    
    AbstractSelector(String l) {
      label = l;  
    }
    
    public String label() {
      return label;
    }
  }
  
  public static class Util {
    public static Selector combineSelectors(final Selector s1, final Selector s2) {
      return new Selector() {
        public boolean select(IRNode n) {
          return s1.select(n) && s2.select(n);
        }
        public String label() {
          return s1.label()+" & "+s2.label();
        }
      };
    }
    
    public static Selector eitherSelector(final Selector s1, final Selector s2) {
        return new Selector() {
          public boolean select(IRNode n) {
            return s1.select(n) || s2.select(n);
          }
          public String label() {
            return s1.label()+" | "+s2.label();
          }
        };
      }
    
    public static final Selector isTypeDecl = new AbstractSelector("Only type decls") {
      public boolean select(IRNode node) {
        return isTypeDecl(node);
      }
    };
    
    public static final Selector isPkgTypeDecl = new AbstractSelector("Only type/pkg decls") {
        public boolean select(IRNode node) {
          final Operator op = JJNode.tree.getOperator(node);
          return isTypeDecl(op) || op instanceof PackageDeclaration;
        }
    };
    
    public static IBinding lookupType(IJavaScope scope, String name, IRNode useSite) {
      if (scope == null) {
      	  return null;
      }
      return scope.lookup(name,useSite,isTypeDecl);
    }
    
    public static final Selector isPackageDecl = new AbstractSelector("Only package decls") {
      public boolean select(IRNode node) {
        return JJNode.tree.getOperator(node) instanceof NamedPackageDeclaration;
      }
    };
    public static IBinding lookupPackage(IJavaScope scope, String name, IRNode useSite) {
      if (scope == null) {
    	  return null;
      }
      return scope.lookup(name,useSite,isPackageDecl);
    }
    
    public static final Selector isConstructorDecl = new AbstractSelector("Only constructors") {
      public boolean select(IRNode node) {
        return JJNode.tree.getOperator(node) instanceof ConstructorDeclaration;
      }
    };
    
    public static final Selector isMethodDecl = new AbstractSelector("Only methods") {
      public boolean select(IRNode node) {
        return JJNode.tree.getOperator(node) instanceof MethodDeclaration;
      }      
    };
    
    public static Iterator<IBinding> lookupCallable(IJavaScope scope, String name, IRNode useSite, 
    		                                        Selector isAccessible, boolean needMethod) {
      return scope.lookupAll(name,useSite, combineSelectors(needMethod ? isMethodDecl : isConstructorDecl, isAccessible));
    }
    
    public static final Selector isValueDecl = new AbstractSelector("Only value decls") {
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof VariableDeclarator || op instanceof ParameterDeclaration
         || op instanceof EnumConstantDeclaration;
      }      
    };
    public static IBinding lookupValue(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,isValueDecl);
    }
    
    public static final Selector isAnnotationElt = new AbstractSelector("Only annotation elements") {
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof AnnotationElement;
      }      
    };
    
    public static IBinding lookupAnnotationElt(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,isAnnotationElt);
    }
    
    public static final Selector isAnnoEltOrNoArgMethod = eitherSelector(isAnnotationElt, new AbstractSelector("") {
      public boolean select(IRNode node) {
			final Operator op = JJNode.tree.getOperator(node);
			if (op instanceof MethodDeclaration) {
				final IRNode params = MethodDeclaration.getParams(node);
				return !JJNode.tree.hasChildren(params);
			}
			return false;
		}
	});
    
    public static final Selector isReceiverDecl = new AbstractSelector("Only receiver decls") {
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof ReceiverDeclaration;
      }
    };
    public static IBinding lookupReceiver(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,isReceiverDecl);
    }
    
    public static final Selector isLabeledStatement = new AbstractSelector("Only labeled stmts") {
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof LabeledStatement;
      }
    };
    public static IBinding lookupLabel(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,isLabeledStatement);
    }
        
    public static final Selector isReturnValue = new AbstractSelector("Only return values") {
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof ReturnValueDeclaration;
      }
    };
    public static IBinding lookupReturnValue(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,isReturnValue);
    }
    
    public static final Selector couldBeNonTypeName = new AbstractSelector("Could bind to a name (not a type)") {
        public boolean select(IRNode node) {
          Operator op = JJNode.tree.getOperator(node);
          return !(op instanceof SomeFunctionDeclaration) && !(op instanceof AnnotationElement) &&
                 !(op instanceof LabeledStatement) && 
                 !(TypeDeclaration.prototype.includes(op) && !(op instanceof EnumConstantClassDeclaration));
        }      
      };
    
    public static final Selector couldBeName = new AbstractSelector("Could bind to a name") {
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return !(op instanceof SomeFunctionDeclaration) && !(op instanceof AnnotationElement) &&
               !(op instanceof LabeledStatement);
      }      
    };
    public static IBinding lookupName(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,couldBeName);
    }
    
    public static final Selector isntType = new AbstractSelector("Not type decl") {
      public boolean select(IRNode node) {
        return !isTypeDecl(node);
      }      
    };
    public static IBinding lookupNonType(IJavaScope scope, String name, IRNode useSite) {
      return scope.lookup(name,useSite,isntType);
    }
    
    public static final Selector isStatic = new AbstractSelector("Only static") {
      public boolean select(IRNode node) {
    	Operator op = JJNode.tree.getOperator(node);
    	if (VariableDeclarator.prototype.includes(op)) {
    		return JavaNode.getModifier(VariableDeclarator.getMods(node), 
    				                    JavaNode.STATIC);
    	}
    	if (NestedTypeDeclaration.prototype.includes(op) || 
    		NestedEnumDeclaration.prototype.includes(op) ||
    		NestedAnnotationDeclaration.prototype.includes(op)) {
    		return true;
    	}
    	if (EnumConstantDeclaration.prototype.includes(op)) {
    		return true;
    	}
        return JavaNode.getModifier(node,JavaNode.STATIC);
      }      
    };
    
    public static final Selector isDecl = eitherSelector(isValueDecl, 
    		                                             eitherSelector(isTypeDecl, isMethodDecl));
    
    public static boolean isTypeDecl(final Operator op) {
    	return op instanceof TypeDeclInterface && 
    	// excluded because it really act as a field/constant declaration, despite how it's implemented
    	!(op instanceof EnumConstantClassDeclaration); 
    }
    
    /**
     * @param node
     * @return
     */
    public static boolean isTypeDecl(IRNode node) {
      return isTypeDecl(JJNode.tree.getOperator(node));
    }
    
    public static boolean isPackageDecl(IRNode node) {
      return JJNode.tree.getOperator(node) instanceof NamedPackageDeclaration;
    }
    
    public static boolean isFieldDecl(IRNode node) {
      IRNode gp = JJNode.tree.getParent(JJNode.tree.getParent(node));
      return JJNode.tree.getOperator(node) instanceof VariableDeclarator &&
        JJNode.tree.getOperator(gp) instanceof FieldDeclaration;
    }
    
    public static boolean isMethodDecl(IRNode node) {
      return JJNode.tree.getOperator(node) instanceof MethodDeclaration;
    } 
  }
  
  /**
   * A Scope that can only be used to find certain things.
   * For example a static import cannot be used to find types, a regular import
   * to find methods.
   * @author boyland
   */
  public static class SelectedScope implements IJavaScope {
    private final IJavaScope scope;
    private final Selector selector;
    public SelectedScope(IJavaScope sc, Selector sel) {
      scope = sc;
      selector = sel;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookup(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    public IBinding lookup(String name, IRNode useSite, Selector sel) {
      return scope.lookup(name,useSite,Util.combineSelectors(selector,sel));
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookupAll(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector sel) {
      return scope.lookupAll(name,useSite,Util.combineSelectors(selector,sel));
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[SelectedScope:"+selector.label()+"]");
      scope.printTrace(out, indent+2);
    }
  }
  
  /**
   * A Scope that is logically nested in another scope.
   * This scope assumes that it is never involved with overloading and thus will never
   * be used for {@link #lookupAll(String, IRNode, Selector)}.
   * This kind of scope should only be used by one thread while it
   * is updating code for one version.  It is innocent of versioning,
   * and in particular ignores the use-site parameter for lookups except
   * in passing it to the outer scope.
   * @author boyland
   */
  public static class NestedScope implements IJavaScope {
    private Map<String,IBinding> locals = null;
    private final IJavaScope outer;
    public NestedScope(IJavaScope o) {
      if (o == null) throw new NullPointerException("Cannot nest inside the null scope");
      outer = o;
    }

    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      boolean debug = LOG.isLoggable(Level.FINER);
      if (debug) {
        LOG.finer("Looking for " + name + " in " + this);
      }
      if (locals != null) {
        IBinding found = locals.get(name);
        if (found != null && found.getNode() != null && selector.select(found.getNode())) {
          if (debug) LOG.finer("Found candidate " + found);
          return found;
        }
      }
      return outer.lookup(name,useSite,selector);
    }
    
    public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      return outer.lookupAll(name,useSite,selector);
    }
    
    /**
     * Add a new binding to the local scope.  The previous binding is returned
     * so that it can be restored once the binding is no longer needed.
     * To restore, simply call this method with the old binding (even if it is null).
     * @param name name for the new binding
     * @param binding new binding
     * @return old binding (or null)
     */
    public IBinding put(String name, IBinding binding) {
      if (locals == null) locals = new HashMap<String,IBinding>();
      if (binding == null) {
        return locals.remove(name);
      }
      return locals.put(name,binding);
    }

    /**
     * Add a node to the local scope using its name as a key.
     * @param node declaration node with INFO set
     */
    public void add(IRNode node) {
      if (node != null) {
        String name = JJNode.getInfo(node);
        put(name,node);
      }
    }

    /**
     * Add a new node to the local scope.
     * @param name name to use
     * @param node node to bind to
     */
    public IRNode put(String name, IRNode node) {
      IBinding binding = put(name,IBinding.Util.makeBinding(node));
      if (binding == null) return null;
      return binding.getNode();
    }

    public void printTrace(PrintStream out, int indent) {
      if (locals == null) {
        DebugUtil.println(out, indent, "[Empty nested scope]");
      } else {
        DebugUtil.println(out, indent, "[Nested"+hashCode()+"]");
        for(String l : locals.keySet()) {
          DebugUtil.println(out, indent+2, l); 
        }
      }
      outer.printTrace(out, indent);
    }
  }

  /**
   * A nested scope in which we may have local overriding and thus 
   * {@link #lookupAll(String, IRNode, Selector)} may return some local
   * overridings.  This nested scope is also innocent of versioning and
   * can only be used within a single binding thread.
   * @author boyland
   */
  public static class OverloadingNestedScope implements IJavaScope {
    public final IJavaScope outer;
    private final Map<String,List<IBinding>> locals = new HashMap<String,List<IBinding>>(); // map names to lists

    public OverloadingNestedScope(IJavaScope sc) {
      outer = sc;
    }
    
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      List<IBinding> l = locals.get(name);
      if (l == null) return outer.lookup(name,useSite,selector);
      for (IBinding bind : l) {
        if (selector.select(bind.getNode())) {
          return bind;
        }
      }
      return outer.lookup(name,useSite,selector);
    }
    
    @SuppressWarnings("unchecked")
	public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      List<IBinding> l = locals.get(name);
      if (l == null) return outer.lookupAll(name,useSite,selector);
      Vector<IBinding> selected = null;
      for (IBinding binding : l) {
        if (selector.select(binding.getNode())) {
          if (selected == null) selected = new Vector<IBinding>();
          selected.addElement(binding);
        }
      }
      Iteratable<IBinding> outerResult = outer.lookupAll(name,useSite,selector);
      if (selected == null || selected.isEmpty()) return outerResult;
      return (Iteratable<IBinding>) AppendIterator.append(selected.iterator(),outerResult);
    }
    
    /**
     * Add a new binding to the local scope.  It overloads any previous binding.
     * @param name name for the new binding
     * @param node node for the new binding
     */
    public void put(String name, IBinding node) {
      List<IBinding> l = locals.get(name);
      if (l == null) {
        l = new ArrayList<IBinding>();
        locals.put(name,l);
      }
      l.add(node);
    }

    /**
     * Add a node to the local scope using its name as a key.
     * @param node declaration node with INFO set
     */
    public void add(IBinding binding) {
      if (binding.getNode() != null) put(JJNode.getInfo(binding.getNode()),binding);
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[OverloadingNested"+hashCode()+"]"+locals);    
      for(String l : locals.keySet()) {
        DebugUtil.println(out, indent+2, l); 
      }
      outer.printTrace(out, indent);
    }
    
  }
  
  /**
   * Extend a scope with more entries.  In the case of looking
   * up all, we try both scopes.
   * @author boyland
   */
  public static class ExtendScope implements IJavaScope {
    final IJavaScope scope1;
    final IJavaScope scope2;

    public ExtendScope(IJavaScope sc1, IJavaScope sc2) {
      scope1 = sc1;
      scope2 = sc2;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookup(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      IBinding result = scope1.lookup(name,useSite,selector);
      if (result == null) {
    	  try {
    		  result = scope2.lookup(name,useSite,selector);
    	  } catch (StackOverflowError e) {
    		  System.out.println("Overflow on "+DebugUnparser.toString(useSite));
    		  throw e;
    	  }
      }
      return result;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookupAll(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    @SuppressWarnings("unchecked")
	public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      Iteratable<IBinding> result1 = scope1.lookupAll(name,useSite,selector);
      Iteratable<IBinding> result2 = scope2.lookupAll(name,useSite,selector);
      if (result1.hasNext()) {
        if (result2.hasNext()) {
          return (Iteratable<IBinding>) AppendIterator.append(result1, result2);
        } else {
          return result1;
        }
      }
      return result2;
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[ExtendScope "+hashCode()+"-1]");
      scope1.printTrace(out, indent+2);
      DebugUtil.println(out, indent, "[ExtendScope "+hashCode()+"-2]");
      scope2.printTrace(out, indent+2);
      DebugUtil.println(out, indent, "[End ExtendScope "+hashCode()+"]");
    }  
  }
  
  /**
   * A Scope that tries to find something in one scope,
   * and only if completely unsuccessful, looks in the other scope.
   * Even if all bindings are desired, we look in only one scope.
   * This matches the semantics of Java's nested classes, where methods
   * from different levels don't overload each other.
   * (At least I don't <em>think</em> they do!)
   * @author boyland
   */
  public static class ShadowingScope implements IJavaScope {
    final IJavaScope scope1;
    final IJavaScope scope2;

    public ShadowingScope(IJavaScope sc1, IJavaScope sc2) {
      scope1 = sc1;
      scope2 = sc2;
    }
    
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      IBinding result = scope1.lookup(name,useSite,selector);
      if (result == null)
        return scope2.lookup(name,useSite,selector);
      return result;
    }
    public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      Iteratable<IBinding> result = scope1.lookupAll(name,useSite,selector);
      if (!result.hasNext())
        return scope2.lookupAll(name,useSite,selector);
      return result;
    }
    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[ShadowingScope"+hashCode()+"-1]");
      scope1.printTrace(out, indent+2);
      DebugUtil.println(out, indent, "[ShadowingScope"+hashCode()+"-2]");
      scope2.printTrace(out, indent+2);
    }  
  }
  
  /**
   * A scope wrapper in which all bindings are subject to a substitution.
   * @author boyland
   */
  public static class SubstScope implements IJavaScope {
    final IJavaScope scope;
    final IJavaTypeSubstitution subst;
    final IJavaDeclaredType context;
    final ITypeEnvironment tEnv;
    
    /**
     * Create a scope in which we are fetching through something of the given type.
     * @param sc
     * @param ty must not be null
     */
    public SubstScope(IJavaScope sc, final ITypeEnvironment tEnv, final IJavaDeclaredType ty) {
      scope = sc;
      context = ty;
      subst = JavaTypeSubstitution.create(tEnv, ty);
      this.tEnv = tEnv;
    }
    
    /**
     * Substitute a binding using the local substitution
     * @param binding binding to substitute for
     * @return new binding using substitution
     */
    private IBinding substBinding(final IBinding binding) {
      final IJavaDeclaredType bContext = binding.getContextType();
      if (bContext == null) {
        // need to find the proper context.
        IRNode node = binding.getNode();
        IRNode bdecl = VisitUtil.getEnclosingType(node);
        if (context.getDeclaration() == bdecl) {
          IBinding newBinding = IBinding.Util.makeBinding(node,context,tEnv, context);
          return newBinding;
        }
        //LOG.warning("substBinding didn't expect to get here...");
        IJavaDeclaredType c = computeContextType(bdecl);
        if (c == null) {
        	computeContextType(bdecl);
        }
        return IBinding.Util.makeBinding(node,c, tEnv, context);
      }
      if (context == bContext) {
    	  // No need to do substitution, since we're talking about a binding from the same type
    	  return binding; 
      }
      if (subst == null) {
        // return binding;
        return IBinding.Util.makeBinding(binding.getNode(),bContext, tEnv, context);
      }
      return IBinding.Util.makeBinding(binding.getNode(),bContext.subst(subst), tEnv, context);
      /* To delay the substitution until later
      return new IBinding.Util.NodeBinding(binding.getNode()) {
    	  IBinding real = null;
    	  
    	  private void computeBinding() {
    		  if (real != null) {
    			  return;
    		  }
    		  real = IBinding.Util.makeBinding(binding.getNode(),bContext.subst(subst), tEnv, context);
    	  }

    	  @Override
    	  public ITypeEnvironment getTypeEnvironment() {
    		  return tEnv;
    	  }
    	  
    	  @Override
    	  public IJavaType convertType(IJavaType ty) {
    		  computeBinding();
    		  return real.convertType(ty);
    	  }

    	  @Override
    	  public IJavaDeclaredType getContextType() {
    		  computeBinding();
    		  return real.getContextType();
    	  }

    	  @Override
    	  public IJavaReferenceType getReceiverType() {
    		  computeBinding();
    		  return real.getReceiverType();
    	  }
      };
      */
    }
    
    private IJavaDeclaredType computeContextType(final IRNode bdecl) {
        IJavaDeclaredType c = findContextType(bdecl, context);
        if (c == null) {
        	// Reset to look at outer classes
        	c = context;
        	
        	while (c != null) {        
        		if (c instanceof IJavaNestedType) {
        			c = ((IJavaNestedType)c).getOuterType();
        		} else {
        			break;
        		}
        		IJavaDeclaredType rv = findContextType(bdecl, c);
        		if (rv != null) {
        			return rv;
        		}
        	}
        }
        return c;
    }
    
    private IJavaDeclaredType findContextType(final IRNode bdecl, IJavaDeclaredType here) {
    	if (here.getDeclaration() == bdecl) {
    		return here;
    	}
    	for(IJavaType t : here.getSupertypes(tEnv)) {
    		if (t instanceof IJavaDeclaredType) {
    			IJavaDeclaredType dt = (IJavaDeclaredType) t;
    			IJavaDeclaredType rv = findContextType(bdecl, dt);
    			if (rv != null) {
    				return rv;
    			}
    		} 
    	}
    	return null;
    }
    
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      IBinding result = scope.lookup(name,useSite,selector);
      if (result == null) return null;
      /*
      if (name.equals("foundUnlock") &&
    	  context != null && context.getName().contains("MustHoldTransfer")) {
    	  return substBinding(result);
      }
      */
      return substBinding(result);
    }

    public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      Iteratable<IBinding> result = scope.lookupAll(name,useSite,selector);
      if (!result.hasNext()) return result;
      return new FilterIterator<IBinding,IBinding>(result) {
        @Override
        protected Object select(IBinding o) {
          return substBinding(o);
        }     
      };
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[Subst : "+subst+" ]");
      scope.printTrace(out, indent+2);
    }
  }
}