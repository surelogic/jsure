package com.surelogic.dropsea.ir.drops.modules;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ModuleWrapperNode;
import com.surelogic.analysis.modules.ModuleAnalysisAndVisitor;
import com.surelogic.annotation.rules.ModuleRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.ModelDrop;
import com.surelogic.dropsea.ir.drops.threadroles.IThreadRoleDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.API;
import edu.cmu.cs.fluid.util.QuickProperties;

/**
 * Drop representing a module, suitable for promise and result drops to depend
 * upon. Created and invalidated by the eAST to fAST converter.
 * 
 * @see edu.cmu.cs.fluid.analysis.util.ConvertToIR
 */
public final class ModuleModel extends ModelDrop implements IThreadRoleDrop {

  private static final Logger LOG;

  private static Map<String, ModuleModel> cache;

  private static Set<ModuleModel> childrenOfWorld; 
  
  private static Drop dependOn = null;

  // private static Set<IRNode> visibleInWorld = null;
  private static final ModuleModel theWorldDrop;
  public final String name;
  private String enclosedModuleNames;
  private final Set<ModuleModel> enclosedModuleModels;
  
  private boolean containsCode = false;

 // final String module;

  private ModuleModel parentModule = null;

  private Set<IRNode> moduleAPI = new HashSet<IRNode>();

  private Collection<ModuleModel> children = new HashSet<ModuleModel>(0);

  private Set<IRNode> visibleInModule = null;

  private Map<IRNode, ModuleModel> wishIWere = null;
  
  private ResultDrop myRD = null;
  
  private static boolean moduleInformationIsConsistent;
  
  public static boolean fakingVis;

  public static final QuickProperties.Flag fakingVisFlag; 

  private int treeDepth = -1;
  
  private static final String DS_BAD_EXPORT_INFO;
  private static final String DS_BAD_EXPORT;
  
  private static final String DS_BAD_MODULE_ENCLOSURE ; 
  
  private static final Category DSC_BAD_MODULE_PROMISE ;
  
  static {
    LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");
    cache = new HashMap<String, ModuleModel>();
    childrenOfWorld = new HashSet<ModuleModel>();
    theWorldDrop = new ModuleModel();
    theWorldDrop.setMessage("The world");
     moduleInformationIsConsistent = true;
    
    DS_BAD_EXPORT_INFO = 
        "Export drop may only export entities that are already exported from a child module";
    DS_BAD_EXPORT =
        "{0} not legal because {1} is not part of the interface of any child module";
  
    DS_BAD_MODULE_ENCLOSURE = 
        "Bad module enclosure: {0} should not contain {1}";
  
    DSC_BAD_MODULE_PROMISE =
        Category.getInstance("Erroneous @module promises");
    
    fakingVisFlag = new QuickProperties.Flag(LOG, "fluid.fakingvis", "FakingVis");
    fakingVis = fakingVisIsOn();
  }
  
  public static boolean fakingVisIsOn() {
    return QuickProperties.checkFlag(fakingVisFlag);
  }

  /**
   * <code>ModuleNum</code> holds the <code>int</code> encoding of the
   * Module that this CU is part of. The value is just a cookie to indicate the
   * actual module. This is a hack to get Dean's module experiments on the air.
   */
  public int ModuleNum = -1;

  /**
   * Build a ModuleModel for a simple module annotation.
   * @param name
   */
  private ModuleModel(String name) {
	super(null);
    this.name = name;
    enclosedModuleNames = null;
    enclosedModuleModels = new HashSet<ModuleModel>(0);
    synchronized(ModuleModel.class) {
      cache.put(name, this);
    }
    setCategory(JavaGlobals.MODULE_CAT);
    dependOn.addDependent(this);
  }
  
