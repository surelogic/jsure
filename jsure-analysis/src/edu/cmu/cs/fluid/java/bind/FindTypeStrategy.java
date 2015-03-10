package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * A Strategy for matching the given ITypeBinding 
 */
public class FindTypeStrategy extends AbstractSuperTypeSearchStrategy<IRNode> {
  private final IRNode match;
  private boolean found = false;

  public FindTypeStrategy(IRNode match) {
  	super(null, "type", JJNode.getInfo(match));
    this.match = match;
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public void visitClass_internal(IRNode type) {
		if (type.equals(match)) {
  		found = true;
			searchAfterLastType = false;
		} else {
			searchAfterLastType = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitSuperclass()
	 */
	@Override
  public boolean visitSuperclass() {
		return !found && searchAfterLastType;
	}	

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ITypeSearchStrategy#getResult()
	 */
	@Override
  public IRNode getResult() {
		return found ? match : null;
	}
}
