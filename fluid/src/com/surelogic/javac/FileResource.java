package com.surelogic.javac;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.surelogic.common.FileUtility;
import com.surelogic.common.Pair;

import edu.cmu.cs.fluid.java.ICodeFile;

public class FileResource implements ICodeFile {
  final String pkgName;
  final String cuName;
  final URI fileURI;
  final String relativePath;
  final String absolutePath;
  final String project;

  private FileResource(String pkg, String cu, Pair<URI, String> location, String proj) {
    pkgName = pkg;
    cuName = cu;
    fileURI = location.first();
    relativePath = FileUtility.normalizePath(location.second());
    absolutePath = new File(location.first()).getAbsolutePath();
    project = proj;
    if (project == null) {
      throw new NullPointerException("No project for " + fileURI);
    }
  }

  public FileResource(String project, File f) {
    this("(unknown)", f.getName(), convertToURI(null, f.toURI()), project);
  }

  /**
   * For individual .class files
   */
  public FileResource(Projects projects, File classFile, String qname, String proj) {
    this(computePackage(qname), classFile.getName(), /*
                                                      * convertToURI(projects,
                                                      * classFile.toURI())
                                                      */
    new Pair<URI, String>(classFile.toURI(), null), proj); // TODO fix null
  }

  // public FileResource(Projects projects, JavaFileObject sourceFile, String
  // pkg, String proj) {
  // this(pkg, sourceFile.getName().substring(0,
  // sourceFile.getName().length()-5),
  // convertToURI(projects, sourceFile.toUri()), proj);
  // }

  public FileResource(Projects projects, JavaSourceFile sourceFile, String pkg, String proj) {
    this(pkg, computeCUName(sourceFile.relativePath), sourceFile.getLocation(), proj);
  }

  public String getProjectName() {
    return project;
  }

  private static String computeCUName(String path) {
    int lastSlash = path.lastIndexOf('/');
    if (File.separatorChar != '/' && lastSlash < 0) {
      lastSlash = path.lastIndexOf(File.separatorChar);
    }
    if (lastSlash < 0) {
      return path; // TODO Need to remove suffix?
    }
    String rv = path.substring(lastSlash + 1, path.length() - 5);
    // System.out.println("CUName: "+path+" => "+rv);
    return rv;
  }

  private static String computePackage(String qname) {
    int lastDot = qname.lastIndexOf('.');
    return lastDot < 0 ? qname : qname.substring(0, lastDot);
  }

  private static Pair<URI, String> convertToURI(Projects projects, final URI origURI) {
    if (projects != null) {
      JavaSourceFile f = null;
      for (Config config : projects.getConfigs()) {
        f = config.mapPath(origURI);
        if (f != null) {
          break;
        }
      }
      if (f != null) {
        String mapped = f.file.toURI().toString();
        // Fix up URI
        if (mapped.startsWith("file:/") && !mapped.startsWith("file://")) {
          mapped = "file://" + mapped.substring(6);
        }
        try {
          return new Pair<URI, String>(new URI(mapped), f.relativePath);
        } catch (URISyntaxException e) {
          e.printStackTrace();
        }
        return new Pair<URI, String>(f.file.toURI(), f.relativePath);
      } else {
        throw new IllegalStateException("Couldn't map " + origURI);
      }
    }
    return new Pair<URI, String>(origURI, null);
  }

  public Object getHostEnvResource() {
    return null;
  }

  public String getPackage() {
    return pkgName;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public URI getURI() {
    return fileURI;
  }

  public String getCUName() {
    return cuName;
  }

  @Override
  public int hashCode() {
    return relativePath.hashCode() + project.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof FileResource) {
      if (o == this) {
        return true;
      }
      FileResource f = (FileResource) o;
      try {
        return relativePath.equals(f.relativePath) && project.equals(f.project);
      } catch (NullPointerException e) {
        System.out.println("Got NPE on " + this);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return fileURI.toString();
  }
}
