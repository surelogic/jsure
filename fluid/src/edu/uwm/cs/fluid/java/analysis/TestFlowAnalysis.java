/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/analysis/TestFlowAnalysis.java,v 1.8 2007/08/22 20:59:03 boyland Exp $*/
package edu.uwm.cs.fluid.java.analysis;

import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.BlankInputPort;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.JavaCanonicalizer;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.FlowUnit;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.UnnamedPackageDeclaration;
import edu.cmu.cs.fluid.java.parse.JavaParser;
import edu.cmu.cs.fluid.java.project.JavaComponent;
import edu.cmu.cs.fluid.java.project.JavaIncrementalBinder;
import edu.cmu.cs.fluid.java.project.JavaProjectClassTable;
import edu.cmu.cs.fluid.java.unparse.AbstractUnparserManager;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.parse.ParseException;
import edu.cmu.cs.fluid.project.Project;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.PathFileLocator;
import edu.cmu.cs.fluid.util.ZipFileLocator;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.LabeledLattice.LabeledValue;
import edu.uwm.cs.fluid.util.Lattice;


/**
 * Test an CFG analysis that runs without the Eclipse workbench.
 * For now, that means it runs without using promises.  But it requires
 * that three files are in your fluid.ir.path:
 * <ul>
 * <li> <tt>mini-JDK.prj</tt> The project file (ASCII, mutable) describing a small JDK
 * <li> <tt>mini-JDK.far</tt> The parsed  files from the mini-JDK
 * <li> <tt>mini-JDK-canon.far</tt> The changes neded for canonicalization.
 * </ul>
 * @author boyland
 */
public abstract class TestFlowAnalysis<T, L extends Lattice<T>, A extends FlowAnalysis<T, L>> {

  private final Project allClasses;
  private final IRNode addedClassesRootNode;
  private JavaParser parser;
  private final IBinder binder;
  private final Map<String,IRNode> addedClassesCompNodes = new HashMap<String,IRNode>();
  
  private static final String JDK_NAME = "mini-JDK";
  
  protected TestFlowAnalysis() {
    final JavaProjectClassTable classTable;
    FileLocator floc = IRPersistent.fluidFileLocator;
    try {
      ZipFileLocator basic_floc = makeFarFileLocator(floc,JDK_NAME);
      ZipFileLocator canon_floc = makeFarFileLocator(floc,JDK_NAME+"-canon");
      floc = new PathFileLocator(new FileLocator[]{canon_floc,basic_floc,floc});    
      File prjFile = IRPersistent.fluidFileLocator.locateFile(JDK_NAME+".prj",true);
      allClasses = Project.loadASCII(new FileReader(prjFile),floc);
    } catch (IOException e) {
      throw new FluidError("can't load canonicalized mini-JDK: " + e);
    }
    Version init = allClasses.lookupVersion("canonicalized");
    allClasses.ensureLoaded(init,floc);
    Version.setVersion(init);
    // System.out.println("Starting in version: " + init);
    Era changes = new Era(init);
    Version.setDefaultEra(changes);
    VersionedRegion  packageVR = new VersionedRegion();
    PlainIRNode.setCurrentRegion(packageVR);
    IRNode packNode = UnnamedPackageDeclaration.prototype.createNode();
    JJNode.tree.clearParent(packNode);
    addedClassesRootNode = addComponent("",allClasses.getRoot(),packageVR,packNode);
    addedClassesCompNodes.put("",addedClassesRootNode);
    classTable = new JavaProjectClassTable(floc,allClasses);
    binder = new JavaIncrementalBinder(classTable);
    // IRPersistent.setTraceIO(true); // generate .tri files.
  }

  /**
   * @param floc
   * @param fName
   * @return
   * @throws IOException
   */
  private ZipFileLocator makeFarFileLocator(FileLocator floc, String fName) throws IOException {
    File zfile =  floc.locateFile(fName+".far",true);
    if (zfile == null) {
      System.err.println("Can't open " + fName + ".far");
      System.exit(1);
    }
    ZipFileLocator zipfloc = new ZipFileLocator(zfile,ZipFileLocator.READ);
    return zipfloc;
  }
  
