package edu.cmu.cs.fluid.java.analysis;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.AbstractBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/** A trivial implementation of {@link edu.cmu.cs.fluid.java.bind.IBinder} 
 * that does less
 * than the minimum job.  To find a binding, it tries to find
 * any declaration node in the same program that has the same
 * name.  All other binding information is left blank (null).
 */
public class FakeBinder extends AbstractBinder {
  private static final Logger LOG =
	  SLLogger.getLogger("FLUID.java.bind");

  //private final IRNode f_defaultRoot;
  private final Set<Operator> declOpSet = new HashSet<Operator>();
  private final Map<IRNode,IBinding> bindings = new HashMap<IRNode,IBinding>();

  public FakeBinder(IRNode aNode) {
	super(false);
    // f_defaultRoot = JJNode.tree.getRoot(aNode);
    declOpSet.add(ClassDeclaration.prototype);
    declOpSet.add(ConstructorDeclaration.prototype);
    declOpSet.add(InterfaceDeclaration.prototype);
    declOpSet.add(MethodDeclaration.prototype);
    declOpSet.add(ParameterDeclaration.prototype);
    declOpSet.add(VariableDeclarator.prototype);
  }

  public void setBinding(IRNode n1, IRNode n2) {
    setBinding(n1,IBinding.Util.makeBinding(n2));
  }

  public void setBinding(IRNode n1, IBinding n2) {
    bindings.put(n1,n2);
  }
  
  protected boolean isDeclaration(Operator op) {
    return declOpSet.contains(op);
  }

  protected IRNode findDeclaration(IRNode root, String name) {
    Iterator<IRNode> nodes = JJNode.tree.topDown(root);
    IRNode found = null;
    while (nodes.hasNext()) {
      IRNode n = nodes.next();
      if (isDeclaration(JJNode.tree.getOperator(n)) &&
	  JJNode.getInfo(n).equals(name)) {
	if (found == null) {
	  found = n;
	} else {
	  LOG.warning("fake binder ambiguity for " + name);
	  break;
	}
      }
    }
    return found;
  }

  @Override
  protected IBinding getIBinding_impl(IRNode n) {
    IBinding b = bindings.get(n);
    if (b != null) return b;
    try {
      setBinding(n,findDeclaration(JJNode.tree.getRoot(n),JJNode.getInfo(n)));
      return bindings.get(n);
    } catch (SlotUndefinedException ex) {
      System.err.println("Could not get info for " +
			 DebugUnparser.toString(n));
      return null;
    }
  }

  /*
  @Override
  public IRNode findMethod(IRNode type, String method, IRNode[] sig) {
    return findDeclaration(f_defaultRoot,method);
  }*/
}
