package com.surelogic.jsure.client.eclipse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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

import com.surelogic.common.LibResources;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.dialogs.ConfirmPerspectiveSwitch;

public class PromisesJarUtility {

	/**
	 * Primarily adds the promises.jar if desired
	 */
	public static void finishProjectSetup(final IProject project,
			final boolean onlyAddJar) {
		final IJavaProject jp = JavaCore.create(project);
		final String jpName = project.getName();
		final String javaSourceVersion = JDTUtility.getJavaSourceVersion(jp);
		final int majorJavaSourceVersion = JDTUtility
				.getMajorJavaSourceVersion(jp);
		if (majorJavaSourceVersion < 5) {
			if (onlyAddJar) {
				MessageDialog
						.openInformation(
								EclipseUIUtility.getShell(),
								I18N.msg(
										"jsure.eclipse.dialog.promises.noPromisesJarNeeded.title",
										jpName),
								I18N.msg(
										"jsure.eclipse.dialog.promises.noPromisesJarNeeded.msg",
										jpName, javaSourceVersion));
			}
			return;
		}
		final boolean foundRegionLock = checkForPromises(jp);
		SLUIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				final Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				try {
					if (!foundRegionLock) { // Promises are NOT available
						// 1. No jar on classpath
						// 2. Jar on classpath, but not longer in project
						// 3. Jar in project, but not on classpath
						LocationChoice lc = askForPromisesJarLocation(shell, jp);
						if (lc.choice != 2 && lc.useJar != null) {
							// User didn't cancel
							setupPromisesJar(jp, shell, lc);
						}
					} else {
						// Promises are available, but check if I need to
						// upgrade the jar
						final List<IPath> old = findOldJarsOnClasspath(jp);
						if (old.isEmpty()) {
							if (onlyAddJar) {
								MessageDialog
										.openInformation(
												shell,
												"Using the Latest SureLogic Promises Library",
												"The latest SureLogic promises library is already on the build path for this project.");
							}
						} else {
							// Check about removing the old jars
							final boolean hasCurrentJar = isJarOnClasspath(jp);
							final StringBuilder sb = new StringBuilder();
							if (hasCurrentJar) {
								// We've got the latest, but also older jars
								sb.append("The latest SureLogic promises library is on the build path, ");
								sb.append("but there are also older SureLogic promises libraries on the build path:\n\n");
							} else {
								// We only have older jars
								sb.append("The project does not contain the latest SureLogic promises library on its build path, ");
								sb.append("but there are older SureLogic promises libraries on the build path:\n\n");
							}
							for (IPath p : old) {
								sb.append('\t').append(p.toString())
										.append('\n');
							}
							sb.append("\nWould you like to remove the older libraries from the project's build path?");
							sb.append("\n\nThis action will not delete the older libraries from your disk (you can do that manually).");

							final MessageDialog dialog = new MessageDialog(
									shell,
									"Remove Old Promises Libraries from your Build Path?",
									null, sb.toString(),
									MessageDialog.QUESTION, new String[] {
											"Yes", "No" }, 0);
							final int removeChoice = dialog.open();
							if (removeChoice == 0) {
								try {
									removeOldFromClasspath(jp);
								} catch (JavaModelException e) {
									handleError(
											shell,
											" while removing jars from build path",
											e);
								}
							}
							if (!hasCurrentJar) {
								final LocationChoice lc = askForPromisesJarLocation(
										shell, jp);
								if (lc.useJar != null) {
									setupPromisesJar(jp, shell, lc);
								}
							}
						}
					}
				} catch (final CoreException e) {
					handleError(shell, " while adding "
							+ LibResources.PROMISES_JAR, e);
				} catch (final IOException e) {
					handleError(shell, " while adding "
							+ LibResources.PROMISES_JAR, e);
				}
				if (!onlyAddJar) {
					ConfirmPerspectiveSwitch.prototype.submitUIJob();
					// Already started by nature change
					// Nature.runAnalysis(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	static class LocationChoice {
		final int choice;
		final IFile useJar;
		final boolean useExisting;

		LocationChoice(int choice, IFile loc, boolean useExisting) {
			this.useExisting = useExisting;
			this.choice = choice;
			useJar = loc;
		}
	}

	static void handleError(Shell shell, String whileMsg, Exception e) {
		SLLogger.getLogger().log(Level.WARNING,
				e.getClass().getSimpleName() + whileMsg, e);
		MessageDialog.openError(shell, "Error", e.getMessage());
	}

	/*
	 * This is sloppy, find a better way to make the foundJar path relative to
	 * the project root.
	 */
	static String computeRelativePath(IFile f, IContainer c) {
		return computeRelativePath(f.getLocation(), c);
	}

	static List<IPath> findOldJarsOnClasspath(IJavaProject jp) {
		OldJarMatcher f = new OldJarMatcher();
		isOnClasspath(jp, f);
		return f.results;
	}

	/**
	 * @return true if found a promise
	 */
	static boolean checkForPromises(IJavaProject p) {
		try {
			if (p.findType("com.surelogic.RegionLock") == null) {
				// Could add promises.jar
				return false;
			} else {
				return true;
			}
		} catch (JavaModelException e) {
			// Ignore any exception
		}
		return true;
	}

	static LocationChoice askForPromisesJarLocation(Shell shell, IJavaProject jp)
			throws CoreException {
		// Build the list of source and output directories
		final Set<IContainer> srcAndOutputDirs = getSourceAndOutputDirs(jp);

		// See if the promises jar is already in the project
		final IFile foundJar = findPromisesjar(jp.getProject(),
				srcAndOutputDirs);

		IFile useJar;
		final int choice;
		boolean useExisting = false;
		if (foundJar == null) {
			final MessageDialog dialog = new MessageDialog(
					shell,
					"Add Promises to Project?",
					null,
					"The project does not contain the latest SureLogic promises JAR file.  "
							+ "Would you like to add it to the project and build path?",
					MessageDialog.QUESTION, new String[] {
							"Add to Project Root", "Browse...", "No" }, 0);
			useJar = jp.getProject().getFile(LibResources.PROMISES_JAR);
			choice = dialog.open();
		} else {
			final String foundJarPath = computeRelativePath(foundJar,
					jp.getProject());
			final MessageDialog dialog = new MessageDialog(shell,
					"Add Promises to Build Path?", null,
					"The project contains the latest SureLogic promises JAR file at \""
							+ foundJarPath
							+ "\", but it is not on the build path.  "
							+ "Would you like to add it to the build path?",
					MessageDialog.QUESTION, new String[] { "Yes",
							"Copy new JAR file to...", "No" }, 0);
			useJar = foundJar;
			choice = dialog.open();
			useExisting = (choice == 0);
		}

		if (choice == 1) { // Choose a location
			final IContainer newLocation = chooseDirectory(shell,
					jp.getProject(), jp, srcAndOutputDirs);
			if (newLocation != null) {
				useJar = newLocation
						.getFile(new Path(LibResources.PROMISES_JAR));
			} else {
				useJar = null; // force a cancel
			}
		} else if (choice == 2) {
			useJar = null; // Don't use the jar
		}
		return new LocationChoice(choice, useJar, useExisting);
	}

	private static void setupPromisesJar(final IJavaProject jp,
			final Shell shell, LocationChoice lc) throws CoreException,
			IOException, JavaModelException {
		boolean createJar = !lc.useExisting;

		// Make sure Eclipse is up-to-date with the OS
		lc.useJar.refreshLocal(0, null);

		// Ask the user what to do if the file already
		// exists
		if (lc.useJar.exists() && !lc.useExisting) {
			final String useJarPath = computeRelativePath(lc.useJar,
					jp.getProject());
			createJar = MessageDialog.openQuestion(shell,
					"Overwrite Existing Promises?",
					"The SureLogic promises JAR file already exists at \""
							+ useJarPath
							+ "\".  Would you like to overwrite it?");
		}
		if (createJar) {
			// Remove first if already exists
			if (lc.useJar.exists()) {
				lc.useJar.delete(false, false, null);
			}
			lc.useJar.create(LibResources.getPromisesJar(), false, null);
		}

		if (!isOnClasspath(jp, lc.useJar)) {
			addToClasspath(jp, lc.useJar);
		} else {
			// useJar is already listed on the classpath
		}
	}

	static boolean isOnClasspath(IJavaProject jp, final IFile useJar) {
		return isOnClasspath(jp, new IPathFilter() {
			boolean match(IPath path) {
				// System.out.println("Comparing "+useJar+" with "+path);
				return useJar.getFullPath().equals(path);
			}
		});
	}

	static boolean isOnClasspath(IJavaProject jp, IPathFilter matcher) {
		boolean rv = false;
		try {
			for (IClasspathEntry e : jp.getRawClasspath()) {
				if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					if (matcher.match(e.getPath())) {
						if (matcher.stopAfterMatch()) {
							return true;
						} else {
							rv = true;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			return true; // FIX?
		}
		return rv;
	}

	/**
	 * @return true if it looks like the current jar is on the classpath
	 */
	static boolean isJarOnClasspath(IJavaProject jp) {
		return isOnClasspath(jp, new IPathFilter() {
			boolean match(IPath path) {
				return LibResources.PROMISES_JAR.equals(path.lastSegment());
			}
		});
	}

	static class OldJarMatcher extends IPathFilter {
		final List<IPath> results = new ArrayList<IPath>();

		boolean stopAfterMatch() {
			return false; // Check the whole classpath
		}

		@Override
		boolean match(IPath path) {
			for (String name : LibResources.PROMISES_JAR_OLD_VERSIONS) {
				if (name.equals(path.lastSegment())) {
					results.add(path);
					return true;
				}
			}
			return false;
		}
	}

	static String computeRelativePath(IPath p, IContainer c) {
		return p.toString().substring(c.getLocation().toString().length() + 1);
	}

	static void addToClasspath(final IJavaProject jp, IFile useJar)
			throws JavaModelException {
		final IClasspathEntry[] orig = jp.getRawClasspath();
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		entries.add(JavaCore.newLibraryEntry(useJar.getFullPath(), null, null,
				new IAccessRule[0], new IClasspathAttribute[0], false));

		for (IClasspathEntry e : orig) {
			entries.add(e);
		}
		jp.setRawClasspath(
				entries.toArray(new IClasspathEntry[entries.size()]), null);
	}

	static void removeOldFromClasspath(final IJavaProject jp)
			throws JavaModelException {
		final IClasspathEntry[] orig = jp.getRawClasspath();
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		IPathFilter f = new OldJarMatcher();
		for (IClasspathEntry e : orig) {
			if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY
					&& f.match(e.getPath())) {
				continue;
			}
			entries.add(e);
		}
		jp.setRawClasspath(
				entries.toArray(new IClasspathEntry[entries.size()]), null);
	}

	static abstract class IPathFilter {
		abstract boolean match(IPath path);

		boolean stopAfterMatch() {
			return true;
		}
	}

	static Set<IContainer> getSourceAndOutputDirs(IJavaProject javaProject)
			throws JavaModelException {
		final Set<IContainer> sourceAndOutputDirs = new HashSet<IContainer>();
		sourceAndOutputDirs.add(getWorkspaceFolder(javaProject.getProject(),
				javaProject.getOutputLocation()));
		for (IClasspathEntry entry : javaProject.getRawClasspath()) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				sourceAndOutputDirs.add(getWorkspaceFolder(
						javaProject.getProject(), entry.getPath()));
			}
			final IPath output = entry.getOutputLocation();
			if (output != null) {
				sourceAndOutputDirs.add(getWorkspaceFolder(
						javaProject.getProject(), output));
			}
		}
		return sourceAndOutputDirs;
	}

	static IContainer getWorkspaceFolder(IProject proj, IPath path) {
		final IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace()
				.getRoot();
		return workspace.getContainerForLocation(path);
	}

	static boolean isGoodPromiseContainer(final IContainer test,
			final Set<IContainer> ignore) {
		final boolean isHidden = test.getName().charAt(0) == '.';
		final boolean isSourceOrOutput = ignore.contains(test);
		return !isHidden && !isSourceOrOutput;
	}

	/**
	 * Search the given project for a file named LibResources.PROMISES_JAR. Does
	 * not search hidden directories, source directories, or output directories.
	 * 
	 * @return The file handle, or {@code null} if not found.
	 * @throws CoreException
	 */
	static IFile findPromisesjar(final IContainer current,
			final Set<IContainer> ignore) throws CoreException {
		final IResource[] members = current.members();

		// First scan for promises jar
		for (final IResource rsrc : members) {
			if (rsrc instanceof IFile) {
				if (rsrc.getName().equals(LibResources.PROMISES_JAR)) {
					return (IFile) rsrc;
				}
			}
		}

		// If we don't find it, search the directories
		for (final IResource rsrc : members) {
			if (rsrc instanceof IContainer) {
				if (isGoodPromiseContainer((IContainer) rsrc, ignore)) {
					final IFile found = findPromisesjar((IFolder) rsrc, ignore);
					if (found != null)
						return found;
				}
			}
		}

		// Didn't find it
		return null;
	}

	static IContainer chooseDirectory(final Shell shell,
			final IProject project, final IJavaProject javaProject,
			final Set<IContainer> srcAndOutputDirs) {
		/**
		 * Class used the root element for the model, used to encapsulate the
		 * project element.
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
					.getSharedImages()
					.getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
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
				return "Cannot get name from " + element.getClass()
						+ " element";
			}
		};

		final ITreeContentProvider contentProvider = new ITreeContentProvider() {
			public Object[] getChildren(final Object parentElement) {
				if (parentElement instanceof IContainer) {
					final IContainer container = (IContainer) parentElement;
					try {
						final IResource[] members = container.members();
						final List<IFolder> folders = new ArrayList<IFolder>(
								members.length);
						for (final IResource rsrc : members) {
							if (rsrc instanceof IFolder) {
								if (isGoodPromiseContainer((IFolder) rsrc,
										srcAndOutputDirs)) {
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

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// does nothing
			}
		};

		final ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				shell, labelProvider, contentProvider);
		dialog.setTitle("Choose a Location");
		dialog.setMessage("Please choose a location for the promises jar file:");
		dialog.setInput(root);
		dialog.setHelpAvailable(false);

		if (dialog.open() == Window.OK) {
			return (IContainer) dialog.getFirstResult();
		}
		return null;
	}
}
