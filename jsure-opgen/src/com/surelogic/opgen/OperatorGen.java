package com.surelogic.opgen;

import java.io.File;
import java.util.*;
import java.util.regex.*;

import com.surelogic.opgen.generator.*;
import com.surelogic.opgen.syntax.*;


/**
 * Duplicated most of the functionality of the original create-operator
 * script
 * 
 * @author chance
 *
 */
public class OperatorGen extends AbstractASTGenerator {
  private static final String START_INDENT = "    ";
  static final String defaultNodeType = "JavaNode";
  static final String defaultRootOp = "JavaOperator";
  String nodetype = defaultNodeType; 
  String rootOp   = defaultRootOp; 
  final String header = "";
  final String create_method = "jjtCreate";
  final String optype = "Operator";
  final boolean unparse = true;
  final boolean genvisitor = true;
  final String targetdir = ".";

  // The following declarations parameterize the generation
  // of the unparsing code.  It will need to be changed as the manner
  // of unparsing is changed.
  final String unparse_name = "unparse";
  final String unparser_name = "unparser";
  final String unparse_type = "JavaUnparser";
  final String tokentype = "Token";
  final String create_keyword = "new Keyword(%s)";
  final String create_delim = "new Delim(%s)"; 
  
  // the following strings are used with printf and a fixed set of
  // parameters:
  final String unparse_child = unparser_name+".unparse(%s);\n";
  final String unparse_slot = "%s.unparse%s(node,"+unparser_name+");\n";
  final String unparse_literal = "%s.emit("+unparser_name+",node);\n";
  final String unparse_break = "%s.emit("+unparser_name+",node);\n";
  final String unparse_child_is_implicit = unparser_name+".isImplicit(%s)";
  final String default_break = "<sp>";
  final String break_format = unparser_name+".getStyle().get%s()";
  // $breakformat = "BP%s";
  
  public OperatorGen(String prefix, String suffix) {
    super(prefix, suffix);
  }

  public OperatorGen() {
    super(STD_PREFIX);
  }
  
  /*
  public static void main(String[] args) {
    String name = (args.length > 0) ? args[0] : "Test.op";
    OpSyntax s = (new SyntaxBuilder()).parse(name);
    s.printState(System.out);
    System.out.println();
    
    OperatorGen m = new OperatorGen();
    m.generate(s);    
  }
  */
  
  public static void main(String[] args) {
    OperatorGen m = new OperatorGen(STD_PREFIX, STD_SUFFIX);
    if (args.length == 0) {
      m.generate(new String[] { "-out", "out", "ops"});
    } else {
      m.generate(args);
    }
  }
  
  @Override
  protected String makeNodeName(String name) {  
    return name;
  }
  @Override
  protected String getCommentForFixedChild(OpSyntax s, Child c) {
    return "@return A non-null node";
  }
  @Override
  protected String getCommentForVariableChild(OpSyntax s, Child c) {
    return "@return A non-null, but possibly empty list of nodes";
  }
  @Override
  protected String getCommentForFixedChildren(OpSyntax s, Child c, OpSyntax child) {
    return getCommentForFixedChild(s, c);
  }
  @Override
  protected final String computeFixedChildType(Child c) {
    return "IRNode";
  }
  @Override
  protected final String computeVariableChildType(Child c) {
    return "IRNode[]";
  }
  @Override
  protected final String computeFixedChildrenType(OpSyntax child) {
    return "IRNode";
  }  
  @Override
  protected final boolean ignoreArgs() {
    return true;
  }
  
  @Override
  protected void initEach(OpSyntax s) {
    if (JAVA_PROMISE.equals(s.packageName)) {
      nodetype = "JavaPromise";
      rootOp = "JavaPromiseOperator";
    } else {
      nodetype = defaultNodeType;
      rootOp = defaultRootOp;
    }
  }
  
  @Override
  protected void generateIntro(OpSyntax s) {
//  while ($arg = shift) {
//  if ($arg =~ /^-header=(.*)/) {
//  $header = $1;
//  } elsif ($arg =~ /^-nodetype=(.*)/) {
//  $nodetype = $1;
//  } elsif ($arg =~ /^-createmethod=(.*)/) {
//  $create_method = $1;
//  } elsif ($arg =~ /^-optype=(.*)/) {
//  $optype = $1;
//  } elsif ($arg =~ /^-unparsetype=(.*)/) {
//  $unparse_type = $1;
//  } elsif ($arg =~ /^-debug=(.*)/) {
//  $debug = $1;
//  } elsif ($arg =~ /^-unparse=(.*)/) {
//  $unparse = $1;
//  } elsif ($arg =~ /^-genvisitor=(.*)/) {
//  $genvisitor = $1;
//  } elsif ($arg =~ /^-breakformat=(.*)/) {
//  $break_format = $1;
//  } elsif ($arg =~ /^-defaultbreak=(.*)/) {
//  $default_break = $1;
//  } elsif ($arg =~ /^-target=(.*)/) {
//  $targetdir = $1;
//  } elsif ($arg =~ /^([A-Za-z]+):([A-Za-z]+)$/) {
//  $typetable{$1} = $2;
//  } elsif ($arg =~ /^(.*).java$/) { 
//  $javafile = $arg;
//  } elsif ($arg =~ /^(.*).op$/) {
//  open(OPFILE,$arg) || throw new Error("Could not read operator file $arg");
//  open(JAVAFILE,">$javafile") || throw new Error("Could not write Java file $javafile for $arg");    
    printJava("// Generated from "+s.sourceFilename+":  Do *NOT* edit!\n\n");
    printJava("import java.util.*;\n");
    if (s.afterText.contains("Level")) {
      printJava("import java.util.logging.*;\n");
    }
    
    if (s.afterText.contains("Component") || s.afterText.contains("Sink")) {
      printJava("import edu.cmu.cs.fluid.control.*;\n");
    }
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    if (s.afterText.contains("IBinder") /*|| s.afterOp.contains("IHasType")*/) {
      printJava("import edu.cmu.cs.fluid.java.bind.*;\n");
    }
    if (s.afterText.contains("ExceptionLabel") || 
        s.afterText.contains("BreakLabel") ||
        s.afterText.contains("ContinueLabel") ||
        s.afterText.contains("ReturnLabel")) {
      printJava("import edu.cmu.cs.fluid.java.control.*;\n");
    }
    printJava("import edu.cmu.cs.fluid.java.operator.*;\n");
    if (s.afterText.contains("ReceiverDeclaration")) {
      printJava("import edu.cmu.cs.fluid.java.promise.*;\n");
    }
    printJava("import edu.cmu.cs.fluid.parse.JJNode;\n");
    printJava("import edu.cmu.cs.fluid.tree.*;\n");
    printJava("import edu.cmu.cs.fluid.unparse.Token;\n");
    printJava("import edu.cmu.cs.fluid.unparse.*;\n");
    if (s.isVariable()) {
      printJava("import edu.cmu.cs.fluid.util.*;\n");
    }
    printJava("\n");
//  if ($header ne "") {
//  open(HEADERFILE,$header) || throw new Error("Could not read header file $header");
//  while ($line = <HEADERFILE>) {
//  if (!($line =~ m/^#/)) {
//  &printjava($line);
//  }
//  }
//  close(HEADERFILE);
//  }
    printJava(s.beforeText);
    printJava("@SuppressWarnings(\"all\")\n");
    printJava(s.modifiers);
    printJava("class ");
    printJava(s.name);
    printJava(" extends ");
    if (s.isRoot) {
      printJava(rootOp);
    } else {
      printJava(s.parentOperator);
    }
    printJava(" ");
    String ifaces = "";
    if (s.isRoot) {
      /*
      if (s.afterOp.contains("implements")) {        
        // Splice IAcceptor in as the first interface implemented
        int len = "implements ".length();
        int split = len + s.afterOp.indexOf("implements ");
        printJava(s.afterOp.substring(0, split));
        printJava("IAcceptor, ");
        printJava(s.afterOp.substring(split));      
      } else {
        printJava(" implements IAcceptor ");
        printJava(s.afterOp);
      }
      */
      if (this.isLogicallyInvisible(s)) {
        ifaces = "IAcceptor, ILogicallyInvisible";
      } else {
        ifaces = "IAcceptor";
      }
    } 
    else if (this.isLogicallyInvisible(s)) {
      ifaces = "ILogicallyInvisible";
    }
    if (ifaces.length() > 0) {
      spliceInterfaces(s.afterOp, ifaces);
    } else {
      printJava(s.afterOp);
    }
    printJava("\n{\n");
  }

