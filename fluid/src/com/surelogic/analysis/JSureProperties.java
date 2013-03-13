/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.annotation.rules.CompUnitPattern;
import com.surelogic.annotation.rules.ModuleRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ide.IDE;

public class JSureProperties {
	private static final Logger LOG = SLLogger.getLogger("analysis.JSureProperties");
	private static final String MODULE_PREFIX = "Module.";
	public static final String JSURE_PROPERTIES = "jsure.properties";
	public static final String SRC_EXCLUDES = "source.excludes";
	
	private static final List<String> excludedLibPaths = new ArrayList<String>();
	
	public static void clearLibraryPath() {
		excludedLibPaths.clear();
	}

	public static boolean onLibPath(final String path) {
		//final String path = resource.getProjectRelativePath().toPortableString();
		for(String excludePath : excludedLibPaths) {
			if (path.startsWith(excludePath)) {
				return false;
			}
		}
		return true;
	}
	
	public static void excludeLibraryPath(String excludePath) {
		excludedLibPaths.add(excludePath);
	}
	
	public static Properties read(File project) {
        final File properties = new File(project, JSureProperties.JSURE_PROPERTIES);
        if (properties.exists() && properties.isFile()) {
            final Properties props = new Properties();
            // props.put(PROJECT_KEY, p);
            try {
                InputStream is = new FileInputStream(properties);
                props.load(is);
                is.close();
            } catch (IOException e) {
                String msg = "Problem while loading "
                        + JSureProperties.JSURE_PROPERTIES + ": "
                        + e.getMessage();
                // reportProblem(msg, null);
                LOG.log(Level.SEVERE, msg, e);
            } finally {
                // Nothing to do
            }
            return props;
        }
        return null;
	}
	
	public static void handle(String proj, Properties props) {
		if (props.size() > 0) {
			// process
			for (String defaults : getValues(props, IDE.MODULE_DEFAULTS)) {
				if (defaults.equals(IDE.AS_CLASS)) {
					ModuleRules.defaultAsSource(false);
				} else if (defaults.equals(IDE.AS_NEEDED)) {
					ModuleRules.defaultAsSource(false);
					ModuleRules.defaultAsNeeded(true);
				} else if (defaults.equals(IDE.AS_SOURCE)) {
					ModuleRules.defaultAsSource(true);
				} else {
					LOG.severe("Unknown value for " + IDE.MODULE_DEFAULTS
							+ ": " + defaults);
				}
			}
			for (String modulePattern : getValues(props, IDE.MODULE_REQUIRED)) {
				ModuleRules.setAsNeeded(modulePattern, false);
			}
			for (String modulePattern : getValues(props, IDE.MODULE_AS_NEEDED)) {
				ModuleRules.setAsNeeded(modulePattern, true);
				ModuleRules.setAsSource(modulePattern, false);
			}
			for (String modulePattern : getValues(props, IDE.MODULE_AS_CLASS)) {
				ModuleRules.setAsSource(modulePattern, false);
			}
			for (String modulePattern : getValues(props, IDE.MODULE_AS_SOURCE)) {
				ModuleRules.setAsNeeded(modulePattern, false);
				ModuleRules.setAsSource(modulePattern, true);
			}
			for (String moduleKey : getModules(props)) {
				String pattern = props.getProperty(moduleKey, null);
				if (pattern != null) {
					createModuleFromKeyAndPattern(proj, IDE.MODULE_DECL_PREFIX,
							moduleKey, pattern);
				}
			}
			clearLibraryPath();
			for (String excludePath : getValues(props, IDE.LIB_EXCLUDES)) {
				excludeLibraryPath(excludePath);
			}
		}
	}
	
	public static void createModuleFromKeyAndPattern(String proj, String prefix, String key,
			String pattern) {
		ModuleRules.createModule(proj, key.substring(prefix.length()), pattern);
	}
	
