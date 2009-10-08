/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ScopedPromiseRules.java,v 1.23 2009/01/15 15:53:05 aarong Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.*;
import com.surelogic.aast.promise.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.parse.AbstractNodeAdaptor;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.PromiseDropSeqStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.sea.drops.promises.PromisePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ScopedPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.QuickProperties;

public class ScopedPromiseRules extends AnnotationRules {
	public static final String PROMISE = "Promise";
	public static final String PACKAGE_PROMISE = "Package Promise";

	public static final QuickProperties.Flag useNewScopedPromisesFlag = new QuickProperties.Flag(
			LOG, "rules.useNewScopedPromises", "Promise", true, true);

	private static boolean useNewScopedPromises() {
		return QuickProperties.checkFlag(useNewScopedPromisesFlag);
	}

	public static final boolean useNewScopedPromises = useNewScopedPromises();

	private static final AnnotationRules instance = new ScopedPromiseRules();

	private static final Promise_ParseRule promiseRule = new Promise_ParseRule();

	private static final PackageScrubber packageScrubber = new PackageScrubber(promiseRule);
	
	public static AnnotationRules getInstance() {
		return instance;
	}

	public static Iterable<PromisePromiseDrop> getScopedPromises(IRNode mdecl) {
		return getDrops(promiseRule.getStorage(), mdecl);
	}

	@Override
	public void register(PromiseFramework fw) {
		if (useNewScopedPromises) {
			registerParseRuleStorage(fw, promiseRule);
			registerScrubber(fw, packageScrubber);
		}
	}

	public static abstract class ScopedPromiseRule<A extends ScopedPromiseNode, P extends ScopedPromiseDrop>
			extends AbstractAntlrParseRule<A, P, ScopedPromisesParser> {
		protected ScopedPromiseRule(String name, Operator[] ops, Class<A> dt) {
			super(name, ops, dt, AnnotationLocation.DECL);
		}

		protected ScopedPromiseRule(String name, Operator op, Class<A> dt) {
			super(name, op, dt, AnnotationLocation.DECL);
		}

		@Override
		protected ScopedPromisesParser initParser(String contents) throws Exception {
			return ScopedPromiseParse.initParser(contents);
		}
	}

