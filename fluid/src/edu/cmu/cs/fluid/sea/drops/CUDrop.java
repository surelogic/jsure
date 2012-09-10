package edu.cmu.cs.fluid.sea.drops;

import java.util.Collections;
import java.util.Set;

import com.surelogic.RequiresLock;
import com.surelogic.ast.java.operator.ICompilationUnitNode;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Drop;

/**
 * Drop representing a compilation unit, suitable for promise and result drops
 * to depend upon. Created and invalidated by the eAST to fAST converter.
 */
public abstract class CUDrop extends Drop {

  private static final SlotInfo<CUDrop> SI_CUDROP = SimpleSlotFactory.prototype.newLabeledAttribute("CUDrop", null);

  protected final CodeInfo f_codeInfo;

  public final String f_javaOSFileName;

  public final IRNode f_cu;

  public final Object f_hostEnvResource;

  public final ICompilationUnitNode f_compilationUnitNode;

  public final int f_linesOfCode;

  private final Set<String> f_elidedFields;

  protected CUDrop(CodeInfo info) {
    // System.out.println("Creating CU for "+info.getFileName());

    // TODO will this suck up space for the source?
    this.f_codeInfo = info;
    f_cu = info.getNode();
    if (info.getCompUnit() != null) {
      f_compilationUnitNode = info.getCompUnit();
    } else {
      if (IDE.debugTypedASTs) {
        LOG.warning("No ICompilationUnitNode for " + info.getFileName());
      }
      f_compilationUnitNode = null;
    }
    f_javaOSFileName = info.getFileName();

    f_hostEnvResource = info.getHostEnvResource();

    Integer loc = (Integer) info.getProperty(CodeInfo.LOC);
    f_linesOfCode = (loc != null) ? loc.intValue() : 0;

    @SuppressWarnings("unchecked")
    final Set<String> ef = (Set<String>) info.getProperty(CodeInfo.ELIDED);
    if (ef == null) {
      f_elidedFields = Collections.emptySet();
    } else {
      f_elidedFields = ef;
    }
    final String pkgName = VisitUtil.getPackageName(f_cu);
    final PackageDrop pd = PackageDrop.createPackage(null, pkgName, null, null);
    pd.addDependent(this);
    finishInit();
  }

  /**
   * Only to be called by {@link PackageDrop}.
   */
  CUDrop(String pkgName, IRNode root) {
    f_codeInfo = null;
    f_cu = root;
    f_compilationUnitNode = null;
    f_linesOfCode = 1;
    f_javaOSFileName = pkgName;
    f_hostEnvResource = null;
    f_elidedFields = Collections.emptySet();

    finishInit();
  }

  public final CodeInfo makeCodeInfo() {
    if (f_codeInfo == null) {
      if (this instanceof PackageDrop) {
        return null;
      }
      throw new UnsupportedOperationException("No CodeInfo for " + DebugUnparser.toString(f_cu));
    }
    f_codeInfo.clearProperty(CodeInfo.DONE);
    return f_codeInfo;
  }

  public final String getRelativePath() {
    return f_codeInfo == null ? null : f_codeInfo.getFile().getRelativePath();
  }

  private void finishInit() {
    if (f_cu != null) {
      f_cu.setSlotValue(SI_CUDROP, this);
    } else {
      LOG.severe("No node while building CUDrop for " + f_javaOSFileName);
    }
    setMessage(this.getClass().getSimpleName() + " " + f_javaOSFileName);
  }

  public final ITypeEnvironment getTypeEnv() {
    if (f_codeInfo == null) {
      return null;
    }
    return f_codeInfo.getTypeEnv();
  }

  /**
   * Looks up the drop corresponding to the given fAST CompilationUnit.
   * 
   * @param cu
   *          the fAST IRNode CompilationUnit to lookup the drop for
   * @return the corresponding drop, or <code>null</code> if a drop does not
   *         exist.
   * 
   * @see edu.cmu.cs.fluid.java.operator.CompilationUnit
   */
  static public CUDrop queryCU(IRNode cu) {
    if (cu == null) {
      return null;
    }
    return cu.getSlotValue(SI_CUDROP);
  }

  public boolean wasElided(String f) {
    return f_elidedFields.contains(f);
  }

  @Override
  public String toString() {
    return "CUDrop: " + f_javaOSFileName;
  }

  /**
   * @return Returns the hostEnvResource.
   */
  public Object getHostEnvResource() {
    return f_hostEnvResource;
  }

  public abstract boolean isAsSource();

  @Override
  public String getXMLElementName() {
    return "cu-drop";
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute("filename", f_javaOSFileName);
  }

  @Override
  @RequiresLock("SeaLock")
  protected JavaSourceReference createSourceRef() {
    final ISrcRef ref = JavaNode.getSrcRef(f_cu);
    return new JavaSourceReference(ref.getPackage(), ref.getCUName(), ref.getLineNumber(), ref.getOffset());
  }
}
