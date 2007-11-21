package com.surelogic.opgen;

import java.io.File;
import java.util.*;

import com.surelogic.opgen.generator.*;
import com.surelogic.opgen.syntax.*;


public class CrystalGen extends AbstractASTImplGenerator {
  public static final String STD_PREFIX = "edu.cmu.cs.crystal";
  static public final String ROOT_TYPE = "JavaOperatorNode";
  
  public CrystalGen(String astPrefix, String implPrefix) {
    super(astPrefix, implPrefix);
  }
  public CrystalGen() {
    super(AST_PREFIX, STD_PREFIX);
  }
  
  public static void main(String[] args) {
    CrystalGen m = new CrystalGen(AST_PREFIX, STD_PREFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
  
  @Override
  protected String makeFilename(OpSyntax s) {
    return makeNodeName(s.name)+".java";
  }
  
  @Override
  protected String makeNodeName(String name) {  
    return name+"Node";
  }
  
  @Override
  protected Set<String> getPackagesUsed(OpSyntax s) {
    Set<String> ss = super.getPackagesUsed(s);
    ss.add("com.surelogic.parse");
    ss.add("edu.cmu.cs.fluid.java");
    ss.add("java.util");
    return ss;
  }    
  
  @Override
  protected void generateIntro(OpSyntax s) {
    /*
    if (hasVariableChildren(s, false)) {
      printJava("import java.util.*;\n\n");
    }
    */
    for (String pkg : getPackagesUsed(s)) {
      printJava("import "+pkg+".*;\n");
    }
    printJava("\n");
    printJava(s.modifiers);
    if (isAbstract(s)) { 
      printJava("abstract class ");
    } else {
      printJava("class "); // CHANGED
    }
    printJava(s.name);
    if (s.isRoot) {
      printJava("Node extends "+ROOT_TYPE);
    } else {
      printJava("Node extends ");
      printJava(s.parentOperator+"Node");
    }
    printJava(" implements I"+s.name+"Node ");
    printJava(" { \n");
  }
  
  @Override
  protected void generateFields(OpSyntax s) {
    generateStandardFields(s);
    generateFactory(s);    
  }
  
  private void generateFactory(OpSyntax s) {
    if (isAbstract(s)) { 
      return; // No factory
    }
    printJava("  public static final AbstractSingleNodeFactory factory =\n");
    printJava("    new AbstractSingleNodeFactory(\""+s.name+"\") {\n");
    printJava("      @SuppressWarnings(\"unchecked\")\n");
    printJava("      public IJavaOperatorNode create(String _token, int _start, int _stop,\n");
    printJava("                                      int _mods, String _id, int _dims, List<IJavaOperatorNode> _kids) {\n");  
    s.generateFromSyntax(typeTable, new VariableDeclarationsStrategy(false) {
      @Override
      protected void doBefore() {
        printJava("        ");
      } 
      @Override
      protected void doAfter() {
        printJava(" = ");
      }  
      @Override
      protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
        super.doForInfo_NoArgs(s, i, a, type);
        if (a.type.equals("DimInfo")) {
          printJava("_dims;\n");
        } else if (a.type.equals("Modifiers")) {
          printJava("_mods;\n");
        } else if (type.equals("String")){
          printJava("_id;\n");
        } else if (type.equals("boolean")) {
          printJava("false;\n");
        } else {
          printJava("null;\n");
        }
      }
      @Override
      protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
        super.doForInfo_WithArgs(s, i, a, type, arg);
        printJava("JavaNode.getModifier(_mods, JavaNode."+arg.toUpperCase()+");\n");
      }
      @Override
      protected void doForFixedChild(OpSyntax s, int i, Child c) {
        super.doForFixedChild(s, i, c);
        printJava(" (");
        variableTypesStrategy.doForFixedChild(s, i, c);
        printJava(") _kids.get("+c.childNum+");\n");
      }
      @Override
      protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
        super.doForFixedChildren(s, i, c, child);
        printJava(" ((TempListNode) _kids.get("+c.childNum+")).toList();\n");
        // FIX to convert the node into its children
      }
      @Override
      protected void doForVariableChild(OpSyntax s, int i, Child c) {
        super.doForVariableChild(s, i, c);
        //printJava(" (");
        //variableTypesStrategy.doForVariableChild(s, i, c);
        //printJava(") ((List) _kids);\n");
        printJava("(List) _kids;\n");
        // FIX to check that the lists match
      }  
    });
    printJava("        return new "+makeNodeName(s.name)+" (_start");
    s.generateFromSyntax(typeTable, variableNamesStrategy);
    for(String arg : variableNamesStrategy) {
      printJava(",\n          "+arg);
    }
    printJava("        );\n");
    printJava("      }\n");
    printJava("    };\n");
    printJava("\n");
  }
  
  private void generateConstructors(OpSyntax s) {
    generateStandardConstructor(s, ROOT_TYPE, "int offset");
  }
  
  @Override
  protected boolean parentOperatorHandled() {
    return true;
  }
  
  @Override
  protected boolean computeValueFromField() {
    return true;
  }
  
  @Override
  protected void generateMethods(OpSyntax s) {
    generateConstructors(s);
    generateMethodsSelectively(s);    
    generateStandardAccessors(s);    
    generateStandardDelegateMethods(s);
    if (!isAbstract(s)) {
      createAcceptImpl(s, hasAbstractParents(s));
    } 
  }
  
  @Override
  protected void generateGetParentBody(OpSyntax s, String type) {
    super.generateGetParentBody(s, type);
    generateStandardGetParentBody(s, type);
  }
    
  @Override
  protected boolean elidesParents() {
    return false;
  }
  
  @Override
  protected void generateForAll() {
    String outPath = (outDir == null) ? "" : outDir + File.separator;
    String pkg     = pkgPrefix+STD_SUFFIX;
    String stdPath = computePath(outPath, pkg);
    
    if (!ROOT_TYPE.equals("JavaNode")) {
      openPrintStream(stdPath + File.separator + ROOT_TYPE+".java");
      generateJavaOperatorNode(pkg);
    }
    
    for(PkgStatus s : getTags()) {
      openPrintStream(stdPath + File.separator + s.getName()+"NodeFactories.java");
      generateNodeFactories(pkg, s);
    }
  }
  
  private void generateJavaOperatorNode(String pkg) {
    generatePkgDecl(pkg);
    printJava("import "+makePackageName(astPrefix, STD_SUFFIX)+".*;\n\n");
    printJava("public abstract class "+ROOT_TYPE+" implements I"+ROOT_TYPE+" {\n");
    printJava("  protected final int offset;\n");
    printJava("  protected "+ROOT_TYPE+" parent;\n\n");
    
    printJava("  protected "+ROOT_TYPE+"(int offset) {\n");
    printJava("    this.offset = offset;\n");
    printJava("  }\n\n");
    
    if (simplifyGetParent) {
      printJava("  public I"+ROOT_TYPE+" getParent() {\n");
    } else {
      printJava("  public I"+ROOT_TYPE+" getParent(I"+ROOT_TYPE+" here) {\n");
      printJava("    if (this != here) { throw new IllegalArgumentException(\"Nodes don't match\"); }\n");
    }
    printJava("    return parent;\n");
    printJava("  }\n");
    printJava("\n");
    generateClassBodyDeclJavadoc("Only to be called by the parent AST node's constructor");
    printJava("  public void setParent("+ROOT_TYPE+" p) {\n");
    printJava("    parent = p;\n");
    printJava("  }\n\n");
    
    printJava("  public int getOffset() {\n");
    printJava("    return offset;\n");
    printJava("  }\n\n");    

    generateUnparseSupport();
    printJava("}\n");
  }
  
  protected final void generateNodeFactories(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg);
    printJava("import java.util.*;\n");
    printJava("import com.surelogic.parse.*;\n");
    for (String p : packagesAppearing()) {
      printJava("import "+STD_PREFIX+"."+p+".*;\n");
    }
    printJava("\n");
    if (ps.getRoot() == null) {
      printJava("public class "+ps.getName()+"NodeFactories {\n");
    } else {
      printJava("public class "+ps.getName()+"NodeFactories extends "+
                ps.getRoot()+"NodeFactories {\n");
    }
    printJava("  public static IASTFactory init(IASTFactory f) {\n");
    if (ps.getRoot() != null) {
      printJava("    "+ps.getRoot()+"NodeFactories.init(f);\n");
    }
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      if (!isAbstract(s)) { 
        printJava("    "+s.name+"Node.factory.register(f);\n");
      }
    }      
    printJava("    return f;\n");
    printJava("  }\n");

    printJava("}\n");
  }  
}
