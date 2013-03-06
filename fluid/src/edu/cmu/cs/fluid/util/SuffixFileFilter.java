package edu.cmu.cs.fluid.util;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class SuffixFileFilter extends FileFilter implements FilenameFilter {
  public SuffixFileFilter(String[] suffixes) { 
    this.suffix = suffixes; 

    for (int i=0; i<suffixes.length; i++) {
      description += "\""+suffixes[i]+"\" ";
    }
  }

  public SuffixFileFilter(String suffix) { 
    this.suffix    = new String[1];
    this.suffix[0] = suffix;

    description += "\""+suffix+"\" ";
  }
  String[] suffix;
  
  @Override
  public boolean accept(File f) {
    if (f.isDirectory()) { 
      return true;
    }
      for(int i=0; i<suffix.length; i++) {
	if (f.getName().endsWith(suffix[i])) return true;
      }
      return false;
  }
  
  @Override
  public boolean accept(File dir, String name) {         
    for(int i=0; i<suffix.length; i++) {
      if (name.endsWith(suffix[i])) return true;
    }
    File f = new File(dir, name);
      return f.isDirectory();
  }

  private String description = "Accepts files ending with ";

  @Override
  public String getDescription() {
    return description;
  }
}
