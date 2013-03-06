package com.surelogic.opgen.generator;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.surelogic.opgen.syntax.*;


public abstract class AbstractASTGenerator extends AbstractGenerator {
  protected static final String[] NO_DOCS = {};
  protected static final String JAVA_PROMISE = "java.promise";
  protected static final String STD_SUFFIX = "java.operator";
  public static final String[] bindings = {
      "Variable", "Method", "Constructor", "Lock", "Region", "Annotation"    
    };
  /**
   * Used to figure out what kind of name this is
   */
  public static final Pattern nameTypeMatch = Pattern.compile("^([A-Z][a-z0-9_]+)Name,?$");
  /**
   * Used to figure out what kind of call this is
   */
  public static final Pattern callTypeMatch = Pattern.compile("^([A-Z][a-z0-9_]+)Call,?$");
  protected static final String methodEnding = ";\n\n";
  public static final boolean turnOnTestOutput = false;
  
  /**
   * If true, getParent() only exists in the root node(s)
   */
  protected static final boolean noGetParent = true;
  
  /**
   * If true, getParent() should respect subtyping of the return type
   */
  protected static final boolean simplifyGetParent = noGetParent;
  
  /**
   * If true, children of type OptFoo are considered the same as Foo,
   * and children of type NoFoo are converted into null
   */
  protected static final boolean convertOptNodes = true;
  
  /**
   * If true, the Op attribute is translated to AssignOperator
   */
  protected static final boolean translateToAssignOperator = true;
  
  protected final String pkgPrefix;
  protected final String pkgSuffix;
  /**
   * Same as above, except with pathSeparator substituting for '.'
   */
  protected final String pathPrefix; 
  
  protected AbstractASTGenerator() { 
    pkgPrefix = null;
    pathPrefix = null;
    pkgSuffix = null;
  }
  
  protected AbstractASTGenerator(String prefix) {
    this(prefix, STD_SUFFIX);
  }
  
  protected AbstractASTGenerator(String prefix, String suffix) {
    pkgPrefix = prefix+".";
    pathPrefix = pkgPrefix.replace('.', File.separatorChar);
    pkgSuffix = suffix;
  }
  
  protected boolean javadocImplementationDetails() {
    return true;
  }
  
  protected abstract String makeNodeName(String name);
  
  @Override
  protected final String makeInterfaceName(String name) {  
    return "I"+name+"Node";
  }
  
  protected String makeConvertedInterfaceName(String name) {
    if (convertOptNodes) {
      String variant = getNonnullVariant(name);
      return makeInterfaceName(variant);
    }
    return makeInterfaceName(name);
  }
  
  /*********************************************************************
   *  Code for suppressing code generation
   *********************************************************************/
  
  protected final boolean okToGenerateInterface(OpSyntax s) {
    if (s == null) {
      return false;
    }
    String val1 = s.props.get(KnownProperty.NONCANONICAL);
    String val2 = s.props.get(KnownProperty.NO_IFACE);
    //String val3 = s.props.get(KnownProperty.NONNULL_VARIANTS);
    String val4 = s.props.get(KnownProperty.NULL_VARIANT);
    
    // FIX Does this make sense when I actually implement it? 
    String val5 = s.props.get(KnownProperty.LOGICALLY_INVISIBLE);
    return (val1 == null || val1.equals("false")) && 
           (val2 == null || val2.equals("false")) &&
           //(val3 == null) &&
           (val4 == null) &&
           (val5 == null) &&
           pkgMappings.containsKey(s.packageName);
  }
  
  protected final boolean okToGenerateImplementation(OpSyntax s) {
    if (s == null) {
      return false;
    }
    if (okToGenerateInterface(s)) {
      String val = s.props.get(KnownProperty.NO_IMPL);
      return val == null || val.equals("false");
    }
    return false; // can't generate impl w/o iface
  }
  
  @Override
  protected final boolean hasExtendedPath(OpSyntax s) {
    return pkgPrefix != null || super.hasExtendedPath(s);
  }
  @Override
  protected final String makeExtendedPath(OpSyntax s) {
    String suffix = super.makeExtendedPath(s);
    if (pkgPrefix != null) {
      return pathPrefix + suffix;
    } else {
      return suffix;
    }
  }
  
