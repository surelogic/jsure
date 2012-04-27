package com.surelogic.xml;

import java.util.*;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.CommonImages;
import com.surelogic.common.xml.Entity;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;

public abstract class AbstractFunctionElement extends AnnotatedJavaElement 
implements IClassMember, TestXMLParserConstants
{	
	private boolean isStatic;
	private final String genericParams;
	private final String parameters;
	private final List<FunctionParameterElement> params = new ArrayList<FunctionParameterElement>();
	
	AbstractFunctionElement(boolean confirmed, String id, Access access, boolean isStatic, String params) {
		super(confirmed, id, access);
		this.isStatic = isStatic;
		parameters = normalize(params);
		genericParams = null;
	}
	
	AbstractFunctionElement(String id, Entity e) {
		super(false, id, Access.PUBLIC);
		
		isStatic = false;
		final String params = e.getAttribute(PARAMS_ATTRB);
		parameters = normalize(params);
		genericParams = e.getAttribute(GENERIC_PARAMS_ATTRB);
	}
	
	public final boolean isStatic() {
		return isStatic;
	}
	
	public final String getImageKey() {
		if (isStatic()) {
			switch (getAccessibility()) {
			case PROTECTED:
				return CommonImages.IMG_PROTECTED_S;
			case DEFAULT:
				return CommonImages.IMG_DEFAULT_S;
			case PUBLIC:
			default:
				return CommonImages.IMG_PUBLIC_S;
			}
		}
		switch (getAccessibility()) {
		case PROTECTED:
			return CommonImages.IMG_PROTECTED_I;
		case DEFAULT:
			return CommonImages.IMG_DEFAULT_I;
		case PUBLIC:
		default:
			return CommonImages.IMG_PUBLIC_I;
		}
	}
	
	public static String normalize(String orig) {
		if (orig == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		String[] splits = orig.split(",");
		for(String s : splits) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(s.trim());			
		}
		return sb.toString();
	}

	public final String getParams() {
		return parameters;
	}

	public final String[] getSplitParams() {
		return parameters.split(",");
	}
	
	public final String getGenericParams() {
		return genericParams;
	}
	
	public void setParameter(FunctionParameterElement p) {
		if (p == null) {
			return;
		}
		// Make params big enough
		while (params.size() <= p.getIndex()) {
			params.add(null);			
		}
		params.set(p.getIndex(), p);
		p.setParent(this);
	}
	
	public FunctionParameterElement getParameter(int i) {
		if (i >= params.size()) {
			return null;
		}
		return params.get(i);
	}

	public void removeParameter(int i) {
		if (i >= params.size()) {
			return; // ignore
		}
		params.set(i, null);
	}
	
	/**
	 * Can return null elements
	 */
	public Iterable<FunctionParameterElement> getParameters() {
		return params;
	}
	
	@Override
	public boolean hasChildren() {
		return super.hasChildren() || !params.isEmpty();
	}
	
	@Override
	protected void collectOtherChildren(List<Object> children) {
		super.collectOtherChildren(children);
		for(FunctionParameterElement p : params) {
			if (p != null) {
				children.add(p);
			}
		}
	}
	
	@Override
	public boolean isDirty() {
		if (super.isDirty()) {
			return true;
		}
		for(FunctionParameterElement p : params) {
			if (p != null && p.isDirty()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isModified() {
		if (super.isModified()) {
			return true;
		}
		for(FunctionParameterElement p : params) {
			if (p != null && p.isModified()) {
				return true;
			}
		}
		return false;
	}
	
	public void markAsClean() {
		super.markAsClean();
		for(FunctionParameterElement p : params) {
			if (p != null) {
				p.markAsClean();
			}
		}
	}
	
	boolean merge(AbstractFunctionElement changed, MergeType type) {
		boolean modified = false;
		for(FunctionParameterElement p2 : changed.params) {
			if (p2 == null) {
				continue;
			}
			FunctionParameterElement p0 = getParameter(p2.getIndex());
			if (p0 != null) {
				modified |= p0.merge(p2, type);
			} else {
				setParameter(p2.cloneMe(this));
				modified = true;
			}
		}
		if (changed.isStatic() && !isStatic()) {			
			isStatic = true;
			modified = true;
		}
		
		modified |= mergeThis(changed, type);
		return modified;
	}
	
	void copyToClone(AbstractFunctionElement clone) {
		super.copyToClone(clone);
		for(FunctionParameterElement p : params) {
			if (p != null) {
				clone.setParameter(p.cloneMe(clone));
			}
		}
	}
	
	void copyIfDirty(AbstractFunctionElement clone) {
		super.copyIfDirty(clone);
		for(FunctionParameterElement p : params) {
			if (p != null) {
				clone.setParameter(p.copyIfDirty());
			}
		}
	}
	
	/**
	 * @return The number of annotations added
	 */
	int applyPromises(final AnnotationVisitor v, final IRNode func) {
		if (func == null) {
			return 0;
		}
		int added = super.applyPromises(v, func);
		final IRNode params = SomeFunctionDeclaration.getParams(func);
		for(FunctionParameterElement p : this.params) {
			if (p != null) {
				added += p.applyPromises(v, Parameters.getFormal(params, p.getIndex()));
			}
		}
		return added;
	}
}
