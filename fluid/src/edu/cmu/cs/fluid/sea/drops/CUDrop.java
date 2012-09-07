package edu.cmu.cs.fluid.sea.drops;


import java.util.*;

import com.surelogic.RequiresLock;
import com.surelogic.ast.java.operator.ICompilationUnitNode;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.xml.*;

/**
 * Drop representing a compilation unit, suitable for promise and result drops
 * to depend upon.  Created and invalidated by the eAST to fAST converter.
 * 
 * @see edu.cmu.cs.fluid.analysis.util.ConvertToIR
 */
public abstract class CUDrop extends Drop {
  private static SlotInfo<CUDrop> si = SimpleSlotFactory.prototype.newLabeledAttribute("CUDrop", null);  
  
  protected final CodeInfo info;
  public final String javaOSFileName;

  public final IRNode cu;
  
  public final Object hostEnvResource;
  /**
   * FIX should be mutable if this drop persists across versions
   */
  public final ICompilationUnitNode cun;  
  
  public final int lines;
  
  private final Set<String> elidedFields;
  
  /**
   * <code>ModuleNum</code> holds the <code>int</code> encoding of the Module that
   * this CU is part of.  The value is just a cookie to indicate the actual module.
   * This is a hack to get Dean's module experiments on the air.
   */
  public int ModuleNum = -1;
  
  @SuppressWarnings("unchecked")
  protected CUDrop(CodeInfo info) {
	  //System.out.println("Creating CU for "+info.getFileName());
	  
	// TODO will this suck up space for the source?
	this.info = info;
    cu = info.getNode();
    if (info.getCompUnit() != null) {
      cun = info.getCompUnit();
    } else {
      if (IDE.debugTypedASTs) {
        LOG.warning("No ICompilationUnitNode for "+info.getFileName());
      }
      cun = null;
    }
    javaOSFileName = info.getFileName();
    
    hostEnvResource = info.getHostEnvResource();
    
    Integer loc = (Integer) info.getProperty(CodeInfo.LOC);
    lines = (loc != null) ? loc.intValue() : 0;
    
    Set<String> ef = (Set<String>) info.getProperty(CodeInfo.ELIDED);
    if (ef == null) {
      elidedFields = Collections.emptySet();
    } else {
      elidedFields = ef;
    }
    String pkgName = VisitUtil.getPackageName(cu);
    final PackageDrop pd = PackageDrop.createPackage(null, pkgName, null, null);
    pd.addDependent(this);    
    finishInit();
  }

  public CodeInfo makeCodeInfo() {	  
	  if (info == null) {
		  if (this instanceof PackageDrop) {
			  return null;
		  }
		  throw new UnsupportedOperationException("No CodeInfo for "+DebugUnparser.toString(cu));
	  }
	  info.clearProperty(CodeInfo.DONE);
	  return info;
  }
  
  public String getRelativePath() {
	  return info == null ? null : info.getFile().getRelativePath();
  }
  
  private void finishInit() {
    if (cu != null) {
      cu.setSlotValue(si, this);
    } else {
      LOG.severe("No node while building CUDrop for "+javaOSFileName);
    }
    setMessage(this.getClass().getSimpleName()+" "+javaOSFileName);
  }
  
  /**
   * Only to be called by PackageDrop()
   */
  CUDrop(String pkgName, IRNode root) {
	info  = null; 
    cu    = root;
    cun   = null;
    lines = 1;
    javaOSFileName  = pkgName;
    hostEnvResource = null;
    elidedFields    = Collections.emptySet();
    
    finishInit();
  }
  
  public ITypeEnvironment getTypeEnv() {
	  if (info == null) {
		  return null;
	  }
	  return info.getTypeEnv();
  }
  
  /**
   * Looks up the drop corresponding to the given fAST CompilationUnit.
   * 
   * @param cu the fAST IRNode CompilationUnit to lookup the drop for
   * @return the corresponding drop, or <code>null</code> if a drop does
   *   not exist.
   * 
   * @see edu.cmu.cs.fluid.java.operator.CompilationUnit
   */
  static public CUDrop queryCU(IRNode cu) {
    /*
    Set drops = Sea.getDefault().getDropsOfType(CUDrop.class);
    for (Iterator i = drops.iterator(); i.hasNext();) {
      CUDrop drop = (CUDrop) i.next();
      if (drop.cu.equals(cu))
        return drop;
    }
    return null;
    */
    if (cu == null) {
      return null;
    }
    return cu.getSlotValue(si);
  }
  
  public boolean wasElided(String f) {
    return elidedFields.contains(f);
  }
  
  @Override
  public String toString() {
    return "CUDrop: "+javaOSFileName;
  }

  
  /**
   * @return Returns the hostEnvResource.
   */
  public Object getHostEnvResource() {
    return hostEnvResource;
  }
  
  public abstract boolean isAsSource();
 
  @Override
  public String getXMLElementName() {
	  return "cu-drop";
  }	
  
  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
	  super.snapshotAttrs(s);
	  s.addAttribute("filename", javaOSFileName);
  }
  
  @Override
  @RequiresLock("SeaLock")
  protected void invalidate_internal() {
	  //System.out.println("Invalidating "+javaOSFileName);
  }
  
  @Override
  protected JavaSourceReference createSourceRef() {
	  final ISrcRef ref = JavaNode.getSrcRef(cu);	  
	  return new JavaSourceReference(ref.getPackage(), ref.getCUName(), ref.getLineNumber(), ref.getOffset());
 
  }
}