	static class Promise_ParseRule extends
			ScopedPromiseRule<ScopedPromiseNode, PromisePromiseDrop> {
		protected Promise_ParseRule() {
			super(PROMISE, anyOp, ScopedPromiseNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				ScopedPromisesParser parser) throws RecognitionException {
			if (useNewScopedPromises) {
				Object rv = parser.scopedPromise().getTree();
				return rv;
			}
			return null;
		}

		@Override
		protected AASTNode finalizeAST(IAnnotationParsingContext c,
				AbstractNodeAdaptor.Node tn) {
			final ScopedPromiseAdaptor.Node node = (ScopedPromiseAdaptor.Node) tn;
			node.useText(c);
			
			ScopedPromiseNode sp = (ScopedPromiseNode) super.finalizeAST(c, tn);
			PromiseContentParser parser = new PromiseContentParser(c, sp.getPromise());
			if (parser.tag == null) {
				// Already reported error
				return null;
			}
			
			// -- passing on errors/warnings
			// -- capturing any AASTs that are created
			AbstractAnnotationParsingContext context = (AbstractAnnotationParsingContext) c;
			IAnnotationParseRule<?, ?> r = PromiseFramework.getInstance()
					.getParseDropRule(parser.tag);
			if (r != null) {
				Proxy proxy = new Proxy(context, r);
				r.parse(proxy, parser.content);
				if (!proxy.createdAAST() && !proxy.hadProblem()) {
					context.reportError(0, "No AAST created from " + sp.getPromise());
					return null;
				}
			} else {
				context.reportError(0, "No rule for @" + parser.tag);
				return null;
			}
			
			if (NamedPackageDeclaration.prototype.includes(c.getOp())) {
			  // Repackage as PackageScopedPromiseNode
			  return new PackageScopedPromiseNode(sp.getOffset(), sp.getPromise(), sp.getTargets());
			}
			return sp;
		}

		@Override
		protected IPromiseDropStorage<PromisePromiseDrop> makeStorage() {
			return PromiseDropSeqStorage.create(name(), PromisePromiseDrop.class);
		}		
		
		@Override
		protected IAnnotationScrubber<ScopedPromiseNode> makeScrubber() {
			return new AbstractAASTScrubber<ScopedPromiseNode>(this, ScrubberType.UNORDERED, 
			                                                   new String[0], ScrubberOrder.FIRST) {
				@Override
				protected boolean customScrub(ScopedPromiseNode a) {
					// Check for inconsistency
					if (a.getTargets().appliesTo() == null) {
						return false;
					}
					return new TargetVisitor().doAccept(a.getTargets());					
				}
				
				@Override
				protected PromiseDrop<ScopedPromiseNode> makePromiseDrop(
						ScopedPromiseNode a) {
					PromisePromiseDrop d = new PromisePromiseDrop(a);
					boolean worked = applyScopedPromises(d);					
					if (!worked) {
					  d.invalidate();
					}
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
	
	static class TargetVisitor extends DescendingVisitor<Boolean> {
		public TargetVisitor() {
			super(Boolean.TRUE);
		}
		@Override
		protected Boolean combineResults(Boolean before, Boolean next) {
			return before && next;
		}
		@Override
		public Boolean visit(ConstructorDeclPatternNode n) {
			return doAccept(n.getInPattern());
		}
		@Override
		public Boolean visit(MethodDeclPatternNode n) {
			return doAccept(n.getInPattern());
		}
		@Override
		public Boolean visit(FieldDeclPatternNode n) {
			return doAccept(n.getInPattern());
		}
		@Override
		public Boolean visit(TypeDeclPatternNode n) {
			return doAccept(n.getInPattern());
		}
		@Override
		public Boolean visit(WildcardTypeQualifierPatternNode n) {
			return !n.getTypePattern().contains("**.*");
		}
		@Override
		public Boolean visit(InPackagePatternNode n) {
			return !n.getPackagePattern().contains("**.*");
		}
	}
	
	/**
	 * Applies a scoped promise to the CU or Type that it is declared on. Needs
	 * to: 1 - find the {@link CompilationUnit} that we are currently parsing 2 -
	 * create an AAST from the promise in the {@link ScopedPromiseNode} 3 - loop
	 * over all of the relevant elements of the {@link CompilationUnit} and apply
	 * the AAST to the matching elements
	 * 
	 * @param scopedPromise
	 *          The AAST representing the scoped promise contained in the code
	 */
	public static <A extends ScopedPromiseDrop> boolean applyScopedPromises(
			A scopedPromiseDrop) {
		final IRNode promisedFor = scopedPromiseDrop.getAST().getPromisedFor();
		boolean success = true;

		final Operator op = JJNode.tree.getOperator(promisedFor);
		//If the node this promise is promised for is a class or type declaration,
	  // pass it on directly
		if(TypeDeclaration.prototype.includes(op)) {
			success = applyPromiseOnType(promisedFor, scopedPromiseDrop);
		}	
		//If the IRNode is a CompilationUnit, iterate over all of the included types
		else if (CompilationUnit.prototype.includes(op)) {
			for (IRNode decl : VisitUtil.getAllTypeDecls(promisedFor)) {
				success = success && applyPromiseOnType(decl, scopedPromiseDrop);
			}
			success = applyPromiseOnType(promisedFor, scopedPromiseDrop);
		} 
		else if (NamedPackageDeclaration.prototype.includes(op)) {
		  throw new UnsupportedOperationException("Should never get here");		  
		}
		//If it's something else, get the enclosing type and apply the promise using that
		else {
			IRNode enclosing = VisitUtil.getEnclosingType(promisedFor);
			success = applyPromiseOnType(enclosing, scopedPromiseDrop);
		}

		return success;
	}

	/**
	 * applies the given scoped promise on all of the matching declarations
	 * contained in the promised for IRNode
	 * 
	 * @param promisedFor
	 *          The TypeDeclaration IRNode that the scoped promise is promised on
	 * @param scopedPromise
	 *          The scoped promise
	 */
	private static <A extends ScopedPromiseDrop> boolean applyPromiseOnType(
			IRNode promisedFor, A scopedPromiseDrop) {
		boolean success = true;
		if (!TypeDeclaration.prototype.includes(promisedFor)) {
			// Probably a package decl
			return false;
		}
		final ScopedPromiseCallback callback = 
			new ScopedPromiseCallback(scopedPromiseDrop);
		
		if (callback.parseRule != null) {
			// TODO only loop over the necessary declarations by checking what type
			// the
			// scoped promise is
			for (IRNode decl : VisitUtil.getAllTypeDecls(promisedFor)) {
				Operator op = JJNode.tree.getOperator(decl);
				if (callback.parseRule.declaredOnValidOp(op)) {
					if (!callback.parseAndApplyPromise(decl, op)) {
						success = false;
						break;
					}
				}
			}
			
			for (IRNode decl : VisitUtil.getClassMethods(promisedFor)) {
				Operator op = JJNode.tree.getOperator(decl);
				if (callback.parseRule.declaredOnValidOp(op)) {
					if (!callback.parseAndApplyPromise(decl, op)) {
						success = false;
						break;
					}
				}
			}
			if (success && callback.parseRule.declaredOnValidOp(FieldDeclaration.prototype)) {			  
				for (IRNode decl : VisitUtil.getClassFieldDecls(promisedFor)) {
					if (!callback.parseAndApplyPromise(decl, FieldDeclaration.prototype)) {
						success = false;
						break;
					}
				}
			}
		} else {
			//FIXME is this all?
			System.err.println("No parse rule found for promise: " + callback.tag);
			success = false;
		}
		return success;
	}
	
	/**
	 * Used to note the creation of an AAST (discarded afterwards)
	 */
	static class Proxy extends AnnotationParsingContextProxy {
		private final IAnnotationParseRule<?, ?> rule;
		private boolean reported;

		public Proxy(AbstractAnnotationParsingContext context,
				IAnnotationParseRule<?, ?> rule) {
			super(context);
			this.rule = rule;
		}

		@Override
		public boolean createdAAST() {
			return reported;
		}

		@Override
		public Operator getOp() {
			// Changed to ignore where the scoped promise is
			return rule.getOps(null)[0];
		}

		@Override
		public <T extends IAASTRootNode> void reportAAST(int offset,
				AnnotationLocation loc, Object o, T ast) {
			reported = true;
		}
	}

	/**
	 * Used when we apply a ScopedPromise to a matching element
	 * 
	 * @author Ethan Urie
	 */
	static class ScopedAnnotationParsingContext extends
			SimpleAnnotationParsingContext {
		private static final String name = "ScopedAnnotationParsingContext";
		private final ScopedPromiseCallback callback;

		public ScopedAnnotationParsingContext(ScopedPromiseCallback callback,
				AnnotationSource src, IRNode n, IAnnotationParseRule<?, ?> r,
				String text, int offset) {
			super(src, n, r, text, offset);
			this.callback = callback;
		}

		@Override
    protected void postAASTCreate(final AASTRootNode root) {
			AASTStore.triggerWhenValidated(root, callback);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.SimpleAnnotationParsingContext#getName()
		 */
		@Override
		protected String getName() {
			return name;
		}

	}

	static class PromiseContentParser {
		final String tag, content;
		
		PromiseContentParser(IAnnotationParsingContext c, String text) {
			int start = text.startsWith("@") ? 1 : 0;
			int lparen = text.indexOf('(');
			String promise, contents;
			if (lparen < 0) {
				promise = text.substring(start);
				contents = "";
			} else { 
				// Has a left paren
				if (text.endsWith(")")) {
					promise = text.substring(start, lparen);
					contents = text.substring(lparen + 1, text.length()-1);
				} else {
					if (c != null) {
						c.reportError(text.length()-1, "No closing parentheses :" + text);
					}
					promise = null;
					contents = null;
				}
			}
			if (promise != null) {
				promise = promise.trim();
				promise = AnnotationVisitor.capitalize(promise);
			}
			tag = promise;
			content = contents;
		}
	}
	
	/**
	 * Also used to help with parsing and applying the scoped promise
	 */
	@SuppressWarnings("unchecked")
	static class ScopedPromiseCallback extends PromiseContentParser 
	implements ValidatedDropCallback {
		private final ScopedPromiseDrop scopedPromiseDrop;
		final PromiseTargetNode target;
	
		// Used to parse the promise when we find a match
		final IAnnotationParseRule<?, ?> parseRule;		
		
		public ScopedPromiseCallback(ScopedPromiseDrop drop) {
			super(null, drop.getAST().getPromise());			
			this.scopedPromiseDrop = drop;		
			target = drop.getAST().getTargets();
			
			if (tag == null) {
				throw new IllegalArgumentException("No closing parentheses: "+drop.getAST().getPromise());
			}
			parseRule = PromiseFramework.getInstance().getParseDropRule(tag);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.scrub.ValidatedDropCallback#validated(edu.cmu.cs.fluid.sea.PromiseDrop)
		 */
		public void validated(PromiseDrop pd) {
			pd.setVirtual(true);
			pd.setSourceDrop(scopedPromiseDrop);
		}
		
		/**
		 * @return false if failed
		 */
		boolean parseAndApplyPromise(final IRNode decl, Operator op) {			
			if (target.matches(decl)) {
				final ISrcRef ref = JavaNode.getSrcRef(decl);
				int offset = -1;
				if (ref != null){
					offset = ref.getOffset();
				}
				final AbstractAnnotationParsingContext context = 
					new ScopedAnnotationParsingContext(this,
						scopedPromiseDrop.getAST().getSrcType(), decl, parseRule,
						content, offset);
				parseRule.parse(context, content);

				if (!context.createdAAST()) {
					StringBuilder msg = new StringBuilder("Could not apply scoped promise, ");
					msg.append(content).append(" on ");
					
					if (FieldDeclaration.prototype.includes(op)) {
						msg.append("field ").append(JavaNames.getFieldDecl(decl));
					}
					else if (SomeFunctionDeclaration.prototype.includes(op)) {
						msg.append("method/constructor ");
						msg.append(JavaNames.genMethodConstructorName(decl));
					}					
					else if (TypeDeclaration.prototype.includes(op)) {
						msg.append("type ");
						msg.append(JavaNames.getFullTypeName(decl));
					}
					context.reportError(offset, msg.toString());
					return false;
				}
			}
			return true;
		}
	}

  static class PackageScrubber extends AbstractAASTScrubber<PackageScopedPromiseNode> {
    final IPromiseDropStorage<PromisePromiseDrop> storage;
    PackageScrubber(Promise_ParseRule rule) {
      // Set to scrub before the normal @Promise scrubber
      super(PACKAGE_PROMISE, PackageScopedPromiseNode.class, rule.getStorage(), 
            ScrubberType.UNORDERED, new String[]{PROMISE}, ScrubberOrder.FIRST);
      storage = rule.getStorage();
    }
    
    @Override
    protected PromiseDrop<ScopedPromiseNode> makePromiseDrop(PackageScopedPromiseNode a) {
      PromisePromiseDrop d = new PromisePromiseDrop(a);
      // FIX scrub targets
      applyPromisesToPackage(d);

      return storeDropIfNotNull(storage, a, d);
    }
  }

  /**
   * Apply promises to types in the package
   */
  static void applyPromisesToPackage(PromisePromiseDrop d) {
	/*
	if (d.getEnclosingFile() != null) {
		System.out.println(d.getEnclosingFile()+": "+d.getMessage());
	}
	*/
    final IRNode pd      = d.getNode();
    final String pkgName = NamedPackageDeclaration.getId(pd);
    final PackageDrop pkg = PackageDrop.findPackage(pkgName);
    for(IRNode type : pkg.getTypes()) {
      applyPromiseOnType(type, d); 
    }
  }
}