  /**
   * Build a ModuleModel for a wrapping module annotation.
   * @param name The wrapping module
   * @param thePromise The IR of the actual promise, so we can pull it apart and build the 
   * pieces.
   */
  private ModuleModel(String name, ModuleWrapperNode thePromise) {
    super(null);	  
    this.name = name;
   
    StringBuilder sb = new StringBuilder();
    enclosedModuleModels = new HashSet<ModuleModel>();
    boolean first = true;
    for (String wmName: thePromise.getWrappedModuleNames()) {
//        enclosedModuleModels.add(mod); // don't add wrapped modules to eMM here!
//      This is handled in the factory method.
      if (!first) {
        sb.append(", ");
      } else {
        first = false;
      }
      sb.append(wmName);
    }
    enclosedModuleNames = sb.toString();

    synchronized(ModuleModel.class) {
      cache.put(name, this);
    }
    setCategory(JavaGlobals.MODULE_CAT);
    dependOn.addDependent(this);
  }

  /**
   * Solely for the purpose of creating the ModuleModel for theWorldDrop. Should
   * never be used anywhere else!
   */
  private ModuleModel() {
	super(null);
	name = "The World";
    enclosedModuleNames = null;
    enclosedModuleModels = null;
    setCategory(JavaGlobals.MODULE_CAT);
    setMessage(12, "(Module \"The World\")");
  }

  /**
   * Looks up the drop corresponding to the given name.
   * 
   * @param name
   *          the name of the module
   * @return the corresponding drop, or <code>null</code> if a drop does not
   *         exist.
   */
  public static ModuleModel query(String name) {
    if (name == null) { return null; }
    synchronized(ModuleModel.class) {
      ModuleModel d = cache.get(name);
      if (d == null) { return null; }
      if (d.isValid()) { return d; }
      cache.remove(name);
    }
    return null;
  }

  /**
   * Create it if it does not exist
   */
  public synchronized static ModuleModel confirmDrop(String name) {
    ModuleModel d = query(name);
    if (d == null) {
      d = new ModuleModel(name);
      d.setMessage("Module " + name);
      d.setParents();
      dependOn.addDependent(d);
    }
    return d;
  }
  
  public synchronized static ModuleModel confirmDrop(final String name,
                                                    final ModuleWrapperNode thePromise) {
    ModuleModel d = query(name);
    if (d == null) {
      d = new ModuleModel(name, thePromise);
      // fill in the enclosedModuleModels set here!!!
      for (String emm: thePromise.getWrappedModuleNames()) {
        ModuleModel mm = cache.get(emm);
        if (mm == null) {
          // the named module didn't have a model yet, so make one.
          mm = confirmDrop(emm);
        }
        d.enclosedModuleModels.add(mm);
      }
      d.setMessage("Module " + d.name + " contains " + d.enclosedModuleNames);
    }
    return d;
  }
  
  public static Collection<ModuleModel> queryModulesDefinedBy(final String name) {
    int dotIndex = name.lastIndexOf('.', name.length());
    final Collection<ModuleModel> res = new HashSet<ModuleModel>();
    ModuleModel mod;
    while (dotIndex > 0) {
      mod = query(name.substring(0, dotIndex));
      if (mod != null) {
        res.add(mod);
      }
      dotIndex = name.lastIndexOf('.', dotIndex-1);
    }
    mod = query(name);
    if (mod != null) {
      res.add(mod);
    }
    return res;
  }

  private void setParents() {
    if (!this.isValid()) { return; }

    int dotIndex = name.lastIndexOf('.', name.length());
    ModuleModel child = this;

    while (dotIndex > 0) {
      final ModuleModel parent = confirmDrop(name.substring(0, dotIndex));
      if ((child.parentModule != null) && (parent != child.parentModule)
          && (child.parentModule.isValid())) {
        LOG.severe("Trying to set parent of " + child.name + " to \""
            + parent.name + "\"; it is currently \"" + child.parentModule.name
            + '"');
      }
      child.parentModule = parent;
      parent.children.add(child);
      child = parent;
      dotIndex = name.lastIndexOf('.', dotIndex-1);
    }
    if (child == null) {
      LOG.severe("null child in setParents");
    } else {
      child.parentModule = null;
      synchronized(ModuleModel.class) {
        childrenOfWorld.add(child);
      }
    }
  }