  /**
   * Add a component to the project.
   * @param parent parent node to use
   * @param vr region for component
   * @param rootNode node to use as root
   * @return new component node
   */
  private IRNode addComponent(String name,IRNode parent, VersionedRegion vr, IRNode rootNode) {
    JavaComponent comp = new JavaComponent(vr);
    comp.setRoot(rootNode);
    IRNode compnode = allClasses.newComponent(comp,name);
    Project.getTree().addChild(parent,compnode);
    comp.complete();
    return compnode;
  }

  /**
   * Add the classes in the given file into the project so they can be analyzed.
   * @param file file to read.
   * @return IRNode for teh compilation unit
   * @see #analyzeCompilationUnit(IRNode)
   * @throws IOException if there was a problem reading it.
   */
  public IRNode addCompilatioUnit(File file) throws IOException {    
    VersionedRegion vr = new VersionedRegion();
    PlainIRNode.setCurrentRegion(vr);
    FileInputStream is = new FileInputStream(file);
    if (parser == null) parser = new JavaParser(is);
    JavaParser.ReInit(is);
    IRNode result;
    try {
      result = JavaParser.Start();
    } catch (ParseException e) {
      throw new IOException("Got parse Exception: "+e);
    }
    //System.out.println("Current version is " + Version.getVersion());
    //dumpUnparsedTree(result);
    JavaCanonicalizer canon = new JavaCanonicalizer(binder);
    canon.canonicalize(result);
    System.out.println("After canionicalization, current version is " + Version.getVersion());
    JJNode.dumpTree(System.out, result, 0);
    //dumpUnparsedTree(result);
    IRNode packageDeclaration = CompilationUnit.getPkg(result);
    if (JJNode.tree.getOperator(packageDeclaration) instanceof UnnamedPackageDeclaration) {
      addCompilationUnitContents(result,vr,"",addedClassesRootNode);
    } else {
      String pString = JJNode.getInfo(packageDeclaration);
      String[] path = pString.split(".");
      addCompilationUnitContents(result,vr,pString+".",getPackageCompNode(path));
    }
    return result;
  }

  protected IRNode getPackageCompNode(String[] path) {
    IRNode currentComp = addedClassesRootNode;
    String prefix = "";
    for (int i=0; i < path.length; ++i) {
      String name = path[i];
      String qName = prefix + name;
      IRNode nextNode = addedClassesCompNodes.get(qName);
      if (nextNode == null) {
        VersionedRegion vr = new VersionedRegion();
        PlainIRNode.setCurrentRegion(vr);
        IRNode pkg = NamedPackageDeclaration.createNode(Annotations.createNode(noNodes), qName);
        JJNode.tree.clearParent(pkg);
        nextNode = addComponent(name,currentComp,vr,pkg);
      }
      currentComp = nextNode;
      if (i != 0) prefix += ".";
      prefix += name;
    }
    return currentComp;
  }
  
  protected void addCompilationUnitContents(IRNode cu, VersionedRegion vr, String prefix, IRNode packCompNode) {
    for (IRNode tdecl : JJNode.tree.children(CompilationUnit.getDecls(cu))) {
      String name = JJNode.getInfo(tdecl);
      String qName = prefix+name;
      IRNode cnode = addedClassesCompNodes.get(qName);
      if (cnode == null) {
        cnode = addComponent(name,packCompNode,vr,tdecl);
        addedClassesCompNodes.put(qName,cnode);
      } else {
        JavaComponent comp = (JavaComponent)allClasses.getComponent(cnode);
        // change the component's root
        comp.setRoot(tdecl);
      }
    }
  }
  
  /**
   * Analyze all the methods in all the classes in this compilation unit.
   * @param cu compilation unit that has already been added to the project.
   */
  public void analyzeCompilationUnit(IRNode cu) {
    for (IRNode tdecl : JJNode.tree.children(CompilationUnit.getDecls(cu))) {
      analyzeTypeDeclaration(tdecl);
    }
  }
  
