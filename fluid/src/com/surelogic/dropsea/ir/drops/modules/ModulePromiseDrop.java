/*
 * Created on Oct 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.modules;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.*;
import com.surelogic.analysis.modules.ModuleAnalysisAndVisitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.DropPredicate;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.BinaryCUDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public abstract class ModulePromiseDrop extends PromiseDrop<ModuleChoiceNode> {
  // private static final Set<ModuleDrop> allModuleDrops = new
  // HashSet<ModuleDrop>();
  // private static final Map<IRNode, ModuleDrop> irToModule =
  // new HashMap<IRNode, ModuleDrop>();

  private static final String DS_ERR_MODULE_WRAPPING_LOOP = "{0} participates in a module wrapping loop.";

  private static final Category DSC_BAD_MODULE_PROMISE = Category.getInstance("Erroneous @module promises");

  private static final Category DSC_OK_MODULE_PROMISE = Category.getInstance("@module promises");

  protected static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");

  private static final Map<IRNode, SimpleModulePromiseDrop> irToSimpleModule = new HashMap<IRNode, SimpleModulePromiseDrop>();
  private static final Map<IRNode, Set<ModuleWrapperPromiseDrop>> irToWrappingModule = new HashMap<IRNode, Set<ModuleWrapperPromiseDrop>>();
  private static final Map<String, Set<ModulePromiseDrop>> nameToModuleDecls = new HashMap<String, Set<ModulePromiseDrop>>();
  private static final Map<IRNode, ModuleScopePromiseDrop> irToModuleScope = new HashMap<IRNode, ModuleScopePromiseDrop>();

  // private static final Set<ModuleDrop> newModuleDrops = new
  // HashSet<ModuleDrop>();

  protected Collection<ModuleModel> declaredModules;
  protected Collection<String> claimsToWrap;
  protected String image = null;
  final protected String modName;

  protected boolean badDecl = false;
  protected boolean badPlacement = false;

  protected IRNode modPromiseIR = null;

  // ModulePromiseDrop

  ModulePromiseDrop(ModuleChoiceNode mcn, String name) {
    super(mcn);
    declaredModules = new HashSet<ModuleModel>(2);
    claimsToWrap = new HashSet<String>(0);
    modName = name.intern();
    setCategory(JavaGlobals.MODULE_CAT);
  }

  public static ModuleWrapperPromiseDrop buildModuleWrapperDrop(ModuleChoiceNode mcn) {
    final ModuleWrapperPromiseDrop res = new ModuleWrapperPromiseDrop(mcn);
    final ModulePromiseDrop resAsMPD = res;
    final IRNode where = mcn.getPromisedFor();
    final String name = mcn.getModPromise().getModuleName();
    // res.modPromiseIR = modsIR;

    // res.setNodeAndCompilationUnitDependency(where);
    synchronized (ModulePromiseDrop.class) {
      Set<ModuleWrapperPromiseDrop> dropsHere = irToWrappingModule.get(where);
      if (dropsHere == null) {
        dropsHere = new HashSet<ModuleWrapperPromiseDrop>();
        irToWrappingModule.put(where, dropsHere);
      }
      dropsHere.add(res);
      Set<ModulePromiseDrop> dropsWithThisName = nameToModuleDecls.get(resAsMPD.modName);
      if (dropsWithThisName == null) {
        dropsWithThisName = new HashSet<ModulePromiseDrop>();
        nameToModuleDecls.put(name, dropsWithThisName);
      }
      dropsWithThisName.add(res);
      // newModuleDrops.add(res);
    }

    StringBuilder sb = new StringBuilder();
    Collection<String> ctw = resAsMPD.claimsToWrap;
    for (String aModName : mcn.getModWrapper().getWrappedModuleNames()) {
      ctw.add(aModName.intern());
      sb.append(aModName);
    }

    resAsMPD.image = "@module " + resAsMPD.modName + " contains " + sb.toString();
    res.setMessage(resAsMPD.image);

    // asdfasdf
    // res.declaredModules.add(ModuleModel.confirmDrop(name, promise));

    // for (ModuleModel model : res.declaredModules) {
    // model.getMyResultDrop().addTrustedPromise(res);
    // }
    return res;
  }

  public static ModuleScopePromiseDrop buildModuleScopeDrop(ModuleChoiceNode mcn) {
    final ModuleScopePromiseDrop res = new ModuleScopePromiseDrop(mcn);
    final ModulePromiseDrop resAsMPD = res;
    final IRNode where = mcn.getPromisedFor();
    final String name = mcn.getModScope().getModuleName();

    return res;
  }

  public static ModulePromiseDrop buildModulePromiseDrop(ModuleChoiceNode a) {
    if (a.getModPromise() != null) {
      return ModulePromiseDrop.buildModuleDrop(a);
    } else if (a.getModScope() != null) {
      return ModulePromiseDrop.buildModuleScopeDrop(a);
    } else {
      return ModulePromiseDrop.buildModuleWrapperDrop(a);
    }
  }

  public static SimpleModulePromiseDrop buildModuleDrop(ModuleChoiceNode mn) {
    final SimpleModulePromiseDrop res = new SimpleModulePromiseDrop(mn);
    final ModulePromiseDrop resAsMPD = res;
    final IRNode where = mn.getPromisedFor();
    final String name = mn.getModPromise().getModuleName();

    // res.setNodeAndCompilationUnitDependency(where);
    // res.declaredModules.addAll(ModuleModel.queryModulesDefinedBy(name));

    resAsMPD.image = "@module " + name;
    res.setMessage(resAsMPD.image + "(Incomplete)");

    synchronized (ModulePromiseDrop.class) {
      irToSimpleModule.put(where, res);
      Set<ModulePromiseDrop> dropsWithThisName = nameToModuleDecls.get(resAsMPD.modName);
      if (dropsWithThisName == null) {
        dropsWithThisName = new HashSet<ModulePromiseDrop>();
        nameToModuleDecls.put(resAsMPD.modName, dropsWithThisName);
      }
      dropsWithThisName.add(res);
      // newModuleDrops.add(res);
    }

    return res;
  }

  public static Collection<ModuleWrapperPromiseDrop> findWrappingDecl(final String declaredName, final String wrapsName) {
    List<ModuleWrapperPromiseDrop> res = new LinkedList<ModuleWrapperPromiseDrop>();
    Set<Set<ModuleWrapperPromiseDrop>> safeSets = new HashSet<Set<ModuleWrapperPromiseDrop>>();
    Set<ModuleWrapperPromiseDrop> safeMods = new HashSet<ModuleWrapperPromiseDrop>();
    synchronized (ModulePromiseDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleWrapperPromiseDrop> ss : safeSets) {
        safeMods.addAll(ss);
      }
    }

    for (ModuleWrapperPromiseDrop mod : safeMods) {
      if (mod.modName.equals(declaredName)) {
        if (mod.claimsToWrap.contains(wrapsName)) {
          res.add(mod);
        }
      }
    }

    return res;
  }

  private static void startChecks() {
    Set<Set<ModuleWrapperPromiseDrop>> safeSets = new HashSet<Set<ModuleWrapperPromiseDrop>>();
    Collection<ModulePromiseDrop> safeModules = new LinkedList<ModulePromiseDrop>();
    synchronized (ModulePromiseDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleWrapperPromiseDrop> ss : safeSets) {
        safeModules.addAll(ss);
      }
      safeModules.addAll(irToSimpleModule.values());
    }

    for (ModulePromiseDrop md : safeModules) {
      md.badDecl = false;
      md.badPlacement = false;
    }
  }

  private void markBadWrapping() {
    Set<ModulePromiseDrop> dropsWithThisName = nameToModuleDecls.get(modName);
    if (dropsWithThisName == null)
      return;

    for (ModulePromiseDrop md : dropsWithThisName) {
      md.badDecl = true;
    }

    // build the error here!
    ResultDrop rd = ModuleAnalysisAndVisitor.makeResultDrop(getNode(), this, false, DS_ERR_MODULE_WRAPPING_LOOP, this.toString());
    // rd.setCategory(DSC_BAD_MODULE_PROMISE);
    LOG.severe(toString() + " participates in a wrapping loop!");
  }

  protected void wrappingLoopCheck(Set<String> outers) {
    if (outers.contains(this.modName)) {
      // loop in wrapping!
      if (!badDecl) {
        // don't mark more than one error...
        markBadWrapping();
      }
    }
    outers.add(modName);
    for (String wrappee : claimsToWrap) {
      Set<ModulePromiseDrop> wrappeeDrops = nameToModuleDecls.get(wrappee);
      if (wrappeeDrops != null) {
        for (ModulePromiseDrop aMD : wrappeeDrops) {
          if (!aMD.badDecl && aMD != this) {
            aMD.wrappingLoopCheck(outers);
          }
        }
      }
    }
    outers.remove(modName);
  }

  private static void checkWrappings() {

    Collection<ModuleWrapperPromiseDrop> safeWrappingModules = new LinkedList<ModuleWrapperPromiseDrop>();
    Set<Set<ModuleWrapperPromiseDrop>> safeSets = new HashSet<Set<ModuleWrapperPromiseDrop>>();
    synchronized (ModulePromiseDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleWrapperPromiseDrop> ss : safeSets) {
        safeWrappingModules.addAll(ss);
      }
    }
    for (ModuleWrapperPromiseDrop md : safeWrappingModules) {
      // 1st check: does any wrapping module decl claim to define a module that
      // is
      // already defined as a simple (dotted) module that is not a leaf module?
      ModuleModel model = ModuleModel.query(md.modName);
      // if it's there, and has no parent
      if (model != null && !model.changeToWrapperMod(md.getAAST().getModWrapper())) {
        // error case.
        md.badDecl = true;
      }

      // do we have a wrapping loop?
      Set<String> outers = new HashSet<String>();
      md.wrappingLoopCheck(outers);
    }
  }

  /**
   * Build the moduleModel drops, including only those that pass the sanity
   * checks in checkWrappings.
   * 
   */
  public static void buildModuleModels() {
    startChecks();

    Collection<SimpleModulePromiseDrop> safeSimpleModules = new LinkedList<SimpleModulePromiseDrop>();
    synchronized (ModulePromiseDrop.class) {
      safeSimpleModules.addAll(irToSimpleModule.values());
    }

    Collection<ModuleModel> simples = new LinkedList<ModuleModel>();
    for (SimpleModulePromiseDrop md : safeSimpleModules) {
      ModuleModel mm = ModuleModel.confirmDrop(md.modName);
      simples.addAll(ModuleModel.queryModulesDefinedBy(md.modName));
      md.addDeponents(simples);
      simples.clear();
      md.setMessage(md.image);
      ResultDrop rd = mm.getMyResultDrop();
      rd.setConsistent();
      // rd.setCategory(JavaGlobals.MODULE_CAT);
    }
    safeSimpleModules.clear();

    checkWrappings();

    Collection<ModuleWrapperPromiseDrop> safeWrappingModules = new LinkedList<ModuleWrapperPromiseDrop>();
    Set<Set<ModuleWrapperPromiseDrop>> safeSets = new HashSet<Set<ModuleWrapperPromiseDrop>>();
    synchronized (ModulePromiseDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleWrapperPromiseDrop> ss : safeSets) {
        safeWrappingModules.addAll(ss);
      }
    }

    Collection<ModuleModel> wraps = new LinkedList<ModuleModel>();
    for (ModuleWrapperPromiseDrop mwd : safeWrappingModules) {
      ModuleModel mm = ModuleModel.confirmDrop(mwd.modName, mwd.getAAST().getModWrapper());
      mm.setMessage(mwd.getMessage());
      wraps.addAll(ModuleModel.queryModulesDefinedBy(mwd.modName));
      mwd.addDeponents(wraps); // do we really want to do this?? asdfasdf
      wraps.clear();
      mwd.setMessage(mwd.image);
      ResultDrop rd = mm.getMyResultDrop();
      rd.setConsistent();
      // rd.setCategory(JavaGlobals.MODULE_CAT);
    }
    safeWrappingModules.clear();
  }

  // public static Collection<ModulePromiseDrop> findModuleDrop(final IRNode
  // where) {
  // IRNode cu = VisitUtil.getEnclosingCompilationUnit(where);
  // if (cu == null) {
  // cu = where;
  // }
  // IRNode pd = CompilationUnit.getPkg(cu);
  //
  // // Set<ModuleDrop> res;
  // if (pd != null) {
  // SimpleModulePromiseDrop t = irToSimpleModule.get(pd);
  // if (t == null) {
  // return irToWrappingModule.get(pd);
  // } else {
  // return Collections.singleton(t);
  // }
  // } else {
  // return null;
  // }
  //
  // }

  public static Collection<ModuleWrapperPromiseDrop> findModuleWrapperDrops(final IRNode where) {
    IRNode cu = VisitUtil.getEnclosingCompilationUnit(where);
    if (cu == null) {
      cu = where;
    }
    IRNode pd = CompilationUnit.getPkg(cu);

    // Set<ModuleDrop> res;
    if (pd != null) {
      return irToWrappingModule.get(pd);
    } else {
      return null;
    }
  }

  public static SimpleModulePromiseDrop findModuleDrop(final IRNode where) {
    IRNode cu = VisitUtil.getEnclosingCompilationUnit(where);
    if (cu == null) {
      cu = where;
    }
    IRNode pd = CompilationUnit.getPkg(cu);

    if (pd != null) {
      return irToSimpleModule.get(pd);
    } else {
      return null;
    }
  }

  public static Collection<ModulePromiseDrop> findModuleDrops(final IRNode where) {
    SimpleModulePromiseDrop smpd = findModuleDrop(where);
    if (smpd != null) {
      return Collections.singleton((ModulePromiseDrop) smpd);
    } else {
      Collection<ModuleWrapperPromiseDrop> t = findModuleWrapperDrops(where);
      Collection<ModulePromiseDrop> res = new ArrayList<ModulePromiseDrop>(t.size());
      res.addAll(t);
      return res;
    }
  }

  public static void buildModuleDropResults() {
    List<ModulePromiseDrop> allMDs = new ArrayList<ModulePromiseDrop>(nameToModuleDecls.values().size());
    synchronized (ModulePromiseDrop.class) {
      for (Set<ModulePromiseDrop> mds : nameToModuleDecls.values()) {
        allMDs.addAll(mds);
      }
    }
    for (ModulePromiseDrop mod : allMDs) {
      if (!mod.badDecl) {
        ResultDrop rd = ModuleAnalysisAndVisitor.makeResultDrop(mod.getNode(), mod, true, mod.getMessage());
        // rd.setCategory(DSC_OK_MODULE_PROMISE);
      }
    }

  }

  private static DropPredicate definingDropPred = new DropPredicate() {
    public boolean match(IDrop d) {
      if (d instanceof Drop) {
        Drop rd = (Drop) d;
        return (rd.isValid()) && d.instanceOfIRDropSea(CUDrop.class) || d.instanceOfIRDropSea(BinaryCUDrop.class);
      } else
        return false;
    }
  };

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    synchronized (ModulePromiseDrop.class) {
      if (hasMatchingDeponents(definingDropPred)) {
        // Our defining module is still valid, so don't invalidate yet...
        return;
      }
      irToSimpleModule.remove(getNode());
      // irToWrappingModule.remove(getNode());
      Set<ModuleWrapperPromiseDrop> dropsHere = irToWrappingModule.get(getNode());
      if (dropsHere != null) {
        dropsHere.remove(this);
      }
      Set<ModulePromiseDrop> dropsWithThisName = nameToModuleDecls.get(modName);
      if (dropsWithThisName != null) {
        dropsWithThisName.remove(this);
      }

      // newModuleDrops.remove(this);
    }
    super.deponentInvalidAction(invalidDeponent);
  }

  public static void moduleDropPrePost() {
    // newModuleDrops.clear();
  }

  public static boolean thereAreModules() {
    return !((nameToModuleDecls == null) || nameToModuleDecls.isEmpty());
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return image;
  }

  /**
   * @return Returns the badPlacement.
   */
  public boolean isBadPlacement() {
    return badPlacement;
  }

  /**
   * @param badPlacement
   *          The badPlacement to set.
   */
  public void setBadPlacement(boolean badPlacement) {
    this.badPlacement = badPlacement;
  }

  public String getModName() {
    return modName;
  }

}
