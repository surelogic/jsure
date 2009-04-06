package edu.cmu.cs.fluid.dc;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDE;

import com.surelogic.common.eclipse.builder.AbstractNature;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.LibResources;
import com.surelogic.jsure.client.eclipse.dialogs.ConfirmPerspectiveSwitch;

/**
 * Management class for the double-checker nature. This class can configure and
 * unconfigure the nature for a project. The double-checker nature controls the
 * double-checker builder which controls assurance analysis. The double-checker
 * plugin manifest mandates that for a project to be allowed to have the
 * double-checker nature it <i>must</i> have a Java nature as well.
 */
public final class Nature extends AbstractNature {

	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	/**
	 * The double-checker builder identifier (<i>must</i> match the plugin
	 * manifest)
	 */
	public static final String DOUBLE_CHECKER_BUILDER_ID = "com.surelogic.jsure.client.eclipse.dcBuilder";

	/**
	 * The double-checker nature identifier (<i>must</i> match the plugin
	 * manifest)
	 */
	public static final String DOUBLE_CHECKER_NATURE_ID = "com.surelogic.jsure.client.eclipse.dcNature";

	private static final String PROMISES_JAR = "promises.jar";
	
	public Nature() {
		super(DOUBLE_CHECKER_BUILDER_ID);
	}
	
	/**
	 * Checks if the double-checker nature is set for a given project.
	 * 
	 * @param project
	 *            the project to check
	 */
	public static boolean hasNature(IProject project) {
		boolean result = false; // assume it doesn't
		try {
			result = project.hasNature(Nature.DOUBLE_CHECKER_NATURE_ID);
		} catch (CoreException e) {
			LOG.log(Level.WARNING,
					"check for double-checker nature on project "
							+ project.getName() + " failed", e);
		}
		return result;
	}

	/**
	 * Adds the double-checker nature to a project.
	 * 
	 * @param project
	 *            the project to add the double-checker nature to
	 * @throws CoreException
	 *             if we are unable to get a {@link IProjectDescription} for the
	 *             project (which is how project natures are managed)
	 */
	public static void addNatureToProject(final IProject project)
			throws CoreException {
		// add our nature to the project if it doesn't already exist
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		boolean hasQualityNature = description
				.hasNature(DOUBLE_CHECKER_NATURE_ID);
		if (!hasQualityNature) {
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = DOUBLE_CHECKER_NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
			finishProjectSetup(project);
		}
	}
	
