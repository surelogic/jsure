/*
 * Created on Mar 4, 2005
 *
 */
package com.surelogic.jsure.client.eclipse.listeners;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;

public class ClearProjectListener implements IResourceChangeListener {
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
      if (event.getResource() instanceof IProject) {
        try {
          clearDropSea();
        
//          System.out.println("Clearing all comp units");
          Binding.clearCompUnits();        
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  private static Set<IClearProjectHelper> helpers = new HashSet<IClearProjectHelper>();
  
  public static void addHelper(IClearProjectHelper h) {
	  helpers.add(h);
  }
  
  public static void clearDropSea() {
    // Sea.getDefault().invalidateAll();
    SourceCUDrop.invalidateAll();
    Sea.getDefault().invalidateMatching(
        DropPredicateFactory.matchType(WarningDrop.class));
    Sea.getDefault().invalidateMatching(
        DropPredicateFactory.matchType(PromiseWarningDrop.class));
    
    for(IClearProjectHelper h : helpers) {
    	if (h != null) {
    		h.clearResults();
    	}
    }
  }
}


