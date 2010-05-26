/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ScopedPromiseRules.java,v 1.23 2009/01/15 15:53:05 aarong Exp $*/
package com.surelogic.annotation.rules;

import java.util.*;
import java.util.logging.Level;

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

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IHasBinding;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.sea.drops.promises.AssumePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.PromisePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ScopedPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class ScopedPromiseRules extends AnnotationRules {
	public static final String ASSUME = "Assume";
	public static final String PROMISE = "Promise";
	public static final String PACKAGE_PROMISE = "Package Promise";

	private static final AnnotationRules instance = new ScopedPromiseRules();
	
	private static final Assume_ParseRule assumeRule = new Assume_ParseRule();

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
		registerParseRuleStorage(fw, assumeRule);
		registerParseRuleStorage(fw, promiseRule);
		registerScrubber(fw, packageScrubber);		
	}

	public static abstract class ScopedPromiseRule<A extends ScopedPromiseNode, P extends ScopedPromiseDrop>
			extends AbstractAntlrParseRule<A, P, ScopedPromisesParser> {
		protected ScopedPromiseRule(String name, Operator[] ops, Class<A> dt) {
			super(name, ops, dt, AnnotationLocation.DECL);
		}

		@Override
		protected ScopedPromisesParser initParser(String contents) throws Exception {
			if (contents == null) {
				System.out.println("Null contents");
			}
			return ScopedPromiseParse.prototype.initParser(contents);
		}
		
		@Override
		protected Object parse(IAnnotationParsingContext context,
				ScopedPromisesParser parser) throws RecognitionException {
			Object rv = parser.scopedPromise().getTree();
			return rv;
		}
		
		/**
		 * Used to pre-check the embedded annotation
		 */
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
				Operator contextOp = sp.getTargets().appliesTo();
				if (contextOp == Operator.prototype) {
					// Applies to anything that the rule applies to
					contextOp = r.getOps(null)[0];
				}
				Proxy proxy = new Proxy(context, r, contextOp);
				r.parse(proxy, parser.content);
				if (!proxy.createdAAST() && !proxy.hadProblem()) {
					context.reportError(0, "No AAST created from " + sp.getPromise());
					return null;
				}
			} else {
				context.reportError(0, "No rule for @" + parser.tag);
				return null;
			}
			
			return rewrap(c, sp);
		}

		protected abstract AASTNode rewrap(IAnnotationParsingContext c, ScopedPromiseNode sp);
	}

	static class Assume_ParseRule extends 
	        ScopedPromiseRule<AssumeScopedPromiseNode, AssumePromiseDrop> {
		protected Assume_ParseRule() {
			super(ASSUME, methodOrClassDeclOps, AssumeScopedPromiseNode.class);
		}
		
		@Override
		protected AASTNode rewrap(IAnnotationParsingContext c, ScopedPromiseNode sp) {
			return new AssumeScopedPromiseNode(sp.getOffset(), sp.getPromise(), sp.getTargets());
		}
		
		@Override
		protected IPromiseDropStorage<AssumePromiseDrop> makeStorage() {
			return PromiseDropSeqStorage.create(name(), AssumePromiseDrop.class);
		}
		
		@Override
		protected IAnnotationScrubber<AssumeScopedPromiseNode> makeScrubber() {
			return new AbstractAASTScrubber<AssumeScopedPromiseNode>(this, ScrubberType.BY_TYPE, 
					                                                 noStrings, ScrubberOrder.FIRST) {
				final PromiseFramework frame = PromiseFramework.getInstance();
				Set<IRNode> bindings = null;
				
				@Override
				protected boolean useAssumptions() {
					return false;
				}
				@Override
				protected void startScrubbingType(IRNode decl) {
					final IRNode cu = VisitUtil.getEnclosingCompilationUnit(decl);
					bindings = collectBoundDecls(cu);
					frame.clearTypeContext(cu);
					/*
					frame.pushTypeContext(cu, true, true); // create one if there isn't one
					*/
					AASTStore.setupAssumption(cu);
				}

				@Override
				protected boolean customScrub(AssumeScopedPromiseNode a) {
					return checkTargets(a);
				}
				@Override
				protected PromiseDrop<ScopedPromiseNode> makePromiseDrop(
						AssumeScopedPromiseNode a) {
					AssumePromiseDrop d = new AssumePromiseDrop(a);
					boolean worked = applyAssumptions(bindings, d);					
					if (!worked) {
					  d.invalidate();
					  return null;
					}
					storeDropIfNotNull(getStorage(), a, d);
					return d;
				}
				@Override
				protected void finishScrubbingType(IRNode decl) {
					//frame.popTypeContext();
					AASTStore.clearAssumption();
					bindings = null;
				}
			};
		}
	}
	
	static class Promise_ParseRule extends
			ScopedPromiseRule<ScopedPromiseNode, PromisePromiseDrop> {
		protected Promise_ParseRule() {
			super(PROMISE, anyOp, ScopedPromiseNode.class);
		}
		
		@Override
		protected AASTNode rewrap(IAnnotationParsingContext c, ScopedPromiseNode sp) {
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
					                                           noStrings, ScrubberOrder.FIRST) {
				@Override
				protected boolean customScrub(ScopedPromiseNode a) {
					return checkTargets(a);
				}
				
				@Override
				protected PromiseDrop<ScopedPromiseNode> makePromiseDrop(
						ScopedPromiseNode a) {
					PromisePromiseDrop d = new PromisePromiseDrop(a);
					boolean worked = applyScopedPromises(d);					
					if (!worked) {
					  d.invalidate();
					  return null;
					}
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
	
	static boolean checkTargets(ScopedPromiseNode a) {
		// Check for inconsistency
		if (a.getTargets().appliesTo() == null) {
			return false;
		}
		return new TargetVisitor().doAccept(a.getTargets());	
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
	 * to: 
	 * 1 - find the {@link CompilationUnit} that we are currently parsing 
	 * 2 - create an AAST from the promise in the {@link ScopedPromiseNode} 
	 * 3 - loop over all of the relevant elements of the {@link CompilationUnit} 
	 *     and apply the AAST to the matching elements
	 * 
	 * @param scopedPromise
	 *          The AAST representing the scoped promise contained in the code
	 */
	static <A extends ScopedPromiseDrop> boolean applyScopedPromises(
			A scopedPromiseDrop) {
		final IRNode promisedFor = scopedPromiseDrop.getAST().getPromisedFor();
		boolean success = true;

		final Operator op = JJNode.tree.getOperator(promisedFor);
		//If the node this promise is promised for is a class or type declaration,
	  // pass it on directly
		if(TypeDeclaration.prototype.includes(op)) {
			success = applyPromiseOnType(promisedFor, scopedPromiseDrop);
		}	
		//If the IRNode is a CompilationUnit, iterate over all of the top-level types
		else if (CompilationUnit.prototype.includes(op)) {
			for (IRNode decl : VisitUtil.getTypeDecls(promisedFor)) {
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
	 * @return true if no failure
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
			// the scoped promise is 
			{
				final IRNode decl = promisedFor;
			//for (IRNode decl : VisitUtil.getAllTypeDecls(promisedFor)) {
				Operator op = JJNode.tree.getOperator(decl);
				if (callback.parseRule.declaredOnValidOp(op)) {
					if (!callback.parseAndApplyPromise(decl, op)) {
						success = false;
						//break;
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
		private final Operator op;

		public Proxy(AbstractAnnotationParsingContext context,
				IAnnotationParseRule<?, ?> rule, Operator op) {
			super(context);
			this.rule = rule;
			this.op = op;
		}

		@Override
		public boolean createdAAST() {
			return reported;
		}

		@Override
		public Operator getOp() {
			return op;
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
			AASTStore.cloneTestResult(callback.scopedPromiseDrop.getAST(), root);
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
				
				// Check for enclosing "..."
				contents = contents.trim();
				if (contents.startsWith("\"") && contents.endsWith("\"")) {
					contents = contents.substring(1, contents.length()-1);
				}
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
			if (scopedPromiseDrop instanceof AssumePromiseDrop) {
				pd.setAssumed(true);
			} else {
				pd.setVirtual(true);
			}
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
				ParseResult result = parseRule.parse(context, content);
				if (result == ParseResult.IGNORE) {
					return false;
				}
				else if (result == ParseResult.FAIL || !context.createdAAST()) {
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
				//System.out.println(scopedPromiseDrop.getMessage()+" on "+DebugUnparser.toString(decl));
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
  
  /**
   * Collect up all unique bindings in the type
   * -- not including those in enclosed types (TODO?)
   * -- compensating for those that aren't ClassBodyDecls
   * 
   * TODO what about bindings in promises themselves?
   */
  private static Set<IRNode> collectBoundDecls(IRNode cu) {
	  final IBinder binder = IDE.getInstance().getTypeEnv().getBinder();
	  return collectBoundDecls(binder, cu, false);
  }
  
  private static Set<IRNode> collectBoundDecls(IBinder binder, final IRNode cu, final boolean includeOriginal) {
  	final Set<IRNode> decls = new HashSet<IRNode>();
    
  	final Iterator<IRNode> nodes = tree.bottomUp(cu);
  	while (nodes.hasNext()) {
  		final IRNode n = nodes.next();
  		final Operator op = tree.getOperator(n);

  		if (op instanceof IHasBinding) {
  			IRNode decl = binder.getBinding(n);        
  			if (decl == null) {
  				continue;
  			}
  			IRNode decl2 = findEnclosingDecl(decl);
  			if (decl2 != null) {
  				decl = decl2;          
  			}
  			decls.add(decl);
  		}
  	}
    if (LOG.isLoggable(Level.FINE)) {
      final Iterator<IRNode> it = decls.iterator();
      while (it.hasNext()) {
        final IRNode decl = it.next();        
        LOG.fine("Collected binding: "+DebugUnparser.toString(decl));
      }
    }  
  	return decls; // Collections.EMPTY_SET;
  }
  
  /**
   * Normalize if inside a class body decl
   * 
   * TODO still needed with changes for Java 5 annos?
   */
  private static IRNode findEnclosingDecl(IRNode decl) {
	  final Operator dop = tree.getOperator(decl);
	  IRNode decl2 = null;

	  if (ReturnType.prototype.includes(dop) ||
			  ClassBodyDeclaration.prototype.includes(dop) ||
			  TypeDeclaration.prototype.includes(dop) ||
			  PackageDeclaration.prototype.includes(dop)) {
		  return null;
	  } 
	  decl2 = VisitUtil.getEnclosingClassBodyDecl(decl);
	  if (decl2 == null) {
		  final IRNode parent = JavaPromise.getParentOrPromisedFor(decl);
		  if (parent == null) {
			  return null;
		  }
		  final Operator pop  = tree.getOperator(parent);
		  if (ReceiverDeclaration.prototype.includes(dop) && 
				  InitDeclaration.prototype.includes(pop)) {
			  LOG.info("Ignoring receiver node of init decl"); 
		  } else {
			  LOG.warning("Not inside a ClassBodyDecl: "+DebugUnparser.toString(decl)+
					  " inside parent = "+
					  DebugUnparser.toString(JavaPromise.getParentOrPromisedFor(decl)));
		  }
		  return null;
	  }           
	  return decl2;       
  }
  
  /**
   * @return true if no failure
   */
  static boolean applyAssumptions(Collection<IRNode> bindings, AssumePromiseDrop d) {	  
	  //final IRNode cu = VisitUtil.getEnclosingCompilationUnit(d.getNode());
	  final ScopedPromiseCallback callback = new ScopedPromiseCallback(d);
	  boolean success = true;
	  for(IRNode decl : bindings) {
		  Operator op = JJNode.tree.getOperator(decl);
		  if (callback.parseRule.declaredOnValidOp(op)) {
			  if (!callback.parseAndApplyPromise(decl, op)) {
				  //System.out.println("Failure on "+DebugUnparser.toString(decl));
				  success = false;
				  break;
			  }
		  }
	  }
	  return success;
  }
}
