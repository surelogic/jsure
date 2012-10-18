package com.surelogic.dropsea.ir.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.ModelingProblemDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.ModelDrop;
import com.surelogic.dropsea.ir.drops.PackageDrop;
import com.surelogic.persistence.JavaIdentifier;
import com.surelogic.promise.PromiseDropStorage;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IHasBinding;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.ClassBodyDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * TODO Assumes that all dependencies are the same, and doesn't distinguish
 * between what different analyses need (which could allow for less
 * reprocessing)
 * 
 * @author Edwin
 */
public class Dependencies {
  /**
   * To avoid cycles and duplication
   */
  private final Set<Drop> checkedDependents = new HashSet<Drop>();
  // private final Set<Drop> checkedDeponents = new HashSet<Drop>();

  /**
   * The set of CUDrops that need to be reprocessed for promises (and
   * reanalyzed)
   */
  private final Set<CUDrop> reprocess = new HashSet<CUDrop>();
  /**
   * The set of changed CUDrops
   */
  private final Set<CUDrop> changed = new HashSet<CUDrop>();
  /**
   * The set of CUDrops that just need to be reanalyzed
   */
  private final Set<CUDrop> reanalyze = new HashSet<CUDrop>();

  /**
   * Info from the old CUs
   */
  private final MultiMap<String, PromiseDrop<?>> oldInfo = new MultiHashMap<String, PromiseDrop<?>>();

  public void markAsChanged(CUDrop d) {
    if (d == null) {
      return; // Nothing to do
    }
    System.out.println("Marking as changed: " + d);
    changed.add(d);
    collect(d);
  }

  /**
   * Collects the CUDrops corresponding to d's dependent drops, so we can
   * reprocess the promises on those
   */
  private void collect(Drop root) {
    if (checkedDependents.contains(root)) {
      return;
    }
    checkedDependents.add(root);

    // Find dependent drops
    boolean first = true;
    for (Drop d : getDependents(root)) {
      if (first && (Drop.debug == null || d.getMessage().startsWith(Drop.debug))) {
        first = false;
        System.out.println(root.getMessage() + " <- ");
      }
      try {
        final CUDrop cud = findEnclosingCUDrop(d);
        if (Drop.debug == null || d.getMessage().startsWith(Drop.debug)) {
          if (cud != null) {
            final IRNode type = VisitUtil.getPrimaryType(cud.getCompilationUnitIRNode());
            System.out.println("\t" + d.getMessage() + "\tfrom " + JavaNames.getTypeName(type));
          } else {
            System.out.println("\t" + d.getMessage());
          }
        }
        collect(d);
      } catch (IgnoredException e) {
        System.out.println("\t" + d.getMessage());
      }
    }
  }

  private static final boolean compensateForModels = true;

  /**
   * Get the true dependents (compensates for weirdness of Region/LockModel)
   */
  private static Iterable<Drop> getDependents(Drop root) {
    // if (root instanceof ModelDrop) {
    if (compensateForModels && ModelDrop.class.isInstance(root)) {
      List<Drop> dependents = new ArrayList<Drop>();
      // Ignore any model drops as "dependents"
      for (Drop d : root.getDependents()) {
        if (ModelDrop.class.isInstance(d)) {
          System.out.println("\tIgnoring dependent " + d.getMessage());
          continue;
        }
        dependents.add(d);
      }
      // Check for model drops as "deponents" -- really should be reversed
      for (Drop d : root.getDeponents()) {
        if (ModelDrop.class.isInstance(d)) {
          dependents.add(d);
        } else {
          System.out.println("\tIgnoring deponent  " + d.getMessage());
        }
      }
      return dependents;
    }
    return root.getDependents();
  }

  private static class IgnoredException extends Exception {
    // Nothing to do
  }

  private static final IgnoredException ignored = new IgnoredException();

