package com.surelogic.dropsea.ir.drops;

import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.surelogic.InRegion;
import com.surelogic.analysis.IIRProject;
import com.surelogic.common.java.Config;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.DropPredicateFactory;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.javac.Projects;
import com.surelogic.xml.PackageAccessor;
import com.surelogic.xml.PromisesXMLAnnotator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.CommonStrings;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.ImportDeclarations;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclarations;
import edu.cmu.cs.fluid.java.operator.UnnamedPackageDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.java.xml.XML;

/**
 * Drop representing a package, suitable for promise and result drops to depend
 * upon. Created and invalidated by the eAST to fAST converter.
 */
public final class PackageDrop extends CUDrop {

  /**
   * From String (package name) to Package&mdash;also from handle identifier to
   * Package.
   */
  private static final ConcurrentHashMap<String, PackageDrop> NAME_TO_INSTANCE = new ConcurrentHashMap<String, PackageDrop>();

  private final IRNode f_packageDeclarationNode; // PackageDeclaration
  private final PackageDrop other;
  private final IIRProject project;

  public IRNode getPackageDeclarationNode() {
    return f_packageDeclarationNode;
  }

  public IIRProject getProject() {
	return project;
  }
  
  @InRegion("DropState")
  private boolean hasPromises = false;

  private PackageDrop(ITypeEnvironment tEnv, String pkgName, IRNode root, IRNode n, boolean fromSrc, PackageDrop o) {
    super(pkgName, root, fromSrc);
    f_packageDeclarationNode = n;
    other = o;
    project = tEnv.getProject();

    /*
    if (pkgName.equals("org.apache.hadoop.yarn.util")) {
    	System.out.println("Creating pkg: "+pkgName);
    }
    */
    // Look for XML annotations
    final String xmlName = PackageAccessor.computeXMLPath(pkgName);
    try {
      int added = PromisesXMLAnnotator.process(tEnv, root, xmlName);
      // System.out.println("Added XML annos: "+added);
      if (added > 0) {
        System.out.println("Found promises for pkg " + pkgName + ": " + added);
        setHasPromises(true);
      }
      /*
       * else if (pkgName.contains("xmlPromises")) {
       * PromisesXMLParser.process(tEnv, root, xmlName); }
       */
    } catch (Exception e) {
      if (!(e instanceof FileNotFoundException)) {
        SLLogger.getLogger().log(Level.SEVERE, "Problem parsing " + xmlName, e);
      } else if (SLLogger.getLogger().isLoggable(Level.FINER)) {
        SLLogger.getLogger().finer("Couldn't find " + xmlName);
      }
    }
    Projects.setProject(root, tEnv.getProject());
  }

  private PackageDrop(ITypeEnvironment tEnv, String pkgName, IRNode root, IRNode n, PackageDrop other) {
    this(tEnv, pkgName, root, n, !XML.getDefault().processingXML(), other);
  }

  public static class Info {
    public final String pkgName;
    public final IRNode root, node;
    public final boolean isFromSrc;

    private Info(PackageDrop d) {
      pkgName = d.getJavaOSFileName();
      root = d.getCompilationUnitIRNode();
      node = d.f_packageDeclarationNode;
      isFromSrc = d.isAsSource();
    }
  }

  public Info makeInfo() {
    return new Info(this);
  }

  public Iterable<IRNode> getTypes() {
    List<IRNode> types = new ArrayList<IRNode>();
    for (Drop d : getDependents()) {
      if (d instanceof CUDrop && !(d instanceof PackageDrop)) {
        for (IRNode t : VisitUtil.getTypeDecls(((CUDrop) d).getCompilationUnitIRNode())) {
          types.add(t);
        }
      }
    }
    return types;
  }

  public Iterable<CUDrop> getCUDrops() {
    List<CUDrop> cus = new ArrayList<CUDrop>();
    for (Drop d : getDependents()) {
      if (d instanceof CUDrop && !(d instanceof PackageDrop)) {
        cus.add((CUDrop) d);
      }
    }
    return cus;
  }

