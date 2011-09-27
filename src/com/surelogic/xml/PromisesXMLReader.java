package com.surelogic.xml;

import java.io.*;
import java.util.*;

import org.xml.sax.Attributes;

import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.IXMLResultListener;
import com.surelogic.common.xml.NestedXMLReader;

/**
 * Reads .promises.xml files to create JSure models
 * 
 * @author Edwin
 */
public class PromisesXMLReader extends NestedXMLReader implements IXMLResultListener, TestXMLParserConstants {	
	PackageElement pkg;
	String pkgName;
	ClassElement clazz;
	final List<AnnotationElement> promises = new ArrayList<AnnotationElement>(0);
	
	@Override
	protected String checkForRoot(String name, Attributes attributes) {
		//System.out.println("Checking for root: "+name);
		if (PACKAGE.equals(name)) {
			if (attributes == null) {
				return "";
			}
			return attributes.getValue(NAME_ATTRB);
		}
		return null;
	}

	public void start(String pkg, String na) {
		pkgName = pkg;
	}
	
	public Entity makeEntity(String name, Attributes a) {
		//System.out.println("Making entity for "+name);
		return new Entity(name, a);
	}
	
	/**
	 * Called for 'top-level' entities within the package
	 */
	public void notify(Entity e) {
		final String name = e.getName().toLowerCase();
		if (COMMENT_TAG.equals(name)) { 
			System.err.println("Ignoring top-level comment: "+e.getAttribute(COMMENT_TAG));
		} else if (CLASS.equals(name)) {
			final String id = e.getAttribute(NAME_ATTRB);
			clazz = new ClassElement(id);
			
			handleNestedElements(clazz, e);
			clazz.addComments(comments);
			comments.clear();
		}
		else {
			final String uid = e.getAttribute(UID_ATTRB);
			promises.add(new AnnotationElement(uid, name, e.getCData(), e.getAttributes()));
		}
	}
	
	private void handleNestedElement(ClassElement c, Entity n, List<String> comments) {		
		final String name = n.getName();
		final String id = n.getAttribute(NAME_ATTRB);
		//System.out.println("Looking at "+name+" -- "+id);
		
		IClassMember m;
		if (METHOD.equals(name)) {
			c.addMember(m = handleNestedElements(new MethodElement(id, n), n));
		}
		else if (CLASS.equals(name)) {
			c.addMember(m = handleNestedElements(new NestedClassElement(id), n));
		}
		else if (CONSTRUCTOR.equals(name)) {
			c.addMember(m = handleNestedElements(new ConstructorElement(n), n));
		}		
		else if (FIELD.endsWith(name)) {
			c.addMember(m = handleAnnotations(new FieldElement(id), n));
		}
		else if (CLASSINIT.equals(name)) {
			c.addMember(m = handleAnnotations(new ClassInitElement(), n));
		}
		else { 			
			handleAnnotationOnElt(c, comments, n);
			return;
		}
		m.addComments(comments);
	}
	
	private IClassMember handleNestedElements(ClassElement cl, Entity c) {
		// Handle comments
		final List<String> comments = new ArrayList<String>(0);
		for(Entity n : c.getReferences()) {
			if (COMMENT_TAG.equals(n.getName())) {
				final String comment = n.getAttribute(COMMENT_TAG);
				System.out.println("Comment: "+comment);
				comments.add(comment);
			} else {
				handleNestedElement(cl, n, comments);
				comments.clear();
			}
		}
		cl.setLastComments(comments);
		
		if (cl instanceof NestedClassElement) {
			return (IClassMember) cl;
		}
		return null;
	}
	
	private IClassMember handleNestedElements(AbstractFunctionElement func, Entity f) {
		// Handle comments
		final List<String> comments = new ArrayList<String>(0);
		for(Entity n : f.getReferences()) {
			if (COMMENT_TAG.equals(n.getName())) {
				comments.add(n.getAttribute(COMMENT_TAG));
			} else if (PARAMETER.equals(n.getName())) {
				final int i = Integer.parseInt(n.getAttribute(INDEX_ATTRB)); 
				FunctionParameterElement fe = new FunctionParameterElement(i);
				func.setParameter(handleAnnotations(fe, n));
				fe.addComments(comments);
				comments.clear();
			} else {
				handleAnnotationOnElt(func, comments, n);
			}
		}
		func.setLastComments(comments);
		return func;
	}

	private static void handleAnnotationOnElt(AnnotatedJavaElement func, final List<String> comments, Entity n) {
		final String uid = n.getAttribute(UID_ATTRB);
		AnnotationElement a = new AnnotationElement(uid, n.getName(), n.getCData(), n.getAttributes());
		func.addPromise(a);
		a.addComments(comments);
		comments.clear();
	}

	private static <T extends AnnotatedJavaElement> T handleAnnotations(T e, Entity n) {
		// Handle comments
		final List<String> comments = new ArrayList<String>(0);
		for(Entity a : n.getReferences()) {
			if (COMMENT_TAG.equals(n.getName())) {
				comments.add(n.getAttribute(COMMENT_TAG));
			} else {
				handleAnnotationOnElt(e, comments, a);
			}
		}
		e.setLastComments(comments);
		return e;
	}
	
	public void done() {
		if (clazz != null) { 
			clazz.setLastComments(comments);
		}
		
		pkg = new PackageElement(pkgName, clazz);		
		for(AnnotationElement a : promises) {
			pkg.addPromise(a);
		}
		pkg.markAsClean();
	}
	
	public final PackageElement getPackage() {
		return pkg;
	}
	
	public static PackageElement load(File f) throws Exception {
		InputStream is = new FileInputStream(f);
		try {
			PromisesXMLReader r = new PromisesXMLReader();
			r.read(is);
			return r.getPackage();
		} finally {
			is.close();
		}
	}
}
