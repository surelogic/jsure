/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.target.ITargetMatcher;
import edu.cmu.cs.fluid.java.target.TargetMatcherFactory;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.sea.drops.promises.ExportDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ModuleDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ModuleModel;
import edu.cmu.cs.fluid.sea.drops.promises.VisDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * @author chance
 *  
 */
@Deprecated
public final class ModulePromises extends AbstractPromiseAnnotation {
  private ModulePromises() {
  }
  
  private static final ModulePromises instance = new ModulePromises();
  
  public static final IPromiseAnnotation getInstance() {
    return instance;
  }

  private static SlotInfo<IRNode> moduleSI;
  private static SlotInfo<IRSequence<IRNode>> moduleContainsSI, scopedModuleSI;
  private static SlotInfo<IRSequence<IRNode>> apiSI, exportsSI;

  /**
   * @return The representation of the module name, or
   *         null if there is no module declaration 
   */
  public static IRNode getModule(IRNode node) {
    return getXorNull_filtered(moduleSI, node);
  }
  
  public static void setModule(IRNode node, IRNode mod) {    
    setX_mapped(moduleSI, node, mod);
  }
 
  public static boolean isModuleSI(SlotInfo si) {
    return moduleSI.equals(si);
  }
  
  /**
   * For @module Bar contains Foo
   */
  public static Iteratable<IRNode> getModuleContainsDecls(IRNode n) {
    return getEnum_filtered(moduleContainsSI, n);
  }
  
  public static void addModuleContains(IRNode node, IRNode mod) {    
    addToSeq_mapped(moduleContainsSI, node, mod);
  }
  
  public static boolean removeModuleContains(IRNode n, IRNode mod) {
    return removeFromEnum_mapped(moduleContainsSI, n, mod);
  }
 
  /**
   * For @module Foo for Bar | Baz
   * (applied to compilation units)
   */
  public static Iteratable<IRNode> getScopedModules(IRNode n) {
    return getEnum_filtered(scopedModuleSI, n);
  }
  
  public static void addScopedModule(IRNode node, IRNode mod) {    
    addToSeq_mapped(scopedModuleSI, node, mod);
    addScopedModuleMatcher(mod);
  }
  
  public static boolean removeScopedModule(IRNode n, IRNode mod) {
    return removeFromEnum_mapped(scopedModuleSI, n, mod);
  }
  
  public static Iteratable<IRNode> apiDecls(IRNode n) {
    return getEnum_filtered(apiSI, n);
  }
  
  public static void addAPIDecl(IRNode node, IRNode mod) {
    VisDrop.buildVisDrop(node, mod);
    addToSeq_mapped(apiSI, node, mod);
  }
  
  public static boolean removeAPIDecl(IRNode n, IRNode mod) {
    return removeFromEnum_mapped(apiSI, n, mod);
  }
  
  public static Iteratable<IRNode> exportDecls(IRNode n) {
    return getEnum_filtered(exportsSI, n);
  }
  
  public static void addExportDecl(IRNode node, IRNode mod) {
    ExportDrop.buildExportDrop(node, mod);
    addToSeq_mapped(exportsSI, node, mod);
  }
  
  public static boolean removeExportDecl(IRNode n, IRNode mod) {
    return removeFromEnum_mapped(exportsSI, n, mod);
  }
  
  /**
   * Searches the drops
   */
  public static IRNode findModule(String name) {
    ModuleModel m = ModuleModel.query(name);
    if (m == null) {
      return null;
    }
    return m.module;
  }
  
  /**
   * @return The representation of the module name, or
   *         null if there is no module declaration 
   */
  public static IRNode getModuleDecl(IRNode here) {
    IRNode cu = VisitUtil.getEnclosingCompilationUnit(here);
    if (cu == null) {
      Operator op = tree.getOperator(here);
      if (CompilationUnit.prototype.includes(op)) {
        cu = here;
      } else {
        LOG.severe("Didn't find enclosing CU for the Module Decl: "+DebugUnparser.toString(here));
      }
    }
    IRNode pd = CompilationUnit.getPkg(cu); 
    return getModule(pd);
//    IRNode mayHaveModuleDecl = VisitUtil.computeOutermostEnclosingTypeOrCU(here);
//    return getModule(mayHaveModuleDecl);
  }
  
  private abstract class Rule<T> extends AbstractPromiseParserCheckRule<T> {
    protected Rule(String tag, int type, Operator[] ops) {
      super(tag, type, false, ops, ops);
    }
    protected Rule(String tag, int type, boolean multi, Operator[] ops) {
      super(tag, type, multi, ops, ops);
    }
    protected Rule(String tag, int type, Operator op) {
      super(tag, type, false, op, op);
    }
  }
  
