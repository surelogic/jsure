package com.surelogic.xml;

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

	@Override
	public void start(String pkg, String na) {
		pkgName = pkg;
	}
	
	@Override
	public Entity makeEntity(String name, Attributes a) {
		//System.out.println("Making entity for "+name);
		return new Entity(name, a);
	}
	
	/**
	 * Called for 'top-level' entities within the package
	 */
	public void notify(Entity e) {
		final String name = e.getName().toLowerCase();
		if (CLASS.equals(name)) {
			final String id = e.getAttribute(NAME_ATTRB);
			clazz = new ClassElement(id);
			for(Entity n : e.getReferences()) {
				handleNestedElement(clazz, n);
			}
		}
		else if (PROMISE.equals(name)) {
			final String uid = e.getAttribute(UID_ATTRB);
			promises.add(new AnnotationElement(uid, name, e.getCData(), e.getAttributes()));
		}
		else {
			throw new IllegalStateException("Unknown entity: "+name);
		}

	}
	
	private void handleNestedElement(ClassElement c, Entity n) {		
		final String name = n.getName();
		final String id = n.getAttribute(NAME_ATTRB);
		System.out.println("Looking at "+name+" -- "+id);
		if (METHOD.equals(name)) {
			final String params = n.getAttribute(PARAMS_ATTRB);
			c.addMember(handleNestedElements(new MethodElement(id, params), n));
		}
		else if (CLASS.equals(name)) {
			c.addMember(handleNestedElements(new NestedClassElement(id), n));
		}
		else if (CONSTRUCTOR.equals(name)) {
			final String params = n.getAttribute(PARAMS_ATTRB);
			c.addMember(handleNestedElements(new ConstructorElement(params), n));
		}		
		else if (FIELD.endsWith(name)) {
			c.addMember(handleAnnotations(new FieldElement(id), n));
		}
		else if (CLASSINIT.equals(name)) {
			c.addMember(handleAnnotations(new ClassInitElement(), n));
		}
		// Make an annotation		 
		final String uid = n.getAttribute(UID_ATTRB);
		c.addPromise(new AnnotationElement(uid, name, n.getCData(), n.getAttributes()));
	}
	
	private IClassMember handleNestedElements(NestedClassElement cl, Entity c) {
		for(Entity n : c.getReferences()) {
			handleNestedElement(cl, n);
		}
		return cl;
	}
	
	private IClassMember handleNestedElements(AbstractFunctionElement func, Entity f) {
		for(Entity n : f.getReferences()) {
			if (PARAMETER.equals(n.getName())) {
				final int i = Integer.parseInt(n.getAttribute(INDEX_ATTRB)); 
				func.setParameter(new FunctionParameterElement(i));
			} else {
				final String uid = n.getAttribute(UID_ATTRB);
				func.addPromise(new AnnotationElement(uid, n.getName(), n.getCData(), n.getAttributes()));
			}
		}
		return func;
	}

	private static <T extends AbstractJavaElement> T handleAnnotations(T e, Entity n) {
		for(Entity a : n.getReferences()) {
			final String uid = a.getAttribute(UID_ATTRB);
			e.addPromise(new AnnotationElement(uid, a.getName(), a.getCData(), a.getAttributes()));
		}
		return e;
	}
	
	@Override
	public void done() {
		pkg = new PackageElement(pkgName, clazz);		
		for(AnnotationElement a : promises) {
			pkg.addPromise(a);
		}
	}
	
	public final PackageElement getPackage() {
		return pkg;
	}
}
