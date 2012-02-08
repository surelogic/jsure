package com.surelogic.analysis.uniqueness.store;

import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

import java.net.URL;

import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.analysis.uniqueness.plusFrom.traditional.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.traditional.store.Store;
import com.surelogic.analysis.uniqueness.plusFrom.traditional.store.StoreLattice;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ide.IClassPath;
import edu.cmu.cs.fluid.ide.IClassPathContext;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Implements;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.Throws;
import edu.cmu.cs.fluid.java.operator.TypeFormals;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;

public final class Test extends IDE {
  {
    IDE.prototype = this;
    AnnotationRules.initialize();
  }
  
  
  public static void main(final String[] args) {
    // avoid problems with versioning:
    Version.setDefaultEra(new Era(Version.getVersion()));
    PlainIRNode.setCurrentRegion(new VersionedRegion());

    // run a test.
    new Test().run(args);
  }

  
  
  // ==================================================================
  // === Set up some useful nodes with promises 
  // ==================================================================

  private final IRNode bufField = VariableDeclarator.createNode("buf", 0, null);
  {
    ClassDeclaration.createNode(
        Annotations.createNode(noNodes),
        JavaNode.PUBLIC,
        "Class1",
        TypeFormals.createNode(new IRNode[0]),
        NamedType.createNode("java.lang.Object"),
        Implements.createNode(new IRNode[] {}),
        ClassBody.createNode(new IRNode[] {
            FieldDeclaration.createNode(
                Annotations.createNode(noNodes),
                JavaNode.PRIVATE,
                NamedType.createNode("java.lang.Object"),
                VariableDeclarators.createNode(new IRNode[] { bufField }))
        }));
    UniquenessRules.setIsUnique(bufField, true);
  }

  private final IRNode sharedField = VariableDeclarator.createNode("f", 0, null);
  {
    ClassDeclaration.createNode(
        Annotations.createNode(noNodes),
        JavaNode.PUBLIC,
        "Class2",
        TypeFormals.createNode(new IRNode[0]),
        NamedType.createNode("java.lang.Object"),
        Implements.createNode(new IRNode[] {}),
        ClassBody.createNode(new IRNode[] {
            FieldDeclaration.createNode(
                Annotations.createNode(noNodes),
                JavaNode.PRIVATE,
                NamedType.createNode("java.lang.Object"),
                VariableDeclarators.createNode(new IRNode[] { sharedField }))
        }));
  }

  private final IRNode recDecl = ReceiverDeclaration.prototype.createNode();
  {
    final IRNode methodDecl = MethodDeclaration.createNode(
        Annotations.createNode(noNodes), JavaNode.PUBLIC, 
        TypeFormals.createNode(new IRNode[0]),
        VoidType.prototype.jjtCreate(),
        "methodWithNormalReceiver",
        Parameters.createNode(new IRNode[] {}), 0,
        Throws.createNode(new IRNode[] {}), null);    
    JavaPromise.attachPromiseNode(methodDecl, recDecl);
  }

  private final IRNode brecDecl = ReceiverDeclaration.prototype.createNode();
  {
    final IRNode methodDecl = MethodDeclaration.createNode(
        Annotations.createNode(noNodes), JavaNode.PUBLIC, 
        TypeFormals.createNode(new IRNode[0]),
        VoidType.prototype.jjtCreate(),
        "methodWithBorrowedReceiver",
        Parameters.createNode(new IRNode[] {}), 0,
        Throws.createNode(new IRNode[] {}), null);    
    JavaPromise.attachPromiseNode(methodDecl, brecDecl);
    UniquenessRules.setIsBorrowed(brecDecl, true);
  }

  private final IRNode retDecl = ReturnValueDeclaration.prototype.createNode();
  {
    UniquenessRules.setIsUnique(retDecl, true);
    JJNode.tree.clearParent(retDecl);
  }

  @SuppressWarnings("unused")
  private final IRNode sretDecl = ReturnValueDeclaration.prototype.createNode();

  private final IRNode param = ParameterDeclaration.createNode(
      Annotations.createNode(noNodes), 0, null, "n");
  {
    UniquenessRules.setIsUnique(param, true);
    JJNode.tree.clearParent(param);
  }

  private final IRNode local = VariableDeclarator.createNode("local", 0, null);
  {
	  JJNode.tree.clearParent(local);
  }

  
  
  // ==================================================================
  // === Tests
  // ==================================================================

