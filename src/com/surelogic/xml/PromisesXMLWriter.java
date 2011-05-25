package com.surelogic.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.surelogic.common.xml.Entities;

import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.Iteratable;

public class PromisesXMLWriter implements TestXMLParserConstants {
	private static final int INCR = 2;
	
	final PrintWriter pw;
	final StringBuilder b = new StringBuilder();
	
	public PromisesXMLWriter(File f) throws FileNotFoundException {
		pw = new PrintWriter(f);
	}
	
	private void flush() {
		pw.write(b.toString());
		pw.flush();
		b.setLength(0);
	}
	
	/**
	 * @param attrs Should be key-value pairs
	 */
	private void start(int indent, String name, AbstractJavaElement e, String... attrs) {
		Entities.start(name, b, indent);
		if (e != null) {
			Entities.addAttribute(NAME_ATTRB, e.getName(), b);
		}
		for(int i=0; i<attrs.length; i+=2) {
			// TODO indent?
			Entities.addAttribute(attrs[i], attrs[i+1], b);
		}
		Entities.closeStart(b, false);
		flush();
	}
	
	private void end(int indent, String name) {
		Entities.end(name, b, indent);
		flush();
	}
	
	public void write(PackageElement pkg) {		
		start(0, PACKAGE, pkg);		
		writeAnnos(INCR, pkg);
		writeClass(INCR, pkg.getClassElement());
		end(0, PACKAGE);
		pw.close();
	}

	private void writeAnnos(int indent, AbstractJavaElement e) {
		for(AnnotationElement a : e.getPromises()) {
			writeAnno(indent, a);
		}
	}
	
	private void writeAnno(int indent, AnnotationElement a) {
		Entities.start(a.getPromise(), b, indent);
		for(Map.Entry<String,String> attr : a.getAttributes()) {
			// TODO indent?
			Entities.addAttribute(attr.getKey(), attr.getValue(), b);
		}
		if (a.isEmpty()) {
			Entities.closeStart(b, true);
		} else {
			Entities.closeStart(b, false);
			b.append(a.getContents());
			Entities.end(a.getPromise(), b, indent);
		}
		flush();
	}
	
	private void writeClass(int indent, ClassElement c) {		
		if (c == null) {
			return;
		}
		start(indent, CLASS, c);
		writeAnnos(indent+INCR, c);		
		if (c.getClassInit() != null) {			
			start(indent+INCR, CLASSINIT, c.getClassInit());
			writeAnnos(indent+INCR, c.getClassInit());
			end(indent+INCR, CLASSINIT);
		}
		for(FieldElement f : c.getFields()) {
			writeField(indent+INCR, f);
		}
		for(ConstructorElement e : c.getConstructors()) {
			writeConstructor(indent+INCR, e);
		}
		for(MethodElement m : c.getMethods()) {
			writeMethod(indent+INCR, m);
		}
		for(ClassElement e : c.getNestedClasses()) {
			writeClass(indent+INCR, e);
		}
		end(indent, CLASS);
	}
	
	private void writeField(int indent, FieldElement f) {
		start(indent, FIELD, f);
		writeAnnos(indent+INCR, f);
		end(indent, FIELD);
	}
	
	private void writeMethod(int indent, MethodElement m) {
		start(indent, METHOD, m, PARAMS_ATTRB, m.getParams());
		writeAnnos(indent+INCR, m);
		writeParameters(indent+INCR, m);
		end(indent, METHOD);
	}
	
	private void writeConstructor(int indent, ConstructorElement m) {
		start(indent, CONSTRUCTOR, null, PARAMS_ATTRB, m.getParams());
		writeAnnos(indent+INCR, m);
		writeParameters(indent+INCR, m);
		end(indent, CONSTRUCTOR);
	}
	
	private void writeParameters(int indent, AbstractFunctionElement f) {
		for(FunctionParameterElement p : f.getParameters()) {
			if (p == null) {
				continue;
			}
			writeParameter(indent, p);
		}		
	}

	private void writeParameter(int indent, FunctionParameterElement p) {
		start(indent, PARAMETER, null, INDEX_ATTRB, Integer.toString(p.getIndex()));
		writeAnnos(indent+INCR, p);		
		end(indent, PARAMETER);
	}

	public static <T> Iteratable<T> getSortedValues(final Map<String,T> map) {
		if (map.isEmpty()) {
			return EmptyIterator.prototype();
		}
		final List<String> keys = new ArrayList<String>(map.keySet());
		Collections.sort(keys);
		return new FilterIterator<String, T>(keys.iterator()) {
			@Override
			protected Object select(String o) {
				return map.get(o);
			}
		};
	}
	
	public static <T> Iterable<Map.Entry<String,T>> getSortedEntries(final Map<String,T> map) {
		if (map.isEmpty()) {
			return EmptyIterator.prototype();
		}
		final List<Map.Entry<String,T>> entries = new ArrayList<Map.Entry<String,T>>(map.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String,T>>() {
			public int compare(Entry<String, T> o1, Entry<String, T> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		return entries;
	}
}
