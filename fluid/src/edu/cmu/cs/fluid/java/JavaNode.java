/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaNode.java,v 1.93 2008/12/12 19:01:03 chance Exp $ */
package edu.cmu.cs.fluid.java;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.analysis.IIRProject;
import com.surelogic.annotation.JavadocAnnotation;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.tree.SyntaxTreeNode;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.ConstantSlotFactory;
import edu.cmu.cs.fluid.ir.IRIntegerType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeType;
import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRStringType;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.util.DeclFactory;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.IROperatorType;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.unparse.Combined;
import edu.cmu.cs.fluid.unparse.Delim;
import edu.cmu.cs.fluid.unparse.Glue;
import edu.cmu.cs.fluid.unparse.Identifier;
import edu.cmu.cs.fluid.unparse.IndepBP;
import edu.cmu.cs.fluid.unparse.Keyword;
import edu.cmu.cs.fluid.unparse.Token;
import edu.cmu.cs.fluid.unparse.UnitedBP;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.util.UniqueID;

/**
 * The class used by the parser to build Java IR. Not all nodes of Java IR (for
 * example, IRNodes stored and then loaded) need have this type.
 * 
 * @see JavaOperator
 */
@SuppressWarnings("serial")
public class JavaNode extends JJNode {
  // For debugging
  // private StackTraceElement[] trace = new Throwable().getStackTrace();

  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.JavaNode");

  /**
   * Only to be called by SyntaxTreeNode()
   */
  protected JavaNode(SyntaxTreeInterface tree) {
    super(tree);
  }

  protected JavaNode(SyntaxTreeInterface tree, Operator operator) {
    super(tree, operator);
  }

  /** Constructor for bottom-up tree creation. */
  protected JavaNode(SyntaxTreeInterface tree, Operator operator, IRNode[] children) {
    super(tree, operator, children);
  }

  public static JavaNode makeJavaNode(Operator op) {
    if (JJNode.specializeForSyntaxTree) {
      return SyntaxTreeNode.create(op, null);
    }
    return new JavaNode(tree, op);
  }

  public static JavaNode makeJavaNode(Operator op, IRNode[] children) {
    if (JJNode.specializeForSyntaxTree) {
      return SyntaxTreeNode.create(op, children);
    }
    return new JavaNode(tree, op, children);
  }

  public static JavaNode makeJavaNode(SyntaxTreeInterface tree, Operator op) {
    if (JJNode.specializeForSyntaxTree && JJNode.tree == tree) {
      return SyntaxTreeNode.create(op, null);
    }
    return new JavaNode(tree, op);
  }

  public static JavaNode makeJavaNode(SyntaxTreeInterface tree, Operator op, IRNode[] children) {
    if (JJNode.specializeForSyntaxTree && JJNode.tree == tree) {
      return SyntaxTreeNode.create(op, children);
    }
    return new JavaNode(tree, op, children);
  }

  /**
   * Defaults to returning the operator for JJNode.tree Designed to be
   * overridden by subclasses
   */
  public Operator getOperator() {
    return tree.getOperator(this);
  }

  /*
   * %- SlotInit stuff
   */
  public static <T> SlotInfo<T> getSlotInfo(String slotName) {
    try {
      // ! Will need to give a real type when we want it to persist.
      return SimpleSlotFactory.prototype.newAttribute(slotName, null);
    } catch (SlotAlreadyRegisteredException e) {
      throw new FluidRuntimeException(slotName + " slot already allocated");
    }
  }

  public static <T> SlotInfo<T> getConstantSlotInfo(String slotName, IRType<T> ty) {
    try {
      return ConstantSlotFactory.prototype.newAttribute(slotName, ty);
    } catch (SlotAlreadyRegisteredException e) {
      throw new FluidRuntimeException(slotName + " slot already allocated");
    }
  }

  public static <T> SlotInfo<T> getVersionedSlotInfo(String slotName, IRType<T> ty) {
    try {
      SlotInfo<T> si = treeSlotFactory.newAttribute(slotName, ty);
      si.addObserver(treeChanged);
      return si;
    } catch (SlotAlreadyRegisteredException e) {
      throw new FluidRuntimeException(slotName + " slot already allocated");
    }
  }

