/*
 * Created on Nov 8, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops;

import java.util.*;
import java.util.Map.Entry;

import edu.cmu.cs.fluid.ir.IRNode;
import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.ImportDeclarations;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclarations;
import edu.cmu.cs.fluid.java.operator.UnnamedPackageDeclaration;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.util.*;

/**
 * Drop representating a package, suitable for promise and result drops
 * to depend upon.  Created and invalidated by the eAST to fAST converter.
 * 
 * @author Edwin
 * @see edu.cmu.cs.fluid.analysis.util.ConvertToIR
 */
public class PackageDrop extends CUDrop {
  // from String (package name) to Package
  private static final Map<String, PackageDrop> packageMap = new HashMap<String, PackageDrop>();
  public final IRNode node; // PackageDeclaration
  public final List<IRNode> types = new ArrayList<IRNode>();
  private boolean hasPromises = false;
  private final boolean isFromSrc;
  
  private PackageDrop(String pkgName, IRNode root, IRNode n,
		              boolean fromSrc) { 
    super(pkgName, root);
    node      = n;
    isFromSrc = fromSrc;
  }
  
  private PackageDrop(String pkgName, IRNode root, IRNode n) {
	this(pkgName, root, n, !XML.getDefault().processingXML());
  }
  
  public static class Info {
	  public final String pkgName;
	  public final IRNode root, node;
	  public final boolean isFromSrc;
	  
	  private Info(PackageDrop d) {
		  pkgName = d.javaOSFileName;
		  root = d.cu;
		  node = d.node;
		  isFromSrc = d.isFromSrc;
	  }
  }
  
  public Info makeInfo() {
	  return new Info(this);
  }
  
  public void addType(IRNode t) { types.add(t); }
  
  public Iterable<IRNode> getTypes() {
    // clean up?
    return types;
  }

  public void setHasPromises(boolean hasPromises) {
    this.hasPromises = hasPromises;
  }

  public boolean hasPromises() {
    return hasPromises;
  }
  
  public boolean isFromSrc() {
    return isFromSrc;
  }
  
  @Override
  public boolean isAsSource() {
    return isFromSrc;
  }
  
  /****************************************************
   * Static methods
   ****************************************************/
  
  private static final String DEFAULT_NAME = "(default)";
  
  public static PackageDrop createPackage(String name, IRNode root) {
    final PackageDrop pd = findPackage(name);
    if (pd != null) {
      return pd;
    }

    IRNode n;
    if (root == null) {
    	if (name == DEFAULT_NAME) {
    		n = JavaNode.makeJavaNode(UnnamedPackageDeclaration.prototype);
    	} else {
    		n = NamedPackageDeclaration.createNode(Annotations.createNode(noNodes), name);
    	}
    	LOG.fine("Creating IR for package "+name);

    	// Just to make it into a complete CU
    	IRNode imports = ImportDeclarations.createNode(noNodes);
    	IRNode types   = TypeDeclarations.createNode(noNodes);
    	root = edu.cmu.cs.fluid.java.operator.CompilationUnit.createNode(n, imports, types);
    } else {
    	n = CompilationUnit.getPkg(root);
    	if (NamedPackageDeclaration.prototype.includes(n)) {
    		String name2 = NamedPackageDeclaration.getId(n);
    		if (!name.equals(name2)) {
    			throw new IllegalArgumentException("name and AST don't match: "+name+", "+name2);
    		}
    	} else {
    		if (name == null || name.length() == 0) {    			    		
    			name = "";
    		} else {
    			throw new IllegalArgumentException("name and AST don't match: "+name);    			 
    		}
    	}
    }
  	if (name.length() == 0) {
		name = DEFAULT_NAME;
	} else {
		name = CommonStrings.intern(name);
	}
  	
    PackageDrop pkg = new PackageDrop(name, root, n);
    packageMap.put(name, pkg);
    if (DEFAULT_NAME.equals(name)) {
      packageMap.put("", pkg);
    }
    return pkg;
  }
  
  public static PackageDrop findPackage(String name) {
    return packageMap.get(name);
  }
  
  /**
   * @return Iterator of PackageDrops
   */
  public static Iteratable<PackageDrop> allPackages() {
    return new FilterIterator<Entry<String,PackageDrop>,PackageDrop>(packageMap.entrySet().iterator()) {
      @Override protected PackageDrop select(Entry<String,PackageDrop> e) {
        return e.getValue();
      }      
    };
  }
}


