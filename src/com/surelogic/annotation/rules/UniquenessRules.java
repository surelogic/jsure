package com.surelogic.annotation.rules;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberOrder;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.analysis.uniqueness.store.State;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import com.surelogic.annotation.scrub.AbstractPromiseScrubber;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

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
  
  private static final Set<PromiseDrop<? extends IAASTRootNode>> uniquenessTags =
      new HashSet<PromiseDrop<? extends IAASTRootNode>>();
  
  
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
		super(READONLY, fieldMethodParamDeclOps, ReadOnlyNode.class);
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
    	    this, ScrubberType.UNORDERED,
    	    RegionRules.EXPLICIT_BORROWED_IN_REGION,
    	    RegionRules.SIMPLE_BORROWED_IN_REGION) {
	  		@Override
		  	protected ReadOnlyPromiseDrop makePromiseDrop(ReadOnlyNode n) {
			  	return storeDropIfNotNull(n, scrubReadOnly(getContext(), n));
  			}    		
    	};
    }
    
    private ReadOnlyPromiseDrop scrubReadOnly(
        final IAnnotationScrubberContext context, final ReadOnlyNode n) {
      // must be a reference type variable
      boolean isGood = checkForReferenceType(context, n, "ReadOnly");
      
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
        if (!fromField) addUniqueAnnotation(readOnlyPromiseDrop);
        return readOnlyPromiseDrop;
      } else {
        return null;
      }
    }
  }
  
  public static class Unique_ParseRule 
  extends AbstractParseRule<UniqueNode,UniquePromiseDrop> {
    public Unique_ParseRule() {
      super(UNIQUE, fieldMethodParamDeclOps, UniqueNode.class);
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
          this, ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY,
          LockRules.IMMUTABLE_REF) {
        @Override
        protected PromiseDrop<UniqueNode> makePromiseDrop(UniqueNode a) {
          return storeDropIfNotNull(a, scrubUnique(getContext(), a));          
        }
        
//        @Override
//        protected boolean processUnannotatedMethodRelatedDecl(
//            final IRNode unannotatedNode) {
//          /* Only care if the unannotated node is a return value declaration.
//           * Parameters (and receivers) can remove uniqueness requirements
//           * with out harm. 
//           */
//          final Operator op = JJNode.tree.getOperator(unannotatedNode);
//          if (ReturnValueDeclaration.prototype.includes(op)) {
//            final IRNode mdecl = JavaPromise.getPromisedFor(unannotatedNode);
//            boolean good = true;
//            for (final IBinding context : getContext().getBinder().findOverriddenParentMethods(mdecl)) {
//              final IRNode parentMethod = context.getNode();
//              final IRNode parentReturn = JavaPromise.getReturnNode(parentMethod);
//              final UniquePromiseDrop parentUnique = getUnique(parentReturn);
//              if (parentUnique != null) {
//                // Parent has unique return, we should have one too
//                good = false;
//                getContext().reportError(mdecl,
//                    "Cannot remove unique return value from annotations of {0}",
//                    JavaNames.genQualifiedMethodConstructorName(parentMethod));
//              }
//            }
//            return good;
//          } else {
//            return true;
//          }
//        }
      };
    }
    
    private UniquePromiseDrop scrubUnique(
        final IAnnotationScrubberContext context, final UniqueNode a) {
      // must be a reference type variable
      boolean good = checkForReferenceType(context, a, "Unique");
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
        
        if (TypeUtil.isStatic(promisedFor) && TypeUtil.isFinal(promisedFor)) {
          good = false;
          context.reportError(a, "@Unique cannot be used on a static final field: use @UniqueInRegion instead");
        }
      }

      if (UniquenessRules.isBorrowed(promisedFor)) {
        context.reportError(
            a, "Cannot be annotated with both @Unique and @Borrowed");
        good = false;
      }
      if (RegionRules.getExplicitBorrowedInRegion(promisedFor) != null) {
        context.reportError(
            a, "Cannot be annotated with both @Unique and @BorrowedInRegion");
        good = false;
      }
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
      