  /**
   * @return true if ignored
   * @throws IgnoredException
   */
  private CUDrop findEnclosingCUDrop(Drop d) throws IgnoredException {
    IRNode cu = VisitUtil.getEnclosingCompilationUnit(d.getNode());
    CUDrop cud = CUDrop.queryCU(cu);
    if (cud != null) {
      if ("java.lang.Object".equals(cud.getJavaOSFileName())) {
        // This stuff should never get invalidated
        throw ignored;
      }
      // System.out.println(cud+" <- "+d);
      reprocess.add(cud);
      return cud;
    } else {
      // System.out.println("No CUDrop for "+d);
    }
    return null;
  }

  /**
   * Recursively check this drop and its deponents for CUDrops
   * 
   * Note: this does what findEnclosingCUDrop() does, and more (too much)
   */
  // private void findCUDropDeponents(Drop d) {
  // if (checkedDeponents.contains(d)) {
  // return;
  // }
  // checkedDeponents.add(d);
  // if (d instanceof CUDrop) {
  // System.out.println("Reprocessing "+d);
  // reprocess.add((CUDrop) d);
  // collect(d);
  // /*
  // if (d instanceof PackageDrop) {
  // // I need to reprocess these if the package changed
  // for(Drop dd : d.getDependents()) {
  // if (dd instanceof CUDrop) {
  // reprocess.add((CUDrop) dd);
  // }
  // }
  // }
  // */
  // return; // No need to look at deponents
  // }
  // for(Drop deponent : d.getDeponents()) {
  // System.out.println(d+" -> "+deponent);
  // findCUDropDeponents(deponent);
  // }
  // }
  //
  /**
   * Collect CU deponents of promise warnings
   * 
   * @return
   */
  private Collection<ModelingProblemDrop> processPromiseWarningDrops() {
    final List<ModelingProblemDrop> warnings = Sea.getDefault().getDropsOfType(ModelingProblemDrop.class);
    for (Drop d : warnings) {
      System.out.println("Processing PWD: " + d.getMessage());
      try {
        final CUDrop cud = findEnclosingCUDrop(d);
        if (cud != null) {
          System.out.println("\tCollecting ...");
          collect(cud);
        }
      } catch (IgnoredException e) {
        // Nothing to do
      }
    }
    return warnings;
  }

  /**
   * Clears drops!
   */
  public void finishReprocessing() {
    final Collection<ModelingProblemDrop> warnings = processPromiseWarningDrops();
    for (CUDrop d : changed) {
      System.out.println("Changed:   " + d.getJavaOSFileName() + " " + d.getClass().getSimpleName());
    }
    for (CUDrop d : reprocess) {
      System.out.println("Reprocess: " + d.getJavaOSFileName() + " " + d.getClass().getSimpleName());
    }
    // if (AbstractWholeIRAnalysis.useDependencies) {
    // collectOldAnnotationInfo();
    // }
    reprocess.removeAll(changed);

    IDE.getInstance().setAdapting();
    try {
      for (CUDrop d : reprocess) {
        clearPromiseDrops(d);
        if (d instanceof PackageDrop) {
          final PackageDrop pkg = (PackageDrop) d;
          for (Drop dependent : pkg.getDependents()) {
            dependent.invalidate();
          }
          handlePackage(pkg);
          /*
           * runVersioned(new AbstractRunner() { public void run() {
           * parsePackagePromises(pkg); } });
           */
        }
        // else if (AbstractWholeIRAnalysis.useDependencies) { // Same as
        // Util.clearOldResults
        // // Clear info/warnings
        // // Clear results
        // for(Drop dd : d.getDependents()) {
        // if (dd instanceof IReportedByAnalysisDrop || dd instanceof
        // PromiseWarningDrop) {
        // dd.invalidate();
        // } else {
        // System.out.println("\tIgnoring "+dd.getMessage());
        // }
        // }
        // }
      }
      // Necessary to process these after package drops
      // to ensure that newly created drops don't invalidated
      for (CUDrop d : reprocess) {
        // Already cleared above
        if (!(d instanceof PackageDrop)) {
          handleType(d);
          // ConvertToIR.getInstance().registerClass(d.makeCodeInfo());
        }
      }
      for (ModelingProblemDrop w : warnings) {
        w.invalidate();
      }
    } finally {
      IDE.getInstance().clearAdapting();
    }
  }