  private void spliceInterfaces(String existing, String ifaces) {
    if (existing.contains("implements")) {        
      // Splice ifaces in as the first interfaces implemented
      int len = "implements ".length();
      int split = len + existing.indexOf("implements ");
      printJava(existing.substring(0, split));
      printJava(ifaces);
      printJava(", ");
      printJava(existing.substring(split));      
    } else {
      printJava(" implements ");
      printJava(ifaces);
      printJava(" ");
      printJava(existing);
    }
  }
  
  @Override
  protected void generateFields(OpSyntax s) {
    printJava("  protected "+s.name+"() {}\n\n");
    printJava("  public static final "+s.name+" prototype = new "+s.name+"();\n\n");
  }

  @Override
  protected void generateMethods(OpSyntax s) {
    if (!s.isRoot) {
      printJava("  @Override\n");
      printJava("  public Operator superOperator() {\n");
      printJava("    return "+s.parentOperator+".prototype;\n");
      printJava("  }\n\n");
    }
    if (s.isConcrete) {
      // generate a routine that says it is a concrete production:
      printJava("  @Override\n");
      printJava("  public boolean isProduction() {\n");
      printJava("    return true;\n");
      printJava("  }\n\n");
    }

    if (s.isConcrete) {
      generateMethodsFromSyntax(s);
      generateCreationRoutines(s);
      
      if (s.attributes.size() != 0) {
        generateMethodsFromAttributes(s);
      }
      generateAccessRoutines(s);    
    }
    generateAccept(s);
  }

