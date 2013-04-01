package com.surelogic.javac;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.surelogic.annotation.parse.*;
import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.SLUtility;
import com.surelogic.common.java.*;
import com.surelogic.common.java.PersistenceConstants;
import com.surelogic.javac.persistence.*;
import com.surelogic.persistence.*;

public class PromiseMatcher {	
	public static boolean findAndLoad(File dataDir) throws Exception {
		File run = null;
		for(File f : dataDir.listFiles()) {
			if (f.isDirectory()) {
				if (run == null || f.lastModified() > run.lastModified()) {
					run = f;
				} else {
					System.out.println("Ignored data dir: "+f.getName());
				}
			}
		}
		if (run == null) {
			return false;
		}
		System.out.println("Loading results from run: "+run.getName());
		return load(run);
	}
	
	public static boolean load(File runDir) throws Exception {
		final JSureScan run = JSureDataDirScanner.findRunDirectory(runDir);
		final Projects projs = run.getProjects();
		
		// Get source info
		File zips = new File(runDir, PersistenceConstants.ZIPS_DIR);
		Map<String,JavaSourceFile> path2JSF = new HashMap<String, JavaSourceFile>();
		for(JavacProject p : projs) {		
			File src = new File(zips, p.getName()+".zip");
			if (!src.exists()) {
				continue;
			}
			String srcPath = src.getAbsolutePath();
			ZipFile zf = new ZipFile(src);
			Properties props = new Properties();
			props.load(zf.getInputStream(zf.getEntry(AbstractJavaZip.CLASS_MAPPING)));
			for(Map.Entry<Object,Object> e : props.entrySet()) {
				final String path = e.getValue().toString();
				//TODO is this right?
				path2JSF.put(path, new JavaSourceFile(e.getKey().toString(), 
						AbstractJavaZip.makeZipReference(srcPath, path), path, false, p.getName()));
			}			
		}
		
		// Get results info
		JSureResultsXMLRefScanner scanner = new JSureResultsXMLRefScanner(projs);
		File results = new File(runDir, PersistenceConstants.RESULTS_ZIP);
		if (!results.exists()) {
			results = new File(runDir, PersistenceConstants.PARTIAL_RESULTS_ZIP);
		}
		if (!results.exists()) {
			return false;
		}
		scanner.readXMLArchive(results);	
		//TODO Do I really need to load these?
		//scanner.selectByTypeLocation(map);
		for(JavaSourceFile jsf : scanner.selectByFilePath(path2JSF)) {
			int firstSlash = jsf.relativePath.indexOf('/');
			String proj = firstSlash < 0 ? jsf.relativePath : jsf.relativePath.substring(0, firstSlash);
			JavacProject p = projs.get(proj);
			p.getConfig().addFile(jsf);
			System.out.println(proj+": added "+jsf.relativePath);
		}		
		Util.process(projs, false);
		
		JSureSubtypeInfo subTypeInfo = JSureSubtypeInfo.load(runDir);
		if (subTypeInfo != null) {
			// Just a test
			subTypeInfo.findCUsContainingSubTypes(Collections.singleton(SLUtility.JAVA_LANG_OBJECT));
			// TODO load up additional dependencies necessary based on the subtype info
		}		
		// Create drops from results! 
		JSureResultsXMLReader resultsReader = new JSureResultsXMLReader(projs);
		ParseUtil.init();    	
		try {
			resultsReader.readXMLArchive(results);
		} finally {
			ParseUtil.clear();
		}
		return true;
	}
}