  public static void processWrapperModParents() {
    boolean problem = false;
    Collection<ModuleModel> safeValues = null;
    synchronized (ModuleModel.class) {
      safeValues = new HashSet<ModuleModel>(cache.values().size());
      for (ModuleModel mod : cache.values()) {
        if (mod.isWrapperMod()) {
          safeValues.add(mod);
        }
      }
    }
    
    // safeValues now contains only wrapper modules!  Furthermore, the moduleAPI of
    // each wrapper module contains only @export-ed IRNodes.
    for (ModuleModel mod : safeValues) {
      for (ModuleModel encMD : mod.enclosedModuleModels) {
        if (encMD.parentModule == null) {
          encMD.parentModule = mod;
          mod.children.add(encMD);
        } else if (encMD.parentModule != mod) {
//          LOG.severe("BAD module enclosure: " +mod.name+ " should not contain " +encMD.name);
          problem = true;
          ResultDrop rd = mod.getMyResultDrop();
          // find all ModuleDrop that claim that mod encloses encMD
          for (ModuleWrapperPromiseDrop aModDecl : ModulePromiseDrop.findWrappingDecl(mod.name, encMD.name)) {
            rd = ModuleAnalysisAndVisitor.makeResultDrop(aModDecl.getNode(),
                                                         aModDecl, false, 
                                                         DS_BAD_MODULE_ENCLOSURE, 
                                                         mod.name, encMD.name);
            //rd.setCategory(DSC_BAD_MODULE_PROMISE);
            moduleInformationIsConsistent = false;
          }
          
        }
      }
      if (!problem) {
        mod.getMyResultDrop().setConsistent();
      }
    }
    
    safeValues.clear();
    safeValues.addAll(childrenOfWorld);
    for (ModuleModel mod : safeValues) {
      if (!problem && childrenOfWorld.contains(mod) && (mod.parentModule != null)) {
        ModuleModel cursor = mod;
        while (cursor.parentModule != null) {
          cursor = cursor.parentModule;
        }
        childrenOfWorld.remove(mod);
        childrenOfWorld.add(cursor);
      }
    }
    
    for (ModuleModel mod : childrenOfWorld) {
      if (mod.isWrapperMod()) {
        problem = problem || mod.fixWrapperAPIs();
      }
    }
  }
  
  /** fix up wrapper module APIs.
   * @return <code>true</code> if everything is OK.
   */
  private boolean fixWrapperAPIs() {
    boolean allOK = true;
    for (ModuleModel mod : children) {
      if (mod.isWrapperMod()) {
        boolean localOK = mod.fixWrapperAPIs();
        allOK = allOK && localOK;
      }
    }
    
    Set<IRNode> unionOfChildAPIs = new HashSet<IRNode>();
    Set<IRNode> origModuleAPI = new HashSet<IRNode>();
    origModuleAPI.addAll(moduleAPI);
    for (ModuleModel child : children) {
      unionOfChildAPIs.addAll(child.moduleAPI);
    }
    
    moduleAPI.retainAll(unionOfChildAPIs); 
    
    origModuleAPI.removeAll(moduleAPI);
    
    if (!origModuleAPI.isEmpty()) {
      // one or more of the original moduleAPI things were removed as part of the
      // fixup process.  That means that their @export annos must have been bogus.
      for (IRNode where : origModuleAPI) {
         Set<ExportDrop> exps = ExportDrop.findExportDrop(where, name);
         for (ExportDrop ed : exps) {
           ResultDrop rd = 
             ModuleAnalysisAndVisitor.makeResultDrop(ed.getNode(), ed, false,
                                                     DS_BAD_EXPORT, 
                                                     ed.toString(), 
                                                     ModuleAnalysisAndVisitor.javaName(where));
           rd.addSupportingInformation(null, DS_BAD_EXPORT_INFO);
         }
      }
    }
    return allOK;
  }

