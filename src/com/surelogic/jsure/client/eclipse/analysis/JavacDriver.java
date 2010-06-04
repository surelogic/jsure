package com.surelogic.jsure.client.eclipse.analysis;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.*;
import com.surelogic.common.eclipse.*;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.*;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;

import edu.cmu.cs.fluid.dc.Majordomo;
import edu.cmu.cs.fluid.dc.Nature;
import edu.cmu.cs.fluid.dc.NotificationHub;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.util.*;

public class JavacDriver {
	private final List<IProject> building = new ArrayList<IProject>();
	private final Map<IProject, ProjectInfo> projects = new HashMap<IProject, ProjectInfo>();
	
	private JavacDriver() {
		PeriodicUtility.addHandler(new Runnable() {
			public void run() {
				final SLProgressMonitor mon = lastMonitor;
				if (mon != null && mon.isCanceled()) {
					if (lastMonitor == mon) {
						lastMonitor = null;
					}
					IDE.getInstance().setCancelled();
				}
			}			
		});
	}
	
	private static final JavacDriver prototype = new JavacDriver();
	
	public static JavacDriver getInstance() {
		return prototype;
	}

	volatile SLProgressMonitor lastMonitor = null;
	
	class ProjectInfo {
		final IProject project;
		final List<ICompilationUnit> allCompUnits;
		final Set<ICompilationUnit> cuDelta = new HashSet<ICompilationUnit>();
		final Set<IResource> removed = new HashSet<IResource>();
		/**
		 * All comp units includes delta?
		 */		
		boolean updated = true;
		
		ProjectInfo(IProject p, List<ICompilationUnit> cus) {
			project = p;
			allCompUnits = new ArrayList<ICompilationUnit>(cus);
		}
		
		boolean hasDeltas() {
			return !cuDelta.isEmpty();
		}
		
		void registerDelta(List<ICompilationUnit> cus) {
			if (!cus.isEmpty()) {
				cuDelta.addAll(cus);
				updated = false;
			}			
		}
		
		void registerResourcesDelta(List<Pair<IResource, Integer>> resources) {
			for(Pair<IResource, Integer> p : resources) {
				if (p.second() == IResourceDelta.REMOVED && p.first().getName().endsWith(".java")) {
					removed.add(p.first());
					updated = false;
				}
			}
		}
	
		private boolean needsUpdate() {
			return !updated && !cuDelta.isEmpty();
		}
		
		Iterable<ICompilationUnit> getAllCompUnits() {
			if (needsUpdate()) {
				update(allCompUnits, cuDelta, removed);
			}
			return allCompUnits;			
		}
		Iterable<IResource> getRemovedResources() {
			return removed;
		}
		Iterable<ICompilationUnit> getDelta() {
			if (needsUpdate()) {
				Iterable<ICompilationUnit> result = new ArrayList<ICompilationUnit>(cuDelta);
				update(allCompUnits, cuDelta, removed);
				return result;
			}
			return allCompUnits;	 
		}
		
		/**
		 * Adds itself to projects to make sure that it's not created multiple times
		 */
		Config makeConfig(final Projects projects, boolean all) throws JavaModelException {
			Config config = new ZippedConfig(project.getName(), false);
			projects.add(config);
			setOptions(config);
			
			for(IResource res : getRemovedResources()) {
				final File f = res.getLocation().toFile();
				config.addRemovedFile(f);
			}
			for(ICompilationUnit icu : all ? getAllCompUnits() : getDelta()) {				
				final IPath path = icu.getResource().getFullPath();
				final IPath loc = icu.getResource().getLocation();
				final File f = loc.toFile();
				String qname;
				if (f.exists()) {
					String pkg = null;
					for(IPackageDeclaration pd : icu.getPackageDeclarations()) {
						config.addPackage(pd.getElementName());
						pkg = pd.getElementName();
					}
					qname = icu.getElementName();
					if (qname.endsWith(".java")) {
						qname = qname.substring(0, qname.length()-5);
					}
					if (pkg != null) {
						qname = pkg+'.'+qname;
					}
				} else { // Removed
					qname = f.getName();
				}
				config.addFile(new JavaSourceFile(qname, f, path.toPortableString()));
			}			
			addDependencies(projects, config, project, false);
			return config;
		}
		
		private void setOptions(Config config) {
			IJavaProject jp = JDTUtility.getJavaProject(config.getProject());
			config.setOption(Config.SOURCE_LEVEL, JDTUtility.getMajorJavaVersion(jp));
		}

