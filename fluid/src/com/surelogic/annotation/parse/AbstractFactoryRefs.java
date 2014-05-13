package com.surelogic.annotation.parse;

import java.util.BitSet;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.parse.IASTFactory;

public class AbstractFactoryRefs {
	private static String[] packages = {
		"com.surelogic.aast.java",
		"com.surelogic.aast.promise",
	};

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static void register(IASTFactory f, String name, BitSet registered, int id) {
		Class<?> cls = null;
		for(String pkg : packages) {
			cls = getNodeClass(pkg, name);
			if (cls != null) {
				if (AASTNode.class.isAssignableFrom(cls)) {
					try {		  
						AbstractAASTNodeFactory factory = (AbstractAASTNodeFactory) cls.getField("factory").get(null);
						factory.register(f);				  
						registered.set(id);
					} catch (Exception e) {
						throw new IllegalStateException("Unable to register "+name);
					}
				}
			}
			else if (name.equals("Nothing") || name.endsWith("s")) {
				registered.set(id); // Not a real token
			}
		}
	}  

	private static Class<?> getNodeClass(String pkg, String name) {
		try {		  
			return Class.forName(pkg+'.'+name+"Node");
		} catch (ClassNotFoundException e) {
			// ignore
			return null;
		}
	}
}
