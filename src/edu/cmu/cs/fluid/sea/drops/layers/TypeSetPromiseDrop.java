/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.layers;

import com.surelogic.aast.layers.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class TypeSetPromiseDrop extends PromiseDrop<TypeSetNode> implements IReferenceCheckDrop {
	public TypeSetPromiseDrop(TypeSetNode a) {
		super(a);
	}
	@Override
	protected void computeBasedOnAST() {
		setMessage(getAST().toString());
	}
	public String getId() {
		return getAST().getId();
	}
	public boolean check(IRNode type) {
		return getAST().check(type);
	}
	public boolean isPartOf(IRNode type) {
		return getAST().check(type);
	}
	public int getResultMessageKind() {
		throw new UnsupportedOperationException();
	}
	public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		throw new UnsupportedOperationException();
	}
}
