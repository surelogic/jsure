package com.surelogic.opgen.generator;

import java.util.*;
import java.util.regex.*;

import com.surelogic.opgen.syntax.*;


/**
 * Base class for generators implementing an AST interface
 * 
 * @author Edwin
 */
public abstract class AbstractASTImplGenerator extends AbstractSharedASTGenerator {
  private enum Binding {
    ANNOTATION("Annotation"),
    VARIABLE("Variable"),
    FUNCTION("Function"),
    CONSTRUCTOR("Constructor"),
    METHOD("Method"),
    PROMISE("Promise"),
    LOCK("Lock"),
    REGION("Region"),
    BINDING("");
    
    final String name;
    Binding(String n) {      
      name = n;
    }
  }
  
  private enum TypeBinding {
    TYPE("Type"),
    VOID("VoidType"),
    PRIM("PrimitiveType"),
    REF("ReferenceType"),
    NULL("NullType"),
    SOURCE("SourceRefType"),
    DECLARED("DeclaredType"),
    FORMAL("TypeFormal"),
    DERIVED("DerivedRefType"),
    ARRAY("ArrayType"),
    WILDCARD("WildcardType"),
    CAPTURE("CaptureType");
    
    final String name;
    TypeBinding(String n) {      
      name = n;
    }
  }
  
  /**
   * Package prefix of the AST interface being implemented
   */
  protected final String astPrefix;

  protected AbstractASTImplGenerator(String astPrefix, String implPrefix) {
    this(astPrefix, implPrefix, STD_SUFFIX);
  }
  
  protected AbstractASTImplGenerator(String astPrefix, String implPrefix, String suffix) {
    super(implPrefix, suffix);
    this.astPrefix = astPrefix+".";
  }
  
  @Override
  protected boolean okToGenerate(OpSyntax s) {
    return okToGenerateImplementation(s);
  }
  
  @Override
  protected void addPackagesUsed(Set<String> used, String suffix) {
    super.addPackagesUsed(used, suffix);
    used.add(makePackageName(astPrefix, suffix));
  }
  
  @Override
  protected Set<String> getPackagesUsed(OpSyntax s) {
    Set<String> ss = super.getPackagesUsed(s);
    ss.add(makePackageName(pkgPrefix, STD_SUFFIX));
    ss.add(makePackageName(astPrefix, s.packageName));
    ss.add(makePackageName(astPrefix, STD_SUFFIX));
    if (getBindsToName(s) != null || 
        isConstructorDeclaration(s) || isMethodDeclaration(s) || 
        getBindsToTypeName(s) != null) {   
      ss.add("com.surelogic.ast");
    }
    return ss;
  }

  @Override
  protected boolean shouldNotGenerate(OpSyntax s, Child c) {
    return c.isAbstract() || super.shouldNotGenerate(s, c);
  }
  
  protected void createAcceptImpl(OpSyntax s, boolean hasAbstractParents) {
    PkgStatus p = pkgMappings.get(s.packageName);
    if (!s.isRoot & !hasAbstractParents) {
      printJava("  @Override\n");
    }
    printJava("  public <T> T accept(INodeVisitor<T> visitor) {\n");
    printJava("    I"+p.getName()+"NodeVisitor<T> v = (I"+p.getName()+"NodeVisitor<T>) visitor;\n");
    printJava("    return v.visit(this);\n");
    printJava("  }\n");
  }

  @Override
  protected void generateBindingExistsBody(OpSyntax s, BindingType val) {
    PkgStatus p = pkgMappings.get(s.packageName);
    printJava(" {\n");
    printJava("    return JavaBinder.get"+p.getName()+"Binder().isResolvable(this);\n");
    printJava("  }\n\n"); 
  }
  
  @Override
  protected void generateResolveBindingBody(OpSyntax s, BindingType val) {
    PkgStatus p = pkgMappings.get(s.packageName);
    printJava(" {\n");
    printJava("    return JavaBinder.get"+p.getName()+"Binder().resolve(this);\n");
    printJava("  }\n\n"); 
  }
  
