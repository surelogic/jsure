package com.surelogic.annotation.parse;

import static com.surelogic.common.AnnotationConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;

import com.surelogic.Cast;
import com.surelogic.NonNull;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.JavadocAnnotation;
import com.surelogic.annotation.SimpleAnnotationParsingContext;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.annotation.rules.StandardRules;
import com.surelogic.annotation.rules.TestRules;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;
import com.surelogic.javac.adapter.SourceAdapter;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.ConstantExpressionVisitor;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.Annotation;
import edu.cmu.cs.fluid.java.operator.ArrayInitializer;
import edu.cmu.cs.fluid.java.operator.ElementValueArrayInitializer;
import edu.cmu.cs.fluid.java.operator.ElementValuePair;
import edu.cmu.cs.fluid.java.operator.ElementValuePairs;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initializer;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NormalAnnotation;
import edu.cmu.cs.fluid.java.operator.SingleElementAnnotation;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.IntegerTable;

public class AnnotationVisitor extends Visitor<Integer> {
  public static final String IMPLEMENTATION_ONLY = "implementationOnly";
  public static final String VERIFY = "verify";
  public static final String ALLOW_RETURN = "allowReturn";
  public static final String ALLOW_REF_OBJECT = "allowReferenceObject";
  public static final String ALLOW_READ = "allowRead";
  
  /** Properties */
  public static final String THROUGH = "through";
  
  static final Logger LOG = SLLogger.getLogger("sl.annotation.parse");

  final boolean inEclipse = !IDE.getInstance().getClass().getSimpleName().startsWith("Javac");
  final ITypeEnvironment tEnv;
  final String name;
  TestResult nextResult = null;
  boolean clearResult = true;
  private final boolean allowJavadoc;
  private final ConstantExpressionVisitor constExprVisitor;
  
  public AnnotationVisitor(ITypeEnvironment te, String label) {
    tEnv = te;
    name = label;
    allowJavadoc = allowJavadoc(te);
    constExprVisitor = new ConstantExpressionVisitor(te.getBinder());
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
    if (id.startsWith(PROMISE_PREFIX) || 
       (id.startsWith(JCIP_PREFIX) && (id.endsWith(".GuardedBy") || id.endsWith(".ThreadSafe") || id.endsWith(".Immutable") || id.endsWith(".NotThreadSafe"))) ||
       (id.startsWith(ANDROID_INTERNAL_PREFIX) && (id.endsWith(".GuardedBy") || id.endsWith(".Immutable"))) ||
       (id.startsWith(JAVAX_PREFIX) && (id.endsWith(".Nullable"))) ||
       (id.startsWith(JAVAX_CONCURRENT_PREFIX) && (id.endsWith(".GuardedBy") || id.endsWith(".Immutable") || id.endsWith(".ThreadSafe") || id.endsWith(".NotThreadSafe")))) {
      int lastDot = id.lastIndexOf('.');
      return id.substring(lastDot + 1);
    }
    /* TODO Has 'when' attribute
    else if (id.equals(JAVAX_NONNULL)) {
      return NonNull.class.getSimpleName();
    }
    */
    if (!id.equals("java.lang.Deprecated")) {
      // FIX currently ignoring other annotations
      // System.out.println("Ignoring "+id);
    }
    return null;
  }
  