  public static <T> SlotInfo<T> getVersionedSlotInfo(String slotName, IRType<T> ty, T defaultValue) {
    try {
      SlotInfo<T> si = treeSlotFactory.newAttribute(slotName, ty, defaultValue);
      si.addObserver(treeChanged);
      return si;
    } catch (SlotAlreadyRegisteredException e) {
      throw new FluidRuntimeException(slotName + " slot already allocated");
    }
  }

  /*
   * %- Modifiers stuff
   */
  public static final int ALL_FALSE = 0;

  public static final int ABSTRACT = (1 << 0);

  public static final int FINAL = (1 << 1);

  public static final int NATIVE = (1 << 2);

  public static final int PRIVATE = (1 << 3);

  public static final int PROTECTED = (1 << 4);

  public static final int PUBLIC = (1 << 5);

  public static final int STATIC = (1 << 6);

  public static final int SYNCHRONIZED = (1 << 7);

  public static final int TRANSIENT = (1 << 8);

  public static final int VOLATILE = (1 << 9);

  public static final int STRICTFP = (1 << 10);

  public static final int IMPLICIT = (1 << 11);

  public static final int INSTANCE = (1 << 12);

  public static final int VARARGS = (1 << 13);

  public static final int WRITE = (1 << 14);

  public static final int AS_BINARY = (1 << 15);

  public static final int MUTABLE = (1 << 16);

  public static final int IS_GRANULE = (1 << 17);

  public static final int NOT_GRANULE = (1 << 18);

  public static final int IMPLEMENTATION_ONLY = (1 << 19);

  public static final int NO_VERIFY = (1 << 20);

  public static final int ALLOW_RETURN = (1 << 21);

  public static final int ALLOW_READ = (1 << 22);

  public static final int ALLOW_REF_OBJECT = (1 << 23);

  public static final int HAS_PARENS = (1 << 24);
  
  public static final int[] MODIFIERS = { ABSTRACT, FINAL, NATIVE, PRIVATE, PROTECTED, PUBLIC, STATIC, SYNCHRONIZED, TRANSIENT,
      VOLATILE, STRICTFP, IMPLICIT, INSTANCE, VARARGS, WRITE, AS_BINARY, IS_GRANULE, NOT_GRANULE, IMPLEMENTATION_ONLY, NO_VERIFY,
      ALLOW_RETURN, ALLOW_READ, ALLOW_REF_OBJECT, HAS_PARENS };

  public static final String MODIFIERS_ID = "Java.modifiers";

  private static final int LASTMODIFIER = HAS_PARENS;
  public static final int ILLEGAL_MOD = LASTMODIFIER << 1;

  static final String[] modifiers = { "abstract", "final", "native", "private", "protected", "public", "static", "synchronized",
      "transient", "volatile", "strictfp", "", // implicit
      "instance", "varargs", "write", "binary", "isGranule", "notGranule", 
      "implementationOnly", "noVerify", "allowReturn", "allowRead", "allowReferenceObject", "hasParens"
  };
  
  private static final SlotInfo<Integer> modifiersSlotInfo = getVersionedSlotInfo(MODIFIERS_ID, IRIntegerType.prototype,
      new Integer(ALL_FALSE));

  public final void setModifiers(int i) {
    setModifiers(this, i);
  }

  public int getModifiers() {
    return getModifiers(this);
  }

  public static void setModifiers(IRNode node, int i) {
    int prev = getModifiers(node);
    if (prev != i) { // TODO space optimization (more time)
      final Integer cached = IntegerTable.newInteger(i);
      node.setSlotValue(modifiersSlotInfo, cached);
    }
  }

  public static int getModifiers(IRNode node) {
    if (node == null) {
      throw new NullPointerException();
    }
    return node.getIntSlotValue(modifiersSlotInfo);
  }

  // verify that the mask is good
  private static void checkMod(int mod) {
    int mask = mod - 1;
    int check = mod & mask;
    if ((check == 0) && (mod > 0) && (mod <= LASTMODIFIER)) {
      return;
    }
    throw new FluidRuntimeException("Bad modifier flag");
  }

  public static boolean getModifier(IRNode node, int mod) {
    checkMod(mod);
    int mods = getModifiers(node);
    return getModifier(mods, mod);
  }

  public static boolean getModifier(int mods, int mod) {
    return ((mods & mod) != 0);
  }

  // FIX?
  public static int setModifier(int mods, int mod, boolean accumulate) {
    checkMod(mod);
    if (accumulate) {
      mods |= mod; // Add this
    } else {
      mods &= ~mod; // Keep all but this
    }
    return mods;
  }

