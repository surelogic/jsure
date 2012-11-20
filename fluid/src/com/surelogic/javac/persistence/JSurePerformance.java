package com.surelogic.javac.persistence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.SortedProperties;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.Projects;

public final class JSurePerformance {
	public static final String PROP_PREFIX = "jsure.";
	private static final long UNINIT = -1;
	
	private final Projects projects;
	private final Properties props = new SortedProperties();
	private long time = UNINIT;
	private long start = UNINIT;
	
	public JSurePerformance(Projects projects) {
		this.projects = projects;
	}

	public void setIntProperty(String key, int i) {	
		setProperty(key, Integer.toString(i));
	}
	
	public void setLongProperty(String key, long i) {	
		setProperty(key, Long.toString(i));
	}
	
	public void setProperty(String key, String value) {	
		if (key == null || value == null) {
			throw new IllegalArgumentException("Null key or value");
		}
		props.setProperty(PROP_PREFIX+key, value);
	}
	
	public long startTiming() {
		if (start != UNINIT) {
			throw new IllegalStateException("Already started timer");
		}
		return start = time = System.currentTimeMillis();
	}
	
	/**
	 * Store the time difference under the given key, and keep the timer going
	 */
	public long markTimeFor(final String key) {		
		if (time == UNINIT) {
			throw new IllegalStateException("Haven't started timer"); 
		}
		final long now = System.currentTimeMillis();
		final long diff = now - time;
		setLongProperty(key, diff);
		time = now;
		return diff;
	}
	
	public long stopTiming(final String key) {
		if (start == UNINIT) {
			throw new IllegalStateException("Haven't started timer"); 
		}
		final long now = System.currentTimeMillis();
		final long diff = now - start;
		setLongProperty(key, diff);
		start = time = UNINIT;
		return diff;
	}

	public void store() {
		File target = new File(projects.getRunDir(), ScanProperty.SCAN_PROPERTIES);
		try {
			props.store(new FileWriter(target), 
					    "Performance data for "+projects.getLabel());
		} catch (IOException e) {
			SLLogger.getLogger().log(Level.WARNING, "Unable to save performance data for "+projects.getRun(), e);
		}
	}

	public void print(PrintStream out) {
		try {
			props.store(out, "Performance data for "+projects.getLabel());
		} catch (IOException e) {
			// Ignore
		}
	}
}
