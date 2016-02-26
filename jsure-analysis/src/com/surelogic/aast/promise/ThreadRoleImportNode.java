package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ITRoleImportBinding;
import com.surelogic.aast.bind.IHasTRoleImportBinding;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ThreadRoleImportNode extends AASTRootNode implements
		IHasTRoleImportBinding, ITRoleImportBinding {

	// Fields
	private final String id;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"ThreadRoleImport") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			String id = _id;
			return new ThreadRoleImportNode(_start, id);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be @unique
	 */
	public ThreadRoleImportNode(int offset, String id) {
		super(offset);
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		this.id = id;
	}

	@Override
  public final String unparseForPromise() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
		}
		sb.append("ThreadRoleImport\n");
		indent(sb, indent + 2);
		sb.append("id=").append(getId());
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * @return A non-null String
	 */
	public String getId() {
		return id;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new ThreadRoleImportNode(getOffset(), getId());
	}

	@Override
  public boolean bindingExists() {
		return AASTBinder.getInstance().isResolvable(this);
	}

	@Override
  public ITRoleImportBinding resolveBinding() {
		if (AASTBinder.getInstance().resolve(this) == null) {
			return null;
		}
		return this;
	}

	@Override
  public IRNode getTRoleImport() {
		return AASTBinder.getInstance().resolve(this);
	}

}
