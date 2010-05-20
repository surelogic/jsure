/*
 * Created on Oct 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.modules;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.InstanceInitializationVisitor;
import com.surelogic.analysis.threadroles.TRolesFirstPass;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.callgraph.SimpleCallGraphDrop;
import edu.cmu.cs.fluid.sea.drops.modules.ModuleModel;
import edu.cmu.cs.fluid.sea.drops.modules.ModulePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.modules.VisibilityDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class ModuleAnalysisAndVisitor {
  
  private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");  //$NON-NLS-1$
  
  private static final ModuleAnalysisAndVisitor INSTANCE = 
    new ModuleAnalysisAndVisitor();
  
  private static  Collection<IRNode> allFields;
  private static  Collection<IRNode> publicFields;
  private static final boolean javaEntityStats = true;

  ModuleAnalysisAndVisitor() {
    if (javaEntityStats) {
      allFields = new HashSet<IRNode>();
      publicFields = new HashSet<IRNode>();
    } else {
      allFields = null;
      publicFields = null;
    }
  }
  
  private static Drop resultDependUpon = null;
  
  private static final Category DSC_BAD_CROSS_MODULE_REF =
    Category.getInstance(ModuleMessages.getString("ModuleAnalysisAndVisitor.dsc.BadCrossModuleRefCat")); //$NON-NLS-1$
  
  private static final Category DSC_API_WISHES =
    Category.getInstance("Non @vis entities with cross-module references");
  
  private static final Category DSC_API_WISHES_PROMOTION = 
    Category.getInstance("Other modules desire @vis promotion");
  
  private static final Category DSC_BAD_MODULE_PROMISE =
    Category.getInstance("Erroneous @module promises");
  
  private static final String DS_BAD_CROSS_MODULE_REF = 
    ModuleMessages.getString("ModuleAnalysis.ds.BadCrossModuleRef"); //$NON-NLS-1$
  
  private static final String DS_API_WISH_CALLERS = 
    "{0} {1} would like it to be part of the exported interface of Module {2}";
  
  private static final String DS_API_WISH_INFO =
    "Corrective action: if you are currently modularizing code, consider adding @vis or @export; otherwise, fix the reference.";
  
  private static final String DS_API_WISHES_PROMOTION_INFO = 
    "Consider adding @vis or @export annotations.";
  
  private static final String DS_MODULE_ERR_NONLEAF_WITH_CODE = 
    "Types may not be placed in non-leaf module {0}";
  
  public class MAVisitor extends VoidTreeWalkVisitor {
    MAVisitor getInstance() {
      return INSTANCE;
    }
    
    final MAVisitor INSTANCE = this;

//    InstanceInitVisitor<Void> initHelper = null;
    
    public ModuleModel currMod = null;
    
    public IRNode currMethod = null;
    public String currMethName = null;
    
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitArrayRefExpression(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitArrayRefExpression(IRNode node) {
//      // TODO Auto-generated method stub
//      return super.visitArrayRefExpression(node);
//    }
    
    /** check whether javaThingy what is visible from where. Issue an error message
     * if the reference violates module encapsulation.
     * @param where The place the reference comes from.
     * @param what The JavaEntity we are referring to.
     */
    private void checkVisibility(IRNode where, final IRNode what) {
 
      if (!currMod.moduleVisibleFromHere(what)) {
        // mark an error here.  Add mDecl to the WishIWasVis for its module.
        ResultDrop rd = makeResultDrop(where, currMod, false, 
                                       DS_BAD_CROSS_MODULE_REF,
                                       DebugUnparser.toString(where));
        rd.setCategory(DSC_BAD_CROSS_MODULE_REF);
//        ModuleModel.setModuleInformationIsConsistent(false);
        // this is an error, but the module STRUCTURE is OK.
        
        ModuleModel.updateWishIWere(what, currMod);
      }
      
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitClassDeclaration(IRNode node) {
      final ModuleModel saveCurrMod = currMod;
      try {
        // OK to over-ride TheWorld or null, but not anything else.
        if (currMod == null || currMod.moduleIsTheWorld()) {
          currMod = ModuleModel.getModuleDrop(node);
          if (currMod != saveCurrMod) {
          // ...but it is extremely suspicious if this ever happens!
            LOG.warning("Overriding module " +saveCurrMod+ " with " +currMod);
          }
        }
        checkTypePlacement(node);
        super.visitClassDeclaration(node);
      } finally {
        currMod = saveCurrMod;
      }
      return null;
    }
    
    // don't look in class initializers or field declarations
    // (Alternatively, we could look in if they *are* static.)
    @Override
    public Void visitClassInitializer(IRNode node) {
      return null;
    }

    /**
     * @param node
     */
    private void checkTypePlacement(IRNode node) {
      if (!currMod.isLeafModule()) {
        
        final Collection<ModulePromiseDrop> promiseSet =  ModulePromiseDrop.findModuleDrops(node);
        for (ModulePromiseDrop modPromise : promiseSet) {
          if (modPromise != null) {
            ResultDrop rd = makeResultDrop(node, modPromise, false,
                                           DS_MODULE_ERR_NONLEAF_WITH_CODE,
                                           currMod.name);
            rd.setCategory(DSC_BAD_MODULE_PROMISE);
            ModuleModel.setModuleInformationIsConsistent(false);
            modPromise.setBadPlacement(true);
          }
        }
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorCall(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitConstructorCall(IRNode node) {
      final IRNode mDecl = binder.getBinding(node);
      
      if (currMethod != null) {        
        final IRNode object = ConstructorCall.getObject(node);
        final IJavaType receiverType = binder.getJavaType(object);
        
        // build call graph connections
        cgBuild(currMethod, mDecl, receiverType);
      }


      checkVisibility(node, mDecl);
      
      super.visitConstructorCall(node);
      
      InstanceInitializationVisitor.processConstructorCall(node, getInstance());
      
//      if (initHelper != null) {
//        // initHelper is non-null only when we are traversing tree somewere
//        // inside
//        // a constructorDeclaration. That means that the ConstructorCall we're
//        // looking at right now may possibly be the call to super() at the
//        // beginning
//        // of the constructorDeclaration. If it is, we need to traverse the init
//        // code right now. The call below will do that, if necessary.
//        initHelper.doVisitInstanceInits(node);
//      }
      return null;
    }

   /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitConstructorDeclaration(IRNode node) {
      final IRNode saveCurrMeth = currMethod;
      final String saveCurrMethName = currMethName;
//      final InstanceInitVisitor<Void> saveInitHelper = initHelper;
      currMethod = node;
      currMethName = JavaNames.genQualifiedMethodConstructorName(currMethod);
      
      Void res = null;
      try {
        // Replaced with call to InstanceInitializationVisitor in visitConstructorCall
//        initHelper = new InstanceInitVisitor<Void>(getInstance());
//        // note that doVisitInstanceInits will only do the traversal when
//        // appropriate, and will call back into this visitor to travers the
//        // inits themselves.
//        initHelper.doVisitInstanceInits(node);
        
        res = super.visitConstructorDeclaration(node);
      } finally {
//        initHelper = saveInitHelper;
        currMethod = saveCurrMeth;
        currMethName = saveCurrMethName;
      }
      return res;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitEnumDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitEnumDeclaration(IRNode node) {
      Void res = null;
      final ModuleModel saveCurrMod = currMod;
      try {
        // OK to over-ride TheWorld or null, but not anything else.
        if (currMod == null || currMod.moduleIsTheWorld()) {
          currMod = ModuleModel.getModuleDrop(node);
          if (currMod != saveCurrMod) {
          // ...but it is extremely suspicious if this ever happens!
            LOG.warning("Overriding module " +saveCurrMod+ " with " +currMod);
          }
        }
        checkTypePlacement(node);
        res = super.visitEnumDeclaration(node);
      } finally {
        currMod = saveCurrMod;
      }
      return res;
    }

    @Override
    public Void visitFieldDeclaration(IRNode node) {
      return null;
    }

    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitFieldRef(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitFieldRef(IRNode node) {
      final IRNode field = binder.getBinding(node);
      checkVisibility(node, field);
      
      if (javaEntityStats) {
        // only check fields that are in CUs whose source is loaded...
        final CUDrop cud = TRolesFirstPass.getCUDropOf(field);
        if (cud.isAsSource()) {
          final IRNode declStmt =
            JJNode.tree.getParent(JJNode.tree.getParent(field));
          if (JavaNode.getModifier(declStmt, JavaNode.PUBLIC)) {
            publicFields.add(field);
          }
          allFields.add(field);
        }
      }
      return super.visitFieldRef(node);
    }
    
    @Override
    public Void visitImportDeclarations(IRNode node) {
      // ignore these
      return null;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitInterfaceDeclaration(IRNode node) {
      final ModuleModel saveCurrMod = currMod;
      try {
        // OK to over-ride TheWorld or null, but not anything else.
//      OK to over-ride TheWorld or null, but not anything else.
        if (currMod == null || currMod.moduleIsTheWorld()) {
          currMod = ModuleModel.getModuleDrop(node);
          if (currMod != saveCurrMod) {
          // ...but it is extremely suspicious if this ever happens!
            LOG.warning("Overriding module " +saveCurrMod+ " with " +currMod);
          }
        }
        currMod.setContainsCode(true);
        checkTypePlacement(node);
        super.visitInterfaceDeclaration(node);
      } finally {
        currMod = saveCurrMod;
      }
      return null; 
    }

 
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodBody(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodBody(IRNode node) {
      SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop.getCGDropFor(currMethod);
      cgDrop.setTheBody(node);
      cgDrop.setFoundABody(true);
      return super.visitMethodBody(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodCall(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodCall(IRNode node) {
      MethodCall call = (MethodCall) getOperator(node);
      final IRNode mDecl = binder.getBinding(node);
      
      if (currMethod != null) {
        final IRNode obj = call.get_Object(node);
        final IJavaType receiverType = binder.getJavaType(obj);
        
        // build call graph connections
        cgBuild(currMethod, mDecl, receiverType);
      }

      checkVisibility(node, mDecl);
      
      return super.visitMethodCall(node);
    }
    
    

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodDeclaration(IRNode node) {
      final IRNode saveCurrMeth = currMethod;
      currMethod = node;
      final String saveCurrMethName = currMethName;
      currMethName = JavaNames.genQualifiedMethodConstructorName(currMethod);
      
      Void res = null;
      try {
        res = super.visitMethodDeclaration(node);
      } finally {
        currMethod = saveCurrMeth;
        currMethName = saveCurrMethName;
      }
      return res;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNamedType(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitNamedType(IRNode node) {
      final IRNode typ = binder.getBinding(node);

      checkVisibility(node, typ);
      
      return super.visitNamedType(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNewExpression(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitNewExpression(IRNode node) {
      final IRNode cDecl = binder.getBinding(node);
       
      if (currMethod != null) {
        final IJavaType type = binder.getJavaType(node);
        cgBuild(currMethod, cDecl, type);
      }
      checkVisibility(node, cDecl);
      
      return super.visitNewExpression(node);
    }

 

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitTypeRef(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitTypeRef(IRNode node) {
      final IRNode typ = binder.getBinding(node);
      if (!currMod.moduleVisibleFromHere(typ)) {
        // mark an error, add Node to the WishIWasVis for the right module
        ResultDrop rd = makeResultDrop(node, currMod, false, 
                                       DS_BAD_CROSS_MODULE_REF,
                                       TypeRef.getId(node));
        rd.setCategory(DSC_BAD_CROSS_MODULE_REF);
        
        ModuleModel.updateWishIWere(typ, currMod);
      }
      return super.visitTypeRef(node);
    }
    

  }

  private void cgBuildOne(final IRNode caller, final IRNode callee) {
    final SimpleCallGraphDrop callerDrop = SimpleCallGraphDrop
        .getCGDropFor(caller);
    final SimpleCallGraphDrop calleeDrop = SimpleCallGraphDrop
        .getCGDropFor(callee);

    callerDrop.getCallees().add(callee);
    calleeDrop.getCallers().add(caller);

  }

  /**
   * Update the call graph to indicate that caller invokes callee. Also note
   * that caller may possibly invoke any method that overrides or implements
   * callee.
   * 
   * @param caller
   *          The caller's mDecl.
   * @param callee
   *          The callee's mDecl.
   * @param receiverType
   *          The callee's receiver type.
   */
  private void cgBuild(final IRNode caller, final IRNode callee,
      final IJavaType receiverType) {
    cgBuildOne(caller, callee);
    final SimpleCallGraphDrop calleeDrop = 
      SimpleCallGraphDrop.getCGDropFor(callee);

//    final String callerName = JavaNames.genMethodConstructorName(caller);
//    final String calleeName = JavaNames.genMethodConstructorName(callee);
    calleeDrop.numCallSitesSeen += 1;

    final boolean isStatic = JavaNode.getModifier(callee, JavaNode.STATIC);
    final boolean isFinal = JavaNode.getModifier(callee, JavaNode.FINAL);
    final boolean isPrivate = JavaNode.getModifier(callee, JavaNode.PRIVATE);
    final boolean methodCanBeOveridden = !(isStatic || isFinal || isPrivate);
    if (methodCanBeOveridden) {
      IRNode rType;
      if (receiverType instanceof IJavaDeclaredType) {
        rType = ((IJavaDeclaredType) receiverType).getDeclaration();
      } else if (receiverType instanceof IJavaArrayType) {
        rType = binder.getTypeEnvironment().getObjectType().getDeclaration();
      } else if (receiverType instanceof IJavaTypeFormal) {
        IJavaTypeFormal f = (IJavaTypeFormal) receiverType;
        rType = f.getDeclaration();        
      } else {
        LOG.severe("Couldn't get receiver type for "+receiverType.getClass().getCanonicalName());
        return;
      }
      Iterator<IRNode> overrides = binder.findOverridingMethodsFromType(callee,
                                                                        rType);
      while (overrides.hasNext()) {
        IRNode oCallee = overrides.next();
        cgBuildOne(caller, oCallee);
        calleeDrop.numOverridingMethods += 1;
      }
    }
  }
  
  
  
  private static void setResultDep(final IRReferenceDrop drop, final IRNode node) {
//    drop.setNode(node);
    drop.setNodeAndCompilationUnitDependency(node);
    if (resultDependUpon != null && resultDependUpon.isValid()) {
      resultDependUpon.addDependent(drop);
    } else {
      LOG.log(Level.SEVERE,
          "setResultDep found invalid or null resultDependUpon drop"); //$NON-NLS-1$
    }
  }
  public static WarningDrop makeWarningDrop(
      final Category category, final IRNode context,
      final String msgTemplate, final Object... msgArgs) {
    final String msg = MessageFormat.format(msgTemplate, msgArgs);
    final WarningDrop info = new WarningDrop();
    setResultDep(info, context);
    info.setMessage(msg);
    info.setCategory(category);
    return info;
  }
  
  @SuppressWarnings("unchecked")
  public static ResultDrop makeResultDrop(
      final IRNode context, final PromiseDrop p, final boolean isConsistent,
      final String msgTemplate, final Object... msgArgs) {
    final String msg = MessageFormat.format(msgTemplate, msgArgs);
    final ResultDrop result = new ResultDrop("ModuleAnalysis_simpleModulePromiseDrop");
    setResultDep(result, context);
    result.setMessage(msg);
    result.addCheckedPromise(p);
    result.setConsistent(isConsistent);
    return result;
  }
  
  public static void addSupportingInformation(final IRReferenceDrop drop,
      final IRNode link, final String msgTemplate,
      final Object... msgArgs) {
    final String msg = MessageFormat.format(msgTemplate, msgArgs);
    drop.addSupportingInformation(msg, link);
  }
 

  public void maStart(final Drop dependOn) {
    resultDependUpon = dependOn;
//    VisibilityDrop.visibilityPrePost();
//    ModulePromiseDrop.moduleDropPrePost();
    ModuleModel.initModuleModels(dependOn);
    
    // check whether there are any moduleDrops. If not, nothing to do!
    if (!ModulePromiseDrop.thereAreModules()) { return; }
    
    
//    ModuleModel.setAllParents();
    resultDependUpon = dependOn;
    ModulePromiseDrop.buildModuleModels();
//    ModuleModel.purgeUnusedModuleModels();
    
    VisibilityDrop.checkVisibilityDrops();
    ModuleModel.processWrapperModParents();
    ModuleModel.computeVisibles();
    
 
  }
  
  public void maPre(final Drop dependOn) {
    resultDependUpon = dependOn;
    VisibilityDrop.visibilityPrePost();
    ModulePromiseDrop.moduleDropPrePost();
    ModuleModel.initModuleModels(dependOn);
    ModuleModel.setModuleInformationIsConsistent(true);
  }
  
  private static Operator getOperator(final IRNode node) {
    return JJNode.tree.getOperator(node);
  }
  
  private static IRNode getParent(final IRNode node) {
    return JJNode.tree.getParentOrNull(node);
  }

  public static String javaName(final IRNode javaThing) {
    final Operator op = getOperator(javaThing);
    
    if (ConstructorDeclaration.prototype.includes(op) ||
        MethodDeclaration.prototype.includes(op)) {
      return JavaNames.genQualifiedMethodConstructorName(javaThing);
    } else if (VariableDeclarator.prototype.includes(op)) {
      return JavaNames.getFieldDecl(getParent(getParent(javaThing)));
    } else if (FieldDeclaration.prototype.includes(op)) {
      return JavaNames.getFieldDecl(javaThing);
    } else if (InterfaceDeclaration.prototype.includes(javaThing) ||
        TypeDeclaration.prototype.includes(op)) {
      return JavaNames.getFullTypeName(javaThing);
    } else if (EnumConstantDeclaration.prototype.includes(op)) {
      return EnumConstantDeclaration.getId(javaThing);
    }
    return "!BogusName!";
  }
  
  private String getRefStr(final IRNode javaThing) {
   final Operator op = getOperator(javaThing);
    
    if (ConstructorDeclaration.prototype.includes(op) ||
        MethodDeclaration.prototype.includes(op)) {
      return "Callers of";
    } else if (FieldDeclaration.prototype.includes(op) ||
        VariableDeclarator.prototype.includes(op)) {
      return "References to";
    } else if (InterfaceDeclaration.prototype.includes(javaThing)) {
      return "Implementors of";
    } else if (TypeDeclaration.prototype.includes(op)) {
      return "References to";
    } else if (EnumConstantDeclaration.prototype.includes(op)) {
      return "References to";
    }
    return "!UnknownRefkind ("+op.name()+") of!";
  }
  public void maEnd() {
    if (!ModuleModel.thereAreModules()) { return; }
//    
//    ModuleModel.purgeUnusedModuleModels();
    
    // issue warnings for WishIWere
    final boolean saveFakingVis = ModuleModel.fakingVis;
    // don't let fakingVis cause us to give incorrect error messages about
    // wishIWere items
    ModuleModel.fakingVis = false; 
    
    final List<ModuleModel> wishMods = ModuleModel.getModulesThatWishIWere();
    for (ModuleModel md : wishMods) {
      // issue a warning for each wish...
      final Map<IRNode, ModuleModel> wishes = md.getWishIWere();
      for (Map.Entry<IRNode, ModuleModel> ent : wishes.entrySet()) {
        final ModuleModel desiredMod = ent.getValue();
        final String modName = desiredMod.name;
        final IRNode javaThing = ent.getKey();
        final String javaThingName = javaName(javaThing);
        final String refStr = getRefStr(javaThing);
        
        final Category warnCat;
        final String infoStr;
        if (ModuleModel.isAPIinParentModule(javaThing)) {
          warnCat = DSC_API_WISHES_PROMOTION;
          infoStr = DS_API_WISHES_PROMOTION_INFO;
        } else {
          warnCat = DSC_API_WISHES;
          infoStr = DS_API_WISH_INFO;
        }
        
        InfoDrop rd = makeWarningDrop(warnCat, javaThing,
                                      DS_API_WISH_CALLERS, 
                                      refStr, javaThingName, modName);
        rd.addSupportingInformation(infoStr, null);
      }
      // restore fakingVis to its old value so that we will get useful
      // colorErrors as chosen by the user.
      ModuleModel.fakingVis = saveFakingVis;
    }
    
    ModulePromiseDrop.buildModuleDropResults();
    VisibilityDrop.visibilityPrePost();
    ModulePromiseDrop.moduleDropPrePost();
  }
  
  IBinder binder = null;
  
  public void doOneCU(IRNode cu, IBinder useThisBinder) {
//    if (!ModuleModel.thereAreModules()) { return; }
    // module analysis must run, at least to detect that everything is
    // "in the world" rather than in specific modules. That will be the
    // result of running the analysis when there are no defined modules.
    // As of 2006.08.11, Module analysis also builds the call graph that
    // coloring and other analyses depend on, so that's yet another reason
    // to always run it.
    
    binder = useThisBinder;
    MAVisitor mav = new MAVisitor();
    mav.currMod = ModuleModel.getModuleDrop(cu);
    mav.doAccept(cu);
  }

  
  /**
   * @return Returns the iNSTANCE.
   */
  public static ModuleAnalysisAndVisitor getInstance() {
    return INSTANCE;
  }

  static Collection<IRNode> getAllFields() {
    return allFields;
  }


  static Collection<IRNode> getPublicFields() {
    return publicFields;
  }

}
