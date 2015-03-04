/*
 * Created on Jun 24, 2004
 *
 */
package com.surelogic.annotation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chance
 */
public class CompUnitPattern {  
  final String project;
  final String pattern;
  final Pattern compiledPattern;
  final boolean matchPath;
  
  public static CompUnitPattern create(String p, String pattern) {
	return new CompUnitPattern(p, pattern);  
  }
  
  private CompUnitPattern(String p, String pattern) {
    this.project = p;
    this.pattern = pattern;

    if (pattern.contains("/")) { 
    	matchPath = true;
    	pattern = '/'+project+'/'+pattern;
    } else {
    	matchPath = false;
    }
	final String pattern2 = pattern.replaceAll("\\.", "\\.").replaceAll("\\*", ".*");
	this.compiledPattern = Pattern.compile(pattern2);
  }
  
  public boolean matches(String pkg, String path) {
	final String subject = matchPath ? path : pkg;
    if (pattern.indexOf('*') < 0) {
      // no wildcards       
      return pattern.equals(subject);
    }      
    //final String pattern2 = pattern.replaceAll("\\*", ".*");
    //return Pattern.matches(pattern2, pkg);
    Matcher m = compiledPattern.matcher(subject);
    return m.matches();
  }
  
  @Override public String toString() {
    return project+" : "+pattern;
  }
  
  /*
  public String getPackage(ICompilationUnit cu) {
    if (project != cu.getJavaProject().getProject()) {
      //System.out.println("Project doesn't match: "+cu.getJavaProject().getProject());
      return "";
    }
    try {
      IPackageDeclaration[] pkgs = cu.getPackageDeclarations();
      if (pkgs.length == 0) {
        return "(default)";
      }
      //System.out.println("Looking at package: "+pkgs[0].getElementName());
      return pkgs[0].getElementName();      
    } catch (JavaModelException e) {
      e.printStackTrace();
    }
    return "";
  }
  */
}
