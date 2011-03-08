package com.surelogic.annotation.parse;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.*;

import com.surelogic.aast.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.rules.*;
import com.surelogic.annotation.test.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.comment.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

public class AnnotationVisitor extends Visitor<Integer> {
	public static final String IMPLEMENTATION_ONLY = "implementationOnly";
	public static final String VERIFY = "verify";
	
	static final Logger LOG = SLLogger.getLogger("sl.annotation.parse");
	private static final String promisePrefix = "com.surelogic.";
	private static final String jcipPrefix = "net.jcip.annotations.";

	// private static final boolean allowJavadoc = true;
	public static final boolean onlyUseAnnotate = true;

	final boolean inEclipse = !IDE.getInstance().getClass().getSimpleName()
			.startsWith("Javac");
	final ITypeEnvironment tEnv;
	final String name;
	TestResult nextResult = null;
	boolean clearResult = true;

	public AnnotationVisitor(ITypeEnvironment te, String label) {
		tEnv = te;
		name = label;
	}

	public static boolean allowJavadoc(ITypeEnvironment te) {
		final IDE ide = IDE.getInstance();
		return ide.getBooleanPreference(IDEPreferences.ALLOW_JAVADOC_ANNOS)
				|| te.getMajorJavaVersion() < 5; // project is Java 1.4 or below
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
			IJavaDeclaredType type = (IJavaDeclaredType) tEnv.getBinder()
					.getJavaType(anno);
			id = JavaNames.getQualifiedTypeName(type);
		}
		if (id.startsWith(promisePrefix)
				|| (id.startsWith(jcipPrefix) && (id.endsWith("ThreadSafe") || id
						.endsWith(".Immutable")))) {
			int lastDot = id.lastIndexOf('.');
			return id.substring(lastDot + 1);
		}
		if (!id.equals("java.lang.Deprecated")) {
			// FIX currently ignoring other annotations
			// System.out.println("Ignoring "+id);
		}
		return null;
	}

	private Context makeContext(IRNode node, String promise, String c,
			AnnotationSource src, int offset) {
		return makeContext(node, promise, c, src, offset, false, true);
	}
	
	private Context makeContext(IRNode node, String promise, String c,
			AnnotationSource src, int offset, boolean implOnly, boolean verify) {
		/* Bad things happen if contents is null */
		String contents = (c == null) ? "" : c;
		
		IAnnotationParseRule<?, ?> r = 
			PromiseFramework.getInstance().getParseDropRule(promise);
		
		return new Context(src, node, r, contents, offset, implOnly, verify);
	}
	
	// FIX needs more info about where the contents are coming from
	private boolean createPromise(Context context) {
		try {
			if (context.getRule() != null) {	
				// System.out.println("Got "+promise+" : "+contents);
				TestResult.setPromise(nextResult, context.getRule().name(), context.getAllText());
				
				context.getRule().parse(context, context.getAllText());
				return context.createdAAST();
			} else {
				// FIX throw new Error("No rule for "+promise);
				// System.out.println("No rule for "+promise);
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
		
		Context(AnnotationSource src, IRNode n, IAnnotationParseRule<?, ?> r,
				String text, int offset, boolean implOnly, boolean verify) {
			super(src, n, r, text, offset);
			
			int modifiers = JavaNode.ALL_FALSE;
			if (implOnly) {
				modifiers |= JavaNode.IMPLEMENTATION_ONLY;
			}
			if (!verify) {
				modifiers |= JavaNode.NO_VERIFY;
			}
			mods = modifiers;
		}
		@Override
		public int getModifiers() {
			return mods;
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
			createPromise(makeContext(node, TestRules.TEST_RESULT, result,
					AnnotationSource.JAVA_5, -1));
		}
	}

	@Override
	public Integer visitMarkerAnnotation(IRNode node) {
		String promise = mapToPromiseName(node);
		if (promise != null) {
			return translate(handleJava5Promise(node, promise, false, true));
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
						num += translate(handleJava5Promise(node, v, promise,
								StringLiteral.getToken(v)));
					}
				} else {
					num += translate(handleJava5Promise(node, value, promise,
							""));
				}
			} else if (StringLiteral.prototype.includes(op)) {
				num += translate(handleJava5Promise(node, value, promise,
						StringLiteral.getToken(value)));
			} else
				throw new IllegalArgumentException("Unexpected value: "
						+ op.name());
		} else {
			if (!plural) {
				throw new Error(promise + " contains Annotations: "
						+ DebugUnparser.toString(value));
			}
			if (ElementValueArrayInitializer.prototype.includes(op)) {
				num += sum(doAcceptForChildrenWithResults(value));
			} else if (Annotation.prototype.includes(op)) {
				num += doAccept(value);
			} else
				throw new IllegalArgumentException("Unexpected value: "
						+ op.name());
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
			Iteratable<IRNode> pairs = ElementValuePairs
					.getPairIterator(pairsNode);
			if (pairs.hasNext()) {
				boolean implOnly = false;
				boolean verify = true;
				for (IRNode valuePair : pairs) {
					final String id = ElementValuePair.getId(valuePair);
					if ("value".equals(id)) {
						IRNode value = ElementValuePair.getValue(valuePair);
						if (StringLiteral.prototype.includes(value)) {
							return translate(handleJava5Promise(node, value,
									promise, StringLiteral.getToken(value)));
						}
					}
					else if (IMPLEMENTATION_ONLY.equals(id)) {
						implOnly = extractBoolean(valuePair, implOnly);
					}
					else if (VERIFY.equals(id)) {
						verify = extractBoolean(valuePair, verify);
					}
				}
				return translate(handleJava5Promise(node, promise, implOnly, verify));
			} else {
				return translate(handleJava5Promise(node, promise, false, true));
			}
			//throw new Error("A NormalAnnotation in a SL package?!?");
		}
		return 0;
	}

	private boolean extractBoolean(IRNode valuePair, boolean defValue) {
		IRNode value = ElementValuePair.getValue(valuePair);
		if (TrueExpression.prototype.includes(value)) {
			return true;
		}
		return defValue;
	}
	
	@Override
	public Integer visitAnnotation(IRNode node) {
		throw new Error("Unknown Annotation type: "
				+ JJNode.tree.getOperator(node).name());
	}

	@Override
	public Integer visitBlockStatement(IRNode node) {
		final int num = checkForBlockComment(node);
		return num + super.visitBlockStatement(node);
	}

	@Override
	public Integer visitFieldDeclaration(IRNode node) {
		ISrcRef ref = JavaNode.getSrcRef(node);
		if (ref == null) {
			return super.visitVariableDeclList(node);
		}
		final int num = checkForJavadoc(node, ref);
		return num + super.visitVariableDeclList(node);
	}

	@Override
	public Integer visitDeclaration(IRNode node) {
		ISrcRef ref = JavaNode.getSrcRef(node);
		if (ref == null) {
			return super.visitDeclaration(node);
		}
		final int num = checkForJavadoc(node, ref);
		return num + super.visitDeclaration(node);
	}

	private int checkForBlockComment(IRNode node) {
		final String comment = JavaNode.getCommentOrNull(node);
		if (comment != null && comment.length() != 0) {
			// Trim comment bits
			int start = comment.indexOf('@');
			if (start >= 0) {
				int end = comment.length();
				if (comment.endsWith("*/")) {
					end = end - 2;
				}
				final ISrcRef ref = JavaNode.getSrcRef(node);
				return translate(handleJavadocPromise(node,
						comment.substring(start, end), ref.getOffset()));
			}
		}
		return 0;
	}

	private int checkForJavadoc(IRNode node, ISrcRef ref) {
		if (!allowJavadoc(tEnv)) {
			return 0;
		}
		IJavadocElement elt = ref.getJavadoc();
		if (elt != null) {
			for (Object o : elt) {
				if (o instanceof IJavadocTag) {
					return handleJavadocTag(node, (IJavadocTag) o);
				}
			}
			ref.clearJavadoc();
		}
		return 0;
	}

	private int handleJavadocTag(IRNode decl, IJavadocTag tag) {
		if (tag.getTag() == null) {
			return 0; // Leading text
		}
		if (onlyUseAnnotate && !"annotate".equals(tag.getTag())) {
			return 0; // ignore other tags
		}
		int num = 0;
		String contents = null;
		for (Object o : tag) {
			if (o instanceof String) {
				if (contents != null) {
					LOG.fine("New contents: " + o);
				}
				contents = o.toString();
			} else if (o instanceof IJavadocTag) {
				num += handleJavadocTag(decl, (IJavadocTag) o);
			} else {
				System.out.println("Unknown: " + o);
			}
		}

		if (onlyUseAnnotate) {
			num += translate(handleJavadocPromise(decl, contents,
					tag.getOffset()));
		} else {
			num += translate(createPromise(makeContext(decl, capitalize(tag.getTag()),
					contents, AnnotationSource.JAVADOC, tag.getOffset())));
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

	public boolean handleJava5Promise(IRNode node, String promise, boolean implOnly, boolean verify) {
		return handleJava5Promise(node, node, promise, "", implOnly, verify);
	}

	public boolean handleJava5Promise(IRNode anno, IRNode here, String promise,
			String c) {
		return handleJava5Promise(anno, here, promise, c, false, true);
	}
	
	public boolean handleJava5Promise(IRNode anno, IRNode here, String promise,
			String c, boolean implOnly, boolean verify) {
		checkForTestResult(anno);

		ISrcRef src = JavaNode.getSrcRef(here);
		int offset = src == null ? 0 : src.getOffset();
		if (c.startsWith("\"") && c.endsWith("\"")) {
			c = c.substring(1, c.length() - 1);
		}
		/*
		 * if (src != null) {
		 * System.out.println("Handling promise: "+promise+' '+c); }
		 */
		return createPromise(makeContext(here, promise, c, AnnotationSource.JAVA_5, offset, implOnly, verify));
	}

	public boolean handleXMLPromise(IRNode node, String promise, String c, boolean implOnly, boolean verify) {
		return createPromise(makeContext(node, capitalize(promise), c,
				AnnotationSource.XML, Integer.MAX_VALUE, implOnly, verify));
	}

	/**
	 * Assumes that text looks like Foo("...")
	 */
	private boolean handleJavadocPromise(IRNode decl, String text, int offset) {
		// Test result?
		final int startContents = text.indexOf("(\"");
		final int endContents = text.lastIndexOf("\")");
		if (startContents < 0 && endContents < 0) {
			return handleSimpleJavadocPromise(decl, text, offset);
		}
		if (startContents < 0 || endContents < 0) {
			SimpleAnnotationParsingContext.reportError(decl, offset,
					"Syntax not matching Foo(\"...\"): " + text);
			return false;
		}
		// Check if the rest is whitespace
		for (int i = endContents + 2; i < text.length(); i++) {
			if (!Character.isWhitespace(text.charAt(i))) {
				SimpleAnnotationParsingContext.reportError(decl, offset,
						"Non-whitespace after annotation: " + text);
				return false;
			}
		}
		final int start = text.startsWith("@") ? 1 : 0;
		final String tag = text.substring(start, startContents).trim();
		final String contents = text.substring(startContents + 2, endContents);
		// System.out.println("Trying to parse: "+tag+" -- "+contents);
		return createPromise(makeContext(decl, tag, contents, AnnotationSource.JAVADOC,
				offset));
	}

	/**
	 * Assumes that text looks like Foo (e.g. no parameters)
	 */
	private boolean handleSimpleJavadocPromise(IRNode decl, String text,
			int offset) {
		String tag = text.trim();
		if (tag.startsWith("@")) {
			tag = tag.substring(1).trim();
		}
		// Check if legal identifier
		boolean first = true;
		for (int i = 0; i < tag.length(); i++) {
			final char ch = tag.charAt(i);
			final boolean legal = first ? Character.isJavaIdentifierStart(ch)
					: Character.isJavaIdentifierPart(ch);
			first = false;
			if (!legal) {
				String msg;
				if (tag.indexOf('(') >= 0 || tag.lastIndexOf(')') >= 0) {
					msg = "Syntax not matching Foo(\"...\"): " + text;
				} else {
					msg = "Not a legal annotation name: " + text;
				}
				SimpleAnnotationParsingContext.reportError(decl, offset, msg);
				return false;
			}
		}
		return createPromise(makeContext(decl, tag, "", AnnotationSource.JAVADOC, offset));
	}
}