  public static void setModifier(IRNode node, int mod, boolean value) {
    int mods = setModifier(getModifiers(node), mod, value);
    setModifiers(node, mods);
  }

  public static void setImplicit(IRNode n) {
    setModifier(n, IMPLICIT, true);
  }

  public static boolean wasImplicit(IRNode n) {
    return getModifier(n, IMPLICIT);
  }

  public static boolean isSet(int mods, int mod) {
    return (mods & mod) != 0;
  }

  /*
   * %- Op stuff
   */
  private static final SlotInfo<Operator> opSlotInfo = getVersionedSlotInfo("Java.op", IROperatorType.prototype);

  public static void setOp(IRNode node, JavaOperator i) {
    node.setSlotValue(opSlotInfo, i);
  }

  /**
   * Gets the operator for OpAssignExpressions Doesn't apply to any other kind
   * of node
   */
  public static JavaOperator getOp(IRNode node) {
    Operator op = tree.getOperator(node);
    if (OpAssignExpression.prototype.includes(op)) {
      return (JavaOperator) node.getSlotValue(opSlotInfo);
    }
    throw new IllegalArgumentException("Not an OpAssignExpression" + op.name());
  }

  /*
   * %- Dimensions
   */
  private static final SlotInfo<Integer> dimsSlotInfo = getVersionedSlotInfo("Java.dims", IRIntegerType.prototype,
      Integer.valueOf(0));

  public static void setDimInfo(IRNode node, int dims) {
    node.setSlotValue(dimsSlotInfo, dims);
  }

  public static int getDimInfo(IRNode node) {
    return node.getIntSlotValue(dimsSlotInfo);
  }

  // / Node (for refs to other nodes without re-parenting
  private static final SlotInfo<IRNode> nodeSlotInfo = getConstantSlotInfo("Java.node", IRNodeType.prototype);

  public static void setConstantNode(IRNode node, IRNode n) {
    node.setSlotValue(nodeSlotInfo, n);
  }

  public static IRNode getConstantNode(IRNode node) {
    return node.getSlotValue(nodeSlotInfo);
  }

  public static void unparseConstantNode(IRNode node, JavaUnparser u) {
    // throw new FluidError("Not implemented");
    unparse(getConstantNode(node), u);
  }

  // / Int
  private static final SlotInfo<Integer> intSlotInfo = getConstantSlotInfo("Java.int", IRIntegerType.prototype);

  public static void setConstantInt(IRNode node, int i) {
    node.setSlotValue(intSlotInfo, i);
  }

  public static int getConstantInt(IRNode node) {
    return node.getIntSlotValue(intSlotInfo);
  }

  public static void unparseConstantInt(IRNode node, JavaUnparser u) {
    // throw new FluidError("Not implemented");
    int i = getConstantInt(node);
    Token t = new Identifier(Integer.toBinaryString(i));
    t.emit(u, node);
  }

  /*
   * %- code for compiled methods.
   */
  /**
   * The code slot holds the byte code and extra information such as exception
   * ranges necessary to execute a method on the virtual machine.
   * 
   * TODO Currently, we assume it is a string. This will be changed.
   */
  private static final SlotInfo<String> codeSlotInfo = getVersionedSlotInfo("Java.code", IRStringType.prototype);

  public static void setCode(IRNode node, Object code) {
    node.setSlotValue(codeSlotInfo, (String) code);
  }

  public static String getCode(IRNode node) {
    return node.getSlotValue(codeSlotInfo);
  }

  /*
   * %- comment
   */
  /**
   * This slot holds text of comments. Not currently used, but I want it in the
   * bundle since Bundle's are immutable.
   */
  public static final SlotInfo<String> commentSlotInfo =
  // new RootNamedSlotInfoWrapper(
  getVersionedSlotInfo("Java.comment", IRStringType.prototype, "");

  public static void setComment(IRNode node, String comment) {
    node.setSlotValue(commentSlotInfo, comment);
  }

  public static String getComment(IRNode node) {
    return node.getSlotValue(commentSlotInfo);
  }

  public static String getCommentOrNull(IRNode node) {
    if (node.valueExists(commentSlotInfo)) {
      return node.getSlotValue(commentSlotInfo);
    }
    return null;
  }