  public void setHasPromises(boolean hasPromises) {
    synchronized (getSeaLock()) {
    	this.hasPromises = hasPromises;
    }
  }

  public boolean hasPromises() {
	synchronized (getSeaLock()) {
		return hasPromises;
	}
  }

  /****************************************************
   * Static methods
   ****************************************************/

  private static final String DEFAULT_NAME = "(default)";

  public static PackageDrop createPackage(IIRProject proj, String name, IRNode root, String id, Config.Type type) {
    final PackageDrop pd = findPackage(name, proj);
    if (root == null) {
      if (pd != null && pd.isValid() && (proj == null || proj == pd.getProject())) {    	
        return pd;
      }
    }
    
    name = CommonStrings.intern(name);
    IRNode n;
    if (root == null) {
      if (name == DEFAULT_NAME) {
        n = JavaNode.makeJavaNode(UnnamedPackageDeclaration.prototype);
      } else {
        n = NamedPackageDeclaration.createNode(Annotations.createNode(noNodes), name);
      }
      // Just to make it into a complete CU
      IRNode imports = ImportDeclarations.createNode(noNodes);
      IRNode types = TypeDeclarations.createNode(noNodes);
      root = edu.cmu.cs.fluid.java.operator.CompilationUnit.createNode(n, imports, types);
      JavaNode.setModifiers(root, !type.fromSourceFile() ? JavaNode.AS_BINARY : JavaNode.ALL_FALSE);
      Projects.setProject(root, proj);
            
      JavaNode.makeFluidJavaRefForPackage(proj, n);
      SLLogger.getLogger().fine("Creating IR for package " + name);
    } else {
      n = CompilationUnit.getPkg(root);
      if (NamedPackageDeclaration.prototype.includes(n)) {
        String name2 = NamedPackageDeclaration.getId(n);
        if (!name.equals(name2)) {
          throw new IllegalArgumentException("name and AST don't match: " + name + ", " + name2);
        }
      } else {
        if (name == null || name.length() == 0) {
          name = "";
        } else {
          throw new IllegalArgumentException("name and AST don't match: " + name);
        }
      }      
    }
    if (name.length() == 0) {
      name = DEFAULT_NAME;
    } else {
      name = CommonStrings.intern(name);
    }

    PackageDrop pkg = new PackageDrop(proj.getTypeEnv(), name, root, n, pd != null && proj != pd.getProject() ? pd : null);
    PackageDrop old = NAME_TO_INSTANCE.put(name, pkg);
    /*
    if (old != null) {
    	System.out.println("Replacing "+pkg);
    }
    */
    if (DEFAULT_NAME.equals(name)) {
      NAME_TO_INSTANCE.put("", pkg);
    }
    if (id != null) {
      NAME_TO_INSTANCE.put(id, pkg);
    }
    return pkg;
  }

  public static PackageDrop findPackage(String name, IRNode context) {
	return findPackage(name, Projects.getEnclosingProject(context));
  }
  
  /**
   * Tries to match both the name and the project, 
   * otherwise returns the "first" package with the same name
   */
  public static PackageDrop findPackage(String name, IIRProject proj) {
    final PackageDrop p = NAME_TO_INSTANCE.get(name);
    PackageDrop next = p;
    while (next != null) {
    	if (proj == next.getProject()) {
    		return next;
    	}
    	next = next.other;
    }
    return p;
  }

  public static Collection<PackageDrop> getKnownPackageDrops() {
    return new ArrayList<PackageDrop>(NAME_TO_INSTANCE.values());
  }

  public static void invalidateAll() {
    synchronized (Sea.getDefault().getSeaLock()) {
      Sea.getDefault().invalidateMatching(DropPredicateFactory.matchExactType(PackageDrop.class));
      NAME_TO_INSTANCE.clear();
    }
  }
}
