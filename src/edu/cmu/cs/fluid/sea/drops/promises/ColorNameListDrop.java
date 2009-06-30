/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.ColorFirstPass;
import edu.cmu.cs.fluid.java.analysis.ColorMessages;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public abstract class ColorNameListDrop extends PromiseDrop {
  private final String kind;
  private final IRNode declContext;
  static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  private Collection<String> listedColors = null;
  private Collection<String> listedRenamedColors = null;
  
//  private ColorNameListDrop() {
//    kind = null;
//    XML e = XML.getDefault();
//    if (e == null || e.processingXML()) {
//      setFromSrc(false);
//    }
//    setCategory(ColorMessages.assuranceCategory);
//  }
//  
//  private ColorNameListDrop(Collection<String> declColors) {
//    this(declColors, "ColorNameList");
//  }
  
  ColorNameListDrop(Collection<String> declColors, String theKind, IRNode locInIR) {
    super();
    this.kind = theKind;
    declContext = VisitUtil.computeOutermostEnclosingTypeOrCU(locInIR);
    //listedColors = colors;
    if ((declColors == null) || (declColors.size() == 0)) {
      listedColors = Collections.emptySet();
    } else {
      listedColors = new HashSet<String>(declColors.size());
      listedColors.addAll(declColors);
    }
    StringBuilder sb = new StringBuilder();
    Iterator<String> dcIter = listedColors.iterator();
    boolean first = true;
    sb.append(kind);
    sb.append(' ');
    while (dcIter.hasNext()) {
      String name = dcIter.next();
      if (!first) {
        sb.append(", ");
      }
      sb.append(name);
      first = false;
    }
    setMessage(sb.toString());
    XML e = XML.getDefault();
    if (e == null || e.processingXML()) {
      setFromSrc(false);
    }
    setCategory(ColorMessages.assuranceCategory);
    setNodeAndCompilationUnitDependency(locInIR);
    makeColorNameModelDeps(locInIR);
  }
  
  private void makeColorNameModelDeps(final IRNode locInIR) {
    if (listedColors == null) return;
    
    Iterator<String> lcIter = listedColors.iterator();
    while (lcIter.hasNext()) {
      final String aName = lcIter.next();
      ColorNameModel cnm = ColorNameModel.getInstance(aName, locInIR);
      cnm.addDependent(this);
    }
  }
  
  private void makeCanonicalColorNameModelDeps() {
    if (listedRenamedColors == null) return;
    
    for (String aName : listedRenamedColors) {
      ColorNameModel cnm = ColorNameModel.getCanonicalInstance(aName, getNode());
      cnm.addDependent(this);
    }
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    ColorFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }
  /**
   * @return Returns the listedColors.
   */
  public Collection<String> getListedColors() {
    return listedColors;
  }

  /**
   * @return Returns the listedRenamedColors.
   */
  public Collection<String> getListedRenamedColors() {
    if (listedRenamedColors == null) return listedColors;
    return listedRenamedColors;
  }

  /**
   * @param listedRenamedColors The listedRenamedColors to set.
   */
  public void setListedRenamedColors(Collection<String> listedRenamedColors) {
    this.listedRenamedColors = listedRenamedColors;
    
    makeCanonicalColorNameModelDeps();
  }

  
  /**
   * @return Returns the declContext.
   */
  public IRNode getDeclContext() {
    return declContext;
  }
}