  public interface Consts {
	  public static final IRObjectType<IJavaRef> FLUID_JAVA_REF_SLOT_TYPE = new IRObjectType<IJavaRef>();
	  public static final String FLUID_JAVA_REF_SLOT_NAME = "JavaNode.IJavaRef";
  }
  
  /**
   * Fluid IR slot to hold Fluid Java code reference information.
   */
  private static final SlotInfo<IJavaRef> f_fluidJavaRefSlotInfo = 
	  getVersionedSlotInfo(Consts.FLUID_JAVA_REF_SLOT_NAME,
			  Consts.FLUID_JAVA_REF_SLOT_TYPE);

  /**
   * Given an IRNode from a Java AST, this method returns the node's Java code
   * reference information, or {@code null} if none exists.
   * <p>
   * <i>Implementation note:</i> This call constructs and cache the code
   * reference from a {@link FluidJavaRef.IRBuilder} object place on the node
   * during fAST construction.
   * 
   * @param node
   *          an IRNode which should have binding information.
   * @return a Java code reference, or {@code null} if none exists.
   */
  @Nullable
  public static IJavaRef getJavaRef(IRNode node) {
    if (node == null) {
      return null;
    }
    if (node.valueExists(f_fluidJavaRefSlotInfo))
      return node.getSlotValue(f_fluidJavaRefSlotInfo);
    else {
      final IJavaRef javaRef = SkeletonJavaRefUtility.buildOrNullOnFailure(node);
      if (javaRef != null) {
        node.setSlotValue(f_fluidJavaRefSlotInfo, javaRef);
        return javaRef;
      }
    }
    return null;
  }

  /**
   * Gets if the passed node has a Java code reference information.
   * 
   * @param node
   *          an IRNode which should have binding information.
   * @return {@code true} if the passed node has a Java code reference
   *         information, {@code false} otherwise.
   */
  public static boolean hasJavaRef(IRNode node) {
    if (node == null)
      return false;
    return node.valueExists(f_fluidJavaRefSlotInfo);
  }

  public static void makeFluidJavaRefForPackage(IIRProject proj, IRNode pkg) {
    DeclFactory f = new DeclFactory(proj.getTypeEnv().getBinder());
    IDecl decl = f.getDeclAndPosition(pkg).first();
    pkg.setSlotValue(f_fluidJavaRefSlotInfo, new JavaRef.Builder(decl).setEclipseProjectName(proj.getName()).build());
  }

  /**
   * Attempts to copy the Java code reference from one node to another. It
   * returns the code reference if the copy is successful, or {@code null} if
   * the copy is unsuccessful.
   * 
   * @param src
   *          the node to copy from.
   * @param target
   *          the node to copy to.
   * @return the code reference if the copy is successful, or {@code null} if
   *         the copy is unsuccessful.
   */
  static IJavaRef copyFluidJavaRef(IRNode src, IRNode target) {
    if (src == null) {
      throw new IllegalArgumentException();
    }
    final IJavaRef ref = JavaNode.getJavaRef(src);
    if (ref != null) {
      target.setSlotValue(f_fluidJavaRefSlotInfo, ref);
    }
    return ref;
  }

  /**
   * Fluid IR slot to hold Javadoc annotations, via the
   * <code>&#064;annotate</code> tag.
   */
  private static final SlotInfo<List<JavadocAnnotation>> f_javadocAnnosSlotInfo = getVersionedSlotInfo(
      JavadocAnnotation.JAVADOC_REF_SLOT_NAME, JavadocAnnotation.FLUID_JAVADOC_REF_SLOT_TYPE);

  /**
   * Returns the SlotInfo to access the Javadoc annotations information within
   * Java IR nodes.
   */
  private static SlotInfo<List<JavadocAnnotation>> getJavadocAnnosSlotInfo() {
    return f_javadocAnnosSlotInfo;
  }

  /**
   * Sets the ordered list of Javadoc annotations information on the passed
   * IRNode.
   * 
   * @param node
   *          an IRNode which should have binding information.
   * @param ref
   *          the Javadoc annotations.
   */
  public static void setJavadocAnnotations(IRNode node, List<JavadocAnnotation> ref) {
    if (ref == null) {
      return;
    }
    node.setSlotValue(getJavadocAnnosSlotInfo(), ref);
  }

