package com.surelogic.jsure.client.eclipse.analysis;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.Util;

import edu.cmu.cs.fluid.dc.Majordomo;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;
import edu.cmu.cs.fluid.util.*;

public class JavacDriver {
	private final Map<IProject, ProjectInfo> projects = new HashMap<IProject, ProjectInfo>();
	
	private JavacDriver() {
		// Just to keep it private
	}
	
	private static final JavacDriver prototype = new JavacDriver();
	
	public static JavacDriver getInstance() {
		return prototype;
	}

	static class ProjectInfo {
		final IProject project;
		final List<ICompilationUnit> allCompUnits;
		final Set<ICompilationUnit> cuDelta = new HashSet<ICompilationUnit>();
		/**
		 * All comp units includes delta?
		 */		
		boolean updated = true;
		
		ProjectInfo(IProject p, List<ICompilationUnit> cus) {
			project = p;
			allCompUnits = new ArrayList<ICompilationUnit>(cus);
		}
		
		void registerDelta(List<ICompilationUnit> cus) {
			if (!cus.isEmpty()) {
				cuDelta.addAll(cus);
				updated = false;
			}			
		}
	
		Iterable<ICompilationUnit> getAllCompUnits() {
			if (!updated && !cuDelta.isEmpty()) {
				update(allCompUnits, cuDelta);
			}
			return allCompUnits;			
		}
		
		Config makeConfig() throws JavaModelException {
			Config config = new Config(project.getName());
			for(ICompilationUnit icu : getAllCompUnits()) {
				final File f = icu.getResource().getLocation().toFile();
				for(IPackageDeclaration pd : icu.getPackageDeclarations()) {
					config.addPackage(pd.getElementName());
				}	
				config.addFile(new Pair<String, File>(icu.getElementName(), f));
			}			
			addDependencies(config, project);
			return config;
		}
		
		static void addDependencies(Config config, IProject p) throws JavaModelException {
			final boolean isDependency = !config.getProject().equals(p.getName());
			final IJavaProject jp = JDTUtility.getJavaProject(p.getName());
			for(IClasspathEntry cpe : jp.getResolvedClasspath(true)) {
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					if (isDependency) {
						final File dir = EclipseUtility.resolveIPath(cpe.getPath());
						final File[] excludes = new File[cpe.getExclusionPatterns().length];
						int i=0;
						for(IPath xp : cpe.getExclusionPatterns()) {
							excludes[i] = EclipseUtility.resolveIPath(xp);
							i++;
						}
						Util.addJavaFiles(dir, config, true, excludes);
					}
					break;
				case IClasspathEntry.CPE_LIBRARY:
					config.addJar(EclipseUtility.resolveIPath(cpe.getPath()).getAbsolutePath());
					break;
				case IClasspathEntry.CPE_PROJECT:
					String projName = cpe.getPath().lastSegment();
					IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
					addDependencies(config, proj);
					break;
				default:
					System.out.println("Unexpected: "+cpe);
				}
			}
		}
		
		/**
		 * Either add/remove as needed
		 */
		static void update(Collection<ICompilationUnit> all, Collection<ICompilationUnit> cus) {
			for(ICompilationUnit cu : cus) {
				// TODO use a Set instead?
				if (cu.getResource().exists()) {
					if (!all.contains(cu)) {
						all.add(cu);
						//System.out.println("Added:   "+cu.getHandleIdentifier());
					} else {
						//System.out.println("Exists:  "+cu.getHandleIdentifier());
					}
				} else {
					all.remove(cu);
					//System.out.println("Deleted: "+cu.getHandleIdentifier());
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	void registerBuild(IProject project, Map args,
			           List<Pair<IResource, Integer>> resources, 
			           List<ICompilationUnit> cus) {
		final String kind = (String) args.get(Majordomo.BUILD_KIND);		
		final int k = Integer.parseInt(kind);
		if (k == IncrementalProjectBuilder.CLEAN_BUILD || 
			k == IncrementalProjectBuilder.FULL_BUILD) {
			// TODO what about resources?
			projects.put(project, new ProjectInfo(project, cus));
			//System.out.println("Got full build");
		} else {
			ProjectInfo info = projects.get(project);
			if (info == null) {
				throw new IllegalStateException("No full build before this?");
			}
			info.registerDelta(cus);
		}
	}
	
	void doBuild(IProject p) {
		ProjectDrop.ensureDrop(p.getName(), p);
		final ProjectInfo info = projects.get(p);
		if (info == null) {
			return; // No info!
		}
		// TODO in a job!
		try {
			final Config config = info.makeConfig();
			Util.openFiles(config);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
