package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * A Strategy for matching the field in a type
 */
public class FindFieldStrategy extends AbstractSuperTypeSearchStrategy<IRNode> {	
  public FindFieldStrategy(IBinder bind, String fname) {
    super(bind, "field", fname);
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public void visitClass_internal(IRNode type) {
		if (type == null) {
			LOG.severe("Type was null, while looking for a field "+name);
			searchAfterLastType = false;
		} else {
      if (LOG.isLoggable(Level.FINE)) {
			  LOG.fine("Looking for field "+name+" in "+TypeUtil.printType(type));
      }
			IRNode body  = VisitUtil.getClassBody(type);
			IRNode field = null;
			
			if (body != null) {			
				field = BindUtil.findFieldInBody(body, name);
			} else {
				// array decl or something else
				field = null;
			}	
			searchAfterLastType = (field == null);
			
			if (result == null) {
				// Nothing found yet
				result = field;
			} else if (!result.equals(field)) {
				// Found more than one distinct field
				LOG.warning("Found a duplicate field: "+DebugUnparser.toString(field));
			}
		}	
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ITypeSearchStrategy#getResult()
	 */
	@Override
  public IRNode getResult() {
		return result;
	}
}
