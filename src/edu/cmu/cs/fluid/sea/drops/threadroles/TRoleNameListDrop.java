/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ThreadRoleNameListNode;
import com.surelogic.aast.promise.ThreadRoleNameNode;
import com.surelogic.analysis.threadroles.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
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
public abstract class TRoleNameListDrop<A extends ThreadRoleNameListNode> extends PromiseDrop<A> {
  private final String kind;
  private final IRNode declContext;
  static final Logger LOG = SLLogger.getLogger("TRoleDropBuilding");

  private Collection<String> listedTRoles = null;
  private Collection<String> listedRenamedTRoles = null;
  
  TRoleNameListDrop(A a, String theKind) {
    super();
    kind = theKind;
    List<ThreadRoleNameNode> declTRoles = a.getThreadRoleList();
    final IRNode locInIR = a.getPromisedFor();
    declContext = VisitUtil.computeOutermostEnclosingTypeOrCU(locInIR);
    if ((declTRoles == null) || (declTRoles.size() == 0)) {
      listedTRoles = Collections.emptySet();
    } else {
      listedTRoles = new HashSet<String>(declTRoles.size());
      
      for (ThreadRoleNameNode cnn: declTRoles) {
        listedTRoles.add(cnn.getId());
      }
    }
    StringBuilder sb = new StringBuilder();
    Iterator<String> dtrIter = listedTRoles.iterator();
    boolean first = true;
    sb.append(kind);
    sb.append(' ');
    while (dtrIter.hasNext()) {
      String name = dtrIter.next();
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
    setCategory(TRoleMessages.assuranceCategory);
    setNodeAndCompilationUnitDependency(locInIR);
    makeTRoleNameModelDeps(locInIR);
  }

  private void makeTRoleNameModelDeps(final IRNode locInIR) {
    if (listedTRoles == null) return;
    
    Iterator<String> lcIter = listedTRoles.iterator();
    while (lcIter.hasNext()) {
      final String aName = lcIter.next();
      TRoleNameModel cnm = TRoleNameModel.getInstance(aName, locInIR);
      cnm.addDependent(this);
    }
  }
  
  private void makeCanonicalTRoleNameModelDeps() {
    if (listedRenamedTRoles == null) return;
    
    for (String aName : listedRenamedTRoles) {
      TRoleNameModel trnm = TRoleNameModel.getCanonicalInstance(aName, getNode());
      trnm.addDependent(this);
    }
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    TRolesFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }
  /**
   * @return Returns the listedColors.
   */
  public Collection<String> getListedTRoles() {
    return listedTRoles;
  }

  /**
   * @return Returns the listedRenamedColors.
   */
  public Collection<String> getListedRenamedTRoles() {
    if (listedRenamedTRoles == null) return listedTRoles;
    return listedRenamedTRoles;
  }

  /**
   * @param listedRenamedTRoles The listedRenamedColors to set.
   */
  public void setListedRenamedTRoles(Collection<String> listedRenamedTRoles) {
    this.listedRenamedTRoles = listedRenamedTRoles;
    
    makeCanonicalTRoleNameModelDeps();
  }

  
  /**
   * @return Returns the declContext.
   */
  public IRNode getDeclContext() {
    return declContext;
  }
}