  private void generateMethodsFromAttributes(OpSyntax s) {
    // generate check routine
    printJava("  @Override\n");
    printJava("  public boolean isComplete(IRNode node) {\n");
    printJava("    if (!super.isComplete(node)) return false;\n");
    printJava("    try {\n");
    s.generateFromSyntax(typeTable, new SyntaxStrategy() {
      @Override
      public void doForInfo(OpSyntax s, int i, Attribute a, String type) {
        printJava("      "+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(node);\n");
      }
    });      
    printJava("    } catch (SlotUndefinedException ex) {\n");
    printJava("      return false;\n");
    printJava("    }\n");
    printJava("    return true;\n");
    printJava("  }\n\n");
    
    // generate copy routine
    printJava("  @Override\n");
    printJava("  public IRNode copyTree(IRNode node) {\n");
    printJava("    IRNode _result = super.copyTree(node);\n");
    s.generateFromSyntax(typeTable, new SyntaxStrategy() {
      @Override
      public void doForInfo(OpSyntax s, int i, Attribute a, String type) {
        printJava(START_INDENT+accessRcvrTable.get(a.type)+".set"+capitalize(a.type)+"(_result,"+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(node));\n");
      }
    });       
    printJava("    return _result;\n");
    printJava("  }\n\n");
    
    // generate local compare routine
    printJava("  @Override\n");   
    printJava("  public boolean isEquivalentNode(IRNode n1, IRNode n2) {\n");
    printJava("    return super.isEquivalentNode(n1, n2) &&\n");
    s.generateFromSyntax(typeTable, new SyntaxStrategy() {
      @Override
      public void doForInfo(OpSyntax s, int i, Attribute a, String type) {
        if (isPrimType(type)) {
          printJava("           ("+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(n1) == "+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(n2)) &&\n");
        } else {
          printJava("           ("+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(n1).equals("+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(n2))) &&\n");     
        }
      }
    });      
    printJava("           true;\n");
    printJava("  }\n\n");
  }

  protected boolean isPrimType(String typeName) {
    if (typeName.contains(".")) {
      return false;
    }
    return Character.isLowerCase(typeName.charAt(0));
  }

  private void generateMethodsFromSyntax(OpSyntax s) {
    // variable *or* fixed number of children
    generateMethod(s, new ChildMethodDescriptor("Operator childOperator(int i)", "null") { 
      @Override
      protected String childValue(OpSyntax s, Child c) {
        return c.type+".prototype";
      }
      @Override
      protected String variableValue(OpSyntax s) { 
        return s.variableChild.type+".prototype";
      }
    });
    
    // efficient for special case
    if (s.isVariable() && s.numChildren == 0) {
      printJava("  @Override\n");
      printJava("  public Operator childOperator(IRLocation loc) {\n");
      printJava("    return "+s.variableChild.type+".prototype;\n");
      printJava("  }\n\n");
    }
    
    generateMethod(s, new ChildMethodDescriptor("String childLabel(int i)", "\"\"") {
      @Override
      protected String childValue(OpSyntax s, Child c) {
        return "\""+c.name+"\"";
      }
      @Override
      protected String variableValue(OpSyntax s) { 
        return "\""+s.variableChild.name+"\"";
      }
    });
    
    generateMethod(s, new InfoMethodDescriptor("String infoType(int i)", "\"\"") {
      @Override
      protected String infoValue(OpSyntax s, Attribute a) {
        return "\""+a.type+"\"";
      }      
    });
    
    generateMethod(s, new InfoMethodDescriptor("String infoLabel(int i)", "\"\"") {
      @Override
      protected String infoValue(OpSyntax s, Attribute a) {
        return "\""+a.name+"\"";
      }      
    });
    
    if (s.isVariable()) {
      printJava("  public Operator variableOperator() {\n");
      printJava("    return "+s.variableChild.type+".prototype;\n");
      printJava("  }\n\n");
    }
    
    printJava("  @Override\n");
    printJava("  public int numInfo() {\n");
    printJava("    return "+s.attributes.size()+";\n");
    printJava("  }\n\n");    
    
    printJava("  @Override\n");
    printJava("  public int numChildren() {\n");
    if (s.isVariable()) {
      int tmp = s.numChildren+1;
      printJava("    return -"+tmp+";\n");
    } else {
      printJava("    return "+s.numChildren+";\n");
    }
    printJava("  }\n\n");
  }
  
  @Override
  protected void generateEnding(OpSyntax s) {
    printJava("  "+s.afterText);
  }
  
  private void generateCreationRoutines(OpSyntax s) {
    if (s.numChildren != 0 || s.attributes.size() != 0 || s.isVariable()) {
      final String intro = "  public static "+defaultNodeType+" createNode(";
      printJava(intro);
      s.generateFromSyntax(typeTable, new ConstructorFormalsStrategy(intro)); 
      printJava(") {\n");
      printJava("    return createNode(tree");
      s.generateFromSyntax(typeTable, new SyntaxStrategy() {
        @Override
        public void doForInfo(OpSyntax s, int i, Attribute a, String type) {
          printJava(", "+a.name);
        }
        @Override
        public void doForChild(OpSyntax s, int i, Child c, boolean isVariable) {
          printJava(", "+c.name);
        }
      });
      printJava(");\n");
      printJava("  }\n");
      
      printJava("  public static "+defaultNodeType+" createNode(SyntaxTreeInterface tree, ");
      s.generateFromSyntax(typeTable, new ConstructorFormalsStrategy(intro));
      printJava(") {\n");
      final String create = nodetype+".make"+nodetype; // "new "+nodetype
      if (s.numChildren == 0 && !s.isVariable()) {
        printJava(START_INDENT+defaultNodeType+" _result = "+create+"(tree, prototype);\n");
      } else if (s.numChildren == 0 && s.isVariable()) {
        printJava(START_INDENT+defaultNodeType+" _result = "+create+"(tree, prototype,"+s.variableChild.name+");\n");
      } else if (!s.isVariable()) {
        printJava(START_INDENT+defaultNodeType+" _result = "+create+"(tree, prototype, new IRNode[]{ ");
        s.generateFromSyntax(typeTable, new SyntaxStrategy() {
          // attributes handled below
          @Override
          public void doForChild(OpSyntax s, int i, Child c, boolean isVariable) {
            printJava(c.name+", ");
          }
        });
        printJava("});\n");
      } else {
        // variable *and* more children.
        printJava("    IRNode[] _children = new IRNode[s.children+"+s.variableChild.name+".length];\n");
        s.generateFromSyntax(typeTable, new SyntaxStrategy() {
          int i = 0;
          @Override
          public void doForChild(OpSyntax s, int j, Child c, boolean isVariable) {
            if (!isVariable) {
              printJava("    _children["+i+"] = "+c.name+";\n");
            }
            i++;
          }
        });
        printJava("    for (int _i=0; _i < "+s.variableChild.name+".length; ++_i) {\n");
        printJava("      _children["+s.numChildren+"+_i] = "+s.variableChild.name+" [_i];\n");
        printJava("    }\n");
        printJava(START_INDENT+nodetype+" _result = "+create+"(tree, prototype, _children);\n");
      }
      // Set attributes
      s.generateFromSyntax(typeTable, new SyntaxStrategy() {
        @Override
        public void doForInfo(OpSyntax s, int i, Attribute a, String type) {
          printJava(START_INDENT+accessRcvrTable.get(a.type)+".set"+capitalize(a.type)+"(_result,"+a.name+");\n");
        }
      });
      printJava("    return _result;\n");
      printJava("  }\n\n");
    }
  }

  private void generateAccessRoutines(OpSyntax s) {
    // Someone made the access check only happen
    // if logging is on.  This is a mistake. --JTB
    final String accesscheck = 
      // "    if (LOG.isLoggable(Level.INFO)) {\n" . 
      "    Operator op = tree.getOperator(node);\n" +
      "    if (!(op instanceof "+s.name+")) {\n" +
      "      throw new IllegalArgumentException(\"node not "+s.name+": \"+op);\n" +
      "    }\n"
      // . "    }\n"
      ;
    
    s.generateFromSyntax(typeTable, new SyntaxStrategy() {
      int i = 0;
      @Override
      public void doForInfo(OpSyntax s, int i, Attribute a, String type) {
        printJava("  public static "+type+" get"+capitalize(a.name)+"(IRNode node) {\n");
        printJava(accesscheck);
        printJava("    return "+accessRcvrTable.get(a.type)+".get"+capitalize(a.type)+"(node);\n");
        printJava("  }\n\n");
      }
      @Override
      public void doForChild(OpSyntax s, int j, Child c, boolean isVariable) {   
        final String name = capitalize(c.name);
        if (isVariable) {          
          printJava("  public static IRNode get"+name+"(IRNode node, int i) {\n");
          printJava("    return get"+name+"(tree, node, i);\n");
          printJava("  }\n\n");
          printJava("  public static Iteratable<IRNode> get"+name+"Iterator(IRNode node) {\n");
          printJava("    return get"+name+"Iterator(tree, node);\n");
          printJava("  }\n\n");
          
          printJava("  public static IRNode get"+name+"(SyntaxTreeInterface tree, IRNode node, int i) {\n");
          printJava(accesscheck);
          printJava("    return tree.getChild(node,"+s.numChildren+"+i);\n");
          printJava("  }\n\n");
          printJava("  public static Iteratable<IRNode> get"+name+"Iterator(SyntaxTreeInterface tree, IRNode node) {\n");
          printJava(accesscheck);
          printJava("    Iteratable<IRNode> _result = tree.children(node);\n");
          for (int i=0; i < s.numChildren; ++i) {
            printJava("    _result.next(); // discard prefix\n");
          }
          printJava("    return _result;\n");
          printJava("  }\n\n");
        }        
        else { // a fixed child
          printJava("  public static IRNode get"+name+"(IRNode node) {\n");
          printJava("    return get"+name+"(tree, node);\n");
          printJava("  }\n\n");
          
          printJava("  public static void set"+name+"(IRNode node, IRNode ch) {\n");
          printJava("    set"+name+"(tree, node, ch);\n");
          printJava("  }\n\n");
          
//          if (!s.isRoot && !hasAbstractParents(s)) {
//            printJava("  @Override\n");
//          }
          if ("OptArguments".equals(c.type)) {
              printJava("  public IRNode get_"+name+"(IRNode node) throws CallInterface.NoArgs {\n");
          } else {
              printJava("  public IRNode get_"+name+"(IRNode node) {\n");
          }
          printJava("    return get"+name+"(tree, node);\n");
          printJava("  }\n\n");
          
//          if (!s.isRoot && !hasAbstractParents(s)) {
//            printJava("  @Override\n");
//          }
          printJava("  public void set_"+name+"(IRNode node, IRNode ch) {\n");
          printJava("    set"+name+"(tree, node, ch);\n");
          printJava("  }\n\n");
          
          if (c.isAbstract()) {      
            // static methods that calls methods below
            printJava("  public static IRNode get"+name+"(SyntaxTreeInterface tree, IRNode node) {\n");
            printJava(accesscheck);
            printJava("    return (("+s.name+") op).get_"+name+"(tree, node);\n");
            printJava("  }\n\n");
                      
            printJava("  public static void set"+name+"(SyntaxTreeInterface tree, IRNode node, IRNode ch) {\n");
            printJava(accesscheck);
            printJava("    (("+s.name+") op).set_"+name+"(tree, node, ch);\n");
            printJava("  }\n\n");
            
            // methods overridable by the concrete ops, initially throwing exceptions
            printJava("  public IRNode get_"+name+"(SyntaxTreeInterface tree, IRNode node) {\n");
            printJava("    throw new UnsupportedOperationException();\n");
            printJava("  }\n\n");
                      
            printJava("  public void set_"+name+"(SyntaxTreeInterface tree, IRNode node, IRNode ch) {\n");
            printJava("    throw new UnsupportedOperationException();\n");
            printJava("  }\n\n");
          } else {
            printJava("  public static final int "+c.name+"Loc = "+i+";\n");
            printJava("  public static final IRLocation "+c.name+"Location = IRLocation.get("+i+");\n\n");

            printJava("  public static IRNode get"+name+"(SyntaxTreeInterface tree, IRNode node) {\n");
            printJava(accesscheck);
            printJava("    return tree.getChild(node,"+i+");\n");
            printJava("  }\n\n");
                                  
            printJava("  public static void set"+name+"(SyntaxTreeInterface tree, IRNode node, IRNode ch) {\n");
            printJava(accesscheck);
            printJava("    tree.setChild(node,"+i+",ch);\n");
            printJava("  }\n\n");
            
            // FIX: these could be omitted if there's no abstract child that needs it
//            if (!s.isRoot && !hasAbstractParents(s)) {
//              printJava("  @Override\n");
//            }
            printJava("  public IRNode get_"+name+"(SyntaxTreeInterface tree, IRNode node) {\n");
            printJava("    return get"+name+"(tree, node);\n");
            printJava("  }\n\n");
                      
//            if (!s.isRoot && !hasAbstractParents(s)) {
//              printJava("  @Override\n");
//            }
            printJava("  public void set_"+name+"(SyntaxTreeInterface tree, IRNode node, IRNode ch) {\n");
            printJava("    set"+name+"(tree, node, ch);\n");
            printJava("  }\n\n");
          }
          i++;
        }
      }
    });
    generateUnparseRoutines(s, accesscheck);
    generatePrecRoutines(s, accesscheck);
    generateMissingTokens(s, accesscheck);
  }
  
  private void generatePrecRoutines(OpSyntax s, String accesscheck) {
    if (s.usesPrecedence) {
      if (s.isVariable()) {
        final String childprec = s.variableChild.prec;
        printJava("  @Override\n");     
        printJava("  public int childPrecedence(int i) {\n");
        printJava("    return "+childprec+";\n");
        printJava("  }\n\n");
        printJava("  @Override\n");     
        printJava("  public int childPrecedence(IRLocation loc) {\n");
        printJava("    return "+childprec+";\n");
        printJava("  }\n\n");
      } else { 
        printJava("  @Override\n");         
        printJava("  public int childPrecedence(int i) {\n");
        printJava("    switch (i) {\n");
        int i = 0;
        for (Child c : s.children) {
          Matcher m = precMatch.matcher(c.prec);
          if (m.matches()) {
            printJava("    case "+i+": return "+m.group(1)+";\n");
          }
          i++;
        }
        printJava("    default: return 0;\n");
        printJava("    }\n");
        printJava("  }\n\n");
      }
    }
  }
  /*
  else {
    generateAccept(s);
    if (word.equals("}")) {
      printJava("\n");
    }
    printJava(line);
  }
}
}
*/
 
  class TokenStrategy extends SyntaxStrategy {
    int keyword = 0;
    @Override
    public void doForToken(Token token) {
      keyword += 1;
      printJava("  private static "+tokentype+" littoken"+keyword+" = ");
      if (keywordMatch.matcher(token.text).matches()) {
        printfJava(create_keyword, token);
      } else {
        printfJava(create_delim, token);
      }
      printJava(";\n");
    }
  }
  
  private void generateUnparseRoutines(OpSyntax s, final String accesscheck) {
    if (unparse) {
      final int total = s.syntax.size();
      
      // generate tokens for literals:
      TokenStrategy ts = new TokenStrategy();
      s.generateFromSyntax(typeTable, ts);           
      
      if (ts.keyword > 0) {
        printJava("\n");
        printJava("  @Override\n");
        printJava("  public Token asToken() {\n");
        printJava("    return littoken1;\n");
        printJava("  }\n\n");
      }     
      
      if (total == 0) {
        printJava("  public void "+unparse_name+
                  "Wrapper(IRNode node, "+unparse_type+" "+unparser_name+") {\n");
        printJava("  }\n\n");
      } else if (s.isVariable() && empty_yield_possible(s, 0, total-1)) {
        printJava("  public void "+unparse_name+
                  "Wrapper(IRNode node, "+unparse_type+" "+unparser_name+") {\n");
        printJava("    if ("+unparser_name+".getTree().numChildren(node) > 0) super."+unparse_name +
                  "Wrapper(node,"+unparser_name+");\n");
        printJava("  }\n\n");
      }
      
      printJava("  @Override public void "+unparse_name+
      "(IRNode node, "+unparse_type+" "+unparser_name+") {\n");
      
      printJava("    SyntaxTreeInterface tree = "+unparser_name+".getTree();\n");
      printJava(accesscheck);
      
      if (s.isVariable()) {
        printJava("    Iteratable<IRNode> e = tree.children(node);\n");
      }
      generateMethodFromSyntax(s, new UnparseStrategy());
      /*
      String indent = START_INDENT;
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
        SyntaxElement nextElement = s.syntax.get(nextj);
        if (element instanceof CloseParen && variableMatch.matcher(nextElement.text).matches()) {  // $syntax[$nextj] =~ /^[+*]/) {
          printJava(indent+"while (e.hasNext()) {\n");
          indent += "  ";
        } else if (element instanceof CloseParen && nextElement instanceof Option) {
          printJava(indent+"if (e.hasNext()) {\n");
          indent += "  ";
        }
          
        if (element instanceof Attribute) {
          // if ($broke ne true && $default_break =~ /^<(.+)>$/) {
          //   &printbreak(indent+$1);
          // }
          printJava(indent);
          printfJava(unparse_slot, nodetype, ((Attribute) element).type);
          // $broke = false;
        } else if (element instanceof Child) {
          // if ($broke ne true && $default_break =~ /^<(.+)>$/) {
          //   &printbreak(indent+$1);
          // }
          printJava(indent);
          if (s.isVariable()) {
            printfJava(unparse_child, nodetype, "e.next()");
          } else {
            printfJava(unparse_child,nodetype, "tree.getChild(node,"+i+")");
            i += 1;
          }
          // $broke = false
        } else if (element instanceof Token) { 
            // if ($broke ne true && $default_break =~ /^<(.+)>$/) {
            //   &printbreak(indent+$1);
            // }

            // FIX $kind = $delimiters{substr($element,1,1)};
            printJava(indent);
            keyword += 1;
            printfJava(unparse_literal, "littoken"+keyword);
            // $broke = false;
        } else if (element instanceof Tag) {
          // special case for implicit markers: <?> </?>
          if (element.text.equals("<?>")) {
            if (s.isVariable()) {
              throw new Error("cannot use <?> </?> in variable lists (missing feature)");
            }
            printJava(indent +"if (!");
            printfJava(unparse_child_is_implicit, "tree.getChild(node,"+i+")");
            printJava(") {\n");
            indent += "  ";
          } else if (element.text.equals("</?>")) {
            indent = indent.substring(2);
            printJava(indent +"}\n");
          } else {
            printBreak(indent, element.text.substring(1, element.text.length()-1));
          }
          // $broke = true;
        } else if (plusOrStarDotMatch.matcher(element.text).matches()) {
          printJava(indent+"if (!e.hasNext()) break;\n");
        } else if (element.text.equals("?/")) {
          indent = indent.substring(2);
          printJava(indent+"} else {\n");
          indent += "  ";
        }
        
        // are we ending a loop or an if?
        SyntaxElement lastElement = s.syntax.get(lastj);
        if (element.text.equals("?/") ||
            (!(element instanceof OpenParen) && plusOrStarDotMatch.matcher(lastElement.text).matches())) {
          indent = indent.substring(2);
          printJava(indent+"}\n"); // end while
        } else if (element.equals("?") ||
                   (!element.equals("(") && lastElement.text.equals("?/"))) {
          indent = indent.substring(2);
          printJava(indent+"}\n");
        } 
      }
      if (!indent.equals(START_INDENT)) {
        throw new Error("got confused in unparsing");
      }
      */
      printJava("  }\n\n");

    }
  }     

  private boolean empty_yield_possible(OpSyntax s, int start, int stop) {
    SyntaxElement startSyntax = s.syntax.get(start);
    SyntaxElement stopSyntax = s.syntax.get(stop);
    
    // check for X ? and X *
    if (stopSyntax.text.equals("*") || stopSyntax.text.equals("?")) {
      return (start+1 == stop ||
          (startSyntax instanceof OpenParen && ((OpenParen) startSyntax).closeIndex+1 == stop));
    }
    // check for X */ Y
    if (startSyntax instanceof OpenParen) {
      start = ((OpenParen) startSyntax).closeIndex;
    }
    ++start;
    if (!startSyntax.text.equals("*/")) {
      return false;
    }
    ++start;
    if (startSyntax instanceof OpenParen) {
      start = ((OpenParen) startSyntax).closeIndex;
    }
    return (start == stop);
  }

  private void printBreak(String indent, String name) {
    if (name.equals("")) {
      name = "none";
    }
    printJava(indent);
    if (name.startsWith("/")) {
      name = "end" + name.substring(1);
    }    
    printfJava(unparse_break, sprintf(break_format, name.toUpperCase()));
  }
  
  private String sprintf(String break_format2, String s) {
    return String.format(break_format2, s);
  }
  
  /**
   * Generate functions that return the tokens that appear
   * between the various attributes and children
   */
  private void generateMissingTokens(OpSyntax s, String accesscheck) {
    final int total = s.syntax.size();
    // create isMissingTokens function
    printJava("  @Override\n");
    printJava("  public boolean isMissingTokens(IRNode node)  {\n");
    if (total == 0) {
      printJava("    return false;\n");
      printJava("  }\n\n");
    } else if (s.isVariable() && empty_yield_possible(s, 0, total - 1)) {
      printJava("    if (JJNode.tree.numChildren(node) > 0) return true;\n");
      printJava("    else return false;\n");
      printJava("  }\n\n");
    } else {
      printJava("    return true;\n");
      printJava("  }\n\n");
    }

    // create missingTokens function
    printJava("  @Override\n");     
    printJava("  public Vector<Token>[] missingTokens(IRNode node) {\n");
    printJava("    SyntaxTreeInterface tree = JJNode.tree;\n");
    printJava(accesscheck);
    if (s.isVariable()) {
      printJava("    Iteratable<IRNode> e = tree.children(node);\n");
      printJava("    int i = 0;\n");
    }
    printJava("    int numChildren = tree.numChildren(node);\n");
    // printJava("    if ((index < 0) || (index > numChildren))\n");
    // printJava("       throw new IllegalArgumentException(\"index is out of bound.\");\n");
    printJava("    Vector<Token>[] TokenList = new Vector[numChildren+1];\n");
    printJava("    for (int j = 0; j < numChildren + 1; j++)\n");
    printJava("       TokenList[j] = new Vector<Token>();\n");
    generateMethodFromSyntax(s, new MissingTokensStrategy());
    printJava("    return TokenList;\n");
    printJava("  }\n\n");
  }
  
  private void generateMethodFromSyntax(OpSyntax s, UnfoldSyntaxStrategy uss) {
    final int total = s.syntax.size();
    String indent = START_INDENT;
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
          printJava(indent+"while (e.hasNext()) {\n");
          indent += "  ";
        } else if (nextElement instanceof Option) {
          printJava(indent+"if (e.hasNext()) {\n");
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
        printJava(indent+"if (!e.hasNext()) break;\n");
      } else if (element.text.equals("?/")) {
        printJava(indent.substring(2)+"} else {\n");
      }
      
      // are we ending a loop or an if?
      SyntaxElement lastElement = (lastj >=0) ? s.syntax.get(lastj) : null;
      if ((element.text.equals("*") || element.text.equals("+")) ||
          (lastElement != null && !(element instanceof OpenParen) && plusOrStarDotMatch.matcher(lastElement.text).matches())) {
        indent = indent.substring(2);
        printJava(indent+"}\n"); // end while
      } else if (element.text.equals("?") ||
          (lastElement != null && !(element instanceof OpenParen) && lastElement.text.equals("?/"))) {
        indent = indent.substring(2);
        printJava(indent+"}\n");
      } 
    }
    if (!indent.equals(START_INDENT)) {
      throw new Error("got confused in unparsing");
    }
  }
  
