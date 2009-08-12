/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/UniquenessRules.java,v 1.31 2008/01/25 22:13:53 aarong Exp $*/
package com.surelogic.annotation.rules;

import java.util.HashSet;
import java.util.Set;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.SimpleScrubber;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class UniquenessRules extends AnnotationRules {
  public static final String UNIQUE = "Unique";
  public static final String NOTUNIQUE = "NotUnique";
  public static final String BORROWED = "Borrowed";
  public static final String CONFLICTS = "CheckForUniquenessConflicts";
  public static final String UNIQUENESS_DONE = "UniquenessDone";
  
  private static final AnnotationRules instance = new UniquenessRules();
  
  private static final Set<IRNode> uniqueNodes = new HashSet<IRNode>(); 
  
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
    return getUniqueDrop(vdecl) != null;
  }
  
  public static UniquePromiseDrop getUniqueDrop(IRNode vdecl) {
    return getBooleanDrop(uniqueRule.getStorage(), vdecl);
  }
  
  public static boolean isNotUnique(IRNode vdecl) {
    return getNotUniqueDrop(vdecl) != null;
  }
  
  public static NotUniquePromiseDrop getNotUniqueDrop(IRNode vdecl) {
    return getBooleanDrop(notUniqueRule.getStorage(), vdecl);
  }
  
  public static boolean isBorrowed(IRNode vdecl) {
    return getBorrowedDrop(vdecl) != null;
  }
  
  public static BorrowedPromiseDrop getBorrowedDrop(IRNode vdecl) {
    return getBooleanDrop(borrowedRule.getStorage(), vdecl);
  }
  
  public static boolean isImmutable(IRNode vdecl) {
    return getImmutableDrop(vdecl) != null;
  }
  
  // Suppress warning because the needed type doesn't yet exist
  @SuppressWarnings("unchecked")
  public static /*Immutable*/PromiseDrop getImmutableDrop(IRNode vdecl) {
    //return getBooleanDrop(immutableRule.getStorage(), vdecl);
    throw new UnsupportedOperationException("no immutable yet");
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
    registerParseRuleStorage(fw, uniqueRule);
    registerParseRuleStorage(fw, borrowedRule);
    registerParseRuleStorage(fw, notUniqueRule);
    registerScrubber(fw, conflictsRule);
    registerScrubber(fw, uniquenessDone);
  }
  
  
  
  private static interface DropGenerator<T extends IAASTRootNode, D extends PromiseDrop<T>> {
    public D generateDrop(T a);
  }

  
  
  public static class Unique_ParseRule 
  extends DefaultBooleanAnnotationParseRule<UniqueNode,UniquePromiseDrop> {
    public Unique_ParseRule() {
      super(UNIQUE, fieldMethodParamDeclOps, UniqueNode.class);
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
    @Override
    protected IAASTRootNode makeAAST(int offset) {
      return new UniqueNode(offset);
    }
    @Override
    protected IPromiseDropStorage<UniquePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), UniquePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<UniqueNode> makeScrubber() {
      return new AbstractAASTScrubber<UniqueNode>(this) {
        @Override
        protected PromiseDrop<UniqueNode> makePromiseDrop(UniqueNode a) {
          //System.out.println("Promised on "+DebugUnparser.toString(a.getPromisedFor()));
          final UniquePromiseDrop storedDrop =
            storeDropIfNotNull(getStorage(), a, scrubUnique(getContext(), a));
          if (storedDrop != null) {
            uniqueNodes.add(a.getPromisedFor());
          }
          return storedDrop;          
        }
      };
    }
    
    private UniquePromiseDrop scrubUnique(
        final IAnnotationScrubberContext context, final UniqueNode a) {
      // must be a reference type variable
      boolean good = checkForReferenceType(context, a, "Unique");
      
      // Unique fields must not be volatile
      final IRNode promisedFor = a.getPromisedFor();
      final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
      if (VariableDeclarator.prototype.includes(promisedForOp)) {
        if (TypeUtil.isVolatile(promisedFor)) {
          good = false;
          context.reportError("Volatile fields cannot be unique", a);
        }
      }

      if (good) {
        return new UniquePromiseDrop(a);
      } else {
        return null;
      }
    }
  }

  
  public static class Borrowed_ParseRule
  extends DefaultBooleanAnnotationParseRule<BorrowedNode,BorrowedPromiseDrop>
  implements DropGenerator<BorrowedNode, BorrowedPromiseDrop> {
    public Borrowed_ParseRule() {
      super(BORROWED, methodOrParamDeclOps, BorrowedNode.class);
    }
   
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      if (context.getSourceType() == AnnotationSource.JAVADOC) {
        return parser.borrowedList().getTree();
      }
      if (ParameterDeclaration.prototype.includes(context.getOp())) {
        return parser.nothing().getTree();
      }
      return parser.borrowedFunction().getTree();
    }
    @Override
    protected IAASTRootNode makeAAST(int offset) {
      return new BorrowedNode(offset);
    }
    @Override
    protected IPromiseDropStorage<BorrowedPromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), BorrowedPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<BorrowedNode> makeScrubber() {
      return new AbstractAASTScrubber<BorrowedNode>(this) {
        @Override
        protected PromiseDrop<BorrowedNode> makePromiseDrop(BorrowedNode a) {
          return storeDropIfNotNull(getStorage(), a, 
              checkForReferenceType(Borrowed_ParseRule.this, getContext(), a, "Borrowed"));          
        }
      };
    }    
    
    public BorrowedPromiseDrop generateDrop(BorrowedNode a) {
      return new BorrowedPromiseDrop(a); 
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
    protected IAASTRootNode makeAAST(int offset) {
      return new NotUniqueNode(offset);
    }
    @Override
    protected IPromiseDropStorage<NotUniquePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), NotUniquePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<NotUniqueNode> makeScrubber() {
      return new AbstractAASTScrubber<NotUniqueNode>(this) {
        @Override
        protected PromiseDrop<NotUniqueNode> makePromiseDrop(NotUniqueNode a) {
          return storeDropIfNotNull(getStorage(), a, 
              checkForReferenceType(NotUnique_ParseRule.this, getContext(), a, "NotUnique"));
        }
      };
    }   
    
    public NotUniquePromiseDrop generateDrop(final NotUniqueNode a) {
      return new NotUniquePromiseDrop(a);
    }
  }

  
  private static <T extends IAASTRootNode, D extends PromiseDrop<T>> boolean
  checkForReferenceType(
      final IAnnotationScrubberContext context, final T a, final String label) {
    final IRNode promisedFor = a.getPromisedFor();
    final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
    final IJavaType type;
    if (ParameterDeclaration.prototype.includes(promisedForOp)
        || ReceiverDeclaration.prototype.includes(promisedForOp)
        || VariableDeclarator.prototype.includes(promisedForOp)) {
      type = context.getBinder().getJavaType(promisedFor);
    } else if (ReturnValueDeclaration.prototype.includes(promisedForOp)) {
      final IRNode method = JavaPromise.getPromisedFor(promisedFor);
      type = context.getBinder().getJavaType(method);
    } else {
      return false;
    }
    
    if (type instanceof IJavaPrimitiveType) {
      context.reportError(a, "{0} may not be used on primitive types", label);
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
        final UniquePromiseDrop uniqueDrop = getUniqueDrop(uniqueNode);
        final UniqueNode uniqueAST = uniqueDrop.getAST();
        final BorrowedPromiseDrop borrowedDrop = getBorrowedDrop(uniqueNode);
        final NotUniquePromiseDrop notUniqueDrop = getNotUniqueDrop(uniqueNode);

        boolean isAlreadyBad = false;
        if (borrowedDrop != null) {
          getContext().reportError("Cannot be both unique and borrowed", uniqueAST);
          uniqueDrop.invalidate();
          borrowedDrop.invalidate();
          isAlreadyBad = true;
        }
        if (notUniqueDrop != null) {
          getContext().reportError("Cannot be both unique and not unique", uniqueAST);
          if (!isAlreadyBad) uniqueDrop.invalidate();
          notUniqueDrop.invalidate();
        }
      }
      // Reset set of unique nodes.
      uniqueNodes.clear();
    }
  }
}
