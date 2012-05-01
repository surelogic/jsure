package com.surelogic.persistence;

import java.util.*;
import java.util.zip.ZipEntry;

import com.surelogic.analysis.*;
import com.surelogic.common.FileUtility;
import com.surelogic.common.xml.*;
import com.surelogic.common.jsure.xml.*;

import edu.cmu.cs.fluid.sea.*;

/**
 * Designed to find references to other CUs among the source refs in the XML
 * 
 * @author Edwin
 */
public class JSureResultsXMLRefScanner extends
		AbstractJSureResultsXMLReader<Void> {
	final Set<String> referencedTypeLocations = new HashSet<String>();
	final Set<String> referencedFilePaths = new HashSet<String>();

	public JSureResultsXMLRefScanner(IIRProjects p) {
		super(p);
	}

	@Override
	protected void finishedZipEntry(ZipEntry ze) {
		referencedFilePaths.add(ze.getName());
	}

	@Override
	protected Void createResult() {
		return null;
	}

	@Override
	protected void handleAboutRef(Void result, Entity pe, PromiseDrop<?> pd) {
		// Nothing else to do
	}

	@Override
	protected void handleAndRef(Void result, Entity pe, PromiseDrop<?> pd) {
		// Nothing else to do
	}

	@Override
	protected PromiseDrop<?> handlePromiseRef(Entity pr) {
		String location = pr.getAttribute(PROMISE_LOCATION);
		referencedTypeLocations.add(JavaIdentifier.extractType(location));
		return null;
	}

	@Override
	protected void handleSourceRef(Void result, Entity sr) {
		final String path = sr.getAttribute(AbstractXMLReader.PATH_ATTR);
		referencedFilePaths.add(FileUtility.normalizePath(path));
	}

	@Override
	protected void finishResult(Void result, Entity e, boolean checkedPromises) {
		// Nothing to do
	}

	public <T> Collection<T> selectByFilePath(Map<String, T> map) {
		return select(map, true);
	}

	public <T> Collection<T> selectByTypeLocation(Map<String, T> map) {
		return select(map, false);
	}

	public <T> Collection<T> select(Map<String, T> map, boolean useFile) {
		List<T> results = new ArrayList<T>();
		for (String file : useFile ? referencedFilePaths
				: referencedTypeLocations) {
			T result = map.get(file);
			if (result == null) {
				throw new IllegalStateException("No result for " + file);
			}
			results.add(result);
		}
		return results;
	}
}
