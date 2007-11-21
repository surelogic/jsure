package com.surelogic.opgen;


import com.surelogic.opgen.syntax.*;

/**
 * Generates nodes that correspond to each Operator
 * 
 * @author chance
 */
public class OperatorProxyNodeGen extends AbstractOperatorProxyNodeGen {
  public static final boolean doesCaching = false;
  
  public OperatorProxyNodeGen(String astPrefix, String implPrefix, String suffix) {
    super(astPrefix, implPrefix, suffix);
  }
  public OperatorProxyNodeGen() {
    super(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
  }
  
  public static void main(String[] args) {
    OperatorProxyNodeGen m = new OperatorProxyNodeGen(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
 
  @Override
  protected void generateFields(final OpSyntax s) {
    if (doesCaching) {
      generateStandardFields(s, fieldDeclarationsStrategy);
    }  
    if (!isAbstract(s)) { 
      printJava("  public static final NodeFactories.ITranslator translator = new NodeFactories.ITranslator() {\n");
      printJava("    public IJavaOperatorNode translate(IRNode n) {\n");
      printJava("      return new "+makeNodeName(s.name)+"(n);\n");
      printJava("    }\n");
      printJava("  };\n");
      printJava("\n");
    }
  }
  
  @Override
  protected void generateConstructors(final OpSyntax s) {
    printJava("  public "+makeNodeName(s.name)+"(IRNode n) {\n");
    printJava("    super(n);\n");
    //printJava("    return "+callGetInfo(s.packageName, a)+";\n");
    //printJava("    boolean rv = "+callGetModifiers(arg)+";\n");  
    printJava("  }\n");
  }
  
  @Override
  protected void generateGetParentBody(OpSyntax s, String type) {
    super.generateGetParentBody(s, type);
    if (doesCaching) {
      if (type.equals(ROOT_IFACE)) {
        printJava("    if (parent != null) { return parent; }\n");
      } else {
        printJava("    if (parent != null) { return ("+type+") parent; }\n");
      }
    }
    printJava("    IRNode parent = JavaNode.tree.getParent(this);\n");
    
    if (substGrandparentForFixedChildren) {
      switch (getLogicalParentStatus(s.name)) {
      case ALL:
        printJava("    IRNode gparent = JavaNode.tree.getParent(parent);\n");
        printJava("    return "+callTranslateNode("gparent", type)+";\n");
        break;
      case NONE:
        printJava("    return "+callTranslateNode("parent", type)+";\n");
        break;
      case SOME: default:
        printJava("    Operator op = JavaNode.tree.getOperator(parent);\n");
        printJava("    IRNode next;\n");
        printJava("    if (op instanceof ILogicallyInvisible) {\n");
        printJava("      next = JavaNode.tree.getParent(parent);\n"); 
        printJava("    } else {\n"); 
        printJava("      next = parent;\n"); 
        printJava("    }\n"); 
        printJava("    return "+callTranslateNode("next", type)+";\n");
      }
    } else {
      printJava("    return "+callTranslateNode("parent", type)+";\n");
    }
    printJava("  }\n\n"); 
  }
  
  @Override
  protected void accessInfo_NoArgs(OpSyntax s, Attribute a) {
    if (doesCaching) {
      printJava("    return "+a.name);      
    } else {
      super.accessInfo_NoArgs(s, a);
    }
  }
  
  @Override
  protected void accessInfo_WithArgs(OpSyntax s, Attribute a, String type, String arg) {
    if (doesCaching) {  
      printJava("    return "+computeArgAttrName(arg));      
    } else {
      super.accessInfo_WithArgs(s, a, type, arg);
    }
  }
  
  private void returnIfCaching(Child c) {
    if (doesCaching) { 
      printJava("    if (this."+c.name+" != null) { return this."+c.name+"; }\n");
    }
  }
  
  @Override
  protected void accessVariableChild(OpSyntax s, Child c) {
    returnIfCaching(c);
    
    translateNodeList(c);
    if (doesCaching) { 
      printJava("    this."+c.name+" = "+c.name+";\n");
    }
    printJava("    return "+c.name);
  }
  
  @Override
  protected void accessFixedChildren(OpSyntax s, Child c, OpSyntax child) {
    returnIfCaching(c);
    
    final String xlation = translateFixedChildren(s, c, child);
    if (doesCaching) { 
      printJava("    this."+c.name+" = "+xlation+";\n");
      printJava("    return this."+c.name);
    } else {
      printJava("    return " + xlation);
    }
  }
  
  @Override
  protected void accessFixedChild(OpSyntax s, Child c) {
    returnIfCaching(c);
    
    final String xlation = translateFixedChild(s, c);
    if (doesCaching) { 
      printJava("    this."+c.name+" = "+xlation+";\n");
      printJava("    return this."+c.name);
    } else {
      printJava("    return " + xlation);
    }
  }
  
  @Override
  protected void generateJavaOperatorNode(String pkg, String name) {
    generatePkgDecl(pkg);
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n\n");
    printJava("import "+makePackageName(astPrefix, STD_SUFFIX)+".*;\n\n");

    if (doesCaching) {
      generateClassJavadoc("Parent of all proxy AST nodes",
                           "Does lazy translation to proxy nodes, caching them inside the nodes",
                           "Effectively makes a copy of the structure of the AST, so it's UNSAFE if the underlying AST changes");
      printJava("public abstract class "+name+" extends ProxyNode implements IJavaOperatorNode {\n");
      printJava("  protected "+name+" parent;\n");
      printJava("\n");
    } else {
      printJava("import edu.cmu.cs.fluid.parse.*;\n\n");
      generateClassJavadoc("Parent of all proxy AST nodes",
                           "Does lazy translation to proxy nodes");
      printJava("public abstract class "+name+" extends JJNodeCachingProxy implements IJavaOperatorNode {\n");
    }
    generateJavaOperatorBody(name);
    if (simplifyGetParent) {
      printJava("  public IJavaOperatorNode getParent() {\n");
    } else {
      printJava("  public IJavaOperatorNode getParent(IJavaOperatorNode here) {\n");
    }
    if (doesCaching) {
      printJava("    if (parent != null) { return parent; }\n");
    }
    if (!simplifyGetParent) {
      printJava("    if (this != here) { throw new IllegalArgumentException(\"Nodes don't match\"); }\n");
    } 
    printJava("    IRNode parent = JavaNode.tree.getParent(this);\n");
    printJava("    return "+callTranslateNode("parent", "IJavaOperatorNode")+";\n");
    printJava("  }\n\n");    

    generateUnparseSupport();
    printJava("}\n");
  }
}
