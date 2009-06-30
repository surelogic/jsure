/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/drops/promises/NotUniquePromiseDrop.java,v 1.1 2007/10/17 19:02:38 ethan Exp $*/
package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.NotUniqueNode;
import com.surelogic.sea.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * TODO Fill in purpose.
 * @author ethan
 */
public class NotUniquePromiseDrop extends BooleanPromiseDrop<NotUniqueNode> {

	/**
	 * @param a
	 */
	public NotUniquePromiseDrop(NotUniqueNode a) {
		super(a);
		setCategory(JavaGlobals.UNIQUENESS_CAT);
	}
  
	@Override
	protected void computeBasedOnAST(){
		String name = JavaNames.genMethodConstructorName(getNode());
		setMessage(Messages.UniquenessAnnotation_notUniqueDrop, name);
	}
}
