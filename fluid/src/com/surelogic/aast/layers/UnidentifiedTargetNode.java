/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.SingletonIterator;

/**
 * Could be a layer, typeset, package, or type
 * 
 * @author Edwin
 */
public class UnidentifiedTargetNode extends AbstractLayerMatchTarget implements IHasLayerBinding {
	private final String qname;
	
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("UnidentifiedTarget") {
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
	
	public String getQualifiedName() {
		if (parent instanceof UnionTargetNode) {
			UnionTargetNode u = (UnionTargetNode) parent;
			return u.getPrefix()+'.'+this.getName();
		} else {
			return this.getName();
		}
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
	public boolean matches(IRNode type) {
		final ILayerBinding b = resolveBinding();
		if (b == null) {
			System.out.println("Couldn't bind "+this.unparse(false));
			return false;
		}
		switch (b.getKind()) {
		case LAYER:
		case TYPESET:
			return b.getOther().isPartOf(type);
		case PACKAGE:
			final IRNode cu  = VisitUtil.getEnclosingCompilationUnit(type);
			final String pkg = VisitUtil.getPackageName(cu);
			for(IRNode p : b.getPackages()) {
				if (pkg.equals(NamedPackageDeclaration.getId(p))) {
					return true;
				}
			}		
			return false;
		case TYPE:
			return type == b.getType();
		}
		return false;
	}
	
	@Override
  public boolean bindingExists() {
		return AASTBinder.getInstance().isResolvable(this);
	}

	@Override
  public ILayerBinding resolveBinding() {
		return AASTBinder.getInstance().resolve(this);
	}

	@Override
	public Iterable<String> getNames() {
		return new SingletonIterator<String>(getQualifiedName());
	}
}