	private static void finishProjectSetup(final IProject project) {
		final IJavaProject jp = checkForPromisesJar(project);
		SLUIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (jp != null) { // Promises are NOT on the classpath
          final Shell shell = 
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
          
				  try {
            // Build the list of source and output directories
            final Set<IFolder> srcAndOutputDirs = getSourceAndOutputDirs(jp);
            
            // See if the promises jar is already in the project
            final IFile foundJar = findPromisesjar(project, srcAndOutputDirs);

            IFile useJar;
            boolean useExisting = false;
            final int choice;
            if (foundJar == null) {
              final MessageDialog dialog = new MessageDialog(
                  shell, "Add Promises to Project?", null,
                  "The project does not contain the SureLogic promises JAR file.  " +
                  "Would you like to add it to the project and build path?",
                  MessageDialog.QUESTION,
                  new String[] { "Add to Project Root", "Browse...", "No" }, 0);
              useJar = project.getFile(PROMISES_JAR);
              choice = dialog.open();
            } else {
              /* This is sloppy, find a better way to make the foundJar path 
               * relative to the project root.
               */
              final String foundJarPath =
                foundJar.getLocation().toString().substring(
                    project.getLocation().toString().length()+1);
              final MessageDialog dialog = new MessageDialog(
                  shell, "Add Promises to Build Path?", null,
                  "The project contains the SureLogic promises JAR file at \"" +
                  foundJarPath +
                  "\", but it is not on the build path.  " +
                  "Would you like to add it to the build path?",
                  MessageDialog.QUESTION,
                  new String[] { "Yes", "Copy new JAR file to...", "No" }, 0);
              useJar = foundJar;
              choice = dialog.open();
              useExisting = (choice == 0);
            }

            if (choice == 1) { // Choose a location
              final IContainer newLocation = chooseDirectory(shell, project, jp, srcAndOutputDirs);
              if (newLocation != null) {
                useJar = newLocation.getFile(new Path(PROMISES_JAR));
              } else {
                useJar = null; // force a cancel
              }
            }
            
            if (choice != 2 && useJar != null) { // User didn't cancel
              boolean createJar = !useExisting;
              // Ask the user what to do if the file already exists
              if (useJar.exists() && !useExisting) {
                /* This is sloppy, find a better way to make the useJar path 
                 * relative to the project root.
                 */
                final String useJarPath =
                  foundJar.getLocation().toString().substring(
                      project.getLocation().toString().length()+1);
                createJar = MessageDialog.openQuestion(shell,
                    "Overwrite Existing Promises?",
                    "The SureLogic promises JAR file already exists at \"" +
                    useJarPath + "\".  Would you like to overwrite it?");
              }
              if (createJar) {
                // Remove first if already exists
                if (useJar.exists()) {
                  useJar.delete(false, false, null);
                }
                useJar.create(LibResources.getPromisesJar(), false, null);
              }
              
              // Update build path
              final IClasspathEntry[] orig = jp.getRawClasspath();
              List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
              for(IClasspathEntry e : orig) {
                entries.add(e);
              }
              entries.add(JavaCore.newLibraryEntry(useJar.getFullPath(), null, null));
              jp.setRawClasspath(entries.toArray(new IClasspathEntry[orig.length+1]), null);
            }
          } catch (final JavaModelException e) {
            SLLogger.getLogger().log(Level.WARNING,
                "Error while adding promises.jar", e);
            MessageDialog.openError(shell, "Error", e.getMessage());
          } catch (final CoreException e) {
            SLLogger.getLogger().log(Level.WARNING,
                "Error while adding promises.jar", e);
            MessageDialog.openError(shell, "Error", e.getMessage());
          } catch (final IOException e) {
            SLLogger.getLogger().log(Level.WARNING,
                "Error while adding promises.jar", e);
            MessageDialog.openError(shell, "Error", e.getMessage());
          }
				}
				ConfirmPerspectiveSwitch.prototype.submitUIJob();     
				runAnalysis(project);
        return Status.OK_STATUS;
      }

    };
    job.schedule();
  }

	private static Set<IFolder> getSourceAndOutputDirs(IJavaProject javaProject) throws JavaModelException {
    final IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
    final Set<IFolder> sourceAndOutputDirs = new HashSet<IFolder>();
    sourceAndOutputDirs.add(workspace.getFolder(javaProject.getOutputLocation()));
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
        sourceAndOutputDirs.add(workspace.getFolder(entry.getPath()));
      }
      final IPath output = entry.getOutputLocation();
      if (output != null) {
        sourceAndOutputDirs.add(workspace.getFolder(output));
      }
    }
    return sourceAndOutputDirs;
	}
	
	private static boolean isGoodPromiseContainer(
	    final IFolder test, final Set<IFolder> ignore) {
    final boolean isHidden = test.getName().charAt(0) == '.';
    final boolean isSourceOrOutput = ignore.contains(test);
    return !isHidden && !isSourceOrOutput;
	}
	
	/**
	 * Search the given project for a file named "promises.jar".  Does not
	 * search hidden directories, source directories, or output directories.
	 * @return The file handle, or {@code null} if not found.
	 * @throws CoreException 
	 */
	private static IFile findPromisesjar(final IContainer current,
      final Set<IFolder> ignore) throws CoreException {
	  final IResource[] members = current.members();
	  
	  // First scan for promises jar
	  for (final IResource rsrc : members) {
	    if (rsrc instanceof IFile) {
	      if (rsrc.getName().equals(PROMISES_JAR)) {
	        return (IFile) rsrc;
	      }
	    }
	  }
	  
	  // If we don't find it, search the directories
	  for (final IResource rsrc : members) {
	    if (rsrc instanceof IFolder) {
	      if (isGoodPromiseContainer((IFolder) rsrc, ignore)) {
	        final IFile found = findPromisesjar((IFolder) rsrc, ignore);
	        if (found != null) return found;
	      }
	    }
	  }
	  
	  // Didn't find it
	  return null;
	}
	
  private static IContainer chooseDirectory(final Shell shell,
      final IProject project, final IJavaProject javaProject,
      final Set<IFolder> srcAndOutputDirs) {
    /**
     * Class used the root element for the model, used to encapsulate
     * the project element.
     */
    final class BogusRoot {
      public final IProject project;
      
      public BogusRoot(final IProject p) {
        project = p;
      }
    }

    final BogusRoot root = new BogusRoot(project);
    
    final LabelProvider labelProvider = new LabelProvider() {
      private final Image IMG_PROJECT = PlatformUI.getWorkbench()
          .getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
      private final Image IMG_FOLDER = PlatformUI.getWorkbench()
          .getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

      @Override
      public Image getImage(final Object element) {
        if (element instanceof IProject) {
          return IMG_PROJECT;
        } else if (element instanceof IFolder) {
          return IMG_FOLDER;
        }
        return null;
      }

      @Override
      public String getText(Object element) {
        if (element instanceof IResource) {
          return ((IResource) element).getName();
        }
        return "Cannot get name from " + element.getClass() + " element";
      }
    };

    final ITreeContentProvider contentProvider = new ITreeContentProvider() {
      public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof IContainer) {
          final IContainer container = (IContainer) parentElement;
          try {
            final IResource[] members = container.members();
            final List<IFolder> folders = new ArrayList<IFolder>(members.length);
            for (final IResource rsrc : members) {
              if (rsrc instanceof IFolder) {
                if (isGoodPromiseContainer((IFolder) rsrc, srcAndOutputDirs)) {
                  folders.add((IFolder) rsrc);
                }
              }
            }
            IFolder[] foldersArray = new IFolder[folders.size()];
            return folders.toArray(foldersArray);
          } catch (final CoreException e) {
            return null;
          }
        } else {
          return null;
        }
      }

      public Object getParent(final Object element) {
        if (element instanceof IResource) {
          return ((IResource) element).getParent();
        } else {
          return null;
        }
      }

      public boolean hasChildren(final Object element) {
        final Object[] children = getChildren(element);
        return (children != null) && children.length > 0;
      }

      public Object[] getElements(final Object inputElement) {
        return new Object[] { ((BogusRoot) inputElement).project };
      }

      public void dispose() {
        // does nothing
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // does nothing
      }
    };
    
    final ElementTreeSelectionDialog dialog =
      new ElementTreeSelectionDialog(shell,labelProvider, contentProvider);
    dialog.setTitle("Choose a Location");
    dialog.setMessage("Please choose a location for the promises jar file:");
    dialog.setInput(root);
    dialog.setHelpAvailable(false);

    if (dialog.open() == Window.OK) {
      return (IContainer) dialog.getFirstResult();
    }
    return null;
  } 

	
	private static IJavaProject checkForPromisesJar(IProject project) {
		IJavaProject p = JavaCore.create(project);
		try {
			if (p.findType("com.surelogic.RegionLock") == null) {
				// Could add promises.jar
				return p;
			}
		} catch (JavaModelException e) {
			// Ignore any exception
		}
		return null;
	}
	
	public static void runAnalysis(IProject project) {
		// perform initial analysis
		new FirstTimeAnalysis(project).schedule();
	}

	/**
	 * Removes the double-checker nature from a project.
	 * 
	 * @param project
	 *            the project to remove the double-checker nature from
	 * @throws CoreException
	 *             if we are unable to get a {@link IProjectDescription} for the
	 *             project (which is how project natures are managed)
	 */
	static public void removeNatureFromProject(IProject project)
			throws CoreException {
		// remove our nature from the project if it exists
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		boolean hasQualityNature = description
				.hasNature(DOUBLE_CHECKER_NATURE_ID);
		if (hasQualityNature) {
			String[] newNatures = new String[natures.length - 1];
			int newNatureIndex = 0;
			for (int i = 0; i < natures.length; ++i) {
				if (!natures[i].equals(DOUBLE_CHECKER_NATURE_ID)) {
					newNatures[newNatureIndex++] = natures[i];
				}
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}
}