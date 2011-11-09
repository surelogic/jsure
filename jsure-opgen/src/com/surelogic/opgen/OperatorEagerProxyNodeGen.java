package com.surelogic.opgen;

import com.surelogic.opgen.syntax.Attribute;
import com.surelogic.opgen.syntax.Child;
import com.surelogic.opgen.syntax.OpSyntax;

public class OperatorEagerProxyNodeGen extends AbstractOperatorProxyNodeGen {
  public OperatorEagerProxyNodeGen(String astPrefix, String implPrefix, String suffix) {
    super(astPrefix, implPrefix, suffix);
  }
  
  public static void main(String[] args) {
    OperatorEagerProxyNodeGen m = new OperatorEagerProxyNodeGen(AST_PREFIX, STD_PREFIX, STD_SUFFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
  
  @Override
  protected boolean skipChildrenForImports() {
    return false;
  }
  @Override
  protected boolean parentOperatorHandled() {
    return true;
  }
  
  @Override
  protected void generateFields(OpSyntax s) {
    generateStandardFields(s);
    
    if (!isAbstract(s)) {
      OpSyntax syn;
      if (s.isConcrete) {
        syn = s;
      } else {
        syn = lookup(s.parentOperator); 
        if (syn == null) {
          syn = s;
        }
      }
      
      generateClassBodyDeclJavadoc("Creates proxy nodes for all its children");
      printJava("  public static final NodeFactories.ITranslator translator = new NodeFactories.ITranslator() {\n");
      printJava("    public IJavaOperatorNode translate(IRNode n) {\n");
      generateTranslateBody(s, syn);
      printJava("    }\n");
      printJava("  };\n");
      printJava("\n");
    }
  }

  @Override
  protected boolean computeValueFromField() {
    return true;
  }
  
  protected final void generateTranslateBody(OpSyntax s, OpSyntax syn) {
    if (convertOptNodes && isNullVariant(s.name)) {
      printJava("      return null;\n");
    } else {
      // Initialize fields      
      syn.generateFromSyntax(typeTable, new VariableDeclarationsStrategy(alwaysFalse) {
        @Override
        protected void doBefore() {
          printJava("      ");
        }
        @Override
        protected void doAfter() {
          printJava(" = ");
        }  
        
        @Override
        protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
          super.doForInfo_NoArgs(s, i, a, type);
          printJava(callGetInfo(s.packageName, a, "n")+";\n");
        }
        @Override
        protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
          super.doForInfo_WithArgs(s, i, a, type, arg);
          printJava(callGetModifiers("n", arg)+";\n");
        }
        @Override
        protected void doForVariableChild(OpSyntax s, int i, Child c) {
          // super.doForVariableChild(s, i, c);
          translateNodeList(c, "n");
        }
        @Override
        protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
          final String xlation = translateFixedChildren(s, c, child, "n");
          super.doForFixedChildren(s, i, c, child);
          printJava(xlation+";\n");
        }
        @Override
        protected void doForFixedChild(OpSyntax s, int i, Child c) {
          final String xlation = translateFixedChild(s, c, "n");
          super.doForFixedChild(s, i, c);
          printJava(xlation+";\n");
        }      
      });       
      
      // Call constructor
      final VariableNamesStrategy names = new VariableNamesStrategy(false, "n");
      syn.generateFromSyntax(typeTable, names); 
      generateCall(names, "      IJavaOperatorNode rv = new "+makeNodeName(s.name)+"(");
      if (implementsBinding(s)) {
        printJava("      NodeFactories.addBinding(n, (IBinding) rv);\n"); 
      }
      printJava("      return rv;\n");
    }
  }
  @Override
  protected void generateConstructors(OpSyntax s) {
    generateStandardConstructor(s, ROOT_TYPE, "IRNode n");
  }  
  @Override
  protected String createNoFooNode(OpSyntax s, Child c) {
    if (convertOptNodes) {
      return "null";
    }
    return "new "+makeNoFooNode(c.type)+"("+callGetChild(s,c,"n")+")";
  }
  @Override
  protected void accessInfo_NoArgs(OpSyntax s, Attribute a) {
    printJava("    return "+a.name);
  }
  @Override
  protected void accessInfo_WithArgs(OpSyntax s, Attribute a, String type, String arg) {
    printJava("    return "+computeArgAttrName(arg));
  }
  @Override
  protected void accessVariableChild(OpSyntax s, Child c) {
    printJava("    return "+c.name);
  }
  @Override
  protected void accessFixedChildren(OpSyntax s, Child c, OpSyntax child) {
    printJava("    return "+c.name);
  }
  @Override
  protected void accessFixedChild(OpSyntax s, Child c) {
    printJava("    return "+c.name);
  }
  @Override
  protected void generateGetParentBody(OpSyntax s, String type) {
    super.generateGetParentBody(s, type);
    printJava("    return ("+type+") parent;\n");
    printJava("  }\n");
  }
  @Override
  protected void generateJavaOperatorNode(String pkg, String name) {
    generatePkgDecl(pkg);
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n\n");
    printJava("import "+makePackageName(astPrefix, STD_SUFFIX)+".*;\n\n");
    printJava("public abstract class "+name+" extends ProxyNode implements IJavaOperatorNode {\n");
    printJava("  protected "+ROOT_TYPE+" parent;\n\n");

    generateJavaOperatorBody(name);
    
    if (simplifyGetParent) {
      printJava("  public "+ROOT_IFACE+" getParent() {\n");
    } else {
      printJava("  public "+ROOT_IFACE+" getParent("+ROOT_IFACE+" here) {\n");
      printJava("    if (this != here) { throw new IllegalArgumentException(\"Nodes don't match\"); }\n");
    }
    printJava("    return parent;\n");
    printJava("  }\n");
    printJava("\n"); 
    generateClassBodyDeclJavadoc("Only to be called by the parent AST node's constructor");
    printJava("  public void setParent("+ROOT_TYPE+" p) {\n");
    printJava("    parent = p;\n");
    printJava("  }\n\n");
    
    generateUnparseSupport();
    printJava("}\n");
  }
}