	/**
	 * @param props
	 *            The set of properties to search for module definitions
	 * @return The keys for all the modules found
	 */
	private static Iteratable<String> getModules(Properties props) {
		if (props.isEmpty()) {
			return new EmptyIterator<String>();
		}
		final Set<Object> keys = props.keySet();
		return new FilterIterator<Object, String>(keys.iterator()) {
			@Override
			protected Object select(Object o) {
				if (o instanceof String) {
					String key = (String) o;
					if (key.startsWith(IDE.MODULE_DECL_PREFIX)) {
						return key;
					}
				}
				return IteratorUtil.noElement;
			}
		};
	}
	
	/**
	 * Gets the comma-separated values for the given key
	 */
	private static Iteratable<String> getValues(Properties props, String key) {
		String prop = props.getProperty(key, "");
		if (prop.equals("")) {
			return new EmptyIterator<String>();
		}
		final StringTokenizer st = new StringTokenizer(prop, ",");
		if (!st.hasMoreTokens()) {
			return new EmptyIterator<String>();
		}
		return new SimpleRemovelessIterator<String>() {
			@Override
			protected Object computeNext() {
				if (st.hasMoreTokens()) {
					return st.nextToken().trim();
				}
				return IteratorUtil.noElement;
			}
		};
	}
	
	public static void handleArgs(String proj, Map<String,String> args) {
		final Iterator<Map.Entry<String, String>> it = args.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> e = it.next();
			String key = e.getKey();
			String pattern = e.getValue();
			/*if (key.equals(Majordomo.BUILD_KIND)) {
				continue; // Nothing to do here
			} else */
			
			if (key.startsWith("ConvertToIR.asClass")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(proj,
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					LOG.info("Adding pattern to convert as .class: '" + pat
							+ "'");
					ModuleRules.setAsSource(pat, false);
				}
			} else if (key.startsWith("ConvertToIR.asSource")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// true);
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(proj,
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					LOG.info("Adding pattern to convert as source: '" + pat
							+ "'");
					ModuleRules.setAsSource(pat, true);
					ModuleRules.setAsNeeded(pat, false);
				}
			} else if (key.equals("ConvertToIR.defaultAsSource")) {
				ModuleRules.defaultAsSource(true);
			} else if (key.equals("ConvertToIR.defaultAsClass")) {
				ModuleRules.defaultAsSource(false);
			} else if (key.equals("ConvertToIR.defaultAsNeeded")) {
				ModuleRules.defaultAsSource(false);
				ModuleRules.defaultAsNeeded(true);
			} else if (key.startsWith("ConvertToIR.asNeeded")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// false);
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// true);
				for (Iterator<CompUnitPattern> p = parsePatterns(proj,
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					ModuleRules.setAsSource(pat, false);
					ModuleRules.setAsNeeded(pat, true);
				}
			} else if (key.startsWith("ConvertToIR.required")) {
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(proj,
						pattern); p.hasNext();) {
					ModuleRules.setAsNeeded(p.next(), false);
				}
			} else if (key.startsWith(MODULE_PREFIX)) {
				JSureProperties.createModuleFromKeyAndPattern(proj, 
						MODULE_PREFIX, key, pattern);
			} else {
				String warn = "Got an unrecognized key in .project: " + key;
				/*
				reportWarning(warn, Eclipse.getDefault().getResourceNode(
				".project"));
				*/
				LOG.warning(warn);
				continue;
			}
		}
	}

	private static Iterator<CompUnitPattern> parsePatterns(final String proj,
			String patterns) {
		final StringTokenizer st = new StringTokenizer(patterns, ",");
		return new SimpleRemovelessIterator<CompUnitPattern>() {
			@Override
			protected Object computeNext() {
				if (st.hasMoreElements()) {
					String pat = st.nextToken().trim();
					return CompUnitPattern.create(proj, pat);
				}
				return IteratorUtil.noElement;
			}
		};
	}
}
