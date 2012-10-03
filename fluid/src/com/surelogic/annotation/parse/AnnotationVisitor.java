package com.surelogic.annotation.parse;

import static com.surelogic.common.AnnotationConstants.JCIP_PREFIX;
import static com.surelogic.common.AnnotationConstants.PROMISE_PREFIX;
import static com.surelogic.common.AnnotationConstants.VALUE_ATTR;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.JavadocAnnotation;
import com.surelogic.annotation.SimpleAnnotationParsingContext;
import com.surelogic.annotation.rules.StandardRules;
import com.surelogic.annotation.rules.TestRules;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.Annotation;
import edu.cmu.cs.fluid.java.operator.ArrayInitializer;
import edu.cmu.cs.fluid.java.operator.ElementValueArrayInitializer;
import edu.cmu.cs.fluid.java.operator.ElementValuePair;
import edu.cmu.cs.fluid.java.operator.ElementValuePairs;
import edu.cmu.cs.fluid.java.operator.FalseExpression;
import edu.cmu.cs.fluid.java.operator.Initializer;
import edu.cmu.cs.fluid.java.operator.NormalAnnotation;
import edu.cmu.cs.fluid.java.operator.SingleElementAnnotation;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.TrueExpression;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

public class AnnotationVisitor extends Visitor<Integer> {
  public static final String IMPLEMENTATION_ONLY = "implementationOnly";
  public static final String VERIFY = "verify";
  public static final String ALLOW_RETURN = "allowReturn";
  public static final String ALLOW_READ = "allowRead";
  public static final String UPTO = "upTo";

  static final Logger LOG = SLLogger.getLogger("sl.annotation.parse");

  final boolean inEclipse = !IDE.getInstance().getClass().getSimpleName().startsWith("Javac");
  final ITypeEnvironment tEnv;
  final String name;
  TestResult nextResult = null;
  boolean clearResult = true;
  private final boolean allowJavadoc;

  public AnnotationVisitor(ITypeEnvironment te, String label) {
    tEnv = te;
    name = label;
    allowJavadoc = allowJavadoc(te);
  }

  public static boolean allowJavadoc(ITypeEnvironment te) {
    final IDE ide = IDE.getInstance();
    /*
     * Check if project is Java 1.4 or below
     */
    return ide.getBooleanPreference(IDEPreferences.ALLOW_JAVADOC_ANNOS) || te.getMajorJavaVersion() < 5;
  }

  public ITypeEnvironment getTypeEnv() {
    return tEnv;
  }

  @Override
  public Integer visit(IRNode node) {
    return sum(doAcceptForChildrenWithResults(node));
  }

  private Integer sum(List<Integer> ints) {
    int sum = 0;
    for (Integer i : ints) {
      sum += i;
    }
    return sum;
  }

  private int translate(boolean rv) {
    return rv ? 1 : 0;
  }

  /**
   * @return The simple name of the SL annotation (capitalized)
   */
  private String mapToPromiseName(IRNode anno) {
    String id;
    if (inEclipse) {
      // Already fully qualified
      id = Annotation.getId(anno);
    } else {
      IJavaDeclaredType type = (IJavaDeclaredType) tEnv.getBinder().getJavaType(anno);
      id = JavaNames.getQualifiedTypeName(type);
    }
    if (id.startsWith(PROMISE_PREFIX) || (id.startsWith(JCIP_PREFIX) && (id.endsWith("ThreadSafe") || id.endsWith(".Immutable")))) {
      int lastDot = id.lastIndexOf('.');
      return id.substring(lastDot + 1);
    }
    if (!id.equals("java.lang.Deprecated")) {
      // FIX currently ignoring other annotations
      // System.out.println("Ignoring "+id);
    }
    return null;
  }

  private Context makeContext(IRNode node, String promise, String c, AnnotationSource src, int offset) {
    return makeContext(node, promise, c, src, offset, JavaNode.ALL_FALSE, Collections.<String, String> emptyMap());
  }

  private Context makeContext(IRNode node, String promise, String c, AnnotationSource src, int offset, int modifiers,
      Map<String, String> props) {
    /* Bad things happen if contents is null */
    String contents = (c == null) ? "" : c;

    IAnnotationParseRule<?, ?> r = PromiseFramework.getInstance().getParseDropRule(promise);
    Context context = new Context(src, node, r, contents, offset, modifiers, props);
    if (r == null && context.getSourceType() != AnnotationSource.JAVA_5) {
      SimpleAnnotationParsingContext.reportError(node, offset, "Javadoc @annotate '" + promise + "' is invalid");
    }
  	return context;
  }

