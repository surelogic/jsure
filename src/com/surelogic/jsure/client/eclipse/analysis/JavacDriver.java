package com.surelogic.jsure.client.eclipse.analysis;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.FileUtility;
import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.SLUtility;
import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.SourceZip;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.Util;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;

import edu.cmu.cs.fluid.dc.Majordomo;
import edu.cmu.cs.fluid.dc.NotificationHub;
import edu.cmu.cs.fluid.sea.Sea;
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
	
	public void doBuild(IProject p) {	    
		ProjectDrop.ensureDrop(p.getName(), p);
		final ProjectInfo info = projects.get(p);
		if (info == null) {
			return; // No info!
		}
        JavacEclipse.initialize();
		try {
		    final Config config = info.makeConfig();		    
		    final File dataDir = 
		        //new File(IDE.getInstance().getStringPreference(IDEPreferences.DATA_DIRECTORY));
		        PreferenceConstants.getJSureDataDirectory();
		    final String time = SLUtility.toStringHMS(new Date());
		    final String name = p.getName()+' '+time.replace(':', '-');		   		    
		    final File zips   = new File(dataDir, name+"/zips");
		    final File target = new File(dataDir, name+"/srcs");
		    config.setRun(name);
		    for(JavacRunDrop d : Sea.getDefault().getDropsOfExactType(JavacRunDrop.class)) {
		        d.invalidate();
		    }
		    new JavacRunDrop(config);
		    JSureHistoricalSourceView.setLastRun(config, new ISourceZipFileHandles() {
                public Iterable<File> getSourceZips() {
                    return Arrays.asList(zips.listFiles());
                }		        
		    });
		    
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
            final List<Pair<String, File>> srcFiles = new ArrayList<Pair<String, File>>();
            final List<Pair<String, File>> auxFiles = new ArrayList<Pair<String, File>>();
            for(String proj : config.getProjects()) {
                IProject ip = ResourcesPlugin.getWorkspace().getRoot().getProject(proj);
                try {
                    copySources(proj.equals(config.getProject()) ? srcFiles : auxFiles, ip);
                } catch (IOException e) {
                    return SLStatus.createErrorStatus("Problem while copying sources", e);
                }
            }            
            // TODO projects need separate lists of jars
            final Map<String,String> jarMapping = new HashMap<String,String>();
            for(String jar : config.getJars()) {
                final String name;
                final int lastSlash = jar.lastIndexOf(File.separatorChar);
                if (lastSlash < 0) {
                    name = jar;
                } else {
                    name = jar.substring(lastSlash+1);
                }
                File target = new File(targetDir, name);
                FileUtility.copy(new File(jar), new File(targetDir, name));
                //System.out.println("Copying "+new File(jar)+" to "+new File(targetDir, name));               
                jarMapping.put(jar, target.getAbsolutePath());
            }
            config.setFiles(srcFiles, false);
            config.setFiles(auxFiles, true);
            config.relocateJars(jarMapping);            
            
            if (afterJob != null) {
                EclipseJob.getInstance().schedule(afterJob);
            }
            return SLStatus.OK_STATUS;
        }
            
        boolean copySources(List<Pair<String, File>> srcFiles, IProject project) throws IOException {
            final SourceZip srcZip = new SourceZip(project);
            File zipFile           = new File(zipDir, project.getName()+".zip");
            if (!zipFile.exists()) {
                zipFile.getParentFile().mkdirs();
                srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
            }

            targetDir.mkdir();
            File projectDir = new File(targetDir, project.getName());
            ZipFile zf = new ZipFile(zipFile);
            
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
            
            return true;
        }	    
	}
	
	class AnalysisJob extends Job {
        AnalysisJob(Config config, File target) {
            super("Running JSure on "+config.getProject(), config, target);
        }

        public SLStatus run(SLProgressMonitor monitor) {
            JavacEclipse.initialize();
            NotificationHub.notifyAnalysisStarting();
            try {
                Util.openFiles(config, monitor);
            } catch (Exception e) {
                NotificationHub.notifyAnalysisPostponed();
                return SLStatus.createErrorStatus("Problem while running JSure", e);
            }
            NotificationHub.notifyAnalysisCompleted();
            return SLStatus.OK_STATUS;
        }
	    
	}
}
