/*
 * Created on Oct 30, 2003
 *  
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import com.surelogic.annotation.*;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.parse.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 * 
 */
@Deprecated
public class UniquenessAnnotation extends AbstractPromiseAnnotation {
  static final String BORROWED_TAG = "Borrowed";
  
  static SlotInfo<Boolean> uniqueSI, borrowedSI, immutableSI;
  
  private static DefaultDropFactory<UniquePromiseDrop> uniqueSIFactory = 
    new DefaultDropFactory<UniquePromiseDrop>("Unique") {
    @Override
    public UniquePromiseDrop newDrop(IRNode node, Object val) {
      if (isUnique(node)) {
        UniquePromiseDrop drop = new UniquePromiseDrop(null);
        drop.setCategory(JavaGlobals.UNIQUENESS_CAT);

        /*
        if (VariableDeclarator.prototype.includes(node)) {
          drop.setMessage(Messages.UniquenessAnnotation_uniqueDrop1, JavaNames
              .getFieldDecl(node)); //$NON-NLS-1$
        } else {
          drop.setMessage(Messages.UniquenessAnnotation_uniqueDrop2, JavaNames
              .getFieldDecl(node), JavaNames.genMethodConstructorName(VisitUtil
              .getEnclosingClassBodyDecl(node))); //$NON-NLS-1$
        }
        */
        if (ReturnValueDeclaration.prototype.includes(JJNode.tree
            .getOperator(node))) {
          drop.setUniqueReturn(true);
        }
        // System.err.println("Op type " +
        // JJNode.tree.getOperator(node).getClass().getName());
        return drop;
      }
      return null;
    }
  };

  private static DefaultDropFactory<BorrowedPromiseDrop> borrowedSIFactory = 
    new DefaultDropFactory<BorrowedPromiseDrop>("Borrowed") {
    @Override
    public BorrowedPromiseDrop newDrop(IRNode node, Object val) {
      if (isBorrowed(node)) {
        BorrowedPromiseDrop drop = new BorrowedPromiseDrop(null);
        drop.setCategory(JavaGlobals.UNIQUENESS_CAT);
        drop.setMessage(Messages.UniquenessAnnotation_borrowedDrop, JavaNames
            .getFieldDecl(node), JavaNames.genMethodConstructorName(VisitUtil
            .getEnclosingClassBodyDecl(node))); //$NON-NLS-1$
        return drop;
      }
      return null;
    }
  };

  private UniquenessAnnotation() {
    AnnotationRules.initialize();
  }

  private static final UniquenessAnnotation instance = new UniquenessAnnotation();

  public static final UniquenessAnnotation getInstance() {
    return instance;
  }

  public static SlotInfo<Boolean> getIsUniqueSlotInfo() {
    return uniqueSI;
  }

  public static boolean isUnique(IRNode node) {
    boolean rv = isX_filtered(uniqueSI, node);
    if (rv) {
      isX_filtered(uniqueSI, node);
    }
    return rv;
  }

  public static UniquePromiseDrop getUniqueDrop(IRNode node) {
    return getDrop(uniqueSIFactory, node);
  }

  public static void setIsUnique(IRNode node, boolean unique) {
    setX_mapped(uniqueSI, node, unique);
  }

  public static SlotInfo<Boolean> getIsBorrowedSlotInfo() {
    return borrowedSI;
  }

  public static boolean isBorrowed(IRNode node) {
    return isX_filtered(borrowedSI, node);
  }

  public static BorrowedPromiseDrop getBorrowedDrop(IRNode node) {
    return getDrop(borrowedSIFactory, node);
  }

  public static void setIsBorrowed(IRNode node, boolean borrowed) {
    setX_mapped(borrowedSI, node, borrowed);
    getBorrowedDrop(node);
  }

  public static boolean isImmutable(IRNode node) {
    return isX_filtered(immutableSI, node);
  }

  public static void setIsImmutable(IRNode node, boolean immutable) {
    setX_mapped(immutableSI, node, immutable);
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    final IAnnotationParseRule uniqueRule = new UniquenessRules.Unique_ParseRule();
    final IPromiseParseRule unsharedRule  = new BooleanFieldRule("Unshared") {
      @Override
      protected SlotInfo<Boolean> getSI() {
        return uniqueSI;
      }
      @Override
      public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
        if (inStrictMode) {          
          cb.noteProblem("@Unshared is deprecated; use @Unique instead");
        }
        return super.parse(n, contents, cb);
      }
      @Override
      protected void parsedSuccessfully(IRNode decl) {
        getUniqueDrop(decl);
      }
    };

