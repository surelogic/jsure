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
		this(new PrintWriter(f));
	}

	public PromisesXMLWriter(PrintWriter w) {
		pw = w;
	}
	
	private void flush() {
		pw.write(b.toString());
		pw.flush();
		b.setLength(0);
	}
	
	/**
	 * @param attrs Should be key-value pairs
	 */
	private void start(int indent, String name, AnnotatedJavaElement e, String... attrs) {
		Entities.start(name, b, indent);
		if (e != null) {
			Entities.addAttribute(NAME_ATTRB, e.getName(), b);
		}
		for(int i=0; i<attrs.length; i+=2) {
			// TODO indent?
			final String value = attrs[i+1];
			if (value != null) {
				Entities.addAttribute(attrs[i], value, b);
			}
		}
		Entities.closeStart(b, false);
		flush();
	}
	
	private void end(int indent, String name) {
		Entities.end(name, b, indent);
		flush();
	}
	
	public void write(PackageElement pkg) {		
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.println();
		start(0, PACKAGE, pkg, REVISION_ATTRB, Integer.toString(pkg.getRevision()));		
		writeAnnos(INCR, pkg);
		writeClass(INCR, pkg.getClassElement());
		end(0, PACKAGE);
		pw.close();
	}

	
	
	private void writeComments(int indent, AnnotatedJavaElement e) {
		writeComments(indent, e.getComments());
	}
	
	private void writeLastComments(int indent, AnnotatedJavaElement e) {
		writeComments(indent, e.getLastComments());
	}
	
	private void writeComments(int indent, Iterable<CommentElement> comments) {
		for(CommentElement c : comments) {
			Entities.indent(b, indent);
			b.append("<!--");
			b.append(CommentElement.MARKER);
			b.append(c.getUID().toString());
			b.append(CommentElement.SEPARATOR);
			b.append(c.getRevision());
			b.append(CommentElement.SEPARATOR);
			b.append(c.getModStatus().name());
			b.append(CommentElement.END_MARKER);
			b.append(Entities.unescape(c.getLabel()));
			b.append("-->\n");
			flush();
		}
	}
	
	private void writeAnnos(int indent, AnnotatedJavaElement e) {
		for(AnnotationElement a : e.getPromises(true)) {
			writeAnno(indent, a);
		}
	}
	
	private void writeAnno(int indent, AnnotationElement a) {
		writeComments(indent, a.getComments());
		Entities.start(a.getPromise(), b, indent);
		for(Map.Entry<String,String> attr : a.getAttributes()) {
			// TODO indent?
			Entities.addAttribute(attr.getKey(), attr.getValue(), b);
		}
		if (a.isEmpty()) {
			Entities.closeStart(b, true);
		} else {
			final boolean newline = a.getContents().contains("\n");
			Entities.closeStart(b, false, newline);
			Entities.addEscaped(a.getContents(), b);
			if (newline) {
				b.append('\n');
			}
			Entities.end(a.getPromise(), b, newline ? indent : -1);
		}
		flush();
	}
	
	private void writeClass(int indent, ClassElement c) {		
		if (c == null) {
			return;
		}
		writeComments(indent, c);
		start(indent, CLASS, c);
		writeAnnos(indent+INCR, c);		
		if (c.getClassInit() != null) {			
			writeComments(indent+INCR, c.getClassInit());
			start(indent+INCR, CLASSINIT, null);
			writeAnnos(indent+INCR+INCR, c.getClassInit());
			writeLastComments(indent+INCR+INCR, c.getClassInit());
			end(indent+INCR, CLASSINIT);
		}
		for(FieldElement f : c.getFields()) {
			writeField(indent+INCR, f);
		}
		boolean first = true;
		for(ConstructorElement e : c.getConstructors()) {
			first = handleFirstElt(first);
			writeConstructor(indent+INCR, e);
		}
		for(MethodElement m : c.getMethods()) {
			first = handleFirstElt(first);
			writeMethod(indent+INCR, m);
		}
		for(ClassElement e : c.getNestedClasses()) {
			first = handleFirstElt(first);
			writeClass(indent+INCR, e);
		}
		writeLastComments(indent, c);
		end(indent, CLASS);
	}
	
	private boolean handleFirstElt(boolean first) {
		if (!first) {
			pw.println();
		} else {
			first = false;
		}
		return first;
	}

	private void writeField(int indent, FieldElement f) {
		writeComments(indent, f);
		start(indent, FIELD, f);
		writeAnnos(indent+INCR, f);
		writeLastComments(indent+INCR, f);
		end(indent, FIELD);
	}
	
	private void writeMethod(int indent, MethodElement m) {
		writeComments(indent, m);
		if (m.getParams().length() == 0) {
			start(indent, METHOD, m);
		} else {
			start(indent, METHOD, m, PARAMS_ATTRB, m.getParams(), GENERIC_PARAMS_ATTRB, m.getGenericParams());
		}
		writeAnnos(indent+INCR, m);
		writeParameters(indent+INCR, m);
		writeLastComments(indent+INCR, m);
		end(indent, METHOD);
	}
	
	private void writeConstructor(int indent, ConstructorElement m) {
		writeComments(indent, m);
		if (m.getParams().length() == 0) {
			start(indent, CONSTRUCTOR, null);
		} else {
			start(indent, CONSTRUCTOR, null, PARAMS_ATTRB, m.getParams(), GENERIC_PARAMS_ATTRB, m.getGenericParams());
		}
		writeAnnos(indent+INCR, m);
		writeParameters(indent+INCR, m);
		writeLastComments(indent+INCR, m);
		end(indent, CONSTRUCTOR);
	}
	
	private void writeParameters(int indent, AbstractFunctionElement f) {
		boolean first = true;
		for(FunctionParameterElement p : f.getParameters()) {
			if (p == null) {
				continue;
			}
			first = handleFirstElt(first);
			writeParameter(indent, p);
		}		
	}

	private void writeParameter(int indent, FunctionParameterElement p) {
		writeComments(indent, p);
		start(indent, PARAMETER, null, INDEX_ATTRB, Integer.toString(p.getIndex()));
		writeAnnos(indent+INCR, p);		
		writeLastComments(indent+INCR, p);
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
