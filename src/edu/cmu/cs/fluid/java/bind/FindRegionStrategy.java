package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

/**
 */
public class FindRegionStrategy extends AbstractSuperTypeSearchStrategy<IRNode> {
  public FindRegionStrategy(IBinder bind, String name) {
    super(bind, "region", name);
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public void visitClass_internal(IRNode type) {
		if (type == null) {
			LOG.severe("Type was null, while looking for a region "+name);
			searchAfterLastType = false;
		} else {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Looking for region "+name+" in "+DebugUnparser.toString(type));
      }
		  IRNode reg = binder.findRegionInType(type, name);
		  /*
		   * if (result == null) { LOG.debug("Couldn't find "+name+" as region,
		   * looking for field"); result = FindFieldStrategy.findFieldInType(type,
		   * name); }
		   */
			searchAfterLastType = (reg == null);
			
			if (result == null) {
				// Nothing found yet
				result = reg;
			} else if (!result.equals(reg)) {
				// Found more than one distinct region
				LOG.warning("Found a duplicate region: "+DebugUnparser.toString(reg));
			}
		}
	}
}