  /**
   * Given an IRNode from a Java AST, this method returns an ordered, possibly
   * empty, list of the node's Javadoc annotations.
   * 
   * @param node
   *          an IRNode which should have binding information.
   * @return an ordered, possibly empty, list of the node's Javadoc annotations.
   */
  @NonNull
  public static List<JavadocAnnotation> getJavadocAnnotations(IRNode node) {
    if (node == null) {
      return Collections.emptyList();
    }
    final SlotInfo<List<JavadocAnnotation>> javadocSlotInfo = getJavadocAnnosSlotInfo();
    if (!node.valueExists(javadocSlotInfo)) {
      return Collections.emptyList();
    }
    List<JavadocAnnotation> result = node.getSlotValue(javadocSlotInfo);
    if (result == null)
      return Collections.emptyList();
    else
      return result;
  }

  // Copied from JavaPromise formatting
  private static Glue commentIndent = new Glue(2);

  private static Token startComments = new Combined(new Keyword(""), new UnitedBP(1, "comment", Glue.UNIT, commentIndent));

  private static Token startComment = new Combined(new UnitedBP(2, "comment", Glue.UNIT, Glue.UNIT), new Combined(new Keyword(""),
      IndepBP.JUXTBP));

  private static Token stopComments = new Combined(new UnitedBP(1, "comment", Glue.UNIT, Glue.JUXT), new Combined(new Keyword(""),
      IndepBP.DEFAULTBP));

  public static void unparseComment(IRNode node, JavaUnparser u) {
    String comment = getCommentOrNull(node);
    if (comment == null || !u.getStyle().unparseComments()) {
      return;
    }
    StringTokenizer st = new StringTokenizer(comment, "\n\r\f");
    if (st.hasMoreTokens()) {
      // does it starts with Javadoc?
      // omit leading * and spaces if present
      // detect @ tags
      startComments.emit(u, node);
      do {
        String t = st.nextToken();
        String s = t.trim();
        if (s.length() >= 2 && s.charAt(0) == '*') {
          char c = s.charAt(1);
          if (c == ' ') {
            t = s = s.substring(2).trim();
          } else if (c == '/') {
            t = "*/";
          }
        }
        startComment.emit(u, node);
        (new Identifier(t)).emit(u, node);
      } while (st.hasMoreTokens());
      stopComments.emit(u, node);
    }
  }

  /*
   * %- Unparsing
   */
  private static Token ellipsis = new Delim("...");

  public static void unparse(IRNode node, JavaUnparser u) {
    // ! TODO: add comments
    if (node == null) {
      // Assume that it is elided
      // What node should I associate it with?
      ellipsis.emit(u, null);
      return;
    }
    unparseComment(node, u);
    // leave catching to debug unparser, or else leave some record in unparser.
    // try {
    if (node.identity() != IRNode.destroyedNode) {
      ((JavaOperator) u.getTree().getOperator(node)).unparseWrapper(node, u);
    }
    /*
     * } catch(SlotUndefinedException e) { if (JJNode.tree != u.getTree()) {
     * LOG.
     * severe("unparse died trying to unparse: "+DebugUnparser.toString(node));
     * } e.printStackTrace(); }
     */
  }

  // FIX SlotInfos for SyntaxTreeInterface??
  public static void unparseInfo(IRNode node, JavaUnparser u) {
    (new Identifier(getInfo(node).toString())).emit(u, node);
  }

  private static Vector<Token> dimInfos = new Vector<Token>();

