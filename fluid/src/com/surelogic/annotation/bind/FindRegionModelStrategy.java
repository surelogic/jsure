package com.surelogic.annotation.bind;

import java.util.logging.Level;

import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.*;
import com.surelogic.dropsea.ir.drops.promises.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.*;

/**
 */
public class FindRegionModelStrategy extends AbstractSuperTypeSearchStrategy<IRegion> {
  public FindRegionModelStrategy(IBinder bind, String name) {
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
			final boolean finerIsLoggable = LOG.isLoggable(Level.FINEST);
			if (finerIsLoggable) {
				LOG.finer("Looking for region "+name+" in "+DebugUnparser.toString(type));
			}

			IRegion reg = null;
      String qname = null;// = AnnotationRules.computeQualifiedName(type, name);
      for(RegionModel r : RegionRules.getModels(type)) {
        if (finerIsLoggable) {
          LOG.finer("Looking at "+r.getRegionName());
        }
        // Check simple name to avoid qna
        if (!r.getName().equals(name)) {
        	continue;
        }
        if (qname == null) {
        	// Lazily compute qname
        	qname = AnnotationRules.computeQualifiedName(type, name);
        }
        if (r.getRegionName().equals(qname)) {
          reg = r;
          break;
        }
        if (r.getRegionName().indexOf('.') < 0) { // simple name
          final String qname2 = AnnotationRules.computeQualifiedName(type, r.getRegionName());
          if (finerIsLoggable) {
            LOG.finer("Looking at "+qname2);
          }
          if (qname2.equals(qname)) {
            reg = r;
            break;
          }
        }
      }	  
      if (reg == null) { 
    	  //LOG.info("Couldn't find "+name+" as region, looking for field"); 
    	  IRNode body = VisitUtil.getClassBody(type);
    	  IRNode decl = BindUtil.findFieldInBody(body, name); 
    	  if (decl != null) {
    		  reg = RegionModel.getInstance(decl);          
    	  } else if (LOG.isLoggable(Level.FINER)) {
    		  LOG.finer("Couldn't find "+qname);
    	  }
      }		         
      searchAfterLastType = (reg == null);

      if (result == null) {
    	  // Nothing found yet
    	  result = reg;
      } else if (reg != null && !result.equals(reg)) {
    	  // Found more than one distinct region
    	  LOG.warning("Found a duplicate region: "+reg);
      }
		}
	}
}
