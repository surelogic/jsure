/*
 * Created on Oct 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.analysis.modules.ModuleAnalysisAndVisitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.promise.EnclosingModule;
import edu.cmu.cs.fluid.java.promise.Module;
import edu.cmu.cs.fluid.java.promise.Modules;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.BinaryCUDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;


public class ModuleDrop extends PromiseDrop {
//  private static final Set<ModuleDrop> allModuleDrops = new HashSet<ModuleDrop>();
//  private static final Map<IRNode, ModuleDrop> irToModule = 
//    new HashMap<IRNode, ModuleDrop>();
  
  private static final String DS_ERR_MODULE_WRAPPING_LOOP = 
    "{0} participates in a module wrapping loop.";
  
  private static final Category DSC_BAD_MODULE_PROMISE =
    Category.getInstance("Erroneous @module promises");
  
  private static final Category DSC_OK_MODULE_PROMISE =
    Category.getInstance("@module promises");
  
  
  protected static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");
  
  private static final  Map<IRNode, ModuleDrop> irToSimpleModule = 
    new HashMap<IRNode, ModuleDrop>();
  private static final  Map<IRNode, Set<ModuleDrop>> irToWrappingModule = 
    new HashMap<IRNode, Set<ModuleDrop>>();
  private static final Map<String, Set<ModuleDrop>> nameToModuleDecls =
    new HashMap<String, Set<ModuleDrop>>();
  
//  private static final Set<ModuleDrop> newModuleDrops = new HashSet<ModuleDrop>();
  
  private Collection<ModuleModel> declaredModules;
  private Collection<String> claimsToWrap;
  private String image = null;
  final private String modName;
  
  private boolean badDecl = false;
  private boolean badPlacement = false;

  private IRNode modPromiseIR = null;
  
  ModuleDrop(final String name) {
    declaredModules = new HashSet<ModuleModel>(2);
    claimsToWrap = new HashSet<String>(0);
    modName = name.intern();
    setCategory(JavaGlobals.MODULE_CAT);
  }
  
  public static ModuleDrop buildModuleDrop(final IRNode where, 
                                           final String name, 
                                           final IRNode promise) {
    final ModuleDrop res = new ModuleDrop(name);
    final IRNode modsIR = EnclosingModule.getModules(promise);
    res.modPromiseIR = modsIR;
   
    res.setNodeAndCompilationUnitDependency(where);
    synchronized (ModuleDrop.class) {
      Set<ModuleDrop> dropsHere = irToWrappingModule.get(where);
      if (dropsHere == null) {
        dropsHere = new HashSet<ModuleDrop>();
        irToWrappingModule.put(where, dropsHere);
      }
      dropsHere.add(res);
      Set<ModuleDrop> dropsWithThisName = nameToModuleDecls.get(res.modName);
      if (dropsWithThisName == null) {
        dropsWithThisName = new HashSet<ModuleDrop>();
        nameToModuleDecls.put(res.modName, dropsWithThisName);
      }
      dropsWithThisName.add(res);
//      newModuleDrops.add(res);
    }
    
    Iterator<IRNode> modIter = Modules.getModuleIterator(modsIR);
    StringBuilder sb = new StringBuilder();
    if (modIter.hasNext()) {
      do {
        IRNode mod = modIter.next();
        final String aModName = Module.getId(mod);
        res.claimsToWrap.add(aModName.intern());
        sb.append(aModName);
        if (modIter.hasNext()) {
          sb.append(", ");
        }
      } while (modIter.hasNext()); 
    }
    
    res.image = "@module " +name+ " contains " +sb.toString();
    res.setMessage(res.image);

    //asdfasdf
//    res.declaredModules.add(ModuleModel.confirmDrop(name, promise));
 
//    for (ModuleModel model : res.declaredModules) {
//      model.getMyResultDrop().addTrustedPromise(res);
//    }
    return res;
  }
  
  public static ModuleDrop buildModuleDrop(final IRNode where, final String name) {
    final ModuleDrop res = new ModuleDrop(name);
    
    res.setNodeAndCompilationUnitDependency(where);
//    res.declaredModules.addAll(ModuleModel.queryModulesDefinedBy(name));
    
    res.image = "@module " +name;
    res.setMessage(res.image +"(Incomplete)");
    
    synchronized (ModuleDrop.class) {
      irToSimpleModule.put(where, res);
      Set<ModuleDrop> dropsWithThisName = nameToModuleDecls.get(res.modName);
      if (dropsWithThisName == null) {
        dropsWithThisName = new HashSet<ModuleDrop>();
        nameToModuleDecls.put(res.modName, dropsWithThisName);
      }
      dropsWithThisName.add(res);
//      newModuleDrops.add(res);
    }
    
    return res;
  }
  
  public static Collection<ModuleDrop> findWrappingDecl(final String declaredName, 
                                                        final String wrapsName) {
    List<ModuleDrop> res = new LinkedList<ModuleDrop>();
    Set<Set<ModuleDrop>> safeSets = new HashSet<Set<ModuleDrop>>();
    Set<ModuleDrop> safeMods = new HashSet<ModuleDrop>();
    synchronized (ModuleDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleDrop> ss : safeSets) {
        safeMods.addAll(ss);
      }
    }
    
    for (ModuleDrop mod : safeMods) {
      if (mod.modName.equals(declaredName)) {
        if (mod.claimsToWrap.contains(wrapsName)) {
          res.add(mod);
        }
      }
    }
         
    return res;
  }
  
  private static void startChecks() {
    Set<Set<ModuleDrop>> safeSets = new HashSet<Set<ModuleDrop>>();
    Collection<ModuleDrop> safeModules = new LinkedList<ModuleDrop>();
    synchronized (ModuleDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleDrop> ss : safeSets) {
        safeModules.addAll(ss);
      }
      safeModules.addAll(irToSimpleModule.values());
    }
  
    for (ModuleDrop md : safeModules) {
      md.badDecl = false;
      md.badPlacement = false;
    }
  }
  
  private void markBadWrapping() {
    Set<ModuleDrop> dropsWithThisName = nameToModuleDecls.get(modName);
    if (dropsWithThisName == null) return;
    
    for (ModuleDrop md : dropsWithThisName) {
      md.badDecl = true;
    }
    
    // build the error here!
    ResultDrop rd =
      ModuleAnalysisAndVisitor.makeResultDrop(getNode(), this, false, 
                                              DS_ERR_MODULE_WRAPPING_LOOP,
                                              this.toString());
    rd.setCategory(DSC_BAD_MODULE_PROMISE);
    LOG.severe(toString() +" participates in a wrapping loop!");
  }
  
  private void wrappingLoopCheck(Set<String> outers) {
    if (outers.contains(this.modName)) {
      // loop in wrapping!
      if (!badDecl) {
        // don't mark more than one error...
        markBadWrapping();
      }
    }
    outers.add(modName);
    for (String wrappee : claimsToWrap) {
      Set<ModuleDrop> wrappeeDrops = nameToModuleDecls.get(wrappee);
      if (wrappeeDrops != null) {
        for (ModuleDrop aMD : wrappeeDrops) {
          if (!aMD.badDecl && aMD != this) {
            aMD.wrappingLoopCheck(outers);
          }
        }
      }
    }
    outers.remove(modName);
  }
  
  private static void checkWrappings() {
  
    Collection<ModuleDrop> safeWrappingModules = new LinkedList<ModuleDrop>();
    Set<Set<ModuleDrop>> safeSets = new HashSet<Set<ModuleDrop>>();
    synchronized (ModuleDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleDrop> ss : safeSets) {
        safeWrappingModules.addAll(ss);
      }
    }
    for (ModuleDrop md : safeWrappingModules) {
      // 1st check: does any wrapping module decl claim to define a module that is
      // already defined as a simple (dotted) module that is not a leaf module?
      ModuleModel model = ModuleModel.query(md.modName);
      // if it's there, and has no parent
      if (model != null && !model.changeToWrapperMod(md.modPromiseIR)) {
        // error case.
        md.badDecl = true;
      }
      
      // do we have a wrapping loop?
      Set<String> outers = new HashSet<String>();
      md.wrappingLoopCheck(outers);
    }
  }
  
  
  /**
   * Build the moduleModel drops, including only those that pass the sanity checks in
   * checkWrappings.
   * 
   */
  public static void buildModuleModels() {
    startChecks();
    
    Collection<ModuleDrop> safeSimpleModules = new LinkedList<ModuleDrop>();
    synchronized (ModuleDrop.class) {
      safeSimpleModules.addAll(irToSimpleModule.values());
    }
    
    Collection<ModuleModel> simples = new LinkedList<ModuleModel>();
    for (ModuleDrop md : safeSimpleModules) {
      ModuleModel mm = ModuleModel.confirmDrop(md.modName);
      simples.addAll(ModuleModel.queryModulesDefinedBy(md.modName));
      md.addDeponents(simples);
      simples.clear();
      md.setMessage(md.image);
      ResultDrop rd = mm.getMyResultDrop();
      rd.setConsistent();
      rd.setCategory(JavaGlobals.MODULE_CAT);
    }
    safeSimpleModules.clear();
    
    checkWrappings();
    
    Collection<ModuleDrop> safeWrappingModules = new LinkedList<ModuleDrop>();
    Set<Set<ModuleDrop>> safeSets = new HashSet<Set<ModuleDrop>>();
    synchronized (ModuleDrop.class) {
      safeSets.addAll(irToWrappingModule.values());
      for (Set<ModuleDrop> ss : safeSets) {
        safeWrappingModules.addAll(ss);
      }
    }
    
    Collection<ModuleModel> wraps = new LinkedList<ModuleModel>();
    for (ModuleDrop md : safeWrappingModules) {
      ModuleModel mm = ModuleModel.confirmDrop(md.modName, md.modPromiseIR);
      mm.setMessage(md.getMessage());
      wraps.addAll(ModuleModel.queryModulesDefinedBy(md.modName));
      md.addDeponents(wraps); // do we really want to do this?? asdfasdf
      wraps.clear();
      md.setMessage(md.image);
      ResultDrop rd = mm.getMyResultDrop();
      rd.setConsistent();
      rd.setCategory(JavaGlobals.MODULE_CAT);
    }
    safeWrappingModules.clear();
  }
  
  public static Collection<ModuleDrop> findModuleDrop(final IRNode where) {
    IRNode cu = VisitUtil.getEnclosingCompilationUnit(where);
    if (cu == null) {
      cu = where;
    }
    IRNode pd = CompilationUnit.getPkg(cu);
    
    Set<ModuleDrop> res;
    if (pd != null) {
      ModuleDrop t = irToSimpleModule.get(pd);
      if (t == null) {
        Set<ModuleDrop> resSet = irToWrappingModule.get(pd);
        return resSet;
      }
      res = Collections.singleton(t);
    } else {
      res = null;
    }
    return res;
  }

  public static void buildModuleDropResults() {
    List<ModuleDrop> allMDs = new LinkedList<ModuleDrop>();
    synchronized (ModuleDrop.class) {
      for (Set<ModuleDrop> mds : nameToModuleDecls.values()) {
        allMDs.addAll(mds);
      }
    }
    for (ModuleDrop mod : allMDs) {
      if (!mod.badDecl) {
        ResultDrop rd = 
          ModuleAnalysisAndVisitor.makeResultDrop(mod.getNode(), mod, 
                                                  true, mod.getMessage());
        rd.setCategory(DSC_OK_MODULE_PROMISE);
      }
    }
    
  }
  
  private static DropPredicate definingDropPred = new DropPredicate() {
    public boolean match(IDrop d) {
      return (d.isValid()) &&
        d.instanceOf(CUDrop.class) ||
        d.instanceOf(BinaryCUDrop.class);
    }    
  };
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected  void deponentInvalidAction(Drop invalidDeponent) {
    synchronized (ModuleDrop.class) {
      if (hasMatchingDeponents(definingDropPred)) {
        // Our defining module is still valid, so don't invalidate yet...
        return;
      }
      irToSimpleModule.remove(getNode());
//      irToWrappingModule.remove(getNode());
      Set<ModuleDrop> dropsHere = irToWrappingModule.get(getNode());
      if (dropsHere != null) {
        dropsHere.remove(this);
      }
      Set<ModuleDrop> dropsWithThisName = nameToModuleDecls.get(modName);
      if (dropsWithThisName != null) {
        dropsWithThisName.remove(this);
      }
      
//      newModuleDrops.remove(this);
    }
    super.deponentInvalidAction(invalidDeponent);
  }
  
  public static void moduleDropPrePost() {
//    newModuleDrops.clear();
  }
  
  public static boolean thereAreModules() {
    return !((nameToModuleDecls == null) || nameToModuleDecls.isEmpty());
  }

  /* (non-Javadoc)
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
   * @param badPlacement The badPlacement to set.
   */
  public void setBadPlacement(boolean badPlacement) {
    this.badPlacement = badPlacement;
  }

}