  // FIX needs more info about where the contents are coming from
  private boolean createPromise(ContextBuilder builder) {
	Context context = builder.build();
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
  
  public class ContextBuilder {
	  final IRNode anno;
	  final String promise;
	  final String contents;
	  /**
	   * Usually pointing to the contents IRNode
	   */
	  final IRNode context;
	  AnnotationSource source;
	  
	  int modifiers = JavaNode.ALL_FALSE;
	  Map<String, String> properties = Collections.emptyMap();	   
      
	  public ContextBuilder(IRNode n, String promise, String contents) {
    	  anno = n;
    	  this.promise = promise;
    	  this.contents = contents;
    	  context = n;
      }
      
	  public ContextBuilder(IRNode n, String promise, IRNode contents) {
		  anno = n;
		  this.promise = promise;
    	  this.contents = extractString(contents);    	  
    	  context = contents;
    	  /*
    	  if (SourceAdapter.includeQuotesInStringLiteral) {
    		  offset++;
    	  }
    	  */
	  }
	  
      /**
       * Defaults to most of the things needed for Java5 annotations
       */
      Context build() {
    	  if (source == null) {
    		  if (TypeUtil.isBinary(anno)) {
    			  source = AnnotationSource.XML;
    		  } else {
    			  source = AnnotationSource.JAVA_5;
    		  }
    	  } 	  
    	  
    	  /* Bad things happen if contents is null */
    	  String c = (contents == null) ? "" : contents;

    	  IAnnotationParseRule<?, ?> r = PromiseFramework.getInstance().getParseDropRule(promise);
    	  if (r == null && source != AnnotationSource.JAVA_5) {
    	      SimpleAnnotationParsingContext.reportError(anno, "Javadoc @annotate '" + promise + "' is unknown -- is it misspelled?");    	      
    	  }
    	  return new Context(source, AnnotationOrigin.DECL, anno, r, c, context, modifiers, properties);
      }

	public ContextBuilder setSrc(AnnotationSource src) {
		source = src;
		return this;
	}

	public ContextBuilder setProps(int mods, Map<String, String> props) {
		modifiers = mods;
		if (props == null) {
			throw new IllegalArgumentException("null props");
		}
		properties = props;
		return this;
	}
  }
  
  class Context extends SimpleAnnotationParsingContext {
    final int mods;
    final Map<String, String> properties;

    Context(AnnotationSource src, AnnotationOrigin origin, IRNode n, IAnnotationParseRule<?, ?> r, String text, IRNode ref, int modifiers,
        Map<String, String> props) {
      super(src, origin, n, r, text, ref);
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
    public void setProperty(String key, String value) {
      properties.put(key, value);
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
      createPromise(new ContextBuilder(node, TestRules.TEST_RESULT, result).setSrc(AnnotationSource.JAVA_5));
    }
  }

  @Override
  public Integer visitMarkerAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise != null) {
      return translate(handleJava5Promise(node, promise));
    }
    return 0;
  }

  private static boolean isPlural(String promise) {
	  return promise.endsWith("s") && PromiseFramework.getInstance().getParseDropRule(promise) == null;
  }
  
  @Override
  public Integer visitSingleElementAnnotation(final IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise == null) {
      // FIX ignoring other annos
      return sum(doAcceptForChildrenWithResults(node));
    }
    final boolean plural = isPlural(promise);
    IRNode value = SingleElementAnnotation.getElt(node);
    Operator op = JJNode.tree.getOperator(value);