  // FIX needs more info about where the contents are coming from
  private boolean createPromise(Context context) {
    try {
      if (context.getRule() != null) {
        // System.out.println("Got "+promise+" : "+contents);
        TestResult.setPromise(nextResult, context.getRule().name(), context.getAllText());

        context.getRule().parse(context, context.getAllText());
        return context.createdAAST();
      }
    } catch (Exception e) {
      if (e instanceof RecognitionException) {
        System.err.println(e.getMessage());
      } else {
        LOG.log(Level.WARNING, "Unable to create promise", e);
      }
    } finally {
      if (clearResult) {
        // System.out.println("Clearing result");
        nextResult = null;
      } else {
        clearResult = true;
      }
    }
    return false;
  }

  class Context extends SimpleAnnotationParsingContext {
    final int mods;
    final Map<String, String> properties;

    Context(AnnotationSource src, IRNode n, IAnnotationParseRule<?, ?> r, String text, int offset, int modifiers,
        Map<String, String> props) {
      super(src, n, r, text, offset);
      mods = modifiers;
      properties = new HashMap<String, String>(props);
    }

    @Override
    public int getModifiers() {
      return mods;
    }

    @Override
    public String getProperty(String key) {
      return properties.get(key);
    }

    @Override
    protected String getName() {
      return name;
    }

    @Override
    public TestResult getTestResult() {
      return nextResult;
    }

    @Override
    public void setTestResultForUpcomingPromise(TestResult r) {
      if (r == null) {
        clearTestResult();
        return;
      }
      /*
       * if (nextResult == r) { System.out.println("Same TestResult"); }
       * System.out.println("Set to "+r.hashCode());
       */
      nextResult = r;
      clearResult = false;
    }

    @Override
    public void clearTestResult() {
      /*
       * if (nextResult != null) {
       * System.out.println("Cleared "+nextResult.hashCode()); } else {
       * System.out.println("Already cleared"); }
       */
      nextResult = null;
      clearResult = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.surelogic.annotation.SimpleAnnotationParsingContext#postAASTCreate
     * (com.surelogic.aast.AASTRootNode)
     */
    @Override
    protected void postAASTCreate(AASTRootNode root) {
      // Nothing to do
    }
  }

  private void checkForTestResult(IRNode node) {
    String result = JavaNode.getComment(node);
    if (result != null && result != "" && result.startsWith("/*")) { // /**/
      // minimum
      // trim off the ending */
      result = result.substring(0, result.length() - 2);
      createPromise(makeContext(node, TestRules.TEST_RESULT, result, AnnotationSource.JAVA_5, -1));
    }
  }

