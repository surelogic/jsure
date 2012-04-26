// $Header: /var/cvs/fluid/code/fluid/java/bind/VisitUtil.java,v 1.4 2002/09/09 20:44:13 chance Exp $
package edu.cmu.cs.fluid.java.util;

import java.util.*;
import java.util.Stack;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.util.*;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

// This is for code that is used to "visit" parts of a tree, given a root
public class VisitUtil implements JavaGlobals {
  /// findRoot : traverse up until no longer possible
  //
  // This doesn't try to use the promisedFor node 
  public static IRNode findRoot(IRNode here) {
	return OpSearch.rootSearch.find(here);
  }
  
  /**
   * Check for a promisedFor node before declaring the node as a root
   */
  public static IRNode findCompilationUnit(IRNode here) {
	  if (here == null) {
		  return null;
	  }
	  IRNode cu = JJNode.tree.getRoot(here); 
	  //IRNode cu = VisitUtil.findRoot(here);
	  IRNode promisedFor = cu;
	  while (cu == null || !CompilationUnit.prototype.includes(cu)) {
		  // Check for promises
		  promisedFor = JavaPromise.getPromisedFor(cu);
		  if (promisedFor == null) {
			  break;
		  }
		  cu = JJNode.tree.getRoot(promisedFor); 
		  //cu = VisitUtil.findRoot(promisedFor);
	  }
	  if (promisedFor == null) {
		  System.out.println("No cu for "+DebugUnparser.toString(here));
	  }
	  return cu;
  }

  /// rootWalk
  public static Iteratable<IRNode> rootWalk(final IRNode starting) {
    return new AbstractRemovelessIterator<IRNode>(){
      IRNode next = starting;
      public boolean hasNext() {
        return next != null;
      }
      public IRNode next() {
        if (next == null) throw new NoSuchElementException("root walk done");
        try {
          return next;
        } finally {
          next = JavaPromise.getParentOrPromisedFor(next);
        }
      }
    };
  }
  
  /// isAncestor
  public static boolean isAncestor(final IRNode ancestor, final IRNode start) {
    if (ancestor == null) {
      return false;
    }
    IRNode next = start;
    while (next != null) {
      if (next.equals(ancestor)) {
        return true;
      }
  	  next = JavaPromise.getParentOrPromisedFor(next);
    }
    return false;
  }

  /// getEnclosingCompilationUnit - for any node
  public static IRNode getEnclosingCompilationUnit(IRNode here) {
    return OpSearch.cuSearch.findEnclosing(here);
  }
  
  /// just a fencepost different from getEnclosingCompilationUnit. 
  /// When here is already a CU node, we just return here, rather than searching above it.
  public static IRNode getEnclosingCUorHere(IRNode here) {
    if (here == null) {
      return null;
    } else {
      final Operator hereOp = jtree.getOperator(here);
      if (CompilationUnit.prototype.includes(hereOp)) {
	return here;
      }
    }
    return getEnclosingCompilationUnit(here);
  }
  
  public static IRNode getClosestType(IRNode here) {
    Operator op = jtree.getOperator(here);
    if (op instanceof TypeDeclInterface) {
      return here;
    }
    return getEnclosingType(here);
  }
  
  public static IRNode getClosestClassBodyDecl(IRNode here) {
    Operator op = jtree.getOperator(here);
    if (ClassBodyDeclaration.prototype.includes(op)) {
      return here;
    }
    return getEnclosingClassBodyDecl(here);
  }
  
  public static IRNode getClosestDecl(IRNode here) {
    Operator op = jtree.getOperator(here);
    if (OpSearch.declSearch.found(op)) {
      return here;
    }
    return getEnclosingDecl(here);
  }
  
  public static IRNode getEnclosingDecl(IRNode here) {
    return OpSearch.declSearch.findEnclosing(here);
  }
  
  /// getEnclosingType - for any node
  public static IRNode getEnclosingType(final IRNode here) {
    IRNode type  = OpSearch.typeSearch.findEnclosing(here);
    if (type == null) {
      // LOG.info("Couldn't find enclosing type for "+DebugUnparser.toString(here));
      return null;
    }
    return checkForAnonClass(type, here);
  }
  
