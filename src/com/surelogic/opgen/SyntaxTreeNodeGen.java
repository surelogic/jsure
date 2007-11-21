package com.surelogic.opgen;

/**
 * Generates SyntaxTreeNodes that correspond to each Operator
 * 
 * @author chance
 */
public class SyntaxTreeNodeGen extends OperatorNodeGen {
  public static final String STD_PREFIX = "com.surelogic.syntaxNode";

  public SyntaxTreeNodeGen(String astPrefix, String implPrefix, String suffix) {
    super(astPrefix, implPrefix, suffix);
  }
  public SyntaxTreeNodeGen() {
    super(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
  }
  
  public static void main(String[] args) {
    SyntaxTreeNodeGen m = new SyntaxTreeNodeGen(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
  
  @Override
  protected void generateJavaOperatorNode(String pkg, String name) {
    generatePkgDecl(pkg);
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n");
    printJava("import com.surelogic.tree.*;\n");
    printJava("import "+makePackageName(astPrefix, STD_SUFFIX)+".*;\n");
    printJava("\n");
    printJava("public class "+name+" extends SyntaxTreeNode {\n");
    printJava("  public "+name+"(Operator operator, "+nodeType+"[] children) {\n");
    printJava("    super(operator, children);\n");
    printJava("  }\n");
    //printJava("\n");
    //printJava("  public abstract Operator getOperator();\n");
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
}
