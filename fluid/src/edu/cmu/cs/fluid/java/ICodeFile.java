package edu.cmu.cs.fluid.java;

/*
 * Abstraction of a file that represents some code
 */
public interface ICodeFile {
  Object getHostEnvResource();
  String getPackage();
  String getProjectName();
  String getRelativePath();
}
