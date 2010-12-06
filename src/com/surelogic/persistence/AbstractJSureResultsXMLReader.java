/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import org.xml.sax.Attributes;

import com.surelogic.analysis.*;
import com.surelogic.common.xml.*;
import com.surelogic.jsure.xml.*;

import edu.cmu.cs.fluid.sea.*;

public abstract class AbstractJSureResultsXMLReader<T> extends NestedXMLReader implements IXMLResultListener, PersistenceConstants {
	protected final IIRProjects projects;

	public AbstractJSureResultsXMLReader(IIRProjects p) {
		projects = p;
	}
	
	@Override
	protected final String checkForRoot(String name, Attributes attributes) {
		if (COMP_UNIT.equals(name)) {
			if (attributes == null) {
				return "";
			}
			return attributes.getValue("path");
		}
		return null;
	}

	public final Entity makeEntity(String name, Attributes a) {
		return new Entity(name, a);
	}

	public final void start(String uid, String project) {
		System.out.println("uid = "+uid);
	}
	
	public void notify(Entity e) {
		if (!RESULT.equals(e.getName())) {
			throw new IllegalStateException("Unexpected top-level entity: "+e.getName());
		}
		if (e.numRefs() < 2) {
			throw new IllegalStateException("Missing about/source-ref: "+e.getName());
		}
		// I cannot build each result drop here
		boolean checkedPromises = false;
		T result = createResult(); 
		
		for(Entity nested : e.getReferences()) {
			if (ABOUT_REF.equals(nested.getName())) {
				PromiseDrop<?> pd = handlePromiseRef(nested);
				handleAboutRef(result, nested, pd);
			}
			else if (JSureXMLReader.SOURCE_REF.equals(nested.getName())) {				
				handleSourceRef(result, nested);		
			}
			else if (AND_REF.equals(nested.getName())) {			
				PromiseDrop<?> pd = handlePromiseRef(nested);
				handleAndRef(result, nested, pd);
				checkedPromises = true;
			}			
		}		
		finishResult(result, e, checkedPromises);		
	}
	
	protected abstract T createResult();
	protected abstract void handleSourceRef(T result, Entity srcRef);
	protected abstract PromiseDrop<?> handlePromiseRef(Entity pr);
	protected abstract void handleAboutRef(T result, Entity pe, PromiseDrop<?> pd);
	protected abstract void handleAndRef(T result, Entity pe, PromiseDrop<?> pd);
	protected abstract void finishResult(T result, Entity e, boolean checkedPromises);

	public final void done() {
		// Nothing to do here?
	}
}
