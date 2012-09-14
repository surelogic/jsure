/*
 * Created on Oct 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ExportNode;
import com.surelogic.aast.promise.ModuleAnnotationNode;
import com.surelogic.aast.promise.VisClauseNode;
import com.surelogic.analysis.modules.ModuleAnalysisAndVisitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.WarningDrop;

public abstract class VisibilityDrop<T extends ModuleAnnotationNode> extends PromiseDrop<T> {
  
  protected static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");
  
  protected String image = null;

  // private IRNode promisedOn;

  private static Set<VisibilityDrop<? extends ModuleAnnotationNode>> allVisDrops = new HashSet<VisibilityDrop<? extends ModuleAnnotationNode>>();
//  private static Set<VisibilityDrop> newVisDrops = new HashSet<VisibilityDrop>();

  private static Map<IRNode, Set<VisibilityDrop<? extends ModuleAnnotationNode>>> edMap = new HashMap<IRNode, Set<VisibilityDrop<? extends ModuleAnnotationNode>>>();

  String refdModule;
  
  private static final Category DSC_BAD_VISIBILITY_DECL = 
    Category.getInstance("Erroneous visibility declarations");
  
  private static final Category DSC_GOOD_VISIBILITY_DECL = 
    Category.getInstance("Valid visibility declarations");
  
  private static final Category DSC_WARN_VISIBILITY_DECL = 
    Category.getInstance("Visibility declaration warnings");
  
  private static final String DS_ERROR_VIS_NO_SUCH_MODULE =
    "Named module \"{0}\" in \"{1}\" does not exist";
  
  private static final String DS_ERROR_VIS_WORLD_FROM_MOD =
    "Visibility annotation \"{0}\" exports to \"The World\". Possible internal error";
  
  private static final String DS_WARN_WORLD_VIS = 
    "Visibility declaration {0} adds {1} to the exported interface of module \"The World\". This is redundant.";

  private static final String DS_ERROR_VIS =
    "Visibility declaration {0} refers to a module ({1}) that is not a parent of {2}";
  
  private static final String DS_OK_VIS = 
    "{0}";

  protected VisibilityDrop(T a) {
    super(a);
    if (a instanceof VisClauseNode) {
      refdModule = ((VisClauseNode) a).getModName();
    } else if (a instanceof ExportNode) {
      refdModule = ((ExportNode) a).getToModuleName();
    }
   // setNodeAndCompilationUnitDependency(a.getPromisedFor());
    setCategory(JavaGlobals.MODULE_CAT);
  }

  protected static <T extends VisibilityDrop<? extends ModuleAnnotationNode>> T buildVisDrop(T res) {
    // VisibilityDrop res = new VisibilityDrop(locInIR, modIR);
    res.setResultMessage(12, res.toString());
    synchronized (VisibilityDrop.class) {
      allVisDrops.add(res);
//      newVisDrops.add(res);
      final IRNode locInIR = res.getAAST().getPromisedFor();
      Set<VisibilityDrop<? extends ModuleAnnotationNode>> dropsHere = edMap.get(locInIR);
      if (dropsHere == null) {
        dropsHere = new HashSet<VisibilityDrop<? extends ModuleAnnotationNode>>(2);
        edMap.put(locInIR, dropsHere);
      }
      dropsHere.add(res);
    }
    return res;
  }
  
  public static void visibilityPrePost() {
//    newVisDrops.clear();
  }

//  private static DropPredicate definingDropPred = new DropPredicate() {
//    public boolean match(Drop d) {
//      return (d.isValid()) &&
//        d instanceof CUDrop ||
//        d instanceof BinaryCUDrop;
//    }    
//  };
  
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // can't do the hack below, because we keep links to both the defining module
    // AND the location where we found the visibility info. Even 'though the
    // module may still be live, the CU containing the location may not!
//    if (hasMatchingDeponents(definingDropPred)) {
//      // Our defining module is still valid, so don't invalidate yet...
//      return;
//    }
    synchronized (VisibilityDrop.class) {
      allVisDrops.remove(this);
//      newVisDrops.remove(this);
      Set<VisibilityDrop<? extends ModuleAnnotationNode>> dropsHere = edMap.get(this.getNode());
      if (dropsHere != null) {
        dropsHere.remove(this);
      }
    }
    super.deponentInvalidAction(invalidDeponent);
  }

  protected static Set<VisibilityDrop<? extends ModuleAnnotationNode>> findVisibilityDrops(IRNode promisedFor,
      String modName) {

    Set<VisibilityDrop<? extends ModuleAnnotationNode>> res = new HashSet<VisibilityDrop<? extends ModuleAnnotationNode>>(1);
    if (promisedFor == null || modName == null) { return res; }
    final Set<VisibilityDrop<? extends ModuleAnnotationNode>> pfSet = edMap.get(promisedFor);
    if (pfSet == null) { return res; }
    for (VisibilityDrop<? extends ModuleAnnotationNode> ed : pfSet) {
      if (ed.refdModule.equals(modName)) {
        res.add(ed);
      }
    }
    return res;
  }
  
  public static void checkVisibilityDrops() {
    final Collection<Set<VisibilityDrop<? extends ModuleAnnotationNode>>> safeVDsets;
    synchronized(VisibilityDrop.class) {
      safeVDsets = new ArrayList<Set<VisibilityDrop<? extends ModuleAnnotationNode>>>(edMap.values().size());
      safeVDsets.add(allVisDrops);
    }
    
    for (Set<VisibilityDrop<? extends ModuleAnnotationNode>> vds : safeVDsets) {
      final List<VisibilityDrop<? extends ModuleAnnotationNode>> someVDs;
      synchronized(VisibilityDrop.class) {
        someVDs = new ArrayList<VisibilityDrop<? extends ModuleAnnotationNode>>(vds.size());
        someVDs.addAll(vds);
      }
      for (VisibilityDrop<? extends ModuleAnnotationNode> vd : someVDs) {
        // make sure that each VisibilityDrop names a module that both actually 
        // exists AND is not TheWorld.
        if (!vd.isValid()) continue;
        
        final IRNode where = vd.getNode();
        final ModuleModel containingModule = ModuleModel.getModuleDrop(where);
        if (containingModule == null) {
          LOG.severe("Visibility Drop not contained in any module");
        } else {
          containingModule.addDependent(vd);
        }
        
        // start by finding the module named in the anno (if any)
        final ModuleModel refdMod;
        final String reqModName;
        if (vd.refdModule == null) {
          refdMod = ModuleModel.getModuleDrop(where);
          reqModName = refdMod.name;
        } else {
          reqModName = vd.refdModule;
          if ("".equals(reqModName)) {
            refdMod = ModuleModel.getModuleDrop(where);
          } else {
            refdMod = ModuleModel.query(reqModName);
          }
        }
        

        if (refdMod == null) {
          // error: this VD refers to a module that does not exist.
          
          ResultDrop rd = 
            ModuleAnalysisAndVisitor.makeResultDrop(where, vd, false, 
                                                    DS_ERROR_VIS_NO_SUCH_MODULE,
                                                    reqModName, vd.toString());
          rd.setCategory(JavaGlobals.PROMISE_PARSER_PROBLEM);
          
        } else if (refdMod.moduleIsTheWorld()) {
          final ModuleModel enclosingLeafMod = ModuleModel.getModuleDrop(where);
          if (enclosingLeafMod.moduleIsTheWorld()) {
            // warning: no point in @vis or @export on item in TheWorld
            WarningDrop wd = 
              ModuleAnalysisAndVisitor.makeWarningDrop(DSC_WARN_VISIBILITY_DECL,
                                                       where, DS_WARN_WORLD_VIS,
                                                       vd.toString(), 
                                                       JJNode.getInfo(where));
            
          } else {
            // error: somehow managed to say @vis TheWorld or @export TheWorld from
            // a modularized context.  Internal error?
            ResultDrop rd = 
              ModuleAnalysisAndVisitor.makeResultDrop(where, vd, false,
                                                      DS_ERROR_VIS_WORLD_FROM_MOD,
                                                      vd.toString());
            rd.setCategory(DSC_BAD_VISIBILITY_DECL);
            
          }
        } else {
          // VD is good. mark it so.
          
          final ModuleModel enclosingLeafMod = ModuleModel.getModuleDrop(where);
          final ModuleModel baseMod;
          if (vd instanceof VisDrop) {
            baseMod = enclosingLeafMod;
            
          } else if (vd instanceof ExportDrop) {
            baseMod = refdMod;
          } else {
            baseMod = null;  // failFAST!
          }
          if (baseMod.setAPI(where, refdMod)) {
            ResultDrop rd =
              ModuleAnalysisAndVisitor.makeResultDrop(where, vd, true, 
                                                      DS_OK_VIS,
                                                      vd.getMessage());
            
            rd.setCategory(DSC_GOOD_VISIBILITY_DECL);
          } else {
            ResultDrop rd =
              ModuleAnalysisAndVisitor.makeResultDrop(where, vd, false, 
                                                      DS_ERROR_VIS,
                                                      vd.getMessage(),
                                                      refdMod, baseMod);
            
            rd.setCategory(DSC_BAD_VISIBILITY_DECL);
          }
        }
      }
    }
  }

  @Override
  public abstract String toString();

}
