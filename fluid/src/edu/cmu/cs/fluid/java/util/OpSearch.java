// $Header: /var/cvs/fluid/code/fluid/java/operator/OpSearch.java,v 1.1 1999/04/02 16:41:42 chance Exp $
package edu.cmu.cs.fluid.java.util;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;

// Used to search the parents of a node for a particular kind of parent
public class OpSearch implements JavaGlobals {
  private static IRNode find(OpSearch os, IRNode here, IRNode last) {
    // based on bsi.findRoot
    while (os.continueLoop(here, last)) {
      IRNode rv = os.foundNode(here, last);
      if (rv != null) {
	return rv;
      }
      last = here;
      here = jtree.getParentOrNull(here);
    }
    return null;    
  }
  /*
  private String test() {
    return DebugUnparser.toString(null);
  }
  */
  public final IRNode find(IRNode here) {
    return find(this, here, null);
  }
  public final IRNode findEnclosing(IRNode here) {
    return find(this, JavaPromise.getParentOrPromisedFor(here), here);
  }

  protected boolean continueLoop(IRNode n, IRNode last) {
    return (n != null);
  }

  protected IRNode foundNode(IRNode n, IRNode last) {
    return (foundNode(n)) ? n : null;
  }

  protected boolean foundNode(IRNode n) {
    Operator op = jtree.getOperator(n);
    return found(op);
  }

  protected boolean found(Operator op) {
    return false;
  }

  /*
  static IRNode findEnclosingOp(Operator op0, IRNode here) {
    // based on bsi.findRoot
    IRNode parent = JavaPromise.getParentOrPromisedFor(here);
    while (parent != null) {
      Operator op = jtree.getOperator(parent); 
      if (op == op0) {
	return parent;
      }
      here = parent;
      parent = JavaPromise.getParentOrPromisedFor(here);
    }
    return null;    
  }
  */

  /// Specific searches
  public static final OpSearch rootSearch = new OpSearch() {
    @Override
    protected IRNode foundNode(IRNode n, IRNode last) { 
      return (n == null) ? last : null;
    }
    @Override
    protected boolean continueLoop(IRNode n, IRNode last) {
      return (n != null) || (last != null);
    }
  };

	public static final OpSearch cuSearch = new OpSearch() {
		@Override
    protected boolean found(Operator op) { return op instanceof CompilationUnit; }
	};
  public static final OpSearch stmtSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) { return op instanceof StatementInterface; }
  };
  public static final OpSearch exprSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) { return op instanceof Expression; }
  };
  public static final OpSearch typeSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) { 
      return op instanceof TypeDeclInterface; 
    }
  };
  public static final OpSearch memberSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) { 
      return ClassBodyDeclaration.prototype.includes(op);
    }
  };

  public static final OpSearch breakSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) { 
      return 
      (op instanceof ForStatement) ||
      (op instanceof DoStatement) ||
      (op instanceof WhileStatement) ||
      (op instanceof SwitchStatement); 
    }
  };
  public static final OpSearch continueSearch = new OpSearch() {
    @Override
    protected boolean found(Operator op) { 
      return 
      (op instanceof ForStatement) ||
      (op instanceof DoStatement) ||
      (op instanceof WhileStatement);
    }
  };  

  /**
   * Searches for method/constructor declarations.
   */  
  public static final OpSearch methodSearch = new OpSearch() {
    @Override
    protected boolean found(final Operator op) { 
      return    (op instanceof MethodDeclaration)
             || (op instanceof ConstructorDeclaration);
    }
  };

  /**
   * Searches for Synchronized blocks.
   */
  public static final OpSearch syncSearch = new OpSearch() {
    @Override
    protected boolean found( final Operator op )
    { 
      return (op instanceof SynchronizedStatement);
    }
  };
  
  /**
   * Searches for declarations.
   */
  public static final OpSearch declSearch = new OpSearch() {
    @Override
    protected boolean found( final Operator op )
    { 
      return op instanceof TypeDeclInterface || Declaration.prototype.includes(op); 
    }
  };
  
  public static final OpSearch annoSearch = new OpSearch() {
	  @Override
	  protected boolean found( final Operator op )
	  { 
		  return op instanceof Annotation; 
	  }
  };  
}
