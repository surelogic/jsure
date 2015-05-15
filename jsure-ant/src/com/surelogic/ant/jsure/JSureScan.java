package com.surelogic.ant.jsure;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;

import com.surelogic.Nullable;

public class JSureScan extends Javac {
  /**
   * The location of built JSure Ant task.
   */
  private String jsureAntHome;

  /**
   * The name of the project being scanned.
   */
  private String jsureProjectName;

  /**
   * The name of the target output file (the scan is zipped into). A value of
   * "scan.zip" is used if this isn't set. If ".zip" is not the extension that
   * is added.
   */
  @Nullable
  private String jsureToFile;

  /**
   * The location of the JSure ant task.
   * 
   * @return the location of the JSure ant task.
   */
  public String getJSureAntHome() {
    return jsureAntHome;
  }

  /**
   * The location of the JSure ant task.
   * 
   * @param value
   *          the location of the JSure ant task.
   */
  public void setJSureAntHome(String value) {
    jsureAntHome = value;
  }

  /**
   * Human readable name for the project being scanned.
   * 
   * @return the name of the project being scanned.
   */
  public String getJSureProjectName() {
    return jsureProjectName;
  }

  /**
   * Human readable name for the project being scanned.
   * 
   * @param value
   *          the name of the project being scanned.
   */
  public void setJSureProjectName(String value) {
    jsureProjectName = value;
  }

  /**
   * The name of the output file.
   * 
   * @return the name of the output file.
   */
  public String getJSureToFile() {
    return jsureToFile;
  }

  /**
   * The name of the output file.
   * 
   * @param value
   *          the name of the output file.
   */
  public void setJSureToTile(String value) {
    jsureToFile = value;
  }

  /**
   * Gets the JSure ant task directory.
   * 
   * @return the JSure ant task directory.
   * @throws BuildException
   *           if the directory doesn't exist on the disk.
   */
  public File getJSureAntHomeDir() {
    final File result = new File(jsureAntHome);
    if (!result.isDirectory())
      throw new BuildException("JSureAntHome a directory: " + result.getAbsolutePath());
    return result;
  }

  /**
   * Gets the output zip file name to use. A value of "scan.zip" is used if this
   * isn't set. If ".zip" is not the extension that is added.
   * 
   * @return an file to put the scan in.
   */
  public File getJSureToFileZip() {
    final String outfile;
    if (jsureToFile == null)
      outfile = "scan.zip";
    else if (jsureToFile.endsWith(".zip"))
      outfile = jsureToFile;
    else
      outfile = jsureToFile + ".zip";
    final File result = new File(outfile);
    return result;
  }

  @Override
  protected void scanDir(File srcDir, File destDir, String[] files) {
    File[] newFiles = new File[files.length];
    int i = 0;
    for (String name : files) {
      newFiles[i] = new File(srcDir, name);
      i++;
    }

    if (newFiles.length > 0) {
      File[] newCompileList = new File[compileList.length + newFiles.length];
      System.arraycopy(compileList, 0, newCompileList, 0, compileList.length);
      System.arraycopy(newFiles, 0, newCompileList, compileList.length, newFiles.length);
      compileList = newCompileList;
    }
  }

  /**
   * Modified from Javac.compile()
   */
  @Override
  protected void compile() {
    File destDir = this.getDestdir();

    if (compileList.length > 0) {
      log("JSure examining " + compileList.length + " source file" + (compileList.length == 1 ? "" : "s") + " in "
          + destDir.getAbsolutePath());

      if (listFiles) {
        for (int i = 0; i < compileList.length; i++) {
          String filename = compileList[i].getAbsolutePath();
          log(filename);
        }
      }

      CompilerAdapter adapter = new JSureJavacAdapter(this);

      // now we need to populate the compiler adapter
      adapter.setJavac(this);

      // finally, lets execute the compiler!!
      if (!adapter.execute()) {
        if (failOnError) {
          throw new BuildException("Failed", getLocation());
        } else {
          log("Failed", Project.MSG_ERR);
        }
      }
    }
  }
}