  protected void handlePackage(PackageDrop d) {
    // Nothing to do
  }

  protected void handleType(CUDrop d) {
    // Nothing to do
  }

  private void clearPromiseDrops(CUDrop d) {
    // Clear out promise drops
    // System.out.println("Reprocessing "+d.javaOSFileName);
    for (IRNode n : JavaPromise.bottomUp(d.getCompilationUnitIRNode())) {
      PromiseDropStorage.clearDrops(n);
    }
  }

  // Written to collect info BEFORE the old AST is destroyed
  //
  // Decls as Strings for field/method decls
  // TODO what if I've got the same name from two projects?
  private void collectOldAnnotationInfo() {
    // Get all the CUs that will be re-annotated
    System.out.println("Collecting all the CUs to be re-annotated");
    reanalyze.clear();
    reanalyze.addAll(reprocess);
    reanalyze.addAll(changed);
    oldInfo.clear();

    for (final CUDrop cud : reanalyze) {
      System.out.println("Collecting old info for " + cud.getJavaOSFileName());
      // record old decls and what annotations were on them
      for (final IRNode n : JJNode.tree.bottomUp(cud.getCompilationUnitIRNode())) {
        final Operator op = JJNode.tree.getOperator(n);
        if (ClassBodyDeclaration.prototype.includes(op)) {
          if (FieldDeclaration.prototype.includes(op)) {
            for (IRNode vd : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(n))) {
              collectOldInfoAnnotationInfoForDecl(cud, vd);
            }
          } else {
            collectOldInfoAnnotationInfoForDecl(cud, n);
          }
        } else if (TypeDeclaration.prototype.includes(op)) {
          collectOldInfoAnnotationInfoForDecl(cud, n);
        }
      }
    }
  }

  private void collectOldInfoAnnotationInfoForDecl(final CUDrop cud, final IRNode n) {
    // Does this include the method signature?
    final String name = JavaIdentifier.encodeDecl(cud.getTypeEnv().getProject(), n);
    oldInfo.put(name, null); // Used to mark that it was declared

    final List<PromiseDrop<?>> drops = PromiseDropStorage.getAllDrops(n);
    if (!drops.isEmpty()) {
      System.out.println("Collecting old drops for " + name);
      oldInfo.putAll(name, drops);
    }
    /*
     * if ("testDeps.Deponent".equals(name)) { for(LockModel l :
     * LockRules.getModels(n)) { System.out.println(l.getMessage()); } }
     */
  }

  static class AnnotationInfo {
    final IRNode decl;
    final Collection<PromiseDrop<?>> drops;

    AnnotationInfo(IRNode n, Collection<PromiseDrop<?>> annos) {
      decl = n;
      drops = annos;
    }
  }

  // Written to collect info from the new AST AFTER the old AST is destroyed,
  // promises are parsed/scrubbed, but BEFORE analysis
  //
  // 2. compare with new decls, eliminating those that didn't appear before
  // 3. compare the annotations on the remaining decls, eliminating those that
  // "existed" before
  // 4. scan for dependencies
  public Collection<CUDrop> findDepsForNewlyAnnotatedDecls(Iterable<CodeInfo> newInfos) {
    if (oldInfo.isEmpty()) {
      System.out.println("No old info to compare with");
      return Collections.emptyList();
    }
    final MultiMap<ITypeEnvironment, AnnotationInfo> toScan = new MultiHashMap<ITypeEnvironment, AnnotationInfo>();
    // Find the newly annotated decls
    for (final CodeInfo info : newInfos) {
      // if (AbstractWholeIRAnalysis.debugDependencies) {
      // System.out.println("Checking for new dependencies on "+info.getFileName());
      // }
      // find new decls to compare
      for (final IRNode n : JJNode.tree.bottomUp(info.getNode())) {
        // TODO what about receivers and what not?
        final Operator op = JJNode.tree.getOperator(n);
        if (ClassBodyDeclaration.prototype.includes(op)) {
          if (FieldDeclaration.prototype.includes(op)) {
            for (IRNode vd : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(n))) {
              findDepsForNewlyAnnotatedDecls(toScan, info, vd);
            }
          } else {
            findDepsForNewlyAnnotatedDecls(toScan, info, n);
          }
        } else if (TypeDeclaration.prototype.includes(op)) {
          findDepsForNewlyAnnotatedDecls(toScan, info, n);
        }
      }
    }
    if (toScan.isEmpty()) {
      System.out.println("No decls to scan for.");
    } else {
      for (Entry<ITypeEnvironment, Collection<AnnotationInfo>> e : toScan.entrySet()) {
        scanForDependencies(e.getKey(), e.getValue());
      }
    }
    // reanalyze.removeAll(reprocess);
    reanalyze.removeAll(changed); // These should be invalidated already
    return reanalyze;
  }

  private void findDepsForNewlyAnnotatedDecls(MultiMap<ITypeEnvironment, AnnotationInfo> toScan, CodeInfo info, IRNode n) {
    final String name = JavaIdentifier.encodeDecl(info.getTypeEnv().getProject(), n);
    final Collection<PromiseDrop<?>> oldDrops = oldInfo.remove(name);
    if (oldDrops == null) {
      // New decl, so any annotations are brand-new, and will be analyzed
      // if (AbstractWholeIRAnalysis.debugDependencies) {
      // System.err.println("New decl will be analyzed normally: "+name);
      // }
      return;
    }
    // Otherwise, it's an existing decl
    final Collection<PromiseDrop<?>> newDrops = PromiseDropStorage.getAllDrops(n);
    // First elt just marks that it was declared
    if (oldDrops.size() <= 1) {
      // Any new drops will be new annotations on this decl, so we'll have to
      // scan
      if (!newDrops.isEmpty()) {
        System.out.println("Found all-new annotations for " + name);
        toScan.put(info.getTypeEnv(), new AnnotationInfo(n, newDrops));
      } else {
        // System.err.println("No old/new drops for "+name);
      }
    } else {
      // We'll have to compare the drops to see which are truly new
      if (newDrops.isEmpty()) {
        System.out.println("Only removed drops for " + name);
        return;
      }
      Set<Wrapper> diff = new HashSet<Wrapper>();
      doWrappedDrops(diff, newDrops, true); // add new drops
      doWrappedDrops(diff, oldDrops, false); // remove old drops
      if (!diff.isEmpty()) {
        System.out.println("Found new annotations for " + name);
        Collection<PromiseDrop<?>> diffDrops = new ArrayList<PromiseDrop<?>>();
        for (Wrapper w : diff) {
          diffDrops.add(w.drop);
        }
        toScan.put(info.getTypeEnv(), new AnnotationInfo(n, diffDrops));
      } else {
        System.out.println("No new drops for " + name);
      }
    }
  }

  private static void doWrappedDrops(final Collection<Wrapper> wrapped, final Collection<PromiseDrop<?>> drops, final boolean add) {
    for (PromiseDrop<?> d : drops) {
      if (d == null) {
        continue;
      }
      if (add) {
        wrapped.add(new Wrapper(d));
      } else {
        /*
         * if (wrapped.isEmpty()) { return; // Nothing else to remove }
         */
        wrapped.remove(new Wrapper(d));
      }
    }
  }

  private static class Wrapper {
    final PromiseDrop<?> drop;

    Wrapper(PromiseDrop<?> d) {
      drop = d;
    }

    @Override
    public final int hashCode() {
      return drop.getClass().hashCode() + drop.getMessage().hashCode() + drop.getNode().identity().hashCode();
    }

    /**
     * Checking if the class and message match
     */
    @Override
    public final boolean equals(Object o) {
      if (o instanceof Wrapper) {
        Wrapper w = (Wrapper) o;
        return drop.getNode().equals(w.drop.getNode()) && drop.getClass().equals(w.drop.getClass())
            && drop.getMessage().equals(w.drop.getMessage());
      }
      return false;
    }
  }

  /**
   * Find uses of the given declarations, and add their CUDrops to the queue to
   * be reprocessed
   * 
   * @param decls
   *          A sequence of existing declarations with new annotations
   */
  private void scanForDependencies(ITypeEnvironment te, Iterable<AnnotationInfo> decls) {
    final DeclarationScanner depScanner = new DeclarationScanner() {
      @Override
      protected void scanCUDrop(IBinder binder, CUDrop cud, Set<IRNode> decls) {
        scanCUDropForDependencies(binder, cud, decls);
      }

      @Override
      protected void handleLocals(IBinder binder, CUDrop cud, Collection<IRNode> locals) {
        // Make sure that we reanalyze this file
        reanalyze.add(cud);
      }
    };
    // Types need to be handled differently, because we need to
    // find the uses of the variables declared to be of this type
    final DeclarationScanner typeScanner = new DeclarationScanner() {
      @Override
      protected void scanCUDrop(IBinder binder, CUDrop cud, Set<IRNode> decls) {
        if (cud instanceof PackageDrop) {
          return;
        }
        // System.out.println("Scanning for decls using type in "+cud.javaOSFileName);
        scanCUDropForGivenTypedVarDecls(binder, cud, decls, depScanner);
      }

      @Override
      protected void handleLocals(IBinder binder, CUDrop cud, Collection<IRNode> decls) {
        scanCUDrop(binder, cud, new HashSet<IRNode>(decls));
      }
    };
    // Categorize decls by access
    for (final AnnotationInfo info : decls) {
      final IRNode decl = info.decl;
      final Operator op = JJNode.tree.getOperator(decl);
      if (TypeDeclaration.prototype.includes(op)) {
        collectSubTypeDeclsForScanning(typeScanner, decl, te);
      }
      depScanner.categorizeDecl(decl, op);
    }
    typeScanner.scan(te); // This will add var decls to depScanner to be further
                          // scanned
    depScanner.scan(te);
  }

  private void collectSubTypeDeclsForScanning(DeclarationScanner scanner, IRNode type, ITypeEnvironment te) {
    final Operator op = JJNode.tree.getOperator(type);
    scanner.categorizeDecl(type, op);

    // The enclosing CU needs to be reanalyzed, except in a few corner cases
    // (e.g. an class with only static methods)
    final IRNode cu = VisitUtil.getEnclosingCompilationUnit(type);
    final CUDrop cud = CUDrop.queryCU(cu);
    reanalyze.add(cud);

    for (IRNode sub : te.getRawSubclasses(type)) {
      collectSubTypeDeclsForScanning(scanner, sub, te);
    }
  }

  private static abstract class DeclarationScanner {
    // CU -> decl
    final MultiMap<IRNode, IRNode> localDecls = new MultiHashMap<IRNode, IRNode>();
    final MultiMap<IRNode, IRNode> packageDecls = new MultiHashMap<IRNode, IRNode>();
    // type -> decl
    final MultiMap<IRNode, IRNode> protectedDecls = new MultiHashMap<IRNode, IRNode>();
    final Set<IRNode> publicDecls = new HashSet<IRNode>();

    void categorizeDecl(IRNode decl, Operator op) {
      final int mods;
      if (VariableDeclarator.prototype.includes(op)) {
        mods = VariableDeclarator.getMods(decl);
      } else {
        mods = JavaNode.getModifiers(decl);
      }
      categorizeDecl(decl, mods);
    }

    private void categorizeDecl(IRNode decl, int mods) {
      if (JavaNode.isSet(mods, JavaNode.PRIVATE)) {
        // final IRNode type = VisitUtil.getEnclosingType(decl);
        final IRNode root = VisitUtil.findCompilationUnit(decl);
        /*
         * Not necessary since it can only be used in the same CU
         * 
         * if (NestedTypeDeclaration.prototype.includes(type)) { // If it's in a
         * nested type, it's really accessible anywhere in the CU
         * packageDecls.put(root, decl); } else {
         */
        localDecls.put(root, decl);
        // }
      } else if (JavaNode.isSet(mods, JavaNode.PROTECTED)) {
        final IRNode type = VisitUtil.getEnclosingType(decl);
        final IRNode root = VisitUtil.findCompilationUnit(decl);
        protectedDecls.put(type, decl);
        packageDecls.put(root, decl);
      } else if (JavaNode.isSet(mods, JavaNode.PUBLIC)) {
        publicDecls.add(decl);
      } else { // package
        final IRNode root = VisitUtil.findCompilationUnit(decl);
        packageDecls.put(root, decl);
      }
    }

    void categorizeVarDecl(IRNode decl) {
      // Either a field or local
      final IRNode gparent = JJNode.tree.getParent(JJNode.tree.getParent(decl));
      final Operator op = JJNode.tree.getOperator(gparent);
      if (DeclStatement.prototype.includes(op)) {
        handleLocalDecl(decl);
      } else {
        final int mods = JavaNode.getModifiers(gparent);
        categorizeDecl(decl, mods);
      }
    }

    void handleLocalDecl(IRNode decl) {
      // To handle inits and methods
      // final IRNode bodyDecl = VisitUtil.getEnclosingClassBodyDecl(decl);
      final IRNode root = VisitUtil.findCompilationUnit(decl);
      localDecls.put(root, decl);
    }

    void scan(ITypeEnvironment te) {
      if (!packageDecls.isEmpty()) {
        scanForPackage(te, packageDecls);
      }
      if (!protectedDecls.isEmpty()) {
        scanForSubclass(te, protectedDecls);
      }
      if (!publicDecls.isEmpty()) {
        scanForPublic(te, publicDecls);
      }
      if (!localDecls.isEmpty()) {
        for (Entry<IRNode, Collection<IRNode>> e : localDecls.entrySet()) {
          final CUDrop cud = CUDrop.queryCU(e.getKey());
          handleLocals(te.getBinder(), cud, e.getValue());
        }
      }
    }

    protected abstract void handleLocals(IBinder binder, CUDrop cud, Collection<IRNode> localDecls);

    /**
     * Look for dependencies in the same package as the decl
     * 
     * @param te
     * @param decls
     */
    private void scanForPackage(ITypeEnvironment te, MultiMap<IRNode, IRNode> cu2decls) {
      for (final Entry<IRNode, Collection<IRNode>> e : cu2decls.entrySet()) {
        final Set<IRNode> decls = new HashSet<IRNode>(e.getValue());
        final String name = VisitUtil.getPackageName(e.getKey());
        System.out.println("Scanning for dependencies in package: " + name);
        // TODO does this have the right info?
        final PackageDrop pd = PackageDrop.findPackage(name);
        for (CUDrop cud : pd.getCUDrops()) {
          scanCUDrop(te.getBinder(), cud, decls);
        }
      }
    }

    private void scanForSubclass(ITypeEnvironment te, MultiMap<IRNode, IRNode> type2decls) {
      for (Entry<IRNode, Collection<IRNode>> e : type2decls.entrySet()) {
        final IRNode type = e.getKey();
        final Set<IRNode> decls = new HashSet<IRNode>(e.getValue());
        scanForSubclass(te, type, decls);
      }
    }

    private void scanForSubclass(ITypeEnvironment te, IRNode type, Set<IRNode> decls) {
      System.out.println("Scanning for subclass dependencies: " + JavaNames.getFullTypeName(type));
      for (IRNode sub : te.getRawSubclasses(type)) {
        // TODO check if already on the list first?
        final IRNode cu = VisitUtil.findCompilationUnit(sub);
        final CUDrop cud = CUDrop.queryCU(cu);
        scanCUDrop(te.getBinder(), cud, decls);
        scanForSubclass(te, sub, decls);
      }
    }

    private void scanForPublic(ITypeEnvironment te, Set<IRNode> decls) {
      System.out.println("Scanning for public dependencies: " + this);
      // TODO do i need to check binaries?
      final List<CUDrop> allCus = Sea.getDefault().getDropsOfType(CUDrop.class);
      for (CUDrop cud : allCus) {
        scanCUDrop(te.getBinder(), cud, decls);
      }
    }

    protected abstract void scanCUDrop(IBinder binder, CUDrop cud, Set<IRNode> decls);
  }

  /**
   * Checks for uses of the given declarations
   */
  void scanCUDropForDependencies(IBinder binder, CUDrop cud, Set<IRNode> decls) {
    if (reanalyze.contains(cud)) {
      System.out.println("Already slated to be reanalyzed: " + cud.getJavaOSFileName());
      return; // Already on the list
    }
    final boolean present = hasUses(binder, cud.getCompilationUnitIRNode(), decls);
    if (present) {
      System.out.println("Queued to be reanalyzed: " + cud.getJavaOSFileName());
      reanalyze.add(cud);
    }
  }

  /**
   * Returns true if the CU has any uses of the specified decls
   */
  private boolean hasUses(IBinder binder, IRNode cu, Set<IRNode> decls) {
    for (final IRNode n : JJNode.tree.bottomUp(cu)) {// JavaPromise.bottomUp(cu))
                                                     // {
      final Operator op = JJNode.tree.getOperator(n);
      if (op instanceof IHasBinding) {
        final IBinding b = binder.getIBinding(n);
        if (b == null) {
          final String unparse = DebugUnparser.toString(n);
          if (!unparse.endsWith(" . 1")) {
            System.err.println("Ignoring null binding on " + unparse);
          }
          continue;
        }
        if (decls.contains(b.getNode())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Find the var decls in the CU that are declared with one of the given types
   * (even as a subtype?)
   */
  // TODO what about subclasses of the original?
  void scanCUDropForGivenTypedVarDecls(IBinder binder, CUDrop cud, final Set<IRNode> typeDecls, DeclarationScanner depScanner) {
    final Types types = new Types(binder, typeDecls);
    for (final IRNode n : JJNode.tree.bottomUp(cud.getCompilationUnitIRNode())) {// JavaPromise.bottomUp(cu))
                                                                                 // {
      final Operator op = JJNode.tree.getOperator(n);
      if (VariableDeclaration.prototype.includes(op)) {
        if (ParameterDeclaration.prototype.includes(op)) {
          final IRNode type = ParameterDeclaration.getType(n);
          if (types.usesType(binder, type)) {
            depScanner.handleLocalDecl(n);
          }
        } else if (VariableDeclarator.prototype.includes(op)) {
          final IRNode type = VariableDeclarator.getType(n);
          if (types.usesType(binder, type)) {
            depScanner.categorizeVarDecl(n);
          }
        } else if (EnumConstantDeclaration.prototype.includes(op)) {
          // Always declared in its own type, so no need to scan
          continue;
        } else {
          throw new IllegalStateException("Unknown decl: " + op.name());
        }
      }
      // This may be necessary if the new annotation on the type affects its
      // method calls, like @RegionLock
      else if (MethodDeclaration.prototype.includes(op)) {
        final IRNode type = MethodDeclaration.getReturnType(n);
        if (types.usesType(binder, type)) {
          depScanner.categorizeDecl(n, op);
        }
      }
    }
  }

  private static class Types {
    private final Set<IRNode> typeDecls;

    // private final List<IJavaType> types;

    Types(IBinder binder, Set<IRNode> decls) {
      typeDecls = decls;
      /*
       * types = new ArrayList<IJavaType>(); for(IRNode t : decls) {
       * types.add(binder.getTypeEnvironment().convertNodeTypeToIJavaType(t)); }
       */
    }

    boolean usesType(IBinder binder, IRNode type) {
      final IBinding b = binder.getIBinding(type);
      if (b == null) {
        final String unparse = DebugUnparser.toString(type);
        if (!unparse.endsWith(" . 1")) {
          System.err.println("Ignoring null binding on " + unparse);
        }
        return false;
      }
      // Check if it's exactly one of these types
      if (typeDecls.contains(b.getNode())) {
        return true;
      }
      /*
       * // Check if it's a subtype of one of these types final IJavaType s =
       * b.getTypeEnvironment().convertNodeTypeToIJavaType(b.getNode());
       * for(IJavaType t : types) { if (binder.getTypeEnvironment().isSubType(s,
       * t)) { return true; } }
       */
      return false;
    }
  }
}
