/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/JavaProject.java,v 1.32 2008/06/24 19:13:17 thallora Exp $
 */
package edu.cmu.cs.fluid.java.project;

import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaClassTable;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaCanonicalizer;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.Expression;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NameExpression;
import edu.cmu.cs.fluid.java.operator.NameType;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.UnnamedPackageDeclaration;
import edu.cmu.cs.fluid.java.parse.JavaParser;
import edu.cmu.cs.fluid.java.unparse.AbstractUnparserManager;
import edu.cmu.cs.fluid.parse.ParseException;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.project.Component;
import edu.cmu.cs.fluid.project.Project;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.CountInstances;
import edu.cmu.cs.fluid.util.DirectoryFileLocator;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.MemorySafety;
import edu.cmu.cs.fluid.util.PathFileLocator;
import edu.cmu.cs.fluid.util.QuickProperties;
import edu.cmu.cs.fluid.util.ZipFileLocator;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;


/**
 * Code to help load Java code into fluid projects.
 * @author boyland
 */
public class JavaProject {
  private static final Logger LOG = SLLogger.getLogger("FLUID.project");
  static final SyntaxTreeInterface tree = JJNode.tree;

  static Project project;
  static Tree ptree;
  static JavaParser parser;
  
  static FileLocator floc = IRPersistent.fluidFileLocator; 
  
  /**
   * This is the amount of memory held to handle saving files when memory gets full.
   * This should be about 5-10% or so of the max heap size
   */
  private static int memorySafetyHedgeBytes;
  
  /**
   * This the amount of memory that must be free before parsing any file.
   * It must be at least enough to get through initial setup (comparing file names).
   * This value should be at least 100kbyte.  Large sizes work fine too and reduce the
   * chance that a parse will be interrupted and have to be done again.
   */
  private static int minFreeMemoryBytes;

  static {
    Properties properties = QuickProperties.getInstance().getProperties();
    String value = properties.getProperty("fluid.ir.JavaProject.SafetyHedge",
        "16000000");
    memorySafetyHedgeBytes = Integer.parseInt(value);

    value = properties.getProperty("fluid.ir.JavaProject.MinFreeMemory",
        "1000000");
    minFreeMemoryBytes = Integer.parseInt(value);
    LOG.info("Using hedge of " + memorySafetyHedgeBytes + " and min memory of " + minFreeMemoryBytes);
  }

