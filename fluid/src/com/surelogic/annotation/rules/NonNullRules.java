package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.aast.promise.NullableNode;
import com.surelogic.aast.promise.RawNode;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.DefaultBooleanAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;

public class NonNullRules extends AnnotationRules {	
  private static final int CANNOT_BE_BOTH = 900;
  
	public static final String NONNULL = "NonNull";
	public static final String NULLABLE = "Nullable";
	public static final String RAW = "Raw";
	
	private static final NonNullRules instance = new NonNullRules();
	private static final NonNull_ParseRule nonNullRule = new NonNull_ParseRule();
	private static final Nullable_ParseRule nullableRule = new Nullable_ParseRule();
	private static final Raw_ParseRule rawRule = new Raw_ParseRule();
	
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
	} 

	public static class Raw_ParseRule extends DefaultBooleanAnnotationParseRule<RawNode,RawPromiseDrop> {
		public Raw_ParseRule() {
			super(RAW, methodOrParamDeclOps, RawNode.class);
		}

		/**
		 * @Raw(upTo="*") — on formal parameter of type T
		 * @Raw(upTo="<type>") — on formal parameter of type T
		 * @Raw(upTo="*|<type>", value="this") — On method of type T
		 * @Raw(upTo="*|<type>", value="return") — On method with return type T
		 * @Raw(upTo="*", value="static(<type>)") — On method/constructor
		 */
		@Override
		protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
			if (ParameterDeclaration.prototype.includes(context.getOp())) {
				return parser.nothing().getTree();
			}
			else if (MethodDeclaration.prototype.includes(context.getOp())) {
				return parser.rawExpression().getTree();
			}
			throw new NotImplemented();
		}		
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
			return new RawNode(offset, context.getProperty(AnnotationVisitor.UPTO));
		}
		@Override
		protected IPromiseDropStorage<RawPromiseDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(), RawPromiseDrop.class);
		}
		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<RawNode, RawPromiseDrop>(
					this, ScrubberType.UNORDERED) {
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
					this, ScrubberType.UNORDERED, NONNULL) {
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
    return !good ? null : new NonNullPromiseDrop(n);
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
//      context.reportError(n, "Cannot be both @NonNull and @Nullable");
    }

    // TODO: Cannot also be @Raw

    return !good ? null : new NullablePromiseDrop(n);
  }	

  private static RawPromiseDrop scrubRaw(
      final IAnnotationScrubberContext context,
      final RawNode n) {
    // Cannot be on a primitive type
    boolean good = RulesUtilities.checkForReferenceType(context, n, "Raw");
    return !good ? null : new RawPromiseDrop(n);
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
