package com.surelogic.annotation.rules;

import java.util.logging.Level;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
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
  public static final String CONFLICTS = "CheckForUniquenessConflicts";
  public static final String READONLY = "ReadOnly";
  
  private static final AnnotationRules instance = new UniquenessRules();
  
  private static final Readonly_ParseRule readonlyRule = new Readonly_ParseRule();
  private static final Unique_ParseRule uniqueRule     = new Unique_ParseRule();
  private static final Borrowed_ParseRule borrowedRule = new Borrowed_ParseRule();

  
  
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
			  	return storeDropIfNotNull(n, scrubReadOnly(context, n));
  			}    		
    	};
    }
    
    private ReadOnlyPromiseDrop scrubReadOnly(
        final IAnnotationScrubberContext context, final ReadOnlyNode n) {
      // must be a reference type variable
      boolean isGood = checkForReferenceType(context, n, "ReadOnly");
      
      /* Cannot also be borrowed, unless the annotation is on a field.
       * Imples we don't have to check for BorrowedInRegion, because that
       * can only appear on a field, and would thus be legal.
       */
      final IRNode promisedFor = n.getPromisedFor();
      if (isBorrowed(promisedFor) &&
          !FieldDeclaration.prototype.includes(
              JJNode.tree.getParentOrNull(JJNode.tree.getParentOrNull(promisedFor)))) {
        context.reportError(n, "Cannot be annotated with both @Borrowed and @ReadOnly");
        isGood = false;
      }
      
      if (isGood) {
        return new ReadOnlyPromiseDrop(n);
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
        
        @Override
        protected boolean processUnannotatedMethodRelatedDecl(
            final IRNode unannotatedNode) {
          /* Only care if the unannotated node is a return value declaration.
           * Parameters (and receivers) can remove uniqueness requirements
           * with out harm. 
           */
          final Operator op = JJNode.tree.getOperator(unannotatedNode);
          if (ReturnValueDeclaration.prototype.includes(op)) {
            final IRNode mdecl = JavaPromise.getPromisedFor(unannotatedNode);
            boolean good = true;
            for (final IBinding context : getContext().getBinder().findOverriddenParentMethods(mdecl)) {
              final IRNode parentMethod = context.getNode();
              final IRNode parentReturn = JavaPromise.getReturnNode(parentMethod);
              final UniquePromiseDrop parentUnique = getUnique(parentReturn);
              if (parentUnique != null) {
                // Parent has unique return, we should have one too
                good = false;
                getContext().reportError(mdecl,
                    "Cannot remove unique return value from annotations of {0}",
                    JavaNames.genQualifiedMethodConstructorName(parentMethod));
              }
            }
            return good;
          } else {
            return true;
          }
        }
      };
    }
    
    private UniquePromiseDrop scrubUnique(
        final IAnnotationScrubberContext context, final UniqueNode a) {
      // must be a reference type variable
      boolean good = checkForReferenceType(context, a, "Unique");
      
      // Unique fields must not be volatile or static final
      final IRNode promisedFor = a.getPromisedFor();
      final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
      if (VariableDeclarator.prototype.includes(promisedForOp)) {
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
      
      if (good) {
        /* Check consistency of annotated parameters and receiver declarations.
         * Cannot add @Unique to a previously unannotated parameter/receiver.
         */
        if (ReceiverDeclaration.prototype.includes(promisedForOp)) {
          good = consistencyCheckReceiver(context, promisedFor);
        } else if(ParameterDeclaration.prototype.includes(promisedForOp)) {
          good = consistencyCheckParameter(context, promisedFor);          
        }
      }
      
      if (good) {
        return new UniquePromiseDrop(a);
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
        
        @Override
        protected boolean processUnannotatedMethodRelatedDecl(
            final IRNode promisedFor) {
          /* Cannot remove @Borrowed annotations */
          final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
          if (ReceiverDeclaration.prototype.includes(promisedForOp)) {
            return consistencyCheckReceiver(context, promisedFor);
          } else if(ParameterDeclaration.prototype.includes(promisedForOp)) {
            return consistencyCheckParameter(context, promisedFor);          
          }
          
          return true;
        }
        
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
      
      final IRNode promisedFor = a.getPromisedFor();
      final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
      if (VariableDeclarator.prototype.includes(promisedForOp)) {
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
        return new BorrowedPromiseDrop(a);
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
}