  public void run(final String[] args) {
    if (args.length == 0) { // run all the tests!
      System.out.println("**** paper ****");
      papertest();
      
      System.out.println();
      System.out.println("**** destructive ****");
      effectstest(true);
      
      System.out.println();
      System.out.println("**** borrowing ****");
      effectstest(false);

      System.out.println();
      System.out.println("**** this ****");
      System.out.println("Testing storing a borrowed this:\n");
      thistest(brecDecl, true);
      System.out.println("\n*********************************\n");
      System.out.println("Testing storing a shared this:\n");
      thistest(recDecl, true);
      System.out.println("\n*********************************\n");
      System.out.println("Testing returning a borrowed this:\n");
      thistest(brecDecl, false);
      System.out.println("\n*********************************\n");
      System.out.println("Testing returning a shared this:\n");
      thistest(recDecl, false);

      System.out.println();
      System.out.println("**** zero ****");
      zerotest();
      
      System.out.println("**** DONE ****");
    } else if (args[0].equals("paper")) {
      papertest();
    } else if (args[0].equals("destructive")) {
      effectstest(true);
    } else if (args[0].equals("borrowing")) {
      effectstest(false);
    } else if (args[0].equals("this")) {
      System.out.println("Testing storing a borrowed this:\n");
      thistest(brecDecl, true);
      System.out.println("\n*********************************\n");
      System.out.println("Testing storing a shared this:\n");
      thistest(recDecl, true);
      System.out.println("\n*********************************\n");
      System.out.println("Testing returning a borrowed this:\n");
      thistest(brecDecl, false);
      System.out.println("\n*********************************\n");
      System.out.println("Testing returning a shared this:\n");
      thistest(recDecl, false);
    } else if (args[0].equals("zero")) {
      zerotest();
    } else {
      System.err.println("Test.Store: unknown test");
    }
  }

  private void papertest() {
    final StoreLattice sl = new StoreLattice(
        new IRNode[] { recDecl, retDecl, param });
    Store store = sl.bottom();
    
    System.out.println("Pristine:");
    System.out.println(sl.toString(store));

    System.out.println("Initial:");
    store = sl.opStart();
    System.out.println(sl.toString(store));

    System.out.println("  this");
    store = sl.opGet(store, recDecl);
    System.out.println(sl.toString(store));

    System.out.println("  .buf");
    store =
      sl.opLoad(store, bufField);
    System.out.println(sl.toString(store));

    System.out.println("  .sync();");
    store = sl.opRelease(store);
    System.out.println(sl.toString(store));

    System.out.println("");

    System.out.println("  this");
    store = sl.opGet(store, recDecl);
    System.out.println(sl.toString(store));

    System.out.println("  .buf");
    store = sl.opLoad(store, bufField);
    System.out.println(sl.toString(store));

    System.out.println("  .getFile()");
    store = sl.opConsume(store,State.UNIQUE);
    store = sl.opNew(store);
    System.out.println(sl.toString(store));

    System.out.println("  -> return value;");
    store = sl.opSet(store, retDecl);
    System.out.println(sl.toString(store));

    System.out.println("");

    System.out.println("  #1=this");
    store = sl.opGet(store, recDecl);
    System.out.println(sl.toString(store));

    System.out.println("  new Buffer()");
    store = sl.opNew(store);
    System.out.println(sl.toString(store));

    System.out.println("  -> #1#.buf;");
    store = sl.opStore(store, bufField);
    System.out.println(sl.toString(store));

    System.out.println("  return;");
    store = sl.opGet(store, retDecl);
    System.out.println(sl.toString(store));

    System.out.println("final:");
    store = sl.opStop(store);
    System.out.println(sl.toString(store));

    System.out.println("consume return value");
    store = sl.opConsume(store,State.UNIQUE);
    System.out.println(sl.toString(store));
  }

