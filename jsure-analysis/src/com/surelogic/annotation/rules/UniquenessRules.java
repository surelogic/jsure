package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.*;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.analysis.uniqueness.classic.sideeffecting.store.State;
import com.surelogic.annotation.AnnotationLocation;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.DefaultBooleanAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.AbstractPosetConsistencyChecker;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.AnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedTypeDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class UniquenessRules extends AnnotationRules {
  public static final String UNIQUE = "Unique";
  public static final String BORROWED = "Borrowed";
  public static final String CONSISTENCY = "UniquenessConsistency";
  
  private static final AnnotationRules instance = new UniquenessRules();
  
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
  
  /**
   * Meant for testing
   */
  public static void setIsUnique(IRNode vdecl, boolean val) {
    if (!val) {
      if (isUnique(vdecl)) {
        throw new UnsupportedOperationException();
      }
    } else {
      uniqueRule.getStorage().add(vdecl, new UniquePromiseDrop(new UniqueNode(0)));
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
      borrowedRule.getStorage().add(vdecl, new BorrowedPromiseDrop(new BorrowedNode(0)));
    }
  }
  
  
  @Override
  public void register(PromiseFramework fw) {
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
  
  public static class Unique_ParseRule 
  extends AbstractParseRule<UniqueNode,UniquePromiseDrop> {
    public Unique_ParseRule() {
      super(UNIQUE, fieldFuncParamDeclOps, UniqueNode.class);
    }
   
    @Override
    protected String getErrorText(IAnnotationParsingContext context, RecognitionException e, String attr, String badContents, String okPrefix) {
    	final Operator op = context.getOp();
    	if (ConstructorDeclaration.prototype.includes(op)) {
    		return "Constructors may only be annotated @Unique(\"return\") indicating that "+
    				"the newly constructed object is not aliased during object construction";
    	}
    	else if (MethodDeclaration.prototype.includes(op)) {
    		return "Methods may only be annotated @Unique(\"return\"), @Unique(\"this\"), or @Unique(\"return, this\")";	
    	}
    	else if (FieldDeclaration.prototype.includes(op)) {
    		return "Fields may only be annotated @Unique";
    	}
    	else if (VariableDeclaration.prototype.includes(op)) {
    		return "Locals may only be annotated @Unique";
    	}
    	return super.getErrorText(context, e, attr, badContents, okPrefix);    	
    }
    
    @Override
    protected ProposedPromiseDrop.Builder proposeOnRecognitionException(IAnnotationParsingContext context, String badContents, String okPrefix) {
    	final boolean emptyContents = badContents == null || badContents.length() == 0;
    	final boolean onSomeFunc = SomeFunctionDeclaration.prototype.includes(context.getOp());
    	if (onSomeFunc ^ emptyContents) {
    	  return null;
    	}    	
        ProposedPromiseDrop.Builder p = context.startProposal(Unique.class);
        if (p == null) {
      	  return null;
        }
        return p.setValue(onSomeFunc ? "return" : "").replaceSameExisting(badContents);
    }
    
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new UniqueNode(offset);
    }
    @Override
    protected IPromiseDropStorage<UniquePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), UniquePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<UniqueNode, UniquePromiseDrop>(
          this, ScrubberType.UNORDERED, RegionRules.REGION, UniquenessRules.BORROWED) {
        @Override
        protected PromiseDrop<UniqueNode> makePromiseDrop(UniqueNode a) {
          return storeDropIfNotNull(a, scrubUnique(getContext(), a));          
        }
      };
    }
    
    private UniquePromiseDrop scrubUnique(
        final AnnotationScrubberContext context, final UniqueNode a) {
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
          context.reportModelingProblem(a, "@Unique cannot be used on a volatile field");
        }
      }

      if (UniquenessRules.isBorrowed(promisedFor)) {
        context.reportModelingProblem(
            a, "Cannot be annotated with both @Unique and @Borrowed");
        good = false;
      }
      
      // Warn if both @Unique("return") and @Borrowed("this")
      if (ReturnValueDeclaration.prototype.includes(promisedFor)) {
        final IRNode decl = JavaPromise.getPromisedFor(promisedFor);
        if (ConstructorDeclaration.prototype.includes(decl) && 
            isBorrowed(JavaPromise.getReceiverNodeOrNull(decl))) {
          context.reportError("Use of both @Unique(\"return\") and @Borrowed(\"this\") on a constructor is redundant", a);
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
      return new BorrowedNode(offset);
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
        final AnnotationScrubberContext context, final BorrowedNode a) {
      // must be a reference type variable
      boolean good = RulesUtilities.checkForReferenceType(context, a, "Borrowed");
      boolean fromField = false;
      
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
        return State.UNIQUE;
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
        d = getBorrowed(n);
        if (d == null) {
          return SHARED;
        }
      }
      // d should be non-null;
      return getValueImpl(getValue(d), d);
    }

    @Override
    protected State getUnannotatedValue(final IRNode unannotatedNode) {
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
      return new Builder(value.getProposedPromiseName(), promisedFor, parentMethod).setValue(valueValue).setOrigin(Origin.PROBLEM)
          .build();
    }
  }
  
  // For LockRules
  public static void addRelevantUniqueAnnotation(final PromiseDrop<?> a) {
    consistencyChecker.addRelevantDrop(a);
  }
}