  private abstract class API_Rule extends Rule<IRSequence<IRNode>> {
    protected API_Rule(String tag) {
      super(tag, IPromiseStorage.SEQ, true, declOrConstructorOps);
    }
    /**
     * Need to do special processing if n is a FieldDeclaration, since
     * the annotation should really go on its VariableDeclarators
     */
    @Override
    protected boolean processResult(final IRNode n, final IRNode result, IPromiseParsedCallback cb, Collection<IRNode> results) {
      Operator op = tree.getOperator(n);
      if (op instanceof FieldDeclaration) {
        IRNode vds               = FieldDeclaration.getVars(n);
        Iteratable<IRNode> decls = VariableDeclarators.getVarIterator(vds);
        if (decls.hasNext()) { // at least one
          IRNode first = decls.next();
          if (decls.hasNext()) { // more than one
            // Need to make duplicates for the rest
            for(IRNode d : decls) {
              IRNode dup = makeDuplicate(d, result);
              results.add(dup);
              addDecl(d, dup);
            }
          }
          results.add(result);
          addDecl(first, result);
        } else { 
          LOG.severe("No fields to put the result on");
        }
      } else {
        results.add(result);
        addDecl(n, result);        
      }
      return true;
    }    
    
    private IRNode makeDuplicate(IRNode d, IRNode result) {
      String id = API.getId(result);
      IRNode rv = API.createNode(id);
      return rv;
    }
    protected abstract void addDecl(IRNode n, IRNode result);
  }
  
  private static class Module_BindRule extends AbstractPromiseBindRule {
    Module_BindRule() {
      super(Module.prototype);
    }
    @Override
    protected IRNode bind(Operator op, IRNode use) {
      return findModule(Module.getId(use));
    }  
  }
  
