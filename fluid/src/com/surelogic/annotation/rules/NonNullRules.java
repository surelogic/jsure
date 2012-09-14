package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.RawPromiseDrop;

import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class NonNullRules extends AnnotationRules {	
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
		fw.registerParseDropRule(nonNullRule);
		fw.registerParseDropRule(nullableRule);
		fw.registerParseDropRule(rawRule);
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
				return parser.nothing();
			}
			else if (MethodDeclaration.prototype.includes(context.getOp())) {
				return parser.rawExpression();
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
				protected PromiseDrop<RawNode> makePromiseDrop(RawNode a) {
					// TODO
					return storeDropIfNotNull(a, new RawPromiseDrop(a));
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