  @Override
  protected void generateGetBridgeBody(OpSyntax s, String type) {
    printJava(" {\n");
    printJava("    return bridge;\n");
    printJava("  }\n\n"); 
  }
  
  @Override
  protected void generateSetBridgeBody(OpSyntax s, String type) {
    printJava(" {\n");
    printJava("    bridge = b;\n");
    printJava("  }\n\n"); 
  }
  
  @Override
  protected boolean generateGetParent(OpSyntax s) {
    boolean created = false;
    for (String iface : s.superifaces) {
      OpSyntax op = lookupIface(iface);
      if (op != null && op != s) {
        created |= super.generateGetParent(op);
      }
    }
    created |= super.generateGetParent(s, created);
    return created;
  }
  
  @Override
  protected void generateGetParentBody(OpSyntax s, String type) {
    printJava(" {\n");
    if (!simplifyGetParent) {
      printJava("    if (this != here) { throw new IllegalArgumentException(\"Nodes don't match\"); }\n");
    }
  }

  @Override
  protected void generateIsValidFooBody(OpSyntax s, String type, Set<OpSyntax> parents) {
    printJava(" {\n");
    if (simplifyGetParent) {
      printJava("    IJavaOperatorNode parent = this.getParent();\n");
    } else {
      printJava("    IJavaOperatorNode parent = this.getParent((IJavaOperatorNode) this);\n");
    }
    for (OpSyntax p : parents) {
      printJava("    if (parent instanceof "+makeInterfaceName(p.name)+") {\n");
      printJava("      return true;\n");
      printJava("    }\n");
    }
    printJava("    return false;\n");
    printJava("  }\n\n"); 
  }
  
  @Override
  protected void generateGetOverriddenBody() {
    printJava(" {\n");
    printJava("    return JavaBinder.getBinder().getOverriddenMethod(this);\n");
    printJava("  }\n\n"); 
  }
  @Override
  protected void generateGetAllOverriddenBody() {
    printJava(" {\n");
    printJava("    return JavaBinder.getBinder().getAllOverriddenMethods(this);\n");
    printJava("  }\n\n"); 
  }
  @Override
  protected void generateGetSuperConstructorBody() {
    printJava(" {\n");
    printJava("    return JavaBinder.getBinder().getSuperConstructor(this);\n");
    printJava("  }\n\n"); 
  }

  @Override
  protected void generateTypeExistsBody(OpSyntax s) {
    PkgStatus p = pkgMappings.get(s.packageName);
    printJava(" {\n");
    printJava("    return JavaBinder.get"+p.getName()+"Binder().isResolvableToType(this);\n");
    printJava("  }\n\n"); 
  }
  
  @Override
  protected void generateResolveTypeBody(OpSyntax s) {
    PkgStatus p = pkgMappings.get(s.packageName);
    printJava(" {\n");
    printJava("    return JavaBinder.get"+p.getName()+"Binder().resolveType(this);\n");
    printJava("  }\n\n"); 
  }
  
  /*********************************************************************
   *  Standard Java AST code
   *********************************************************************/

  /**
   * Create fields that directly correspond to each child/attribute
   */
  protected final void generateStandardFields(OpSyntax s) {
    final String type = getBridgesToName(s);
    if (type != null) {
      printJava("  "+makeBridgeName(type)+" bridge;\n\n");
    }
    generateStandardFields(s, finalFieldDeclarationsStrategy);
  } 
  
  protected final void generateStandardFields(OpSyntax s, FieldDeclarationsStrategy fds) {
    if (s.isConcrete) {
      printJava("  // Fields\n");
      s.generateFromSyntax(typeTable, fds);
      printJava("\n");
    }
  }
  
  private static final Pattern childMatch = Pattern.compile("%([a-zA-Z][A-Za-z0-9_]+)"); 
  
  /**
   * May generate helper methods need to compute parameters
   * 
   * @author chance
   */
  private class SuperCallParametersStrategy extends VariableNamesStrategy {
    final OpSyntax child;

    protected SuperCallParametersStrategy(ASTPredicate pred, OpSyntax child, String... prefix) {
      super(pred, prefix);
      
      this.child = child;
    }
 
