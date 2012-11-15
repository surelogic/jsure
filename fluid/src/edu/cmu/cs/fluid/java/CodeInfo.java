/*
 * Created on Dec 3, 2004
 *
 */
package edu.cmu.cs.fluid.java;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.IJavaFileLocator.Type;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

import java.util.*;

import com.surelogic.ast.java.operator.ICompilationUnitNode;

/**
 * @author Edwin
 *
 */
public final class CodeInfo {
  private final Map<Object,Object> properties = new HashMap<Object,Object>();
  private final IRNode n;
  private final ICompilationUnitNode cun;
  private final String filename;
  private final ICodeFile file;
  //private final String source;
  private final Type type;
  private final Object hostEnvResource;
  private final ITypeEnvironment tEnv;
  
  /**
   * @param cu The handle identifier for the compilation unit
   * @param node The JavaNode at the top of the IR representing this ICompilationUnit.
   * @param javaOSFileName The complete OS path and name for the Java file.
   * @param src The source code for this ICompilationUnit.
   */
  public CodeInfo(ITypeEnvironment te, ICodeFile cu, IRNode node, ICompilationUnitNode icu, String javaOSFileName, String src, Type t) {
    tEnv = te;
    file = cu;
    n = node;
    cun = icu;
    filename = javaOSFileName;
    //source = src;
    type = t;
    hostEnvResource = (file == null) ? null : file.getHostEnvResource();
  }
  
  public static CodeInfo createMatchTemplate(IRNode n, String name) {
    return new CodeInfo(null, null, n, null, name, null, Type.UNKNOWN);
  }
  
  public ITypeEnvironment getTypeEnv() {
	  return tEnv;
  }
  
  public IRNode getNode() {
    return n;    
  }
  
  public ICompilationUnitNode getCompUnit() {
    return cun;
  }
  
  public String getFileName() {
    return filename; 
  }
  /**
   * The String handle for the file 
   * @return
   */
  public ICodeFile getFile() {  
    return file;
  } 
  /*
  public String getSource() {
    return source;
  }
  */
  public Object setProperty(Object key, Object value) {
    return properties.put(key, value);
  }
  public Object getProperty(Object key) {
    return properties.get(key);
  }
  
  public static final String TOTAL_LINES = "CodeInfo.allLines";
  public static final String BLANK_LINES = "CodeInfo.blanks"; 
  public static final String SEMICOLONS = "CodeInfo.semicolons";
  public static final String DECLS = "CodeInfo.decls";
  public static final String STMTS = "CodeInfo.stmts";
  public static final String LOC = "CodeInfo.loc";  
  public static final String DONE = "CodeInfo.done";
  public static final String CU = "CodeInfo.cu";
  public static final String ELIDED = "CodeInfo.elided";
  public static final String CLASSPATH = "CodeInfo.classpath";
  
  @Override
  public int hashCode() {    
    if (n == null) {
      //System.out.println("n == null");
      return 0;
    }
    return n.hashCode();
  }
  @Override
  public boolean equals(Object o) {
    if (o instanceof CodeInfo) {
      return n.equals(((CodeInfo)o).n);
    }
    return false;
  }

  public void clearProperty(String key) {
    properties.remove(key);
  }

  public boolean isAsSource() {
    return type == Type.SOURCE;
  }
  
  /**
   * @return Returns the hostEnvResource.
   */
  public Object getHostEnvResource() {
    return hostEnvResource;
  }


  public Type getType() {
    return type;
  }
}
