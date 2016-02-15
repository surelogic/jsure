package com.surelogic.jsure.core;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.Nullable;
import com.surelogic.RegionLock;
import com.surelogic.common.FileUtility;
import com.surelogic.common.LibResources;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.JDTUtility.IPathFilter;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.java.persistence.JSureScanInfo;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;

/**
 * A utility of non-UI JSure-specific methods.
 */
public final class JSureUtility {

  /**
   * Gets a count of modeling problems after passing each through the
   * {@link UninterestingPackageFilterUtility#keep(com.surelogic.dropsea.IDrop)}
   * filter.
   * 
   * @param scan
   *          a JSure scan that may have modeling problems.
   * @return the number of interesting modeling problems.
   */
  public static int getInterestingModelingProblemCount(@Nullable final JSureScanInfo scan) {
    int result = 0;
    if (scan != null) {
      for (IModelingProblemDrop problem : scan.getModelingProblemDrops()) {
        /*
         * We filter results based upon the resource.
         */
        if (UninterestingPackageFilterUtility.keep(problem))
          result++;
      }
    }
    return result;
  }

  /**
   * Constructs an Eclipse workspace job to add or update the SureLogic
   * promises.jar file in the passed Eclipse Java project.
   * 
   * @param jp
   *          an Eclipse Java Project
   * @param jpName
   *          the name of the passed Eclipse Java project.
   * @param java8Source
   *          {@code true} indicates a Java 8 (or above) source project,
   *          {@code false} indicates prior to Java 8.
   * @return an Eclipse workspace job.
   */
  public static WorkspaceJob getJobToAddUpdatePromisesJar(final IJavaProject jp, final String jpName, final boolean java8Source) {
    final WorkspaceJob wJob = new WorkspaceJob("Add/Update SureLogic Promises Jar") {

      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        final String jarName = java8Source ? LibResources.PROMISES8_JAR : LibResources.PROMISES_JAR;
        try {
          final InputStream jarStream = java8Source ? LibResources.getPromises8Jar() : LibResources.getPromisesJar();
          /*
           * State holding the workspace or non-workspace proposed locations of
           * the new/updated promises.jar file.
           */
          IFile wsFile = null;
          boolean wsFileIsCurrentVersion = false;
          File fsFile = null;
          boolean fsFileIsCurrentVersion = false;

          final List<IPath> promisesJarsOnClasspath = findPromisesJarsOnClasspath(jp);
          for (IPath path : promisesJarsOnClasspath) {
            final boolean isCurrentVersion = jarName.equals(path.lastSegment());
            /*
             * Unless the promises.jar is the current version we remove it from
             * the project's classpath.
             */
            if (!isCurrentVersion) {
              JDTUtility.removeFromClasspath(jp, path);
            }

            /*
             * Check if this path is within the workspace by attempting to get
             * an IFile version of it an checking if it exists.
             * 
             * If it doesn't then it is an absolute path on the file system.
             */
            IFile fPath = EclipseUtility.getWorkspaceRoot().getFile(path);
            if (fPath.exists()) {
              /*
               * The Jar is in the workspace
               */
              if (isCurrentVersion) {
                /*
                 * We found the current library in the Eclipse workspace. We go
                 * ahead and copy the library bits even over an existing current
                 * version because this helps SureLogic engineers during
                 * development when the library contents are updated but the
                 * version is not.
                 */
                wsFile = fPath;
                wsFileIsCurrentVersion = true;
              } else {
                /*
                 * We found an older version of the promises.jar file in the
                 * Eclipse workspace.
                 * 
                 * Unless we already found the current version of the
                 * promises.jar, we want to use its containing folder as the
                 * location to place the new version of our library. This is
                 * because the user went to the trouble of using this location
                 * so we'll respect that choice.
                 */
                if (!wsFileIsCurrentVersion) {
                  final IContainer parent = fPath.getParent();
                  if (parent != null) {
                    IFile newFileSamePlace = parent.getFile(new Path(jarName));
                    if (newFileSamePlace != null)
                      wsFile = newFileSamePlace;
                  }
                }
              }
            } else {
              /*
               * The Jar is outside the workspace
               */
              File fsPath = path.toFile();
              if (fsPath.isFile()) {
                if (isCurrentVersion) {
                  /*
                   * We found the current library outside the Eclipse workspace.
                   * We go ahead and copy the library bits even over an existing
                   * current version because this helps SureLogic engineers
                   * during development when the library contents are updated
                   * but the version is not.
                   */
                  fsFile = fsPath;
                  fsFileIsCurrentVersion = true;
                } else {
                  /*
                   * We found an older version of the promises.jar outside the
                   * Eclipse workspace.
                   * 
                   * Unless we already found the current version of the
                   * promises.jar, we want to use its containing folder as the
                   * location to place the new version of our library. This is
                   * because the user went to the trouble of using this location
                   * so we'll respect that choice.
                   */
                  if (!fsFileIsCurrentVersion) {
                    File parent = fsPath.getParentFile();
                    if (parent != null) {
                      File newFileSamePlace = new File(parent, jarName);
                      fsFile = newFileSamePlace;
                    }
                  }
                }
              } else {
                SLLogger.getLogger().warning(I18N.err(222, fsPath.toString(), jpName));
              }
            }
          }

          /*
           * Now go ahead and add/update the promises.jar
           */
          if ((wsFile != null && wsFileIsCurrentVersion) || (wsFile != null && !fsFileIsCurrentVersion)) {
            EclipseUtility.copy(jarStream, wsFile);
            if (!wsFileIsCurrentVersion) {
              JDTUtility.addToEndOfClasspath(jp, wsFile.getFullPath());
            }
          } else if (fsFile != null) {
            FileUtility.copy(jarName, jarStream, fsFile);
            if (!fsFileIsCurrentVersion) {
              final IPath path = new Path(fsFile.getAbsolutePath());
              JDTUtility.addToEndOfClasspath(jp, path);
            }
          } else {
            /*
             * Use the default location at the root of the project for the
             * promises.jar file. Also add the jar to the project's classpath.
             */
            final IFile jarFile = jp.getProject().getFile(jarName);
            EclipseUtility.copy(jarStream, jarFile);
            JDTUtility.addToEndOfClasspath(jp, jarFile.getFullPath());
          }
        } catch (final Exception e) {
          final int code = 221;
          return SLEclipseStatusUtility.createErrorStatus(code, I18N.err(code, jarName, jpName), e);
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
   *          a Eclipse Java project.
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
   * Matches all known versions of the promises.jar on a path.
   */
  static class PromisesJarMatcher extends IPathFilter {
    final List<IPath> results = new ArrayList<>();

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
      // Check if path is an older version of the promises8
      for (String name : LibResources.PROMISES8_JAR_OLD_VERSIONS) {
        if (name.equals(path.lastSegment())) {
          results.add(path);
          return true;
        }
      }
      // Check if path is the current version of the promises8
      if (LibResources.PROMISES8_JAR.equals(path.lastSegment())) {
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
   * Gets all the promise.jars on the classpath of the given project. The older
   * versions are determined by the
   * {@link LibResources#PROMISES_JAR_OLD_VERSIONS} array. The current version
   * is determined by {@link LibResources#PROMISES_JAR}.
   * 
   * @param jp
   *          an Eclipse Java project.
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