  private class UnparseStrategy extends UnfoldSyntaxStrategy {
    @Override
    public void handleAttribute(OpSyntax s, String indent, int i, Attribute a) {
      // if ($broke ne true && $default_break =~ /^<(.+)>$/) {
      //   &printbreak(indent+$1);
      // }
      printJava(indent);
      printfJava(unparse_slot, unparseRcvrTable.get(a.type), a.type);
      // $broke = false;
      
    }
    @Override
    public boolean handleChild(OpSyntax s, String indent, int i, Child c) {
      // if ($broke ne true && $default_break =~ /^<(.+)>$/) {
      //   &printbreak(indent+$1);
      // }
      printJava(indent);
      if (s.isVariable()) {
        printfJava(unparse_child, "e.next()");
      } else {
        printfJava(unparse_child, "tree.getChild(node,"+i+")");
        return true;
      }
      // $broke = false
      return false;
    }
    @Override
    public void handleToken(OpSyntax s, String indent, int i, int keyword, Token t) {
      // if ($broke ne true && $default_break =~ /^<(.+)>$/) {
      //   &printbreak(indent+$1);
      // }
      
      // FIX $kind = $delimiters{substr($element,1,1)};
      printJava(indent);

      printfJava(unparse_literal, "littoken"+keyword);
      // $broke = false;
    }
    @Override
    public String handleTag(OpSyntax s, String indent, int i, Tag t) {
      // special case for implicit markers: <?> </?>
      if (t.text.equals("<?>")) {
        if (s.isVariable()) {
          throw new Error("cannot use <?> </?> in variable lists (missing feature)");
        }
        printJava(indent +"if (!");
        printfJava(unparse_child_is_implicit, "tree.getChild(node,"+i+")");
        printJava(") {\n");
        indent += "  ";
      } else if (t.text.equals("</?>")) {
        indent = indent.substring(2);
        printJava(indent +"}\n");
      } else {
        printBreak(indent, t.text.substring(1, t.text.length()-1));
      }
      // $broke = true;
      return indent;
    }
  }
  
