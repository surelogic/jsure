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
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

public class ClearProjectListener implements IResourceChangeListener {
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
      if (event.getResource() instanceof IProject) {
        clearJSureState();
      }
    }
  }

  public static void clearJSureState() {
	  try {
		  clearDropSea();

//		  System.out.println("Clearing all comp units");
		  Binding.clearCompUnits();        
	  } catch(Exception e) {
		  e.printStackTrace();
	  }
  }
  
  private static Set<IClearProjectHelper> helpers = new HashSet<IClearProjectHelper>();
  
  public static void addHelper(IClearProjectHelper h) {
	  helpers.add(h);
  }
  
  public static void clearDropSea() {
    // Sea.getDefault().invalidateAll();
	// final IJavaFileLocator loc = IDE.getInstance().getJavaFileLocator();
	for(RegionModel region : Sea.getDefault().getDropsOfExactType(RegionModel.class)) {
	  IRNode n    = region.getNode();
	  IRNode root = VisitUtil.findRoot(n);
	  CUDrop drop = CUDrop.queryCU(root);
	  if (drop instanceof SourceCUDrop) {
		System.out.println(region.getMessage());
		region.invalidate();
	  }
	}
	RegionModel.purgeUnusedRegions();
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
    Sea.getDefault().notifySeaObservers();
  }
}


