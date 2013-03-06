package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.BindUtil;

/**
 */
public class BindHelper implements IBindHelper {
	private IBinder binder;
	
	BindHelper(IBinder bind) {
		binder = bind;
	}

  /**
   * @see edu.cmu.cs.fluid.java.bind.IBindHelper#getNamedTypeBinding(IRNode)
   */
  @Override
  public IRNode getNamedTypeBinding(IRNode type) {
    return binder.getBinding(type);
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.IBindHelper#getHelper(IRNode)
   */
  @Override
  public IBindHelper getHelper(IRNode type) {
    return this;
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.IBindHelper#findFieldInBody(IRNode, String)
   */
  @Override
  public IRNode findFieldInBody(IRNode body, String name) {
    return BindUtil.findFieldInBody(body, name);
  }
}
