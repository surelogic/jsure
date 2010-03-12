/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.IHasLayerBinding;
import com.surelogic.aast.bind.ILayerBinding;
import com.surelogic.aast.bind.IVariableBinding;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Could be a layer, typeset, package, or type
 * 
 * @author Edwin
 */
public class UnidentifiedTargetNode extends AbstractLayerMatchTarget implements IHasLayerBinding {
	private final String qname;
	
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("UnidentifiedTarget") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			return new UnidentifiedTargetNode(_start, _id);
		}
	};
	
	UnidentifiedTargetNode(int offset, String name) {
		super(offset);
		qname = name;
	}

	public String getName() {
		return qname;
	}
	
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public IAASTNode cloneTree() {
		return new UnidentifiedTargetNode(offset, qname);
	}

	@Override
	public String unparse(boolean debug, int indent) {
		if (debug) {
		    StringBuilder sb = new StringBuilder();
		    indent(sb, indent);		    
		    sb.append("UnidentifiedTarget\n");
		    indent(sb, indent + 2);
		    sb.append("qname=").append(qname);
		    sb.append("\n");
		    return sb.toString();
		} 
		return qname;
	}
	
	@Override
	public Operator appliesTo() {
		return Declaration.prototype;
	}

	@Override
	public boolean matches(IRNode irNode) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean bindingExists() {
		return AASTBinder.getInstance().isResolvable(this);
	}

	public ILayerBinding resolveBinding() {
		return AASTBinder.getInstance().resolve(this);
	}
}
