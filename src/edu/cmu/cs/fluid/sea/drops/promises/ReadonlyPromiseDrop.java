package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;

public final class ReadonlyPromiseDrop extends BooleanPromiseDrop<ReadonlyNode> 
implements MaybeTopLevel {  
  public ReadonlyPromiseDrop(ReadonlyNode n) {
    super(n);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
  }

  @Override
  protected void computeBasedOnAST() {
	    /*
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
    */
    setMessage(getAST().toString());
  }

  @Override
  public boolean requestTopLevel() {
	  return true;
  }
  
  @Override
  public void snapshotAttrs(AbstractSeaXmlCreator s) {
	  super.snapshotAttrs(s);
	  s.addAttribute(REQUEST_TOP_LEVEL, true);
  }
}