  /**
   * Compute for each leaf module the set of javaThings from other modules that
   * are visible inside the module. Note that these sets DO NOT INCLUDE any
   * IRNodes for javaThings contained in TheWorld and not in any other module.
   * All visibleInModule sets contain only javaThings that are declared to be
   * part of the API of some non-default module.
   */
  public static void computeVisibles() {
    theWorldDrop.visibleInModule = new HashSet<IRNode>();
    for (ModuleModel mod : childrenOfWorld) {
      theWorldDrop.visibleInModule.addAll(mod.moduleAPI);
    }

    for (ModuleModel mod : childrenOfWorld) {
      mod.computeVisibles(theWorldDrop.visibleInModule, 0);
    }
  }

  private void computeVisibles(final Set<IRNode> fromParentAndSiblings,
      int treeDepth) {
    if (wishIWere != null) {
      wishIWere.clear();
    }

    this.treeDepth = treeDepth;

//    if ((children == null) || (children.isEmpty())) {
      // this is a leaf, so actually store the set.
      if (visibleInModule == null) {
        visibleInModule = new HashSet<IRNode>(fromParentAndSiblings.size());
      }
      visibleInModule.addAll(fromParentAndSiblings);
//    } else {
      final Set<IRNode> tVis = new HashSet<IRNode>(fromParentAndSiblings.size());
      tVis.addAll(fromParentAndSiblings);
      for (ModuleModel mod : children) {
        tVis.addAll(mod.moduleAPI);
      }
      final int newTreeDepth = treeDepth + 1;
      for (ModuleModel mod : children) {
        mod.computeVisibles(tVis, newTreeDepth);
      }
//    }
  }

  public static ModuleModel getModuleDrop(final IRNode node) {
    final ModulePromiseDrop modDecl = ModuleRules.getModuleDecl(node);
    if (modDecl == null) { return theWorldDrop; }

    final String modName = modDecl.getModName();
    final ModuleModel res = query(modName);
    return res;
  }
  
  
  public static boolean sameNonWorldModule(final IRNode n1, final IRNode n2) {
    if (n1 == n2) return true;

    final ModuleModel n1Mod = ModuleModel.getModuleDrop(n1);
    final ModuleModel n2Mod = ModuleModel.getModuleDrop(n2);
    return n1Mod == n2Mod && !n1Mod.moduleIsTheWorld();
  }

  /**
   * Check whether javaThing should be visible from within this module. Does not
   * consider Java visibility rules, only Module-based visibility.
   * 
   * This method produces valid results only when called AFTER computation of
   * visibility is complete.
   * 
   * @param javaThing
   *          The java entity being referred to from within this module.
   * @return true, if javaThing should be visible, false otherwise.
   */
  public boolean moduleVisibleFromHere(final IRNode javaThing) {
    ModuleModel hisModule = getModuleDrop(javaThing);

    if ((hisModule == this) || (hisModule == theWorldDrop)) {
      return true;
    } else {
      return visibleInModule.contains(javaThing);
    }
  }

  public boolean moduleIsTheWorld() {
    return this == theWorldDrop;
  }
  
  /**
   * Having found a VIS declaration on javaThing somewhere in module this.
   * Go add javaThing to the API of all the right modules. The "right modules" are 
   * everything from this up the parentModule chain to refdModule.
   * 
   * @param javaThing
   *          The IRNode for the java thingy marked as VIS
   * @param refdModule
   *          The ModuleModel for the (leaf) module that contains javaThing
   *          (could be null, if javaThing lives in "TheWorld".
   */
  public boolean setAPI(IRNode javaThing, ModuleModel refdModule) {
    if ((refdModule == null) || (javaThing == null)) return  false;

    ModuleModel tMod = this;
    tMod.moduleAPI.add(javaThing);
    while (tMod != refdModule) {
      tMod = tMod.parentModule;

      if (tMod == null) {
        final String jName;
        final JavaOperator jThingOp = JavaNode.getOp(javaThing);
        if (MethodDeclaration.prototype.includes(jThingOp) ||
            ConstructorDeclaration.prototype.includes(jThingOp)) {
          jName = JavaNames.genMethodConstructorName(javaThing);
        } else if (ClassDeclaration.prototype.includes(jThingOp) ||
            InterfaceDeclaration.prototype.includes(jThingOp)) {
          jName = JavaNames.getTypeName(javaThing);
        } else {
          jName = "???";
        }
        LOG
            .severe("setAPI reached root of module tree without encountering THIS\n"
                + "  started from "
                + refdModule.name
                + " looking for module "
                + name
                + " while exporting "
                + jName);
        return false;
      }
      tMod.moduleAPI.add(javaThing);
    }
    return true;
  }
  
