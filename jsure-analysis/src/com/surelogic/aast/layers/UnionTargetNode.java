/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A union of various layers, typesets, packages, or types
 * 
 * @author Edwin
 */
public class UnionTargetNode extends AbstractLayerMatchTarget {	
	private final String qname;
	private final List<UnidentifiedTargetNode> union;
	
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("UnionTarget") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			List<UnidentifiedTargetNode> union = new ArrayList<UnidentifiedTargetNode>();
			for(AASTNode n : _kids) {
				union.add((UnidentifiedTargetNode) n);
			}
			return new UnionTargetNode(_start, _id, union);
		}
	};
	
	UnionTargetNode(int offset, String name, List<UnidentifiedTargetNode> set) {
		super(offset);
		qname = name;
		if (set == null || set.isEmpty()) {
			throw new IllegalArgumentException("Bad set: "+set);
		}		
		union = set;
		for(UnidentifiedTargetNode ut : union) {
			ut.setParent(this);
		}
	}

	public String getPrefix() {
		return qname;
	}
	
	public Iterable<UnidentifiedTargetNode> getUnion() {
		return union;
	}
	
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		List<UnidentifiedTargetNode> clonedUnion = new ArrayList<UnidentifiedTargetNode>();
		for(UnidentifiedTargetNode ut : union) {
			clonedUnion.add((UnidentifiedTargetNode) ut.cloneOrModifyTree(mod));
		}
		return new UnionTargetNode(offset, qname, clonedUnion);
	}

	@Override
	public String unparse(boolean debug, int indent) {
	    StringBuilder sb = new StringBuilder();
		if (debug) {
		    indent(sb, indent);		    
		    sb.append("UnionTargetNode\n");
		    indent(sb, indent + 2);
		    sb.append("qname=").append(qname);		    
		    sb.append("\n");
		    for(UnidentifiedTargetNode ut : union) {
		    	indent(sb, indent + 2);
			    sb.append(ut.unparse(debug, indent+2));		    
			    sb.append("\n");
		    }
		} else {
			sb.append(qname);
			if (union.size() > 1) {
				boolean first = true;
				sb.append(".{ ");
				for(UnidentifiedTargetNode ut : union) {
					if (first) {
						first = false;
					} else {
						sb.append(", ");
					}
					sb.append(ut.unparse(debug, indent));
				}
				sb.append(" }");
			} else {
				sb.append('.').append(union.get(0).unparse(debug, indent));
			}
		}
	    return sb.toString();
	}
	
	@Override
	public Operator appliesTo() {
		return Declaration.prototype;
	}

	@Override
	public boolean matches(IRNode type) {
		for(UnidentifiedTargetNode n : union) {
			if (n.matches(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<String> getNames() {
		List<String> names = new ArrayList<String>();
		for(UnidentifiedTargetNode t : union) {
			for(String s : t.getNames()) {
				names.add(s);
			}
		}
		return names;
	}
}
