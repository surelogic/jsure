package com.surelogic.jsure.client.eclipse.listeners;

import java.util.*;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.fluid.javac.JavacProject;
import com.surelogic.fluid.javac.Projects;

import edu.cmu.cs.fluid.dc.Nature;
import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.eclipse.adapter.TypeBindings;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.adapter.AdapterUtil;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

public class ClearProjectListener implements IResourceChangeListener {
	public static final boolean clearAfterChange = true;
	private static IProject lastProject = null;

	public void resourceChanged(final IResourceChangeEvent event) {
		synchronized (ClearProjectListener.class) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				if (lastProject != null) {
					lastProject = null;
					clearJSureState();
				}
			} else if (event.getResource() instanceof IProject) {
				final IProject p = (IProject) event.getResource();
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
					return;
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
			synchronized (Sea.getDefault()) {
				clearDropSea(clearAll);
			}
			/*
			 * for(Drop d : Sea.getDefault().getDrops()) {
			 * System.out.println(d.getMessage()); }
			 */
			// System.out.println("Clearing all comp units");
			Binding.clearCompUnits(clearAll);
			TypeBindings.clearAll();

			if (clearAll) {
				JavaTypeFactory.clearAll();
				IDE.getInstance().notifyASTsChanged();
				IDE.getInstance().clearAll();
			}
			// Go ahead and garbage collect the IR.
			SlotInfo.gc();
		} catch (final Exception e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Problem while clearing JSure state", e);
		}
	}

	private static Set<IClearProjectHelper> helpers = new HashSet<IClearProjectHelper>();

	public static void addHelper(final IClearProjectHelper h) {
		helpers.add(h);
	}

	private static void clearDropSea(final boolean clearAll) {
		// FIX to clear out drops for a given project
		for (final RegionModel region : Sea.getDefault().getDropsOfExactType(
				RegionModel.class)) {
			final IRNode n = region.getNode();
			final IRNode root = VisitUtil.findRoot(n);
			final CUDrop drop = CUDrop.queryCU(root);
			if (drop instanceof SourceCUDrop) {
				// System.out.println(region.getMessage());
				region.invalidate();
			}
		}
		RegionModel.purgeUnusedRegions();
		for(SourceCUDrop cud : SourceCUDrop.invalidateAll()) {
			AdapterUtil.destroyOldCU(cud.cu);
		}
		
	    ProjectsDrop pd = ProjectsDrop.getDrop();
	    if (pd != null) {
	    	for(JavacProject jp : ((Projects) pd.getIIRProjects())) {
	    		System.out.println("Deactivating "+jp);
	    		jp.deactivate();
	    	}
	    }
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(ProjectsDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(ProjectDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(WarningDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(PromiseWarningDrop.class));

		if (clearAll) {
			for(BinaryCUDrop d : BinaryCUDrop.invalidateAll()) {
				AdapterUtil.destroyOldCU(d.cu);
			}
			PackageDrop.invalidateAll();
			IDE.getInstance().clearAll();
			AnnotationRules.XML_LOG.reset();
		}
		for (final IClearProjectHelper h : helpers) {
			if (h != null) {
				h.clearResults(clearAll);
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

		for (final IProject p : projects) {
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

	public static Iterable<IProject> clearNatureFromAllOpenProjects() {
		// Handle projects that are still active
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		if (projects == null) {
			return Collections.emptyList();
		}
		final List<IProject> removed = new ArrayList<IProject>();
		for (final IProject p : projects) {
			if (p.isOpen() && Nature.hasNature(p)) {
				try {
					Nature.removeNatureFromProject(p);
					removed.add(p);
				} catch (final CoreException e) {
					SLLogger.getLogger().log(
							Level.SEVERE,
							"CoreException trying to remove the JSure nature from "
									+ p.getName(), e);
				}
			}
		}
		postNatureChangeUtility();
		
		return removed;
	}
}