  protected static final String makePackageName(String prefix, String packageName) {
    if (prefix != null) {      
      if (!prefix.endsWith(".")) {
        return prefix + '.' + packageName;
      }
      return prefix + packageName;
    } else {
      return packageName;
    }
  }
  
  @Override
  protected final void generateEach(OpSyntax s) {
    try {
      if (s.packageName != null) {
        generatePkgDecl(makePackageName(pkgPrefix, s.packageName));
      }
      generateIntro(s);
      generateFields(s);
      generateMethods(s);
      generateEnding(s);
    } catch (Throwable e) {
      IllegalArgumentException iae = new IllegalArgumentException(e.getClass().getCanonicalName()+" '"+e.getMessage()+"' while processing "+s.name);
      iae.setStackTrace(e.getStackTrace());
      throw iae;
    }
  }

  /**
   * Generate the imports and initial part of the type declaration
   */
  protected abstract void generateIntro(OpSyntax s);
  protected abstract void generateFields(OpSyntax s);
  protected abstract void generateMethods(OpSyntax s);
  
  protected void generateEnding(OpSyntax s) {
    printJava("}\n\n");
  }
  
  /*********************************************************************
   *  AST Generator
   *********************************************************************/
  
  protected boolean shouldNotGenerate(OpSyntax s, Attribute a) {
    return existsInParent(s, a);
  }
  
  private boolean existsInParent(OpSyntax s, Attribute a) {
    if (!s.isRoot) {
      OpSyntax parent = lookup(s.parentOperator);
      if (parent.hasMatching(a)) {
        // No need to add it again to the interface, since the parent has it
        return true;
      }
    }
    return false;
  }
   
  protected boolean shouldNotGenerate(OpSyntax s, Child c) {
    return existsInParent(s, c);
  }
  
  private boolean existsInParent(OpSyntax s, Child c) {
    if (!s.isRoot) {
      OpSyntax parent = lookup(s.parentOperator);
      Child c2 = parent.findMatching(c);
      if (c2 != null && !c2.isAbstract()) {
        // No need to add it again to the interface, since the parent has it
        return true;
      }
    }
    return false;
  }
  
  /**
   * @return true if s or its ancestors has a variable child or fixed children
   */
  public boolean hasVariableChildren(OpSyntax s) {
    return hasVariableChildren(s, true);
  }
  
  public boolean hasVariableChildren(OpSyntax s, boolean skip) {
    if (s.isConcrete) {
      return thisHasVariableChildren(s, skip);
    }
    if (!s.isRoot) { 
      OpSyntax parent = lookup(s.parentOperator);
      return hasVariableChildren(parent, skip);
    }
    return false;
  }
  
  /**
   * @return true if s has a variable child or fixed children
   */
  private boolean thisHasVariableChildren(OpSyntax s, boolean skip) {
    HasVariableChildrenStrategy hfcs = new HasVariableChildrenStrategy(skip);
    s.generateFromSyntax(typeTable, hfcs);
    return hfcs.hasVariableChildren;
  }
  
