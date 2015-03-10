/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/adapter/AbstractAdapter.java,v 1.9 2008/09/08 14:45:02 chance Exp $*/
package edu.cmu.cs.fluid.java.adapter;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.SLUtility;
import com.surelogic.javac.Util;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.JavaRewrite;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclarationInInit;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.unparse.TokenArray;
import edu.cmu.cs.fluid.unparse.TokenView;

public class AbstractAdapter {
	protected interface Function<T> {
		// Can return null
		IRNode call(T t, CodeContext context, int i, int n);
	}
	protected static abstract class AbstractFunction<T> implements Function<T> {
		@Override
    public final IRNode call(T t, CodeContext context, int i, int n) {
			return call(t, context);
		}
		public abstract IRNode call(T t, CodeContext context);
	}
	
	public static final SyntaxTreeInterface tree = JJNode.tree;
	public static final IRNode[] noNodes = JavaGlobals.noNodes;
	
	/**
	 * Logger for this class
	 */
	protected final Logger LOG;
	
	protected AbstractAdapter(Logger log) {
		LOG = log;
	}
	
	/***************************************************************************
	 * Debug code
	 **************************************************************************/

	/**
	 * For debugUnparse() to discern the first call from subsequent calls.
	 */
	protected boolean f_debugUnparsefirstCall = true;

	/**
	 * For debugUnparse() to unparse an IR representation of a Java file.
	 */
	protected JavaFmtStream f_debugUnparserUnparser;

