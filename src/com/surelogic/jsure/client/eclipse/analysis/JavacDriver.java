package com.surelogic.jsure.client.eclipse.analysis;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.SourceZip;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
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
					//System.out.println("Adding "+cpe.getPath()+" for "+p.getName());
					config.addJar(EclipseUtility.resolveIPath(cpe.getPath()).getAbsolutePath());
					break;
				case IClasspathEntry.CPE_PROJECT:
					String projName = cpe.getPath().lastSegment();
					IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
					if (config.addProject(projName)) {
					    addDependencies(config, proj);
					}
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
	
	void doBuild(IProject p, SLProgressMonitor monitor) {
		ProjectDrop.ensureDrop(p.getName(), p);
		final ProjectInfo info = projects.get(p);
		if (info == null) {
			return; // No info!
		}
		try {
		    final Config config = info.makeConfig();
		    File zips   = null; // TODO where will we copy to
		    File target = null; // TODO where will we copy to
		    AnalysisJob analysis = new AnalysisJob(config, target);
		    CopyJob copy = new CopyJob(config, target, zips, analysis);
		    // TODO fix to lock the workspace
		    EclipseJob.getInstance().schedule(copy);
		} catch(JavaModelException e) {
		    System.err.println("Unable to make config for JSure");
		    e.printStackTrace();
		    return;
		}
	}
	
	abstract class Job extends AbstractSLJob {
	    final Config config;
	    /**
	     * Where the source files will be copied to
	     */
	    final File targetDir;
	    
	    Job(String name, Config config, File target) {
	        super(name);
            this.config = config;
            targetDir = target;
        }
	}
	
	class CopyJob extends Job {
	    private final SLJob afterJob;
	    /**
	     * Where the source zips will be created
	     */
	    private final File zipDir;
	    
        CopyJob(Config config, File target, File zips, SLJob after) {
            super("Copying project info for "+config.getProject(), config, target);
            afterJob = after;
            zipDir = zips;
        }

        public SLStatus run(SLProgressMonitor monitor) {
            for(String proj : config.getProjects()) {
                IProject ip = ResourcesPlugin.getWorkspace().getRoot().getProject(proj);
                try {
                    copySources(ip);
                } catch (IOException e) {
                    return SLStatus.createErrorStatus("Problem while copying sources", e);
                }
            }            
            // TODO projects need separate lists of jars
            for(String jar : config.getJars()) {
                final String name;
                final int lastSlash = jar.lastIndexOf('/');
                if (lastSlash < 0) {
                    name = jar;
                } else {
                    name = jar.substring(lastSlash+1);
                }
                FileUtility.copy(new File(jar), new File(targetDir, name));
            }
            
            if (afterJob != null) {
                EclipseJob.getInstance().schedule(afterJob);
            }
            return SLStatus.OK_STATUS;
        }
            
        boolean copySources(IProject project) throws IOException {
            final SourceZip srcZip = new SourceZip(project);
            File zipFile           = new File(zipDir, project.getName()+".zip");
            if (zipFile.isFile()) {
                return false;
            }
            srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);

            targetDir.mkdir();
            File projectDir = new File(targetDir, project.getName());
            ZipFile zf = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                File f = new File(projectDir, ze.getName());
                f.getParentFile().mkdirs();
                FileUtility.copy(ze.getName(), zf.getInputStream(ze), f);
            }
            zf.close();
            
            return true;
        }	    
	}
	
	class AnalysisJob extends Job {
        AnalysisJob(Config config, File target) {
            super("Running JSure on "+config.getProject(), config, target);
        }

        public SLStatus run(SLProgressMonitor monitor) {
            try {
                Util.openFiles(config, monitor);
            } catch (Exception e) {
                return SLStatus.createErrorStatus("Problem while running JSure", e);
            }
            return SLStatus.OK_STATUS;
        }
	    
	}
}