  @Override
  public Integer visitMarkerAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise != null) {
      return translate(handleJava5Promise(node, promise, JavaNode.ALL_FALSE, Collections.<String, String> emptyMap()));
    }
    return 0;
  }

  @Override
  public Integer visitSingleElementAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise == null) {
      // FIX ignoring other annos
      return 0;
    }
    boolean plural = promise.endsWith("s");
    IRNode value = SingleElementAnnotation.getElt(node);
    Operator op = JJNode.tree.getOperator(value);

    int num = 0;
    if (Initializer.prototype.includes(op)) {
      /*
       * Not true for @starts if (plural) { throw new
       * Error(promise+" doesn't contains Annotations: "
       * +DebugUnparser.toString(value)); }
       */
      // Should be a String?
      if (ArrayInitializer.prototype.includes(op)) {
        Iteratable<IRNode> it = ArrayInitializer.getInitIterator(value);
        if (it.hasNext()) {
          for (IRNode v : it) {
            num += translate(handleJava5Promise(node, v, promise, removeQuotes(StringLiteral.getToken(v))));
          }
        } else {
          num += translate(handleJava5Promise(node, value, promise, ""));
        }
      } else if (StringLiteral.prototype.includes(op)) {
        num += translate(handleJava5Promise(node, value, promise, removeQuotes(StringLiteral.getToken(value))));
      } else
        throw new IllegalArgumentException("Unexpected value: " + op.name());
    } else {
      if (!plural) {
        throw new Error(promise + " contains Annotations: " + DebugUnparser.toString(value));
      }
      if (ElementValueArrayInitializer.prototype.includes(op)) {
        num += sum(doAcceptForChildrenWithResults(value));
      } else if (Annotation.prototype.includes(op)) {
        num += doAccept(value);
      } else
        throw new IllegalArgumentException("Unexpected value: " + op.name());
    }
    return num;
  }

  @Override
  public Integer visitNormalAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise != null) {
      // We should never have any of these
      // but we might want to convert other ppl's into ours

      // Assume that we only car
      IRNode pairsNode = NormalAnnotation.getPairs(node);
      Iteratable<IRNode> pairs = ElementValuePairs.getPairIterator(pairsNode);
      if (pairs.hasNext()) {
        boolean implOnly = false;
        boolean verify = true;
        boolean allowReturn = false;
        boolean allowRead = false;
        String contents = "";
        Map<String, String> props = new HashMap<String, String>();
        for (IRNode valuePair : pairs) {
          final String id = ElementValuePair.getId(valuePair);
          if (VALUE_ATTR.equals(id)) {
            contents = extractStringFromPair(valuePair);
          } else if (ALLOW_READ.equals(id)) {
            allowRead = extractBoolean(valuePair, allowRead);
          } else if (ALLOW_RETURN.equals(id)) {
            allowReturn = extractBoolean(valuePair, allowReturn);
          } else if (IMPLEMENTATION_ONLY.equals(id)) {
            implOnly = extractBoolean(valuePair, implOnly);
          } else if (VERIFY.equals(id)) {
            verify = extractBoolean(valuePair, verify);
          } else {
            IRNode val = ElementValuePair.getValue(valuePair);
            if (ArrayInitializer.prototype.includes(val)) {
              props.put(id, reformatStringArray(val));
            } else {
              props.put(id, extractString(val));
            }
          }
        }
        return translate(handleJava5Promise(node, node, promise, contents,
            convertToModifiers(implOnly, verify, allowReturn, allowRead), props));
      } else {
        return translate(handleJava5Promise(node, promise, JavaNode.ALL_FALSE, Collections.<String, String> emptyMap()));
      }
      // throw new Error("A NormalAnnotation in a SL package?!?");
    }
    return 0;
  }

  private static String removeQuotes(String c) {
    if (c.startsWith("\"") && c.endsWith("\"")) {
      c = c.substring(1, c.length() - 1);
    }
    return c;
  }

  private static String reformatStringArray(IRNode strings) {
    StringBuilder b = new StringBuilder();
    for (IRNode ev : ArrayInitializer.getInitIterator(strings)) {
      if (b.length() != 0) {
        b.append(',');
      }
      b.append(extractString(ev));
    }
    return b.toString();
  }

  private static String extractStringFromPair(IRNode valuePair) {
    IRNode value = ElementValuePair.getValue(valuePair);
    return extractString(value);
  }

  private static String extractString(IRNode value) {
    if (StringLiteral.prototype.includes(value)) {
      String c = StringLiteral.getToken(value);
      return removeQuotes(c);
    }
    return "";
  }

  public static int convertToModifiers(boolean implOnly, boolean verify, boolean allowReturn, boolean allowRead) {
    int modifiers = JavaNode.ALL_FALSE;
    if (implOnly) {
      modifiers |= JavaNode.IMPLEMENTATION_ONLY;
    }
    if (!verify) {
      modifiers |= JavaNode.NO_VERIFY;
    }
    if (allowReturn) {
      modifiers |= JavaNode.ALLOW_RETURN;
    }
    if (allowRead) {
      modifiers |= JavaNode.ALLOW_READ;
    }
    return modifiers;
  }

  private static boolean extractBoolean(IRNode valuePair, boolean defValue) {
    IRNode value = ElementValuePair.getValue(valuePair);
    if (TrueExpression.prototype.includes(value)) {
      return true;
    } else if (FalseExpression.prototype.includes(value)) {
      return false;
    }
    return defValue;
  }

  @Override
  public Integer visitAnnotation(IRNode node) {
    throw new Error("Unknown Annotation type: " + JJNode.tree.getOperator(node).name());
  }

  @Override
  public Integer visitFieldDeclaration(IRNode node) {
    final int num = checkForJavadoc(node);
    return num + super.visitVariableDeclList(node);
  }

  @Override
  public Integer visitDeclaration(IRNode node) {
    final int num = checkForJavadoc(node);
    return num + super.visitDeclaration(node);
  }

  private int checkForJavadoc(IRNode node) {
    if (!allowJavadoc) {
      return 0;
    }
    final List<JavadocAnnotation> elt = JavaNode.getJavadocAnnotations(node);
    int num = 0;
    for (JavadocAnnotation javadocAnnotation : elt) {
      num += translate(handleJavadocPromise(node, javadocAnnotation));
    }
    return num;
  }

  public static String capitalize(String tag) {
    if (tag.length() <= 0) {
      return tag;
    }
    char first = tag.charAt(0);
    if (Character.isLowerCase(first)) {
      return Character.toUpperCase(first) + tag.substring(1);
    }
    return tag;
  }

  public boolean handleJava5Promise(IRNode node, String promise, int modifiers, Map<String, String> props) {
    return handleJava5Promise(node, node, promise, "", modifiers, props);
  }

  public boolean handleJava5Promise(IRNode anno, IRNode here, String promise, String c) {
    return handleJava5Promise(anno, here, promise, c, JavaNode.ALL_FALSE, Collections.<String, String> emptyMap());
  }

  public boolean handleJava5Promise(IRNode anno, IRNode here, String promise, String c, int modifiers, Map<String, String> props) {
    checkForTestResult(anno);

    ISrcRef src = JavaNode.getSrcRef(here);
    int offset = src == null ? 0 : src.getOffset();
    /*
     * if (src != null) {
     * System.out.println("Handling promise: "+promise+' '+c); }
     */
    AnnotationSource from;
    if (TypeUtil.isBinary(here)) {
      from = AnnotationSource.XML;
    } else {
      from = AnnotationSource.JAVA_5;
    }
    return createPromise(makeContext(here, promise, c, from, offset, modifiers, props));
  }

  public boolean handleXMLPromise(IRNode node, String promise, String c, int modifiers, Map<String, String> props) {
    /*
     * if (c.contains("CopyOn")) {
     * System.out.println("Visiting @"+promise+" "+c); }
     */
    return createPromise(makeContext(node, capitalize(promise), c, AnnotationSource.XML, Integer.MAX_VALUE, modifiers, props));
  }

  private boolean handleJavadocPromise(IRNode decl, JavadocAnnotation javadocAnnotation) {
    final int offset = javadocAnnotation.getOffset();
    if (!javadocAnnotation.isValid()) {
      SimpleAnnotationParsingContext.reportError(decl, offset, "Javadoc @annotate matches no known JSure promise: "
          + javadocAnnotation.getRawCommentText());
      return false;
    }
    final String annotation = javadocAnnotation.getAnnotation();
    if (!javadocAnnotation.hasArgument()) {
      return handleSimpleJavadocPromise(decl, annotation, offset);
    }
    final String argument = javadocAnnotation.getArgument().trim();
    if (!(argument.startsWith("\"") && argument.endsWith("\""))) {
      SimpleAnnotationParsingContext.reportError(decl, offset, "Javadoc @annotate " + annotation + "(" + argument
          + ") : JSure only handles a single string as an argument to any Javadoc promise");
      return false;
    }
    return createPromise(makeContext(decl, annotation, argument.substring(1, argument.length() - 1), AnnotationSource.JAVADOC,
        offset));
  }

  /**
   * Assumes that text looks like Foo (e.g. no parameters)
   */
  private boolean handleSimpleJavadocPromise(IRNode decl, String text, int offset) {
    String tag = text.trim();
    if (tag.startsWith("@")) {
      tag = tag.substring(1).trim();
    }
    // Check if legal identifier
    boolean first = true;
    for (int i = 0; i < tag.length(); i++) {
      final char ch = tag.charAt(i);
      final boolean legal = first ? Character.isJavaIdentifierStart(ch) : Character.isJavaIdentifierPart(ch);
      first = false;
      if (!legal) {
        String msg;
        if (tag.indexOf('(') >= 0 || tag.lastIndexOf(')') >= 0) {
          msg = "Syntax not matching Foo(\"...\"): " + text;
        } else {
          if (ch == ' ' && StandardRules.ignore(tag.substring(0, i))) {
            // It's a normal Javadoc tag
            return false;
          }
          msg = "Not a legal annotation name: " + text;
        }
        SimpleAnnotationParsingContext.reportError(decl, offset, msg);
        return false;
      }
    }
    return createPromise(makeContext(decl, tag, "", AnnotationSource.JAVADOC, offset));
  }
}
