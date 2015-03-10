package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.util.OpSearch;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

@Deprecated
public class TypeQualifierPatternNode extends AASTNode {
	// Fields
	private final String pkg;
	private final String type;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"TypeQualifierPattern") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			String pkg = "*";
			String type = _id; // FIX 
			return new TypeQualifierPatternNode(_start, pkg, type);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public TypeQualifierPatternNode(int offset, String pkg, String type) {
		super(offset);
		if (pkg == null) {
			throw new IllegalArgumentException("pkg is null");
		}
		this.pkg = pkg;
		if (type == null) {
			throw new IllegalArgumentException("type is null");
		}
		this.type = type;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("TypeQualifierPattern\n");
			indent(sb, indent + 2);
			sb.append("pkg=").append(getPkg());
			sb.append("\n");
			indent(sb, indent + 2);
			sb.append("type=").append(getType());
			sb.append("\n");
		} else {
			boolean noPkg = getPkg().equals("");
			boolean noType = getType().equals("");
			if (noPkg && noType) {
				return "";
			}
			sb.append(noPkg ? "*" : getPkg());
			sb.append('.');
			sb.append(noType ? "*" : getType());
			sb.append('.');
		}
		return sb.toString();
	}

	/**
	 * @return A non-null String
	 */
	public String getPkg() {
		return pkg;
	}

	/**
	 * @return A non-null String
	 */
	public String getType() {
		return type;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	/**
	 * @param decl
	 *          An IRNode representing a type declaration
	 * @return true if the type IRNode matches the type represented by this node
	 */
	public boolean matches(final IRNode decl, final boolean useEnclosing) {
		IRNode here = decl;
		if ("".equals(type)) {
			return true;
		}
		if (!"*".equals(type)) {
			final IRNode enclosingT = useEnclosing ? VisitUtil.getEnclosingType(decl)
					: decl;
			final String tName = JJNode.getInfo(enclosingT);
    	System.out.println("tname: " + tName);
			if (!tName.matches(type)) {
				return false;
			}
			here = enclosingT;
		}
		// otherwise: wildcard matches anything

		if ("".equals(pkg) || "*".equals(pkg)) {
			return true;
		}
		final IRNode compUnit = OpSearch.cuSearch.findEnclosing(here);
		final IRNode packg = CompilationUnit.getPkg(compUnit);
		if (NamedPackageDeclaration.prototype.includes(JJNode.tree.getOperator(packg))) {
			final String pkgName = NamedPackageDeclaration.getId(packg);
			//final String pattern2 = pkg.replaceAll("\\*", ".*");

			return pkgName.matches(pkg);
		}
		return false;
	}
	
  @Override
  public IAASTNode cloneTree(){
  	return new TypeQualifierPatternNode(getOffset(), new String(getPkg()), new String(getType()));
  }
}