  private static class API_BindRule extends AbstractPromiseBindRule {
    API_BindRule() {
      super(API.prototype);
    }
    @Override
    protected IRNode bind(Operator op, IRNode use) {
      String name = API.getId(use);
      if (name == null || name.length() == 0) {
        // find the current module and use its name
        IRNode modDecl = getModuleDecl(use);
        name = Module.getId(modDecl);
      }
      return findModule(name);
    }  
  }
  
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {      
      new Rule<IRNode>("Module", IPromiseStorage.NODE, PackageDeclaration.prototype) {        
        @Override
        public boolean checkSanity(Operator op, IRNode promisedFor, IPromiseCheckReport report) {
          IRNode module = getModule(promisedFor);
          if (module == null) {
            return true;
          }
//          if (binder.getBinding(module) == null) {
//            setBogus(module, true);
//            return false;
//          }
          return true;
        }        
        public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
          moduleSI = si;
          return new TokenInfo<IRNode>("Module", si, "module");
        }        
        @Override
        protected boolean processResult(final IRNode n, final IRNode result, IPromiseParsedCallback cb) {   
          Operator op = tree.getOperator(result);
          //String s = DebugUnparser.toString(result);
          String name;
          if (EnclosingModule.prototype.includes(op)) {
            // Enclosing Modules
            name = EnclosingModule.getId(result);
            addModuleContains(n, result);
            
            // check if not package-info.java
            IRNode cu = VisitUtil.getEnclosingCompilationUnit(n);
            if (VisitUtil.getTypeDecls(cu).hasNext()) {
              cb.noteProblem("@module with contains clause found outside of package-info.java");
            }
            // Module Drop processing handles these "specially"
//            final IRNode enclosedMods = EnclosingModule.getModules(result);
//            ModuleModel.confirmDrop(name, result);
            ModuleDrop.buildModuleDrop(n, name, result);
          } else if (ScopedModule.prototype.includes(op)) {
            // scoped module
            IRNode targets = ScopedModule.getTargets(result);
            if (!isOnlyTypes(targets)) {
            }
            name = ScopedModule.getId(result);
            addScopedModule(n, result);
            // module drop processing handles these just like the ordinary sort.
//            ModuleModel.confirmDrop(name);
            ModuleDrop.buildModuleDrop(n, name);
          } else {
            // regular old module
            name = Module.getId(result);
            setModule(n, result);
//            ModuleModel.confirmDrop(name);
            ModuleDrop.buildModuleDrop(n, name);
          }
          
          return true;
        }
        // make sure target is either ComplexTarget or TypeDeclPattern
        private boolean isOnlyTypes(IRNode targets) {
          Operator op = tree.getOperator(targets);
          if (ComplexTarget.prototype.includes(op)) {
            Iterator<IRNode> it = tree.children(targets);
            while (it.hasNext()) {
              IRNode n = it.next();
              if (!isOnlyTypes(n)) {
                return false;
              }
            }
            return true;
          } 
          else return TypeDeclPattern.prototype.includes(op);
        }
      }, 
      new AbstractPromiseStorageAndCheckRule<IRSequence<IRNode>>("Module contains", IPromiseStorage.SEQ, PackageDeclaration.prototype) {
        public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
          moduleContainsSI = si;
          return new TokenInfo<IRSequence<IRNode>>("Module contains", si, "module");
        }
      },
      new AbstractPromiseStorageAndCheckRule<IRSequence<IRNode>>("Scoped module", IPromiseStorage.SEQ, PackageDeclaration.prototype) {
        public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
          scopedModuleSI = si;
          return new TokenInfo<IRSequence<IRNode>>("Scoped modules", si, "module");
        }
      },
      new API_Rule("Vis") {
        public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
          apiSI = si;
          return new TokenInfo<IRSequence<IRNode>>("API for", si, "vis");
        }        
        @Override
        protected void addDecl(IRNode n, IRNode result) {
          addAPIDecl(n, result);
        }
      }, 
      new API_Rule("Export") {
        public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
          exportsSI = si;
          return new TokenInfo<IRSequence<IRNode>>("Export to", si, "export");
        } 
        @Override
        protected void addDecl(IRNode n, IRNode result) {
          addExportDecl(n, result);
        }
      }, 
      new Module_BindRule(),
      new API_BindRule(),
    };
  }
  
  private static class ScopedModuleRecord {
    final IRNode module;
    final String name;
    final ITargetMatcher matcher;

    ScopedModuleRecord(IRNode module, String name, ITargetMatcher matcher) {
      this.module = module;
      this.name = name;
      this.matcher = matcher;
    }    
  }
  
  private static Map<String,List<ScopedModuleRecord>> scopedModules = new HashMap<String,List<ScopedModuleRecord>>();
  
  /**
   * Adds implicit scoping to the package
   * @param sm
   */
  public static void addScopedModuleMatcher(final IRNode sm) {
    final String pkgName   = JavaNames.getPackageName(sm);
    final String name      = ScopedModule.getId(sm);
    final IRNode targets   = ScopedModule.getTargets(sm);
    ITargetMatcher matcher = TargetMatcherFactory.prototype.create(targets);            
    
    // Remember the info for later use just before the comp unit is 
    // actually converted           
    List<ScopedModuleRecord> modules = scopedModules.get(pkgName);
    if (modules == null) {
      modules = new ArrayList<ScopedModuleRecord>(2);
      LOG.fine("Creating scoped module for "+pkgName+": "+DebugUnparser.toString(sm));
      scopedModules.put(pkgName, modules);
    }
    modules.add(new ScopedModuleRecord(sm, name, matcher));   
  }
  
  public static String warnIfNotEqual(String label, String current, String next) {
    if (next == null) {
      return current;
    }
    if (current == null) {
      return next;
    }     
    else if (!current.equals(next)) {
      // two different matches
      LOG.severe(label+" is mapped to two different modules: "+current+" and "+next);
      // Use the latest one
      return next; 
    }
    // otherwise, they're the same, and that's OK 
    return current;
  }
  
  /**
   * 
   * @param pkgName null if default package
   * @param name
   * @return null if no module mapping found
   */
  public static String whichModule(String pkgName, String name) {
    String rv = null;
    List<ScopedModuleRecord> modules = scopedModules.get(pkgName);
    if (modules == null) {
      return null;
    }
    for (final ScopedModuleRecord r : modules) {
      if (r.module.identity().equals(IRNode.destroyedNode)) {
        modules.remove(r);
        continue;
      }
      if (r.matcher.match(name)) {
        rv = warnIfNotEqual(name, rv, r.name);
      }
    }
    return rv;
  }

  /**
   * Create individual module annos
   * (not actually used by the loading mechanism)
   */
  public static void applyScopedModules(final IRNode pkg, final IRNode cu) {    
    String pkgName = NamedPackageDeclaration.getId(pkg);
    
    List<ScopedModuleRecord> modules = scopedModules.get(pkgName);
    if (modules == null) {
      return;    
    }
    String modName = null;
    for (final ScopedModuleRecord r : modules) {      
      // find out if cu matches the targets
      for (final String name : JavaNames.getQualifiedTypeNames(cu)) {
        if (r.matcher.match(name)) {
          // matched one of the types
          modName = warnIfNotEqual(name, modName, r.name);
        }
      }
    }  
    if (modName == null) {
      return;
    }
    IRNode pd     = CompilationUnit.getPkg(cu);
    IRNode module = Module.createNode(modName);
    setModule(pd, module);
  }
}