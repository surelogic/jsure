package com.surelogic.javac.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.surelogic.annotation.JavadocAnnotation;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.FileResource;
import com.surelogic.javac.JavaSourceFile;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.SkeletonJavaRefUtility;
import edu.cmu.cs.fluid.java.adapter.AbstractAdapter;
import edu.cmu.cs.fluid.java.adapter.CodeContext;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.DeclFactory;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.IllegalChildException;
import edu.cmu.cs.fluid.tree.Operator;

public class SourceAdapter extends AbstractAdapter implements TreeVisitor<IRNode, CodeContext> {
  public static final boolean includeQuotesInStringLiteral = true;

  enum TypeKind {
    IFACE, ENUM, ANNO, OTHER
  }

  private SourcePositions source;
  private LineMap lines;
  private Map<JCTree, String> javadoc;
  private CompilationUnitTree root;
  private FileResource cuRef;
  private String srcCode;
  private boolean asBinary;
  final Projects projects;
  final JavacProject jp;
  final DeclFactory declFactory;

  public SourceAdapter(Projects p, JavacProject jp) {
    super(SLLogger.getLogger());
    projects = p;
    this.jp = jp;
    declFactory = new DeclFactory(jp.getTypeEnv().getBinder());
  }

  public CodeInfo adapt(Trees t, JCCompilationUnit cut, JavaSourceFile srcFile, boolean asBinary) {
    source = t.getSourcePositions();
    root = cut;
    lines = cut.getLineMap();
    javadoc = cut.docComments;
    cuRef = new FileResource(projects, srcFile, getPackage(cut), jp.getName());
    srcCode = getSourceCode(cut.getSourceFile().getName());

    this.asBinary = asBinary;
    initACEInfo(cut);

    IRNode result = acceptNode(cut, new CodeContext(false, false, false));
    createLastMinuteNodes(result);
    if (asBinary) {
      JavaNode.setModifiers(result, JavaNode.AS_BINARY);
    }
    try {
      return new CodeInfo(jp.getTypeEnv(), cuRef, result, null, cuRef.getURI().toString(), srcCode,
          asBinary ? IJavaFileLocator.Type.INTERFACE : IJavaFileLocator.Type.SOURCE);
    } finally {
      resetACEInfo(cut);
    }
  }

