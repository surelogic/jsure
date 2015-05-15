package com.surelogic.ant.jsure;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.SLUtility;

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
   * The name of the directory to place the scan zip.
   */
  @Nullable
  private String jsureScanDir;

  /**
   * The location of the 'surelogic-tools.properties' file.
   */
  @Nullable
  private String surelogicToolsPropertiesFile;

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
   * The name of the directory to place the scan zip.
   * 
   * @return the name of the directory to place the scan zip.
   */
  @Nullable
  public String getJSureScanDir() {
    return jsureScanDir;
  }

  /**
   * The name of the output file.
   * 
   * @param value
   *          the name of the output file.
   */
  public void setJSureScanDir(String value) {
    jsureScanDir = value;
  }

  /**
   * The path to the surelogic-tools.properties file.
   * 
   * @return the path to the surelogic-tools.properties file.
   */
  @Nullable
  public String getSurelogicToolsPropertiesFile() {
    return surelogicToolsPropertiesFile;
  }

  /**
   * The path to the surelogic-tools.properties file.
   * 
   * @param value
   *          the path to the surelogic-tools.properties file.
   */
  public void setSurelogicToolsPropertiesFile(String value) {
    surelogicToolsPropertiesFile = value;
  }

  /**
   * Gets the JSure ant task directory.
   * 
   * @return the JSure ant task directory.
   * @throws BuildException
   *           if the directory doesn't exist on the disk.
   */
  public File getJSureAntHomeAsFile() {
    final File result = new File(jsureAntHome);
    if (!result.isDirectory())
      throw new BuildException("JSureAntHome does not exist: " + result.getAbsolutePath());
    return result;
  }

  /**
   * Gets the location to place the output scan zip into
   * 
   * @return an file to put the scan in.
   */
  @NonNull
  public File getJSureScanDirAsFile() {
    final String outDir;
    if (jsureScanDir == null)
      outDir = ".";
    else
      outDir = jsureScanDir;
    final File result = new File(outDir);
    if (!result.isDirectory())
      throw new BuildException("JSure scan output directory does not exist: " + result.getAbsolutePath());
    return result;
  }

  /**
   * Gets the location of the 'surelogic-tools.properties' file. If none is set
   * the current working directory is checked.
   * 
   * @return the location of the 'surelogic-tools.properties' file, or
   *         {@code null} if none.
   * @throws BuildException
   *           if the file doesn't exist on the disk and the Ant script
   *           specified a precise location.
   */
  @Nullable
  public File getSurelogicToolsPropertiesAsFile() {
    final boolean pathnameSet = surelogicToolsPropertiesFile != null;
    final String pathname = pathnameSet ? surelogicToolsPropertiesFile : "./" + SLUtility.SL_TOOLS_PROPS_FILE;
    final File result = new File(pathname);
    if (result.isFile())
      return result;
    else {
      if (pathnameSet)
        throw new BuildException("JSureAntHome does not exist: " + result.getAbsolutePath());
      else
        return null;
    }
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
    if (compileList.length > 0) {
      CompilerAdapter adapter = new JSureJavacAdapter(this, compileList);

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
    } else {
      log("No Java files found to scan in " + jsureProjectName + "...JSure scan skipped");
    }
  }
}
