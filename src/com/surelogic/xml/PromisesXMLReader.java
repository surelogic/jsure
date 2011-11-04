package com.surelogic.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

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
	int revision;
	ClassElement clazz;
	final List<AnnotationElement> promises = new ArrayList<AnnotationElement>(0);
	
	@Override
	protected String checkForRoot(String name, Attributes attributes) {
		//System.out.println("Checking for root: "+name);
		if (PACKAGE.equals(name)) {
			if (attributes == null) {
				return "";
			}
			final String pkg = attributes.getValue(NAME_ATTRB);
			final String rev = attributes.getValue(REVISION_ATTRB);
			if (rev == null) {
				revision = 0;
			} else {
				try {
					revision = Integer.parseInt(rev);
				} catch(NumberFormatException e) {
					LOG.warning("Bad revision for package "+pkg+": "+rev);
					revision = 0;
				}
			}
			return pkg;
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
			//clazz.addComments(comments);
			comments.clear();
		}
		else {
			final String uid = e.getAttribute(UID_ATTRB);
			promises.add(new AnnotationElement(null, uid, name, e.getCData(), e.getAttributes()));
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
		//m.addComments(comments);
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
		//cl.setLastComments(comments);
		
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
				//fe.addComments(comments);
				comments.clear();
			} else {
				handleAnnotationOnElt(func, comments, n);
			}
		}
		//func.setLastComments(comments);
		return func;
	}

	private static void handleAnnotationOnElt(AnnotatedJavaElement func, final List<String> comments, Entity n) {
		final String uid = n.getAttribute(UID_ATTRB);
		AnnotationElement a = new AnnotationElement(func, uid, n.getName(), n.getCData(), n.getAttributes());
		func.addPromise(a);
		//a.addComments(comments);
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
		//e.setLastComments(comments);
		return e;
	}
	
	public void done() {
		/*
		if (clazz != null) { 
			clazz.setLastComments(comments);
		}
		*/
		
		pkg = new PackageElement(pkgName, revision, clazz);		
		for(AnnotationElement a : promises) {
			pkg.addPromise(a);
		}
		pkg.markAsClean();
	}
	
	public final PackageElement getPackage() {
		return pkg;
	}
	
	public static PackageElement loadRaw(File f) throws Exception {
		InputStream is = new FileInputStream(f);
		return loadRaw(is);
	}
	
	public static PackageElement loadRaw(InputStream is) throws Exception {
		try {
			PromisesXMLReader r = new PromisesXMLReader();
			r.read(is);
			return r.getPackage();
		} finally {
			is.close();
		}
	}

	public interface Listener {
		void refresh(PackageElement e);
	}
	
	private static final Collection<Listener> listeners = new CopyOnWriteArraySet<Listener>();
	
	public static void listenForRefresh(Listener l) {
		listeners.add(l);
	}
	
	public static void refreshAll(PackageElement e) {
		for(Listener v : listeners) {
			v.refresh(e);
		}
	}
	private static final Map<String,PackageElement> cache = new WeakHashMap<String, PackageElement>();
	
	public static PackageElement load(String relativePath, File fluid, File local) {
		System.out.println("Getting XML for "+relativePath);
		PackageElement p = cache.get(relativePath);
		if (p == null) {
			PackageElement f = loadOrNull(fluid);
			PackageElement l = loadOrNull(local);
			if (f == null) {
				p = l;
			}
			else if (l == null) {
				p = f;
			}
			else {
				p = l.merge(f, true);
			}
			cache.put(relativePath, p);			
		} else {
			System.out.println("Used cache for "+relativePath);
		}
		return p;
	}
	
	private static PackageElement loadOrNull(File f) {
		if (f != null) {
			try {
				return loadRaw(f);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
