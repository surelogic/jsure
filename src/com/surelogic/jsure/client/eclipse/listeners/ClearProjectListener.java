package com.surelogic.jsure.client.eclipse.listeners;

import java.util.*;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.surelogic.analysis.IIRProject;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.IClassPathEntry;
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
	private static final List<IProject> lastProjects = new ArrayList<IProject>();

	public ClearProjectListener() {
		// Nothing to do
	}
	
	public void resourceChanged(final IResourceChangeEvent event) {
		synchronized (ClearProjectListener.class) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				if (!lastProjects.isEmpty()) {
					clearJSureState(lastProjects);					
					lastProjects.clear();
				}
			} else if (event.getResource() instanceof IProject) {
				final IProject p = (IProject) event.getResource();
				if (!p.getName().equals(ProjectDrop.getProject())) {
					return; 
				}
				switch (event.getType()) {
				case IResourceChangeEvent.PRE_CLOSE:
				case IResourceChangeEvent.PRE_DELETE:
					if (clearAfterChange) {
						lastProjects.add(p);
					} else {
						clearJSureState(Collections.singletonList(p));
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
		clearJSureState(null);
	}
	
	public static void clearJSureState(Iterable<IProject> removedProjects) {
		System.out.println("Clearing JSure state");
		try {
			synchronized (Sea.getDefault()) {
				clearDropSea(clearAll, removedProjects);
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

	private static void computeProjectDependencies(Config c, Set<String> needed) {
		if (needed.contains(c.getProject())) {
			return; // Already handled
		}
		needed.add(c.getProject());
		for(IClassPathEntry e : c.getClassPath()) {
			if (e instanceof Config) {
				computeProjectDependencies((Config) e, needed);				
			}
		}
	}
	
	private static boolean debug = false;
	
	private static void clearDropSea(final boolean clearAll, Iterable<IProject> removed) {
		final ProjectsDrop pd = ProjectsDrop.getDrop();
		if (pd == null) {
			System.out.println("No ProjectsDrop, so nothing to clear");
			return;
		}
		//final List<IProject> removedProjects = new ArrayList<IProject>();
		Set<String> removedNames = null;
		final StringBuilder sb = debug ? new StringBuilder() : null;
		if (removed != null) {
			removedNames = new HashSet<String>();
			
			for(IProject p : removed) {
				//removedProjects.add(p);
				removedNames.add(p.getName());
				if (debug) {
					if (sb.length() != 0) {
						sb.append(", ");
					}
					sb.append(p.getName());
				}
			}
		}
		//System.out.println("Clearing drop-sea: "+sb);
		
		// Filter out needed projects
    	final Projects oldP = (Projects) pd.getIIRProjects();
    	final Set<String> needed;
    	if (removed != null) {
    		needed = new HashSet<String>();    	
    		for(JavacProject p : oldP) {
    			if (!removedNames.contains(p.getName())) {
    				computeProjectDependencies(p.getConfig(), needed);
    			}
    		}
    		if (debug) {
    			sb.setLength(0);
    			for(String s : needed) {
    				if (sb.length() != 0) {
    					sb.append(", ");
    				}
    				sb.append(s);
    			}
    			System.out.println("Still needed: "+sb);
    		}
    		removedNames.removeAll(needed);
    	}
		
		for (final RegionModel region : Sea.getDefault().getDropsOfExactType(
				RegionModel.class)) {
			final IRNode n = region.getNode();
			final IRNode root = VisitUtil.findRoot(n);
			if (removed != null && !removedNames.contains(region.getProject())) {
				// It's not part of any project that was removed
				continue; 
			}
			final CUDrop drop = CUDrop.queryCU(root);
			if (drop instanceof SourceCUDrop) {
				// System.out.println(region.getMessage());
				region.invalidate();
			}
		}
		RegionModel.purgeUnusedRegions();

		if (pd != null) {
			final List<IIRProject> removedJps = new ArrayList<IIRProject>();
	    	for(JavacProject jp : ((Projects) pd.getIIRProjects())) {
	    		if (removed == null || removedNames.contains(jp.getName())) {
	    			removedJps.add(jp);
	    		}
	    	}
	    	if (debug) {
	    		sb.setLength(0);
	    		for(IIRProject p : removedJps) {
	    			if (sb.length() != 0) {
	    				sb.append(", ");
	    			}
	    			sb.append(p.getName());
	    		}
	    		System.out.println("Removing projects: "+sb);
	    	}	    	
	    	for(SourceCUDrop cud : SourceCUDrop.invalidateAll(removedJps)) {
	    		System.out.println("Destroyed: "+cud.javaOSFileName);
				AdapterUtil.destroyOldCU(cud.cu);
			}
	    	
	    	for(JavacProject jp : oldP) {
	    		System.out.println("Deactivating "+jp);
	    		jp.deactivate();
	    	}
	    	final Projects newP = oldP.remove(removedNames);
	    	if (newP == null) {	    			    	
	    		// No projects left
	    		pd.invalidate();
	    	} else {
	    		ProjectsDrop.ensureDrop(newP);
	    	}
		} else {
			// Nuke everything
			for(SourceCUDrop cud : SourceCUDrop.invalidateAll(null)) {
				AdapterUtil.destroyOldCU(cud.cu);
			}
			if (clearAll) {
				for(BinaryCUDrop d : BinaryCUDrop.invalidateAll()) {
					AdapterUtil.destroyOldCU(d.cu);
				}
				PackageDrop.invalidateAll();
				IDE.getInstance().clearAll();
				AnnotationRules.XML_LOG.reset();
			}
		}		
		// TODO are these necessary?
		/*
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(ProjectsDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(ProjectDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(WarningDrop.class));
		Sea.getDefault().invalidateMatching(
				DropPredicateFactory.matchType(PromiseWarningDrop.class));
        */

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
	 * 
	 * Especially if we focus verification
	 */	
	public static void postNatureChangeUtility(boolean removedNature) {
		postNatureChangeUtility(null, removedNature);
	}
	
	/**
	 * Helper method to call after you add or remove the nature for JSure from
	 * one or more projects.
	 */	
	public static void postNatureChangeUtility(Iterable<IProject> projs, boolean removedNature) {		
		System.out.println("postNatureChangeUtility "+removedNature);
		if (removedNature) {
			ClearProjectListener.clearJSureState(projs);			
		}

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

	public static Collection<IProject> clearNatureFromAllOpenProjects() {
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
		postNatureChangeUtility(!removed.isEmpty());
		
		return removed;
	}
}