  /** Record that javaThing is declared to be part of the API of forModuleDecl.
   * <code>this</code> will either be the ModuleModel for the leaf module enclosing
   * <code>javaThing</code> (for vis declarations), or will be the ModuleModel for
   * <code>forModuleDecl</code> (for export declarations).
   * @param javaThing The java entity that is to be marked as API.
   * @param forModuleDecl IR for the module that javaThing is API of.
   * @return <code>true</code> if and only if the setting of APIness is successful.
   */
  public boolean recordAPI(IRNode javaThing, IRNode forModuleDecl) {
    ModuleModel mod = query(API.getId(forModuleDecl));
    if (mod == null) return false;
    return setAPI(javaThing, mod);
  }
  
  /** Predicate to check whether javaThing is part of the API of this module.
   * @param javaThing The javaThing to check. It need not be part of this module in
   * truth, but should not be null.
   * @return true if and only if javaThing really is part of the API of this module.
   */
  public boolean isAPI(final IRNode javaThing) {
    if (moduleIsTheWorld()) {
      // everything in TheWorld "isAPI" by definition.
      return true;
    } else if (moduleAPI == null) {
      return false;
    }
    
    boolean res = moduleAPI.contains(javaThing);
    
    if (fakingVis && !res && (wishIWere != null)) {
      res = (wishIWere.get(javaThing) != null);
    }
    return res;
  }
  
  public boolean isWrapperMod() {
    return (enclosedModuleNames != null);
  }
  
  /** Update this moduleModel to be a wrapperModule. Note that it is an error to 
   * ever update TheWorld.
   * @param modulesIR The IR of the set of modules to wrap
   * @return <code>true</code> if and only if the change is successful.
   */
  public boolean changeToWrapperMod(ModuleWrapperNode modulesIR) {
    if (moduleIsTheWorld()) {
      return false;
    }
      // all we need to do is add the new modules to the ones that are already
      // defined.
    boolean first;
    StringBuilder sb = new StringBuilder();
    if (enclosedModuleNames != null && !"".equals(enclosedModuleNames)) {
      sb.append(enclosedModuleNames);
      sb.append(", ");
      first = false;
    } else {
      first = true;
    }
    for (String wmName : modulesIR.getWrappedModuleNames()) {
      enclosedModuleModels.add(cache.get(wmName));
      sb.append(wmName);
      if (!first) {
        sb.append(", ");
        first = false;
      }
    }

    enclosedModuleNames = sb.toString();
    
    return true;
  }
  
  /** Predicate to check whether javaThing is part of the API of the smallest 
   * enclosing module. Note that <b>everything</b> in TheWorld is considered to be
   * part of its API for this purpose. javaThing need not be enclosed within a module
   * (in this case it is part of "TheWorld"), but should not be <code>null</code>
   * @param javaThing The java entity to check.
   * @return <code>true</code> if and only if javaThing is in fact part of the API 
   * of its smallest enclosing module.
   */
  public static boolean isAPIinParentModule(final IRNode javaThing) {
    final ModuleModel mod = getModuleDrop(javaThing);
    
    if (mod == null || mod.moduleIsTheWorld()) {
      return true;
    }
    return mod.isAPI(javaThing);
  }

