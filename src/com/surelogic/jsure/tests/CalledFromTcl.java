/*
 * Created on May 4, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.jsure.tests;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;

import edu.cmu.cs.fluid.util.*;

public class CalledFromTcl {
  private static final boolean createFiles = true;
  
  private static IProject scratchProject;
  private static IJavaProject scratchJProject;
  private static IPackageFragmentRoot scratchPackageRoot;
  private static IPackageFragment defaultPackage;
  
  private static final IProgressMonitor mon = new NullProgressMonitor();
  
  private static boolean isInitialized() {
    return (scratchJProject != null && scratchPackageRoot.exists() &&
            defaultPackage.exists());
  }
  
  public static synchronized boolean ensureScratchProjectExists() throws CoreException {
    if (isInitialized()) {
      return true;
    }
    IWorkspaceRoot root  = ResourcesPlugin.getWorkspace().getRoot();    
    UniqueID id          = new UniqueID();
    IProject project;
    do {
      String name = "scratch_"+id;
      project     = root.getProject(name);    
    } while (project.exists());

    // Create a basic project
    project.create(mon);
    project.open(mon);

    // Make it a Java project
    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    String[] newNatures = new String[natures.length + 1];
    System.arraycopy(natures, 0, newNatures, 0, natures.length);
    newNatures[natures.length] = JavaCore.NATURE_ID;
    description.setNatureIds(newNatures);
    project.setDescription(description, mon);

    // Make src/bin folders
    IFolder srcFolder = project.getFolder("src");
    IFolder binFolder = project.getFolder("bin");
    srcFolder.create(true, true, mon);
    binFolder.create(true, true, mon);

    // Set up classpath entries
    IJavaProject javaProject    = JavaCore.create(project);
    IClasspathEntry[] buildPath = {
        JavaCore.newSourceEntry(project.getFullPath().append("src")),
        JavaRuntime.getDefaultJREContainerEntry()
    };
    javaProject.setRawClasspath(buildPath, project.getFullPath().append("bin"), mon);
    scratchProject      = project;
    scratchJProject     = javaProject;
    scratchPackageRoot  = javaProject.getPackageFragmentRoot(srcFolder);
    defaultPackage      = scratchPackageRoot.createPackageFragment("", true, mon);
    return isInitialized();
  }
  
  private static IPackageFragmentRoot addToClassPath(String name) throws CoreException {
    IFolder folder = scratchProject.getFolder(name);
    folder.create(true, true, mon);
    
    List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
    // Should be initialized to use the root of the project as a source folder
    entries.addAll(Arrays.asList(scratchJProject.getRawClasspath()));
    entries.add(0, JavaCore.newSourceEntry(scratchProject.getFullPath().append(name)));
    scratchJProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), 
                                    scratchProject.getFullPath().append("bin"), mon);  
    return scratchJProject.getPackageFragmentRoot(folder);
  }
  
  private static List<ICompilationUnit> recentlyCreated = new ArrayList<ICompilationUnit>();
  
  public static void saveas(String name, String contents) throws CoreException {
    try {
      if (!name.endsWith(".java")) {
        name = name + ".java"; 
      }      
      ensureScratchProjectExists();
      System.out.println("Saving '"+contents+"' as "+name);      
      int firstSlash = name.indexOf('/');
      ICompilationUnit cu;
      if (firstSlash < 0) {        
        // No leading folders
        cu = createFiles ? defaultPackage.createCompilationUnit(name, contents, true, mon) : null;
      } else if (name.startsWith("T")) {
        // Put in a custom src folder
        String src                = name.substring(0, firstSlash);
        IPackageFragmentRoot root = addToClassPath(src);
        cu = createCompUnit(root, name.substring(firstSlash+1), contents);
      } else {
        cu = createCompUnit(scratchPackageRoot, name, contents);
      }
      if (cu != null) {
        recentlyCreated.add(cu);
      }
    } catch(CoreException e) {
      e.printStackTrace();
      throw e;
    }
  }

  private static ICompilationUnit createCompUnit(IPackageFragmentRoot root, String name, String contents) throws JavaModelException {
    int lastSlash         = name.lastIndexOf('/');
    String pkg            = name.substring(0, lastSlash).replace('/', '.');
    String file           = name.substring(lastSlash+1);
    IPackageFragment frag = root.createPackageFragment(pkg, true, mon);
    return createFiles ? frag.createCompilationUnit(file, contents, true, mon) : null;
  }
  
  public static void compile(String args) throws CoreException {
    try {
      ensureScratchProjectExists();
      System.out.println("Compiling "+args);
      if (createFiles) {
        scratchProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, mon);
        checkRecentlyCreated();
      } else {
        StringTokenizer st = new StringTokenizer(args);
        while (st.hasMoreTokens()) {
          String arg = st.nextToken();
          if (arg.equals("-pedantic") || arg.equals("-classpath") || 
              arg.startsWith("T") || arg.equals("-g") || arg.equals(".")) {
            continue;
          }
          if (!arg.endsWith(".java")) {
            System.out.println("not Java");
          }     
        }
      }
    } catch(CoreException e) {
      e.printStackTrace();
      throw e;
    }
  }  

  public static void delete(String args) throws CoreException {
    try {
      ensureScratchProjectExists();
      System.out.println("Deleting "+args);

      StringTokenizer st = new StringTokenizer(args);
      while (st.hasMoreTokens()) {
        String arg          = st.nextToken();
        ICompilationUnit cu = defaultPackage.getCompilationUnit(arg);
        if (createFiles && cu != null) {
          cu.delete(true, mon);
        }
      }    
    } catch(CoreException e) {
      e.printStackTrace();
      throw e;
    }
  }  
  
  private static void checkRecentlyCreated() {
    List<ICompilationUnit> l = new ArrayList<ICompilationUnit>(recentlyCreated);
    recentlyCreated.clear();
    
    for(ICompilationUnit cu : l) {
      try {
        IMarker[] markers = cu.getCorrespondingResource().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
        for(IMarker m : markers) {
          int severity = m.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
          if (severity == IMarker.SEVERITY_ERROR) {
            System.out.println("Deleting due to errors: "+cu.getElementName());
            cu.delete(true, mon);
            break;
          }
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
  }  
}