	/**
	 * Outputs a unparsed version of the Java file we loaded into the Fluid IR.
	 * This is ouput via log4j and since it is expensive it is guarded to be
	 * skipped entirely if debug-level output is not needed.
	 * 
	 * @param irNode
	 *            The top level IR node for the Java file.
	 * @param name
	 *            The name of the Java file to print at the top of the ouput.
	 */
	protected final void debugUnparse(IRNode irNode, String name) {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Adapting " + name);

			// Only reachable if also INFO
			if (LOG.isLoggable(Level.FINER)) {
				if (f_debugUnparsefirstCall) {
					f_debugUnparserUnparser = new JavaFmtStream(true);
					f_debugUnparserUnparser.getStyle().setUnparsePromises(true);
					f_debugUnparsefirstCall = false;
				} else {
					f_debugUnparserUnparser.resetStream();
				}
				f_debugUnparserUnparser.unparse(irNode);
				TokenArray tarray = f_debugUnparserUnparser.getTokenArray();
				tarray.finish();
				TokenView tview = new TokenView(80, tarray, false);
				tview.init();
				String[] lines = tview.strTV();
				LOG.fine("+--BEGIN--- " + name + " ---BEGIN---");
				for (int j = 0; j < lines.length; j++) {
					if (!lines[j].trim().equals("")) { // trim out blank lines
						LOG.fine("| " + lines[j]);
					}
				}
				LOG.finer("+---END---- " + name + " ----END----");
			}
		}
	}
	
	public static String getUnparsedText(IRNode node) {
		JavaFmtStream stream = new JavaFmtStream(true);

		stream.unparse(node);
		stream.prepStream();
		TokenArray tokens = stream.getTokenArray();
		tokens.finish();

		TokenView tview = new TokenView(80, tokens, false);
		tview.init();

		String[] text = tview.strTV();
		String newtext = "";
		final String eol = SLUtility.PLATFORM_LINE_SEPARATOR;
		for (int i = 0; i < text.length; i++)
			newtext = newtext + text[i] + eol;
		return newtext;
	}
	
	protected void createRequiredConstructorNodes(IRNode result) {
		createRequiredMethodNodes(false, result);
	}
	
	protected static void createRequiredMethodNodes(boolean isStatic, IRNode result) {
		IRNode rv = ReturnValueDeclaration.makeReturnNode(result);
		SkeletonJavaRefUtility.copyIfPossible(result, rv);
		if (!isStatic) {
			IRNode rd = ReceiverDeclaration.makeReceiverNode(result);
			SkeletonJavaRefUtility.copyIfPossible(result, rd);
		}
	}

	public static void createRequiredClassNodes(IRNode result) {
		createRequiredTypeNodes(result);
		createRequiredAnonClassNodes(result);
	}

	protected static void createRequiredAnonClassNodes(IRNode result) {
		IRNode init = InitDeclaration.getInitMethod(result);
		ReceiverDeclarationInInit.makeReceiverNodeForInit(init);
		ReceiverDeclaration.makeReceiverNode(result);
	}

	protected static void createRequiredTypeNodes(IRNode result) {
		IRNode clinit = ClassInitDeclaration.getClassInitMethod(result);
		SkeletonJavaRefUtility.copyIfPossible(result, clinit);
	}

	protected void createLastMinuteNodes(IRNode root) {
		createLastMinuteNodes(root, false, null);
	}
	
	protected void createLastMinuteNodes(IRNode root, final boolean makeSrcRefs, final String project) {
		final String pkg  = makeSrcRefs ? VisitUtil.getPackageName(root) : null;
		final IRNode type = makeSrcRefs ? VisitUtil.getPrimaryType(root) : null;
		final String cu   = makeSrcRefs ? JavaNames.getTypeName(type) : null; 
		for (IRNode n : JJNode.tree.topDown(root)) {
			Operator op = JJNode.tree.getOperator(n);
			if (MethodDeclaration.prototype.includes(op)) {
				ReturnValueDeclaration.getReturnNode(n);
				
				if (!JavaNode.getModifier(n, JavaNode.STATIC)) {
				/*
				final String name = JavaNames.genQualifiedMethodConstructorName(n);
				if ("test.Outer.foo()".equals(name)) {
					System.out.println("Making receiver decls for "+name);
				}
				*/
					PromiseUtil.addReceiverDeclsToMethod(n);
				}				
			} else if (ConstructorDeclaration.prototype.includes(op)) {
				ReturnValueDeclaration.getReturnNode(n);
				PromiseUtil.addReceiverDeclsToConstructor(n);
			} else if (InterfaceDeclaration.prototype.includes(op)) {
				createRequiredTypeNodes(n);    
				ReceiverDeclaration.getReceiverNode(n); // Only for Java 8
			} else if (AnnotationDeclaration.prototype.includes(op)) {
				createRequiredTypeNodes(n);
				/*
				// Are these necessary?
				IRNode init = InitDeclaration.getInitMethod(n);		
				PromiseUtil.addReceiverDecls(n);
				*/
			} else if (ClassDeclaration.prototype.includes(op)
					|| AnonClassExpression.prototype.includes(op)
					|| EnumDeclaration.prototype.includes(op)
					|| EnumConstantClassDeclaration.prototype.includes(op)) {
				IRNode init = InitDeclaration.getInitMethod(n);
				createRequiredTypeNodes(n);
				PromiseUtil.addReceiverDeclsToType(n);
				PromiseUtil.addReceiverDeclsToMethod(init);

				//System.out.println("Adding last-minute nodes to "+JavaNames.getFullTypeName(n));
				// Assume that these are made by the ClassAdapter				 
				if (makeSrcRefs) {
					// Used by ClassAdapter
					if (!SkeletonJavaRefUtility.hasRegistered(n)) {
						System.out.println("No src ref for "+JavaNames.getFullTypeName(n));
					}
//					ISrcRef ref = JavaNode.getSrcRef(n);
//					if (ref == null) {
//						String name = JavaNames.getFullTypeName(n);
//						ref = new NamedSrcRef(project, name, pkg, cu);
//						JavaNode.setSrcRef(n, ref);
//					}
				}
				
			} else if (edu.cmu.cs.fluid.java.operator.AnnotationElement.prototype.includes(op)) {
				ReturnValueDeclaration.getReturnNode(n);
				PromiseUtil.addReceiverDeclsToMethod(n);
			}
		}
		
		if (Util.useIntegratedRewrite) {
			/**
			 * Creates IncompleteThrows for any default constructors it creates
			 * 
			 * Needs new instance to protect it from other threads running concurrently
			 */
			final JavaRewrite rewrite = new JavaRewrite(null);
			rewrite.ensureDefaultsExist(root);
		}
	}
	
	// Code to create various kinds of nodes

	/**
	 * Creates a IR NamedType from the given typeName String.
	 */
	protected final IRNode createNamedType(String typeName) {
		IRNode n = edu.cmu.cs.fluid.java.operator.NamedType
				.createNode(typeName);
		return n;
	}
	
	/**
	 * HACK to get around lack of ability to call the "inherited" createNode()
	 * from ClassDeclaration.
	 */
	protected final IRNode createNestedClassDeclarationNode(IRNode annos,
			int mods, String id, IRNode formals, IRNode extension,
			IRNode impls, IRNode body) {
		IRNode _result = JavaNode
				.makeJavaNode(
						edu.cmu.cs.fluid.parse.JJOperator.tree,
						edu.cmu.cs.fluid.java.operator.NestedClassDeclaration.prototype,
						new IRNode[] { annos, formals, extension, impls, body });
		JavaNode.setModifiers(_result, mods);
		JJNode.setInfo(_result, id);
		return _result;
	}

	/**
	 * HACK to get around lack of ability to call the "inherited" createNode()
	 * from InterfaceDeclaration.
	 */
	protected final IRNode createNestedInterfaceNode(IRNode annos, int mods,
			String id, IRNode formals, IRNode extensions, IRNode body) {
		IRNode _result = JavaNode
				.makeJavaNode(
						edu.cmu.cs.fluid.parse.JJOperator.tree,
						edu.cmu.cs.fluid.java.operator.NestedInterfaceDeclaration.prototype,
						new IRNode[] { annos, formals, extensions, body });
		JavaNode.setModifiers(_result, mods);
		JJNode.setInfo(_result, id);
		return _result;
	}

	/**
	 * Create a single IR NoArrayInitializer to use
	 */
	public final IRNode createNoArrayInitializer() {
		return edu.cmu.cs.fluid.java.operator.NoArrayInitializer.prototype
				.jjtCreate();
	}

	/**
	 * Create a single IR NoFinally to use
	 */
	public final IRNode createNoFinally() {
		return edu.cmu.cs.fluid.java.operator.NoFinally.prototype.jjtCreate();
	}

	/**
	 * Create a single IR NoInitialization to use
	 */
	public final IRNode createNoInitialization() {
		return edu.cmu.cs.fluid.java.operator.NoInitialization.prototype
				.jjtCreate();
	}

	/**
	 * Create a single IR NoMethodBody to use
	 */
	public final IRNode createNoMethodBody() {
		return edu.cmu.cs.fluid.java.operator.NoMethodBody.prototype
				.jjtCreate();
	}

	/**
	 * Create a single IR NullLiteral to use
	 */
	protected final IRNode createNullLiteral() {
		return edu.cmu.cs.fluid.java.operator.NullLiteral.prototype.jjtCreate();
	}
	
	/**
	 * Create a single IR SuperExpression for use
	 */
	public final IRNode createSuperExpression() {
		return edu.cmu.cs.fluid.java.operator.SuperExpression.prototype
				.jjtCreate();
	}

	/**
	 * Creates a SwitchElement from the given SwitchLabel (irLabel) and list of
	 * Statements (that is turned into an IR SwitchStatements node).
	 */
	public final IRNode createSwitchElement(IRNode irLabel,
			List<IRNode> switchStatementsList) {
		IRNode irSwitchStatements = edu.cmu.cs.fluid.java.operator.SwitchStatements
				.createNode(switchStatementsList
						.toArray(new IRNode[switchStatementsList.size()]));
		IRNode irSwitchElement = edu.cmu.cs.fluid.java.operator.SwitchElement
				.createNode(irLabel, irSwitchStatements);
		return irSwitchElement;
	}

	/**
	 * Create a single IR ThisExpression for use
	 */
	public final IRNode createThisExpression() {
		return edu.cmu.cs.fluid.java.operator.ThisExpression.prototype
				.jjtCreate();
	}

	/**
	 * Create a single IR TrueExpression to use
	 */
	public final IRNode createTrueExpression() {
		return edu.cmu.cs.fluid.java.operator.TrueExpression.prototype
				.jjtCreate();
	}
	
	/**
	 * Create a single IR UnnamedPackageDeclaration for use
	 */
	public final IRNode createUnnamedPackageDeclaration() {
		return edu.cmu.cs.fluid.java.operator.UnnamedPackageDeclaration.prototype
				.jjtCreate();
	}

	/**
	 * Create a single IR VoidReturnStatement for use
	 */
	public final IRNode createVoidReturnStatement() {
		return edu.cmu.cs.fluid.java.operator.VoidReturnStatement.prototype
				.jjtCreate();
	}

	/**
	 * Create a single IR VoidType for use
	 */
	protected final IRNode createVoidType() {
		return edu.cmu.cs.fluid.java.operator.VoidType.prototype.jjtCreate();
	}

	protected final IRNode createEmptyAnnos() {
		return Annotations.createNode(noNodes);
	}

	protected <T> IRNode[] map(Function<T> f, List<? extends T> trees, CodeContext context) {
		if (trees == null) {
			return null;
		}
		final int size = trees.size();
		if (size == 0) {
			return noNodes;
		}
		final List<IRNode> temp = new ArrayList<IRNode>(size);
		//IRNode[] result = new IRNode[size];
		for(int i=0; i<size; i++) {
			//result[i] = f.call(trees.get(i), context, i, size);
			IRNode result = f.call(trees.get(i), context, i, size);
			if (result != null) {
				temp.add(result);
			}
		}
		return temp.toArray(new IRNode[temp.size()]);
	}
	
	protected <T> IRNode[] map(Function<T> f, T[] trees, CodeContext context) {
		if (trees == null) {
			return noNodes;
		}
		final int size = trees.length;
		if (size == 0) {
			return noNodes;
		}
		IRNode[] result = new IRNode[size];
		for(int i=0; i<size; i++) {
			result[i] = f.call(trees[i], context, i, size);
		}
		return result;
	}

	private static final int maxDummies = 15;
	private static final String[] dummyArgs = new String[maxDummies];

	{
		initDummyArgs();
	}

	private static void initDummyArgs() {
		for (int i = 0; i < maxDummies; i++) {
			dummyArgs[i] = ("arg" + i).intern();
		}
		// LOG.info("Initialized dummy args for ITypeBindings");
	}

	public static String getDummyArg(int i) {
		if (i < maxDummies) {
			String arg = dummyArgs[i];
			if (arg == null) {
				initDummyArgs();
				arg = dummyArgs[i];
			}
			return arg;
		}
		return "arg" + i;
	}
	
	/**
	 * Info to deal with naming AnonClassExpressions
	 */
	public static final String ACE_Prefix = "ACE #";
	private static final int MAX_PRECOMPUTED = 10;
	private static final String[] precomputedNames = new String[MAX_PRECOMPUTED];
	static {
		for(int i=0; i<MAX_PRECOMPUTED; i++) {
			precomputedNames[i] = ACE_Prefix + i;
		}
	}	
	private int aceCount;
	private final Map<Object, String> aceNames = new HashMap<Object, String>();
	private final Set<Object> working = new HashSet<Object>();
	
	protected synchronized final void initACEInfo(Object key) {
		working.add(key);
	}
	
	protected synchronized final void resetACEInfo(Object key) {
		working.remove(key);
		if (working.isEmpty()) {
			aceCount = 0;
			aceNames.clear();
		}
	}
	
	protected final String getNextACEName() {
		return getNextACEName(null);
	}

	protected synchronized final String getNextACEName(Object tb) {
		String name = aceCount < MAX_PRECOMPUTED ? precomputedNames[aceCount] : 
			                                       ACE_Prefix + aceCount;
		aceCount++;
		if (tb != null) {
			aceNames.put(tb, name);
		}
		return name;
	}

	protected synchronized final String lookupACEName(Object tb) {
		return aceNames.get(tb);
	}
}
