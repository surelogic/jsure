package com.surelogic.xml;

import java.util.*;

public abstract class AbstractFunctionElement extends AbstractJavaElement 
implements IClassMember 
{	
	private final String parameters;
	private final List<FunctionParameterElement> params = new ArrayList<FunctionParameterElement>();
	
	AbstractFunctionElement(String id, String params) {
		super(id);
		parameters = normalize(params);
	}
	
	private static String normalize(String orig) {
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
}
