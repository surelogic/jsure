package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;

import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class NonNullRules extends AnnotationRules {	
	public static final String NONNULL = "NonNull";
	public static final String NULLABLE = "Nullable";

	private static final NonNullRules instance = new NonNullRules();
	private static final NonNull_ParseRule nonNullRule = new NonNull_ParseRule();
	private static final Nullable_ParseRule nullableRule = new Nullable_ParseRule();
	
	private NonNullRules() {
		// Just to make it a singleton
	}
	
	public static AnnotationRules getInstance() {
		return instance;
	}
	
	@Override
	public void register(PromiseFramework fw) {
		fw.registerParseDropRule(nonNullRule);
		fw.registerParseDropRule(nullableRule);
	}

	static Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
		if (context.getSourceType() == AnnotationSource.JAVADOC) {
			return parser.nonNullList().getTree();
		}
		if (MethodDeclaration.prototype.includes(context.getOp())) {
			// Only allows "return"
			return parser.nonNullMethod();
		} else {
			// For parameters and fields 
			return parser.nothing().getTree();
		}
	}
	
	public static class NonNull_ParseRule
	extends DefaultBooleanAnnotationParseRule<NonNullNode,NonNullPromiseDrop> {
		public NonNull_ParseRule() {
			super(NONNULL, fieldMethodParamDeclOps, NonNullNode.class);
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
				protected PromiseDrop<NonNullNode> makePromiseDrop(NonNullNode a) {
					// TODO
					return storeDropIfNotNull(a, new NonNullPromiseDrop(a));
				}
			};
		}   
	}
	
	public static class Nullable_ParseRule
	extends DefaultBooleanAnnotationParseRule<NullableNode,NullablePromiseDrop> {
		public Nullable_ParseRule() {
			super(NULLABLE, fieldMethodParamDeclOps, NullableNode.class);
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
					this, ScrubberType.UNORDERED) {
				@Override
				protected PromiseDrop<NullableNode> makePromiseDrop(NullableNode a) {
					// TODO
					return storeDropIfNotNull(a, new NullablePromiseDrop(a));
				}
			};
		}   
	}
}
