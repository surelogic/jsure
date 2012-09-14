/*
 * Created on Oct 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.analysis.modules.ModuleAnalysisAndVisitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.DropPredicate;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.WarningDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.promise.API;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.BinaryCUDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public abstract class VisibilityDrop extends PromiseDrop {
  
  protected static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");
  
  protected String image = null;

  // private IRNode promisedOn;

  private static Set<VisibilityDrop> allVisDrops = new HashSet<VisibilityDrop>();
//  private static Set<VisibilityDrop> newVisDrops = new HashSet<VisibilityDrop>();

  private static Map<IRNode, Set<VisibilityDrop>> edMap = new HashMap<IRNode, Set<VisibilityDrop>>();

  IRNode refdModule;
  
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

  protected VisibilityDrop(IRNode locInIR, IRNode modIR) {
	  super(null); // WILL BLOW UP;
    refdModule = modIR;
    //setNodeAndCompilationUnitDependency(locInIR);
    setCategory(JavaGlobals.MODULE_CAT);
  }

  protected static <T extends VisibilityDrop> T buildVisDrop(T res,
      IRNode locInIR, IRNode modIR) {
    // VisibilityDrop res = new VisibilityDrop(locInIR, modIR);
    res.setMessage(12, res.toString());
    synchronized (VisibilityDrop.class) {
      allVisDrops.add(res);
//      newVisDrops.add(res);
      Set<VisibilityDrop> dropsHere = edMap.get(locInIR);
      if (dropsHere == null) {
        dropsHere = new HashSet<VisibilityDrop>(2);
        edMap.put(locInIR, dropsHere);
      }
      dropsHere.add(res);
    }
    return res;
  }
  
  public static void visibilityPrePost() {
//    newVisDrops.clear();
  }

  private static DropPredicate definingDropPred = new DropPredicate() {
    public boolean match(IDrop d) {
      return (d.isValid()) && d.instanceOf(CUDrop.class) || d.instanceOf(BinaryCUDrop.class);
    }    
  };
  
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
      Set<VisibilityDrop> dropsHere = edMap.get(this.getNode());
      if (dropsHere != null) {
        dropsHere.remove(this);
      }
    }
    super.deponentInvalidAction(invalidDeponent);
  }

  protected static Set<VisibilityDrop> findVisibilityDrops(IRNode promisedFor,
      IRNode modIR) {

    Set<VisibilityDrop> res = new HashSet<VisibilityDrop>(1);
    if (promisedFor == null || modIR == null) { return res; }
    final Set<VisibilityDrop> pfSet = edMap.get(promisedFor);
    if (pfSet == null) { return res; }
    for (VisibilityDrop ed : pfSet) {
      if (ed.refdModule.equals(modIR)) {
        res.add(ed);
      }
    }
    return res;
  }
  
  public static void checkVisibilityDrops() {
    final Collection<Set<VisibilityDrop>> safeVDsets;
    synchronized(VisibilityDrop.class) {
      safeVDsets = new ArrayList<Set<VisibilityDrop>>(edMap.values().size());
      safeVDsets.add(allVisDrops);
    }
    
    for (Set<VisibilityDrop> vds : safeVDsets) {
      final List<VisibilityDrop> someVDs;
      synchronized(VisibilityDrop.class) {
        someVDs = new ArrayList<VisibilityDrop>(vds.size());
        someVDs.addAll(vds);
      }
      for (VisibilityDrop vd : someVDs) {
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
          reqModName = API.getId(vd.refdModule);
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
