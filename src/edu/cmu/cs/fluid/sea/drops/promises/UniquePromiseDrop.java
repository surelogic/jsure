package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.UniqueNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;

/**
 * Promise drop for "unique" promises established by the
 * uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 * @see edu.cmu.cs.fluid.java.bind.UniquenessAnnotation
 */
public final class UniquePromiseDrop extends BooleanPromiseDrop<UniqueNode> 
implements MaybeTopLevel {
  //This page intentionally left blank
  
  private boolean isUniqueReturn;
  
  public UniquePromiseDrop(UniqueNode n) {
    super(n);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
    isUniqueReturn = false;
  }
  
  @Override
  public boolean isCheckedByAnalysis() {
    if (isUniqueReturn) {
      return super.isCheckedByAnalysis();
    } else {
      return true;
    }
  }
  
  /**
   * @return Returns the isUniqueReturn.
   */
  public boolean isUniqueReturn() {
    return isUniqueReturn;
  }
  
  /**
   * @param isUniqueReturn The isUniqueReturn to set.
   */
  public void setUniqueReturn(boolean isUniqueReturn) {
    this.isUniqueReturn = isUniqueReturn;
  }

  @Override
  protected void computeBasedOnAST() {
    final IRNode node = getNode();
    if (VariableDeclarator.prototype.includes(node)) {
      setMsg(Messages.UniquenessAnnotation_uniqueDrop1, 
             JavaNames.getFieldDecl(node)); //$NON-NLS-1$
    } else {
      IRNode method = VisitUtil.getEnclosingClassBodyDecl(node);
      if (method == null) {
        // Assume that it is a method
        method = node;
      }
      setMsg(Messages.UniquenessAnnotation_uniqueDrop2, 
             JavaNames.getFieldDecl(node), 
             JavaNames.genMethodConstructorName(method)); //$NON-NLS-1$
    }
  }

  public boolean requestTopLevel() {
	  return true;
  }
}