    return new IPromiseRule[] {
        new Uniqueness_ParseRule(BORROWED_TAG) {

          public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
            borrowedSI = si;
            return new TokenInfo<Boolean>(BORROWED_TAG, si, name);
          }

          @Override
          protected void getDrop(IRNode n) {
            getBorrowedDrop(n);
          }
        },
        // Handles parsing, storage, and checking for method-related decls
        new Uniqueness_ParseRule("Unique", fieldMethodDeclOps) {
          @Override
          public boolean parse(IRNode n, String contents,
              IPromiseParsedCallback cb) {
            if (FieldDeclaration.prototype.includes(n)) {
              return unsharedRule.parse(n, contents, cb);
            } else {
              uniqueRule.parse(IAnnotationParsingContext.nullPrototype, contents);
              return super.parse(n, contents, cb);
            }
          }

          public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
            uniqueSI = si;
            return new TokenInfo<Boolean>("Unique", si, name);
          }

          @Override
          protected void getDrop(IRNode n) {
            getUniqueDrop(n);
          }
        },
        unsharedRule,
        // Handles checking for field-related decls
        new AbstractPromiseCheckRule("Unique", varDeclaratorOps) {
          @Override
          public boolean checkSanity(Operator op, IRNode promisedFor,
              IPromiseCheckReport report) {
            // invoke lazy creation of drops (if they don't exist
            // already...these
            // calls will create them)
            getUniqueDrop(promisedFor);
            return true;
          }
        },
        new AbstractPromiseStorageAndCheckRule<Boolean>("Immutable",
            IPromiseStorage.BOOL, varDeclaratorOps, varDeclaratorOps) {

          public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
            immutableSI = si;
            return new TokenInfo<Boolean>("Immutable", si, name);
          }
        }, };
  }

  abstract class Uniqueness_ParseRule extends AbstractPromiseParserCheckRule<Boolean> {
    protected Uniqueness_ParseRule(String tag) {
      this(tag, methodDeclOps);
    }

    /**
     * For parsing something different
     * 
     * @param tag
     */
    protected Uniqueness_ParseRule(String tag, Operator[] parseOps) {
      super(tag, IPromiseStorage.BOOL, false, parseOps, methodDeclOps,
          varDeclOps);
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      boolean rv = true;

      final Iterator<IRNode> e = StatementExpressionList
          .getExprIterator(result);
      while (e.hasNext()) {
        final IRNode expr = e.next();
        final Operator eop = tree.getOperator(expr);

        IRNode nodeToSet = null;
        if (eop instanceof ThisExpression) {
          nodeToSet = JavaPromise.getReceiverNodeOrNull(n);
          if (nodeToSet == null) {
            cb.noteProblem("Couldn't find a receiver node for "
                + DebugUnparser.toString(n));
            rv = false;
            continue;
          }
        } else if (eop instanceof VariableUseExpression) {
          nodeToSet = BindUtil.findLV(n, VariableUseExpression.getId(expr));

          if (nodeToSet == null) {
            cb.noteProblem("Couldn't find '"
                + VariableUseExpression.getId(expr) + "' as parameter in "
                + DebugUnparser.toString(n));
            rv = false;
            continue;
          }
        } else {
          cb.noteProblem("Unexpected expression for @" + name + ": "
              + DebugUnparser.toString(expr));
          rv = false;
          continue;
        }

        if (name.equals(BORROWED_TAG)) {
          setIsBorrowed(nodeToSet, true);
        } else {
          setIsUnique(nodeToSet, true);
        }
      }
      return rv;
    }

    /**
     * Called on the method decl
     */
    @Override
    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      // invoke lazy creation of drops (if they don't exist already...these
      // calls will create them)
      IRNode receiver = JavaPromise.getReceiverNodeOrNull(promisedFor);
      if (receiver != null) {
        getDrop(receiver);
      }

      IRNode params;
      if (MethodDeclaration.prototype.includes(op)) {
        params = MethodDeclaration.getParams(promisedFor);
      } else {
        params = ConstructorDeclaration.getParams(promisedFor);
      }
      for (IRNode param : Parameters.getFormalIterator(params)) {
        getDrop(param);
      }
      return true;
    }

    protected abstract void getDrop(IRNode n);
  }
}