    int num = 0;
    if (Initializer.prototype.includes(op)) {
      if (ArrayInitializer.prototype.includes(op)) {
    	// FIX this shouldn't ever happen for our current annotations
        Iteratable<IRNode> it = ArrayInitializer.getInitIterator(value);
        if (it.hasNext()) {
          for (IRNode v : it) {
        	// Treat each as if it's for a separate annotation?
            num += translate(handleJava5Promise(node, promise, v));
          }
        } else {
          // Treat as marker annotation
          num += translate(handleJava5Promise(value, promise));
        }
      } else { //if (StringLiteral.prototype.includes(op) || StringConcat.prototype.includes(op)) {
        num += translate(handleJava5Promise(node, promise, value));
      //} else {
      //  throw new IllegalArgumentException("Unexpected value: " + op.name());      
      }
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
      final boolean plural = isPlural(promise);
      IRNode pairsNode = NormalAnnotation.getPairs(node);
      Iteratable<IRNode> pairs = ElementValuePairs.getPairIterator(pairsNode);
      if (pairs.hasNext()) {
        boolean implOnly = false;
        boolean verify = true;
        boolean allowReturn = false;
        boolean allowRead = false;
        boolean allowReferenceObject = false;
        IRNode contents = null;
        Map<String, String> props = new HashMap<String, String>();
        for (IRNode valuePair : pairs) {
          final String id = ElementValuePair.getId(valuePair);
          if (VALUE_ATTR.equals(id)) {
        	contents = ElementValuePair.getValue(valuePair);       
          } else if (ALLOW_READ.equals(id)) {
            allowRead = extractBoolean(valuePair, allowRead);
          } else if (ALLOW_RETURN.equals(id)) {
            allowReturn = extractBoolean(valuePair, allowReturn);
          } else if (ALLOW_REF_OBJECT.equals(id)) {
        	allowReferenceObject = extractBoolean(valuePair, allowReferenceObject);
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
        ContextBuilder builder; 
        if (contents != null) {
        	if (plural || ElementValueArrayInitializer.prototype.includes(contents)) {
        		return doAccept(contents);
        	}
        	builder = new ContextBuilder(node, promise, contents);
        } else {
        	builder = new ContextBuilder(node, promise, "");
        }
        builder.setProps(convertToModifiers(implOnly, verify, allowReturn, allowRead, 
        		allowReferenceObject), props);
        return translate(handleJava5Promise(builder));                   
      } else {
    	// Basically the same as a marker annotation
        return translate(handleJava5Promise(node, promise));
      }
    }
    return sum(doAcceptForChildrenWithResults(node));
  }

  private String reformatStringArray(IRNode strings) {
    StringBuilder b = new StringBuilder();
    for (IRNode ev : ArrayInitializer.getInitIterator(strings)) {
      if (b.length() != 0) {
        b.append(',');
      }
      b.append(extractString(ev));
    }
    return b.toString();
  }
  
  String extractString(IRNode value) {
	  try {
		  return constExprVisitor.doAccept(value).toString();
	  } catch(NullPointerException e) {
		  return constExprVisitor.doAccept(value).toString();
	  }
  }
  
  String extractString_old(IRNode value) {
	final IBinder b = tEnv.getBinder();
	final IJavaType t = b.getJavaType(value);
	if (tEnv.getStringType().equals(t)) {
		return new ConstantStringExprVisitor(b).doAccept(value);
	}
	else if (t == JavaTypeFactory.booleanType) {
		return new ConstantBooleanExprVisitor(b).doAccept(value).toString();
	}
	final Operator op = JJNode.tree.getOperator(value);
    if (StringLiteral.prototype.includes(op)) {
      String c = StringLiteral.getToken(value);
      if (SourceAdapter.includeQuotesInStringLiteral) {
    	  final boolean quoteStart = c.startsWith("\"");    	  
    	  if (quoteStart && c.endsWith("\"")) {
    		  c = c.substring(1, c.length() - 1);
    		  
    	  }
    	  /*
    	  else if (!quoteStart) {
    		  System.out.println("String literal without quotes");          
    	  }
          */
      }      
      return c;
    }
    else if (StringConcat.prototype.includes(op)) {
      return extractString(StringConcat.getOp1(value)) +
             extractString(StringConcat.getOp2(value));
    }
    else if (FieldRef.prototype.includes(op)) {
      IBinding binding = b.getIBinding(value);
      Operator bop = JJNode.tree.getOperator(binding.getNode());
      if (EnumConstantDeclaration.prototype.includes(bop)) {
    	  // The name is the value
    	  return FieldRef.getId(value);
      }
      /*
      else if (VariableDeclarator.prototype.includes(bop)) {
    	  return extractString(VariableDeclarator.getInit(b.getNode()));
      }
      */
    }
    throw new IllegalStateException("Unexpected expression: "+DebugUnparser.toString(value));
  }

  private static int convertToModifiers(boolean implOnly, boolean verify, boolean allowReturn, boolean allowRead, 
		  boolean allowReferenceObject) {
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
    if (allowReferenceObject) {
      modifiers |= JavaNode.ALLOW_REF_OBJECT;
    }
    return modifiers;
  }

  private boolean extractBoolean(IRNode valuePair, boolean defValue) {
    IRNode value = ElementValuePair.getValue(valuePair);
    Boolean rv = (Boolean) constExprVisitor.doAccept(value);
    return rv.booleanValue();
	/*
    if (TrueExpression.prototype.includes(value)) {
      return true;
    } else if (FalseExpression.prototype.includes(value)) {
      return false;
    }
    return defValue;
    */
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

  private boolean handleJava5Promise(IRNode n, String promise) {
	  return handleJava5Promise(new ContextBuilder(n, promise, ""));
  }
  
  /**
   * Uses the contents as the context node
   */
  private boolean handleJava5Promise(IRNode anno, String promise, IRNode contents) {
	  return handleJava5Promise(new ContextBuilder(anno, promise, contents));
  }
  
  private boolean handleJava5Promise(ContextBuilder builder) {
    checkForTestResult(builder.anno);
    return createPromise(builder);
  }

  public boolean handleImplicitPromise(IRNode node, String promise, String c, Map<String, String> props) {
	  return handleXMLPromise(node, promise, c, props, JavaNode.IMPLICIT);
  }
	  
  public boolean handleXMLPromise(IRNode node, String promise, String c, Map<String, String> props, int mods) {
    /*
     * if (c.contains("CopyOn")) {
     * System.out.println("Visiting @"+promise+" "+c); }
     */
	final boolean implOnly    = "true".equals(props.get(AnnotationVisitor.IMPLEMENTATION_ONLY));
	final String rawVerify    = props.get(AnnotationVisitor.VERIFY);
	final boolean verify      = rawVerify == null || "true".equals(rawVerify);
	final boolean allowReturn = "true".equals(props.get(AnnotationVisitor.ALLOW_RETURN));
	final boolean allowRead   = "true".equals(props.get(AnnotationVisitor.ALLOW_READ));	
	final boolean allowReferenceObject   = "true".equals(props.get(AnnotationVisitor.ALLOW_REF_OBJECT));		
	final int finalMods       = mods | 
	                            convertToModifiers(implOnly, verify, allowReturn, allowRead, allowReferenceObject);
    return createPromise(new ContextBuilder(node, capitalize(promise), c) 
                              .setSrc(AnnotationSource.XML).setProps(finalMods, props));
  }

  private boolean handleJavadocPromise(IRNode decl, JavadocAnnotation javadocAnnotation) {
    if (!javadocAnnotation.isValid()) {
      SimpleAnnotationParsingContext.reportError(decl, "Javadoc @annotate matches no known JSure promise: "
          + javadocAnnotation.getRawCommentText());
      return false;
    }
    final String annotation = javadocAnnotation.getAnnotation();
    if (!javadocAnnotation.hasArgument()) {
      return handleSimpleJavadocPromise(decl, annotation);
    }
    final String argument = javadocAnnotation.getArgument().trim();
    if (!(argument.startsWith("\"") && argument.endsWith("\""))) {
      SimpleAnnotationParsingContext.reportError(decl, "Javadoc @annotate " + annotation + "(" + argument
          + ") : JSure only handles a single string as an argument to any Javadoc promise");
      return false;
    }
    return createPromise(new ContextBuilder(decl, annotation, argument.substring(1, argument.length() - 1))
    		.setSrc(AnnotationSource.JAVADOC));
  }

  /**
   * Assumes that text looks like Foo (e.g. no parameters)
   * 
   * TODO this code doesn't match the Javadoc
   */
  private boolean handleSimpleJavadocPromise(IRNode decl, String text) {
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
        SimpleAnnotationParsingContext.reportError(decl, msg);
        return false;
      }
    }
    return createPromise(new ContextBuilder(decl, tag, "").setSrc(AnnotationSource.JAVADOC));
  }
  
  @Override
  public Integer visitMethodCall(IRNode node) {
	  Integer count = super.visitMethodCall(node);

	  // Special case for Cast
	  final IBinding mb = tEnv.getBinder().getIBinding(node);
	  if (mb.getContextType().getName().equals(Cast.class.getName())) {
		  return handleJava5Promise(new ContextBuilder(node, NonNullRules.CAST, MethodDeclaration.getId(mb.getNode()))) ? 
				 IntegerTable.incrInteger(count) : count;
	  }
	  return count;
  }
}
