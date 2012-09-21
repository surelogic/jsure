/*
 * Created on Jul 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.HashMap;
import java.util.Map;

import SableJBDD.bdd.JBDD;
import SableJBDD.bdd.JBddManager;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRoleBDDPack {


  static final TRoleBDDPack INSTANCE = new TRoleBDDPack();
  
  private static final Map<JBDD, String> printMap = new HashMap<JBDD, String>();
  

  //private BDDFactory bddFact;
  private JBddManager bddManager;
  //private int numBddVars = 0;
  
 // public BDDFactory getBddFactory() {
  public static JBddManager getBddFactory() {
    if (INSTANCE.bddManager == null) {
      INSTANCE.bddManager = new JBddManager();
    }
    return INSTANCE.bddManager;
  }
  
  public static JBDD one() {
    return getBddFactory().ONE();
  }
  
  public static JBDD zero() {
    return getBddFactory().ZERO();
  }
  
//  public JBDD bddForColor(String colorName) {
//    final TRoleNameModel nameModel = TRoleNameModel.getInstance(colorName);
//    return bddManager.posBddOf(nameModel.getTheBddVar());
//  }
  
  private void bddTest() {
    JBDD awt = getBddFactory().posBddOf(getBddFactory().newVariable("awt"));
    JBDD compute = bddManager.posBddOf(bddManager.newVariable("compute"));
    JBDD nyv = bddManager.posBddOf(bddManager.newVariable("nyv"));
    
    JBDD t1 = awt.and(compute.not());
    JBDD r1 = t1.or(nyv);
    JBDD t2 = awt.and(compute).not();
    r1 = r1.and(t2);
    prval("((awt & !compute) | nyv)&!(awt&compute) evaluated to:", r1, "xxx");
    
    
    t1 = r1.and(awt);
    t2 = r1.and(awt.not());
    JBDD r2 = t1.or(t2);
    JBDD r2a = awt.or(nyv);
    prval("(awt | nyv)", r2a, "xxx");
    prval("(((awt & !compute) | nyv)&compute) | (((awt & !compute) | nyv)&!compute) evaluated to:",
          r2, r2a.toString());
    
    JBDD r2b = r1.exist(awt);
    prval("((awt & !compute) | nyv).exist(awt) evaluated to", r2b, r2a.toString());
  }
  
  private void prval(String msg, JBDD tocheck, String expectedRes) {
    System.out.print(msg);
    if (tocheck.isZero()) {
      System.out.println("ZERO");
    } else if (tocheck.isOne()) {
      System.out.println("ONE");
    } else {
      System.out.println(tocheck);
    }
    System.out.print("Expected result is: ");
    System.out.println(expectedRes);
  }
  public void initBDDPack() {
    //bddTest();
  }
  /**
   * @return Returns the iNSTANCE.
   */
  public static TRoleBDDPack getInstance() {
    return INSTANCE;
  }
  
  /** Register the print image of a BDD. The shortest such image for a given BDD will
   * be the one that is stored.
   * @param image The image to (potentially) remember.
   * @param forBDD The BDD that matches the image.
   */
  public static void registerCanonicalImage(final String image, JBDD forBDD) {
    final String savedImage = printMap.get(forBDD);
    if (savedImage == null) {
      printMap.put(forBDD, image);
    } else if (image.length() < savedImage.length()) {
      printMap.put(forBDD, image);
    }
  }
  
  public static void unRegisterCanonicalImage(final String image, 
                                              final JBDD forBDD) {
    final String savedImage = printMap.get(forBDD);
    if (savedImage == null || savedImage.equals(image)) {
      printMap.remove(forBDD);
    }
  }
  
  /** Get the canonical print image of forBDD. This will be the shortest image
   * presented via registerCanonicalImage. Note that the result may be
   * <code>null</code> if no image was presented for this BDD.
   * 
   * @param forBDD A BDD whose canonical image is desired.
   * @return The canonical image, or <code>null</code> if none is available.
   */
  public static String getCanonicalImage(final JBDD forBDD) {
    final String res = printMap.get(forBDD);
    return res;
  }
  
  public static void resetCanonicalImages() {
    printMap.clear();
  }
  

  public static String userStr(JBDD expr) {
    String res = getCanonicalImage(expr);
    if (res != null) {
      return res;
    } else {
      // maybe the expr at hand implies one (or more) of the renames.
      return expr.toString();
    }
  }
}