		void addDependencies(Projects projects, Config config, IProject p, boolean addSource) throws JavaModelException {
			final IJavaProject jp = JDTUtility.getJavaProject(p.getName());			
			String mappedJDK = null;
			// TODO what export rules?
			scanForJDK(projects, jp);
			
			for(IClasspathEntry cpe : jp.getResolvedClasspath(true)) {				
				// TODO ignorable since they'll be handled by the compiler
				//cpe.getAccessRules();
				//cpe.combineAccessRules();
				
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					if (addSource) {
						// TODO handle multiple deltas?
						final File dir = EclipseUtility.resolveIPath(cpe.getPath());
						final File[] excludes = new File[cpe.getExclusionPatterns().length];
						int i=0;
						for(IPath xp : cpe.getExclusionPatterns()) {
							excludes[i] = EclipseUtility.resolveIPath(xp);
							i++;
						}
						Util.addJavaFiles(dir, config, excludes);
					}
					config.addToClassPath(config);
					break;
				case IClasspathEntry.CPE_LIBRARY:
					//System.out.println("Adding "+cpe.getPath()+" for "+p.getName());
					final File f = EclipseUtility.resolveIPath(cpe.getPath());
					String mapped = projects.checkMapping(f);
					if (mapped != null) {
						if (mappedJDK != null) {
							//System.out.println("Ignoring "+f);
							break; // Already handled
						}
						mappedJDK = mapped;
						config.addToClassPath(projects.get(mappedJDK).getConfig());
					} else {
						config.addJar(f, cpe.isExported());
					}
					break;
				case IClasspathEntry.CPE_PROJECT:
					final String projName = cpe.getPath().lastSegment();
					final JavacProject jcp = projects.get(projName);
					if (jcp != null) {
						// Already created
						config.addToClassPath(jcp.getConfig());
						break;
					}
					final IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
					final ProjectInfo info = JavacDriver.this.projects.get(proj);
					final Config dep;
					if (info != null) {
						final boolean hasDeltas = info.hasDeltas();
						dep = info.makeConfig(projects, hasDeltas);
					} else {
						dep = new ZippedConfig(projName, cpe.isExported());		
						projects.add(dep);
					}
					config.addToClassPath(dep);

					if (info == null) {
						addDependencies(projects, dep, proj, true);													
					}
					break;
				default:
					System.out.println("Unexpected: "+cpe);
				}
			}
		}
		
