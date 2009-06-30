/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.Drop;

/**
 * @author dfsuther
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public class ColorDeclareDrop extends ColorNameListDrop {

  protected static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  private static final String myKind = "color";
  
  private final String cuContainingDecl;

//  private static final Map<IRNode, Collection<ColorDeclareDrop>> cuToDecls = new HashMap<IRNode, Collection<ColorDeclareDrop>>();

  // Map<stdContextName => 
  //     Collection<ColorDeclareDrop> in that context>
  private static final Map<String, Collection<ColorDeclareDrop>> localColorDecls = 
    new HashMap<String, Collection<ColorDeclareDrop>>();
  
//  // Map<qual-name of ColorDeclareDrop (as String) ==> ColorDeclareDrop> for global
//  // visibility!
//  private static final Map<String, ColorDeclareDrop> globalColorDecls = 
//    new HashMap<String, ColorDeclareDrop>();

  // public ColorDeclareDrop(Collection declColors) {
  // super(declColors, myKind);
  // }

  /**
   * Return the simple name portion of a possibly qualified name. raises
   * exception for null arg, but handles empty string correctly.
   * 
   * @param name
   *          A possibly qualified name
   * @return the part of name that is NOT the simple name.  This could be an empty string.
   */
  private static String genQualifier(final String name) {
    // failfast on null arg!
    int posOfLastDot = name.lastIndexOf('.');
    if (posOfLastDot < 0 || (posOfLastDot == name.length() - 1)) {
      return "";
    }

    return name.substring(0, posOfLastDot);
  }
  
  public ColorDeclareDrop(Collection<String> declColors, IRNode locInIR) {
    // don't have to mess around with expanding names to be fully qualified.
    // Callers are required to have done that already!
    super(declColors, myKind, locInIR);
    // want to see all declare drops!
    setFromSrc(true);

    final String cuName = JavaNames.computeQualForOutermostTypeOrCU(locInIR);
    
    // #2 is to do string hacking on the names.
    String stringCuName = "";
    nameLoop: for (String colorName : declColors) {
      stringCuName = genQualifier(colorName);
      break nameLoop;
    }
    if (!stringCuName.equals(cuName)) {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("cuName \"" + cuName +"\" != stringCuName \"" + stringCuName + "\"");
      }
    }
    
    cuContainingDecl = cuName;

    synchronized (ColorDeclareDrop.class) {

//      Collection<ColorDeclareDrop> decls = cuToDecls.get(cu);
//      if (decls == null) {
//        decls = new HashSet<ColorDeclareDrop>(1);
//        cuToDecls.put(cu, decls);
//      }
//      decls.add(this);

      Collection<ColorDeclareDrop> decls = localColorDecls.get(cuName);

      if (decls == null) {
        decls = new HashSet<ColorDeclareDrop>(1);
        decls.add(this);
        localColorDecls.put(cuName, decls);
      } else if (!decls.contains(this)) {
        decls.add(this);
      }
    }

  }

  public static synchronized Collection<ColorDeclareDrop> getColorDeclsForCU(final String cuName) {
    return localColorDecls.get(cuName);
  }

  public Collection<String> getDeclaredColors() {
    return getListedColors();
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }
    // Any DeclareDrop specific action would go here.
    // note that super... takes care of the ColorAnnoMap for us...
    final IRNode locInIR = getNode();
    synchronized (ColorDeclareDrop.class) {
      Collection<ColorDeclareDrop> decls = localColorDecls.get(cuContainingDecl);
      if (decls != null) {
        decls.remove(this);
      } else {
        // invariant violation! Any previously created ColorDeclareDrop that hasn't
        // been invalidated yet should be in localColorDecls!
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine(cuContainingDecl + " not found in map!");
        }
      }
    }
    ColorImportDrop.reprocessImporters(locInIR);
    super.deponentInvalidAction(invalidDeponent);
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getMessage();
  }

}
