package com.surelogic.opgen;

import java.io.File;
import java.util.*;

import com.surelogic.opgen.generator.*;
import com.surelogic.opgen.syntax.*;


/**
 * Generates nodes that correspond to each Operator
 * 
 * @author chance
 */
public abstract class AbstractOperatorNodeGen extends AbstractASTImplGenerator {
  protected static final String nodeType = "IRNode";
  protected final String ROOT_TYPE = makeNodeName("JavaOperator");
  protected final String ROOT_IFACE = makeInterfaceName("JavaOperator");

  public AbstractOperatorNodeGen(String astPrefix, String implPrefix, String suffix) {
    super(astPrefix, implPrefix, suffix);
  }
 
  @Override
  protected final String makeFilename(OpSyntax s) {
    return makeNodeName(s.name)+".java";
  }
  
  @Override
  protected abstract String makeNodeName(String name);
  
  @Override
  protected final Set<String> getPackagesUsed(OpSyntax s) {
    Set<String> ss = super.getPackagesUsed(s);
    ss.add(makePackageName(OperatorGen.STD_PKG_PREFIX+".", s.packageName));
    if (getLogicalParentStatus(s.name) == ParentSubstitutionType.SOME) {
      ss.add(makePackageName(OperatorGen.STD_PKG_PREFIX+".", STD_SUFFIX));
    }
//    switch (getLogicalParentStatus(s.name)) {
//    case SOME:
//      ss.add(makePackageName(OperatorGen.STD_PREFIX+".", STD_SUFFIX));
//    }
    return ss;
  }
  
  protected boolean skipChildrenForImports() {
    return true;
  }
  
  @Override
  protected final void generateIntro(OpSyntax s) {
    if (hasVariableChildren(s, skipChildrenForImports()) || isMethodDeclaration(s)) {
      printJava("import java.util.*;\n\n");
    }
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n");

    for (String pkg : getPackagesUsed(s)) {
      printJava("import "+pkg+".*;\n");
    }
    printJava("\n");
    //printJava("@SuppressWarnings(\"all\")\n");
    printJava(s.modifiers);    
    if (isAbstract(s)) { 
      printJava("abstract class ");
    } else {
      printJava("class ");
    }
    printJava(makeNodeName(s.name));
    if (s.isRoot) {
      printJava(" extends "+ROOT_TYPE+" implements ");
    } else {
      printJava(" extends ");
      printJava(makeNodeName(s.parentOperator)+" implements ");
    }
    printJava(makeInterfaceName(s.name));    
    printJava(" { \n");
  }
  
  protected abstract void generateConstructors(final OpSyntax s);
  
  static final String ending = ";\n  }\n";
  
  @Override
  protected final void generateMethods(final OpSyntax s) {
    generateConstructors(s);
    
    printJava("  @Override\n");
    printJava("  public Operator getOperator() {\n");
    printJava("    return "+s.name+".prototype;\n");
    printJava("  }\n\n");
    
    generateMethodsSelectively(s);

    s.generateFromSyntax(typeTable, new MethodSignatureStrategy(true, false) {      
      @Override
      protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
        super.doForInfo_NoArgs(s, i, a, type);
        accessInfo_NoArgs(s, a);
        printJava(ending);
      }
      @Override
      protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
        super.doForInfo_WithArgs(s, i, a, type, arg);
        accessInfo_WithArgs(s, a, type, arg);
        printJava(ending);
      }
      @Override
      protected void doForVariableChild(OpSyntax s, int i, Child c) {
        printJava("  @SuppressWarnings(\"unchecked\")");
        super.doForVariableChild(s, i, c);
        accessVariableChild(s, c);
        printJava(ending);
      }
      @Override
      protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
        super.doForFixedChildren(s, i, c, child);
        accessFixedChildren(s, c, child);
        printJava(ending);
      }
      @Override
      protected void doForFixedChild(OpSyntax s, int i, Child c) {
        super.doForFixedChild(s, i, c);
        accessFixedChild(s, c);
        printJava(ending);
      }      
    });
    
    generateStandardDelegateMethods(s);
    
    if (!isAbstract(s)) {
      createAcceptImpl(s, hasAbstractParents(s));
    } 
  }

  protected final String callGetInfo(String pkg, Attribute a) {
    return callGetInfo(pkg, a, "this");
  }
  protected final String callGetInfo(String pkg, Attribute a, String name) {
    String prefix = JAVA_PROMISE.equals(pkg)? "JavaPromise.get" : "JavaNode.get";  
    if (translateToAssignOperator) {
      String translation = getTranslation(a.type);
      if (translation != null) {
        return translation+"("+prefix+capitalize(a.type)+"("+name+"))";
      }
    }
    return prefix+capitalize(a.type)+"("+name+")";
  }
  
  private String getTranslation(String type) {
    if ("Op".equals(type)) {
      return "com.surelogic.ast.fluid.SharedImpl.translateToAssignOp";
    }
    return null;
  }

  protected final String callGetModifiers(String arg) {
    return callGetModifiers("this", arg);
  }
  
  protected final String callGetModifiers(String name, String arg) {
    return "JavaNode.getModifier("+name+", JavaNode."+arg.toUpperCase()+")";
  }
  
  protected final String callGetChild(OpSyntax s, Child c) {
    return callGetChild(s, c, "this");
  }
  protected final String callGetChild(OpSyntax s, Child c, String name) {
    return "JavaNode.tree.getChild("+name+", "+s.name+"."+c.name+"Loc)";
  }
  
  protected final String callGetFixedChildren(Child c, OpSyntax child) {
    return c.name + "."+getSigForVariableChild(child.variableChild);
  }

  protected void accessInfo_NoArgs(OpSyntax s, Attribute a) {
    printJava("    return "+callGetInfo(s.packageName, a));
  }
  protected void accessInfo_WithArgs(OpSyntax s, Attribute a, String type, String arg) {
    printJava("    return "+callGetModifiers(arg));
  }
  protected abstract void accessVariableChild(OpSyntax s, Child c);
  protected abstract void accessFixedChildren(OpSyntax s, Child c, OpSyntax child);
  protected abstract void accessFixedChild(OpSyntax s, Child c);
  
  @Override
  protected final void generateForAll() {
    String outPath = (outDir == null) ? "" : outDir + File.separator;
    String pkg     = pkgPrefix+STD_SUFFIX;
    String stdPath = computePath(outPath, pkg);
    
    if (!ROOT_TYPE.equals("JavaNode")) {
      openPrintStream(stdPath + File.separator + ROOT_TYPE+".java");
      generateJavaOperatorNode(pkg, ROOT_TYPE);
    }

    for(PkgStatus s : getTags()) {
      openPrintStream(stdPath + File.separator + s.getName()+"NodeFactories.java");
      generateNodeFactories(pkg, s);
    }
  }
  
  protected abstract void generateJavaOperatorNode(String pkg, String name);
  
  protected abstract void generateNodeFactories(String pkg, PkgStatus s);
}
