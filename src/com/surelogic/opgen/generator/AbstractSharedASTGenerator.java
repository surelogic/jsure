package com.surelogic.opgen.generator;

import java.util.*;
import java.util.regex.Matcher;

import com.surelogic.opgen.syntax.Attribute;
import com.surelogic.opgen.syntax.Child;
import com.surelogic.opgen.syntax.KnownProperty;
import com.surelogic.opgen.syntax.OpSyntax;


/**
 * Base class of all generators that implement the interfaces created
 * by InterfaceGen
 * 
 * @author chance
 *
 */
public abstract class AbstractSharedASTGenerator extends AbstractASTGenerator {
  public static final String AST_PREFIX = "com.surelogic.ast";

  { 
    if (translateToAssignOperator) {
      typeTable.put("Op", "com.surelogic.ast.AssignOperator");
    }
  }
  
  public AbstractSharedASTGenerator(String implPrefix, String suffix) {
    super(implPrefix, suffix);
  }

  @Override
  protected boolean okToGenerate(OpSyntax s) {
    return okToGenerateInterface(s);
  }

  protected List<String> makeJavadoc(OpSyntax s) {
    List<String> l = new ArrayList<String>();
    
    Matcher m = javadocMatch.matcher(s.beforeText);
    if (m.find()) {
      StringTokenizer st = new StringTokenizer(m.group(), "\n");
      while (st.hasMoreTokens()) {
        String line = st.nextToken().trim();
        if (line.startsWith("* ")) {
          l.add(line.substring(2));
        }
        else if (line.startsWith("/** ")) {
          l.add(line.substring(4));
        }
        else if (line.startsWith("/* ")) {
          l.add(line.substring(3));
        }
        else {
          //System.err.println("Ignoring '"+line+"'");
        }
      }
      if (!l.isEmpty()) {
        l.add("");
      }
    }    
    return l;
  }
  
  protected void generateJavadoc(OpSyntax s) { 
    List<String> lines = makeJavadoc(s);
    if (!lines.isEmpty()) {
      generateClassJavadoc(lines.toArray(noStrings));
    }
  }
  
  protected final String makeEnumConstant(String name) {
    name = name.replace("Declaration", "Decl");
    name = name.replace("Expression", "Expr");
    name = name.replace("Statement", "Stmt");
    name = name.replace("Specification", "Spec");
    name = name.replace("Element", "Elt");
  
    // convert to caps with underscores
    final StringBuilder sb = new StringBuilder();
    final int len = name.length();
    for (int i=0; i<len; i++) {
      char c = name.charAt(i);
      if (i > 0 && Character.isUpperCase(c)) {
        sb.append('_');
      }
      sb.append(Character.toUpperCase(c));
    }
    return sb.toString();
  }
  
  protected void generateGetNodeType(OpSyntax s) {
    PkgStatus p = pkgMappings.get(s.packageName);
    if (s.props.get(KnownProperty.EXTENDABLE) != null) {
      // Extendable, so the type needs to be more generic
      printJava("  public INodeType getNodeType() {\n");
    } else {
      if (!s.isRoot && hasAbstractParents(s)) {
        printJava("  @Override\n");
      }
      printJava("  public "+p.getName()+"NodeType getNodeType() {\n");
    }
    printJava("    return "+p.getName()+"NodeType."+makeEnumConstant(s.name)+";\n");
    printJava("  }\n\n");
  }
  
  /*********************************************************************
   *  generateMethodsSelectively
   *********************************************************************/
  
  protected void generateMethodsSelectively(final OpSyntax s) {
    generateUnparse(s);
    generateGetNodeType(s);
    generateResolveBinding(s);
    if (!noGetParent) {
      generateGetParent(s);
    }
    generateGetOverriddenMethod(s);
    generateBridge(s);
  }

  protected void generateUnparseSupport() {
    printJava("  public final String unparse(boolean debug) {\n");
    printJava("    return unparse(debug, 0);\n");
    printJava("  }\n\n");
    
    printJava("  protected final void indent(StringBuilder sb, int indent) {\n");
    printJava("    for(int i=0; i<indent; i++) { sb.append(' '); }\n");
    printJava("  }\n\n");
  }
  
  protected void generateUnparse(final OpSyntax s) {
    if (isAbstract(s)) { 
      return; // No need to unparse
    }
    
    printJava("  public String unparse(boolean debug, int indent) {\n");
    printJava("    StringBuilder sb = new StringBuilder();\n");
    printJava("    indent(sb, indent);\n");
    printJava("    sb.append(\""+s.name+"\\n\");\n");
    s.generateFromSyntax(typeTable, new ASTSyntaxStrategy(false) {
      @Override
      protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
        printJava("    indent(sb, indent+2);\n");
        printJava("    sb.append(\""+a.name+"=\").append("+getSigForInfo_NoArgs(a)+");\n");
        printJava("    sb.append(\"\\n\");\n");
      }
      @Override
      protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
        final String argName = computeArgAttrName(arg);
        printJava("    indent(sb, indent+2);\n");
        printJava("    sb.append(\""+argName+"=\").append("+getSigForInfo_WithArgs(arg)+");\n");
        printJava("    sb.append(\"\\n\");\n");
      }
      @Override
      protected void doForFixedChild(OpSyntax s, int i, Child c) {
        printJava("    sb.append("+getSigForFixedChild(c)+".unparse(debug, indent+2));\n");
      }
      @Override
      protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
        doForVariableChild(s, i, c);
      }
      @Override
      protected void doForVariableChild(OpSyntax s, int i, Child c) {
        printJava("    for(IJavaOperatorNode _n : "+getSigForVariableChild(c)+") {\n"); 
        printJava("      sb.append(_n.unparse(debug, indent+2));\n");
        printJava("    }\n");
      }      
    });
    printJava("    return sb.toString();\n");
    printJava("  }\n\n");
  }
  
  @Override
  protected Set<String> getPackagesUsed(OpSyntax s) {
    Set<String> ss = super.getPackagesUsed(s);
    if (getBindsToName(s) != null || getBindsToTypeName(s) != null ||
        implementsBinding(s) ||        
        getBridgesToName(s) != null) {   
      ss.add("com.surelogic.ast");
    }
    return ss;
  }
}