    @Override
    protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
      if (child.hasMatching(a)) {
        // Pass up to parent in the usual way
        names.add(a.name);
      } else {
        Property p = Property.props.get(a.name);
        if (p != null) {
          String val = child.props.get(p);
          if (val != null) {
            names.add(computeParentValue(child, a.name, val));
            return;
          }
        }      
        System.out.println("No attribute '"+a.name+"' in "+child.name+", yet in "+s.name);
        names.add("null");
      }
    }
    
    @Override
    protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax childType) {
      if (child.hasMatching(c)) {
        // Pass up to parent in the usual way
        names.add(c.name); 
      } else if (c.type.startsWith(OpSyntax.OPT_CHILD_PREFIX)) {        
        names.add("java.util.Collections.<"+makeInterfaceName(childType.variableChild.type)+">emptyList()");
      } else {
        System.out.println("No child '"+c.name+"' in "+child.name+", yet in "+s.name);
        names.add("null");
      }
    }
    
    @Override
    protected void doForFixedChild(OpSyntax s, int i, Child c) {
      if (child.hasMatching(c)) {
        // Pass up to parent in the usual way
        names.add(c.name); 
      } else if (c.type.startsWith(OpSyntax.OPT_CHILD_PREFIX)) {
        names.add(createNoFooNode(child, c));
      } else {
        System.out.println("No child '"+c.name+"' in "+child.name+", yet in "+s.name);
        names.add("null");
      }
    }
  }
  
  /**
   * Computes a String value to return
   */
  private String computeParentValue(OpSyntax s, String name, String val) {
    StringBuilder sb = new StringBuilder();
    String rv       = computeParentValue(sb, s, name, val);
    printJava(sb.toString());
    return rv;
  }
      
  private String computeParentValue(StringBuilder buf, OpSyntax s, String name, String val) {
    // Replace any underscores with spaces
    if (val.indexOf('_') >= 0) {
      val = val.replace('_', ' ');
    }
    
    // Replace references to children with computed values
    Matcher m       = childMatch.matcher(val);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String replace = generateComputedChildValue(buf, s, name, m.group(1));
      m.appendReplacement(sb, replace);
    }
    m.appendTail(sb);
    return '\"'+sb.toString()+'\"';
  }
  
  private String generateComputedChildValue(StringBuilder buf, OpSyntax s, String prop, String name) {
    for (Child c : s.children) {
      if (name.equals(c.name)) {
        final boolean isVariable = s.isVariable() && s.variableChild == c;
        OpSyntax ctype = lookup(c.type);
        if (isVariable) {
          return generateComputedValueFromVariableChild(buf, s, prop, name, ctype);
        }
        else if (isLogicallyInvisible(ctype)) {          
          return generateComputedValueFromFixedChildren(buf, s, prop, name, ctype);
        }
        else {
          return generateComputedValueFromFixedChild(buf, s, prop, name, ctype);
        }
      }
    }
    return "";
  }
  
  protected boolean computeValueFromField() {
    return false;
  }

  private String generateComputedValueFromFixedChildren(StringBuilder buf, OpSyntax s, String prop, String name, OpSyntax ctype) {
    OpSyntax varOp = lookup(ctype.variableChild.type);
    return generateComputedValueFromVariableChild(buf, s, prop, name, varOp);
  }

  private String generateComputedValueFromFixedChild(StringBuilder buf, OpSyntax s, String prop, String name, OpSyntax ctype) {
    if (computeValueFromField()) {
      return "\"+"+name+".get"+capitalize(prop)+"()+\"";
    }
    final Child namedChild = s.findMatching(name);
    return "\"+"+getSigForFixedChild(namedChild)+".get"+capitalize(prop)+"()+\"";
  }

  private String generateComputedValueFromVariableChild(StringBuilder buf, OpSyntax s, String prop, String name, OpSyntax ctype) {
    // Side-effect: generate helper method
    final String mName = "compute"+capitalize(prop)+"List";
    final String iface = makeInterfaceName(ctype.name);
    buf.append("  // Helper method needed for call to super-constructor\n");
    buf.append("  private static String "+mName+"(List<"+iface+"> l) {\n"); 
    buf.append("    StringBuilder sb = new StringBuilder();\n");
    buf.append("    boolean first = true;\n");
    buf.append("    for ("+iface+" n : l) {\n");
    buf.append("      String s = n.get"+capitalize(prop)+"();\n");
    buf.append("      if (\"\".equals(s)) { continue; }\n");
    buf.append("      if (first) {\n");
    buf.append("        first = false;\n");
    buf.append("        sb.append(s);\n");
    buf.append("      } else {\n");
    buf.append("        sb.append(\", \");\n");
    buf.append("        sb.append(s);\n");
    buf.append("      }\n");
    buf.append("    }\n");
    buf.append("    return sb.toString();\n");
    buf.append("  }\n");
    if (computeValueFromField()) {
      return "\"+"+mName+"("+name+")+\"";
    }
    final Child namedChild = s.findMatching(name);
    return "\"+"+mName+"("+getSigForVariableChild(namedChild)+")+\"";
  }
  
  protected final String makeNoFooNode(String optName) {
    return makeNodeName("No"+optName.substring(OpSyntax.OPT_CHILD_PREFIX.length()));
  }
  
  protected String createNoFooNode(OpSyntax s, Child c) {
    if (convertOptNodes) {
      return "null";
    }
    return "new "+makeNoFooNode(c.type)+"()";
  }
  
  protected final VariableNamesStrategy pregenerateActualsForSuperCall(ASTPredicate p, OpSyntax parent, OpSyntax child, String[] prefixFormals) {
    final VariableNamesStrategy names = new SuperCallParametersStrategy(p, child, computeFormalNames(prefixFormals));
    parent.generateFromSyntax(typeTable, names); 
    return names;
  }
  
  protected final void generateCall(final VariableNamesStrategy names, final String prefix) {
    final String spaces = getSpaces(prefix.length());
    
    printJava(prefix);
    // Moved to pregenerateFoo()
    //
    // s.generateFromSyntax(typeTable, names); 
    printNames(names, spaces);
    printJava(");\n");
  }
  
  /**
   * Print a sequence of names separated by commas
   */
  protected final void printNames(final Iterable<String> names, final String spaces) {
    int len       = 0;
    boolean first = true;
    for (String arg : names) {
      len += arg.length();

      if (first) {
        first = false;
      } 
      else if (len > 60) {
        printJava(",\n");
        printJava(spaces);
        len = 0;  
      } 
      else {
        printJava(", ");
      }
      printJava(arg);
    }
  }
  
  /**
   * 
   * @param formals Assumed to be the form of "Type name"
   */
  protected String[] computeFormalNames(String[] formals) {
    if (formals.length == 0) {
      return noStrings;
    }
    String[] names = new String[formals.length]; 
    for (int i=0; i<formals.length;i++) {
      names[i] = formals[i].substring(formals[i].indexOf(' ')+1);
    }
    return names;
  }
  
  /**
   * Generate Javadoc and return type for the standard constructor
   */
  private void generateStandardConstructorIntro(final String returnSig) {
    printJava("  // Constructors\n");
    generateClassBodyDeclJavadoc("Lists passed in as arguments must be @unique");
    printJava(returnSig);
  }
  
  protected void generateStandardConstructor(OpSyntax s, final String rootType, String... prefixFormals) {
    final String returnSig = "  public "+makeNodeName(s.name)+"(";
    
    if (!s.isRoot) { // Has a parent, so we need a super call
      OpSyntax parent   = lookup(s.parentOperator);
      ASTPredicate pred = isAbstractChild;
      OpSyntax base;
      
      if (s.isConcrete) { // We have our own set of parameters
        base = s;
      } 
      else if (parent.isConcrete) { // We duplicate our parent's parameters
        base = parent;
      }      
      else {
        base = s;
        pred = alwaysTrue; // Skip all syntax
      }
      // Needs to be first to generate helper methods, if any
      final VariableNamesStrategy names = pregenerateActualsForSuperCall(isAbstractChild, parent, base, prefixFormals);
      generateStandardConstructorIntro(returnSig);
      base.generateFromSyntax(typeTable, new ConstructorFormalsStrategy(pred, returnSig, prefixFormals));
      printJava(") {\n");
      /*
       s.generateFromSyntax(typeTable, variableNamesStrategy);
       for (String var : variableNamesStrategy) {
       printJava("    this."+var+" = "+var+";\n");
       }
       */
      generateCall(names, "    super(");
    } else if (prefixFormals.length > 0) { 
      // Needs to be first to generate helper methods, if any
      final VariableNamesStrategy names = pregenerateActualsForSuperCall(alwaysTrue, s, s, prefixFormals);
      generateStandardConstructorIntro(returnSig);
      
      // The default parent requires some set parameters
      s.generateFromSyntax(typeTable, new ConstructorFormalsStrategy(isAbstractChild, returnSig, prefixFormals));
      printJava(") {\n");
      generateCall(names, "    super(");
    } else {
      generateStandardConstructorIntro(returnSig);
      // FIX what if the default parent requires parameters!
      s.generateFromSyntax(typeTable, new ConstructorFormalsStrategy(isAbstractChild, returnSig));
      printJava(") {\n");
    }
    if (s.isConcrete) {
      s.generateFromSyntax(typeTable, new ASTSyntaxStrategy(true) {
        private void printNullCheck(String name) {
          printJava("    if ("+name+" == null) { throw new IllegalArgumentException(\""+name+" is null\"); }\n");
        }
        private void printSetParents(String name, String type) {  
          printJava("    for ("+type+" _c : "+name+") {\n");
          printJava("      (("+rootType+") _c).setParent(this);\n");
          printJava("    }\n");
        }
        @Override
        protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
          String lower = type.toLowerCase();
          // Assume that reference types are capitalized
          if (!a.isAbstract() && !type.equals(lower)) {
            printNullCheck(a.name);            
          }
          printJava("    this."+a.name+" = "+a.name+";\n");
        }
        @Override
        protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
          final String name = computeArgAttrName(arg);
          printJava("    this."+name+" = "+name+";\n");
        }
        @Override
        protected void doForVariableChild(OpSyntax s, int i, Child c) {
          printNullCheck(c.name);
          printSetParents(c.name, makeInterfaceName(c.type));
          printJava("    this."+c.name+" = Collections.unmodifiableList("+c.name+");\n");
        }
        @Override
        protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
          if (convertOptNodes && couldBeNullVariant(c)) {
            // could be null
            printJava("    if ("+c.name+" != null) {\n");
            printSetParents(c.name, makeInterfaceName(child.variableChild.type));
            printJava("      this."+c.name+" = Collections.unmodifiableList("+c.name+");\n");
            printJava("    } else {\n");
            printJava("      this."+c.name+" = null;\n");
            printJava("    }\n");
          } else {
            printNullCheck(c.name);
            printSetParents(c.name, makeInterfaceName(child.variableChild.type));
            printJava("    this."+c.name+" = Collections.unmodifiableList("+c.name+");\n");
          }
        }
        @Override
        protected void doForFixedChild(OpSyntax s, int i, Child c) {
          if (convertOptNodes && couldBeNullVariant(c)) {
            // could be null
            printJava("    if ("+c.name+" != null) {\n");
            printJava("      (("+rootType+") "+c.name+").setParent(this);\n");
            printJava("    }\n");
          } else {
            printNullCheck(c.name);
            printJava("    (("+rootType+") "+c.name+").setParent(this);\n");
          }
          printJava("    this."+c.name+" = "+c.name+";\n");
        }
      });
    }
    printJava("  }\n\n");
  }

  protected void generateStandardAccessors(OpSyntax s) {
    s.generateFromSyntax(typeTable, new MethodSignatureStrategy(true, false) {      
      final String start  = "    return ";
      final String ending = ";\n  }\n";
      @Override
      protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
        super.doForInfo_NoArgs(s, i, a, type);
        printJava(start + a.name);
        printJava(ending);
      }
      @Override
      protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
        super.doForInfo_WithArgs(s, i, a, type, arg);
        printJava(start + computeArgAttrName(arg));
        printJava(ending);
      }
      @Override
      protected void doForVariableChild(OpSyntax s, int i, Child c) {
        super.doForVariableChild(s, i, c);
        printJava(start + c.name);
        printJava(ending);
      }
      @Override
      protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
        super.doForFixedChildren(s, i, c, child);
        printJava(start + c.name);
        printJava(ending);
      }
      @Override
      protected void doForFixedChild(OpSyntax s, int i, Child c) {
        super.doForFixedChild(s, i, c);
        printJava(start + c.name);
        printJava(ending);
      }      
    });
  }

  class AlreadyExistsPredicate extends ASTPredicate {
    final OpSyntax s;
    AlreadyExistsPredicate(OpSyntax s) {
      super(false);
      this.s = s;
    }
    @Override
    protected boolean eval(OpSyntax s, Attribute a) {
      return this.s.hasMatching(a);
    }
    @Override
    protected boolean eval(OpSyntax s, Child c) {
      return this.s.hasMatching(c);
    }
  }
  
  /**
   * Generate needed methods that delegate to subtree nodes
   */
  protected void generateStandardDelegateMethods(final OpSyntax child) {
    final OpSyntax parent             = lookup(child.parentOperator);
    if (parent == null) {
      return;
    }
    final ASTPredicate existsInS      = new AlreadyExistsPredicate(child);    
    final MethodSignatureStrategy mss = new MethodSignatureStrategy(false, false); 
    final StringBuilder sb             = new StringBuilder();
    
    // Do something for each syntax element in the parent, but not in the child
    generateMethodsNeededByParents(child, new ASTSyntaxStrategy(existsInS) {
      @Override
      protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
        if (parentOperatorHandled() && s == parent) {
          return;
        }
        // System.out.println("Found "+a.name+" missing in "+child.name);
        mss.doForInfo_NoArgs(child, i, a, type);
        if ("String".equals(type)) {
          String rv = "\"\"";
          Property p = Property.props.get(a.name);
          if (p != null) {
            String val = child.props.get(p);
            if (val != null) {
              rv = computeParentValue(sb, child, a.name, val);
            }
          }  
          printJava("    return "+rv);
        } else {
          printJava("    return null");
        }
        printJava(";\n  }\n");
      }
      @Override
      protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
    	  // Nothing to do
      }
      @Override
      protected void doForVariableChild(OpSyntax s, int i, Child c) {
    	  // Nothing to do
      }
      @Override
      protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax cOp) {
        if (c.isAbstract()) {
          Property p = Property.props.get(c.name);
          if (p != null) {
            // Check if there's a property defined in the child
            String val = child.props.get(p);            
            if (val != null) {
              Child delegate = child.findMatching(val);
              if (delegate != null) {
                OpSyntax delegateOp = lookup(delegate.type);
                if (delegateOp != null) {
                  Child match = delegateOp.findMatching(c.name);
                  if (match != null) {
                    // generate signature
                    mss.doForFixedChildren(s, i, c, cOp);                     
                    // generate body                
                    printJava("    return "+getSigForFixedChild(delegate)+"."+getSigForFixedChildren(match, null)+";\n");
                    printJava("  }\n");
                  }
                }
              } else { // Nothing to delegate to
                // generate signature
                mss.doForFixedChildren(s, i, c, cOp);                     
                // generate body                
                printJava("    return "+val+";\n");
                printJava("  }\n");
              }
            } else if (convertOptNodes && couldBeNullVariant(c)) {
              // generate signature
              mss.doForFixedChildren(s, i, c, cOp);       
              // generate body                
              printJava("    return Collections.emptyList();\n");
              printJava("  }\n");
            }
          }
        }
      }
      @Override
      protected void doForFixedChild(OpSyntax s, int i, Child c) {
        if (convertOptNodes && couldBeNullVariant(c)) {
          // generate signature
          mss.doForFixedChild(s, i, c);       
          // generate body                
          printJava("    return null;\n");
          printJava("  }\n");
        }          
      }
    });
    
    if (sb.length() > 0) {
      printJava(sb.toString());
    }
  }
  
  
  
  /**
   * Generate methods required by superops, not
   * including the standard parent
   * 
   * @param s
   */
  protected void generateMethodsNeededByParents(final OpSyntax s, ASTSyntaxStrategy strategy) {
    //final OpSyntax p = lookup(s.parentOperator);
    for (OpSyntax parent: getSuperOps(s)) {
      parent.generateFromSyntax(typeTable, strategy);
    }
  }
  
  protected boolean parentOperatorHandled() {
    return false;
  }

  protected void generateStandardGetParentBody(OpSyntax s, String type) { 
    printJava("    return ("+type+") parent;\n");
    printJava("  }\n\n"); 
  }
  
  /*********************************************************************
   *  Code for implementing bindings
   *********************************************************************/
  
  @Override
  protected void generateMethodsSelectively(final OpSyntax s) {
    super.generateMethodsSelectively(s);
    generateMethodsNeededByBinding(s);
  }
  
  private Binding getBinding(String name) {
    for (Binding b : Binding.values()) {
      if (b.name.equals(name)) {
        return b;
      }
    }
    return null;
  }
  
  private TypeBinding getTypeBinding(String name) {
    for (TypeBinding b : TypeBinding.values()) {
      if (b.name.equals(name)) {
        return b;
      }
    }
    return null;
  }
  
  private void handleBinding(OpSyntax s, Binding b) {
    printJava("  public "+makeInterfaceName(s.name)+" getNode() { return this; }\n");
    
    switch (b) {
    case BINDING:
    case PROMISE:
    case LOCK:
    case REGION:
      return;
    case ANNOTATION:
      printJava("  public String getName() { return getId(); }\n");
      return;
    case VARIABLE:
    case FUNCTION:
    case CONSTRUCTOR:
    case METHOD:
      final String typeB = makeTypeName("Type");
      printJava("  public "+makeTypeName("Declared")+" getContextType() { return null; }  // FIX \n");
      if (!s.isRoot && !hasAbstractParents(s)) {
        printJava("  @Override\n");
      }
      printJava("  public "+typeB+" convertType("+typeB+" t) { return t; }\n");
      return;
    }
  }
  
  private void handleTypeBinding(OpSyntax s, TypeBinding b) {
    printJava("  public "+makeInterfaceName(s.name)+" getNode() { return this; }\n");
    
    switch (b) {    
    case FORMAL:
      printJava("  public "+makeTypeName("SourceRef")+" getExtendsBound() { return JavaBinder.getBaseBinder().resolveExtendsBound(this); }\n");
      generateCommonBindingMethods();
      return;
    case DECLARED: 
      generateCommonBindingMethods();
      return;
    case SOURCE:    
      generateCommonBindingMethods();
      return;
    case TYPE:
    case VOID:
    case PRIM:
    case REF:
    case NULL:
    case DERIVED:
    case ARRAY: 
    case WILDCARD:
    case CAPTURE:
    }
  }

  private void generateCommonBindingMethods() {
    printJava("  public String getName() { return getId(); }\n");
    printJava("  public boolean isSubtypeOf(IType t) { return JavaBinder.getBinder().isSubtypeOf(this, t); }\n");
    printJava("  public boolean isCastCompatibleTo(IType t) { return JavaBinder.getBinder().isCastCompatibleTo(this, t); }\n");
    printJava("  public boolean isAssignmentCompatibleTo(IType t) { return JavaBinder.getBinder().isAssignmentCompatibleTo(this, t); }\n");
  }
  
  protected void generateMethodsNeededByBinding(OpSyntax s) {
    String name    = s.props.get(KnownProperty.BINDING);

    if (name != null) {
      Binding b   = getBinding(name);
      if (b != null) {
        handleBinding(s, b);
      }
      else {
        TypeBinding tb = getTypeBinding(name);
        if (tb != null) {
          handleTypeBinding(s, tb);
        } else {
          System.err.println("Couldn't find binding: "+name);
        }
      }
    }
    else { // check for type
      name = getTypeBindingName(s);
      if (name == null) {
        return;
      }
      TypeBinding b = getTypeBinding(name);
      if (b != null) {
        handleTypeBinding(s, b);
      } else {
        System.err.println("Couldn't find type binding: "+name);
      }
    }
  }
}