  private static String getSourceCode(String path) {
    try {
      final InputStream is;
      final long length;
      if (path.endsWith(")")) {
        final int paren = path.indexOf('(');
        final ZipFile zf = new ZipFile(new File(path.substring(0, paren)));
        final ZipEntry ze = zf.getEntry(path.substring(paren + 1, path.length() - 1).replace('\\', '/'));
        is = zf.getInputStream(ze);
        length = ze.getSize();
      } else if (path.startsWith("jar:")) {
        final int bang = path.indexOf('!');
        final ZipFile zf = new ZipFile(new File(path.substring(5, bang)));
        final ZipEntry ze = zf.getEntry(path.substring(bang + 1).replace('\\', '/'));
        is = zf.getInputStream(ze);
        length = ze.getSize();
      } else {
        File f = new File(path);
        is = new FileInputStream(path);
        length = f.length();
      }
      Reader r = new InputStreamReader(is);
      char[] buf = new char[1024];
      StringBuilder sb = new StringBuilder();
      int numRead;
      long total = 0;
      while ((numRead = r.read(buf)) > 0) {
        sb.append(buf, 0, numRead);
        total += numRead;
      }
      if (total != length) {
        System.out.println("Source sizes not matching: got " + total + " out of " + length);
      }
      return sb.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getPackage(CompilationUnitTree cut) {
    if (cut.getPackageName() != null) {
      return cut.getPackageName().toString();
    } else {
      return "";
    }
  }

  private IRNode acceptNode(Tree t, CodeContext context) {
    if (t == null) {
      return null;
    }
    IRNode result = t.accept(this, context);
    addJavaRefAndCheckForJavadocAnnotations(t, result);
    return result;
  }

  /**
   * Look for a comment within the () part of an annotation
   */
  private String findComment(Tree t, IRNode result) {
    final String src = getSource(t);
    int lastParen = src.lastIndexOf(')');
    if (lastParen >= 0) {
      int endComment = src.lastIndexOf("*/", lastParen);
      if (endComment >= 0) {
        int startComment = src.lastIndexOf("/*", endComment);
        if (startComment >= 0 && startComment < endComment) {
          String comment = src.substring(startComment, endComment + 2);
          JavaNode.setComment(result, comment);
          return comment;
        }
      }
    }
    return null;
  }

  private String getSource(Tree t) {
    long end = source.getEndPosition(root, t);
    return getSource(t, end);
  }

  /**
   * Get the source, starting at where t starts, and ending at end
   */
  private String getSource(Tree t, long end) {
    long start = source.getStartPosition(root, t);
    if (start < 0 || end < 0) {
      return null;
    }
    String src = srcCode.substring((int) start, (int) end);
    return src;
  }

  /**
   * Look for a comment at the beginning of a block Either /** or //@
   */
  private void findStartBlockComment(BlockTree node, IRNode block) {
    // Find first statement to compute the source to search
    final List<? extends StatementTree> stmts = node.getStatements();
    final String src;
    if (stmts.isEmpty()) {
      src = getSource(node);
    } else {
      final StatementTree stmt0 = stmts.get(0);
      src = getSource(node, source.getStartPosition(root, stmt0));
    }
    if (src == null) {
      return;
    }
    // Look for the comment
    final int start = src.indexOf('{');
    final int startComment = src.indexOf("/*", start);
    if (startComment >= 0) {
      final int endComment = src.indexOf("*/", startComment);
      if (endComment >= 0) {
        final String comment = src.substring(startComment, endComment + 2);
        JavaNode.setComment(block, comment);
      }
    }
    // Otherwise, no comment
  }

  private void addJavaRefAndCheckForJavadocAnnotations(Tree t, IRNode result) {
    if (SkeletonJavaRefUtility.hasRegistered(result))
      return;

    long start = source.getStartPosition(root, t);
    long end = source.getEndPosition(root, t);
    if (start < 0) {
      start = end;
    }
    long line = lines.getLineNumber(start);
    String comment = javadoc == null ? null : javadoc.get(t);
    if (comment != null && AnnotationVisitor.allowJavadoc(jp.getTypeEnv())) {
      JavaNode.setComment(result, comment);
      List<JavadocAnnotation> elmt = getJavadocAnnotations(comment);
      if (elmt != null)
        JavaNode.setJavadocAnnotations(result, elmt);
    }
    /*
     * save skeletion JavaRef
     */
    SkeletonJavaRefUtility.registerSourceLocation(declFactory, result, cuRef, (int) line, (int) start, (int) (end - start));
  }

  private List<JavadocAnnotation> getJavadocAnnotations(final String declarationComment) {
    if (declarationComment == null)
      return null;

    final List<JavadocAnnotation> javadocAnnotations = new ArrayList<JavadocAnnotation>();

    String tag = null;
    StringBuilder sb = new StringBuilder();
    StringTokenizer st = new StringTokenizer(declarationComment, "\n");
    while (st.hasMoreTokens()) {
      String line = st.nextToken().trim();
      if (line.startsWith("@")) {
        if (tag != null) {
          javadocAnnotations.add(new JavadocAnnotation(sb.toString()));
          sb.setLength(0);
        }
        int spaceIndex = line.indexOf(' ');
        if (spaceIndex == -1) {
          // rest of the line is the tag
          tag = line.substring(1);
          line = ""; // trim off
        } else {
          tag = line.substring(1, spaceIndex);
          line = line.substring(spaceIndex + 1);
        }
        System.out.println("found tag: " + tag);
        if (SLUtility.JAVADOC_ANNOTATE_TAG.equals(tag)) {
          /*
           * Found an "annotate" tag
           */
          sb.setLength(0); // clear
          sb.append(line.trim()); // add rest of line - no tag
        } else {
          // keep searching -- just a normal tag, e.g., @author @see
          tag = null;
        }
      } else if (tag != null) {
        sb.append(' ').append(line.trim());
      }
    }
    // at end -- process last tag, if any
    if (tag != null) {
      javadocAnnotations.add(new JavadocAnnotation(sb.toString()));
      sb.setLength(0);
    }
    System.out.println(javadocAnnotations);
    if (javadocAnnotations.isEmpty())
      return null; // if empty so won't store on IRNode
    else
      return javadocAnnotations;
  }

  protected abstract class AbstractSourceFunction<T extends Tree> extends AbstractFunction<T> {
    public final IRNode call(T t, CodeContext context) {
      IRNode result = callFunc(t, context);
      addJavaRefAndCheckForJavadocAnnotations(t, result);
      return result;
    }

    abstract IRNode callFunc(T t, CodeContext context);
  }

  private final Function<Tree> acceptNodes = new AbstractSourceFunction<Tree>() {
    public IRNode callFunc(Tree t, CodeContext context) {
      return acceptNode(t, context);
    }
  };

  private final Function<Tree> adaptTypes = new AbstractSourceFunction<Tree>() {
    public IRNode callFunc(Tree t, CodeContext context) {
      return adaptType(t, context);
    }
  };

  private IRNode adaptType(Tree t, CodeContext context) {
    if (t == null) {
      return null;
    }
    switch (t.getKind()) {
    case ARRAY_TYPE:
      return visitArrayType((ArrayTypeTree) t, context);
    case PARAMETERIZED_TYPE:
      return visitParameterizedType((ParameterizedTypeTree) t, context);
    case PRIMITIVE_TYPE:
      return visitPrimitiveType((PrimitiveTypeTree) t, context);
    case IDENTIFIER:
      /*
       * IdentifierTree id = (IdentifierTree) t; return
       * NamedType.createNode(id.getName().toString());
       */
    case MEMBER_SELECT: // TypeRef
      /*
       * MemberSelectTree mst = (MemberSelectTree) t; IRNode base =
       * adaptType(mst.getExpression()); return TypeRef.createNode(base,
       * mst.getIdentifier().toString());
       */
      IRNode name = acceptNode(t, context);
      if (TypeRef.prototype.includes(name)) {
        return name;
      }
      IRNode rv = NameType.createNode(name);
      addJavaRefAndCheckForJavadocAnnotations(t, rv);
      return rv;
    case EXTENDS_WILDCARD:
    case SUPER_WILDCARD:
    case UNBOUNDED_WILDCARD:
      return visitWildcard((WildcardTree) t, context);
    case UNION_TYPE:
      return visitUnionType((UnionTypeTree) t, context);
    default:
      throw new IllegalArgumentException("Unknown type: " + t.getKind());
    }
  }

  private final Function<Tree> adaptParameters = new Function<Tree>() {
    public IRNode call(Tree t, CodeContext context, int i, int n) {
      return adaptParameter(t, context, false);
    }
  };
  private final Function<Tree> adaptVarargsParameters = new Function<Tree>() {
    public IRNode call(Tree t, CodeContext context, int i, int n) {
      return adaptParameter(t, context, i == n - 1);
    }
  };

  private IRNode adaptParameter(Tree t, CodeContext context, boolean varargs) {
    VariableTree v = (VariableTree) t;
    int mods = adaptModifiers(v.getModifiers());
    IRNode annos = adaptAnnotations(v.getModifiers(), context);
    IRNode type;
    if (varargs) {
      ArrayTypeTree at = (ArrayTypeTree) v.getType();
      type = VarArgsType.createNode(adaptType(at.getType(), context));
    } else {
      type = adaptType(v.getType(), context);
    }
    String id = v.getName().toString();
    id = CommonStrings.intern(id);
    IRNode rv = ParameterDeclaration.createNode(annos, mods, type, id);
    addJavaRefAndCheckForJavadocAnnotations(t, rv);
    return rv;
  }

  private final Function<Tree> adaptStatements = new AbstractSourceFunction<Tree>() {
    public IRNode callFunc(Tree t, CodeContext context) {
      return adaptStatement(t, context);
    }
  };

  private IRNode adaptStatement(Tree t, CodeContext context) {
    if (t instanceof VariableTree) {
      VariableTree node = (VariableTree) t;
      int mods = adaptModifiers(node.getModifiers());
      IRNode annos = adaptAnnotations(node.getModifiers(), context);
      IRNode type = adaptType(node.getType(), context);
      IRNode init = adaptExpr(node.getInitializer(), context);
      if (init == null) {
        init = NoInitialization.prototype.jjtCreate();
      } else {
        init = Initialization.createNode(init);
      }
      String id = node.getName().toString();
      id = CommonStrings.intern(id);
      IRNode vd = VariableDeclarator.createNode(id, 0, init);
      addJavaRefAndCheckForJavadocAnnotations(node.getInitializer(), vd);

      IRNode[] vars = new IRNode[] { vd };
      IRNode vdecls = VariableDeclarators.createNode(vars);
      return DeclStatement.createNode(annos, mods, type, vdecls);
    } else if (t instanceof ClassTree) {
      IRNode decl = adaptClass((ClassTree) t, context, false);
      return TypeDeclarationStatement.createNode(decl);
    }
    return acceptNode(t, context);
  }

  private final Function<Tree> adaptStatementExprs = new AbstractSourceFunction<Tree>() {
    public IRNode callFunc(Tree t, CodeContext context) {
      if (t.getKind() == Kind.EXPRESSION_STATEMENT) {
        ExpressionStatementTree s = (ExpressionStatementTree) t;
        return adaptExpr(s.getExpression(), context);
      }
      return adaptStatement(t, context);
    }
  };

  private class AdaptMembers extends AbstractSourceFunction<Tree> {
    final String className;
    final TypeKind kind;

    AdaptMembers(String name, TypeKind k) {
      className = name;
      kind = k;
    }

    public IRNode callFunc(Tree t, CodeContext context) {
      switch (t.getKind()) {
      case CLASS:
        // Needed for the Java 7 javac
      case INTERFACE:
      case ENUM:
      case ANNOTATION_TYPE:
        return adaptClass((ClassTree) t, context, true);
      case BLOCK:
        boolean isStatic = t.toString().startsWith("static");
        if (isStatic) {
          context = new CodeContext(context, JavaNode.STATIC);
        }
        IRNode block = asBinary ? BlockStatement.createNode(noNodes) : acceptNode(t, context);
        return ClassInitializer.createNode(isStatic ? JavaNode.STATIC : 0, block);
      case METHOD:
        return adaptMethod((MethodTree) t, className, kind, context);
      case VARIABLE:
        return adaptField((VariableTree) t, className, kind, context);
      default:
        return acceptNode(t, context);
      }
    }
  };

  private IRNode adaptExpr(Tree t, CodeContext context) {
    if (t == null) {
      return null;
    }
    IRNode rv;
    switch (t.getKind()) {
    case IDENTIFIER:
    case MEMBER_SELECT:
      IRNode name = acceptNode(t, context);
      if (edu.cmu.cs.fluid.java.operator.Name.prototype.includes(name)) {
        rv = NameExpression.createNode(name);
      } else {
        rv = name;
      }
      break;
    default:
      rv = acceptNode(t, context);
    }
    addJavaRefAndCheckForJavadocAnnotations(t, rv);
    return rv;
  }

  private final Function<Tree> adaptExprs = new AbstractSourceFunction<Tree>() {
    public IRNode callFunc(Tree t, CodeContext context) {
      return adaptExpr(t, context);
    }
  };

  private int adaptModifiers(ModifiersTree t) {
    int mods = JavaNode.ALL_FALSE;
    for (Modifier m : t.getFlags()) {
      switch (m) {
      case ABSTRACT:
        mods = JavaNode.setModifier(mods, JavaNode.ABSTRACT, true);
        break;
      case FINAL:
        mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
        break;
      case NATIVE:
        mods = JavaNode.setModifier(mods, JavaNode.NATIVE, true);
        break;
      case PRIVATE:
        mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
        break;
      case PROTECTED:
        mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
        break;
      case PUBLIC:
        mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
        break;
      case STATIC:
        mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
        break;
      case STRICTFP:
        mods = JavaNode.setModifier(mods, JavaNode.STRICTFP, true);
        break;
      case SYNCHRONIZED:
        mods = JavaNode.setModifier(mods, JavaNode.SYNCHRONIZED, true);
        break;
      case TRANSIENT:
        mods = JavaNode.setModifier(mods, JavaNode.TRANSIENT, true);
        break;
      case VOLATILE:
        mods = JavaNode.setModifier(mods, JavaNode.VOLATILE, true);
        break;
      }
    }
    return mods;
  }

  private IRNode adaptAnnotations(ModifiersTree t, CodeContext context) {
    IRNode[] result = map(acceptNodes, t.getAnnotations(), context);
    return Annotations.createNode(result);
  }

  public IRNode visitAnnotation(AnnotationTree node, CodeContext context) {
    String name = node.getAnnotationType().toString();
    name = CommonStrings.intern(name);
    // IRNode r = acceptNode(node.getAnnotationType()); // ident

    // literal, assignment
    final int numArgs = node.getArguments().size();
    IRNode rv = null;
    try {
      switch (numArgs) {
      case 0:
        return rv = MarkerAnnotation.createNode(name);
      case 1:
        ExpressionTree a = node.getArguments().get(0);
        if (!(a instanceof AssignmentTree)) {
          IRNode arg = adaptExpr(a, context);
          return rv = SingleElementAnnotation.createNode(name, arg);
        }
      default:
        IRNode[] args = adaptElementValuePairs(node.getArguments(), context);
        IRNode pairs = ElementValuePairs.createNode(args);
        return rv = NormalAnnotation.createNode(name, pairs);
      }
    } finally {
      if (rv != null) {
        findComment(node, rv);
        addJavaRefAndCheckForJavadocAnnotations(node, rv);
      }
    }
  }

  private IRNode[] adaptElementValuePairs(List<? extends ExpressionTree> arguments, CodeContext context) {
    IRNode[] results = new IRNode[arguments.size()];
    int i = 0;
    for (ExpressionTree e : arguments) {
      AssignmentTree a = (AssignmentTree) e;
      IRNode value = adaptExpr(a.getExpression(), context);
      String name = a.getVariable().toString();
      results[i] = ElementValuePair.createNode(name, value);
      i++;
    }
    return results;
  }

  /*
   * public IRNode visitAnnotatedType(AnnotatedTypeTree node, CodeContext
   * context) { throw new UnsupportedOperationException(); }
   */

  public IRNode visitArrayAccess(ArrayAccessTree node, CodeContext context) {
    IRNode e = adaptExpr(node.getExpression(), context);
    IRNode i = adaptExpr(node.getIndex(), context);
    return ArrayRefExpression.createNode(e, i);
  }

  public IRNode visitArrayType(ArrayTypeTree node, CodeContext context) { // FIX
                                                                          // inefficient
    IRNode base = adaptType(node.getType(), context);
    IRNode rv = ArrayType.createNode(base, 1);
    addJavaRefAndCheckForJavadocAnnotations(node, rv);
    return rv;
  }

  public IRNode visitAssert(AssertTree node, CodeContext context) {
    IRNode cond = adaptExpr(node.getCondition(), context);
    IRNode msg = adaptExpr(node.getDetail(), context);
    if (msg == null) {
      return AssertStatement.createNode(cond);
    }
    return AssertMessageStatement.createNode(cond, msg);
  }

  public IRNode visitAssignment(AssignmentTree node, CodeContext context) {
    IRNode var = adaptExpr(node.getVariable(), context);
    IRNode e = adaptExpr(node.getExpression(), context);
    return AssignExpression.createNode(var, e);
  }

  public IRNode visitBinary(BinaryTree node, CodeContext context) {
    IRNode op1 = adaptExpr(node.getLeftOperand(), context);
    IRNode op2 = adaptExpr(node.getRightOperand(), context);
    switch (node.getKind()) {
    case AND:
      return AndExpression.createNode(op1, op2);
    case CONDITIONAL_AND:
      return ConditionalAndExpression.createNode(op1, op2);
    case CONDITIONAL_OR:
      return ConditionalOrExpression.createNode(op1, op2);
    case DIVIDE:
      return DivExpression.createNode(op1, op2);
    case EQUAL_TO:
      return EqExpression.createNode(op1, op2);
    case GREATER_THAN:
      return GreaterThanExpression.createNode(op1, op2);
    case GREATER_THAN_EQUAL:
      return GreaterThanEqualExpression.createNode(op1, op2);
    case LEFT_SHIFT:
      return LeftShiftExpression.createNode(op1, op2);
    case LESS_THAN:
      return LessThanExpression.createNode(op1, op2);
    case LESS_THAN_EQUAL:
      return LessThanEqualExpression.createNode(op1, op2);
    case MINUS:
      return SubExpression.createNode(op1, op2);
    case MULTIPLY:
      return MulExpression.createNode(op1, op2);
    case NOT_EQUAL_TO:
      return NotEqExpression.createNode(op1, op2);
    case OR:
      return OrExpression.createNode(op1, op2);
    case PLUS:
      return AddExpression.createNode(op1, op2);
    case REMAINDER:
      return RemExpression.createNode(op1, op2);
    case RIGHT_SHIFT:
      return RightShiftExpression.createNode(op1, op2);
    case UNSIGNED_RIGHT_SHIFT:
      return UnsignedRightShiftExpression.createNode(op1, op2);
    case XOR:
      return XorExpression.createNode(op1, op2);
    default:
      throw new IllegalArgumentException("Unknown binop: " + node.getKind());
    }
  }

  public IRNode visitBlock(BlockTree node, CodeContext context) {
    IRNode[] result = map(adaptStatements, node.getStatements(), context);
    IRNode block = BlockStatement.createNode(result);
    findStartBlockComment(node, block);
    return block;
  }

  public IRNode visitBreak(BreakTree node, CodeContext context) {
    Name label = node.getLabel();
    if (label != null) {
      return LabeledBreakStatement.createNode(label.toString());
    }
    return SomeBreakStatement.createNode("");
  }

  public IRNode visitCase(CaseTree node, CodeContext context) {
    ExpressionTree e = node.getExpression();
    IRNode label;
    if (e == null) {
      label = DefaultLabel.prototype.jjtCreate();
    } else {
      IRNode expr = adaptExpr(e, context);
      label = ConstantLabel.createNode(expr);
    }
    IRNode[] stmts = map(adaptStatements, node.getStatements(), context);
    return SwitchElement.createNode(label, SwitchStatements.createNode(stmts));
  }

  public IRNode visitCatch(CatchTree node, CodeContext context) {
    IRNode v = adaptParameter(node.getParameter(), context, false);
    IRNode b = acceptNode(node.getBlock(), context);
    return CatchClause.createNode(v, b);
  }

  public IRNode visitClass(ClassTree node, CodeContext context) {
    return adaptClass(node, context, false);
  }

  private IRNode adaptClass(ClassTree node, CodeContext context, boolean isNested) {
    String mod = node.getModifiers().toString();
    String id = node.getSimpleName().toString();
    String src = node.toString().substring(mod.length() + 1);
    int idLoc = src.indexOf(id);
    int clazz = src.indexOf("class ");
    int enm = src.indexOf("enum ");

    // int brace = src.indexOf('{');
    final boolean isClass = clazz >= 0 && clazz < idLoc;
    final boolean isAnno = mod.contains("interface @");
    final boolean isInterface = !isAnno && mod.contains("interface ");
    final boolean isEnum = !isClass && !isInterface && enm >= 0 && enm < idLoc;
    final TypeKind kind;
    if (isAnno) {
      kind = TypeKind.ANNO;
      context = CodeContext.makeFromAnnotation(context, true);
    } else if (isInterface) {
      kind = TypeKind.IFACE;
      context = CodeContext.makeFromInterface(context, true);
    } else if (isEnum) {
      kind = TypeKind.ENUM;
    } else {
      kind = TypeKind.OTHER;
    }
    int mods = adaptModifiers(node.getModifiers());
    IRNode annos = adaptAnnotations(node.getModifiers(), context);
    IRNode[] types = map(acceptNodes, node.getTypeParameters(), context);
    IRNode ext = adaptType(node.getExtendsClause(), context);
    if (ext == null) {
      ext = NamedType.createNode(CommonStrings.JAVA_LANG_OBJECT);
      addJavaRefAndCheckForJavadocAnnotations(node, ext);
    }

    id = CommonStrings.intern(id);
    /*
     * if ("HASessionStateImpl".equals(id)) {
     * System.out.println("Found HASessionStateImpl"); }
     */
    IRNode[] impl = map(adaptTypes, node.getImplementsClause(), context);
    IRNode[] mbrs = map(new AdaptMembers(id, kind), node.getMembers(), context);
    IRNode formals = TypeFormals.createNode(types);
    IRNode body;
    try {
      body = ClassBody.createNode(mbrs);
      addJavaRefAndCheckForJavadocAnnotations(node, body);
    } catch (IllegalChildException e) {
      body = null;
      map(new AdaptMembers(id, kind), node.getMembers(), context);
    }
    IRNode rv;
    if (context.fromInterface()) {
      // JLS 9.5
      mods |= JavaNode.PUBLIC;
      mods |= JavaNode.STATIC;
    }
    if (isInterface) {
      if (isNested) {
        rv = NestedInterfaceDeclaration.createNode(annos, mods, id, formals, Extensions.createNode(impl), body);
      } else {
        rv = InterfaceDeclaration.createNode(annos, mods, id, formals, Extensions.createNode(impl), body);
      }
      addJavaRefAndCheckForJavadocAnnotations(node, rv);
      createRequiredInterfaceNodes(rv);
      return rv;
    }
    IRNode impls = Implements.createNode(impl);
    if (isEnum) {
      if (isNested) {
        mods |= JavaNode.STATIC;
        rv = NestedEnumDeclaration.createNode(annos, mods, id, impls, body);
      } else {
        rv = EnumDeclaration.createNode(annos, mods, id, impls, body);
      }
      addJavaRefAndCheckForJavadocAnnotations(node, rv);
      createRequiredClassNodes(rv);
      return rv;
    }
    if (!isClass) { // Java 5 Annotation!
      if (isNested) {
        rv = NestedAnnotationDeclaration.createNode(annos, mods, id, body);
      } else {
        rv = AnnotationDeclaration.createNode(annos, mods, id, body);
      }
      return rv;
    }
    if (context.fromInterface() || context.isStatic()) {
      mods |= JavaNode.STATIC;
    }
    if (isNested) {
      rv = NestedClassDeclaration.createNode(annos, mods, id, formals, ext, impls, body);
    } else {
      rv = ClassDeclaration.createNode(annos, mods, id, formals, ext, impls, body);
    }
    addJavaRefAndCheckForJavadocAnnotations(node, rv);
    createRequiredClassNodes(rv);
    return rv;
  }

  public IRNode visitCompilationUnit(CompilationUnitTree node, CodeContext context) {
    IRNode pkg;
    if (node.getPackageName() == null) {
      pkg = UnnamedPackageDeclaration.prototype.jjtCreate();
    } else {
      String pkgName = CommonStrings.intern(node.getPackageName().toString());
      IRNode[] annos = map(acceptNodes, node.getPackageAnnotations(), context);
      pkg = NamedPackageDeclaration.createNode(Annotations.createNode(annos), pkgName);
      addJavaRefAndCheckForJavadocAnnotations(node, pkg);
    }
    IRNode[] imports = augmentImports(map(acceptNodes, node.getImports(), context));
    IRNode[] types = map(acceptNodes, filterStmts(node.getTypeDecls()), context);
    return CompilationUnit.createNode(pkg, ImportDeclarations.createNode(imports), TypeDeclarations.createNode(types));
  }

  private IRNode[] augmentImports(IRNode[] imports) {
    for (IRNode i : imports) {
      String unparse = DebugUnparser.toString(i);
      if ("import java.lang.*;".equals(unparse)) {
        return imports;
      }
    }
    IRNode[] result = new IRNode[imports.length + 1];
    System.arraycopy(imports, 0, result, 1, imports.length);
    result[0] = ImportDeclaration.createNode(DemandName.createNode("java.lang"));
    return result;
  }

  private List<? extends Tree> filterStmts(List<? extends Tree> typeDecls) {
    for (Tree t : typeDecls) {
      if (t.getKind() == Kind.EMPTY_STATEMENT) {
        List<Tree> result = new ArrayList<Tree>(typeDecls.size());
        for (Tree td : typeDecls) {
          if (td.getKind() != Kind.EMPTY_STATEMENT) {
            result.add(td);
          }
        }
        return result;
      }
    }
    return typeDecls;
  }

  public IRNode visitCompoundAssignment(CompoundAssignmentTree node, CodeContext context) {
    IRNode v = adaptExpr(node.getVariable(), context);
    IRNode e = adaptExpr(node.getExpression(), context);
    JavaOperator op = getOperator(node);
    return OpAssignExpression.createNode(v, op, e);
  }

  private JavaOperator getOperator(CompoundAssignmentTree node) {
    switch (node.getKind()) {
    case AND_ASSIGNMENT:
      return AndExpression.prototype;
    case DIVIDE_ASSIGNMENT:
      return DivExpression.prototype;
    case LEFT_SHIFT_ASSIGNMENT:
      return LeftShiftExpression.prototype;
    case MINUS_ASSIGNMENT:
      return SubExpression.prototype;
    case MULTIPLY_ASSIGNMENT:
      return MulExpression.prototype;
    case OR_ASSIGNMENT:
      return OrExpression.prototype;
    case PLUS_ASSIGNMENT:
      return AddExpression.prototype;
    case REMAINDER_ASSIGNMENT:
      return RemExpression.prototype;
    case RIGHT_SHIFT_ASSIGNMENT:
      return RightShiftExpression.prototype;
    case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
      return UnsignedRightShiftExpression.prototype;
    case XOR_ASSIGNMENT:
      return XorExpression.prototype;
    default:
      throw new IllegalArgumentException("Unknown binop: " + node.getKind());
    }
  }

  public IRNode visitConditionalExpression(ConditionalExpressionTree node, CodeContext context) {
    IRNode c = adaptExpr(node.getCondition(), context);
    IRNode e1 = adaptExpr(node.getTrueExpression(), context);
    IRNode e2 = adaptExpr(node.getFalseExpression(), context);
    return ConditionalExpression.createNode(c, e1, e2);
  }

  public IRNode visitContinue(ContinueTree node, CodeContext context) {
    Name label = node.getLabel();
    if (label != null) {
      return LabeledContinueStatement.createNode(label.toString());
    }
    return SomeContinueStatement.createNode("");
  }

  public IRNode visitDoWhileLoop(DoWhileLoopTree node, CodeContext context) {
    // To compensate for how javac handles ()
    ParenthesizedTree exp = (ParenthesizedTree) node.getCondition();
    IRNode c = adaptExpr(exp.getExpression(), context);
    IRNode s = acceptNode(node.getStatement(), context);
    return DoStatement.createNode(s, c);
  }

  public IRNode visitEmptyStatement(EmptyStatementTree node, CodeContext context) {
    return EmptyStatement.prototype.jjtCreate();
  }

  public IRNode visitEnhancedForLoop(EnhancedForLoopTree node, CodeContext context) {
    IRNode v = adaptParameter(node.getVariable(), context, false);
    IRNode e = adaptExpr(node.getExpression(), context);
    IRNode s = acceptNode(node.getStatement(), context);
    return ForEachStatement.createNode(v, e, s);
  }

  public IRNode visitErroneous(ErroneousTree node, CodeContext context) {
    throw new UnsupportedOperationException("What to do with erroneous code?");
  }

  public IRNode visitExpressionStatement(ExpressionStatementTree node, CodeContext context) {
    IRNode e = adaptExpr(node.getExpression(), context);
    return ExprStatement.createNode(e);
  }

  private boolean onlyVars(List<? extends StatementTree> init) {
    for (StatementTree st : init) {
      if (st.getKind() != Kind.VARIABLE) {
        return false;
      }
    }
    return true;
  }

  /**
   * Make one or more DeclStatements, as needed
   */
  private IRNode[] makeDeclStatements(List<? extends StatementTree> inits, CodeContext context) {
    final List<IRNode> decls = new ArrayList<IRNode>();
    final IRNode[] vars = new IRNode[1];
    for (StatementTree st : inits) {
      VariableTree node = (VariableTree) st;
      IRNode annos = adaptAnnotations(node.getModifiers(), context);
      IRNode type = adaptType(node.getType(), context);
      int mods = adaptModifiers(node.getModifiers());
      IRNode init = adaptExpr(node.getInitializer(), context);
      if (init == null) {
        init = NoInitialization.prototype.jjtCreate();
      } else {
        init = Initialization.createNode(init);
      }
      String id = node.getName().toString();
      id = CommonStrings.intern(id);
      IRNode vd = VariableDeclarator.createNode(id, 0, init);
      addJavaRefAndCheckForJavadocAnnotations(node.getInitializer(), vd);
      vars[0] = vd;

      IRNode vdecls = VariableDeclarators.createNode(vars);
      decls.add(DeclStatement.createNode(annos, mods, type, vdecls));
    }
    return decls.toArray(noNodes);
  }

  public IRNode visitForLoop(ForLoopTree node, CodeContext context) {
    IRNode cond = adaptExpr(node.getCondition(), context);
    if (cond == null) {
      cond = TrueExpression.prototype.jjtCreate();
    }
    IRNode[] updates = map(adaptStatementExprs, node.getUpdate(), context);
    IRNode update = StatementExpressionList.createNode(updates);
    IRNode loop = acceptNode(node.getStatement(), context);
    IRNode init;
    if (node.getInitializer().isEmpty()) {
      init = StatementExpressionList.createNode(noNodes);
    } else if (onlyVars(node.getInitializer())) {
      IRNode[] decls = makeDeclStatements(node.getInitializer(), context);
      if (decls.length == 1) {
        init = decls[0];
      } else {
        // Different types, so we need to create a separate block to accommodate
        // that
        IRNode lastDecl = decls[decls.length - 1];
        decls[decls.length - 1] = ForStatement.createNode(lastDecl, cond, update, loop);
        return BlockStatement.createNode(decls);
      }
    } else {
      IRNode[] inits = map(adaptStatementExprs, node.getInitializer(), context);
      init = StatementExpressionList.createNode(inits);
    }
    return ForStatement.createNode(init, cond, update, loop);
  }

  public IRNode visitIdentifier(IdentifierTree node, CodeContext context) {
    IRNode rv = null;
    try {
      String id = node.getName().toString();
      if ("this".equals(id)) {
        return rv = ThisExpression.prototype.jjtCreate();
      } else if ("super".equals(id)) {
        return rv = SuperExpression.prototype.jjtCreate();
      }
      // return VariableUseExpression.createNode(id);
      id = CommonStrings.pool(id);
      return rv = SimpleName.createNode(id);
    } finally {
      if (rv != null) {
        addJavaRefAndCheckForJavadocAnnotations(node, rv);
      }
    }
  }

  public IRNode visitIf(IfTree node, CodeContext context) {
    // To compensate for how javac handles ()
    ParenthesizedTree exp = (ParenthesizedTree) node.getCondition();
    IRNode cond = adaptExpr(exp.getExpression(), context);
    IRNode s1 = acceptNode(node.getThenStatement(), context);
    IRNode s2 = acceptNode(node.getElseStatement(), context);
    if (s2 == null) {
      s2 = NoElseClause.prototype.jjtCreate();
    } else {
      s2 = ElseClause.createNode(s2);
    }
    return IfStatement.createNode(cond, s1, s2);
  }

  public IRNode visitImport(ImportTree node, CodeContext context) {
    MemberSelectTree t = (MemberSelectTree) node.getQualifiedIdentifier();
    String id = t.getIdentifier().toString();
    IRNode item;
    if ("*".equals(id)) {
      // Some kind of demand name
      if (node.isStatic()) {
        IRNode type = adaptType(t.getExpression(), context);
        item = StaticDemandName.createNode(type);
      } else {
        String name = t.getExpression().toString();
        name = CommonStrings.intern(name);
        item = DemandName.createNode(name);
      }
    } else if (node.isStatic()) {
      IRNode type = adaptType(t.getExpression(), context);
      id = CommonStrings.intern(id);
      item = StaticImport.createNode(type, id);
    } else {
      item = adaptType(t, context);
    }
    return ImportDeclaration.createNode(item);
  }

  /*
   * // Squashes the whole name into one SimpleName private IRNode
   * adaptImportType(Tree t) {
   * 
   * IRNode n = null; switch (t.getKind()) { case MEMBER_SELECT:
   * MemberSelectTree mst = (MemberSelectTree) t; IRNode base =
   * SimpleName.createNode(mst.getExpression().toString()); n =
   * QualifiedName.createNode(base, mst.getIdentifier().toString()); break; case
   * IDENTIFIER: n = SimpleName.createNode(t.toString()); break; } return
   * NameType.createNode(n); }
   */

  public IRNode visitInstanceOf(InstanceOfTree node, CodeContext context) {
    IRNode e = adaptExpr(node.getExpression(), context);
    IRNode t = adaptType(node.getType(), context);
    return InstanceOfExpression.createNode(e, t);
  }

  public IRNode visitLabeledStatement(LabeledStatementTree node, CodeContext context) {
    String label = node.getLabel().toString();
    IRNode s = acceptNode(node.getStatement(), context);
    return LabeledStatement.createNode(label, s);
  }

  public IRNode visitLiteral(LiteralTree node, CodeContext context) {
    Object o = node.getValue();
    switch (node.getKind()) {
    case INT_LITERAL:
      Integer i = (Integer) o;
      return IntLiteral.createNode(CommonStrings.valueOf(i)); // FIX
    case LONG_LITERAL:
      Long l = (Long) o;
      return IntLiteral.createNode(l.toString() + 'L'); // FIX
    case FLOAT_LITERAL:
      Float f = (Float) o;
      return FloatLiteral.createNode(f.toString() + 'f'); // FIX
    case DOUBLE_LITERAL:
      Double d = (Double) o;
      return FloatLiteral.createNode(d.toString()); // FIX
    case BOOLEAN_LITERAL:
      Boolean b = (Boolean) o;
      return (b ? TrueExpression.prototype : FalseExpression.prototype).jjtCreate();
    case CHAR_LITERAL:
      Character c = (Character) o;
      return CharLiteral.createNode("'\\u" + Integer.toHexString(c.charValue()) + "'");
    case STRING_LITERAL:
      String s = (String) o;
      if (includeQuotesInStringLiteral) {
        if (s.length() == 0) {
          s = "\"\"";
        } else {
          s = '\"' + s + '\"';
        }
      }
      return StringLiteral.createNode(s); // FIX
    case NULL_LITERAL:
      return NullLiteral.prototype.jjtCreate();
    default:
      throw new IllegalArgumentException("Unknown literal: " + node.getKind());
    }
  }

  public IRNode visitMemberSelect(MemberSelectTree node, CodeContext context) {
    IRNode rv = null;
    try {
      String unparse = node.toString();
      /*
       * if (unparse.equals("com.acclamation.config.xml") ||
       * unparse.equals("com.acclamation.datawrappers")) {
       * System.out.println("Looking at "+unparse); }
       */
      if (jp.getTypeEnv().findPackage(unparse, null) != null) {
        // A package matches this name
        unparse = CommonStrings.intern(unparse);
        return rv = SimpleName.createNode(unparse);
      }
      ExpressionTree t = node.getExpression();
      String id = node.getIdentifier().toString();
      if ("class".equals(id)) {
        IRNode e = adaptType(t, context);
        return rv = ClassExpression.createNode(e);
      }
      if ("this".equals(id)) {
        IRNode e = adaptType(t, context);
        return rv = QualifiedThisExpression.createNode(e);
      }
      if ("super".equals(id)) {
        IRNode e = adaptType(t, context);
        return rv = QualifiedSuperExpression.createNode(e);
      }
      IRNode e = acceptNode(t, context);

      if (Expression.prototype.includes(e)) {
        return rv = FieldRef.createNode(e, id);
      }
      if (Type.prototype.includes(e)) {
        return rv = TypeRef.createNode(e, id);
      }
      id = CommonStrings.intern(id);
      return rv = QualifiedName.createNode(e, id);
    } finally {
      if (rv != null) {
        addJavaRefAndCheckForJavadocAnnotations(node, rv);
      }
    }
  }

  public IRNode visitMethod(MethodTree node, CodeContext context) {
    throw new UnsupportedOperationException();
  }

  public IRNode adaptMethod(MethodTree node, String className, TypeKind kind, CodeContext context) {
    final boolean varargs;
    List<? extends VariableTree> params = node.getParameters();
    if (params.size() > 0) {
      VariableTree v = params.get(params.size() - 1);
      String last = v.toString();
      varargs = v.getType().getKind() == Tree.Kind.ARRAY_TYPE && last.contains("...");
      /*
       * TODO is this the best way to figure this out? if (varargs) {
       * System.out.println("Found varargs: "+node); }
       */
    } else {
      varargs = false;
    }

    int mods = adaptModifiers(node.getModifiers());
    if (kind == TypeKind.IFACE || kind == TypeKind.ANNO) {
      mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
      mods = JavaNode.setModifier(mods, JavaNode.ABSTRACT, true);
    }
    if (JavaNode.getModifier(mods, JavaNode.STATIC)) {
      context = new CodeContext(context, mods);
    }
    IRNode rType = adaptType(node.getReturnType(), context);

    IRNode annos = adaptAnnotations(node.getModifiers(), context);
    IRNode[] typs = map(acceptNodes, node.getTypeParameters(), context);
    IRNode[] fmls = map(varargs ? adaptVarargsParameters : adaptParameters, node.getParameters(), context);
    IRNode[] exs = map(adaptTypes, node.getThrows(), context);
    IRNode body = acceptNode(node.getBody(), context);
    if (asBinary && !context.fromAnnotation()) {
      body = CompiledMethodBody.prototype.jjtCreate();
    } else if (body == null) {
      body = NoMethodBody.prototype.jjtCreate();
    } else {
      body = MethodBody.createNode(body);
    }
    // IRNode defVal = acceptNode(node.getDefaultValue());
    if (rType == null) {
      if (className == null) {
        throw new IllegalArgumentException("No class name for constructor");
      }
      IRNode rv = ConstructorDeclaration.createNode(annos, mods, TypeFormals.createNode(typs), className,
          Parameters.createNode(fmls), Throws.createNode(exs), body);
      ReceiverDeclaration.makeReceiverNode(rv);
      return rv;
    }
    String id = node.getName().toString();
    if (context.fromAnnotation()) {
      mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);

      IRNode value;
      Tree t = node.getDefaultValue();
      if (t != null) {
        IRNode v = adaptExpr(t, context);
        value = DefaultValue.createNode(v);
      } else {
        value = NoDefaultValue.prototype.jjtCreate();
      }
      IRNode rv = AnnotationElement.createNode(annos, mods, rType, id, Parameters.createNode(fmls), 
    		                                   Throws.createNode(exs), body, value);
      return rv;
    }
    IRNode rv = MethodDeclaration.createNode(annos, mods, TypeFormals.createNode(typs), rType, id, 
    		                                 Parameters.createNode(fmls), 0, Throws.createNode(exs), body);
    addJavaRefAndCheckForJavadocAnnotations(node, rv);
    createRequiredMethodNodes(JavaNode.isSet(mods, JavaNode.STATIC), rv);
    return rv;
  }

  public IRNode visitMethodInvocation(MethodInvocationTree node, CodeContext context) {
    IRNode[] targs = map(adaptTypes, node.getTypeArguments(), context);
    IRNode[] rawArgs = map(adaptExprs, node.getArguments(), context);
    IRNode args = Arguments.createNode(rawArgs);
    // TODO what about first arg?
    addJavaRefAndCheckForJavadocAnnotations(node, args);
    
    String method = null;
    IRNode object = null;
    if (node.getMethodSelect() instanceof MemberSelectTree) {
      MemberSelectTree ms = (MemberSelectTree) node.getMethodSelect();
      object = adaptExpr(ms.getExpression(), context);
      method = ms.getIdentifier().toString();
      IRNode inner = null;
      if ("this".equals(method)) {
        inner = ThisExpression.prototype.jjtCreate();
      } else if ("super".equals(method)) {
        inner = SuperExpression.prototype.jjtCreate();
      }
      if (inner != null) {
        IRNode call = NonPolymorphicConstructorCall.createNode(inner, args);
        return OuterObjectSpecifier.createNode(object, call);
      }
    } else if (node.getMethodSelect() instanceof IdentifierTree) {
      IdentifierTree it = (IdentifierTree) node.getMethodSelect();
      String id = it.getName().toString();
      if ("this".equals(id)) {
        object = ThisExpression.prototype.jjtCreate();
        return NonPolymorphicConstructorCall.createNode(object, args);
      } else if ("super".equals(id)) {
        object = SuperExpression.prototype.jjtCreate();
        return NonPolymorphicConstructorCall.createNode(object, args);
      }
      // object = ThisExpression.prototype.jjtCreate();
      object = ImplicitReceiver.prototype.jjtCreate();      
      method = id;
      addJavaRefAndCheckForJavadocAnnotations(node, object);
    } else {
      throw new IllegalArgumentException(node.getMethodSelect().getKind().name());
    }
    if (targs.length > 0) {
      return PolymorphicMethodCall.createNode(object, TypeActuals.createNode(targs), method, args);
    }
    method = CommonStrings.pool(method);
    return NonPolymorphicMethodCall.createNode(object, method, args);
  }

  public IRNode visitModifiers(ModifiersTree node, CodeContext context) {
    throw new UnsupportedOperationException();
  }

  public IRNode visitNewArray(NewArrayTree node, CodeContext context) {
    // List<? extends Tree> inits = node.getInitializers();
    // System.out.println(inits);
    IRNode base = adaptType(node.getType(), context);
    IRNode[] dims = map(adaptExprs, node.getDimensions(), context);
    IRNode[] init = map(adaptExprs, node.getInitializers(), context);
    int unalloc = 0;
    if (base == null) {
      if (init.length == 0) {
        return ArrayInitializer.createNode(noNodes);
      }
      // Some kind of ArrayInitializer
      Operator op = JJNode.tree.getOperator(init[0]);
      if (ElementValueArrayInitializer.prototype.includes(op) || edu.cmu.cs.fluid.java.operator.Annotation.prototype.includes(op)) {
        return ElementValueArrayInitializer.createNode(init);
      } else {
        return ArrayInitializer.createNode(init);
      }
    }
    if (dims == null) {
      System.out.println(node);
    } else if (dims.length == 0) {
      unalloc = 1;
    }
    if (init == null) {
      return ArrayCreationExpression.createNode(base, DimExprs.createNode(dims), unalloc, NoArrayInitializer.prototype.jjtCreate());
    } else {
      return ArrayCreationExpression.createNode(base, DimExprs.createNode(dims), unalloc, ArrayInitializer.createNode(init));
    }
  }

  public IRNode visitNewClass(NewClassTree node, CodeContext context) {
    IRNode oos = adaptExpr(node.getEnclosingExpression(), context);
    IRNode id = adaptType(node.getIdentifier(), context);
    IRNode[] tArgs = map(adaptTypes, node.getTypeArguments(), context);
    IRNode[] args = map(adaptExprs, node.getArguments(), context);
    IRNode result;
    if (tArgs.length == 0) {
      result = NonPolymorphicNewExpression.createNode(id, Arguments.createNode(args));
    } else {
      result = PolymorphicNewExpression.createNode(TypeActuals.createNode(tArgs), id, Arguments.createNode(args));
    }
    if (node.getClassBody() != null) {
      IRNode body = makeAnonClassBody(node, context);
      String name = getNextACEName();
      result = AnonClassExpression.createNode(result, body);
      JJNode.setInfo(result, name);
      addJavaRefAndCheckForJavadocAnnotations(node, result);
      createRequiredAnonClassNodes(result);
    }
    if (oos != null) {
      result = OuterObjectSpecifier.createNode(oos, result);
    }
    return result;
  }

  private IRNode makeAnonClassBody(NewClassTree node, CodeContext context) {
    IRNode[] mbrs = map(new AdaptMembers(null, TypeKind.OTHER), node.getClassBody().getMembers(), context);
    IRNode body = ClassBody.createNode(mbrs);
    addJavaRefAndCheckForJavadocAnnotations(node, body);
    return body;
  }

  public IRNode visitOther(Tree node, CodeContext context) {
    throw new UnsupportedOperationException("Unknown tree: " + node.getKind());
  }

  public IRNode visitParameterizedType(ParameterizedTypeTree node, CodeContext context) {
    IRNode base = adaptType(node.getType(), context);
    IRNode[] args = map(adaptTypes, node.getTypeArguments(), context);
    IRNode rv = ParameterizedType.createNode(base, TypeActuals.createNode(args));
    addJavaRefAndCheckForJavadocAnnotations(node, rv);
    return rv;
  }

  public IRNode visitParenthesized(ParenthesizedTree node, CodeContext context) {
    IRNode e = adaptExpr(node.getExpression(), context);
    return ParenExpression.createNode(e);
  }

  public IRNode visitPrimitiveType(PrimitiveTypeTree node, CodeContext context) {
    switch (node.getPrimitiveTypeKind()) {
    case BOOLEAN:
      return BooleanType.prototype.jjtCreate();
    case BYTE:
      return ByteType.prototype.jjtCreate();
    case CHAR:
      return CharType.prototype.jjtCreate();
    case SHORT:
      return ShortType.prototype.jjtCreate();
    case INT:
      return IntType.prototype.jjtCreate();
    case LONG:
      return LongType.prototype.jjtCreate();
    case FLOAT:
      return FloatType.prototype.jjtCreate();
    case DOUBLE:
      return DoubleType.prototype.jjtCreate();
    case VOID:
      return VoidType.prototype.jjtCreate();
    default:
      throw new IllegalArgumentException("Unknown type: " + node.getPrimitiveTypeKind());
    }
  }

  public IRNode visitReturn(ReturnTree node, CodeContext context) {
    IRNode e = adaptExpr(node.getExpression(), context);
    if (e == null) {
      return createVoidReturnStatement();
    }
    return ReturnStatement.createNode(e);
  }

  public IRNode visitSwitch(SwitchTree node, CodeContext context) {
    // To compensate for how javac handles ()
    ParenthesizedTree exp = (ParenthesizedTree) node.getExpression();
    IRNode expr = adaptExpr(exp.getExpression(), context);
    IRNode[] b = map(acceptNodes, node.getCases(), context);
    return SwitchStatement.createNode(expr, SwitchBlock.createNode(b));
  }

  public IRNode visitSynchronized(SynchronizedTree node, CodeContext context) {
    // To compensate for how javac handles ()
    ParenthesizedTree exp = (ParenthesizedTree) node.getExpression();
    IRNode e = adaptExpr(exp.getExpression(), context);
    IRNode b = acceptNode(node.getBlock(), context);
    return SynchronizedStatement.createNode(e, b);
  }

  public IRNode visitThrow(ThrowTree node, CodeContext context) {
    IRNode e = adaptExpr(node.getExpression(), context);
    return ThrowStatement.createNode(e);
  }

  public IRNode visitTry(TryTree node, CodeContext context) {
    IRNode b = acceptNode(node.getBlock(), context);
    IRNode[] c = map(acceptNodes, node.getCatches(), context);
    IRNode f = acceptNode(node.getFinallyBlock(), context);
    if (f == null) {
      f = NoFinally.prototype.jjtCreate();
    } else {
      f = Finally.createNode(f);
    }
    if (node.getResources().size() > 0) {
      IRNode[] resources = map(acceptNodes, node.getResources(), context);
      IRNode resourceRoot = Resources.createNode(resources);
      return TryResource.createNode(resourceRoot, b, CatchClauses.createNode(c), f);
    }
    return TryStatement.createNode(b, CatchClauses.createNode(c), f);
  }

  public IRNode visitTypeCast(TypeCastTree node, CodeContext context) {
    IRNode t = adaptType(node.getType(), context);
    IRNode e = adaptExpr(node.getExpression(), context);
    return CastExpression.createNode(t, e);
  }

  public IRNode visitTypeParameter(TypeParameterTree node, CodeContext context) {
    IRNode[] bounds = map(adaptTypes, node.getBounds(), context);
    if (bounds.length == 0) {
      bounds = new IRNode[1];
      bounds[0] = NamedType.createNode("java.lang.Object");
    }
    String id = node.getName().toString();
    id = CommonStrings.intern(id);
    return TypeFormal.createNode(id, MoreBounds.createNode(bounds));
  }

  public IRNode visitUnary(UnaryTree node, CodeContext context) {
    IRNode op = adaptExpr(node.getExpression(), context);
    switch (node.getKind()) {
    case BITWISE_COMPLEMENT:
      return ComplementExpression.createNode(op);
    case LOGICAL_COMPLEMENT:
      return NotExpression.createNode(op);
    case POSTFIX_DECREMENT:
      return PostDecrementExpression.createNode(op);
    case POSTFIX_INCREMENT:
      return PostIncrementExpression.createNode(op);
    case PREFIX_DECREMENT:
      return PreDecrementExpression.createNode(op);
    case PREFIX_INCREMENT:
      return PreIncrementExpression.createNode(op);
    case UNARY_MINUS:
      return MinusExpression.createNode(op);
    case UNARY_PLUS:
      return PlusExpression.createNode(op);
    default:
      throw new IllegalArgumentException(node.getKind().toString());
    }
  }

  // Currently only used for try-with-resources
  public IRNode visitVariable(VariableTree node, CodeContext context) {
    IRNode annos = adaptAnnotations(node.getModifiers(), context);
    int mods = adaptModifiers(node.getModifiers());
    IRNode type = adaptType(node.getType(), context);
    String id = node.getName().toString();
    IRNode initE = adaptExpr(node.getInitializer(), context);
    IRNode init = Initialization.createNode(initE);
    IRNode vd = VariableDeclarator.createNode(id, 0, init);
    return VariableResource.createNode(annos, mods, type, vd);
  }

  public IRNode adaptField(VariableTree node, String className, TypeKind kind, CodeContext context) {
    int mods = adaptModifiers(node.getModifiers());
    if (kind == TypeKind.IFACE || kind == TypeKind.ANNO) {
      mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
      mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
      mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
    }
    /*
     * TODO Do I need this? else if (kind == TypeKind.ENUM) { mods =
     * JavaNode.setModifier(mods, JavaNode.PUBLIC, true); mods =
     * JavaNode.setModifier(mods, JavaNode.STATIC, true); mods =
     * JavaNode.setModifier(mods, JavaNode.FINAL, true); }
     */
    String id = node.getName().toString();
    if (kind == TypeKind.ENUM && node.getType().toString().endsWith(className) && node.getInitializer() instanceof NewClassTree) {
      // System.out.println("Converting "+node+" to an enum constant");
      return adaptEnumConstant(id, node, context);
    }
    IRNode annos = adaptAnnotations(node.getModifiers(), context);
    IRNode type = adaptType(node.getType(), context);
    IRNode init = adaptExpr(node.getInitializer(), context);
    if (asBinary || init == null) {
      // For compatibility w/ fluid-eclipse
      // if (init == null) {
      init = NoInitialization.prototype.jjtCreate();
    } else {
      init = Initialization.createNode(init);
    }
    IRNode vd = VariableDeclarator.createNode(id, 0, init);

    Tree vdt = node.getInitializer();
    if (vdt == null) {
      vdt = node;
    }
    addJavaRefAndCheckForJavadocAnnotations(vdt, vd);

    IRNode[] vars = new IRNode[] { vd };
    IRNode vdecls = VariableDeclarators.createNode(vars);
    return FieldDeclaration.createNode(annos, mods, type, vdecls);
  }

  private IRNode adaptEnumConstant(final String id, VariableTree node, CodeContext context) {
    // Always non-null
    NewClassTree init = (NewClassTree) node.getInitializer();
    IRNode[] rawArgs = map(adaptExprs, init.getArguments(), context);
    IRNode args = Arguments.createNode(rawArgs);
    ClassTree cbody = init.getClassBody();
    IRNode impliedInit = ImpliedEnumConstantInitialization.prototype.jjtCreate();
    IRNode annos = adaptAnnotations(node.getModifiers(), context);
    final IRNode rv;
    if (cbody != null) {
      IRNode body = makeAnonClassBody(init, context);
      addJavaRefAndCheckForJavadocAnnotations(cbody, body);
      rv = EnumConstantClassDeclaration.createNode(annos, id, impliedInit, args, body);
    } else if (rawArgs.length == 0) {
      rv = SimpleEnumConstantDeclaration.createNode(annos, id, impliedInit);
    } else {
      rv = NormalEnumConstantDeclaration.createNode(annos, id, impliedInit, args);
    }
    JavaNode.setModifiers(rv, JavaNode.PUBLIC | JavaNode.STATIC | JavaNode.FINAL);
    return rv;
  }

  public IRNode visitWhileLoop(WhileLoopTree node, CodeContext context) {
    // To compensate for how javac handles ()
    ParenthesizedTree exp = (ParenthesizedTree) node.getCondition();
    IRNode c = adaptExpr(exp.getExpression(), context);
    IRNode s = acceptNode(node.getStatement(), context);
    return WhileStatement.createNode(c, s);
  }

  public IRNode visitWildcard(WildcardTree node, CodeContext context) {
    IRNode bound = adaptType(node.getBound(), context);
    IRNode rv;
    if (bound == null) {
      rv = WildcardType.prototype.jjtCreate();
      addJavaRefAndCheckForJavadocAnnotations(node, rv);
      return rv;
    }
    switch (node.getKind()) {
    case EXTENDS_WILDCARD:
      rv = WildcardExtendsType.createNode(bound);
      addJavaRefAndCheckForJavadocAnnotations(node, rv);
      return rv;
    case SUPER_WILDCARD:
      rv = WildcardSuperType.createNode(bound);
      addJavaRefAndCheckForJavadocAnnotations(node, rv);
      return rv;
    default:
      throw new UnsupportedOperationException(node.getBound().toString());
    }
  }

  // needed for Java 7
  public IRNode visitUnionType(UnionTypeTree u, CodeContext c) {
    final List<? extends Tree> list = u.getTypeAlternatives();
    IRNode[] alts = new IRNode[list.size()];
    int i = 0;
    for (Tree t : list) {
      alts[i] = adaptType(t, c);
      i++;
    }
    IRNode rv = UnionType.createNode(alts);
    addJavaRefAndCheckForJavadocAnnotations(u, rv);
    return rv;
  }

  // needed for Java 8?
  public IRNode visitAnnotatedType(AnnotatedTypeTree arg0, CodeContext c) {
    throw new UnsupportedOperationException();
  }

  public IRNode visitLambdaExpression(LambdaExpressionTree arg0, CodeContext c) {
    throw new UnsupportedOperationException();
  }

  public IRNode visitMemberReference(MemberReferenceTree arg0, CodeContext c) {
    throw new UnsupportedOperationException();
  }
}
