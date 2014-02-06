package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.bind.ISourceRefType;
import com.surelogic.aast.java.NamedTypeNode;
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
import com.surelogic.annotation.scrub.AbstractPosetConsistencyChecker;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
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
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.util.Poset;

public class NonNullRules extends AnnotationRules {	
  private static final String RAW_STAR = "*";
  
  private static final int CANNOT_BE_BOTH = 900;
  private static final int BAD_RAW_TYPE = 901;
  private static final int NOT_A_SUPERCLASS = 902;
  private static final int NO_SUCH_TYPE  = 903;
  
	public static final String NONNULL = "NonNull";
	public static final String NULLABLE = "Nullable";
	public static final String RAW = "Raw";
	public static final String CONSISTENCY = "NullableConsistency";
	
	private static final NonNullRules instance = new NonNullRules();
	private static final NonNull_ParseRule nonNullRule = new NonNull_ParseRule();
	private static final Nullable_ParseRule nullableRule = new Nullable_ParseRule();
	private static final Raw_ParseRule rawRule = new Raw_ParseRule();
	private static final NullableConsistencyChecker consistency = new NullableConsistencyChecker();
	private static final ConflictResolver resolver = new ConflictResolver();
	
	public static IPromiseDropStorage<NullablePromiseDrop> getNullableStorage() {
		return nullableRule.getStorage();
	}

