package com.surelogic.opgen;

import java.io.File;
import java.util.*;

import com.surelogic.opgen.generator.*;
import com.surelogic.opgen.syntax.*;


public class InterfaceGen extends AbstractSharedASTGenerator {
  private static final String ROOT_TYPE = "IJavaOperatorNode";
  
  public InterfaceGen(String prefix, String suffix) {
    super(prefix, suffix);
  }
  public InterfaceGen() {
    super(AST_PREFIX, STD_SUFFIX);
  }
  
  public static void main(String[] args) {
    InterfaceGen m = new InterfaceGen(AST_PREFIX, STD_SUFFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
  
  @Override
  protected final String makeFilename(OpSyntax s) {
    return makeInterfaceName(s.name)+".java";
  }
  
  @Override
  protected String makeNodeName(String name) {  
    return makeInterfaceName(name);
  }
  
  @Override
  protected boolean javadocImplementationDetails() {
    return false;
  }
  
  /**
   * FIX need to refactor
   */
  private void generateJavadocFromSyntax(OpSyntax s, List<String> comments, UnfoldSyntaxStrategy uss) {
    final int total = s.syntax.size();
    String indent = "   ";
    int i = 0, keyword = 0;  
    
    for(int j=0; j<total; j++) {
      SyntaxElement element = s.syntax.get(j);
      int nextj = j+1;
      if (element instanceof OpenParen) {
        nextj = ((OpenParen) element).closeIndex+1;
      }
      int lastj = j-1;
      if (element instanceof CloseParen) {
        lastj = ((CloseParen) element).openIndex-1;
      }
      
      // determine whether the element is to be looped over:
      if (!(element instanceof CloseParen) && nextj < total) {        
        SyntaxElement nextElement = s.syntax.get(nextj);
        if (nextElement instanceof Variability) {  // $syntax[$nextj] =~ /^[+*]/) {
          Variability v = (Variability) nextElement;
          if (v.text.startsWith("*")) {     
            comments.add(indent+"Zero or more of:");
          } else {
            comments.add(indent+"One or more of:");
          }
          indent += "  ";       
        } else if (nextElement instanceof Option) {
          comments.add(indent+"Either:");
          indent += "  ";
        }
      }
      
      if (element instanceof Attribute) {
        uss.handleAttribute(s, indent, i, (Attribute) element);
      } else if (element instanceof Child) {
        if (uss.handleChild(s, indent, i, (Child) element)) {
          i += 1;
        }
      } else if (element instanceof Token) {     
        keyword += 1;
        uss.handleToken(s, indent, i, keyword, (Token) element);
      } else if (element instanceof Tag) {
        indent = uss.handleTag(s, indent, i, (Tag) element);
      } else if (plusOrStarDotMatch.matcher(element.text).matches()) {
        comments.add(indent.substring(2)+"Separated by:");
      } else if (element.text.equals("?/")) {
        comments.add(indent.substring(2)+"Or:");
      }
      
      // are we ending a loop or an if?
      SyntaxElement lastElement = (lastj >=0) ? s.syntax.get(lastj) : null;
      if ((element.text.equals("*") || element.text.equals("+")) ||
          (lastElement != null && !(element instanceof OpenParen) && plusOrStarDotMatch.matcher(lastElement.text).matches())) {
        indent = indent.substring(2);
        // end while
      } else if (element.text.equals("?") ||
          (lastElement != null && !(element instanceof OpenParen) && lastElement.text.equals("?/"))) {
        indent = indent.substring(2);
      } 
    }
  }
  
  /**
   * @return A non-null list of comments
   */
  private List<String> makeInterfaceJavadoc(OpSyntax s, final List<String> comments) {
    if (isAbstract(s)) {
      comments.add("This is an abstract node (i.e., never instantiated)\n");
    }
    if (s.isConcrete) {
      //final List<String> comments = new ArrayList<String>();
      comments.add("Syntax:");
      generateJavadocFromSyntax(s, comments, new UnfoldSyntaxStrategy() {
        @Override
        public void handleAttribute(OpSyntax s, String indent, int i, Attribute a) {
          comments.add(indent + a.name+" : "+a.type+" ("+getType(a.type)+")");
        }
        @Override
        public boolean handleChild(OpSyntax s, String indent, int i, Child c) {
          boolean rv = false;
          String type;
          
          if (s.variableChild != c) {
            // Not the variable child, so
            OpSyntax child = lookup(c.type);
            if (child != null && isLogicallyInvisible(child)) {
              rv   = true;
              if (child.variableChild == null) {
                throw new Error("Need to define children in "+child.name);
              }
              type = child.variableChild.type;
            } else {
              comments.add(indent + c.name+" : "+makeInterfaceName(c.type));
              return true;
            }
          } else {
            type = c.type;
          }
          if (convertOptNodes && couldBeNullVariant(c)) {
            comments.add(indent + c.name+"List : List<"+makeInterfaceName(type)+"> or null");
          } else {
            comments.add(indent + c.name+"List : List<"+makeInterfaceName(type)+">");
          }
          return rv;
        }
        @Override
        public void handleToken(OpSyntax s, String indent, int i, int keyword, Token t) {
          comments.add(indent + t.text);
        }
        @Override
        public String handleTag(OpSyntax s, String indent, int i, Tag t) { 
          comments.add(indent + t.text);
          return indent; 
        }
      });     
      comments.add("");
      for (Map.Entry<Property,String> e : s.props.entrySet()) {
        String msg = e.getKey().getMessage();
        String comment;
        if (msg == null) {
          comment = "Contains unknown property '"+e.getKey().getName()+"'";
        } else {
          comment = String.format(msg, e.getValue());
        }
        comments.add(comment);
      }
      return comments;
    }
    s = lookup(s.parentOperator);
    if (s != null) {
      return makeInterfaceJavadoc(s, comments);
    }
    return comments;
    //return Collections.emptyList();
  }
  
  /**
   * @return A non-null list of comments
   */
  @Override
  protected List<String> makeJavadoc(OpSyntax s) {
    return makeInterfaceJavadoc(s, super.makeJavadoc(s));
  }
  
  @Override
  protected Set<String> getPackagesUsed(OpSyntax s) {
    Set<String> ss = super.getPackagesUsed(s);
    // For getNodeType
    ss.add(makePackageName(AST_PREFIX, STD_SUFFIX));
    return ss;
  }
  
  @Override
  protected void generateIntro(OpSyntax s) {
    if (hasVariableChildren(s) || isMethodDeclaration(s)) {
      printJava("import java.util.List;\n\n");
    }
    for (String pkg : getPackagesUsed(s)) {
      printJava("import "+pkg+".*;\n");
    }
    printJava("\n");
    
    generateJavadoc(s);
    //printJava("@SuppressWarnings(\"all\")\n");
    printJava(s.modifiers);
    printJava("interface I");
    printJava(s.name);
    if (s.isRoot) {
      printJava("Node extends "+ROOT_TYPE);
    } else {
      printJava("Node extends I");
      printJava(s.parentOperator+"Node");
    }
    for (String superiface : s.superifaces) {
      OpSyntax op = lookupIface(superiface);
      if (op == null) {
        System.err.println("Error: Couldn't find interface "+superiface);
      } else 
      if (s != op) {
        printJava(", "+makeInterfaceName(op.name));
      }
    }
    generateImplements(s);
    printJava(" ");  
    /*
    if (s.isRoot) {
      // Splice IAcceptor in as the first interface implemented
      int len = "implements ".length();
      int split = len + s.afterOp.indexOf("implements ");
      printJava(s.afterOp.substring(0, split));
      printJava("IAcceptor, ");
      printJava(s.afterOp.substring(split));      
    } else {
      printJava(s.afterOp);
    }
    */
    printJava("{ \n");
  }
  
  private void generateImplements(OpSyntax s) {
    String binding  = s.props.get(KnownProperty.BINDING);
    String tBinding = getTypeBindingName(s);
    
    if (binding != null) {
      printJava(", ");
      printJava(makeBindingName(binding));
    }
    if (tBinding != null) {
      printJava(", ");
      printJava(makeTypeName(tBinding));
    }
    
    BindingType bt = getBindsToName(s);
    if (bt != null) {
      printJava(", ");
      printJava(bt.getHasBindingName());
    }
    BindingType tt = getBindsToTypeName(s);    
    if (tt != null) {
      printJava(", ");
      printJava(tt.getHasTypeName());
    }    
  }
  
  @Override
  protected void generateFields(final OpSyntax s) {
	// Nothing to do
  }
  
  @Override
  protected void generateMethods(final OpSyntax s) {
    generateMethodsSelectively(s);
    
    s.generateFromSyntax(typeTable, new MethodSignatureStrategy(true, true));
  }
  @Override
  protected void generateGetNodeType(OpSyntax s) {
    if (s.props.get(KnownProperty.EXTENDABLE) == null) {
      PkgStatus p = pkgMappings.get(s.packageName);
      printJava("  public "+p.getName()+"NodeType getNodeType();\n");
    }
  }
  @Override
  protected void generateUnparse(final OpSyntax s) {
	// Nothing to do
  }
  
  @Override
  protected boolean generateGetParent(OpSyntax s) {
    boolean created = false;
    for (String iface : s.superifaces) {
      OpSyntax op = lookupIface(iface);
      if (op != null && op != s) {
        created |= super.wouldGenerateGetParent(op);
      }
    }
    return generateGetParent(s, created);
  }
  
  private abstract class VisitorSpec {
    final String pkg;
    final String label;
    final String[] docs;
    
    VisitorSpec(String pkg, String label, String[] docs) {
      this.pkg   = pkg;
      this.label = label;
      this.docs  = docs;
    }
    /*
    VisitorSpec(String pkg, String name) {
      this(pkg, name, NO_DOCS);
    }
    */
    abstract void generateVisitMethodBody(OpSyntax s);

    void generateRestOfClassBody() {      
    	// Nothing to do
    }
    
    protected void generateDefaultConstructors(String name) {
      printJava("  protected final T defaultValue;\n\n");
      printJava("  public "+name+"(T defaultVal) {\n");
      printJava("    defaultValue = defaultVal;\n");
      printJava("  }\n\n"); 
      
      printJava("  public "+name+"() {\n");
      printJava("    this(null);\n");
      printJava("  }\n\n"); 
    }
    
    protected void generateDefaultSubclassConstructors(String name) {
      printJava("  public "+name+"(T defaultVal) {\n");
      printJava("    super(defaultVal);\n");
      printJava("  }\n\n"); 
      
      printJava("  public "+name+"() {\n");
      printJava("    super(null);\n");
      printJava("  }\n\n"); 
    }
    
    boolean skip(OpSyntax s) {
      return isAbstract(s);
    }
    String name(String tag) {
      System.out.println("Tag: "+tag);
      return label+tag+"Visitor";
    }
  }
  
  @Override
  protected void generateForAll() {
    String outPath = (outDir == null) ? "" : outDir + File.separator;
    String pkg     = pkgPrefix+STD_SUFFIX;
    String stdPath = computePath(outPath, pkg);
    
    openPrintStream(stdPath + File.separator + ROOT_TYPE+".java");
    generateIJavaNode(pkg);
    
    openPrintStream(stdPath + File.separator + "INodeVisitor.java");
    generateINodeVisitor(pkg, null);
    
    openPrintStream(stdPath + File.separator + "IJavaBinder.java");
    generateIJavaBinder(pkg, null);
    
    for(PkgStatus s : getTags()) {
      openPrintStream(stdPath + File.separator + "I"+s.getName()+"NodeVisitor.java");
      generateINodeVisitor(pkg, s);

      openPrintStream(stdPath + File.separator + "Test"+s.getName()+"NodeVisitor.java");
      generateTestNodeVisitor(pkg, s);
    
      for (VisitorSpec vs : getVisitors(pkg)) {
        openPrintStream(stdPath + File.separator + vs.name(s.getName()) + ".java");
        generateSomeVisitor(vs, s);
      }

      openPrintStream(stdPath + File.separator + "I"+s.getName()+"JavaBinder.java");
      generateIJavaBinder(pkg, s);
      
      openPrintStream(stdPath + File.separator + s.getName() + "NodeType.java");
      generateNodeType(pkg, s);
    }
    
    openPrintStream(stdPath + File.separator + "package-info.java");
    generatePackageInfo(pkg);
    
    openPrintStream(stdPath + File.separator + "INodeType.java");
    generateINodeType(pkg);
    
//    for (String val : bindings) {
//      openPrintStream(stdPath + File.separator + makeBindingName(val)+".java");
//      generateBinding(pkg, val);
//    }
    /*
    for (Map.Entry<String,String> e : childBindings.entrySet()) {
      String val = e.getKey();
      openPrintStream(stdPath + File.separator + makeBindingName(val)+".java");
      generateBinding(pkg, val, e.getValue());
    }
    */
    //makeTypeBindings(stdPath, pkg);
  }
 
  private void generateIJavaNode(String pkg) {
    generatePkgDecl(pkg);
    printJava("public interface "+ROOT_TYPE+" {\n");
    printJava("  public int getOffset();\n");
    printJava("  public INodeType getNodeType();\n");
    printJava("  public String unparse(boolean debug);\n");
    printJava("  public String unparse(boolean debug, int indent);\n");
    printJava("  public <T> T accept(INodeVisitor<T> visitor);\n");
    if (simplifyGetParent) {
      printJava("  public "+ROOT_TYPE+" getParent();\n");
    } else {
      printJava("  public "+ROOT_TYPE+" getParent("+ROOT_TYPE+" here);\n");
    }
    printJava("}\n");
  }
  
  private void generateINodeVisitor(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg);
    for (String p : packagesAppearing()) {
      printJava("import "+pkgPrefix+p+".*;\n");
    }
	  printJava("\n");
    printJava("@SuppressWarnings(\"deprecation\")\n");
    if (ps == null) {
      printJava("public interface INodeVisitor<T> {}\n");
      return;
    }
    else if (ps.getRoot() == null) {
      printJava("public interface I"+ps.getName()+"NodeVisitor<T> extends INodeVisitor<T> {\n");
    } 
    else {
      printJava("public interface I"+ps.getName()+"NodeVisitor<T> extends I"+
                ps.getRoot()+"NodeVisitor<T> {\n");
    }
    
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      if (isAbstract(s)) {
        continue;
      }
      printJava("  public T visit("+makeInterfaceName(s.name)+" node);\n");
    }
	  printJava("}\n");
  } 
  