  /**
   * Found a reference to javaThing somewhere in refingMod. Update this module's
   * wishIWere to note that we'd like javaThing to be declared vis in the module
   * that is innermost parent of this such that the putative vis would make
   * javaThing visible in refingMod.
   * 
   * Since we know that code is found only in leaf modules, and further that
   * anything in a module can see the rest of the module, we know that either (a)
   * we have a common parent up the tree, so javaThing should become part of the API
   * of its (recursive) parent that is a child of the common parent, or (b) we
   * have no common parent up to the root, in which case we want javaThing to be
   * part of the API of its top-level parent. There must be a module somewhere up
   * the tree that meets this requirement.
   * 
   * @param javaThing
   *          a Java entity being referred to.
   * @param refingMod
   *          the module containing the reference.
   */
public static void updateWishIWere(IRNode javaThing, ModuleModel refingMod) {
  final ModuleModel jtMod = getModuleDrop(javaThing);
    if ((javaThing == null) || (refingMod == jtMod)) {
      // nothing to do.
      return;
    }
    
    ModuleModel jtModCursor = jtMod;
    if ((refingMod == null) || (jtMod.treeDepth == 0) || refingMod.moduleIsTheWorld()) {
      // javaThing should be part of the API of the top-level parent of this
      // module
      while (jtModCursor.treeDepth > 0) {
        jtModCursor = jtModCursor.parentModule;
      }
     
      jtMod.addToWishIWere(javaThing, jtModCursor);
      
    } else {
      ModuleModel refModCursor = refingMod;
      // find the module we WANT javaThing to be API of
      while (jtModCursor.treeDepth >= 0) {
        while ((refModCursor != null) && refModCursor.treeDepth >= jtModCursor.treeDepth) {
          // move refModCursor up until it points one level above jtModCursor
          // in the module tree.
          refModCursor = refModCursor.parentModule;
        }
        // refModCursor now points to a module one above jtModCursor (or to null, if 
        // jtModCursor.treeDepth == 0).
        if (refModCursor == null) {
          // refModCursor moved up past the top of the tree, which means that the
          // closest common ancestor is TheWorld. This means that we wish javaThing
          // was API in the top-level parent of jtMod. jtModCursor should already
          // be pointing there...
          if (jtModCursor.treeDepth != 0) {
            // ...but it's NOT, so something is VERY ODD.
            LOG.severe("wierd tree in updateWishIWere!");
          }
          jtMod.addToWishIWere(javaThing, jtModCursor);
          return;
        } else if (refModCursor.children.contains(jtModCursor)) {
          // refModCursor is a parent of jtModCursor. That means that anything 
          // marked as API of jtModCursor will be visible in the original refingMod.
          // update the WishIWere accordingly.
          if (jtModCursor.parentModule != refModCursor) {
            // sanity check: refModCursor thinks jtModCursor is one of its children.
            // jtModCursor had better think that refModCursor is it's parent.
            LOG.severe("parent/child broken in module tree, for parent \"" +
                       refModCursor +
                       "\" and child \"" +
                       jtModCursor + '"');
          }
          jtMod.addToWishIWere(javaThing, jtModCursor);
          return;
        } else {
          // refModCursor is not a parent of jtModCursor, even though it is one
          // level higher in the module hierarchy. Move jtModCursor up on level
          // and iterate.
          jtModCursor = jtModCursor.parentModule;
        }
      }
      
      LOG.severe("how did we get here??? Parent \"" +
                 refModCursor +
                 "\", child \"" +
                 jtModCursor + '"');
      jtMod.addToWishIWere(javaThing, jtModCursor);
    }
  }

  /**
   * @param javaThing
   * @param putWhere
   */
  private void addToWishIWere(IRNode javaThing, ModuleModel putWhere) {
    ModuleModel oldMod;
    if (wishIWere == null) {
      wishIWere = new HashMap<IRNode, ModuleModel>();
      oldMod = null;
    } else {
      oldMod = wishIWere.get(javaThing);
    }

    if (oldMod == null) {
      wishIWere.put(javaThing, putWhere);
    } else if (oldMod.treeDepth > putWhere.treeDepth) {
      wishIWere.put(javaThing, putWhere);
    }
  }
  
  public static synchronized List<ModuleModel> getModulesThatWishIWere() {
    List<ModuleModel> res = new LinkedList<ModuleModel>();
    for (ModuleModel md : cache.values()) {
      if (md.wishIWere != null && !md.wishIWere.isEmpty()) {
        res.add(md);
      }
    }
    return res;
  }
  
