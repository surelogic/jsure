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
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.annotation.scrub.SimpleScrubber;
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
  public static final String NOTUNIQUE = "NotUnique";
  public static final String BORROWED = "Borrowed";
  public static final String CONFLICTS = "CheckForUniquenessConflicts";
  public static final String UNIQUENESS_DONE = "UniquenessDone";
  public static final String READONLY = "ReadOnly";
  
  private static final AnnotationRules instance = new UniquenessRules();
  
  private static final Set<IRNode> uniqueNodes = new HashSet<IRNode>(); 
  
  private static final Readonly_ParseRule readonlyRule = new Readonly_ParseRule();
  private static final Unique_ParseRule uniqueRule     = new Unique_ParseRule();
  private static final NotUnique_ParseRule notUniqueRule     = new NotUnique_ParseRule();
  private static final Borrowed_ParseRule borrowedRule = new Borrowed_ParseRule();
  private static final CheckForAnnotationConflicts conflictsRule = new CheckForAnnotationConflicts();
  private static final SimpleScrubber uniquenessDone = new SimpleScrubber(UNIQUENESS_DONE, CONFLICTS) {
    @Override
    protected void scrub() {
      // do nothing
    }
  };

  
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  public static boolean isUnique(IRNode vdecl) {
    return getUnique(vdecl) != null;
  }
  
  public static UniquePromiseDrop getUnique(IRNode vdecl) {
    return getBooleanDrop(uniqueRule.getStorage(), vdecl);
  }
  
  public static boolean isNotUnique(IRNode vdecl) {
    return getNotUnique(vdecl) != null;
  }
  
  public static NotUniquePromiseDrop getNotUnique(IRNode vdecl) {
    return getBooleanDrop(notUniqueRule.getStorage(), vdecl);
  }
  
  public static boolean isBorrowed(IRNode vdecl) {
    return getBorrowed(vdecl) != null;
  }
  
  public static BorrowedPromiseDrop getBorrowed(IRNode vdecl) {
    return getBooleanDrop(borrowedRule.getStorage(), vdecl);
  }
  
  public static boolean isReadOnly(IRNode vdecl) {
	  return getReadOnly(vdecl) != null;
  }
  
  public static ReadonlyPromiseDrop getReadOnly(IRNode vdecl) {
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
    registerParseRuleStorage(fw, notUniqueRule);
    registerScrubber(fw, conflictsRule);
    registerScrubber(fw, uniquenessDone);
  }
  
  /**
   * Constructors can be annoted with either "borrowed this" or 
   * "returns unique" to indicate that the receiver is borrowed.  This
   * method return true if either of those cases is met.
   * @param conDecl A constructor declaration node
   */
  public static boolean constructorYieldsUnaliasedObject(final IRNode conDecl) {
    /* Sanity checking already guarantees that at most one of the two case
     * is met.
     */
    // Unique return is the preferred way of doing things, so check it first
    final IRNode returnNode = JavaPromise.getReturnNodeOrNull(conDecl);
    if (isUnique(returnNode)) {
      return true;
    } else {
      final IRNode rcvrNode = JavaPromise.getReceiverNodeOrNull(conDecl);
      return isBorrowed(rcvrNode);
    }
  }
  
  private static interface DropGenerator<T extends IAASTRootNode, D extends PromiseDrop<T>> {
    public D generateDrop(T a);
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
			  return parser.uniqueJava5Constructor().getTree();
		  }
		  return parser.uniqueJavadocConstructor().getTree();
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
  
  public static class Readonly_ParseRule extends AbstractParseRule<ReadonlyNode, ReadonlyPromiseDrop> {
	public Readonly_ParseRule() {
		super(READONLY, fieldMethodParamDeclOps, ReadonlyNode.class);
	}
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new ReadonlyNode(offset);
    }
    @Override
    protected IPromiseDropStorage<ReadonlyPromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ReadonlyPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ReadonlyNode> makeScrubber() {
    	// TODO
    	return null;
    }
  }
  
  public static class Unique_ParseRule 
  extends AbstractParseRule<UniqueNode,UniquePromiseDrop> {
    public Unique_ParseRule() {
      super(UNIQUE, fieldMethodParamDeclOps, UniqueNode.class);
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
    protected IAnnotationScrubber<UniqueNode> makeScrubber() {
      return new AbstractAASTScrubber<UniqueNode, UniquePromiseDrop>(
          this, ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
        @Override
        protected PromiseDrop<UniqueNode> makePromiseDrop(UniqueNode a) {
          final UniquePromiseDrop storedDrop =
            storeDropIfNotNull(a, scrubUnique(getContext(), a));
          if (storedDrop != null) {
            uniqueNodes.add(a.getPromisedFor());
          }
          return storedDrop;          
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
    protected IAnnotationScrubber<BorrowedNode> makeScrubber() {
      return new AbstractAASTScrubber<BorrowedNode, BorrowedPromiseDrop>(this,
          ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, UNIQUE) {
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
      
      /* If the annotation is @Borrowed("this"), and it appears on a constructor,
       * then we also make sure that the constructor is not annotated with
       * @Unique("return").
       */
      final IRNode promisedFor = a.getPromisedFor();
      final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
      if (ReceiverDeclaration.prototype.includes(promisedForOp)) {
        // Get the method/constructor declaration that the receiver belongs to
        final IRNode decl = JavaPromise.getPromisedForOrNull(promisedFor);
        if (ConstructorDeclaration.prototype.includes(decl)) {
          // It's from a constructor, look for unique on the return node
          final IRNode returnNode = JavaPromise.getReturnNodeOrNull(decl);
          if (returnNode != null) {
            if (isUnique(returnNode)) {
              good = false;
              context.reportError("Cannot use both @Borrowed(\"this\") and @Unique(\"return\") on a constructor declaration", a);
            }
          }
        }
      }

      if (good) {
        return new BorrowedPromiseDrop(a);
      } else {
        return null;
      }
    }
  }
  
  public static class NotUnique_ParseRule extends
      DefaultBooleanAnnotationParseRule<NotUniqueNode, NotUniquePromiseDrop>
      implements DropGenerator<NotUniqueNode, NotUniquePromiseDrop> {  	
		public NotUnique_ParseRule() {
      super(NOTUNIQUE, methodOrParamDeclOps, NotUniqueNode.class);
    }
		
    @Override
    protected Object parse(IAnnotationParsingContext context,
        SLAnnotationsParser parser) throws Exception, RecognitionException {
      if (context.getSourceType() == AnnotationSource.JAVADOC) {
        return parser.borrowedList().getTree();
      }
      else if (SomeFunctionDeclaration.prototype.includes(context.getOp())) {
        return parser.thisExpr().getTree();
      }
      // on a parameter
      return parser.nothing().getTree();
    } 
		
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new NotUniqueNode(offset);
    }
    @Override
    protected IPromiseDropStorage<NotUniquePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), NotUniquePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<NotUniqueNode> makeScrubber() {
      return new AbstractAASTScrubber<NotUniqueNode, NotUniquePromiseDrop>(this) {
        @Override
        protected PromiseDrop<NotUniqueNode> makePromiseDrop(NotUniqueNode a) {
          return storeDropIfNotNull(a, 
              checkForReferenceType(
                  NotUnique_ParseRule.this, getContext(), a, "NotUnique"));
        }
      };
    }   
    
    public NotUniquePromiseDrop generateDrop(final NotUniqueNode a) {
      return new NotUniquePromiseDrop(a);
    }
  }

  
  private static <T extends IAASTRootNode> boolean checkForReferenceType(
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
  
  private static <T extends IAASTRootNode, D extends PromiseDrop<T>> D
  checkForReferenceType(
      final DropGenerator<T, D> dropGen, final IAnnotationScrubberContext context,
      final T a, final String label) {
    if (checkForReferenceType(context, a, label)) {
      return dropGen.generateDrop(a);
    } else {
      return null;
    }
  }
  

  private static final class CheckForAnnotationConflicts extends SimpleScrubber {
    public CheckForAnnotationConflicts() {
      super(CONFLICTS, UNIQUE, NOTUNIQUE, BORROWED);
    }
    
    @Override
    protected void scrub() {
      for (final IRNode uniqueNode : uniqueNodes) {
        // This seems wasteful and redundant
        final UniquePromiseDrop uniqueDrop = getUnique(uniqueNode);
        final UniqueNode uniqueAST = uniqueDrop.getAST();
        final BorrowedPromiseDrop borrowedDrop = getBorrowed(uniqueNode);
        final NotUniquePromiseDrop notUniqueDrop = getNotUnique(uniqueNode);

        boolean alsoBorrowed = borrowedDrop != null;
        boolean alsoNotUnique = notUniqueDrop != null;
        if (alsoBorrowed) {        	
          getContext().reportError("Cannot be both unique and borrowed", uniqueAST);
          borrowedDrop.invalidate();
        }
        if (alsoNotUnique) {
          getContext().reportError("Cannot be both unique and not unique", uniqueAST);
          notUniqueDrop.invalidate();
        }
        if (alsoBorrowed || alsoNotUnique) {
          uniqueDrop.invalidate();
        }
      }
      // Reset set of unique nodes.
      uniqueNodes.clear();
    }
  }
}
