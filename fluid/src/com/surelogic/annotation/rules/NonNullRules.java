package com.surelogic.annotation.rules;

import java.util.HashSet;
import java.util.Set;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.aast.promise.NullableNode;
import com.surelogic.aast.promise.RawNode;
import com.surelogic.annotation.AnnotationLocation;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.DefaultBooleanAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.AASTAdaptor;
import com.surelogic.annotation.parse.AASTAdaptor.Node;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.parse.SLParse;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.AbstractPromiseScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberOrder;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class NonNullRules extends AnnotationRules {	
  private static final int CANNOT_BE_BOTH = 900;
  private static final int BAD_RAW_TYPE = 901;
  private static final int NOT_A_SUPERCLASS = 902;
  
	public static final String NONNULL = "NonNull";
	public static final String NULLABLE = "Nullable";
	public static final String RAW = "Raw";
	public static final String CONSISTENCY = "NullableConsistency";
	
	private static final NonNullRules instance = new NonNullRules();
	private static final NonNull_ParseRule nonNullRule = new NonNull_ParseRule();
	private static final Nullable_ParseRule nullableRule = new Nullable_ParseRule();
	private static final Raw_ParseRule rawRule = new Raw_ParseRule();
	private static final NullableConsistencyChecker consistency = new NullableConsistencyChecker();
	
  private static final Set<PromiseDrop<?>> annosForMethodChecking = new HashSet<PromiseDrop<?>>();

  
  
	private NonNullRules() {
		// Just to make it a singleton
	}
	
	public static AnnotationRules getInstance() {
		return instance;
	}
	
	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, nonNullRule);
		registerParseRuleStorage(fw, nullableRule);
		registerParseRuleStorage(fw, rawRule);
		registerScrubber(fw, consistency);
	} 

	public static class Raw_ParseRule extends DefaultBooleanAnnotationParseRule<RawNode,RawPromiseDrop> {
		public Raw_ParseRule() {
			super(RAW, new Operator[] { ClassInitDeclaration.prototype,
			    SomeFunctionDeclaration.prototype, ParameterDeclaration.prototype,
			    DeclStatement.prototype }, RawNode.class);
		}

		/**
		 * @Raw(upTo="*") � on formal parameter of type T
		 * @Raw(upTo="<type>") � on formal parameter of type T
		 * @Raw(upTo="*|<type>", value="this") � On method of type T
		 * @Raw(upTo="*|<type>", value="return") � On method with return type T
		 * @Raw(upTo="*", value="static(<type>)") � On method/constructor
		 */
		@Override
		protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
		  if (MethodDeclaration.prototype.includes(context.getOp())) {
		    return parser.rawMethod().getTree();
		  }
		  else if (ConstructorDeclaration.prototype.includes(context.getOp())) { 
			  return parser.rawConstructor().getTree();
		  } 
		  else { // parameter, local var
		    return parser.nothing().getTree();
		  }
//			if (ParameterDeclaration.prototype.includes(context.getOp())) {
//				return parser.nothing().getTree();
//			}
//			else if (MethodDeclaration.prototype.includes(context.getOp())) {
//				return parser.rawExpression().getTree();
//			}
//			throw new NotImplemented();
		}

		/**
		 * To handle static(...)
		 */
		@Override
		protected AnnotationLocation translateTokenType(int type, Operator op) {
			if (type == SLAnnotationsParser.NamedType) {
				return AnnotationLocation.RECEIVER; // TODO is this right?
			} else {
				return super.translateTokenType(type, op);
			}
		}
		
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int mappedOffset, int modifiers, AASTAdaptor.Node node) {
			final String upTo = context.getProperty(AnnotationVisitor.UPTO);
			if (upTo == null) {
				// TODO
			} else try {
				AASTAdaptor.Node upToE = (Node) SLParse.prototype.initParser(upTo).rawUpToExpression().getTree();			
			} catch (RecognitionException e) {
				handleRecognitionException(context, upTo, e);				
				return null;
			} catch (Exception e) {
				context.reportException(IAnnotationParsingContext.UNKNOWN, e);
				return null;
			}
			if (node.getType() == SLAnnotationsParser.NamedType) {
				// TODO
				return new RawNode(mappedOffset, node.getText()+" -- "+context.getProperty(AnnotationVisitor.UPTO));
			}
			return new RawNode(mappedOffset, context.getProperty(AnnotationVisitor.UPTO));
		}
		@Override
		protected IPromiseDropStorage<RawPromiseDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(), RawPromiseDrop.class);
		}
		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<RawNode, RawPromiseDrop>(
					this, ScrubberType.UNORDERED, NONNULL) {
				@Override
				protected PromiseDrop<RawNode> makePromiseDrop(final RawNode a) {
					return storeDropIfNotNull(a, scrubRaw(getContext(), a));
				}
			};
		}   
	}
	
	static Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
		if (context.getSourceType() == AnnotationSource.JAVADOC) {
			return parser.nonNullList().getTree();
		}
		if (MethodDeclaration.prototype.includes(context.getOp())) {			
			// Only allows "return"
			return parser.nonNullMethod().getTree();
		} else {
			// For parameters and fields 
			return parser.nothing().getTree();
		}
	}
	
	public static class NonNull_ParseRule
	extends DefaultBooleanAnnotationParseRule<NonNullNode,NonNullPromiseDrop> {
		public NonNull_ParseRule() {
			super(NONNULL, fieldMethodVarDeclOps, NonNullNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
			return NonNullRules.parse(context, parser);
		}
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
			return new NonNullNode(offset);
		}
		@Override
		protected IPromiseDropStorage<NonNullPromiseDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(), NonNullPromiseDrop.class);
		}
		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<NonNullNode, NonNullPromiseDrop>(
					this, ScrubberType.UNORDERED) {
				@Override
				protected PromiseDrop<NonNullNode> makePromiseDrop(final NonNullNode a) {
					return storeDropIfNotNull(a, scrubNonNull(getContext(), a));
				}
			};
		}   
	}
	
	public static class Nullable_ParseRule
	extends DefaultBooleanAnnotationParseRule<NullableNode,NullablePromiseDrop> {
		public Nullable_ParseRule() {
			super(NULLABLE, fieldMethodVarDeclOps, NullableNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
			return NonNullRules.parse(context, parser);
		}
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
			return new NullableNode(offset);
		}
		@Override
		protected IPromiseDropStorage<NullablePromiseDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(), NullablePromiseDrop.class);
		}
		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<NullableNode, NullablePromiseDrop>(
					this, ScrubberType.UNORDERED, RAW) {
				@Override
				protected PromiseDrop<NullableNode> makePromiseDrop(final NullableNode a) {
					return storeDropIfNotNull(a, scrubNullable(getContext(), a));
				}
			};
		}   
	}
	
	
	
  private static NonNullPromiseDrop scrubNonNull(
      final IAnnotationScrubberContext context,
      final NonNullNode n) {
    // Cannot be on a primitive type
    boolean good = RulesUtilities.checkForReferenceType(context, n, "NonNull");
    if (good) {
      final NonNullPromiseDrop pd = new NonNullPromiseDrop(n);
      addAnnotationForMethodChecking(pd, pd.getPromisedFor());
      return pd;
    } else {
      return null;
    }
  }

  private static NullablePromiseDrop scrubNullable(
      final IAnnotationScrubberContext context, final NullableNode n) {
    final IRNode promisedFor = n.getPromisedFor();
    // Cannot be on a primitive type
    boolean good = RulesUtilities.checkForReferenceType(context, n, "Nullable");

    // Cannot also be @NonNull
    if (getNonNull(promisedFor) != null) {
      good = false;
      context.reportError(n, CANNOT_BE_BOTH, "@NonNull", "@Nullable");
    }

    // Cannot also be @NonNull
    if (getRaw(promisedFor) != null) {
      good = false;
      context.reportError(n, CANNOT_BE_BOTH, "@Raw", "@Nullable");
    }

    if (good) {
      final NullablePromiseDrop pd = new NullablePromiseDrop(n);
      addAnnotationForMethodChecking(pd, promisedFor);
      return pd;
    } else {
      return null;
    }
  }	

  private static RawPromiseDrop scrubRaw(
      final IAnnotationScrubberContext context, final RawNode n) {
    final IRNode promisedFor = n.getPromisedFor();
    final IJavaType promisedForType =
        RulesUtilities.getPromisedForDeclarationType(context, n);
    boolean good = true;
    
    // Cannot also be @NonNull
    if (getNonNull(promisedFor) != null) {
      good = false;
      context.reportError(n, CANNOT_BE_BOTH, "@Raw", "@NonNull");
    }

    // Cannot be on a primitive type
    if (!RulesUtilities.checkForReferenceType(context, n, "Raw", promisedForType)) {
      good = false;
      // checkForReferenceType already creates errors
    } else {
      // TODO: Deal with Raw("static(...)") below

      // Cannot be on an array, etc.
      if (promisedForType instanceof IJavaDeclaredType) {
        // Cannot be an interface
        final IJavaDeclaredType dt = (IJavaDeclaredType) promisedForType;
        if (TypeUtil.isInterface(dt.getDeclaration())) {
          good = false;
          context.reportError(n, BAD_RAW_TYPE);
        } else {
          // Named type must be an ancestor of the annotated type
          final String upTo = n.getUpTo();
          if (!upTo.equals("*")) {
            final ITypeEnvironment typeEnv =
                context.getBinder(n.getPromisedFor()).getTypeEnvironment();
            final IJavaType upToType = typeEnv.findJavaTypeByName(upTo);
            if (upToType == null) {
              good = false;
              context.reportError("Unable to find upTo type: "+upTo, n);
            } else if (TypeUtil.isInterface(((IJavaDeclaredType) upToType).getDeclaration())) {
              // upTo cannot name an interface
              good = false;
              context.reportError(n, NOT_A_SUPERCLASS, upTo, promisedForType.getName());
            } else if (!typeEnv.isSubType(promisedForType, upToType)) {
              // upTo must name a superclass 
              good = false;
              context.reportError(n, NOT_A_SUPERCLASS, upTo, promisedForType.getName());
            }
          }
        }
      } else {
        good = false;
        context.reportError(n, BAD_RAW_TYPE);
      }
    }
    
    if (good) {
      final RawPromiseDrop pd = new RawPromiseDrop(n);
      addAnnotationForMethodChecking(pd, promisedFor);
      return pd;
    } else {
      return null;
    }
  }
  
  
  
  private static void addAnnotationForMethodChecking(
      final PromiseDrop<?> anno, final IRNode promisedFor) {
    /* Only care about annotations on formal parameters, receivers,
     * and return values.
     */
    final Operator op = JJNode.tree.getOperator(promisedFor);
    if (ParameterDeclaration.prototype.includes(op) ||
        ReceiverDeclaration.prototype.includes(op) ||
        ReturnValueDeclaration.prototype.includes(op)) {
      annosForMethodChecking.add(anno);
    }
  }
  
  private static final class NullableConsistencyChecker extends AbstractPromiseScrubber<PromiseDrop<?>> {
    public NullableConsistencyChecker() {
      super(ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, NONE,
          CONSISTENCY, ScrubberOrder.NORMAL,
          new String[] { NULLABLE, NONNULL, RAW }); 
    }

    @Override
    protected void processDrop(final PromiseDrop<?> a) {
      // TODO Auto-generated method stub
    }

    @Override
    protected boolean processUnannotatedMethodRelatedDecl(
        final IRNode unannotatedNode) {
      return true;
    }
    
    @Override
    protected Iterable<PromiseDrop<?>> getRelevantAnnotations() {
      return annosForMethodChecking;
    }
    
    @Override
    protected void finishRun() {
      annosForMethodChecking.clear();
    }
  }

	
	
	public static NonNullPromiseDrop getNonNull(final IRNode decl) {
	  return getBooleanDrop(nonNullRule.getStorage(), decl);
	}

	public static NullablePromiseDrop getNullable(final IRNode decl) {
	  return getBooleanDrop(nullableRule.getStorage(), decl);
	}
	
	public static RawPromiseDrop getRaw(final IRNode decl) {
	  return getBooleanDrop(rawRule.getStorage(), decl);
	}
}