  public static void unparseDimInfo(IRNode node, JavaUnparser u) {
    int dims = getDimInfo(node);
    if (dims > 0) {
      if (dimInfos.size() < dims + 1)
        dimInfos.setSize(dims + 1);
      Token tok = dimInfos.elementAt(dims);
      if (tok == null) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims; ++i)
          sb.append("[]");
        tok = new Keyword(sb.toString());
        dimInfos.setElementAt(tok, dims);
      }
      tok.emit(u, node);
    }
  }

  public static Token getDimToken(IRNode node) {
    int dims = getDimInfo(node);
    if (dims > 0) {
      if (dimInfos.size() < dims + 1)
        dimInfos.setSize(dims + 1);
      Token tok = dimInfos.elementAt(dims);
      if (tok == null) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims; ++i)
          sb.append("[]");
        tok = new Keyword(sb.toString());
        dimInfos.setElementAt(tok, dims);
      }
      return tok;
    } else
      return null;
  }

  static final Token[] modifierTokens = new Token[modifiers.length];
  static {
    for (int i = 0; i < modifiers.length; ++i) {
      modifierTokens[i] = new Keyword(modifiers[i]);
    }
  }

  public static void unparseModifiers(IRNode node, JavaUnparser u) {
    int mods = getModifiers(node);
    for (int i = 0; mods != 0; ++i, mods >>>= 1) {
      if ((mods & 1) != 0) {
        modifierTokens[i].emit(u, node);
      }
    }
  }

  public static Token[] getModiferTokens(IRNode node) {
    Vector<Token> modifierVector = new Vector<Token>();
    int mods = getModifiers(node);
    for (int i = 0; mods != 0; ++i, mods >>>= 1) {
      if ((mods & 1) != 0) {
        modifierVector.add(modifierTokens[i]);
      }
    }
    Token[] modifierTokens = new Token[modifierVector.size()];
    modifierTokens = modifierVector.toArray(modifierTokens);
    return modifierTokens;
  }

  public static void unparseOp(IRNode node, JavaUnparser u) {
    getOp(node).asToken().emit(u, node);
  }

  private static Token codeToken = new Keyword("<compiled>;");

  public static void unparseCode(IRNode node, JavaUnparser u) {
    codeToken.emit(u, node);
  }

  /** Print a node with Java-specific AST information */
  public static String toString(IRNode node) {
    if (node == null) {
      return "null";
    } else {
      StringBuilder sb = new StringBuilder();
      try {
        int mods = getModifiers(node);

        for (int i = 0; mods != 0; ++i, mods >>>= 1) {
          if ((mods & 1) != 0) {
            sb.append(modifiers[i]);
            sb.append(' ');
          }
        }
      } catch (SlotUndefinedException e) {
        // Ignore it
      }
      sb.append(JJNode.toString(node));
      try {
        Operator op = tree.getOperator(node);
        sb.append(' ');
        sb.append(op.name());
      } catch (SlotUndefinedException e) {
        // ignore
      }
      return sb.toString();
    }
  }

  /**
   * Dump a tree made up of Java AST nodes. We add Java-specific AST
   * information.
   * 
   * @see JJNode#dumpTree
   */
  public static void dumpTree(PrintStream s, IRNode root, int indent) {
    doindent(s, indent);
    if (root == null) {
      s.println("null");
    } else {
      s.println(toString(root));
      Iterator<IRNode> children = tree.children(root);
      while (children.hasNext()) {
        IRNode child = children.next();
        dumpTree(s, child, indent + 1);
      }
    }
  }

  public static void dumpTree(PrintStream s, IRNode root) {
    dumpTree(s, root, 0);
  }

  public static void dumpTree(IRNode root) {
    dumpTree(System.out, root);
  }

  /*
   * %- Storage methods.
   */
  public static void saveAttributes(Bundle b) {
    // NB: Do not call JJNode.saveAttributes(b);
    b.saveAttribute(modifiersSlotInfo);
    b.saveAttribute(opSlotInfo);
    b.saveAttribute(dimsSlotInfo);
    // b.saveAttribute(codeSlotInfo);
    b.saveAttribute(nodeSlotInfo);
    b.saveAttribute(intSlotInfo);
    b.saveAttribute(commentSlotInfo);
  }

  private static Bundle javabundle = null;
  static {
    final UniqueID id = UniqueID.parseUniqueID("javanode");
    try {
      javabundle = Bundle.loadBundle(id, IRPersistent.fluidFileLocator);
    } catch (IOException ex) {
      JavaGlobals.PARSE.fine(ex.toString());
      // temp
      javabundle = Bundle.findBundle(id);
      saveAttributes(javabundle);
    }
  }

  public static Bundle getBundle() {
    return javabundle;
  }

  public static void main(String args[]) {
    JJNode.main(args);
    Bundle b = getBundle();
    if (b != null) {
      b.describe(System.out);
      if (!b.getID().equals(UniqueID.parseUniqueID("javanode")) && JJNode.versioningIsOn) {
        System.out.println("bundle has wrong naming, saving...");
        try {
          b.store(IRPersistent.fluidFileLocator);
        } catch (IOException e) {
          System.out.println("Failed to write");
          e.printStackTrace();
          return;
        }
        System.out.println("now you must rename " + b.getID() + ".ab to javanode.ab");
      }
    }
  }
}
