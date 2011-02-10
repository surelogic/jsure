package com.surelogic.jsure.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.RegionLock;
import com.surelogic.common.LibResources;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.JDTUtility.IPathFilter;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;

/**
 * A utility of non-UI JSure-specific methods.
 */
public final class JSureUtility {

	/**
	 * Constructs an Eclipse workspace job to add or update the SureLogic
	 * promises.jar file in the passed Eclipse Java project.
	 * 
	 * @param jp
	 *            an Eclipse Java Project
	 * @param jpName
	 *            the name of the passed Eclipse Java project.
	 * @return an Eclipse workspace job.
	 */
	public static WorkspaceJob getJobToAddUpdatePromisesJar(
			final IJavaProject jp, final String jpName) {
		final WorkspaceJob wJob = new WorkspaceJob(
				"Add/Update SureLogic Promises Jar") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				try {
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
						final List<IPath> promisesJarsOnClasspath = findPromisesJarsOnClasspath(jp);
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
								System.out.println("*** new file location "
										+ jarFile);
								break;
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
								System.out.println("***" + path);
								IPath newVersionPath = path.removeLastSegments(
										1).append(LibResources.PROMISES_JAR);
								final IWorkspaceRoot root = ResourcesPlugin
										.getWorkspace().getRoot();
								IFile pathFile = root.getFile(newVersionPath);
								if (pathFile != null) {
									jarFile = pathFile;
								}
								System.out.println("constructing path from "
										+ path + " " + newVersionPath
										+ " file " + pathFile);
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
		return wJob;
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
	public static boolean checkForRegionLockPromiseOnClasspathOf(IJavaProject p) {
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
	 *            a file that may or may not exist in the project. If it exists
	 *            it will be overwritten.
	 * @throws CoreException
	 *             if things go wrong interacting with the Java project.
	 * @throws IOException
	 *             if the promises.jar bits can't be read from
	 *             {@link LibResources#getPromisesJar()}.
	 */
	public static void addPromisesJarAndAddToClasspath(final IJavaProject jp,
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

	/**
	 * Matches all known versions of the promises.jar on a path.
	 */
	static class PromisesJarMatcher extends IPathFilter {
		final List<IPath> results = new ArrayList<IPath>();

		@Override
		public boolean stopAfterMatch() {
			return false; // Check the whole classpath
		}

		@Override
		public boolean match(IPath path) {
			// Check if path is an older version of the promises
			for (String name : LibResources.PROMISES_JAR_OLD_VERSIONS) {
				if (name.equals(path.lastSegment())) {
					results.add(path);
					return true;
				}
			}
			// Check if path is the current version of the promises
			if (LibResources.PROMISES_JAR.equals(path.lastSegment())) {
				results.add(path);
				return true;
			}
			return false;
		}
	}

	/**
	 * Checks if the current promises.jar, as specified by
	 * {@link LibResources#PROMISES_JAR}, is on the Eclipse Java project's
	 * classpath.
	 * 
	 * @return {@code true} if the current promises.jar is on the classpath,
	 *         {@code false} otherwise.
	 */
	public static boolean isPromisesJarOnClasspath(IJavaProject jp) {
		return JDTUtility.isOnClasspath(jp, new IPathFilter() {
			@Override
			public boolean match(IPath path) {
				return LibResources.PROMISES_JAR.equals(path.lastSegment());
			}
		});
	}

	/**
	 * Gets all the promise.jars on the classpath of the given project. The
	 * older versions are determined by the
	 * {@link LibResources#PROMISES_JAR_OLD_VERSIONS} array. The current version
	 * is determined by {@link LibResources#PROMISES_JAR}.
	 * 
	 * @param jp
	 *            an Eclipse Java project.
	 * @return a (possibly empty) list of promise.jar files.
	 */
	public static List<IPath> findPromisesJarsOnClasspath(IJavaProject jp) {
		PromisesJarMatcher matcher = new PromisesJarMatcher();
		JDTUtility.isOnClasspath(jp, matcher);
		return matcher.results;
	}

	private JSureUtility() {
		// utility
	}
}