  public static boolean thereAreModules() {
//    if (cache == null || cache.isEmpty()) return false;
    return true;
  }
  
  /*
  private static DropPredicate definingDropPred = new DropPredicate() {
    public boolean match(Drop d) {
      return 
        d instanceof VisibilityDrop ||
        d instanceof ModuleDrop;
    }    
  };
  */
  
//  public static synchronized void purgeUnusedModuleModels() {
//    Map<String, ModuleModel> newCache = new HashMap<String, ModuleModel>();
//    for (String name : cache.keySet()) {
//      final ModuleModel mod = cache.get(name);
//      
//      boolean moduleDefinedInCode = mod.hasMatchingDependents(definingDropPred);
//      if (moduleDefinedInCode) {
//        newCache.put(name, mod);
//      } else {
//        if (mod.parentModule != null) {
//          mod.parentModule.children.remove(mod);
//        } else {
//          childrenOfWorld.remove(mod);
//        }
//        mod.children.clear();
//        mod.parentModule = null;
//        mod.moduleAPI.clear();
//        mod.moduleAPI = null;
//        mod.visibleInModule.clear();
//        mod.visibleInModule = null;
//        mod.invalidate();
//      }
//    }
//    cache = newCache;
//  }
  
  

  @Override
  public String toString() {
    return "Module Model " + name;
  }

  
  /**
   * @return Returns the wishIWere.
   */
  public Map<IRNode, ModuleModel> getWishIWere() {
    return wishIWere;
  }

  
  /**
   * @return Returns the myRD.
   */
  public  ResultDrop getMyResultDrop() {
    if (myRD == null || !myRD.isValid()) {
      myRD = ModuleAnalysisAndVisitor.makeResultDrop(null, this, true, getMessage());
     // myRD.setCategory(JavaGlobals.MODULE_CAT);
    }
    return myRD;
  }

  
  /**
   * @return Returns the containsCode.
   */
  public boolean isContainsCode() {
    return containsCode;
  }

  
  /**
   * @param containsCode The containsCode to set.
   */
  public void setContainsCode(boolean containsCode) {
    this.containsCode = containsCode;
  }
  
  public boolean isLeafModule() {
    boolean res = moduleIsTheWorld();
    if (!res) {
      res = (children == null || children.isEmpty());
    }
    return res;
  }
  
  public static synchronized Collection<ModuleModel> getAllModuleModels() {
    Collection<ModuleModel> res = new ArrayList<ModuleModel>(cache.values().size());
    res.addAll(cache.values());
    return res;
  }
  
  public static void initModuleModels(Drop dependOn) {
    ModuleModel.dependOn = dependOn;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (parentModule != null) {
      parentModule.children.remove(this);
    } else {
      childrenOfWorld.remove(this);
    }
    children.clear();
    parentModule = null;
    moduleAPI.clear();
//    moduleAPI = null;
    if (visibleInModule != null) {
      visibleInModule.clear();
    }
//    visibleInModule = null;
    invalidate();
    super.deponentInvalidAction(invalidDeponent);
  }

  
  /** This method is valid only AFTER ModuleAnalysis has run.  Calling it at other
   * times may produce bogus results.
   *  
   * @return Returns the childrenOfWorld. 
   */
  public static Set<ModuleModel> getChildrenOfWorld() {
    return Collections.unmodifiableSet(childrenOfWorld);
  }
  
  public ModuleModel getParent() {
    return parentModule;
  }
  
  public Collection<ModuleModel> getChildren() {
    return Collections.unmodifiableCollection(children); 
  }

  public static ModuleModel getTheWorld() {
    return theWorldDrop;
  }
  
  /**
   * @return Returns the moduleInformationIsConsistent.
   */
  public static boolean isModuleInformationIsConsistent() {
    return moduleInformationIsConsistent;
  }

  
  /**
   * @param moduleInformationIsConsistent The moduleInformationIsConsistent to set.
   */
  public static void setModuleInformationIsConsistent(
                                               boolean infoIsConsistent) {
    moduleInformationIsConsistent = infoIsConsistent;
  }
}