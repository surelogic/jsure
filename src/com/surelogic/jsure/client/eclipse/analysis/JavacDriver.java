package com.surelogic.jsure.client.eclipse.analysis;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import com.surelogic.common.*;
import com.surelogic.common.eclipse.*;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.JavacProject;
import com.surelogic.fluid.javac.Util;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;

import edu.cmu.cs.fluid.dc.Majordomo;
import edu.cmu.cs.fluid.dc.NotificationHub;
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
		
		boolean hasDeltas() {
			return !cuDelta.isEmpty();
		}
		
		void registerDelta(List<ICompilationUnit> cus) {
			if (!cus.isEmpty()) {
				cuDelta.addAll(cus);
				updated = false;
			}			
		}
	
		private boolean needsUpdate() {
			return !updated && !cuDelta.isEmpty();
		}
		
		Iterable<ICompilationUnit> getAllCompUnits() {
			if (needsUpdate()) {
				update(allCompUnits, cuDelta);
			}
			return allCompUnits;			
		}
		
		Iterable<ICompilationUnit> getDelta() {
			if (needsUpdate()) {
				return cuDelta;
			}
			return allCompUnits;	 
		}
		
		Config makeConfig(boolean all) throws JavaModelException {
			Config config = new ZippedConfig(project.getName(), false);
			for(ICompilationUnit icu : all ? getAllCompUnits() : getDelta()) {
				final File f = icu.getResource().getLocation().toFile();
				String pkg = null;
				for(IPackageDeclaration pd : icu.getPackageDeclarations()) {
					config.addPackage(pd.getElementName());
					pkg = pd.getElementName();
				}
				String qname = icu.getElementName();
				if (qname.endsWith(".java")) {
				    qname = qname.substring(0, qname.length()-5);
				}
				if (pkg != null) {
				    qname = pkg+'.'+qname;
				}
				config.addFile(new Pair<String, File>(qname, f));
			}			
			addDependencies(config, project, false);
			return config;
		}
		
		static void addDependencies(Config config, IProject p, boolean addSource) throws JavaModelException {
			final IJavaProject jp = JDTUtility.getJavaProject(p.getName());			
			// TODO what export rules?
			
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
					config.addJar(EclipseUtility.resolveIPath(cpe.getPath()), cpe.isExported());
					break;
				case IClasspathEntry.CPE_PROJECT:
					String projName = cpe.getPath().lastSegment();
					IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
					Config dep = new ZippedConfig(projName, cpe.isExported());	
					config.addToClassPath(dep);
					addDependencies(dep, proj, true);								
					break;
				default:
					System.out.println("Unexpected: "+cpe);
				}
			}
		}
		
		/**
		 * Either add/remove as needed
		 */
		void update(Collection<ICompilationUnit> all, Collection<ICompilationUnit> cus) {
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
	
	public void doBuild(IProject p) {	    
		//ProjectDrop.ensureDrop(p.getName(), p);
		final ProjectInfo info = projects.get(p);
		if (info == null) {
			return; // No info!
		}
        JavacEclipse.initialize();
		try {
			final boolean hasDeltas = info.hasDeltas();
		    final Config config = info.makeConfig(!hasDeltas);		    
		    final File dataDir = 
		        //new File(IDE.getInstance().getStringPreference(IDEPreferences.DATA_DIRECTORY));
		        PreferenceConstants.getJSureDataDirectory();
		    final String time = SLUtility.toStringHMS(new Date());
		    final String name = p.getName()+' '+time.replace(':', '-');		   		    
		    final File zips   = new File(dataDir, name+"/zips");
		    final File target = new File(dataDir, name+"/srcs");
		    target.mkdirs();
		    config.setRun(name);

		    JSureHistoricalSourceView.setLastRun(config, new ISourceZipFileHandles() {
                public Iterable<File> getSourceZips() {
                    return Arrays.asList(zips.listFiles());
                }		        
		    });
		    JavacProject project = null;
		    if (hasDeltas) {
		    	ProjectDrop pd = ProjectDrop.getDrop();
		    	project = (JavacProject) pd.getIIRProject();
		    }		    
		    AnalysisJob analysis = new AnalysisJob(project, config, target, zips);
		    CopyJob copy = new CopyJob(config, target, zips, analysis);
		    // TODO fix to lock the workspace
		    EclipseJob.getInstance().schedule(copy);
		} catch(JavaModelException e) {
		    System.err.println("Unable to make config for JSure");
		    e.printStackTrace();
		    return;
		}
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
			}
			
			super.zipSources(zipDir);
		}
		@Override
		public void copySources(File zipDir, File targetDir) throws IOException {
            final List<Pair<String, File>> srcFiles = new ArrayList<Pair<String, File>>();
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
                if (f.exists()) {
                	continue;
                }
                f.getParentFile().mkdirs();
                FileUtility.copy(ze.getName(), zf.getInputStream(ze), f);
                if (ze.getName().endsWith(".java")) {
                    final List<String> names = path2qnames.get(ze.getName());
                    if (names != null) {
                        for(String name : names) {
                            //System.out.println("Mapping "+name+" to "+f.getAbsolutePath());
                            srcFiles.add(new Pair<String,File>(name.replace('$', '.'), f));
                        }
                    } else if (ze.getName().endsWith("/package-info.java")) {
                        // TODO what to do about this?
                    } else {
                        throw new IllegalStateException("Unable to get qname for "+ze.getName());
                    }
                }
            }
            zf.close();
            
            this.setFiles(srcFiles);
            super.copySources(zipDir, targetDir);
        }  
	}
	
	abstract class Job extends AbstractSLJob {
	    final Config config;
	    /**
	     * Where the source files will be copied to
	     */
	    final File targetDir;
	    
	    /**
         * Where the source zips will be created
         */
        final File zipDir;
	    
	    Job(String name, Config config, File target, File zips) {
	        super(name);
            this.config = config;
            targetDir = target;
            zipDir = zips;
        }
	}
	
	class CopyJob extends Job {
	    private final SLJob afterJob;

        CopyJob(Config config, File target, File zips, SLJob after) {
            super("Copying project info for "+config.getProject(), config, target, zips);
            afterJob = after;
        }

        public SLStatus run(SLProgressMonitor monitor) {
        	final long start = System.currentTimeMillis();
        	try {
        		config.zipSources(zipDir);           
            } catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while zipping sources", e);
            }
            try {
				config.relocateJars(targetDir);
			} catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while copying jars", e);
			}
            final long end = System.currentTimeMillis();
            System.out.println("Copying = "+(end-start)+" ms");
            
            if (afterJob != null) {
                EclipseJob.getInstance().schedule(afterJob);
            }
            return SLStatus.OK_STATUS;
        }   
	}
	
	class AnalysisJob extends Job {
		private final JavacProject project;
		
        AnalysisJob(JavacProject project, Config config, File target, File zips) {
            super("Running JSure on "+config.getProject(), config, target, zips);
            this.project = project;
        }

        public SLStatus run(SLProgressMonitor monitor) {
        	try {
        		config.copySources(zipDir, targetDir);
            } catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while copying sources", e);
            }
            
            JavacEclipse.initialize();
            NotificationHub.notifyAnalysisStarting();
            try {
            	if (project == null) {
            		Util.openFiles(config, monitor);
            	} else {
            		Util.openFiles(project, config, monitor);
            	}
            } catch (Exception e) {
                NotificationHub.notifyAnalysisPostponed();
                return SLStatus.createErrorStatus("Problem while running JSure", e);
            }
            NotificationHub.notifyAnalysisCompleted();
            return SLStatus.OK_STATUS;
        }  
	}
}
