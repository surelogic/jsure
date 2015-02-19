package com.surelogic.opgen;

import java.util.Map;

import com.surelogic.opgen.syntax.Child;
import com.surelogic.opgen.syntax.OpSyntax;


public abstract class AbstractOperatorProxyNodeGen extends AbstractOperatorNodeGen {
  public static final String STD_PREFIX = "com.surelogic.proxy";

  public AbstractOperatorProxyNodeGen(String astPrefix, String implPrefix, String suffix) {
    super(astPrefix, implPrefix, suffix);
  }

  @Override
  protected String makeNodeName(String name) {
    return name+"ProxyNode";
  }

  protected void generateJavaOperatorBody(String name) {
    printJava("  protected "+name+"(IRNode n) {\n");
    printJava("    super(n);\n");
    printJava("    Operator thisOp = this.getOperator();\n");
    printJava("    Operator op = JavaNode.tree.getOperator(n);\n");
    printJava("    if (!thisOp.includes(op)) {\n");
    printJava("      throw new IllegalArgumentException(\"Node is not a \"+op.name());\n");
    printJava("    }\n");
    printJava("  }\n");
    printJava("  public abstract Operator getOperator();\n");
    printJava("\n");
  }

  private boolean countUsage = false;
  
  @Override
  protected final void generateNodeFactories(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg);
    printJava("import java.util.*;\n");
    printJava("import com.surelogic.ast.*;\n");
    printJava("import edu.cmu.cs.fluid.util.*;\n");
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n");
    for (String p : packagesAppearing()) {
      printJava("import "+OperatorGen.STD_PKG_PREFIX+"."+p+".*;\n");
      printJava("import "+astPrefix+p+".*;\n");
      printJava("import "+pkgPrefix+p+".*;\n");
    }
    printJava("\n");
    if (ps.getRoot() == null) {
      printJava("public class "+ps.getName()+"NodeFactories {\n");
      printJava("  public interface ITranslator {\n");
      printJava("    public IJavaOperatorNode translate(IRNode n);\n");
      printJava("  }\n");
      printJava("\n");    
      printJava("  protected static Map<IRNode, IBinding> bindings = new IRNodeHashedMap<IBinding>();\n");
      printJava("\n");
      printJava("  public static void addBinding(IRNode n, IBinding b) {\n");
      printJava("    bindings.put(n, b);\n");
      printJava("  }\n");
      printJava("\n");
      printJava("  public static IBinding getBinding(IRNode n) {\n");
      printJava("    return bindings.get(n);\n");
      printJava("  }\n");
      printJava("\n");
      if (countUsage) {
        printJava("  protected static final Map<Operator, Integer> usage = new HashMap<Operator, Integer>();\n");
        printJava("\n");
        printJava("  public static void bumpUsage(Operator op) {\n");
        printJava("    Integer i = usage.get(op);\n");
        printJava("    if (i == null) {\n");
        printJava("      usage.put(op, IntegerTable.newInteger(1));\n");
        printJava("    } else {\n");  
        printJava("      usage.put(op, IntegerTable.incrInteger(i));\n");
        printJava("    }\n");
        printJava("  }\n\n");  
        printJava("  public static void printUsage() {\n");
        printJava("    System.out.println(\"Printing factory usage\");\n");
        printJava("    for (Operator op : usage.keySet()) {\n");
        printJava("      System.out.println(op.name()+\": \"+usage.get(op));\n");
        printJava("    }\n");
        printJava("  }\n\n"); 
      }
      printJava("\n");
      printJava("  protected static final Map<Operator, ITranslator> translators = new HashMap<Operator, ITranslator>();\n");      
      printJava("\n");
      printJava("  public static IJavaOperatorNode translateToIJavaOperatorNode(IRNode n) {\n");
      insertTranslateChecks("IJavaOperatorNode", "IJavaOperatorNode");
      printJava("    Operator op = JavaNode.tree.getOperator(n);\n");
      printJava("    if (op instanceof JavaOperator) {\n"); 
      generateTranslateEnding("IJavaOperatorNode");
    } else {
      printJava("public class "+ps.getName()+"NodeFactories extends "+
                ps.getRoot()+"NodeFactories {\n");
    }
    printJava("  static {\n");
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      if (!isAbstract(s)) { 
        printJava("    translators.put("+s.name+".prototype, "+makeNodeName(s.name)+".translator);\n");
      }
    }
    printJava("  }\n");
    printJava("\n");    
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      final String type  = makeInterfaceName(s.name);
      final String rtype = convertOptNodes ? makeConvertedInterfaceName(s.name) : type;
      generateClassBodyDeclJavadoc("Creates a proxy "+type+" if not already that type");
      printJava("  public static "+rtype+" translateTo"+type+"(IRNode n) {\n");
      //printJava("    System.out.println(\"Translating to "+type+"\\n\");\n");
      insertTranslateChecks(type, rtype);  
      printJava("    Operator op = JavaNode.tree.getOperator(n);\n");
      printJava("    if ("+s.name+".prototype.includes(op)) {\n");
      generateTranslateEnding(rtype);  
    }
    printJava("}\n");
  }

  private void insertTranslateChecks(final String type, final String rtype) {
    printJava("    if (n instanceof "+rtype+") {\n");
    printJava("      return ("+rtype+") n;\n");
    printJava("    }\n");  
    if (convertOptNodes && !type.equals(rtype)) {
      printJava("    if (n instanceof "+type+") {\n");
      printJava("      return null;\n");
      printJava("    }\n");  
    }
    printJava("    if (n == null) {\n");
    printJava("      return null;\n");
    printJava("    }\n");
  }

  /**
   * Finish generating translatetoFooNode()
   * @param rtype The return type of the method
   */
  private void generateTranslateEnding(final String rtype) {
    if (countUsage) {
      printJava("      bumpUsage(op);\n");
    }
    printJava("      ITranslator xlate = translators.get(op);\n"); 
    if ("IJavaOperatorNode".equals(rtype)) {
      printJava("      "+rtype+" rv = xlate==null ? null : xlate.translate(n);\n");
    } else {
      printJava("      "+rtype+" rv = ("+rtype+") (xlate==null ? null : xlate.translate(n));\n");
    }
    printJava("      return rv;\n");      
    printJava("    }\n");  
    printJava("    throw new IllegalArgumentException(\"Can't translate to "+rtype+": \"+op.name());\n");
    printJava("  }\n\n");
  }
  
  /**
   * Make a copy of the children, translating each one
   * @param c
   */
  protected final void translateNodeList(Child c) {
    translateNodeList(c, "this");
  }
  
  protected final void translateNodeList(Child c, String name) {
    final String type = makeInterfaceName(c.type);
    printJava("    List<"+type+"> "+c.name+" = new ArrayList<"+type+">();\n");
    printJava("    for (IRNode _n : JavaNode.tree.children("+name+")) {\n");
    printJava("       "+c.name+".add("+callTranslateNode("_n", type)+");\n");
    printJava("    }\n");
  }

  protected final String callTranslateNode(String name, String type) {
    //return name;
    return "NodeFactories.translateTo"+type+"("+name+")";
  }
  
  protected final String translateFixedChild(OpSyntax s, Child c) {
    return translateFixedChild(s, c, "this");
  }
  
  protected final String translateFixedChild(OpSyntax s, Child c, String name) {
    final String type = makeInterfaceName(c.type);
    return callTranslateNode(callGetChild(s,c,name), type);
  }
  
  protected final String translateFixedChildren(OpSyntax s, Child c, OpSyntax child) {
    return translateFixedChildren(s, c, child, "this");
  }
  
  protected final String translateFixedChildren(OpSyntax s, Child c, OpSyntax child, String name) {
    final String type = makeInterfaceName(c.type);
    printJava("      "+type+" _"+c.name+" = "+callTranslateNode(callGetChild(s,c,name), type)+";\n");
    if (couldBeNullVariant(c)) {
      final String eltType = makeInterfaceName(child.variableChild.type);
      return "_"+c.name+"==null ? Collections.<"+eltType+">emptyList() : _"+callGetFixedChildren(c, child);
    }
    return "_"+callGetFixedChildren(c, child);
  }
}