  public static IPromiseDropStorage<NonNullPromiseDrop> getNonNullStorage() {
    return nonNullRule.getStorage();
  }
  
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
		registerConflictResolution(resolver);
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
			final NamedTypeNode upToType;
			if (upTo == null) {
				upToType = new NamedTypeNode(mappedOffset, "*");
			} else try {
				// TODO workaround for namedType issues
				String textToParse = upTo.contains("*") ? upTo : upTo+')';
				AASTAdaptor.Node upToE = (Node) SLParse.prototype.initParser(textToParse).rawUpToExpression().getTree();			
				upToType = (NamedTypeNode) upToE.finalizeAST(context);
			} catch (RecognitionException e) {
				handleRecognitionException(context, upTo, e);				
				return null;
			} catch (Exception e) {
				context.reportException(IAnnotationParsingContext.UNKNOWN, e);
				return null;
			}
			if (node.getType() == SLAnnotationsParser.NamedType) {
				// TODO deal with static(<type>)
				return new RawNode(mappedOffset, node.getText()+" -- "+upTo, upToType);
			}
			return new RawNode(mappedOffset, upTo, upToType);
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
          final NamedTypeNode typeName = n.getUpToType();
          final String upTo = typeName.getType();
          //final String upTo = n.getUpTo();
          //if (!upTo.equals(RAW_STAR)) {
          if (!typeName.getType().equals(RAW_STAR)) {
            final ITypeEnvironment typeEnv =
                context.getBinder(n.getPromisedFor()).getTypeEnvironment();
            //final IJavaType upToType = typeEnv.findJavaTypeByName(upTo);
            final ISourceRefType type = typeName.resolveType();
            final IJavaType upToType = type.getJavaType();            
            if (upToType == null) {
              good = false;
              context.reportError(n, NO_SUCH_TYPE, upTo);
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
      consistency.addRelevantDrop(anno);
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
	
	
  // ======================================================================
	
	private static final class NullableConsistencyChecker extends AbstractPosetConsistencyChecker<Element, NonNullPoset> {
    private final Pair DEFAULT_NULLABLE = new Pair(Elements.NULLABLE, Source.NO_PROMISE);
    private final Pair DEFAULT_NON_NULL = new Pair(Elements.NON_NULL, Source.NO_PROMISE);
	  
    public NullableConsistencyChecker() {
      super(NonNullPoset.INSTANCE, CONSISTENCY,
          new String[] { NULLABLE, NONNULL, RAW }, false);
    }

    @Override
    protected Element getValue(final PromiseDrop<?> a) {
      if (a instanceof NonNullPromiseDrop) {
        return Elements.NON_NULL;
      } else if (a instanceof NullablePromiseDrop) {
        return Elements.NULLABLE;
      } else if (a instanceof RawPromiseDrop) {
        final RawPromiseDrop pd = (RawPromiseDrop) a;
        final String upTo = pd.getUpTo();
        if (upTo.equals(RAW_STAR)) {
          return Elements.RAW_STAR;
        } else {
          final ITypeEnvironment typeEnv = getContext().getBinder(
              pd.getPromisedFor()).getTypeEnvironment();
          final IJavaType upToType = typeEnv.findJavaTypeByName(upTo);
          return new OverridingRawElement(upToType, typeEnv);
        }
      } else {
        throw new IllegalArgumentException(
            "No nullable state for " + a.getClass().getName());
      }
    }

    @Override
    protected Pair getValue(final IRNode n) {
      final NullablePromiseDrop pd1 = getNullable(n);
      if (pd1 != null) return getValueImpl(Elements.NULLABLE, pd1);

      final NonNullPromiseDrop pd2 = getNonNull(n);
      if (pd2 != null) return getValueImpl(Elements.NON_NULL, pd2);
      
      final RawPromiseDrop rawPD = getRaw(n);
      if (rawPD != null) {
        final String upTo = rawPD.getUpTo();
        if (upTo.equals(RAW_STAR)) {
          return getValueImpl(Elements.RAW_STAR, rawPD);
        } else {
          final ITypeEnvironment typeEnv = getContext().getBinder(
              rawPD.getPromisedFor()).getTypeEnvironment();
          final IJavaType upToType = typeEnv.findJavaTypeByName(upTo);
          return getValueImpl(new OverriddenRawElement(upToType), rawPD);
        }
      }
      
      /* Unannotated formal argument or return value is @Nullable.
       * Unannotated receiver or qualified receiver is @NonNUll.
       */
      // Unannotated => Nullable
      final Operator op = JJNode.tree.getOperator(n);
      if (ReceiverDeclaration.prototype.includes(op) ||
          QualifiedReceiverDeclaration.prototype.includes(op)) {
        return DEFAULT_NON_NULL;
      } else {
        return DEFAULT_NULLABLE;
      }
    }

    @Override
    protected Element getUnannotatedValue(final IRNode unannotatedNode) {
      final Operator op = JJNode.tree.getOperator(unannotatedNode);
      if (ReceiverDeclaration.prototype.includes(op) ||
          QualifiedReceiverDeclaration.prototype.includes(op)) {
        /* Receivers are NON_NULL because a method cannot be called on
         * "null". Qualified receivers are NON_NULL because nested classes
         * cannot be instantiated on "null".
         */
        return Elements.NON_NULL;
      } else {
        /* Formal arguments and return values default to NULLABLE */
        return Elements.NULLABLE;
      }
    }

    @Override
    protected String getAnnotationName(final Element value) {
      return value.getAnnotation();
    }

    @Override
    protected ProposedPromiseDrop proposePromise(
        final Element value, final String valueValue,
        final IRNode promisedFor, final IRNode parentMethod) {
      /* XXX Not doing this yet.  Don't forget to set the flag to true in the
       * constructor when I fix this.
       */
      return null;
    }
	  
	}
	
  // ======================================================================
	
	
	private static interface Element {
	  public String getAnnotation();
//	  public String getProposalName();
	  public boolean lessEq(Element other);
	}
	
	private static enum Elements implements Element {
	  NON_NULL("@NonNull") {
	    @Override
	    public boolean lessEq(final Element other) {
	      /* @NonNull is bottom and thus less than everything */
	      return true;
	    }
	  },
	  
	  NULLABLE("@Nullable") {
	    @Override
	    public boolean lessEq(final Element other) {
	      /* Equal to itself, but that is it. */
	      return other == NULLABLE;
	    }
	  },
	  
	  RAW_STAR("@Raw") {
	    @Override
	    public boolean lessEq(final Element other) {
	      /* equal to itself, but that is it. */
	      return other == RAW_STAR;
	    }
	  };
	  
	  private final String annotation;
	  
	  private Elements(final String a) {
	    annotation = a;
	  }
	  
	  @Override
	  public String getAnnotation() { return annotation; }
	}
	
	/* Need two different classes for Raw elements because Edwin says we should
	 * compare using the type environment from the overriding method.  This 
	 * way we can keep track of whether the raw element comes from the overridden
	 * or overriding method.
	 */
	private static abstract class AbstractRawElement implements Element {
    protected final IJavaType upTo;
    
    protected AbstractRawElement(final IJavaType u) {
      upTo = u;
    }
    
    @Override
    public String getAnnotation() {
      return "@Raw(upTo=\"" + upTo.toSourceText()  + "\")";
    }
  }
	
  private static final class OverriddenRawElement extends AbstractRawElement {
    public OverriddenRawElement(final IJavaType u) {
      super(u);
    }

    @Override 
    public boolean lessEq(final Element other) {
      if (other == Elements.RAW_STAR) {
        return true;
      } else if (other instanceof OverriddenRawElement) {
        throw new IllegalArgumentException(
            "Cannot compare two Raw annotations from overridden methods");
      } else if (other instanceof OverridingRawElement) {
        final OverridingRawElement otherRaw = (OverridingRawElement) other;
        return otherRaw.typeEnv.isSubType(this.upTo, otherRaw.upTo);
      } else {
        // Incomparable to @Nullable, greater than @NonNull
        return false;
      }
    }
  }
	
	
	private static final class OverridingRawElement extends AbstractRawElement {
	  private final ITypeEnvironment typeEnv;
	  
	  public OverridingRawElement(final IJavaType u, final ITypeEnvironment te) {
	    super(u);
	    typeEnv = te;
	  }

    @Override 
    public boolean lessEq(final Element other) {
      if (other == Elements.RAW_STAR) {
        return true;
      } else if (other instanceof OverridingRawElement) {
        throw new IllegalArgumentException(
            "Cannot compare two Raw annoations from overriding methods");
      } else if (other instanceof OverriddenRawElement) {
        final OverriddenRawElement otherRaw = (OverriddenRawElement) other;
        return this.typeEnv.isSubType(this.upTo, otherRaw.upTo);
      } else {
        // Incomparable to @Nullable, greater than @NonNull
        return false;
      }
    }
	}
	
	private static final class NonNullPoset implements Poset<Element> {
	  public static final NonNullPoset INSTANCE = new NonNullPoset();
	  
	  private NonNullPoset() {
	    super();
	  }
	  
    @Override
    public boolean lessEq(final Element v1, final Element v2) {
      return v1.lessEq(v2);
    }
	}
	
	static class ConflictResolver implements IAnnotationConflictResolver {
		@Override
		public void resolve(Context context) {
			List<IAASTRootNode> related = new ArrayList<IAASTRootNode>(); 
			related.addAll(context.getAASTs(NonNullNode.class));
			related.addAll(context.getAASTs(RawNode.class));
			related.addAll(context.getAASTs(NullableNode.class));
			removeLowOriginAASTs(context, related);
		}		
	}
}
