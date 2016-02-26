package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ISourceRefType;
import com.surelogic.ast.ResolvableToType;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class TypeDeclPatternNode extends ConcreteTargetNode implements
		ResolvableToType {
	// Fields
	private final int mods;
	private final String type;
	private final InPatternNode inPattern;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"TypeDeclPattern") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			int mods = _mods;
			InPatternNode inPattern = (InPatternNode)_kids.get(0);
			return new TypeDeclPatternNode(_start, mods, _id, inPattern);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @param inPattern An InPatternNode representing this type decl's 'in' statement - may be null if this is using the old style annotation
	 * @unique
	 */
	public TypeDeclPatternNode(int offset, int mods, String type, InPatternNode inPattern) {
		super(offset);
		this.mods = mods;
		if (type == null) {
			throw new IllegalArgumentException("type is null");
		}
		this.type = type;
		
		if(inPattern == null){
			throw new IllegalArgumentException("inPattern is null");
		}
		this.inPattern = inPattern;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("TypeDeclPattern\n");
			indent(sb, indent + 2);
			sb.append("mods=").append(getMods());
			sb.append("\n");
			indent(sb, indent + 2);
			sb.append("type=").append(getType());
			sb.append("\n");
      sb.append(getInPattern().unparse(debug, indent + 2));
		} else {
			sb.append(getType());
			sb.append(getInPattern().unparse(debug));
		}
		return sb.toString();
	}

	@Override
  public boolean typeExists() {
		//TODO add InPattern check
		if ("*".equals(getType())) {
			return true;
		}
		return true;//AASTBinder.getInstance().isResolvableToType(this);
	}

	/**
	 * Gets the binding corresponding to the type of the TypeDeclPattern
	 */
	public Iterable<ISourceRefType> resolveType() {
		return AASTBinder.getInstance().resolveType(this);
	}

	/**
	 * @return A non-null int
	 */
	public int getMods() {
		return mods;
	}

	/**
	 * @return A non-null String
	 */
	public String getType() {
		return type;
	}
	
	public InPatternNode getInPattern(){
		return inPattern;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Operator appliesTo() {
		return TypeDeclaration.prototype;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.PromiseTargetNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		if (TypeDeclaration.prototype.includes(irNode)) {
			// Match type & package
			return matchesType(irNode, false)
			// Match modifiers
					&& matchesModifiers(mods, JavaNode.getModifiers(irNode))
					//FIXME
					&& inPattern.matches(irNode);
		}
		return false;
	}

	private boolean matchesType(IRNode decl, boolean useEnclosing) {
		IRNode here = decl;

		if ("".equals(type)) {
			return true;
		}
		if (!"*".equals(type)) {
			IRNode enclosingT = useEnclosing ? VisitUtil.getEnclosingType(decl)
					: decl;
			if (enclosingT == null) {
				enclosingT = decl;
			}
			here = enclosingT;
			
			final String tName;
			if (type.indexOf('.') < 0) {
				// Not qualified
				tName = JJNode.getInfo(here);
			} else {
				tName = JavaNames.getFullName(here);
			}
			if (type.indexOf("*") < 0) {
				//no wildcards			
				if (!tName.equals(type)) {
					return false;
				}
			}
			else{
				final String typePattern = type.replaceAll("\\*", ".*");
				return tName.matches(typePattern);
			}
		}

		/*
		// otherwise: wildcard matches anything

		if ("".equals(pkg) || "*".equals(pkg)) {
			return true;
		}

		final IRNode compUnit = OpSearch.cuSearch.findEnclosing(here);
		final IRNode dPkg = CompilationUnit.getPkg(compUnit);
		if (NamedPackageDeclaration.prototype.includes(dPkg)) {
			final String dPkgName = NamedPackageDeclaration.getId(dPkg);
			return pkg.equals(dPkgName);
		}
		return false;
		*/
		return true;
	}
	
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new TypeDeclPatternNode(getOffset(), getMods(), getType(), (InPatternNode)getInPattern().cloneOrModifyTree(mod));
  }

  @Override
  public boolean isFullWildcard() {
	  return mods == JavaNode.ALL_FALSE && (type.length() == 0 || "*".equals(type)) && inPattern.isFullWildcard();
  }
}