  private class MissingTokensStrategy extends UnfoldSyntaxStrategy {
    @Override
    public void handleAttribute(OpSyntax s, String indent, int i, Attribute a) {
      printJava(indent);
      if (a.type.equals("Modifiers")) {
        if (s.isVariable()) {
          printJava("Token[] tl = JavaNode.getModiferTokens(node);\n");
          printJava(indent+"if (tl != null && tl.length > 0)\n");
          printJava(indent+"  for (int j = 0; j < tl.length; j++)\n");
          printJava(indent+"    TokenList[i].add(tl[j]);\n");
        } else {
          printJava("Token[] tl = JavaNode.getModiferTokens(node);\n");
          printJava(indent+"if (tl != null && tl.length > 0)\n");
          printJava(indent+"  for (int j = 0; j < tl.length; j++)\n");
          printJava(indent+"    TokenList["+i+"].add(tl[j]);\n");
        }
      } else if (a.type.equals("Info")) {
        if (s.isVariable()) {
          printJava("Identifier id = new Identifier(JJNode.getInfo(node));\n");
          printJava(indent+"TokenList[i].add(id);\n");
        } else {
          printJava("Identifier id = new Identifier(JJNode.getInfo(node));\n");
          printJava(indent+"TokenList["+i+"].add(id);\n");
        }
      } else if (a.type.equals("Op")) {
        if (s.isVariable()) {
          printJava("TokenList[i].add(JavaNode.getOp(node).asToken());\n");
        } else {
          printJava("TokenList["+i+"].add(JavaNode.getOp(node).asToken());\n"); 
        }
      } else if (a.type.equals("Code")) {
        if (s.isVariable()) {
          printJava("TokenList[i].add(new Keyword(\"<compiled>\"));\n");
        } else {
          printJava("TokenList["+i+"].add(new Keyword(\"<compiled>\"));\n");
        }
      } else if (a.type.equals("DimInfo")) {
        if (s.isVariable()) {
          printJava("Token tok = JavaNode.getDimToken(node);\n");
          printJava(indent+"if (tok != null)\n");
          printJava(indent+"  TokenList[i].add(tok);\n");
        } else {
          printJava("Token tok = JavaNode.getDimToken(node);\n");
          printJava(indent+"if (tok != null)\n");
          printJava(indent+"  TokenList["+i+"].add(tok);\n");
        }
      }
    }