  /**
   * Analyze all the methods with bodies and nested classes in this class/interface declaration.
   * @param tdecl some type declaration node
   */
  public void analyzeTypeDeclaration(IRNode tdecl) {
    for (IRNode member : JJNode.tree.children(VisitUtil.getClassBody(tdecl))) {
      Operator op = JJNode.tree.getOperator(member);
      if (op instanceof TypeDeclaration) {
        analyzeTypeDeclaration(member);
      } else if (op instanceof SomeFunctionDeclaration) {
        analyzeFunction(member);
      }
    }
  }
  
  public void analyzeFunction(IRNode node) {
    FlowUnit op = (FlowUnit)JJNode.tree.getOperator(node);
    A fa = createAnalysis(node,binder);
    final JavaComponentFactory factory = JavaComponentFactory.startUse();
    try {
    	fa.initialize(op.getSource(node, factory));
    	fa.initialize(op.getNormalSink(node, factory));
    	fa.initialize(op.getAbruptSink(node, factory));
    	fa.performAnalysis();
    	IRNode body = SomeFunctionDeclaration.getBody(node);
    	for (IRNode n : JJNode.tree.topDown(body)) {
    		if (select(n)) {
    			printAnalysisResults(fa, n, factory);
    		}
    	}
    } finally {
    	JavaComponentFactory.finishUse(factory);
    }
  }
  
  protected abstract A createAnalysis(IRNode flowUnit, IBinder binder);
  
  protected void printAnalysisResults(A fa, IRNode node, JavaComponentFactory factory) {
	Component cfgComp = factory.getComponent(node);
    if (cfgComp.getEntryPort() instanceof BlankInputPort) return;
    System.out.println("\nNode: " + DebugUnparser.toString(node));
    System.out.print("  Entry:  ");
    printAnalysisResults(fa,(ControlEdge)cfgComp.getEntryPort().getInputs().next());
    System.out.print("  Normal: ");
    printAnalysisResults(fa,(ControlEdge)cfgComp.getNormalExitPort().getOutputs().next());
    System.out.print("  Abrupt: ");
    printAnalysisResults(fa,(ControlEdge)cfgComp.getAbruptExitPort().getOutputs().next());
  }
  
  protected void printAnalysisResults(A fa, ControlEdge e) {
    L l = fa.getLattice();
    LabeledValue<T> rawInfo = fa.getRawInfo(e);
    if (rawInfo == null) {
      System.out.println();
    } else {
      System.out.println(rawInfo.toString(l));
    }
  }
  
  /**
   * If {@link #select(IRNode)} is unchanged, then use the oeprator
   * to decide whether or not to print analysis informationfor it.  By default return true.
   * @param op
   * @return true if node with this operator should have its analysis information printed.
   */
  protected boolean select(Operator op) {
    return true;
  }
  
  /**
   * Return true, if we should print analysis information about this node.
   * @param n node in Java AST
   * @return whether information for this node should be printed
   */
  protected boolean select(IRNode n) {
    return select(JJNode.tree.getOperator(n));
  }
  
  protected static void dumpUnparsedTree(IRNode root) {
    AbstractUnparserManager um = new AbstractUnparserManager(root,JJNode.tree,80);
    for (String s : um.getUnparsedText()) {
      System.out.println(s);
    }
  }

  /**
   * A simple default harness for the test.
   * Each file is analyzed and results printed.  Exceptions are caught,
   * which causes a message to be printed and then we go to the next file.
   * @param files list of files to be added to the mini JDK and then analyzed.
   */
  public void test(String[] files) {
    for (String file : files) {
      try {
        System.out.println("Running analysis on " + file);
        IRNode cu = addCompilatioUnit(new File(file));
        analyzeCompilationUnit(cu);
      } catch (RuntimeException e) {
        System.err.println("Analysis crashed: " + e);
        e.printStackTrace();
      } catch (IOException e) {
        System.err.println("Problem opening/reading file " + file + ": " + e);
      }
    }
  }
}
