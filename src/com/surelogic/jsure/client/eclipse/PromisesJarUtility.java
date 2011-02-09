package com.surelogic.jsure.client.eclipse;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;

import com.surelogic.RegionLock;
import com.surelogic.common.LibResources;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;

public class PromisesJarUtility {

	/**
	 * Constructs an {@link SLUIJob} that, when executed, interacts with the
	 * user to add, if necessary, the promises.jar file into a particular
	 * Eclipse Java project.
	 * <p>
	 * If the passed project is not a Java project then the job exits
	 * immediately.
	 * <p>
	 * A design goal of this job is to avoid user interaction as much as
	 * possible. The user has already confirmed the addition of the promises.jar
	 * so we don't need to bug them about it. Also we assume that older versions
	 * of our library are, in fact, our library so we can delete them from the
	 * project.
	 * 
	 * @param project
	 *            an Eclipse project.
	 */
	public static SLUIJob getAddUpdatePromisesLibraryUIJob(
			final IProject project) {
		return new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				String jpName = "unknown-project-name";
				try {
					final IJavaProject jp = JavaCore.create(project);
					if (jp == null)
						return Status.OK_STATUS; // not a Java project

					jpName = project.getName();
					final String javaSourceVersion = JDTUtility
							.getJavaSourceVersion(jp);
					final int majorJavaSourceVersion = JDTUtility
							.getMajorJavaSourceVersion(jp);

					if (majorJavaSourceVersion < 5) {
						/*
						 * Notify the user that the source level of their
						 * project does not allow use of the promises.jar
						 */
						MessageDialog
								.openInformation(
										EclipseUIUtility.getShell(),
										I18N.msg(
												"jsure.eclipse.dialog.promises.noPromisesJarNeeded.title",
												jpName),
										I18N.msg(
												"jsure.eclipse.dialog.promises.noPromisesJarNeeded.msg",
												jpName, javaSourceVersion));
						return Status.OK_STATUS;
					}

					/*
					 * Set a default location for the added/updated promises
					 * library.
					 */
					IFile jarFile = jp.getProject().getFile(
							LibResources.PROMISES_JAR);

					final boolean foundRegionLockPromisesOnClasspath = checkForRegionLockPromiseOnClasspathOf(jp);
					if (foundRegionLockPromisesOnClasspath) {
						/*
						 * RegionLock is on the classpath of the Eclipse Java
						 * project we are examining.
						 */
						final List<IPath> promisesJarsOnClasspath = JDTUtility
								.findPromisesJarsOnClasspath(jp);
						for (IPath path : promisesJarsOnClasspath) {
							if (LibResources.PROMISES_JAR.equals(path
									.lastSegment())) {
								/*
								 * We found the current library. We will use
								 * this location (which might not be at the root
								 * of the project) where we will freshen the
								 * library.
								 * 
								 * We go ahead and copy the library bits even
								 * over an existing current version because this
								 * helps SureLogic engineers during development
								 * when the library contents are updated but the
								 * version is not.
								 */
								jarFile = jp.getProject().getFile(path);
								;
							} else {
								/*
								 * We found an older version of the promises.jar
								 * file. We'll remove it from the classpath and
								 * delete it.
								 * 
								 * We also want to use its containing folder as
								 * the location to place the new version of our
								 * library. This is because the user went to the
								 * trouble of using this location so we'll
								 * respect that choice.
								 * 
								 * Sadly, the below code only seems to work if
								 * the Jar was within the workspace. If not it
								 * will put the new Jar at the root of the
								 * project.
								 */
								JDTUtility.removeJarFromClasspath(jp, path);
								System.out.println(path);
								IPath newVersionPath = path.removeLastSegments(
										1).append(LibResources.PROMISES_JAR);
								final IWorkspaceRoot root = ResourcesPlugin
										.getWorkspace().getRoot();
								IFile pathFile = root.getFile(newVersionPath);
								if (pathFile != null) {
									jarFile = pathFile;
								}
							}
						}
					}
					addPromisesJarAndAddToClasspath(jp, jarFile);
				} catch (final Exception e) {
					final int code = 221;
					return SLEclipseStatusUtility.createErrorStatus(code,
							I18N.err(code, LibResources.PROMISES_JAR, jpName),
							e);
				}
				return Status.OK_STATUS;
			}
		};
	}

	/**
	 * Checks if the {@link RegionLock} annotation class is on the project's
	 * classpath. If it is then {@code true} is returned.
	 * <p>
	 * This method is used to check for existence of the JSure Promises Library
	 * within a particular project.
	 * 
	 * @param p
	 *            a Eclipse Java project.
	 * 
	 * @return {@code} true if the annotation is on the project's classpath,
	 *         {@code false} otherwise.
	 */
	private static boolean checkForRegionLockPromiseOnClasspathOf(IJavaProject p) {
		try {
			return p.findType("com.surelogic.RegionLock") != null;
		} catch (JavaModelException e) {
			// Ignore any exception
		}
		return false;
	}

	/**
	 * Copies the contents of the promises jar from
	 * {@link LibResources#getPromisesJar()} into the passed file location.
	 * <p>
	 * If <tt>jarFile</tt> exists then it is overwritten.
	 * 
	 * @param jp
	 *            an Eclipse Java project.
	 * @param jarFile
	 *            a file that may or may not exist in the project.
	 * @throws CoreException
	 *             if things go wrong interacting with the Java project.
	 * @throws IOException
	 *             if the promises.jar bits can't be read from
	 *             {@link LibResources#getPromisesJar()}.
	 */
	private static void addPromisesJarAndAddToClasspath(final IJavaProject jp,
			final IFile jarFile) throws CoreException, IOException {

		// Make sure Eclipse is up-to-date with the OS
		jarFile.refreshLocal(0, null);

		// Create or update the file
		if (jarFile.exists()) {
			jarFile.delete(false, false, null);
		}
		jarFile.create(LibResources.getPromisesJar(), false, null);

		// Add the promises.jar to the project's classpath
		if (!JDTUtility.isOnClasspath(jp, jarFile)) {
			JDTUtility.addJarToClasspath(jp, jarFile);
		}
	}
}