    // need to increment if true
    @Override
    public boolean handleChild(OpSyntax s, String indent, int i, Child c) {
      printJava(indent);
      if (s.isVariable()) {
        printJava("e.next();\n");
        printJava(indent+"i++;\n");
      } else {
        printJava("tree.getChild(node,"+i+");\n");
        return true;
      }
      return false;
    }
    
    @Override
    public void handleToken(OpSyntax s, String indent, int i, int keyword, Token t) {
      // $kind = $delimiters(substr(element,1,1));
      printJava(indent);
      if (s.isVariable()) {
        printJava("TokenList[i].add(littoken"+keyword+");\n");
      } else {
        printJava("TokenList["+i+"].add(littoken"+keyword+");\n");
      }
    }
  }
  
  private void generateAccept(OpSyntax s) {
    if (genvisitor) {
      if (!s.isRoot) {
        printJava("  @Override\n");
      }
      printJava("  public <T> T accept(IRNode node, IVisitor<T> visitor) {\n");
      printJava("    return visitor.visit"+s.name+"(node);\n");
      printJava("  }\n\n");
    }
  }
  
  public static final String STD_PREFIX = "edu.cmu.cs.fluid";
  private final String stdPackage = makePackageName(pkgPrefix, STD_SUFFIX);
  
  @Override
  protected void generateForAll() {
    String outPath = computePath(outDir, STD_PREFIX);
    String stdPath = computePath(outPath, STD_SUFFIX);
    
    openPrintStream(stdPath + File.separator + "ILogicallyInvisible.java");
    generateILogicallyInvisible();
    
    openPrintStream(stdPath + File.separator + "IAcceptor.java");
    generateIAcceptor();

    openPrintStream(stdPath + File.separator + "IVisitor.java");
    generateIVisitor();
    
    openPrintStream(stdPath + File.separator + "Visitor.java");
    generateVisitor();
    
    openPrintStream(stdPath + File.separator + "TestVisitor.java");
    generateTestVisitor();
    
    for (String suffix : packagesAppearing()) {
      // Use the last part of the suffix as the name
      String name = "Load"+capitalize(suffix.substring(suffix.lastIndexOf('.')+1));
      String path = computePath(outPath, suffix);
      openPrintStream(path + File.separator + name + ".java");
      generateLoadFile(name, suffix);
    }
  } 

