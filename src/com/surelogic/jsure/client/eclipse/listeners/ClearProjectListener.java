package com.surelogic.jsure.client.eclipse.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.dc.Nature;
import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.eclipse.adapter.TypeBindings;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.BinaryCUDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

public class ClearProjectListener implements IResourceChangeListener {
	public static final boolean clearAfterChange = true;
	private static IProject lastProject = null;
	
	public void resourceChanged(IResourceChangeEvent event) {
		synchronized (ClearProjectListener.class) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				if (lastProject != null) {
					lastProject = null;
					clearJSureState();
				}
			}
			else if (event.getResource() instanceof IProject) {
				IProject p = (IProject) event.getResource();
				if (!p.getName().equals(ProjectDrop.getProject())) {
					return; // Not the current project, so ignore this
				}
				switch (event.getType()) {
				case IResourceChangeEvent.PRE_CLOSE:
				case IResourceChangeEvent.PRE_DELETE:		
					if (clearAfterChange) {
						lastProject = p;
					} else {
						clearJSureState();
					}
				default:
					return;
				}
			}
		}
	}

	/**
	 * Otherwise, clear the current project
	 */
	private static final boolean clearAll = true;	
	
	public static void clearJSureState() {
		try {
			clearDropSea(clearAll);
			/*
			for(Drop d : Sea.getDefault().getDrops()) {
				System.out.println(d.getMessage());
			}
			*/
			// System.out.println("Clearing all comp units");
			Binding.clearCompUnits(clearAll);
			TypeBindings.clearAll();

			if (clearAll) {
				JavaTypeFactory.clearAll();
			}			
			// Go ahead and garbage collect the IR.
			SlotInfo.gc();
		} catch (Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, "Problem while clearing JSure state", e);
		}
	}

	private static Set<IClearProjectHelper> helpers = new HashSet<IClearProjectHelper>();

	public static void addHelper(IClearProjectHelper h) {
		helpers.add(h);
	}

	private static void clearDropSea(final boolean clearAll) {
		// FIX to clear out drops for a given project
		for (RegionModel region : Sea.getDefault().getDropsOfExactType(
				RegionModel.class)) {
			IRNode n = region.getNode();
			IRNode root = VisitUtil.findRoot(n);
			CUDrop drop = CUDrop.queryCU(root);
			if (drop instanceof SourceCUDrop) {
				// System.out.println(region.getMessage());
				region.invalidate();
			}
		}
		RegionModel.purgeUnusedRegions();
		SourceCUDrop.invalidateAll();
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(ProjectDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(WarningDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(PromiseWarningDrop.class));
		
		if (clearAll) {
			BinaryCUDrop.invalidateAll();
			PackageDrop.invalidateAll();
			IDE.getInstance().clearAll();
		}
		for (IClearProjectHelper h : helpers) {
			if (h != null) {
				h.clearResults();
			}
		}
		Sea.getDefault().notifySeaObservers();
	}

	/**
	 * Helper method to call after you add or remove the nature for JSure from
	 * one or more projects.
	 */
	public static void postNatureChangeUtility() {
		ClearProjectListener.clearJSureState();

		// Handle projects that are still active
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		IProject first = null;

		for (IProject p : projects) {
			if (p.isOpen() && Nature.hasNature(p)) {
				if (first == null) {
					first = p;
				} else if (!IDE.allowMultipleProjects) {
					SLLogger.getLogger().severe(
							"Multiple projects with JSure nature: "
									+ first.getName() + " and " + p.getName());
					continue;
				}
				Nature.runAnalysis(p);
			}
		}
	}

	public static void clearNatureFromAllOpenProjects() {
		// Handle projects that are still active
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		if (projects == null)
			return;

		for (IProject p : projects) {
			if (p.isOpen() && Nature.hasNature(p)) {
				try {
					Nature.removeNatureFromProject(p);
				} catch (CoreException e) {
					SLLogger.getLogger().log(
							Level.SEVERE,
							"CoreException trying to remove the JSure nature from "
									+ p.getName(), e);
				}
			}
		}
		postNatureChangeUtility();
	}
}