  private void effectstest(final boolean destructive) {
    final StoreLattice sl = new StoreLattice(
        new IRNode[] { recDecl, retDecl, local });
    Store store = sl.bottom();
    
    System.out.println("Pristine:");
    System.out.println(sl.toString(store));

    System.out.println("Initial:");
    store = sl.opStart();
    System.out.println(sl.toString(store));

    System.out.println("  this");
    store = sl.opGet(store, recDecl);
    System.out.println(sl.toString(store));

    System.out.println("  .buf");
    store = sl.opLoad(store, bufField);
    System.out.println(sl.toString(store));

    System.out.println("  -> local;");
    store = sl.opSet(store, local);
    System.out.println(sl.toString(store));

    if (destructive) {
      // try a destructive read
      System.out.println("  this");
      store = sl.opGet(store, recDecl);
      System.out.println(sl.toString(store));

      System.out.println("  .buf = null");
      store = sl.opNull(store);
      System.out.println(sl.toString(store));

      System.out.println("  ;");
      store = sl.opStore(store, bufField);
      System.out.println(sl.toString(store));
    }

    System.out.println("  local");
    store = sl.opGet(store, local);
    System.out.println(sl.toString(store));

    System.out.println("  .equals(something) with effects ...");
    store = sl.opExisting(store, State.BORROWED, null);
    System.out.println(sl.toString(store));

    System.out.println("   reads this.Instance");
    store = sl.opDup(store, 1);
    store = sl.opLoadReachable(store);
    System.out.println(sl.toString(store));

    System.out.println("   reads other.Instance");
    store = sl.opDup(store, 0);
    store = sl.opLoadReachable(store);
    System.out.println(sl.toString(store));

    System.out.println("   [popping argument and receiver]");
    store = sl.opConsume(sl.opConsume(store,State.BORROWED),State.BORROWED);
    System.out.println(sl.toString(store));

    System.out.println();

    if (destructive) {
      // now write back field:
      System.out.println("  this");
      store = sl.opGet(store, recDecl);
      System.out.println(sl.toString(store));

      System.out.println("  .buf = local");
      store = sl.opGet(store, local);
      System.out.println(sl.toString(store));

      System.out.println("  ;");
      store = sl.opStore(store, bufField);
      System.out.println(sl.toString(store));

      System.out.println();
    }

    System.out.println("  return null;");
    store = sl.opNull(store);
    System.out.println(sl.toString(store));

    System.out.println("final:");
    store = sl.opStop(store);
    System.out.println(sl.toString(store));

    System.out.println("consume return value");
    store = sl.opConsume(store,State.UNIQUE);
    System.out.println(sl.toString(store));
  }

  private void thistest(final IRNode recDecl, final boolean doStore) {
    final StoreLattice sl = new StoreLattice(new IRNode[] { recDecl });
    Store store = sl.bottom();
    
    System.out.println("Pristine:");
    System.out.println(sl.toString(store));

    System.out.println("Initial:");
    store = sl.opStart();
    System.out.println(sl.toString(store));

    if (doStore) {
      System.out.println("  something");
      store = sl.opExisting(store, State.BORROWED, null);
      System.out.println(sl.toString(store));

      System.out.println("  .f = this // not storing yet");
      store = sl.opGet(store, recDecl);
      System.out.println(sl.toString(store));

      System.out.println("  ; // do store");
      store = sl.opStore(store, sharedField);
      System.out.println(sl.toString(store));

      System.out.println("  return null // not returned yet");
      store = sl.opNull(store);
      System.out.println(sl.toString(store));
    } else {
      System.out.println("  return this // not returned yet");
      store = sl.opGet(store, recDecl);
      System.out.println(sl.toString(store));
    }

    System.out.println("final:");
    store = sl.opStop(store);
    System.out.println(sl.toString(store));

    System.out.println("consume return value");
    store = sl.opCompromise(store);
    System.out.println(sl.toString(store));
  }

  private void zerotest() {
    final StoreLattice sl = new StoreLattice(new IRNode[] { local });
    Store store = sl.bottom();
    
    System.out.println("Pristine:");
    System.out.println(sl.toString(store));

    System.out.println("Initial:");
    store = sl.opStart();
    System.out.println(sl.toString(store));

    System.out.println("  0");
    store = sl.opNull(store);
    System.out.println(sl.toString(store));

    System.out.println("  -> local");
    store = sl.opSet(store, local);
    System.out.println(sl.toString(store));
  }

  

  // ==================================================================
  // === Implement methods for IDE
  // ==================================================================
  
  @Override
  public boolean getBooleanPreference(String key) {
  return false;
  }

  @Override
  public int getIntPreference(String key) {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public IJavaFileLocator getJavaFileLocator() {
    return null;
  }

  @Override
  public URL getResourceRoot() {
    return null;
  }

  @Override
  public String getStringPreference(String key) {
    return null;
  }

  @Override
  protected IClassPathContext newContext(IClassPath path) {
    return null;
  }

  @Override
  public IAnalysisInfo[] getAnalysisInfo() {
	  // TODO Auto-generated method stub
	  return null;
  }  
}