  private void generateILogicallyInvisible() {    
    generatePkgDecl(stdPackage);
    printJava("\n");
    generateClassJavadoc("Implemented by operators that should not appear in the shared AST interfaces", 
                         "(e.g., VariableDeclarators, Arguments), and should be skipped to get the logical parent.");
    printJava("public interface ILogicallyInvisible {\n");
    printJava("}\n");
  }
  
  private void generateIAcceptor() {    
    generatePkgDecl(stdPackage);
    printJava("import edu.cmu.cs.fluid.ir.*;\n\n");
    printJava("public interface IAcceptor {\n");
    printJava("  public <T> T accept(IRNode node, IVisitor<T> visitor);\n");
    printJava("}\n");
  }
  
  private void generateIVisitor() {
    generatePkgDecl(stdPackage);
    printJava("import edu.cmu.cs.fluid.ir.*;\n\n");
    printJava("@SuppressWarnings(\"deprecation\")\n");
    printJava("public interface IVisitor<T> {\n");
    for (Map.Entry<String,OpSyntax> e : iterate()) {
      OpSyntax s = e.getValue();
      printJava("  public T visit"+capitalize(s.name)+"(IRNode node);\n");
    }
    printJava("}\n");
  }
  
  private void generateVisitor() {
    generatePkgDecl(stdPackage);
    printJava("import java.util.*;\n");
    printJava("import com.surelogic.*;\n");
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.parse.JJNode;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n\n");
    printJava("@SuppressWarnings(\"deprecation\")\n");
    printJava("@ThreadSafe(implementationOnly=true)\n");
    printJava("public abstract class Visitor<T> implements IVisitor<T> {\n");
    printJava("  // two useful methods\n");
    printJava("  public T doAccept(IRNode node) {\n");
    printJava("    return ((IAcceptor)JJNode.tree.getOperator(node)).accept(node,this);\n");
    printJava("  }\n\n");
    printJava("  public void doAcceptForChildren(IRNode node) {\n");
    printJava("    Iterator enm = JJNode.tree.children(node);\n");
    printJava("    while (enm.hasNext()) {\n");
    printJava("      doAccept((IRNode)enm.next());\n");
    printJava("    }\n");
    printJava("  }\n\n");
    printJava("  public List<T> doAcceptForChildrenWithResults(IRNode node) {\n");
    printJava("    List<T> results = new ArrayList<T>();\n");
    printJava("    Iterator enm = JJNode.tree.children(node);\n");
    printJava("    while (enm.hasNext()) {\n");
    printJava("      results.add(doAccept((IRNode)enm.next()));\n");
    printJava("    }\n");
    printJava("    return Collections.unmodifiableList(results);\n");
    printJava("  }\n\n");
    printJava("  // method called for any operator without a visit method overridden.\n");
    printJava("  public T visit(IRNode node) { return null; }\n");

    for (Map.Entry<String,OpSyntax> e : iterate()) {
      OpSyntax s = e.getValue();

      printJava("  public T visit"+capitalize(s.name)+"(IRNode node) {\n");
      printJava("    return visit"+capitalize(s.parentOperator)+"(node);\n");
      printJava("  }\n");    
    }
    printJava("}\n");
  }
  