  private static IRNode checkForAnonClass(IRNode type, final IRNode start) {
    Operator top = jtree.getOperator(type);
    if (AnonClassExpression.prototype.includes(top)) {
      // here could be in one of the arguments, which really 
      // belongs in the anon class' enclosing type, so check for that      
      if (isAncestor(AnonClassExpression.getArgs(type), start)) {
        // find the anon class' enclosing type instead
        type = OpSearch.typeSearch.findEnclosing(type);
      }
      // otherwise, it's in the body of the anon class 
    }
    return type;
  }
  
  /**
   * Also handles the case that 'here' is in a promise
   */
  public static IRNode getEnclosingTypeForPromise(final IRNode here) {
	  final IRNode start = JavaPromise.getParentOrPromisedFor(here);
	  for(final IRNode n : rootWalk(start)) {
		  Operator op = jtree.getOperator(n);
		  if (op instanceof TypeDeclInterface) {
			  return checkForAnonClass(n, here);
		  }
	  }
	  return null;
  }

  /// getEnclosingTypes
  public static  Iteratable<IRNode> getEnclosingTypes(final IRNode starting, final boolean closest) {
    return new AbstractRemovelessIterator<IRNode>(){
      IRNode next = closest ? getClosestType(starting) : getEnclosingType(starting);
      public boolean hasNext() {
        return next != null;
      }
      public IRNode next() {
        if (next == null) throw new NoSuchElementException("root walk done");
        try {
          return next;
        } finally {
          next = getEnclosingType(next);
        }
      }
    };
  }
  
  /**
   * Find the enclosing method/constructor declaration.
   * 
   * <p><em>This is purely a syntactic operation.  There are times when it does
   * not return the method you would like it to.</em>  For example, if you have a
   * method call that occurs in an instance initialization block or in a field 
   * initializer, this method does not return the "initialization" method; it 
   * returns whatever method encloses the call syntactically.  Whether this is
   * what you want or not depends on context.  If you need to have the 
   * "initialization" method information, use
   * {@link PromiseUtil#getEnclosingMethod(IRNode)}. 
   */
  public static IRNode getEnclosingMethod(final IRNode here) {
    return OpSearch.methodSearch.findEnclosing(here);
  }
  
  public static IRNode getEnclosingStatement(final IRNode here) {
    return OpSearch.stmtSearch.findEnclosing(here);
  }
  
  /// getEnclosingClassBodyDecl - for any node
  public static IRNode getEnclosingClassBodyDecl(IRNode here) {
    return OpSearch.memberSearch.findEnclosing(here);
  }

  /// getEnclosingVarDecls - for any node
  public static IRNode getEnclosingVarDecls(IRNode here) {
    return varDeclsSearch.findEnclosing(here);
  }

