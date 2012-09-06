package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;
import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;

/**
 * Promise drop for "unique" promises established by the
 * uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 */
public final class UniquePromiseDrop extends BooleanPromiseDrop<UniqueNode> 
implements MaybeTopLevel, RegionAggregationDrop, IUniquePromise {
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
    	setResultMessage(Messages.UniquenessAnnotation_uniqueDrop1, 
             JavaNames.getFieldDecl(node)); //$NON-NLS-1$
    } else {
      IRNode method = VisitUtil.getEnclosingClassBodyDecl(node);
      if (method == null) {
        // Assume that it is a method
        method = node;
      }
      setResultMessage(Messages.UniquenessAnnotation_uniqueDrop2, 
             JavaNames.getFieldDecl(node), 
             JavaNames.genMethodConstructorName(method)); //$NON-NLS-1$
    }
  }

  @Override
  public boolean requestTopLevel() {
	  return true;
  }
  
  public final boolean allowRead() {
      return getAAST().allowRead();
  }
  
  public UniquePromiseDrop getDrop() {
    return this;
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
	  super.snapshotAttrs(s);
	  s.addAttribute(REQUEST_TOP_LEVEL, true);
  }
  
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    /* Aggregates Instance into the field if the field is non-final.
     * Aggregates Instance into Instance if the field is final and non-static.
     */
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    if (TypeUtil.isFinal(fieldDecl)) {
      return Collections.<IRegion, IRegion>singletonMap(
          instanceRegion, instanceRegion);
    } else {
      return Collections.<IRegion, IRegion>singletonMap(
          instanceRegion, RegionModel.getInstance(fieldDecl));
    }
  }
}