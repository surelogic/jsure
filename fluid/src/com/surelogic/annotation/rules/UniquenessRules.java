package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.*;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.aast.promise.ReadOnlyNode;
import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.analysis.uniqueness.plusFrom.traditional.store.State;
import com.surelogic.annotation.AnnotationLocation;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.DefaultBooleanAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.AbstractPosetConsistencyChecker;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutableRefPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.ReadOnlyPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedTypeDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class UniquenessRules extends AnnotationRules {
  public static final String UNIQUE = "Unique";
  public static final String BORROWED = "Borrowed";
  public static final String READONLY = "ReadOnly";
  public static final String CONSISTENCY = "UniquenessConsistency";
  
  private static final AnnotationRules instance = new UniquenessRules();
  
  private static final Readonly_ParseRule readonlyRule = new Readonly_ParseRule();
  private static final Unique_ParseRule uniqueRule     = new Unique_ParseRule();
  private static final Borrowed_ParseRule borrowedRule = new Borrowed_ParseRule();
  private static final UniquenessConsistencyChecker consistencyChecker = new UniquenessConsistencyChecker();

  
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  public static boolean isUnique(IRNode vdecl) {
    return getUnique(vdecl) != null;
  }
  
  public static UniquePromiseDrop getUnique(IRNode vdecl) {
    return getBooleanDrop(uniqueRule.getStorage(), vdecl);
  }
  
  public static boolean isBorrowed(IRNode vdecl) {
    return getBorrowed(vdecl) != null;
  }
  
  public static BorrowedPromiseDrop getBorrowed(IRNode vdecl) {
    return getBooleanDrop(borrowedRule.getStorage(), vdecl);
  }
  
  public static PromiseDrop<? extends IAASTRootNode> getBorrowedReceiver(
      final IRNode mdecl) {
    PromiseDrop<? extends IAASTRootNode> drop = 
      UniquenessRules.getBorrowed(JavaPromise.getReceiverNodeOrNull(mdecl));
    if (drop == null && ConstructorDeclaration.prototype.includes(mdecl)) {
      drop = UniquenessRules.getUnique(
          JavaPromise.getReturnNodeOrNull(mdecl));
    }
    return drop;
  }

  public static boolean isBorrowedReceiver(final IRNode mdecl) {
    return getBorrowedReceiver(mdecl) != null;
  }

  public static boolean isReadOnly(IRNode vdecl) {
	  return getReadOnly(vdecl) != null;
  }
  
  public static ReadOnlyPromiseDrop getReadOnly(IRNode vdecl) {
	  return getBooleanDrop(readonlyRule.getStorage(), vdecl);
  }
  
  /**
   * Meant for testing
   */
  public static void setIsUnique(IRNode vdecl, boolean val) {
    if (!val) {
      if (isUnique(vdecl)) {
        throw new UnsupportedOperationException();
      }
    } else {
      uniqueRule.getStorage().add(vdecl, new UniquePromiseDrop(null));
    }
  }
  
  /**
   * Meant for testing
   */
  public static void setIsBorrowed(IRNode vdecl, boolean val) {
    if (!val) {
      if (isBorrowed(vdecl)) {
        throw new UnsupportedOperationException();
      }
    } else {
      borrowedRule.getStorage().add(vdecl, new BorrowedPromiseDrop(null));
    }
  }
  
  
  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, readonlyRule);
    registerParseRuleStorage(fw, uniqueRule);
    registerParseRuleStorage(fw, borrowedRule);
    registerScrubber(fw, consistencyChecker);
  }

  private static abstract class AbstractParseRule<N extends IAASTRootNode,D extends PromiseDrop<N>> 
  extends DefaultBooleanAnnotationParseRule<N,D> {
	  protected AbstractParseRule(String name, Operator[] ops, Class<N> dt) {
		  super(name, ops, dt);
	  }

	  @Override
	  protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
		  final Operator op = context.getOp();
		  if (FieldDeclaration.prototype.includes(op)) {
			  return parser.nothing().getTree();
		  }
		  if (ParameterDeclaration.prototype.includes(op)) {
			  return parser.nothing().getTree();
		  }
		  final boolean isJavadoc = context.getSourceType() == AnnotationSource.JAVADOC;
		  if (MethodDeclaration.prototype.includes(op)) {        
			  return isJavadoc ? parser.uniqueJavadocMethod().getTree() : 
				  parser.uniqueJava5Method().getTree();
		  }     
		  /* else must be a constructor: this is only allowed for javadoc
		   * annotations so that they can name unique parameters
		   */
	      if (isJavadoc) {
	    	  return parser.uniqueJavadocConstructor().getTree();
	      }
	      return parser.uniqueJava5Constructor().getTree();
	  }
	  @Override
	  protected AnnotationLocation translateTokenType(int type, Operator op) {
		  AnnotationLocation loc = super.translateTokenType(type, op);
		  if (loc == AnnotationLocation.DECL && MethodDeclaration.prototype.includes(op)) {
			  return AnnotationLocation.RETURN_VAL;
		  }
		  return loc;
	  }
  }
  
  public static class Readonly_ParseRule extends AbstractParseRule<ReadOnlyNode, ReadOnlyPromiseDrop> {
	public Readonly_ParseRule() {
		super(READONLY, fieldFuncParamDeclOps, ReadOnlyNode.class);
	}
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new ReadOnlyNode(offset);
    }
    @Override
    protected IPromiseDropStorage<ReadOnlyPromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ReadOnlyPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
    	// TODO scrub
    	return new AbstractAASTScrubber<ReadOnlyNode, ReadOnlyPromiseDrop>(
    	    this, ScrubberType.UNORDERED, BORROWED) {
//    	    RegionRules.EXPLICIT_BORROWED_IN_REGION,
//    	    RegionRules.SIMPLE_BORROWED_IN_REGION) {
	  		@Override
		  	protected ReadOnlyPromiseDrop makePromiseDrop(ReadOnlyNode n) {
			  	return storeDropIfNotNull(n, scrubReadOnly(getContext(), n));
  			}    		
    	};
    }
    
    private ReadOnlyPromiseDrop scrubReadOnly(
        final IAnnotationScrubberContext context, final ReadOnlyNode n) {
      // must be a reference type variable
      boolean isGood = RulesUtilities.checkForReferenceType(context, n, "ReadOnly");
      
      /* Cannot also be borrowed, unless the annotation is on a field.
       * Implies we don't have to check for BorrowedInRegion, because that
       * can only appear on a field, and would thus be legal.
       */
      final IRNode promisedFor = n.getPromisedFor();
      final boolean fromField = VariableDeclarator.prototype.includes(promisedFor);
      if (isBorrowed(promisedFor) && !fromField) {
        context.reportError(n, "Cannot be annotated with both @Borrowed and @ReadOnly");
        isGood = false;
      }
      
      if (isGood) {
        final ReadOnlyPromiseDrop readOnlyPromiseDrop = new ReadOnlyPromiseDrop(n);
        if (!fromField) consistencyChecker.addRelevantDrop(readOnlyPromiseDrop); //addUniqueAnnotation(readOnlyPromiseDrop);
        return readOnlyPromiseDrop;
      } else {
        return null;
      }
    }
  }
  
  public static class Unique_ParseRule 
  extends AbstractParseRule<UniqueNode,UniquePromiseDrop> {
    public Unique_ParseRule() {
      super(UNIQUE, fieldFuncParamDeclOps, UniqueNode.class);
    }
   
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new UniqueNode(offset,JavaNode.isSet(mods, JavaNode.ALLOW_READ));
    }
    @Override
    protected IPromiseDropStorage<UniquePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), UniquePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<UniqueNode, UniquePromiseDrop>(
          this, ScrubberType.UNORDERED, LockRules.IMMUTABLE_REF) {
        @Override
        protected PromiseDrop<UniqueNode> makePromiseDrop(UniqueNode a) {
          return storeDropIfNotNull(a, scrubUnique(getContext(), a));          
        }
      };
    }
    
    private UniquePromiseDrop scrubUnique(
        final IAnnotationScrubberContext context, final UniqueNode a) {
      // must be a reference type variable
      boolean good = RulesUtilities.checkForReferenceType(context, a, "Unique");
      boolean fromField = false;
      
      // Unique fields must not be volatile or static final
      final IRNode promisedFor = a.getPromisedFor();
      final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
      if (VariableDeclarator.prototype.includes(promisedForOp)) {
        fromField = true;
        if (TypeUtil.isVolatile(promisedFor)) {
          good = false;
          context.reportError(a, "@Unique cannot be used on a volatile field");
        }
      }

      if (UniquenessRules.isBorrowed(promisedFor)) {
        context.reportError(
            a, "Cannot be annotated with both @Unique and @Borrowed");
        good = false;
      }
      /* Cannot check this here any more, because ExplitBorrowedInRegion's 
       * dependency on InRegion would cause a cycle if we keep ImmutableRef
       * dependent on ExplcitBorrowedInRegion (which we need to make
       * Uniqueness transitively dependent on ExplicitBorrowedInRegion).
       * So, we check this now in ExplicitBorrowedInRegion, which for the same
       * reason now has a transitive dependency on Unique.
       */
//      if (RegionRules.getExplicitBorrowedInRegion(promisedFor) != null) {
//        context.reportError(
//            a, "Cannot be annotated with both @Unique and @BorrowedInRegion");
//        good = false;
//      }
      if (RegionRules.getSimpleBorrowedInRegion(promisedFor) != null) {
        context.reportError(
            a, "Cannot be annotated with both @Unique and @BorrowedInRegion");
        good = false;
      }
      if (UniquenessRules.getReadOnly(promisedFor) != null) {
        context.reportError(
            a, "Cannot be annotated with both @Unique and @ReadOnly");
        good = false;
      }
      if (LockRules.isImmutableRef(promisedFor)) {
        context.reportError(
            a, "Cannot be annotated with both @Unique and @Immutable");
        good = false;
      }
      
      // Warn if both @Unique("return") and @Borrowed("this")
      if (ReturnValueDeclaration.prototype.includes(promisedFor)) {
        final IRNode decl = JavaPromise.getPromisedFor(promisedFor);
        if (ConstructorDeclaration.prototype.includes(decl) && 
            isBorrowed(JavaPromise.getReceiverNodeOrNull(decl))) {
          context.reportWarning(a, "Use of both @Unique(\"return\") and @Borrowed(\"this\") on a constructor is redundant");
        }
      }
      
      if (good) {
        final UniquePromiseDrop uniquePromiseDrop = new UniquePromiseDrop(a);
        if (!fromField) consistencyChecker.addRelevantDrop(uniquePromiseDrop); //addUniqueAnnotation(uniquePromiseDrop);
        return uniquePromiseDrop;
      } else {
        return null;
      }
    }
  }

  
  public static class Borrowed_ParseRule
  extends DefaultBooleanAnnotationParseRule<BorrowedNode,BorrowedPromiseDrop> {
    public Borrowed_ParseRule() {
      super(BORROWED, fieldMethodParamInnerTypeDeclOps, BorrowedNode.class);
    }
   
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      if (context.getSourceType() == AnnotationSource.JAVADOC) {
        // TODO how to handle allowReturn
        return parser.borrowedList().getTree();
      }
      if (ParameterDeclaration.prototype.includes(context.getOp()) ||
          FieldDeclaration.prototype.includes(context.getOp())) {
        return parser.nothing().getTree();
      } else if (NestedTypeDeclaration.prototype.includes(context.getOp())) {
        return parser.borrowedType().getTree();
      } else if (NestedTypeDeclaration.prototype.includes(context.getOp())) {
        return parser.borrowedNestedType().getTree();
      }
      return parser.borrowedFunction().getTree();
    }
    
    @Override
    protected ProposedPromiseDrop.Builder proposeOnRecognitionException(IAnnotationParsingContext context, 
  		  String badContents, String okPrefix) {
      ProposedPromiseDrop.Builder p = context.startProposal(Borrowed.class);
      if (p == null) {
    	  return null;
      }
      p.replaceSameExisting(badContents);
      if (SomeFunctionDeclaration.prototype.includes(context.getOp())) {
          return p.setValue("this");
      }
      return p.setValue(null);
    }
    
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new BorrowedNode(offset, JavaNode.isSet(mods, JavaNode.ALLOW_RETURN));
    }
    @Override
    protected IPromiseDropStorage<BorrowedPromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), BorrowedPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<BorrowedNode, BorrowedPromiseDrop>(
          this, ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<BorrowedNode> makePromiseDrop(BorrowedNode a) {
          return storeDropIfNotNull(a, scrubBorrowed(getContext(), a));
        }
      };
    }    
    
    private BorrowedPromiseDrop scrubBorrowed(
        final IAnnotationScrubberContext context, final BorrowedNode a) {
      // must be a reference type variable
      boolean good = RulesUtilities.checkForReferenceType(context, a, "Borrowed");
      boolean fromField = false;
      
      final IRNode promisedFor = a.getPromisedFor();
      final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
      if (VariableDeclarator.prototype.includes(promisedForOp)) {
        fromField = true;
        if (!TypeUtil.isFinal(promisedFor)) {
          context.reportError(a, "@Borrowed fields must be final");
          good = false;
        }
        if (TypeUtil.isStatic(promisedFor)) {
          context.reportError(a, "@Borrowed fields must not be static");
          good = false;
        }
      } else if (ParameterDeclaration.prototype.includes(promisedForOp)) {
        if (a.allowReturn() && !TypeUtil.isFinal(promisedFor)) {
          context.reportError(a, "@Borrowed(allowReturn=true) parameters must be final");
          good = false;
        }
      }
      
      if (good) {
        final BorrowedPromiseDrop borrowedPromiseDrop = new BorrowedPromiseDrop(a);
        if (!fromField) consistencyChecker.addRelevantDrop(borrowedPromiseDrop); // addUniqueAnnotation(borrowedPromiseDrop);        
        return borrowedPromiseDrop;
      } else {
        return null;
      }
    }
  }
  

  
  private static final class UniquenessConsistencyChecker extends AbstractPosetConsistencyChecker<State, State.Lattice> {
    private final Pair SHARED = new Pair(State.SHARED, Source.NO_PROMISE);

    public UniquenessConsistencyChecker() {
      super(State.lattice, CONSISTENCY, new String[] { UNIQUE }, true);      
    }

    @Override
    protected State getValue(final PromiseDrop<?> a) {
      if (a instanceof UniquePromiseDrop) {
        return ((UniquePromiseDrop) a).allowRead() ? State.UNIQUEWRITE : State.UNIQUE;
      } else if (a instanceof ImmutableRefPromiseDrop) {
        return State.IMMUTABLE;
      } else if (a instanceof ReadOnlyPromiseDrop) {
        return State.READONLY;
      } else if (a instanceof BorrowedPromiseDrop) {
        return State.BORROWED;
      } else {
        throw new IllegalArgumentException(
            "No uniqueness state for " + a.getClass().getName());
      }
    }

    @Override
    protected Pair getValue(final IRNode n) {
      PromiseDrop<?> d = getUnique(n);
      if (d == null) {
        d = LockRules.getImmutableRef(n);
        if (d == null) {
          d = getReadOnly(n);       
          if (d == null) {
            d = getBorrowed(n);
            if (d == null) {
              return SHARED;
            }
          }
        }
      }
      // d should be non-null;
      return getValueImpl(getValue(d), d);
    }

    @Override
    protected State getUnannotatedValue() {
      return State.SHARED;
    }

    @Override
    protected String getAnnotationName(final State value) {
      return value.getAnnotation();
    }

    @Override
    protected ProposedPromiseDrop proposePromise(
        final State value, final String valueValue,
        final IRNode promisedFor, final IRNode parentMethod) {
      return new ProposedPromiseDrop(value.getProposedPromiseName(), valueValue,
          promisedFor, parentMethod, Origin.PROBLEM);
    }
    
  }
  
  // For LockRules
  public static void addRelevantUniqueAnnotation(final PromiseDrop<?> a) {
    consistencyChecker.addRelevantDrop(a);
  }
}
