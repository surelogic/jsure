package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/// LocalEnvironment
public class LocalEnvironment {
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("FLUID.bind");

  public static final LocalEnvironment cleanEnv = new LocalEnvironment();

  public IRNode findSimpleType(String name) {
    return null; // throw new FluidError("Couldn't find type "+name);
  }

  public static String getPackageName(IRNode cu) {
    // Prep For checking if marked as the correct package
    IRNode pkg = CompilationUnit.getPkg(cu);
    String pName = ""; // default for unnamed pkg

    if (JJNode.tree.getOperator(pkg) instanceof NamedPackageDeclaration) {
      pName = NamedPackageDeclaration.getId(pkg);
    }
    return pName;
  }

  // NEW for inner classes
  static void insertInners(String name, IRNode type, Hashtable<String,IRNode> imports) {
    Iterator<IRNode> enm = VisitUtil.getInnerClasses(type);
    while (enm.hasNext()) {
      IRNode n = enm.next();
      String name2 = JJNode.getInfo(n);
      String inner = name + "." + name2;
      imports.put(inner, n);
      imports.put(name2, n); // FIX hack
      // System.out.println("Inserting "+inner+", "+name);
      insertInners(inner, n, imports);
    }
  }

  static void initImports(
    ITypeEnvironment tEnv,
    IRNode cu,
    String pn,
    Vector<String> demands,
    Hashtable<String,IRNode> imports) {
    // System.out.println("cu = "+JJNode.tree.getOperator(cu));

    // A1) Types in the same compilation unit
    Iterator<IRNode> enm = VisitUtil.getTypeDecls(cu);
    while (enm.hasNext()) {
      IRNode type = enm.next();
      // System.out.println("type = "+JJNode.tree.getOperator(type));
      String name = JJNode.getInfo(type); // ???Decl.getId
      imports.put(name, type);
      insertInners(name, type, imports);
    }

    // B3) Types in the same package
    demands.addElement(pn);

    // B4) Types in "java.lang"
    demands.addElement("java.lang");

    // A2) Types specifically imported
    // B5) Types imported on demand from specific packages
    enm = JJNode.tree.children(CompilationUnit.getImps(cu));
    while (enm.hasNext()) {
      IRNode imp = enm.next();
      IRNode item = ImportDeclaration.getItem(imp);
      Operator op = JJNode.tree.getOperator(item);

      if (op == DemandName.prototype) {
        demands.addElement(DemandName.getPkg(item));
      } else if (op == NamedType.prototype) {
        String name = NamedType.getType(item);
        // System.out.println(DebugUnparser.toString(CompilationUnit.getImps(cu)));
        IRNode type = tEnv.findNamedType(name);
        if (type == null) {
          LOG.warning(
            "Couldn't find " + name + " as a imported type.  Ignoring it.");
        } else {
          imports.put(name.substring(name.lastIndexOf('.') + 1), type);
        }
      } else
        throw new FluidError("got a bad import " + op);
    }
  }

  public static LocalEnvironment createCUenv(
    final ITypeEnvironment tEnv,
    final IRNode cu) { // FIX assumes that pName is correct
    final String pn = getPackageName(cu);
    return new LocalEnvironment() {
      final Vector<String> demands = new Vector<String>();
      final Hashtable<String,IRNode> imports = new Hashtable<String,IRNode>();
      {
        LocalEnvironment.initImports(tEnv, cu, pn, demands, imports);
      }

      @Override
      public IRNode findSimpleType(String name) {
        /*
				 * int dot = name.indexOf('.'); if (dot >= 0) { // found a qualified
				 * name // FIX probably an inner class return null; }
				 */
        // First, look at imports (A)
        // System.out.print("looking at "+name+" ... ");
        IRNode type = imports.get(name);
        if (type == null) {
          // System.out.println("No");

          // Otherwise, look at each demand (B)
          for (int i = 0; i < demands.size(); i++) {
            String pkg = demands.elementAt(i);
            String qn = pkg + "." + name;
            // System.out.println("looking at "+qn);
            type = tEnv.findNamedType(qn);
            if (type != null) {
              if (imports.put(name, type) != null) {
                throw new FluidError(
                  "Somehow " + name + " was already imported");
              }
              return type;
            }
          }
        }
        // else System.out.println("Yes");
        return type;
      }
    };
  }
}
