package com.surelogic.xml;

import java.util.*;

import com.surelogic.common.xml.Entity;

public abstract class AbstractFunctionElement extends AnnotatedJavaElement 
implements IClassMember, TestXMLParserConstants
{	
	private final String genericParams;
	private final String parameters;
	private final List<FunctionParameterElement> params = new ArrayList<FunctionParameterElement>();
	
	AbstractFunctionElement(String id, String params) {
		super(id);
		parameters = normalize(params);
		genericParams = null;
	}
	
	AbstractFunctionElement(String id, Entity e) {
		super(id);
		
		final String params = e.getAttribute(PARAMS_ATTRB);
		parameters = normalize(params);
		genericParams = e.getAttribute(GENERIC_PARAMS_ATTRB);
	}
	
	public final String getImageKey() {
		//return CommonImages.IMG_ASTERISK_ORANGE_50; // TODO
		return null;
	}
	
	public static String normalize(String orig) {
		if (orig == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		String[] splits = orig.split(",");
		for(String s : splits) {
			if (sb.length() > 0) {
				sb.append(",");
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

	Iterable<FunctionParameterElement> getParameters() {
		return params;
	}
	
	@Override
	public boolean hasChildren() {
		return super.hasChildren() || !params.isEmpty();
	}
	
	@Override
	protected void collectOtherChildren(List<Object> children) {
		super.collectOtherChildren(children);
		children.addAll(params);
	}
	
	public boolean isDirty() {
		if (super.isDirty()) {
			return true;
		}
		for(FunctionParameterElement p : params) {
			if (p.isDirty()) {
				return true;
			}
		}
		return false;
	}
	
	public void markAsClean() {
		super.markAsClean();
		for(FunctionParameterElement p : params) {
			p.markAsClean();
		}
	}
	
	boolean merge(AbstractFunctionElement changed, MergeType type) {
		boolean modified = false;
		for(FunctionParameterElement p2 : changed.params) {
			FunctionParameterElement p0 = getParameter(p2.getIndex());
			if (p0 != null) {
				modified |= p0.merge(p2, type);
			} else {
				setParameter(p2.cloneMe());
				modified = true;
			}
		}
		modified |= mergeThis(changed, type);
		return modified;
	}
	
	void copyToClone(AbstractFunctionElement clone) {
		super.copyToClone(clone);
		for(FunctionParameterElement p : params) {
			clone.setParameter(p.cloneMe());
		}
	}
	
	void copyIfDirty(AbstractFunctionElement clone) {
		super.copyIfDirty(clone);
		for(FunctionParameterElement p : params) {
			clone.setParameter(p.copyIfDirty());
		}
	}
}