  public static final OpSearch varDeclsSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) {
      return 
	(op instanceof ParameterDeclaration) ||
	(op instanceof DeclStatement) ||
	(op instanceof FieldDeclaration);
    }
  };


  /// getClassBody - for types
  public static IRNode getClassBody(IRNode decl) {
    return getClassBody(JJNode.tree, decl);
  }
  
  public static IRNode getClassBody(SyntaxTreeInterface tree, IRNode decl) {
    Operator op = tree.getOperator(decl);
    if (ClassDeclaration.prototype.includes(op)) {
      return ClassDeclaration.getBody(tree, decl);
    }
    else if (InterfaceDeclaration.prototype.includes(op)) {
      return InterfaceDeclaration.getBody(tree, decl);
    }
    else if (EnumDeclaration.prototype.includes(op)) {
      return EnumDeclaration.getBody(tree, decl);
    }
    else if (AnonClassExpression.prototype.includes(op)) {      
      return AnonClassExpression.getBody(tree, decl);
    }
    else if (AnnotationDeclaration.prototype.includes(op)) {
      return AnnotationDeclaration.getBody(tree, decl);
    }
    else if (EnumConstantClassDeclaration.prototype.includes(op)) {
      return EnumConstantClassDeclaration.getBody(tree, decl);
    }    
    else if (ArrayDeclaration.prototype.includes(op)) {
    	return null;
    }
    throw new FluidError("Getting a class body on a "+op.name()+": "+DebugUnparser.toString(decl));
  }
  
  public static Iteratable<IRNode> getClassBodyMembers(IRNode decl) {
  	IRNode body = getClassBody(decl);
  	if (body == null) {
  		// array decl or something else
  		return new EmptyIterator<IRNode>();
  	}
  	return JJNode.tree.children(body);
  }

  /// getTypeDecls - for CUs
  /**
   * Get the top-level types declared in the compilation unit
   */
  public static Iteratable<IRNode> getTypeDecls(IRNode jf) {
    IRNode decls = CompilationUnit.getDecls(jf);
    return jtree.children(decls);
  }
  
  /**
   * Get all types declared in the AST
   */
  public static Iteratable<IRNode> getAllTypeDecls(final IRNode jf) {
    return new SimpleRemovelessIterator<IRNode>() {
      Iterator<IRNode> enm = JJNode.tree.topDown(jf);
       @Override protected Object computeNext() {
         while (enm.hasNext()) {
           IRNode n = enm.next();
           Operator op = JJNode.tree.getOperator(n);
           if (!(op instanceof TypeDeclInterface)) {
             continue; // Not a type
           }
           return n;
         }
         return noElement;
       }
     };
  }
  
	public static Iteratable<IRNode> getNestedTypes(final IRNode cl) {
		final IRNode cl1 = cl;
  	
		return new SimpleRemovelessIterator<IRNode>() {
      Iterator<IRNode> enm = getClassBodyMembers(cl1);
			 @Override protected Object computeNext() {
				 while (enm.hasNext()) {
					 IRNode n = enm.next();
					 Operator op = JJNode.tree.getOperator(n);

					 if (TypeDeclaration.prototype.includes(op)) {
						 return n;
					 }
				 }
				 return noElement;
			 }
		 };
 
	} 
  
	public static Iteratable<IRNode> getClassConstructors(final IRNode cl) {
		final IRNode cl1 = cl;
  	
		return new SimpleRemovelessIterator<IRNode>() {
      Iterator<IRNode> enm = getClassBodyMembers(cl1);
			 @Override protected Object computeNext() {
				 while (enm.hasNext()) {
					 IRNode n = enm.next();
					 Operator op = JJNode.tree.getOperator(n);

					 if (op == ConstructorDeclaration.prototype) {
						 return n;
					 }
				 }
				 return noElement;
			 }
		 };
 
	}  
  
  public static Iteratable<IRNode> getClassMethods(final IRNode cl) {
  	final IRNode cl1 = cl;
  	
		return new SimpleRemovelessIterator<IRNode>() {
      Iterator<IRNode> enm = getClassBodyMembers(cl1);
			 @Override protected Object computeNext() {
				 while (enm.hasNext()) {
					 IRNode n = enm.next();
					 Operator op = JJNode.tree.getOperator(n);

					 if ((op == MethodDeclaration.prototype)
						 || (op == ConstructorDeclaration.prototype
             || (op == AnnotationElement.prototype))) {
						 return n;
					 }
				 }
				 return noElement;
			 }
		 };
 
  }

  /**
   * 
   * @return All the FieldDeclarations in a class
   */
	public static Iteratable<IRNode> getClassFieldDecls(final IRNode cl){
		final IRNode cl1 = cl;
  	
		return new SimpleRemovelessIterator<IRNode>() {
			 Iterator<IRNode> enm = getClassBodyMembers(cl1);
			 @Override protected Object computeNext() {
				 while (enm.hasNext()) {
					 IRNode n = enm.next();
					 Operator op = JJNode.tree.getOperator(n);

					 if ((op == FieldDeclaration.prototype)) {
						 return n;
					 }
				 }
				 return noElement;
			 }
		 };
	}
  
  public static Iterator<IRNode> getFieldDeclarators(final IRNode fd) {
    IRNode vds = FieldDeclaration.getVars(fd);
    return VariableDeclarators.getVarIterator(vds);
  }
  
  /**
   * @return All the VariableDeclarators corresponding to fields
   */
  public static Iteratable<IRNode> getClassFieldDeclarators(final IRNode cl){
    Iterator<IRNode> fds = getClassFieldDecls(cl);
    return new ProcessIterator<IRNode>(fds) {
       @Override
      protected Iterator<IRNode> getNextIter(Object o) {          
         return getFieldDeclarators((IRNode) o);
       }
     };
  }
  
  public static Iteratable<IRNode> getInnerClasses(IRNode root) {
    final IRNode body = VisitUtil.getClassBody(root);

    return new SimpleRemovelessIterator<IRNode>() {
      Stack<Iterator<IRNode>> stack = new Stack<Iterator<IRNode>>();
      Iterator<IRNode> enm = ClassBody.getDeclIterator(body);

      boolean moreElements() {
        if (enm.hasNext()) {
          return true;
        }
        while (!stack.isEmpty()) {
          enm = stack.pop();
          if (enm.hasNext()) {
            return true;
          }
        }
        return false;
      }
      void pushNewIterator(Iterator<IRNode> e) {
        if (e.hasNext()) {
          stack.push(enm);
          enm = e;
        }
      }      
      @Override protected Object computeNext() {
        while (moreElements()) {
          IRNode n = enm.next();
          Operator op = JJNode.tree.getOperator(n);

          if ((ClassDeclaration.prototype.includes(op))
            || (InterfaceDeclaration.prototype.includes(op)) 
            || (AnonClassExpression.prototype.includes(op)) 
            || EnumDeclaration.prototype.includes(op) 
            || AnnotationDeclaration.prototype.includes(op)) {
            return n;
          }
          else if (MethodDeclaration.prototype.includes(op)) {
            pushNewIterator(getMethodLocalClasses(MethodDeclaration.getBody(n)));
          }
          else if (ConstructorDeclaration.prototype.includes(op)) {
            pushNewIterator(getMethodLocalClasses(ConstructorDeclaration.getBody(n)));            
          }
        }
        return noElement;
      }
    };
  }

  public static Iteratable<IRNode> getMethodLocalClasses(IRNode mbody) {
    if (!MethodBody.prototype.includes(JJNode.tree.getOperator(mbody))) {
      return new EmptyIterator<IRNode>();
    }
    final Iterator<IRNode> enm = JJNode.tree.bottomUp(MethodBody.getBlock(mbody));
    return new SimpleRemovelessIterator<IRNode>() {
      @Override protected Object computeNext() {
        while (enm.hasNext()) {
          IRNode n = enm.next();
          Operator op = JJNode.tree.getOperator(n);
          if (TypeDeclarationStatement.prototype.includes(op)) {
            return TypeDeclarationStatement.getTypedec(n);
          }
        }
        return noElement;
      }    
    };
  }


  /// other stuff
  public static String getPackageName(IRNode cu) {
  	IRNode pkg  = CompilationUnit.getPkg(cu);
  	Operator op = JJNode.tree.getOperator(pkg);
  	if (op instanceof UnnamedPackageDeclaration) {
  		return "";
  	}
  	return NamedPackageDeclaration.getId(pkg);
  }
  
  /// getMethodBody - for types
  public static IRNode getMethodBody(IRNode decl) {
    Operator op = JJNode.tree.getOperator(decl);
    if (MethodDeclaration.prototype.includes(op)) {
      return MethodDeclaration.getBody(decl);
    }
    else if (ConstructorDeclaration.prototype.includes(op)) {
      return ConstructorDeclaration.getBody(decl);
    }
    throw new FluidError("Getting a method body on a "+op.name()+": "+DebugUnparser.toString(decl));
  }
  
  public static IRNode getMethodParameters(IRNode decl) {
    Operator op = JJNode.tree.getOperator(decl);
    if (MethodDeclaration.prototype.includes(op)) {
      return MethodDeclaration.getParams(decl);
    }
    else if (ConstructorDeclaration.prototype.includes(op)) {
      return ConstructorDeclaration.getParams(decl);
    }
    throw new FluidError("Getting method params on a "+op.name()+": "+DebugUnparser.toString(decl));
  }
  
  /**
   * @return The named types that represents the names of the super types
   */
  public static Iteratable<IRNode> getSupertypeNames(IRNode decl) {
    IRNode superName;
    final Iteratable<IRNode> superIfaces;
    
    Operator op = jtree.getOperator(decl);
    if (ClassDeclaration.prototype.includes(op)) {
      superName   = ClassDeclaration.getExtension(decl);     
      superIfaces = Implements.getIntfIterator(ClassDeclaration.getImpls(decl));
    }
    else if (InterfaceDeclaration.prototype.includes(op)) {
      superIfaces = Extensions.getSuperInterfaceIterator(InterfaceDeclaration.getExtensions(decl));
      return superIfaces;
    }
    else if (EnumDeclaration.prototype.includes(op)) {
      superIfaces = Implements.getIntfIterator(EnumDeclaration.getImpls(decl));
      return superIfaces;
    }
    else if (AnonClassExpression.prototype.includes(op)) {
      superName   = AnonClassExpression.getType(decl);
      superIfaces = new EmptyIterator<IRNode>();
    }
    else if (AnnotationDeclaration.prototype.includes(op)) {
      // FIX should return the named type for java.lang.annotation.Annotation
      return new EmptyIterator<IRNode>();
    }
    else if (TypeFormal.prototype.includes(op)) {
      IRNode bounds = TypeFormal.getBounds(decl);
      return MoreBounds.getBoundIterator(bounds);
    }
    else if (EnumConstantClassDeclaration.prototype.includes(op)) {
      // FIX should return the named type for its enum decl
      return new EmptyIterator<IRNode>();
    }
    else {
      throw new IllegalArgumentException("Unexpected type decl: "+op.name());
    }
    return new SimpleRemovelessIterator<IRNode>(superName) {
      @Override protected Object computeNext() {        
        return superIfaces.hasNext() ? superIfaces.next() : IteratorUtil.noElement;
      }      
    };
  }

  public static IRNode computeOutermostEnclosingTypeOrCU(IRNode locInIR) {
    // figure out what the "cu" is as needed by ColorDeclareDrop, TRoleRenameDrop
    // and ColorRenamePerCu should be.  The rule is that we preferentially want the
    // enclosing top-level type. Failing that, we want the enclosing CU.
    if (locInIR == null) return null;
    
    final Operator op = JJNode.tree.getOperator(locInIR);
    IRNode cu;
    if (ClassDeclaration.prototype.includes(op)
        || InterfaceDeclaration.prototype.includes(op)) {
      cu = locInIR;
    } else if (NamedPackageDeclaration.prototype.includes(op)) {
      cu = locInIR;
    } else if (NestedClassDeclaration.prototype.includes(op) ||
        NestedInterfaceDeclaration.prototype.includes(op)) {
      return computeOutermostEnclosingTypeOrCU(getEnclosingType(locInIR));
    } else if (CompilationUnit.prototype.includes(op)){
      cu = locInIR;
    } else {
      cu = getEnclosingType(locInIR);
      if (cu == null) {
        cu = getEnclosingCompilationUnit(locInIR);
      }
      cu = computeOutermostEnclosingTypeOrCU(cu);
    }
    return cu;
  }

  /**
   * 
   * @param cu
   * @return
   */
  public static IRNode getPrimaryType(IRNode cu) {
    Iterator<IRNode> it = getTypeDecls(cu);
    @SuppressWarnings("unused")
    boolean pub = false;
    IRNode rv   = null;
    while (it.hasNext()) {
      IRNode t = it.next();
      if (JavaNode.getModifier(t, JavaNode.PUBLIC)) {
        rv  = t;
        pub = true;
      }
      else if (rv == null) {
        // first one
        rv = t;
        continue;
      }      
    }
    return rv;
  }
}
