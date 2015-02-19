package com.surelogic.opgen;

import java.util.Map;

import com.surelogic.opgen.syntax.*;


/**
 * Generates nodes that correspond to each Operator
 * 
 * @author chance
 */
public class OperatorNodeGen extends AbstractOperatorNodeGen {
  public static final String STD_PREFIX = "com.surelogic.node";
  public OperatorNodeGen(String astPrefix, String implPrefix, String suffix) {
    super(astPrefix, implPrefix, suffix);
  }
  public OperatorNodeGen() {
    super(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
  }
  
  public static void main(String[] args) {
    OperatorNodeGen m = new OperatorNodeGen(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
 
  @Override
  protected final String makeNodeName(String name) {
    return name+"Node";
  }
  
  @Override
  protected final void generateFields(final OpSyntax s) {
    if (!isAbstract(s)) { 
      printJava("  public static final NodeFactories.IFactory factory = new NodeFactories.IFactory() {\n");
      printJava("    public JavaNode make() {\n");
      printJava("      return new "+s.name+"Node("+s.name+".prototype, noNodes);\n");
      printJava("    }\n");
      printJava("    public JavaNode make("+nodeType+"[] children) {\n");
      printJava("      return new "+s.name+"Node("+s.name+".prototype, children);\n");
      printJava("    }\n");
      printJava("  };\n");
      printJava("\n");
    }
  }
  
  @Override
  protected final void generateConstructors(final OpSyntax s) {
    // printJava("  public "+s.name+"Node(Operator operator, "+ROOT_TYPE+"[] children) {\n");
    printJava("  public "+makeNodeName(s.name)+"(Operator operator, "+nodeType+"[] children) {\n");
    printJava("    super(operator, children);\n");
    printJava("  }\n");
  }
  
  @Override
  protected void generateGetParentBody(OpSyntax s, String type) {
    super.generateGetParentBody(s, type);
    if (substGrandparentForFixedChildren) {
      switch (getLogicalParentStatus(s.name)) {
      case ALL:
        printJava("    IRNode parent = tree.getParent(this);\n");
        printJava("    return ("+type+") tree.getParent(parent);\n");
        break;
      case NONE:
        printJava("    return ("+type+") tree.getParent(this);\n");
        break;
      case SOME: default:
        printJava("    "+ROOT_TYPE+" parent = ("+ROOT_TYPE+") tree.getParent(this);\n");
        printJava("    Operator op = parent.getOperator();\n");
        printJava("    if (op instanceof ILogicallyInvisible) {\n");
        printJava("      return ("+type+") tree.getParent(parent);\n");
        printJava("    } else {\n"); 
        printJava("      return ("+type+") parent;\n");
        printJava("    }\n"); 
      }
    } else {
      printJava("    return ("+type+") tree.getParent(this);\n");
    }
    printJava("  }\n\n"); 
  }
  
  @Override
  protected final void accessVariableChild(OpSyntax s, Child c) {
    printJava("    List "+c.name+" = tree.childList(this);\n");
    printJava("    return (List<"+makeInterfaceName(c.type)+">) "+c.name);
  }
  
  @Override
  protected final void accessFixedChildren(OpSyntax s, Child c, OpSyntax child) {
    final String type = makeNodeName(c.type);
    printJava("    "+type+" "+c.name+" = ("+type+") "+callGetChild(s,c)+";\n");
    printJava("    return " + callGetFixedChildren(c, child));
  }
  
  @Override
  protected final void accessFixedChild(OpSyntax s, Child c) {
    printJava("    return (" + makeConvertedInterfaceName(c.type) + ") " + callGetChild(s,c));
  }
  
  @Override
  protected void generateJavaOperatorNode(String pkg, String name) {
    generatePkgDecl(pkg);
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n");
    printJava("import "+makePackageName(astPrefix, STD_SUFFIX)+".*;\n");
    printJava("\n");
    printJava("public abstract class "+name+" extends JavaNode {\n");
    printJava("  protected "+name+"(Operator operator, "+nodeType+"[] children) {\n");
    printJava("    super(tree, operator, children);\n");
    printJava("  }\n");
    printJava("  public abstract Operator getOperator();\n");
    printJava("\n");
    if (simplifyGetParent) {
      printJava("  public I"+ROOT_TYPE+" getParent() {\n");
    } else {
      printJava("  public I"+ROOT_TYPE+" getParent(I"+ROOT_TYPE+" here) {\n");
      printJava("    if (this != here) { throw new IllegalArgumentException(\"Nodes don't match\"); }\n");
    }    
    printJava("    return (I"+ROOT_TYPE+") tree.getParent(this);\n");
    printJava("  }\n");
    printJava("}\n");
  }
  
  @Override
  protected final void generateNodeFactories(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg);
    printJava("import java.util.*;\n");
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n");
    for (String p : packagesAppearing()) {
      printJava("import "+OperatorGen.STD_PKG_PREFIX+"."+p+".*;\n");
      printJava("import "+pkgPrefix+p+".*;\n");
    }
    printJava("\n");
    if (ps.getRoot() == null) {
      printJava("public class "+ps.getName()+"NodeFactories {\n");
      printJava("  public interface IFactory {\n");
      printJava("    public JavaNode make();\n");
      printJava("    public JavaNode make(IRNode[] children);\n");
      printJava("    final JavaNode[] noNodes = new JavaNode[0];\n");
      printJava("  }\n");
      printJava("\n");
      printJava("  protected static final Map<Operator, IFactory> factoryMap = new HashMap<Operator, IFactory>();\n");      
      printJava("\n");
      printJava("  public static IFactory get(Operator op) {\n");
      printJava("    return factoryMap.get(op);\n");
      printJava("  }\n");
      printJava("}\n");
    } else {
      printJava("public class "+ps.getName()+"NodeFactories extends "+
                ps.getRoot()+"NodeFactories {\n");
    }
    printJava("  static {\n");
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      if (!isAbstract(s)) { 
        printJava("    factoryMap.put("+s.name+".prototype, "+s.name+"Node.factory);\n");
      }
    }
    printJava("  }\n");
  }
}