//      if (good) {
//        /* Check consistency of annotated parameters and receiver declarations.
//         * Cannot add @Unique to a previously unannotated parameter/receiver.
//         */
//        if (ReceiverDeclaration.prototype.includes(promisedForOp)) {
//          good = consistencyCheckReceiver(context, promisedFor);
//        } else if(ParameterDeclaration.prototype.includes(promisedForOp)) {
//          good = consistencyCheckParameter(context, promisedFor);          
//        }
//      }
      
      if (good) {
        final UniquePromiseDrop uniquePromiseDrop = new UniquePromiseDrop(a);
        if (!fromField) addUniqueAnnotation(uniquePromiseDrop);
        return uniquePromiseDrop;
      } else {
        return null;
      }
    }
    
    private boolean consistencyCheckReceiver(
        final IAnnotationScrubberContext context, final IRNode rcvrDecl) {
      final IRNode mdecl = JavaPromise.getPromisedFor(rcvrDecl);
      boolean good = true;
      for (final IBinding bc : context.getBinder().findOverriddenParentMethods(mdecl)) {
        final IRNode parentMethod = bc.getNode();
        final IRNode parentRcvr = JavaPromise.getReceiverNode(parentMethod);
        final UniquePromiseDrop parentUnique = getUnique(parentRcvr);
        if (parentUnique == null) {
          // Parent isn't unique, we cannot be
          good = false;
          context.reportError(mdecl,
              "Cannot add unique receiver declaration to annotations of {0}",
              JavaNames.genQualifiedMethodConstructorName(parentMethod));
        }
      }
      return good;
    }
    
    private boolean consistencyCheckParameter(
        final IAnnotationScrubberContext context, final IRNode paramDecl) {
      final IRNode mdecl = JJNode.tree.getParent(JJNode.tree.getParent(paramDecl));
      boolean good = true;
      for (final IBinding bc : context.getBinder().findOverriddenParentMethods(mdecl)) {
        final IRNode parentMethod = bc.getNode();
        
        // find the same parameter in the original
        final IRNode params = MethodDeclaration.getParams(mdecl);
        final IRNode parentParams = MethodDeclaration.getParams(parentMethod);
        final Iteratable<IRNode> paramsIter = Parameters.getFormalIterator(params);
        final Iteratable<IRNode> parentParamsIter = Parameters.getFormalIterator(parentParams);
        while (paramsIter.hasNext()) {
          final IRNode p = paramsIter.next();
          final IRNode p2 = parentParamsIter.next();
          if (p == paramDecl) { // found the original param
            final UniquePromiseDrop parentUnique = getUnique(p2);
            if (parentUnique == null) {
              // Parent isn't unique, we cannot be
              good = false;
              context.reportError(paramDecl,
                  "Cannot add unique declaration to parameter {0} of {1}",
                  ParameterDeclaration.getId(p2),
                  JavaNames.genQualifiedMethodConstructorName(parentMethod));
            }
          }
        }
      }
      return good;
    }
  }

  
  public static class Borrowed_ParseRule
  extends DefaultBooleanAnnotationParseRule<BorrowedNode,BorrowedPromiseDrop> {
    public Borrowed_ParseRule() {
      super(BORROWED, fieldMethodParamDeclOps, BorrowedNode.class);
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
      }
      return parser.borrowedFunction().getTree();
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
      return new AbstractAASTScrubber<BorrowedNode, BorrowedPromiseDrop>(this,
          ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
        @Override
        protected PromiseDrop<BorrowedNode> makePromiseDrop(BorrowedNode a) {
          return storeDropIfNotNull(a, scrubBorrowed(getContext(), a));
        }
//        
//        @Override
//        protected boolean processUnannotatedMethodRelatedDecl(
//            final IRNode promisedFor) {
//          /* Cannot remove @Borrowed annotations */
//          final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
//          if (ReceiverDeclaration.prototype.includes(promisedForOp)) {
//            return consistencyCheckReceiver(getContext(), promisedFor);
//          } else if(ParameterDeclaration.prototype.includes(promisedForOp)) {
//            return consistencyCheckParameter(getContext(), promisedFor);          
//          }
//          
//          return true;
//        }
        
        private boolean consistencyCheckReceiver(
            final IAnnotationScrubberContext context, final IRNode rcvrDecl) {
          final IRNode mdecl = JavaPromise.getPromisedFor(rcvrDecl);
          boolean good = true;
          for (final IBinding bc : context.getBinder().findOverriddenParentMethods(mdecl)) {
            final IRNode parentMethod = bc.getNode();
            final IRNode parentRcvr = JavaPromise.getReceiverNode(parentMethod);
            final BorrowedPromiseDrop parentBorrowed = getBorrowed(parentRcvr);
            if (parentBorrowed != null) {
              // Parent is borrowed, we must be too
              good = false;
              context.reportError(mdecl,
                  "Cannot remove borrowed receiver from annotations of {0}",
                  JavaNames.genQualifiedMethodConstructorName(parentMethod));
            }
          }
          return good;
        }
        
        private boolean consistencyCheckParameter(
            final IAnnotationScrubberContext context, final IRNode paramDecl) {
          final IRNode mdecl = JJNode.tree.getParent(JJNode.tree.getParent(paramDecl));
          boolean good = true;
          for (final IBinding bc : context.getBinder().findOverriddenParentMethods(mdecl)) {
            final IRNode parentMethod = bc.getNode();
            
            // find the same parameter in the original
            final IRNode params = MethodDeclaration.getParams(mdecl);
            final IRNode parentParams = MethodDeclaration.getParams(parentMethod);
            final Iteratable<IRNode> paramsIter = Parameters.getFormalIterator(params);
            final Iteratable<IRNode> parentParamsIter = Parameters.getFormalIterator(parentParams);
            while (paramsIter.hasNext()) {
              final IRNode p = paramsIter.next();
              final IRNode p2 = parentParamsIter.next();
              if (p == paramDecl) { // found the original param
                final BorrowedPromiseDrop parentBorrowed = getBorrowed(p2);
                if (parentBorrowed != null) {
                  // Parent is borrowed, we must be too
                  good = false;
                  context.reportError(paramDecl,
                      "Cannot remove borrowed declaration from parameter {0} of {1}",
                      ParameterDeclaration.getId(p2),
                      JavaNames.genQualifiedMethodConstructorName(parentMethod));
                }
              }
            }
          }
          return good;
        }
      };
    }    
    
    private BorrowedPromiseDrop scrubBorrowed(
        final IAnnotationScrubberContext context, final BorrowedNode a) {
      // must be a reference type variable
      boolean good = checkForReferenceType(context, a, "Borrowed");
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
      }

      if (good) {
        final BorrowedPromiseDrop borrowedPromiseDrop = new BorrowedPromiseDrop(a);
        if (!fromField) addUniqueAnnotation(borrowedPromiseDrop);        
        return borrowedPromiseDrop;
      } else {
        return null;
      }
    }
  }

  
  public static <T extends IAASTRootNode> boolean checkForReferenceType(
      final IAnnotationScrubberContext context, final T a, final String label) {
    final IRNode promisedFor = a.getPromisedFor();
    final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
    final IJavaType type;
    if (ParameterDeclaration.prototype.includes(promisedForOp)
        || ReceiverDeclaration.prototype.includes(promisedForOp)
        || VariableDeclarator.prototype.includes(promisedForOp)
        || QualifiedReceiverDeclaration.prototype.includes(promisedForOp)) {
      type = context.getBinder().getJavaType(promisedFor);
    } else if (ReturnValueDeclaration.prototype.includes(promisedForOp)) {
      final IRNode method = JavaPromise.getPromisedFor(promisedFor);
      type = context.getBinder().getJavaType(method);
    } else {
      LOG.log(Level.SEVERE, "Unexpected "+promisedForOp.name()+": "+DebugUnparser.toString(promisedFor), new Throwable());
      return false;
    }
    
    if (type instanceof IJavaPrimitiveType) {
      context.reportError(a, "@{0} may not be used with primitive types", label);
      return false;
    } else if (type == JavaTypeFactory.voidType) {
      context.reportError(a, "@{0} may not be used with void types", label);
      return false;
    } else {
      return true;
    }
  }
  
  
  public static void addUniqueAnnotation(final PromiseDrop<? extends IAASTRootNode> a) {
    uniquenessTags.add(a);
  }

  
  private static final class UniquenessConsistencyChecker extends AbstractPromiseScrubber<PromiseDrop<? extends IAASTRootNode>> {
    public UniquenessConsistencyChecker() {
      super(ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, NONE,
          CONSISTENCY, ScrubberOrder.NORMAL, new String[] { UNIQUE });
    }

    @Override
    protected void processDrop(PromiseDrop<? extends IAASTRootNode> a) {
      checkConsistency(a.getPromisedFor(), getState(a));
    }
    
    @Override
    protected boolean processUnannotatedMethodRelatedDecl(
        final IRNode unannotatedNode) {
      final Operator op = JJNode.tree.getOperator(unannotatedNode);
      if (ParameterDeclaration.prototype.includes(op)) {
        System.out.println("XXXXX op = " + op + "; method = " +
            JavaNames.genMethodConstructorName(
                JJNode.tree.getParentOrNull(JJNode.tree.getParent(unannotatedNode))));
        
      } else {
        System.out.println("XXXXX op = " + op + "; method = " +
            JavaNames.genMethodConstructorName(
                JavaPromise.getPromisedFor(unannotatedNode)));
      }
      return checkConsistency(unannotatedNode, State.SHARED);
    }
    
    @Override
    protected Iterable<PromiseDrop<? extends IAASTRootNode>> getRelevantAnnotations() {
      /*
      for(PromiseDrop d : uniquenessTags) {
    	  System.out.println("Got tag: "+d);
      }
      */
      return uniquenessTags;
    }

    @Override
    protected void finishRun() {
      uniquenessTags.clear();
    }
    
    
    
    private State getState(final PromiseDrop<? extends IAASTRootNode> a) {
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

    private State getState(final IRNode n) {
      final UniquePromiseDrop uDrop = getUnique(n);
      if (uDrop != null) {
        return uDrop.allowRead() ? State.UNIQUEWRITE : State.UNIQUE;
      } else if (LockRules.getImmutableRef(n) != null) {
        return State.IMMUTABLE;
      } else if (getReadOnly(n) != null) {
        return State.READONLY; 
      } else if (getBorrowed(n) != null) {
        return State.BORROWED;
      } else {
        return State.SHARED;
      }
    }
    
    
    
    private boolean checkConsistency(final IRNode promisedFor, final State s) {
      /* 3 cases, return value, parameter, receiver.
       */
      boolean good = true;
      
      final Operator op = JJNode.tree.getOperator(promisedFor);
      if (ParameterDeclaration.prototype.includes(op)) {
        final IRNode mdecl = JJNode.tree.getParent(JJNode.tree.getParent(promisedFor));
        for (final IBinding bc : getContext().getBinder().findOverriddenParentMethods(mdecl)) {
          final IRNode parentMethod = bc.getNode();
          
          // find the same parameter in the original
          final IRNode params = MethodDeclaration.getParams(mdecl);
          final IRNode parentParams = MethodDeclaration.getParams(parentMethod);
          final Iteratable<IRNode> paramsIter = Parameters.getFormalIterator(params);
          final Iteratable<IRNode> parentParamsIter = Parameters.getFormalIterator(parentParams);
          while (paramsIter.hasNext()) {
            final IRNode p = paramsIter.next();
            final IRNode parentP = parentParamsIter.next();
            if (p == promisedFor) { // found the original param
              final State parentState = getState(parentP);
              if (!State.lattice.lessEq(parentState, s)) {
                good = false;
                getContext().reportError(promisedFor,
                    "The annotation on parameter {0} of {1} cannot be changed from {2} to {3}",
                    ParameterDeclaration.getId(p),
                    JavaNames.genQualifiedMethodConstructorName(parentMethod),
                    parentState.getAnnotation(), s.getAnnotation());
              }
            }
          }
        }
      } else if (ReceiverDeclaration.prototype.includes(op)) {
        final IRNode mdecl = JavaPromise.getPromisedFor(promisedFor);
        for (final IBinding bc : getContext().getBinder().findOverriddenParentMethods(mdecl)) {
          final IRNode parentMethod = bc.getNode();
          
          final String c = DebugUnparser.toString(mdecl);
          final String p = DebugUnparser.toString(parentMethod);
          
          // Get the receiver in the original
          final IRNode rcvr = JavaPromise.getReceiverNode(parentMethod);
          final State parentState = getState(rcvr);
          
//          if (s == State.SHARED) {
//            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@");
//            System.out.println("method = " + c);
//            System.out.println("state = " + s);
//            System.out.println("parent = " + p);
//            System.out.println("parent state = " + parentState);
//          }
          
          if (!State.lattice.lessEq(parentState, s)) {
            good = false;
            getContext().reportError(promisedFor,
                "The annotation on the receiver of {0} cannot be changed from {1} to {2}",
                JavaNames.genQualifiedMethodConstructorName(parentMethod),
                parentState.getAnnotation(), s.getAnnotation());
          }
        }
      } else if (ReturnValueDeclaration.prototype.includes(op)) {
        final IRNode mdecl = JavaPromise.getPromisedFor(promisedFor);
        for (final IBinding bc : getContext().getBinder().findOverriddenParentMethods(mdecl)) {
          final IRNode parentMethod = bc.getNode();

          // Get the return value in the original
          final IRNode rcvr = JavaPromise.getReturnNode(parentMethod);
          final State parentState = getState(rcvr);          
          if (!State.lattice.lessEq(s, parentState)) {
            good = false;
            getContext().reportError(promisedFor,
                "The annotation on the return value of {0} cannot be changed from {1} to {2}",
                JavaNames.genQualifiedMethodConstructorName(parentMethod),
                parentState.getAnnotation(), s.getAnnotation());
          }
        }
      }
      
      return good;
    }
  }
}