  private void generateTestVisitor() {
    generatePkgDecl(stdPackage);
    printJava("import java.util.*;\n");
    printJava("import edu.cmu.cs.fluid.ir.*;\n");
    printJava("import edu.cmu.cs.fluid.java.*;\n");
    printJava("import edu.cmu.cs.fluid.java.operator.*;\n");
    printJava("import edu.cmu.cs.fluid.java.promise.*;\n");
    printJava("import edu.cmu.cs.fluid.java.bind.*;\n");
    printJava("import edu.cmu.cs.fluid.parse.*;\n");
    printJava("import edu.cmu.cs.fluid.ide.*;\n\n");
    printJava("@SuppressWarnings(\"deprecation\")\n");
    printJava("public class TestVisitor<T> implements IVisitor<T> {\n");
    printJava("  private final IBinder binder;\n");
    printJava("  private final boolean testBindings;\n\n");
    printJava("  public TestVisitor(IBinder b, boolean bind) {\n");
    printJava("    binder       = b;\n");
    printJava("    testBindings = bind;\n");
    printJava("  }\n");
    printJava("  public T doAccept(IRNode node) {\n");
    printJava("    return ((IAcceptor)JJNode.tree.getOperator(node)).accept(node,this);\n");
    printJava("  }\n\n");
    printJava("  // method called for any operator without a visit method overridden.\n");
    printJava("  public T visit(IRNode node) { return null; }\n");
    printJava("\n");  
    for (Map.Entry<String,OpSyntax> e : iterate()) {
      final OpSyntax s  = e.getValue();
      final String type = s.name;
      printJava("  public T visit"+s.name+"(IRNode n) {\n");
      if (turnOnTestOutput) {
        printJava("    StringBuilder sb = new StringBuilder(\""+type+"\");\n");
      } else {
        printJava("    @SuppressWarnings(\"unused\")\n");
      }
      printJava("    IRNode parent = JJNode.tree.getParent(n);\n");       
      if (turnOnTestOutput) {
        printJava("    if (parent != null) {\n");
        printJava("      sb.append(\" \");\n");
        printJava("      sb.append(parent.toString());\n");
        printJava("    }\n");
      }

      final BindingType bindsTo = getBindsToName(s);
      if (bindsTo != null) {
        // try to resolve binding
        printJava("    if (testBindings && binder.getBinding(n) == null) {\n");
        printJava("      throw new NullPointerException(\"binding was null\");\n");
        printJava("    }\n");
      }
      final BindingType bindsToType = getBindsToTypeName(s);
      if (bindsToType != null) {
        // try to resolve type binding
        printJava("    if (testBindings && binder.getJavaType(n) == null) {\n");
        printJava("      throw new NullPointerException(\"type binding was null\");\n");
        printJava("    }\n");
      }
      
      s.generateFromSyntax(typeTable, new ASTSyntaxStrategy(true) {
        @Override
        protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
          String pkg = s.packageName.endsWith("promise")? "JavaPromise." : "JavaNode.";
          if (turnOnTestOutput) {
            printJava("    sb.append(\" \"+"+pkg+getSigForInfo_NoArgs(a)+");\n");
          } else {
            printJava("    "+accessRcvrTable.get(a.type)+"."+getSigForInfo_NoArgs(a)+";\n");
          }
        }
        @Override
        protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
          if (turnOnTestOutput) {
            printJava("    if (JavaNode.getModifiers"+getSigForInfo_WithArgs(arg)+") { sb.append(\" "+arg+"\"); }\n");
          } else {
            printJava("    JavaNode.getModifiers"+getSigForInfo_WithArgs(arg)+";\n");
          }
        }
        @Override
        protected void doForVariableChild(OpSyntax s, int i, Child c) {
//          printJava("    for(IRNode c : "+type+".prototype."+getSigForVariableChild(c)+") {\n");
          printJava("    for(IRNode c : "+type+"."+getSigForVariableChild(c)+") {\n");
          printJava("      doAccept(c);\n");
          printJava("    }\n");
        }
        @Override
        protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
//          printJava("    doAccept("+type+".prototype."+getSigForFixedChildren(c, child)+");\n");
          printJava("    doAccept("+type+"."+getSigForFixedChildren(c, child)+");\n");
        }
        @Override
        protected void doForFixedChild(OpSyntax s, int i, Child c) {
//          printJava("    doAccept("+type+".prototype."+getSigForFixedChild(c)+");\n");
          printJava("    doAccept("+type+"."+getSigForFixedChild(c)+");\n");
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
  
  @Override
  protected String getSigForInfo_NoArgs(Attribute a) {
    return "get"+a.type+"(n)";
  }
  @Override
  protected String getSigForInfo_WithArgs(String arg) {
    return "(n, JavaNode."+arg.toUpperCase()+")";
  }
  @Override
  protected String getSigForVariableChild(Child c) {
    return "get"+capitalize(c.name)+"Iterator(n)";
  }
  @Override
  protected String getSigForFixedChildren(Child c, OpSyntax child) {
    return "get"+capitalize(c.name)+"(n)";
  }
  @Override
  protected String getSigForFixedChild(Child c) {
    return "get"+capitalize(c.name)+"(n)";
  }   
    
  private void generateLoadFile(String name, String selectSuffix) {
    generatePkgDecl("edu.cmu.cs.fluid."+selectSuffix);
    printJava("@SuppressWarnings(\"all\")\n");
    printJava("public class "+name+" {\n");

    for (Map.Entry<String,OpSyntax> e : iterate()) {
      OpSyntax s = e.getValue();
      if (selectSuffix.equals(s.packageName)) {
        printJava("  private static "+s.name+" x"+s.name+" = "+s.name+".prototype;\n");
      }
    }
    printJava("}\n");
  }
}