		private void scanForJDK(Projects projects, IJavaProject jp) throws JavaModelException {
			for(IClasspathEntry cpe : jp.getRawClasspath()) {								
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
					final IClasspathContainer cc = JavaCore.getClasspathContainer(cpe.getPath(), jp);					
					if (cc.getDescription().startsWith(JavacTypeEnvironment.JRE_LIBRARY)) { // HACK
						JavacProject jcp = projects.get(cc.getDescription());
						if (jcp == null) {
							projects.add(makeConfig(projects, cc));							
						}
						return;
					}			
				}
			}
		}

		private Config makeConfig(Projects projects, final IClasspathContainer cc) {
			final Config config = new Config(cc.getDescription(), true);
			for(IClasspathEntry cpe : cc.getClasspathEntries()) {
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
					final File f = EclipseUtility.resolveIPath(cpe.getPath());
					//System.out.println("Adding "+f+" for "+cc.getDescription());
					config.addJar(f, true);
					projects.mapToProject(f, cc.getDescription());
					break;
				default:
					throw new IllegalStateException("Got entryKind: "+cpe.getEntryKind());
				}
			}
			return config;
		}
		
		/**
		 * Either add/remove as needed
		 * @param removed2 
		 */
		void update(Collection<ICompilationUnit> all, Collection<ICompilationUnit> cus, 
				    Set<IResource> removed) {
			// Filter out removed files
			final Iterator<ICompilationUnit> it = all.iterator();
			while (it.hasNext()) {
				final ICompilationUnit cu = it.next();
				if (removed.contains(cu.getResource())) {
					it.remove();
				}
			}
			// Add in changed ones
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
			updated = true;
		}
	}
	
	void preBuild(final IProject p) {
		System.out.println("Pre-build for "+p);
		
		if (building.isEmpty()) {
			// First project build, so populate with active projects
			for(IJavaProject jp : JDTUtility.getJavaProjects()) {
				final IProject proj = jp.getProject();
				if (Nature.hasNature(proj)) {
					building.add(proj);
				}
			}
		}
	}
	
	/**
	 * Register resources
	 */
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
			info.registerResourcesDelta(resources);
		}
	}
	
	public void doBuild(IProject p) {	    
		System.out.println("Finished 'build' for "+p);
		/*
		//ProjectDrop.ensureDrop(p.getName(), p);
		final ProjectInfo info = projects.get(p);
		if (info == null) {
			return; // No info!
		}
		*/
		// Check if any projects are still building
		building.remove(p);
		if (!building.isEmpty()) {
			System.out.println("Still waiting for "+building);
			return;
		}
		System.out.println("Starting to build projects");
        JavacEclipse.initialize();
		try {
			if (!XUtil.testing) {
				final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs(store);
			}
			//final boolean hasDeltas = info.hasDeltas();
		    final Projects newProjects = makeProjects();	    
		    final File dataDir = 
		        //new File(IDE.getInstance().getStringPreference(IDEPreferences.DATA_DIRECTORY));
		        PreferenceConstants.getJSureDataDirectory();
		    final String time = SLUtility.toStringHMS(new Date());
		    final String name = p.getName()+' '+time.replace(':', '-');		   		    
		    final File zips   = new File(dataDir, name+"/zips");
		    final File target = new File(dataDir, name+"/srcs");
		    target.mkdirs();
		    newProjects.setRun(name);

		    JSureHistoricalSourceView.setLastRun(newProjects, new ISourceZipFileHandles() {
                public Iterable<File> getSourceZips() {
                    return Arrays.asList(zips.listFiles());
                }		        
		    });

		    Projects oldProjects = (Projects) ProjectsDrop.getProjects();
		    AnalysisJob analysis = new AnalysisJob(oldProjects, newProjects, target, zips);
		    CopyJob copy = new CopyJob(newProjects, target, zips, analysis);
		    if (XUtil.testing) {
		    	copy.run(new NullSLProgressMonitor());
		    } else {
		    	EclipseJob.getInstance().scheduleWorkspace(copy);
		    }
		} catch(JavaModelException e) {
		    System.err.println("Unable to make config for JSure");
		    e.printStackTrace();
		    return;
		}
	}
	
	private Projects makeProjects() throws JavaModelException {
		final Projects projects = new Projects();
		for(ProjectInfo info : this.projects.values()) {
			if (!projects.contains(info.project.getName())) {
				info.makeConfig(projects, !info.hasDeltas());	
			} else {
				// Already added as a dependency?
			}
			Config config = projects.get(info.project.getName()).getConfig();
			config.setOption(Config.AS_SOURCE, true);
		}
		return projects;

	}

	static class ZippedConfig extends Config {
		ZippedConfig(String name, boolean isExported) {
			super(name, isExported);
		}
		@Override
		protected Config newConfig(String name, boolean isExported) {
			return new ZippedConfig(name, isExported);
		}		
		@Override
		public void zipSources(File zipDir) throws IOException {
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getProject());
			final SourceZip srcZip = new SourceZip(project);
			File zipFile           = new File(zipDir, project.getName()+".zip");
			if (!zipFile.exists()) {
				zipFile.getParentFile().mkdirs();
				srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
			} else {
				//System.out.println("Already exists: "+zipFile);
			}			
			super.zipSources(zipDir);
		}
		@Override
		public void copySources(File zipDir, File targetDir) throws IOException {
            final List<JavaSourceFile> srcFiles = new ArrayList<JavaSourceFile>();
            final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getProject());
            targetDir.mkdir();
            
            File projectDir = new File(targetDir, project.getName());
            File zipFile    = new File(zipDir, project.getName()+".zip");
            ZipFile zf      = new ZipFile(zipFile);
            
            // Get class mapping (qname->zip path)
            Properties props = new Properties();
            ZipEntry mapping = zf.getEntry(AbstractJavaZip.CLASS_MAPPING);            
            props.load(zf.getInputStream(mapping));

            // Reverse mapping
            Map<String,List<String>> path2qnames = new HashMap<String,List<String>>();
            for(Map.Entry<Object,Object> e : props.entrySet()) {
                String path = (String) e.getValue();
                List<String> l = path2qnames.get(path);
                if (l == null) {
                    l = new ArrayList<String>();
                    path2qnames.put(path, l);
                }                
                l.add((String) e.getKey());
            }
            
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                File f = new File(projectDir, ze.getName());
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    FileUtility.copy(ze.getName(), zf.getInputStream(ze), f);
                }
                // Finish setting up srcFiles
                if (ze.getName().endsWith(".java")) {
                    final List<String> names = path2qnames.get(ze.getName());
                    if (names != null) {
                        for(String name : names) {
                            //System.out.println("Mapping "+name+" to "+f.getAbsolutePath());
                            srcFiles.add(new JavaSourceFile(name.replace('$', '.'), f, null));
                        }
                    } else if (ze.getName().endsWith("/package-info.java")) {
                        // TODO what to do about this?
                    } else {
                        System.err.println("Unable to get qname for "+ze.getName());
                    }
                }
            }
            zf.close();
            
            this.setFiles(srcFiles);
            super.copySources(zipDir, targetDir);
        }  
	}
	
	abstract class Job extends AbstractSLJob {
	    final Projects projects;
	    /**
	     * Where the source files will be copied to
	     */
	    final File targetDir;
	    
	    /**
         * Where the source zips will be created
         */
        final File zipDir;
	    
	    Job(String name, Projects projects, File target, File zips) {
	        super(name);
            this.projects = projects;
            targetDir = target;
            zipDir = zips;
        }
	}
	
	class CopyJob extends Job {
	    private final SLJob afterJob;

        CopyJob(Projects projects, File target, File zips, SLJob after) {
            super("Copying project info for "+projects.getLabel(), projects, target, zips);
            afterJob = after;
        }

        public SLStatus run(SLProgressMonitor monitor) {
        	final long start = System.currentTimeMillis();
        	try {
        		for(Config config : projects.getConfigs()) {
        			config.zipSources(zipDir);           
        		}
            } catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while zipping sources", e);
            }
            try {
            	for(Config config : projects.getConfigs()) {
            		config.relocateJars(targetDir);
            	}
			} catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while copying jars", e);
			}
            final long end = System.currentTimeMillis();
            System.out.println("Copying = "+(end-start)+" ms");
            
            if (afterJob != null) {
        	    if (XUtil.testing) {
        	    	afterJob.run(monitor);
        	    } else {
        	    	EclipseJob.getInstance().scheduleDb(afterJob, false, false, Util.class.getName());
        	    }
            }
            return SLStatus.OK_STATUS;
        }   
	}
	
	class AnalysisJob extends Job {
		private final Projects oldProjects;
		
        AnalysisJob(Projects oldProjects, Projects projects, File target, File zips) {
            super("Running JSure on "+projects.getLabel(), projects, target, zips);
            this.oldProjects = oldProjects;
        }

        public SLStatus run(SLProgressMonitor monitor) {
        	lastMonitor = monitor;
        	projects.setMonitor(monitor);
        	
        	if (XUtil.testingWorkspace) {
        		System.out.println("Clearing state before running analysis");
        		ClearProjectListener.clearJSureState();
        	}        	
        	System.out.println("Starting analysis for "+projects.getLabel());
        	try {
        		for(Config config : projects.getConfigs()) {
        			config.copySources(zipDir, targetDir);
        		}
            } catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while copying sources", e);
            }
            
            JavacEclipse.initialize();
            NotificationHub.notifyAnalysisStarting();
            try {
            	boolean ok;
            	if (oldProjects == null) {
            		ok = Util.openFiles(projects);
            	} else {
            		ok = Util.openFiles(oldProjects, projects);
            	}
            	/*
            	final File rootLoc = EclipseUtility.getProject(config.getProject()).getLocation().toFile();
            	final File xmlLocation = new File(rootLoc, "oracle20100415.sea.xml");
            	final SeaSummary.Diff diff = SeaSummary.diff(config.getProject(), Sea.getDefault(), 
            			xmlLocation);
            			*/
            	if (!ok) {
            	    NotificationHub.notifyAnalysisPostponed(); // TODO fix
            	    if (lastMonitor == monitor) {
            	    	lastMonitor = null;
            	    }
                  	return SLStatus.CANCEL_STATUS;
            	}
            } catch (Exception e) {
                NotificationHub.notifyAnalysisPostponed(); // TODO
                if (monitor.isCanceled()) {
                    if (lastMonitor == monitor) {
                    	lastMonitor = null;
                    }
                	return SLStatus.CANCEL_STATUS;
                }
                return SLStatus.createErrorStatus("Problem while running JSure", e);
            }
            NotificationHub.notifyAnalysisCompleted();
            if (lastMonitor == monitor) {
            	lastMonitor = null;
            }
            
            return SLStatus.OK_STATUS;
        }  
	}
}