  protected class HasVariableChildrenStrategy extends ASTSyntaxStrategy {
    boolean hasVariableChildren = false;
    public HasVariableChildrenStrategy(boolean skip) {
      super(skip);
    }
    @Override
    protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
  	  // Nothing to do
    }
    @Override
    protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
  	  // Nothing to do
    }
    @Override
    protected void doForVariableChild(OpSyntax s, int i, Child c) {
      hasVariableChildren = true;
    }
    @Override
    protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
      hasVariableChildren = true;
    }
    @Override
    protected void doForFixedChild(OpSyntax s, int i, Child c) {
  	  // Nothing to do
    }
  }
  /*
  protected final String makePackageName(OpSyntax s) {
    if (s == null) {
      new Throwable().printStackTrace(System.err);
      return "edu.cmu.cs.fluid.java.operator"; 
    }    
    return makePackageName(s.packageName);
  }
  
  protected final String makePackageName(String packageName) {
    return makePackageName(pkgPrefix, packageName);
  }
  */
  
  protected Set<String> getPackagesUsed(OpSyntax s) {
    GetPackagesUsedStrategy gpus = new GetPackagesUsedStrategy();
    s.generateFromSyntax(typeTable, gpus);

    OpSyntax parent = lookup(s.parentOperator);
    if (parent != null) {
      gpus.addPackage(parent);
    } else {
      gpus.addPackage(pkgSuffix);
    }
    return gpus.packagesUsed;
  }
  
  protected void addPackagesUsed(Set<String> used, String suffix) {
    used.add(makePackageName(pkgPrefix, suffix));
  }
  
  protected class GetPackagesUsedStrategy extends ASTSyntaxStrategy {
    Set<String> packagesUsed = new HashSet<String>();
    public GetPackagesUsedStrategy() {
      super(true);
    }
    void addPackage(String suffix) {
      addPackagesUsed(packagesUsed, suffix);
    }
    final void addPackage(OpSyntax s) {
      if (s == null) {
        //new Throwable().printStackTrace(System.err);
        addPackage("java.operator"); 
      } else {
        addPackage(s.packageName);
      }
    }
    @Override
    protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
  	  // Nothing to do
    }
    @Override
    protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
  	  // Nothing to do
    }
    @Override
    protected void doForVariableChild(OpSyntax s, int i, Child c) {
      addPackage(lookup(c.type));
    }
    @Override
    protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
      if (child != null && isLogicallyInvisible(child)) {
        addPackage(lookup(child.variableChild.type));
      } else {
        addPackage(child);
      }
    }
    @Override
    protected void doForFixedChild(OpSyntax s, int i, Child c) {
      addPackage(lookup(c.type));
    }
  }
  
  /*********************************************************************
   *  ASTSyntaxStrategy
   *********************************************************************/
  
  protected String computeArgAttrName(String arg) {
    return "is"+capitalize(arg);
  }
  
  protected String computeFixedChildType(Child c) {
    return makeConvertedInterfaceName(c.type);
  }
  protected String computeVariableChildType(Child c) {
    return "List<"+computeFixedChildType(c)+">";
  }
  protected String computeFixedChildrenType(OpSyntax child) {
    return computeVariableChildType(child.variableChild);
  }
  /**
   * Returns whether this generator ignores the args on attributes
   */
  protected boolean ignoreArgs() {
    return false;
  }
  
  protected class ASTPredicate {
    private final boolean val;
    protected ASTPredicate(boolean rv) {
      val = rv;
    }
    protected boolean eval(OpSyntax s, Attribute a) {
      return val;
    }
    protected boolean eval(OpSyntax s, Child c) {
      return val;
    }
  }
  protected final ASTPredicate alwaysTrue = new ASTPredicate(true);
  protected final ASTPredicate alwaysFalse = new ASTPredicate(false);
  protected final ASTPredicate isAbstractChild = new ASTPredicate(false) {
    @Override
    protected boolean eval(OpSyntax s, Child c) {
      return c.isAbstract();      
    }
  };
  
  protected final ASTPredicate askShouldNotGenerate = new ASTPredicate(false) {
    @Override
    protected boolean eval(OpSyntax s, Attribute a) {
      return shouldNotGenerate(s, a);
    }
    @Override
    protected boolean eval(OpSyntax s, Child c) {
      return shouldNotGenerate(s, c);
    }
  };
  
  protected abstract class ASTSyntaxStrategy extends SyntaxStrategy {
    final ASTPredicate predicate;
    public ASTSyntaxStrategy(boolean skip) {
      predicate = skip? askShouldNotGenerate : alwaysFalse;
    }
    public ASTSyntaxStrategy(ASTPredicate skipPred) {
      predicate = skipPred;      
    }
    @Override
    public final void doForInfo(OpSyntax s, int i, Attribute a, String type) {
      if (predicate.eval(s, a)) {
        return;
      }
      if (ignoreArgs() || a.args.isEmpty()) {
        doForInfo_NoArgs(s, i, a, type);
      } else {       
        for (String arg : a.args) {
          doForInfo_WithArgs(s, i, a, type, arg);
        }
      }
    }
    
    protected abstract void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type);
    protected abstract void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg);
    
    @Override
    public void doForChild(OpSyntax s, int i, Child c, boolean isVariable) {
      if (predicate.eval(s, c)) {
        return;
      }
      if (isVariable) {
        doForVariableChild(s, i, c);
      } else {
        OpSyntax child = lookup(c.type);      
        if (child == null) {
          System.err.println("Error: couldn't find operator "+c.type);
        }
        else if (isLogicallyInvisible(child)) {    
          doForFixedChildren(s, i, c, child);
          return;
        }
        else if (shouldBeLogicallyInvisible(child)) {
          System.err.println("Warning: operator "+c.type+" seems plural, but does not have variable children");
        }
        doForFixedChild(s, i, c);
      }
    }

    protected abstract void doForVariableChild(OpSyntax s, int i, Child c);
    protected abstract void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child);
    protected abstract void doForFixedChild(OpSyntax s, int i, Child c);
  }
  
  /*********************************************************************
   *  MethodSignatureStrategy
   *********************************************************************/
  
  protected String getSigForInfo_NoArgs(Attribute a) {
    return "get"+capitalize(a.name)+"()";
  }
  protected String getSigForInfo_WithArgs(String arg) {
    return "is"+capitalize(arg)+"()";
  }
  protected String getSigForVariableChild(Child c) {
    return "get"+capitalize(c.name)+"List()";
  }
  protected String getSigForFixedChildren(Child c, OpSyntax child) {
    return "get"+capitalize(c.name)+"List()";
  }
  protected String getSigForFixedChild(Child c) {
    return "get"+capitalize(c.name)+"()";
  }   
  

  private String getCommentFromProps(OpSyntax s, String name) {
    String comment = s.props.get(Property.props.get(name));
    if (comment != null && comment.indexOf('%') < 0) {
      return comment.replace('_', ' ');
    }
    return null;
  }

  protected String[] getCommentForInfo_NoArgs(OpSyntax s, Attribute a, String type) {
    List<String> l = new ArrayList<String>();
    l.add(getCommentFromProps(s, a.name));
    if (a.isAbstract()) {
      l.add("@return A possibly-null "+type);
    } else {
      l.add("@return A non-null "+type);
    }
    return l.toArray(noStrings);
  }
  
  protected String getCommentForVariableChild(OpSyntax s, Child c) {
    return "@return A non-null, but possibly empty list of nodes";
  }
  protected String getCommentForFixedChildren(OpSyntax s, Child c, OpSyntax child) {
    return "@return A non-null, but possibly empty list of nodes";
  }
  protected String getCommentForFixedChild(OpSyntax s, Child c) {
    if (convertOptNodes) {
      if (couldBeNullVariant(c)) {
        return "@return A node, or null";
      }
    }
    return "@return A non-null node";
  }
  
  protected class MethodSignatureStrategy extends ASTSyntaxStrategy {
    final String ending;
    public MethodSignatureStrategy(boolean skip, boolean isInterface) {
      this(skip? askShouldNotGenerate : alwaysFalse, isInterface);
    }
    
    public MethodSignatureStrategy(ASTPredicate skip, boolean isInterface) {
      super(skip);
      ending = isInterface? ";\n" : " {\n";
    }
    
    @Override
    protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
      generateClassBodyDeclJavadoc(getCommentForInfo_NoArgs(s, a, type));
      printJava("  public "+type+" "+getSigForInfo_NoArgs(a)+ending);
    }
    @Override
    protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
      printJava("  public boolean "+getSigForInfo_WithArgs(arg)+ending);
    }
    @Override
    protected void doForVariableChild(OpSyntax s, int i, Child c) {
      generateClassBodyDeclJavadoc(getCommentForVariableChild(s, c));
      printJava("  public "+computeVariableChildType(c)+" "+getSigForVariableChild(c)+ending);
    }
    @Override
    protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
      generateClassBodyDeclJavadoc(getCommentForFixedChildren(s, c, child));
      printJava("  public "+computeFixedChildrenType(child)+" "+getSigForFixedChildren(c, child)+ending);
    }
    @Override
    protected void doForFixedChild(OpSyntax s, int i, Child c) {
      generateClassBodyDeclJavadoc(getCommentForFixedChild(s, c));
      printJava("  public "+computeFixedChildType(c)+" "+getSigForFixedChild(c)+ending);
    }    
  }
  
  /*********************************************************************
   *  VariableNamesStrategy
   *********************************************************************/
  
  protected class VariableNamesStrategy extends ASTSyntaxStrategy implements Iterable<String> {
    protected List<String> names = Collections.emptyList();
    final String[] prefix;
    
    public VariableNamesStrategy(boolean skip, String... prefix) {
      super(skip);
      this.prefix = prefix;
    }
    protected VariableNamesStrategy(ASTPredicate skipPred, String[] prefix) {
      super(skipPred);
      this.prefix = prefix;
    }
    @Override
    protected void init() {
      names = new ArrayList<String>();
      for (String pre : prefix) {
        names.add(pre);
      }
    }    
    @Override
    protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
      names.add(a.name);
    }
    @Override
    protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
      names.add(computeArgAttrName(arg));
    }
    @Override
    protected void doForVariableChild(OpSyntax s, int i, Child c) {
      names.add(c.name);
    }
    @Override
    protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
      names.add(c.name);
    }
    @Override
    protected void doForFixedChild(OpSyntax s, int i, Child c) {
      names.add(c.name);
    }
    @Override
    public Iterator<String> iterator() {
      List<String> l = names;
      names = Collections.emptyList();
      return l.iterator();
    }
  }
  
  protected final VariableNamesStrategy variableNamesStrategy = new VariableNamesStrategy(false);
  
  /*********************************************************************
   *  VariableTypesStrategy
   *********************************************************************/
  
  protected class VariableTypesStrategy extends ASTSyntaxStrategy {
    public VariableTypesStrategy(boolean skip) {
      super(skip);
    }
    public VariableTypesStrategy(ASTPredicate skipPred) {
      super(skipPred);
    }

    @Override
    public void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
      printJava(type);
    }
    @Override
    public void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
      printJava("boolean");
    }
    @Override
    public void doForVariableChild(OpSyntax s, int i, Child c) {
      printJava(computeVariableChildType(c));
    }
    @Override
    public void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
      printJava(computeFixedChildrenType(child));
    }
    @Override
    public void doForFixedChild(OpSyntax s, int i, Child c) {
      printJava(computeFixedChildType(c));
    }
  }
  
  protected final VariableTypesStrategy variableTypesStrategy = new VariableTypesStrategy(false);
  
  /*********************************************************************
   *  VariableDeclarationsStrategy
   *********************************************************************/
  
  protected abstract class VariableDeclarationsStrategy extends ASTSyntaxStrategy {
    public VariableDeclarationsStrategy(boolean skip) {
      super(skip);
    }
    public VariableDeclarationsStrategy(ASTPredicate skipPred) {
      super(skipPred);
    }
    
    protected abstract void doBefore();
    protected abstract void doAfter();

    @Override
    protected void doForInfo_NoArgs(OpSyntax s, int i, Attribute a, String type) {
      doBefore();
      printJava(type+" "+a.name);
      doAfter();
    }
    @Override
    protected void doForInfo_WithArgs(OpSyntax s, int i, Attribute a, String type, String arg) {
      doBefore();
      printJava("boolean "+computeArgAttrName(arg));
      doAfter();
    }
    @Override
    protected void doForVariableChild(OpSyntax s, int i, Child c) {
      doBefore();
      printJava(computeVariableChildType(c)+" "+c.name);
      doAfter();
    }
    @Override
    protected void doForFixedChildren(OpSyntax s, int i, Child c, OpSyntax child) {
      doBefore();
      printJava(computeFixedChildrenType(child)+" "+c.name);
      doAfter();
    }
    @Override
    protected void doForFixedChild(OpSyntax s, int i, Child c) {
      doBefore();
      printJava(computeFixedChildType(c)+" "+c.name);
      doAfter();
    }
  }
  
  /*********************************************************************
   *  ConstructorFormalsStrategy
   *********************************************************************/

  protected class ConstructorFormalsStrategy extends VariableDeclarationsStrategy {
    private final String spaces;
    private final String[] prefixFormals;
                         
    public ConstructorFormalsStrategy(String returnSig) {
      this(alwaysFalse, returnSig);
    }    
    public ConstructorFormalsStrategy(ASTPredicate skipPred, String returnSig, String... prefix) {
      super(skipPred);
      spaces        = getSpaces(returnSig.length());
      prefixFormals = prefix;
    }    
    
    @Override
    protected void init() {
      super.init();
      for (String pre : prefixFormals) {
        doBefore();
        printJava(pre);
      }
    }
    
    @Override
    protected void doBefore() {
      if (first) {
        first = false;
      } else {
        printJava(",\n"+spaces);
      }
    }
    @Override
    protected void doAfter() {
  	  // Nothing to do
    }
  }
  
  /*********************************************************************
   *  FieldDeclarationsStrategy
   *********************************************************************/

  protected class FieldDeclarationsStrategy extends VariableDeclarationsStrategy {
    final String before;
    private FieldDeclarationsStrategy(String mods) {
      super(true);
      before = "  "+mods+" ";
    }    
    @Override
    protected void doBefore() {
      printJava(before);
    }
    @Override
    protected void doAfter() {
      printJava(";\n");
    }
  }
  protected final FieldDeclarationsStrategy finalFieldDeclarationsStrategy = new FieldDeclarationsStrategy("private final");
  protected final FieldDeclarationsStrategy fieldDeclarationsStrategy = new FieldDeclarationsStrategy("public");
  

  
  /*********************************************************************
   *  generateGetParent
   *********************************************************************/
  
  /**
   * @return true if created getParent() 
   */
  protected boolean generateGetParent(OpSyntax s) {
    return generateGetParent(s, false);
  }
  
  protected boolean wouldGenerateGetParent(OpSyntax s) {
    Set<OpSyntax> lcs = lookupLeastCommonSuper(s.name);
    if (lcs != null && !lcs.isEmpty()) {
      Set<OpSyntax> parents = lookupParents(s.name);
      return parents != null && !parents.isEmpty();
    }
    return false;
  }
  
  /**
   * @return true if created getParent() 
   */
  @SuppressWarnings("unused")
  protected boolean generateGetParent(OpSyntax s, boolean forceGeneration) {
    if (noGetParent) {
      return false;
    }
    forceGeneration       = forceGeneration && !simplifyGetParent; 
    
    Set<OpSyntax> lcs     = lookupLeastCommonSuper(s.name);
    boolean created       = false; // made getParent()?
    boolean elidesParents = elidesParents();

    if (lcs != null && !lcs.isEmpty()) {
      OpSyntax chosenLCS    = chooseLCS(lcs);
      Set<OpSyntax> parents = elidesParents ? lookupActualParents(s.name) : lookupParents(s.name);
      String type           = makeInterfaceName(chosenLCS.name);
      boolean parentsExist  = parents != null && !parents.isEmpty();

      if (parentsExist || forceGeneration) {
        created = generateGetParentMethod(s, type, elidesParents);
        System.err.println("Created getParent() for "+s.name);
      }
      if (!simplifyGetParent && parentsExist) {
        printJava("  /**\n");
        printJava("   * Checks to see if the node really takes the role of an "+makeInterfaceName(s.name)+"\n");
        if (javadocImplementationDetails()) {
          printJava("   *\n");
          printJava("   * Implementation: The parent must be one of the following:\n");
          for (OpSyntax p : parents) {
            printJava("   *     "+makeInterfaceName(p.name)+"\n");
          }
        }
        printJava("   */\n");
        printJava("  public boolean isValid"+s.name+"Node()");
        generateIsValidFooBody(s, type, parents);
        System.err.println("Created isValid"+s.name+"Node() for "+s.name);
      } 
    } else if (forceGeneration) { 
      // assume it's the root node
      created = generateGetParentMethod(s, "IJavaOperatorNode", elidesParents);    
      System.err.println("Forced generation of getParent() for "+s.name);
    } else {
      System.err.println("Didn't create anything for "+s.name);
    }
    
    if (forceGeneration && !created) {
      System.err.println("Error:   Couldn't force creation of getParent() on "+s.name);
    }
    return created;
  }
  
  private OpSyntax chooseLCS(Set<OpSyntax> lcs) {
    switch (lcs.size()) {
    case 0:
      return null;
    case 1:
      return lcs.iterator().next();
    default:
      OpSyntax rv   = null; 
      int score     = 0;
      for (OpSyntax op : lcs) {
        if (rv==null) {
          // first time through, set rv and score
          rv    = op;
          score = scoreLCS(op);
        } else {
          int opScore = scoreLCS(op);
          if (opScore > score) {
            rv = op;
          } 
          else if (opScore == score) {
            // System.err.println("WARNING: same score for "+rv.name+" and "+op.name);
            if (rv.name.compareTo(op.name) < 0) {
              rv = op;
            }
          }
        }
      }
      return rv;
    }
  }

  private int scoreLCS(OpSyntax op) {
    return op.numChildren + op.attributes.size();
  }

  protected boolean elidesParents() {
    return true;
  }

  private boolean generateGetParentMethod(OpSyntax s, String type, final boolean elidesParents) {
    final String iface = makeInterfaceName(s.name);
    if (elidesParents) {
      generateClassBodyDeclJavadoc("@return The logical parent (possibly skipping a node), ", 
                                   "typed assuming that this node is an "+iface);
    } else {
      generateClassBodyDeclJavadoc("@return The parent, typed assuming that this node is an "+iface);
    }
    if (simplifyGetParent) {
      printJava("  public "+type+" getParent()");
    } else {
      printJava("  public "+type+" getParent("+iface+" here)");
    }
    generateGetParentBody(s, type);
    return true;
  }
  
  protected void generateGetParentBody(OpSyntax s, String type) {
    printJava(methodEnding); 
  }
  protected void generateIsValidFooBody(OpSyntax s, String type, Set<OpSyntax> parents) {
    printJava(methodEnding); 
  }
  
  /*********************************************************************
   *  generateResolveBinding
   *********************************************************************/
  
  protected final String makeDeclNodeName(String name) {
    return makeNodeName(name+"Declaration");
  }
  protected void generateResolveBinding(final OpSyntax s) {
    final BindingType val = getBindsToName(s);
    if (val != null) {
      if (!s.isRoot && !hasAbstractParents(s)) {
        printJava("  @Override\n");
      }
      printJava("  public boolean bindingExists()");
      generateBindingExistsBody(s, val);
      
      printJava("  public "+val.getBindingName()+" resolveBinding()");
      generateResolveBindingBody(s, val);
    }
    else if (s.hasBinding()) { // No property
      System.err.println("Error:   "+s.name+" should have a binding, but lacks property");
    }
  }
  
  protected void generateBindingExistsBody(OpSyntax s, BindingType val) {
    printJava(methodEnding); 
  }
  
  protected void generateResolveBindingBody(OpSyntax s, BindingType val) {
    printJava(methodEnding); 
  }

  /*********************************************************************
   *  generateBridge
   *********************************************************************/
  
  protected final String makeBridgeName(String name) {
    return "I"+name+"Bridge";
  }
  
  protected void generateBridge(OpSyntax s) {
    final String type = getBridgesToName(s);
    if (type != null) {
      printJava("  public "+makeBridgeName(type)+" getBridge()");
      generateGetBridgeBody(s, type);
      
      printJava("  public void setBridge("+makeBridgeName(type)+" b)");
      generateSetBridgeBody(s, type);
    }
  }
  
  protected void generateGetBridgeBody(OpSyntax s, String type) {
    printJava(methodEnding); 
  }
  
  protected void generateSetBridgeBody(OpSyntax s, String type) {
    printJava(methodEnding); 
  }
  
  /*********************************************************************
   *  generatePkgDecl
   *********************************************************************/
  
  protected final void generatePkgDecl(String pkg, String... comments) {
    printJava("// Generated code.  Do *NOT* edit!\n");
    if (comments.length > 0) {
      generateClassJavadoc(comments);
    }
    printJava("package "+pkg+";\n\n");
  }
  
  /**
   * Add javadoc with no indent
   */
  protected final void generateClassJavadoc(String... doc) {
    printJava("/**\n");
    for (String d : doc) {
      if (d == null) {
        continue;
      }
      printJava(" * "+d+"\n");
    }
    printJava(" */\n");
  }
  
  /**
   * Add javadoc with an indent of 2
   */
  protected final void generateClassBodyDeclJavadoc(String... doc) {
    printJava("  /**\n");
    for (String d : doc) {
      if (d == null) {
        continue;
      }
      printJava("   * "+d+"\n");
    }
    printJava("   */\n");
  }
  
  /**
   * Add javadoc with an indent of 4
   */
  protected final void generateNestedClassBodyDeclJavadoc(String... doc) {
    printJava("    /**\n");
    for (String d : doc) {
      if (d == null) {
        continue;
      }
      printJava("     * "+d+"\n");
    }
    printJava("     */\n");
  }
  
  /*********************************************************************
   *  generateGetOverriddenMethod
   *********************************************************************/
  
  protected final boolean isConstructorDeclaration(OpSyntax s) {
    return "ConstructorDeclaration".equals(s.name);
  }
  
  protected final boolean isMethodDeclaration(OpSyntax s) {
    return "MethodDeclaration".equals(s.name);
  }
  
  protected final boolean isExpression(OpSyntax s) {
    return "Expression".equals(s.name);
  }
  
  protected void generateGetOverriddenMethod(OpSyntax s) {
    if (isMethodDeclaration(s)) {
      generateClassBodyDeclJavadoc("Gets the 'super-implementation' (a la Eclipse) for the method");
      printJava("  public "+makeInterfaceName("MethodDeclaration")+" getOverriddenMethod()");
      generateGetOverriddenBody();  
      generateClassBodyDeclJavadoc("Gets all of immediately overridden methods from super-classes and -interfaces");
      printJava("  public List<"+makeInterfaceName("MethodDeclaration")+"> getAllOverriddenMethods()");
      generateGetAllOverriddenBody();  
    }
    else if (isConstructorDeclaration(s)) {    
      generateClassBodyDeclJavadoc("Gets the 'super-implementation' (a la Eclipse) for the constructor");
      printJava("  public "+makeInterfaceName("ConstructorDeclaration")+" getSuperConstructor()");
      generateGetSuperConstructorBody();  
    }
    BindingType t = getBindsToTypeName(s);
    if (t != null) {
      printJava("  public boolean typeExists()");
      generateTypeExistsBody(s);  
      
      generateClassBodyDeclJavadoc("Gets the binding corresponding to the type of the "+s.name);
      printJava("  public "+t.getTypeName()+" resolveType()");
      generateResolveTypeBody(s);  
    }
  }
  protected void generateTypeExistsBody(OpSyntax s) {
    printJava(methodEnding); 
  }
  
  protected void generateGetOverriddenBody() {
    printJava(methodEnding); 
  }
  protected void generateGetAllOverriddenBody() {
    printJava(methodEnding); 
  }
  protected void generateGetSuperConstructorBody() {
    printJava(methodEnding); 
  }
  protected void generateResolveTypeBody(OpSyntax s) {
    printJava(methodEnding); 
  }
  
  protected abstract class UnfoldSyntaxStrategy {
    public void handleAttribute(OpSyntax s, String indent, int i, Attribute a) {
  	  // Nothing to do
    }
    /**
     * @return true if it was a fixed child
     */
    public abstract boolean handleChild(OpSyntax s, String indent, int i, Child c);
    public void handleToken(OpSyntax s, String indent, int i, int keyword, Token t) {
  	  // Nothing to do
    }
    /**
     * @return The (possibly changed) indent
     */
    public String handleTag(OpSyntax s, String indent, int i, Tag t) { return indent; }
  }
}