  private void generateSomeVisitor(VisitorSpec vs, PkgStatus ps) {
    generatePkgDecl(vs.pkg);
    for (String p : packagesAppearing()) {
      printJava("import "+pkgPrefix+p+".*;\n");
    }
    printJava("\n");
    generateClassJavadoc(vs.docs);
    printJava("@SuppressWarnings(\"deprecation\")\n");
    if (ps.getRoot() == null) {
      printJava("public class "+vs.name(ps.getName())+
                "<T> implements I"+ps.getName()+"NodeVisitor<T> {\n");
      vs.generateDefaultConstructors(vs.name(ps.getName()));
      vs.generateRestOfClassBody();
    } else {
      printJava("public class "+vs.name(ps.getName())+
                "<T> extends "+vs.name(ps.getRoot())+
                "<T> implements I"+ps.getName()+"NodeVisitor<T> {\n");
      vs.generateDefaultSubclassConstructors(vs.name(ps.getName()));
    }

    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      if (vs.skip(s)) {
        continue;
      }
      printJava("  public T visit("+makeInterfaceName(s.name)+" n) {\n");
      vs.generateVisitMethodBody(s);
      printJava("  }\n");    
    }    
    printJava("}\n");
  } 
  
  private VisitorSpec[] getVisitors(String pkg) {
    return new VisitorSpec[] {
      new DescendingVisitor(pkg),
      new DefaultVisitor(pkg),
      new AbstractingVisitor(pkg),
      /*
      new PreOrderVisitor(pkg),
      new PostOrderVisitor(pkg),
      */
    };
  }
  
  private class DefaultVisitor extends VisitorSpec {
    DefaultVisitor(final String pkg) {
      super(pkg, "Default", new String[] {  
        "Has every method in INodeVisitor do nothing"
      });
    }
    @Override
    void generateVisitMethodBody(OpSyntax s) {
      printJava("    return defaultValue;\n");
    }
  }
  
  private class AbstractingVisitor extends VisitorSpec {
    AbstractingVisitor(final String pkg) {
      super(pkg, "Abstracting", new String[] {
        "Has each visit() method in INodeVisitor call the",
        "corresponding method for its superinterface"
      });
    }
    @Override
    boolean skip(OpSyntax s) {
      return false;
    }
    @Override
    void generateVisitMethodBody(OpSyntax s) {
      if (s.isRoot) {
        printJava("    return visit(("+ROOT_TYPE+") n);\n");
      } else {
        printJava("    return visit(("+makeInterfaceName(s.parentOperator)+") n);\n");
      }
    }
    @Override
    void generateRestOfClassBody() {      
      printJava("  public T doAccept("+ROOT_TYPE+" node) {\n");
      printJava("    if (node == null) { return defaultValue; }\n");
      printJava("    return node.accept(this);\n");
      printJava("  }\n\n");      
      
      printJava("  public T visit("+ROOT_TYPE+" node) { return defaultValue; }\n");
    }
  }
  
  private class DescendingVisitor extends VisitorSpec {
    DescendingVisitor(final String pkg) {
      super(pkg, "Descending", new String[] {          
      });
    }
    @Override
    void generateVisitMethodBody(OpSyntax s) {
      printJava("    T rv = defaultValue;\n");
      // Set to not skip any syntax (false) 
      s.generateFromSyntax(typeTable, new ASTSyntaxStrategy(false) {
        @Override
        protected void doForVariableChild(OpSyntax s, int i, Child c) {
          printJava("    for("+ROOT_TYPE+" c : n."+getSigForVariableChild(c)+") {\n");
          printJava("      rv = combineResults(rv, doAccept(c));\n");
          printJava("    }\n");
        }
        @Override
        protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
          printJava("    for("+ROOT_TYPE+" c : n."+getSigForFixedChildren(c, child)+") {\n");
          printJava("      rv = combineResults(rv, doAccept(c));\n");
          printJava("    }\n");
        }
        @Override
        protected void doForFixedChild(OpSyntax s, int i, Child c) {
          printJava("    rv = combineResults(rv, doAccept(n."+getSigForFixedChild(c)+"));\n");
        }
        @Override
        protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
        	// Nothing to do
        }
        @Override
        protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
        	// Nothing to do
        }        
      });
      printJava("    return rv;\n");
    }
    @Override
    void generateRestOfClassBody() {     
      printJava("  public T doAccept("+ROOT_TYPE+" node) {\n");
      printJava("    if (node == null) { return defaultValue; }\n");
      printJava("    return node.accept(this);\n");
      printJava("  }\n\n");      
      
      generateClassBodyDeclJavadoc(new String[] {          
      });
      printJava("  public T combineResults(T before, T next) {\n");
      printJava("    return (next == null) ? before : next;\n");
      printJava("  }\n\n");   
    }
  }
  
  private void generateTestNodeVisitor(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg);
    for (String p : packagesAppearing()) {
      printJava("import "+pkgPrefix+p+".*;\n");
    }
    final String mdType = makeInterfaceName("MethodDeclaration");
    printJava("\n");
    printJava("@SuppressWarnings(\"deprecation\")\n");
    if (ps.getRoot() == null) {
      printJava("public class Test"+ps.getName()+"NodeVisitor<T> implements I"+ps.getName()+"NodeVisitor<T> {\n");
      printJava("  protected final boolean testBindings;\n\n");
      printJava("  public Test"+ps.getName()+"NodeVisitor(boolean bind) {\n");
      printJava("    testBindings = bind;\n");
      printJava("  }\n");
      printJava("  public T doAccept("+ROOT_TYPE+" node) {\n");
      printJava("    if (node == null) { return null; }\n");
      printJava("    return node.accept(this);\n");
      printJava("  }\n\n");

      printJava("  protected void checkOwnParent(StringBuilder sb, "+ROOT_TYPE+" n) {\n");
      if (!turnOnTestOutput) {
        printJava("    @SuppressWarnings(\"unused\")");
      }
      if (simplifyGetParent) { 
        printJava("    "+ROOT_TYPE+" parent = n.getParent();\n");       
      } else {
        printJava("    "+ROOT_TYPE+" parent = n.getParent(n);\n");
      }
      if (turnOnTestOutput) {
        printJava("    if (parent != null) {\n");
        printJava("      sb.append(\" \");\n");
        printJava("      sb.append(parent.toString());\n");
        printJava("    }\n");
      }
      printJava("  }\n\n");
      
      printJava("  protected void handleChild("+ROOT_TYPE+" n, "+ROOT_TYPE+" child) {\n");
      printJava("    if (child == null) { return; }\n");
      if (simplifyGetParent) { 
        printJava("    "+ROOT_TYPE+" parent = child.getParent();\n");       
      } else {
        printJava("    "+ROOT_TYPE+" parent = child.getParent(child);\n");
      }
      printJava("    if (parent != n) {\n");
      printJava("      throw new IllegalArgumentException(\"parent didn't match: \"+n+\", \"+parent);\n");
      printJava("    }\n");
      printJava("    doAccept(child);\n");    
      printJava("  }\n\n");
      
      printJava("  protected void checkOverride("+mdType+" od, "+mdType+" n) {\n");
      printJava("    if (od != null) {\n");
      // check that name and #params are same, and the return type is compatible
      printJava("      if (!n.getId().equals(od.getId())) {\n");
      printJava("        throw new NullPointerException(\"method does not override: \"+n.getId()+\", \"+od.getId());\n");
      printJava("      }\n");
      printJava("      if (n.getParamsList().size() != od.getParamsList().size()) {\n");
      printJava("        throw new NullPointerException(\"method does not override: \"+n.getParamsList().size()+\", \"+od.getParamsList().size());\n");
      printJava("      }\n");
      printJava("      if (!n.resolveType().isAssignmentCompatibleTo(od.resolveType())) {\n");
      printJava("        throw new NullPointerException(\"method does not override: return types aren't assignment compatible\");\n");
      printJava("      }\n");
      printJava("    }\n");
      printJava("  }\n\n");
      
      printJava("  public T visit("+ROOT_TYPE+" node) { return null; }\n");
      printJava("\n");  
    } else {
      printJava("public class Test"+ps.getName()+
                "NodeVisitor<T> extends Test"+ps.getRoot()+
                "NodeVisitor<T> implements I"+ps.getName()+"NodeVisitor<T> {\n");

      printJava("  public Test"+ps.getName()+"NodeVisitor(boolean bind) {\n");
      printJava("    super(bind);\n");
      printJava("  }\n");
    }
 
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      final String type = makeInterfaceName(s.name);
      printJava("  public T visit("+type+" n) {\n");
      if (turnOnTestOutput) {
        printJava("    StringBuilder sb = new StringBuilder(\""+type+"\");\n");      
        printJava("    checkOwnParent(sb, n);\n");
      } else {
        printJava("    checkOwnParent(null, n);\n");
      }
      final BindingType bindsTo = getBindsToName(s);
      if (bindsTo != null) {
        // try to resolve binding
        printJava("    if (testBindings && n.resolveBinding() == null) {\n");
        printJava("      throw new NullPointerException(\"binding was null\");\n");
        printJava("    }\n");
      }
      final BindingType bindsToType = getBindsToTypeName(s);
      if (bindsToType != null) {
        // try to resolve type binding
        printJava("    if (testBindings && n.resolveType() == null) {\n");
        printJava("      throw new NullPointerException(\"type binding was null\");\n");
        printJava("    }\n");
      }
      if ("MethodDeclaration".equals(s.name)) {        
        // check overridden methods
        printJava("    checkOverride(n.getOverriddenMethod(), n);\n");
        printJava("    for("+mdType+" od : n.getAllOverriddenMethods()) {\n");
        printJava("      checkOverride(od, n);\n");
        printJava("    }\n");
      }
      // Set to not skip any syntax (false) 
      s.generateFromSyntax(typeTable, new ASTSyntaxStrategy(false) {
        @Override
        protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
          if (turnOnTestOutput) {
            printJava("    sb.append(\" \"+n."+getSigForInfo_NoArgs(a)+");\n");
          } else {
            printJava("    n."+getSigForInfo_NoArgs(a)+";\n");
          }
        }
        @Override
        protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
          if (turnOnTestOutput) {
            printJava("    if (n."+getSigForInfo_WithArgs(arg)+") { sb.append(\" "+arg+"\"); }\n");
          } else {
            printJava("    n."+getSigForInfo_WithArgs(arg)+";\n");
          }
        }
        @Override
        protected void doForVariableChild(OpSyntax s, int i, Child c) {
          printJava("    for("+ROOT_TYPE+" c : n."+getSigForVariableChild(c)+") {\n");
          printJava("      handleChild(n, c);\n");
          printJava("    }\n");
        }
        @Override
        protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
          printJava("    for("+ROOT_TYPE+" c : n."+getSigForFixedChildren(c, child)+") {\n");
          printJava("      handleChild(n, c);\n");
          printJava("    }\n");
        }
        @Override
        protected void doForFixedChild(OpSyntax s, int i, Child c) {
          printJava("    handleChild(n, n."+getSigForFixedChild(c)+");\n");
        }        
      });
      if (turnOnTestOutput) {
        printJava("    System.out.println(sb.toString());\n");
      }
      printJava("    return null;\n");
      printJava("  }\n");    
    }
    printJava("}\n");
  } 
  
  private void generateIJavaBinder(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg, "Interface for resolving Java bindings and the like", "<p>",
                         "Contains one resolve() method for each kind of name/call");
    printJava("import java.util.*;\n");
    printJava("import com.surelogic.ast.*;\n");
    for (String p : packagesAppearing()) {
      printJava("import "+pkgPrefix+p+".*;\n");
    }
    printJava("\n");
    if (ps == null) {
      printJava("public interface IJavaBinder {\n");
      final String mdType = makeInterfaceName("MethodDeclaration");
      generateClassBodyDeclJavadoc("Gets the 'super-implementation' (a la Eclipse) for the method",
                                   "@return The method declaration (possibly abstract) overridden in a superclass of ",
                                   "this method's enclosing class, and otherwise null");
      printJava("  public "+mdType+" getOverriddenMethod("+mdType+" md);\n\n");
      
      generateClassBodyDeclJavadoc("Gets all of immediately overridden methods from super-classes and -interfaces",
                                   "@return A list of all the methods overridden by this one");
      printJava("  public List<"+mdType+"> getAllOverriddenMethods("+mdType+" md);\n\n"); 

      final String cdType = makeInterfaceName("ConstructorDeclaration");
      generateClassBodyDeclJavadoc("Gets the 'super-implementation' (a la Eclipse) for the constructor",
                                   "@return The concrete declaration of the superclass' constructor called by this one");
      printJava("  public "+cdType+" getSuperConstructor("+cdType+" cd);\n\n");
          
      final String dType = makeInterfaceName("TypeDeclaration");
      printJava("  public boolean isSubtypeOf("+dType+" td, IType t);\n\n");
      printJava("  public boolean isCastCompatibleTo("+dType+" td, IType t);\n\n");
      printJava("  public boolean isAssignmentCompatibleTo("+dType+" td, IType t);\n\n");      
    }
    else if (ps.getRoot() == null) {
      printJava("public interface I"+ps.getName()+"JavaBinder extends IJavaBinder {\n");    
      printJava("  public "+makeTypeName("SourceRef")+" resolveExtendsBound("+makeInterfaceName("TypeFormal")+" tf);\n\n");      
    } 
    else {
      printJava("public interface I"+ps.getName()+"JavaBinder extends I"+
                ps.getRoot()+"JavaBinder {\n");
    }

    if (ps != null) {
//    for (Map.Entry<String,OpSyntax> e : iterate()) {
      for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
        OpSyntax s   = e.getValue();
        String iface = makeInterfaceName(s.name);      
        BindingType val = getBindsToName(s);

        if (val != null) {
          generateClassBodyDeclJavadoc("@return true if there is a binding that corresponds",
                                       "to the AST node");
          printJava("  public boolean isResolvable("+iface+" node);\n\n");
          
          String label = val.isSequence ? "Any objects" : "A binding object";
          String verb  = val.isSequence ? "match" : "contains";
          generateClassBodyDeclJavadoc("@return "+label+" that "+verb+" the concrete declaration,",
          "as well as any type context information required by Java 5 generics");
          printJava("  public "+val.getBindingName()+" resolve("+iface+" node);\n");
        }

        val = getBindsToTypeName(s);
        if (val != null) { 
          generateClassBodyDeclJavadoc("@return true if there is a type that corresponds",
                                       "to the AST node");
          printJava("  public boolean isResolvableToType("+iface+" node);\n\n");
          
          String label = val.isSequence ? "binding(s) matching" : "binding corresponding";
          String noun  = val.isSequence ? "Any type binding objects" : "A type binding object";
          String verb  = val.isSequence ? "match" : "contains";
          generateClassBodyDeclJavadoc("Gets the "+label+" to the type of the "+s.name,
              "@return "+noun+" that "+verb+" the concrete declaration,",
          "as well as any type context information required by Java 5 generics");    
          printJava("  public "+val.getTypeName()+" resolveType("+iface+" e);\n");
        }
      }
    }
    printJava("}\n");
  } 
  
  private void generatePackageInfo(String pkg) {
    generatePkgDecl(pkg, "Interfaces for Java AST nodes generated from .op files", "<p>", 
                         "Note: There should be only one active implementation of these interfaces during",
                         "the lifetime of a given JVM, unless otherwise specified for the implementation.",
                         "If this does not hold, there are no guarantees that internal invariants will",
                         "be satisfied", "<p>",
                         "Note: These interfaces do not include any interfaces for mutation, and implementations",
                         "may assume that the ASTs cannot be changed",
                         "<pre>", "</pre>");
  }
  
  private void generateINodeType(String pkg) {
    generatePkgDecl(pkg);
    printJava("\n");
    printJava("public interface INodeType {\n");  
    printJava("}\n");
  }
  
  private void generateNodeType(String pkg, PkgStatus ps) {
    generatePkgDecl(pkg);
    for (String p : packagesAppearing()) {
      printJava("import "+pkgPrefix+p+".*;\n");
    }
    printJava("\n");
    printJava("public enum "+ps.getName()+"NodeType implements INodeType {\n");  
    //boolean first = true;
    for (Map.Entry<String,OpSyntax> e : iterateIfOkToGenerate(ps.getName())) {
      OpSyntax s = e.getValue();
      /*
      if (first) {
        first = false;
      } else {
        printJava(",\n");
      }
      */
      printJava("  "+makeEnumConstant(s.name)+",\n");
    }
    printJava("}\n");
  } 
}
