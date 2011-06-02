package com.surelogic.xml;

import java.util.*;

import com.surelogic.common.xml.Entity;

public abstract class AbstractFunctionElement extends AbstractJavaElement 
implements IClassMember, TestXMLParserConstants
{	
	private final String genericParams;
	private final String parameters;
	private final List<FunctionParameterElement> params = new ArrayList<FunctionParameterElement>();
	
	AbstractFunctionElement(String id, Entity e) {
		super(id);
		
		final String params = e.getAttribute(PARAMS_ATTRB);
		parameters = normalize(params);
		genericParams = e.getAttribute(GENERIC_PARAMS_ATTRB);
	}
	
	private static String normalize(String orig) {
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
	
	public final String getGenericParams() {
		return genericParams;
	}
	
	void setParameter(FunctionParameterElement p) {
		// Make params big enough
		while (params.size() <= p.getIndex()) {
			params.add(null);			
		}
		params.set(p.getIndex(), p);
	}
	
	FunctionParameterElement getParameter(int i) {
		return params.get(i);
	}

	Iterable<FunctionParameterElement> getParameters() {
		return params;
	}
}
