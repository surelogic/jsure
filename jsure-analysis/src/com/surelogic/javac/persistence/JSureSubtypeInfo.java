package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.google.common.collect.Multimap;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.SourceCUDrop;


public class JSureSubtypeInfo {
	private static final String SUBTYPES_ZIP = "subtypes.zip";	
	
	final File zip;
	final Set<String> names = new HashSet<>();
	
	public JSureSubtypeInfo(File zip) throws IOException {
		this.zip = zip;
		
		ZipFile zf = new ZipFile(zip);
		Enumeration<? extends ZipEntry> e = zf.entries();
		while (e.hasMoreElements()) {
			ZipEntry ze = e.nextElement();
			names.add(ze.getName());
		}
		zf.close();
	}

	public static void save(File runDir, Multimap<CUDrop, CUDrop> subtypeDependencies) throws IOException {
		final File zip = new File(runDir, SUBTYPES_ZIP);
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
		for(Map.Entry<CUDrop,Collection<CUDrop>> e : subtypeDependencies.asMap().entrySet()) {
			final String name = computeCUName(e.getKey());
			final ZipEntry ze = new ZipEntry(name);
			out.putNextEntry(ze);

			final PrintWriter pw = new PrintWriter(out);
			for(CUDrop sub : e.getValue()) {
				final String sName = computeCUName(sub);
				pw.println(sName);
			}
			pw.flush();
		}
		out.close();
	}

	private static String computeCUName(CUDrop cu) {
		if (cu instanceof SourceCUDrop) {
			return cu.getRelativePath();
		}
		return cu.getJavaOSFileName();
	}

	public static JSureSubtypeInfo load(File runDir) throws IOException {
		final File zip = new File(runDir, SUBTYPES_ZIP);
		if (runDir == null || !runDir.exists() || !zip.exists()) {
			return null;
		}
		JSureSubtypeInfo info = new JSureSubtypeInfo(zip);
		return info;
	}

	public Set<String> findCUsContainingSubTypes(Set<String> roots) throws IOException {
		if (roots.isEmpty()) {
			return Collections.emptySet();
		}
		final Set<String> results = new HashSet<>();
		final ZipFile zf = new ZipFile(zip);
		for(String root : roots) {
			if (!names.contains(root)) {
				// No (known) subtypes
				continue;
			}
			ZipEntry ze = zf.getEntry(root);
			if (ze == null) {
				throw new IllegalStateException("No zip entry");
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
			String line;
			while ((line = br.readLine()) != null) {
				results.add(line);
			}
			br.close();
		}
		zf.close();
		return results;
	}
}
