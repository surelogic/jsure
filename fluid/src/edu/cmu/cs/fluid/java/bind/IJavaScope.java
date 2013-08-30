/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaScope.java,v 1.28 2008/08/26 20:45:53 chance Exp $
 */
package edu.cmu.cs.fluid.java.bind;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.NotThreadSafe;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

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


/**
 * A representation of the scope when doing binding.
 * @author boyland
 */
public interface IJavaScope {
  public static final Logger LOG = SLLogger.getLogger("FLUID.java");

  public static final EmptyIterator<IBinding> EMPTY_BINDINGS_ITERATOR = new EmptyIterator<IBinding>();
  
  public boolean canContainPackages();
  
  /**
   * Look in the scope to find a declaration that matches the given selector.
   * @param name name to look up in the scope
   * @param useSite use site to mark for incrementality.  We mark the use site as changed
   *        whenever the lookup results might be different.
   * @param selector control over what nodes to return
    * @return an IR node or null
   */
  public IBinding lookup(LookupContext context, IJavaScope.Selector selector);
  
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
  public Iteratable<IBinding> lookupAll(LookupContext context, IJavaScope.Selector selector);
  
  public void printTrace(PrintStream out, int indent);
  
  public static final IJavaScope nullScope = new IJavaScope() {
    @Override
    public IBinding lookup(LookupContext context, Selector selector) {
      return null;
    }
    @Override
    public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
      return EMPTY_BINDINGS_ITERATOR;
    }
    @Override
    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[null]");
    }
	@Override
  public boolean canContainPackages() {
		return false;
	}
  };
  
  @NotThreadSafe
  public static class LookupContext {
	  public IRNode useSite;
	  public String name;
	  //Selector selector;
	  private IRNode enclosingType;
	  
	  public IRNode foundNewType(IRNode t) {
		IRNode last = enclosingType;
		enclosingType = t;
		return last;
	  }
	  public void leavingType(IRNode t, IRNode last) {
		if (t != enclosingType) {
			throw new IllegalStateException();
		}
		enclosingType = last;
	  }
	  public LookupContext use(String name, IRNode node) {
		  this.name = name;
		  useSite = node;
		  return this;
	  }
	  public IRNode getEnclosingType() {
		  if (enclosingType == null) {
			  enclosingType = VisitUtil.getEnclosingType(useSite);
		  }
		  return enclosingType;
	  }
  }
  
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
    
    @Override
    public String label() {
      return label;
    }
  }
  
  public static class Util {
    public static Selector combineSelectors(final Selector s1, final Selector s2) {
      return new Selector() {
        @Override
        public boolean select(IRNode n) {
          return s1.select(n) && s2.select(n);
        }
        @Override
        public String label() {
          return s1.label()+" & "+s2.label();
        }
      };
    }
    
    public static Selector eitherSelector(final Selector s1, final Selector s2) {
        return new Selector() {
          @Override
          public boolean select(IRNode n) {
            return s1.select(n) || s2.select(n);
          }
          @Override
          public String label() {
            return s1.label()+" | "+s2.label();
          }
        };
      }
    
    public static final Selector isTypeDecl = new AbstractSelector("Only type decls") {
      @Override
      public boolean select(IRNode node) {
        return isTypeDecl(node);
      }
    };
    
    public static final Selector isPkgTypeDecl = new AbstractSelector("Only type/pkg decls") {
        @Override
        public boolean select(IRNode node) {
          final Operator op = JJNode.tree.getOperator(node);
          return isTypeDecl(op) || op instanceof PackageDeclaration;
        }
    };
    
    public static IBinding lookupType(IJavaScope scope, LookupContext context) {
      if (scope == null) {
      	  return null;
      }
      return scope.lookup(context,isTypeDecl);
    }
    
    public static final Selector isPackageDecl = new AbstractSelector("Only package decls") {
      @Override
      public boolean select(IRNode node) {
        return JJNode.tree.getOperator(node) instanceof NamedPackageDeclaration;
      }
    };
    public static IBinding lookupPackage(IJavaScope scope, LookupContext context) {
      if (scope == null) {
    	  return null;
      }
      if (!scope.canContainPackages()) {
    	  return null;
      }
      return scope.lookup(context,isPackageDecl);
    }
    
    public static final Selector isConstructorDecl = new AbstractSelector("Only constructors") {
      @Override
      public boolean select(IRNode node) {
        return JJNode.tree.getOperator(node) instanceof ConstructorDeclaration;
      }
    };
    
    public static final Selector isMethodDecl = new AbstractSelector("Only methods") {
      @Override
      public boolean select(IRNode node) {
        return JJNode.tree.getOperator(node) instanceof MethodDeclaration;
      }      
    };
    
    public static Iterator<IBinding> lookupCallable(IJavaScope scope, LookupContext context, 
    		                                        Selector isAccessible, boolean needMethod) {
      return scope.lookupAll(context, combineSelectors(needMethod ? isMethodDecl : isConstructorDecl, isAccessible));
    }
    
    public static final Selector isValueDecl = new AbstractSelector("Only value decls") {
      @Override
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof VariableDeclarator || op instanceof ParameterDeclaration
         || op instanceof EnumConstantDeclaration;
      }      
    };
    public static IBinding lookupValue(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,isValueDecl);
    }
    
    public static final Selector isAnnotationElt = new AbstractSelector("Only annotation elements") {
      @Override
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof AnnotationElement;
      }      
    };
    
    public static IBinding lookupAnnotationElt(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,isAnnotationElt);
    }
    
    public static final Selector isAnnoEltOrNoArgMethod = eitherSelector(isAnnotationElt, new AbstractSelector("") {
      @Override
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
      @Override
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof ReceiverDeclaration;
      }
    };
    public static IBinding lookupReceiver(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,isReceiverDecl);
    }
    
    public static final Selector isLabeledStatement = new AbstractSelector("Only labeled stmts") {
      @Override
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof LabeledStatement;
      }
    };
    public static IBinding lookupLabel(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,isLabeledStatement);
    }
        
    public static final Selector isReturnValue = new AbstractSelector("Only return values") {
      @Override
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return op instanceof ReturnValueDeclaration;
      }
    };
    public static IBinding lookupReturnValue(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,isReturnValue);
    }
    
    public static final Selector couldBeNonTypeName = new AbstractSelector("Could bind to a name (not a type)") {
        @Override
        public boolean select(IRNode node) {
          Operator op = JJNode.tree.getOperator(node);
          return !(op instanceof SomeFunctionDeclaration) && !(op instanceof AnnotationElement) &&
                 !(op instanceof LabeledStatement) && 
                 !(TypeDeclaration.prototype.includes(op) && !(op instanceof EnumConstantClassDeclaration));
        }      
      };
    
    public static final Selector couldBeName = new AbstractSelector("Could bind to a name") {
      @Override
      public boolean select(IRNode node) {
        Operator op = JJNode.tree.getOperator(node);
        return !(op instanceof SomeFunctionDeclaration) && !(op instanceof AnnotationElement) &&
               !(op instanceof LabeledStatement);
      }      
    };
    public static IBinding lookupName(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,couldBeName);
    }
    
    public static final Selector isntType = new AbstractSelector("Not type decl") {
      @Override
      public boolean select(IRNode node) {
        return !isTypeDecl(node);
      }      
    };
    public static IBinding lookupNonType(IJavaScope scope, LookupContext context) {
      return scope.lookup(context,isntType);
    }
    
    public static final Selector isStatic = new AbstractSelector("Only static") {
      @Override
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
    
	@Override
  public boolean canContainPackages() {
		return scope.canContainPackages();
	}   
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookup(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    @Override
    public IBinding lookup(LookupContext context, Selector sel) {
      return scope.lookup(context,Util.combineSelectors(selector,sel));
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookupAll(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    @Override
    public Iteratable<IBinding> lookupAll(LookupContext context, Selector sel) {
      return scope.lookupAll(context,Util.combineSelectors(selector,sel));
    }

    @Override
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

	@Override
  public boolean canContainPackages() {
		return outer.canContainPackages();
	} 
    
    @Override
    public IBinding lookup(LookupContext context, Selector selector) {
      boolean debug = LOG.isLoggable(Level.FINER);
      if (debug) {
        LOG.finer("Looking for " + context.name + " in " + this);
      }
      if (locals != null) {
        IBinding found = locals.get(context.name);
        if (found != null && found.getNode() != null && selector.select(found.getNode())) {
          if (debug) LOG.finer("Found candidate " + found);
          return found;
        }
      }
      return outer.lookup(context,selector);
    }
    
    @Override
    public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
      return outer.lookupAll(context,selector);
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

    @Override
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
    
	@Override
  public boolean canContainPackages() {
		return outer.canContainPackages();
	} 
    
    @Override
    public IBinding lookup(LookupContext context, Selector selector) {
      List<IBinding> l = locals.get(context.name);
      if (l == null) return outer.lookup(context,selector);
      for (IBinding bind : l) {
        if (selector.select(bind.getNode())) {
          return bind;
        }
      }
      return outer.lookup(context,selector);
    }
    
    @Override
    @SuppressWarnings("unchecked")
	public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
      List<IBinding> l = locals.get(context.name);
      if (l == null) return outer.lookupAll(context,selector);
      Vector<IBinding> selected = null;
      for (IBinding binding : l) {
        if (selector.select(binding.getNode())) {
          if (selected == null) selected = new Vector<IBinding>();
          selected.addElement(binding);
        }
      }
      Iteratable<IBinding> outerResult = outer.lookupAll(context,selector);
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
     * @param f_node declaration node with INFO set
     */
    public void add(IBinding binding) {
      if (binding.getNode() != null) put(JJNode.getInfo(binding.getNode()),binding);
    }

    @Override
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

	@Override
  public boolean canContainPackages() {
		return scope1.canContainPackages() || scope2.canContainPackages();
	} 
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookup(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    @Override
    public IBinding lookup(LookupContext context, Selector selector) {
      IBinding result = scope1.lookup(context,selector);
      if (result == null) {
    	  try {
    		  result = scope2.lookup(context,selector);
    	  } catch (StackOverflowError e) {
    		  System.out.println("Overflow on "+DebugUnparser.toString(context.useSite));
    		  throw e;
    	  }
      }
      return result;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookupAll(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    @Override
    @SuppressWarnings("unchecked")
	public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
      Iteratable<IBinding> result1 = scope1.lookupAll(context,selector);
      Iteratable<IBinding> result2 = scope2.lookupAll(context,selector);
      if (result1.hasNext()) {
        if (result2.hasNext()) {
          return (Iteratable<IBinding>) AppendIterator.append(result1, result2);
        } else {
          return result1;
        }
      }
      return result2;
    }

    @Override
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
    
	public boolean canContainPackages() {
		return scope1.canContainPackages() || scope2.canContainPackages();
	} 
    
    public IBinding lookup(LookupContext context, Selector selector) {
      IBinding result = scope1.lookup(context,selector);
      if (result == null)
        return scope2.lookup(context,selector);
      return result;
    }
    public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
      Iteratable<IBinding> result = scope1.lookupAll(context,selector);
      if (!result.hasNext())
        return scope2.lookupAll(context,selector);
      return result;
    }
    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[ShadowingScope"+hashCode()+"-1]");
      scope1.printTrace(out, indent+2);
      DebugUtil.println(out, indent, "[ShadowingScope"+hashCode()+"-2]");
      scope2.printTrace(out, indent+2);
    }  
  }
  
  public static class SelectiveShadowingScope extends ShadowingScope {
	final Selector selectorForS1;
	  
	public SelectiveShadowingScope(IJavaScope sc1, Selector s1, IJavaScope sc2) {
		super(sc1, sc2);
		selectorForS1 = s1;
	}
	@Override
	public IBinding lookup(LookupContext context, Selector selector) {
		final Selector combined = Util.combineSelectors(selectorForS1, selector);
		IBinding result = scope1.lookup(context,combined);
		if (result == null)
			return scope2.lookup(context,selector);
		return result;
	}
	@Override
	public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
		final Selector combined = Util.combineSelectors(selectorForS1, selector);
		Iteratable<IBinding> result = scope1.lookupAll(context,combined);
		/*
		if (result == null) {
			scope1.lookupAll(context,combined);
		}
		*/
		if (!result.hasNext())
			return scope2.lookupAll(context,selector);
		return result;
	}
	@Override
	public void printTrace(PrintStream out, int indent) {
		DebugUtil.println(out, indent, "[SelectiveShadowingScope"+hashCode()+"-1:"+selectorForS1.label()+"]");
		scope1.printTrace(out, indent+2);
		DebugUtil.println(out, indent, "[SelectiveShadowingScope"+hashCode()+"-2]");
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
    
	public boolean canContainPackages() {
		return scope.canContainPackages();
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
          IBinding newBinding = IBinding.Util.makeBinding(node, context, tEnv);
          return newBinding;
        }
        //LOG.warning("substBinding didn't expect to get here...");
        IJavaDeclaredType c = computeContextType(bdecl);
        if (c == null) {
        	computeContextType(bdecl);
        }
        return IBinding.Util.makeBinding(node,c, tEnv);
      }
      if (context == bContext) {
    	  // No need to do substitution, since we're talking about a binding from the same type
    	  return binding; 
      }
      if (subst == null) {
    	return binding;
      }      
      return IBinding.Util.makeBinding(binding.getNode(),bContext.subst(subst), tEnv);
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
    
    public IBinding lookup(LookupContext context, Selector selector) {
      IBinding result = scope.lookup(context,selector);
      if (result == null) return null;
      /*
      if (name.equals("foundUnlock") &&
    	  context != null && context.getName().contains("MustHoldTransfer")) {
    	  return substBinding(result);
      }
      */
      return substBinding(result);
    }

    public Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
      Iteratable<IBinding> result = scope.lookupAll(context,selector);
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