  /**
   * Create a Java project from a list of directories. Each list of directories
   * is added as a JavaComponent under the root nodes of the project, and all
   * its .java files are subcomponents and all its subdirectories are their own
   * subcomponents.
   * 
   * @param args
   *          load/store followed by the name of the project followed by a list
   *          of directories
   */
  public static void main(String[] args) {
    // IRPersistent.setDebugIO(true);
    Project.ensureLoaded();
    try {
      if (args[0].equals("parse")) {
        doParse(args);
      } else if (args[0].equals("store")) {
        IRPersistent.setTraceIO(true);
        File currentDir = new File(System.getProperty("user.dir"));
        File projectDir = new File(currentDir,args[1]);
        if (projectDir.exists() || !projectDir.mkdir()) {
          System.err.println("Cannot create directory: " + projectDir);
          System.err.println("Please delete or rename current occupier.");
          System.exit(1);
        }
        floc = new DirectoryFileLocator(projectDir);
        doStore(args);
        floc.commit();
      } else if (args[0].equals("store-far")) {
        floc = new ZipFileLocator(new File(args[1]+".far"),ZipFileLocator.WRITE);
        IRPersistent.setTraceIO(true);
        doStore(args);
        floc.commit();
      } else if (args[0].equals("load")) {
        setProjectFloc(args[1]);
        IRPersistent.setTraceIO(true);
        doLoad(args);
      } else if (args[0].equals("load-far")) {
        File zfile =  IRPersistent.fluidFileLocator.locateFile(args[1]+".far",true);
        ZipFileLocator zipfloc = new ZipFileLocator(zfile,ZipFileLocator.READ);
        floc = new PathFileLocator(new FileLocator[]{zipfloc,floc});
        /*ZipFileLocator zipfloc = new ZipFileLocator(new File(args[1]+".far"),ZipFileLocator.READ);
        floc = new PathFileLocator(new FileLocator[]{zipfloc,floc});*/
        IRPersistent.setTraceIO(true);
        doLoad(args);
      } else if (args[0].equals("bind-far")) {
    	File zfile =  IRPersistent.fluidFileLocator.locateFile(args[1]+".far",true);
        ZipFileLocator zipfloc = new ZipFileLocator(zfile,ZipFileLocator.READ);
        floc = new PathFileLocator(new FileLocator[]{zipfloc,floc});
        IRPersistent.setTraceIO(true);
        doBind(args);
      } else if (args[0].equals("canon-far")) {
        File zfile =  IRPersistent.fluidFileLocator.locateFile(args[1]+".far",true);
        ZipFileLocator zipfloc = new ZipFileLocator(zfile,ZipFileLocator.READ);
        floc = new PathFileLocator(new FileLocator[]{zipfloc,floc});
        IRPersistent.setTraceIO(true);
        doCanon(args);
      } else if (args[0].equals("append")) {
        System.out.println("append not implemented.");
        System.exit(1);
        setProjectFloc(args[1]);
        //TODO: doAppend(args);
      } else if (args[0].equals("share-far")) {
        ZipFileLocator readfloc = new ZipFileLocator(new File(args[1]+".far"),ZipFileLocator.READ);
        ZipFileLocator sharefloc = new ZipFileLocator(new File(args[3]+".far"),ZipFileLocator.WRITE);
        floc = new PathFileLocator(new FileLocator[]{readfloc,sharefloc,floc});
        doShare(args);
      } else {
        System.err.println("Unknown command " + args[0]);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Something bad happend " + e);
    }
  }
  
  private static void setProjectFloc(String pname) {
    File projectDir = new File(pname);
    if (!projectDir.exists() && !projectDir.isDirectory()) {
      System.err.println("Cannot open directory: " + projectDir);
      System.err.println("Please store something first.");
      System.exit(1);
    }
    floc = new DirectoryFileLocator(projectDir);    
  }
  
  private static Era era;
  private static List<String> errorFiles = new ArrayList<String>();
  private static List<JavaComponent> toSave = new ArrayList<JavaComponent>(); //list of Java components
  private static VersionedRegion inProcess = null;
  private static int phases = 0;
  private static int components = 0;
  
  private static void savePending() {
    ++phases;
    CountInstances.report();
    CountInstances.reset();
    if (inProcess != null) inProcess.destroy();
    era.complete();
    try {
      project.saveDelta(era,floc);
    } catch (IOException e1) {
      LOG.severe("Unable to save project delta for " + era);
    }
    for (Iterator it = toSave.iterator(); it.hasNext();) {
      JavaComponent jc = (JavaComponent)it.next();
      IRNode compNode = jc.getProjectNode();
      LOG.info("Saving " + project.getComponentName(compNode));
      try {
        jc.saveDelta(era,floc);
      } catch (IOException ex) {
        LOG.severe("Unable to save " + jc);
        ex.printStackTrace();
      }
    }
    try {
      era.store(floc);
    } catch (IOException e) {
      LOG.severe("Unable to store Era " + era);
    }
    for (Iterator it = toSave.iterator(); it.hasNext();) {
      JavaComponent jc = (JavaComponent)it.next();
      jc.unload();
    }
    toSave.clear();
    era = new Era(Version.getVersion());
    Version.setDefaultEra(era);
    Version.bumpVersion();
    SlotInfo.gc();
  }
  
  private static MemorySafety memoryHedge = new MemorySafety(memorySafetyHedgeBytes) {
    @Override
    protected void recover() {
      savePending();
    }  
  };
  
  public static void doParse(String[] args) throws IOException{
   Version alpha = Version.getInitialVersion();
    era = new Era(alpha);
    Version.setVersion(alpha);
    Version.setDefaultEra(era);
    Version.bumpVersion();

    for (int i=1; i < args.length; ++i) {
      String name = args[i];
      File f = new File(name);
      InputStream is = new BufferedInputStream(new FileInputStream(f));
      if (parser == null) parser = new JavaParser(is);
      JavaParser.ReInit(is);
       try {
        LOG.info("Parsing file " + name);
        IRNode root = JavaParser.Start();
        tree.clearParent(root);
        dumpUnparsedTree(root);
        Iterator classes = tree.children(CompilationUnit.getDecls(root));
        memoryHedge.checkFree(100000);
        while (classes.hasNext()) {
          IRNode tdecl = (IRNode)classes.next();
          String tname = JJNode.getInfo(tdecl);
          System.out.println("Found class " + tname + " in " + name);
         }
      } catch (ParseException e) {
        System.err.println("Parse exception in file " + name + "\n" + e);
        continue;
      } catch (IllegalChildException e) {
        System.err.println("IllegalChildException in file " + name + "\n");
        e.printStackTrace(System.out);
      }
    }
  }

  /**
   * @param root
   */
  private static void dumpUnparsedTree(IRNode root) {
    AbstractUnparserManager um = new AbstractUnparserManager(root,JJNode.tree,80);
    for (String s : um.getUnparsedText()) {
      System.out.println(s);
    }
  }
  
  public static void doStore(String[] args) throws IOException {
    Version alpha = Version.getInitialVersion();
    era = new Era(alpha);
    Version.setVersion(alpha);
    Version.setDefaultEra(era);
    Version.bumpVersion();
    final String projectName = args[1];
    project = new Project(projectName);
    ptree = Project.getTree();
    String prefix = "";
    IRNode parent = project.getRoot();
    for (int i=2; i < args.length; ++i) {
      /*if (args[i].equals("--prefix")) {
        prefix = args[++i];
        String[] ps = prefix.split(".");
        IRNode dirNode = project.getRoot();
        for (int j=0; j < ps.length; ++j) {
          IRNode next = project.getNamedChild(parent,ps[j]);
          if (next == null) {
            
            next = project.newComponent()
          }
        }
      }*/
      File f = new File(args[i]);
      if (f.isDirectory()) {
        VersionedRegion  packageVR = new VersionedRegion();
        PlainIRNode.setCurrentRegion(packageVR);
        IRNode pack = UnnamedPackageDeclaration.prototype.createNode();
        finishPackage("",parent, f, packageVR, pack, prefix);
      } else {
        System.err.println("Ignoring non-directory: " + args[i]);
      }
    }
    Version done = Version.getVersion();
    memoryHedge.release();
    savePending();
    System.out.println("Entire project of " + components + " component" + 
        (components == 1 ? "" : "s") + " saved using " + phases + 
        " era" + (phases == 1 ? "." : "s."));
    project.assignVersionName(done,"initial");
    Writer w = new FileWriter(projectName+".prj"); // or using floc ?
    project.storeASCII(w);
    if (errorFiles.size() > 0) {
      System.out.println("Errors found in the following files:");
    }
    for (String errorName : errorFiles) {
      System.out.println("  " + errorName);
    }
  }
  
  /**
   * @param parent parent node in project tree
   * @param f file for directory of package
   * @param vr versioned region for package declaration
   * @param packNode package declaration node
   * @param prefix string to affix to nested classes and packages
   * @throws IOException
   */
  private static void finishPackage(String name,IRNode parent, File f, VersionedRegion vr, IRNode packNode, String prefix) throws IOException {
    tree.clearParent(packNode);
    IRNode pcompnode = addJavaComponent(name,parent, vr, packNode);
    getContents(f,pcompnode,prefix);
  }

  /**
   * Add a component to the project.
   * @param parent parent node to use
   * @param vr region for component
   * @param rootNode node to use as root
   * @return new component node
   */
  private static IRNode addJavaComponent(String name,IRNode parent, VersionedRegion vr, IRNode rootNode) {
    JavaComponent comp = new JavaComponent(vr);
    comp.setRoot(rootNode);
    IRNode compnode = project.newComponent(comp,name);
    ptree.addChild(parent,compnode);
    comp.complete();
    toSave.add(comp);
    ++components;
    return compnode;
  }

  static void getContents(File dir, IRNode parent, String prefix) throws IOException {
    File[] members = dir.listFiles();
    for (int i=0; i < members.length; ++i) {
      memoryHedge.checkFree(minFreeMemoryBytes);
      System.out.println("Just checked that sufficient memory remains.");
      File f = members[i];
      String name = f.getName();
      VersionedRegion vr = new VersionedRegion();
      PlainIRNode.setCurrentRegion(vr);
      if (f.isDirectory()) {
        if (name.equals("CVS")) continue;
        System.out.println("Recursing in directory " + name);
        IRNode pack = NamedPackageDeclaration.createNode(Annotations.createNode(noNodes), prefix + name);
        finishPackage(name,parent,f,vr,pack,prefix+name+".");
      } else if (name.endsWith(".java")) {
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        if (parser == null) parser = new JavaParser(is);
        JavaParser.ReInit(is);
        inProcess = vr;
        try {
          LOG.info("Parsing file " + name);
          IRNode root = JavaParser.Start();
          tree.clearParent(root);
          Iterator classes = tree.children(CompilationUnit.getDecls(root));
          memoryHedge.checkFree(100000);
          while (classes.hasNext()) {
            IRNode tdecl = (IRNode)classes.next();
            String tname = JJNode.getInfo(tdecl);
            System.out.println("Found class " + prefix + tname + " in " + name);
            addJavaComponent(tname,parent, vr, tdecl);
          }
        } catch (ParseException e) {
          System.err.println("Parse exception in file " + name + "\n" + e);
          errorFiles.add(name);
          continue;
        } catch (IllegalChildException e) {
          System.err.println("IllegalChildException in file " + name + "\n");
          e.printStackTrace(System.out);
          errorFiles.add(name);
        } catch (OutOfMemoryError e) {
          System.err.println("Parsing interrupted.");
          memoryHedge.handle(e);
          --i; // redo current iteration
        } finally {
          inProcess = null;
        }
      }
    }
  }
  
  public static void doLoad(String[] args) throws IOException {
    String projectName = args[1];
    String versionName = args.length > 2 ? args[2] : "initial";
    // IRPersistent.setTraceIO(true);
    File prjFile = IRPersistent.fluidFileLocator.locateFile(projectName+".prj",true);
    project = Project.loadASCII(new FileReader(prjFile),floc);
    // project = Project.loadASCII(new FileReader(projectName+".prj"),floc);
    Version init = project.lookupVersion(versionName);
    if (init == null) {
      System.err.println("Unregistered version " + versionName);
      return;
    }
    project.loadDeltaForEras(init.getEra(),floc);
    Version.setVersion(init);
    /*
    SlotInfo si;
    try {
      si = SlotInfo.findSlotInfo("Config.components.Digraph.children");
      IRSequence seq = (IRSequence)project.getRoot().getSlotValue(si);
      seq.describe(System.out);
    } catch (SlotNotRegisteredException e) {
      System.out.println("Wrong name for slot info");
    }
    */
    Iterator comps = Project.getTree().topDown(project.getRoot());
    while (comps.hasNext()) {
      Component comp = project.getComponent((IRNode)comps.next());
      if (comp == null) {
        System.out.println("Found a null component (probably the root)");
      } else if (comp instanceof JavaComponent) {
        JavaComponent jc = (JavaComponent)comp;
        jc.ensureLoaded(init,floc);
        IRNode root = jc.getRoot();
        System.out.println("Found a Java Component: " + comp + " with root " + root);
        // System.out.println(DebugUnparser.toString(root));
        System.out.println("Operator is " + JJNode.tree.getOperator(root).name());
        dumpUnparsedTree(root);
        comp.unload();
      }
    }
  }
  
  public static void doBind(String[] args) throws FileNotFoundException, IOException {
    String projectName = args[1];
    String versionName = args.length > 2 ? args[2] : "initial";
    // IRPersistent.setTraceIO(true);
    File prjFile = IRPersistent.fluidFileLocator.locateFile(projectName+".prj",true);
    project = Project.loadASCII(new FileReader(prjFile),floc);
    Version init = project.lookupVersion(versionName);
    Version.setVersion(init);
    System.out.println("Starting in version: " + init);
    Version.clampCurrent();
    IJavaClassTable jct = new JavaProjectClassTable(floc,project);
    JavaIncrementalBinder binder = new JavaIncrementalBinder(jct);
    ITypeEnvironment te = binder.getTypeEnvironment();
    for (Iterator it = jct.allNames().iterator(); it.hasNext();) {
      String name = (String) it.next();
      System.out.println("Found a Java Class: " + name);
      IRNode root = te.findNamedType(name);
      System.out.println("root is " + root);
      System.out.println(DebugUnparser.toString(root));
      for (Iterator e = tree.bottomUp(root); e.hasNext();) {
        IRNode n = (IRNode)e.next();
        Operator op = tree.getOperator(n);
        if (op instanceof NameType || op instanceof NameExpression || op instanceof MethodCall) {
          System.out.println("Found use: " + DebugUnparser.toString(n));
          try {
            IBinding boundTo = binder.getIBinding(n);
            System.out.println("  bound to " + IBinding.Util.debugString(boundTo));
          } catch (RuntimeException e1) {
            System.out.println("Problem binding: " + e1);
            e1.printStackTrace(System.out);
            return;
          }
        }
        if (op instanceof Expression) {
          System.out.print("Type of " + DebugUnparser.toString(n) + " = ");
          try {
            System.out.println(binder.getJavaType(n));
          } catch (RuntimeException ex) {
            System.out.println("<crashes>");
            ex.printStackTrace();
          }
        }
      }
    }
  }
  
  public static void doCanon(String[] args) throws IOException {
    String projectName = args[1];
    String versionName = args.length > 2 ? args[2] : "initial";
    // IRPersistent.setTraceIO(true);
    File prjFile = IRPersistent.fluidFileLocator.locateFile(projectName+".prj",true);
    project = Project.loadASCII(new FileReader(prjFile),floc);
    Version init = project.lookupVersion(versionName);
    Version.setVersion(init);
    System.out.println("Starting in version: " + init);
    Era changes = new Era(init);
    Version.setDefaultEra(changes);
    IJavaClassTable jct = new JavaProjectClassTable(floc,project);
    JavaIncrementalBinder binder = new JavaIncrementalBinder(jct);
    ITypeEnvironment te = binder.getTypeEnvironment();
    JavaCanonicalizer jcanon = new JavaCanonicalizer(binder);
    for (Iterator it = jct.allNames().iterator(); it.hasNext();) {
      String name = (String) it.next();
      System.out.println("Found a Java Class: " + name);
      IRNode root = te.findNamedType(name);
      // in general it's a bad idea to call getVersion: it caauses versions to
      // freeze.  This is just fpr debugging:
      System.out.println("at version " + Version.getVersion() + ", root is " + root);
      System.out.println(DebugUnparser.toString(root));
      System.out.flush();
      VersionedRegion vr= VersionedRegion.getVersionedRegion(root);
      PlainIRNode.setCurrentRegion(vr);
      jcanon.canonicalize(root);
      dumpUnparsedTree(root);
      JJNode.dumpTree(System.out,root,0);
    }
    Version complete = Version.getVersion();
    changes.complete();
    floc = new ZipFileLocator(new File(projectName+"-canon.far"),ZipFileLocator.WRITE);
    project.assignVersionName(complete,"canonicalized");
    project.saveDelta(changes,floc);
    project.saveComponentDeltaForEra(changes,floc);
    changes.store(floc);
    floc.commit();
    project.storeASCII(new FileWriter(prjFile));
  }
  
  public static void doShare(String[] args) throws IOException {
    String projectName = args[1];
    String versionName = args.length > 2 ? args[2] : "initial";
    Version alpha = Version.getInitialVersion();
    // IRPersistent.setTraceIO(true);
    project = Project.loadASCII(new FileReader(projectName+".prj"),floc);
    Version init = project.lookupVersion(versionName);
    project.loadDeltaForEras(init.getEra(),floc);
    Version.setVersion(init);
    Project shared = project.share(projectName+"-shared",init);
    Iterator comps = Project.getTree().topDown(project.getRoot());
    while (comps.hasNext()) {
      IRNode node = (IRNode)comps.next();
      Component comp = project.getComponent(node);
      if (comp == null) {
        System.out.println("Found a null component (probably the root)");
      } else {
        Component sharedComp = comp.share(init);
        sharedComp.saveSnapshot(alpha,floc);
        comp.unload();
        sharedComp.unload();
      }
    }
    shared.saveSnapshot(alpha,floc);
    shared.storeASCII(new FileWriter(projectName+"-shared.prj"));
  }

}
