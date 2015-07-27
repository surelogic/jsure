/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.surelogic.annotation.CompUnitPattern;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.java.ICodeFile;

public class JSureProperties {
	private static final Logger LOG = SLLogger.getLogger("analysis.JSureProperties");
	public static final String JSURE_PROPERTIES = "jsure.properties";
	public static final String SRC_EXCLUDES = "source.excludes";

	public static final String AS_NEEDED = "asNeeded";
	public static final String REQUIRED = "required";
	public static final String AS_SOURCE = "asSource";
	public static final String AS_CLASS = "asClass";
	public static final String MODULE_PREFIX = "Module.";
	public static final String MODULE_DECL_PREFIX = "ModuleDecl.";

	public static final String MODULE_AS_NEEDED = MODULE_PREFIX + AS_NEEDED;
	public static final String MODULE_REQUIRED = MODULE_PREFIX + REQUIRED;
	public static final String MODULE_AS_SOURCE = MODULE_PREFIX + AS_SOURCE;
	public static final String MODULE_AS_CLASS = MODULE_PREFIX + AS_CLASS;
	public static final String MODULE_DEFAULTS = MODULE_PREFIX + "defaults";

	public static final String LIB_PREFIX = "Library.";
	public static final String LIB_EXCLUDES = LIB_PREFIX + "excludes";
	
	private static final List<String> excludedLibPaths = new ArrayList<>();

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
			for (String defaults : getValues(props, MODULE_DEFAULTS)) {
				if (defaults.equals(AS_CLASS)) {
					defaultAsSource(false);
				} else if (defaults.equals(AS_NEEDED)) {
					defaultAsSource(false);
					defaultAsNeeded(true);
				} else if (defaults.equals(AS_SOURCE)) {
					defaultAsSource(true);
				} else {
					LOG.severe("Unknown value for " + MODULE_DEFAULTS
							+ ": " + defaults);
				}
			}
			for (String modulePattern : getValues(props, MODULE_REQUIRED)) {
				setAsNeeded(modulePattern, false);
			}
			for (String modulePattern : getValues(props, MODULE_AS_NEEDED)) {
				setAsNeeded(modulePattern, true);
				setAsSource(modulePattern, false);
			}
			for (String modulePattern : getValues(props, MODULE_AS_CLASS)) {
				setAsSource(modulePattern, false);
			}
			for (String modulePattern : getValues(props, MODULE_AS_SOURCE)) {
				setAsNeeded(modulePattern, false);
				setAsSource(modulePattern, true);
			}
			for (String moduleKey : getModules(props)) {
				String pattern = props.getProperty(moduleKey, null);
				if (pattern != null) {
					createModuleFromKeyAndPattern(proj, MODULE_DECL_PREFIX,
							moduleKey, pattern);
				}
			}
			clearLibraryPath();
			for (String excludePath : getValues(props, LIB_EXCLUDES)) {
				excludeLibraryPath(excludePath);
			}
		}
	}

	public static void createModuleFromKeyAndPattern(String proj, String prefix, String key,
			String pattern) {
		createModule(proj, key.substring(prefix.length()), pattern);
	}

	/**
	 * @param props
	 *            The set of properties to search for module definitions
	 * @return The keys for all the modules found
	 */
	private static Iteratable<String> getModules(Properties props) {
		if (props.isEmpty()) {
			return new EmptyIterator<>();
		}
		final Set<Object> keys = props.keySet();
		return new FilterIterator<Object, String>(keys.iterator()) {
			@Override
			protected Object select(Object o) {
				if (o instanceof String) {
					String key = (String) o;
					if (key.startsWith(MODULE_DECL_PREFIX)) {
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
			return new EmptyIterator<>();
		}
		final StringTokenizer st = new StringTokenizer(prop, ",");
		if (!st.hasMoreTokens()) {
			return new EmptyIterator<>();
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
					setAsSource(pat, false);
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
					setAsSource(pat, true);
					setAsNeeded(pat, false);
				}
			} else if (key.equals("ConvertToIR.defaultAsSource")) {
				defaultAsSource(true);
			} else if (key.equals("ConvertToIR.defaultAsClass")) {
				defaultAsSource(false);
			} else if (key.equals("ConvertToIR.defaultAsNeeded")) {
				defaultAsSource(false);
				defaultAsNeeded(true);
			} else if (key.startsWith("ConvertToIR.asNeeded")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// false);
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// true);
				for (Iterator<CompUnitPattern> p = parsePatterns(proj,
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					setAsSource(pat, false);
					setAsNeeded(pat, true);
				}
			} else if (key.startsWith("ConvertToIR.required")) {
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(proj,
						pattern); p.hasNext();) {
					setAsNeeded(p.next(), false);
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

	private static final Map<CompUnitPattern, Boolean> asSourcePatterns = new HashMap<>();
	private static final Map<CompUnitPattern, Boolean> asNeededPatterns = new HashMap<>();

	public static void clearAsSourcePatterns() {
		asSourcePatterns.clear();
	}

	public static void clearAsNeededPatterns() {
		asNeededPatterns.clear();
		//modulePatterns.clear();
	}

	public static void setAsSource(CompUnitPattern pattern, boolean asSource) {
		// System.out.println("Setting pattern "+pattern+" asSource =
		// "+asSource);
		asSourcePatterns.put(pattern, asSource ? Boolean.TRUE : Boolean.FALSE);
	}

	public static void setAsNeeded(CompUnitPattern pattern, boolean asSource) {
		// System.out.println("Setting pattern "+pattern+" asSource =
		// "+asSource);
		asNeededPatterns.put(pattern, asSource ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * @param f_cu
	 * @return
	 */
	public static boolean treatedAsSource(ICodeFile cf) {
		//ICodeFile cf = new EclipseCodeFile(cu);
		return matchingPattern(asSourcePatterns, getDefaultAsSource(), cf);
	}

	public static boolean loadedAsNeeded(ICodeFile cf) {
		//ICodeFile cf = new EclipseCodeFile(cu);
		return matchingPattern(asNeededPatterns, getDefaultAsNeeded(), cf);
	}

	private static boolean matchingPattern(Map<CompUnitPattern, ?> patterns,
			boolean flag, ICodeFile cu) {
		Boolean b = (Boolean) matchingPattern(patterns, cu, flag ? Boolean.TRUE
				: Boolean.FALSE);
		return b.booleanValue();
	}

	public static Object matchingPattern(Map<CompUnitPattern, ?> patterns,
			ICodeFile cu, Object rv) {
		if (cu == null) {
			return null;
		}
		CompUnitPattern last = null;
		String pkg = null;

		for (Map.Entry<CompUnitPattern, ?> e : patterns.entrySet()) {
			CompUnitPattern pat = e.getKey();
			Object val = e.getValue();

			// optimization
			if (pkg == null) {
				pkg = cu.getPackage(); // All assumed to be in same project
			}

			if (pat.matches(pkg, null)) {
				if (last != null) {
					// multiple matches
					if (!rv.equals(val)) {
						LOG.severe("Multiple CU patterns match and disagree: "
								+ last + ", " + pat);
					} else {
						LOG.warning("Multiple CU patterns match: " + last
								+ ", " + pat);
					}
				}
				last = pat;
				rv = val;
			}
		}
		return rv;
	}

	public static Object matchingPattern(Map<CompUnitPattern, ?> patterns,
			final String pkg, final String path, Object rv) {
		if (pkg == null) {
			return null;
		}
		CompUnitPattern last = null;

		for (Map.Entry<CompUnitPattern, ?> e : patterns.entrySet()) {
			CompUnitPattern pat = e.getKey();
			Object val = e.getValue();

			if (pat.matches(pkg, path)) {
				if (last != null) {
					// multiple matches
					if (!rv.equals(val)) {
						LOG.severe("Multiple CU patterns match and disagree: "
								+ last + ", " + pat);
					} else {
						LOG.warning("Multiple CU patterns match: " + last
								+ ", " + pat);
					}
				}
				last = pat;
				rv = val;
			}
		}
		return rv;
	}

	private static boolean defaultAsSource = true;
	private static boolean defaultAsNeeded = false;

	private static List<ModulePattern> yes_AsNeeded = new ArrayList<>();
	private static List<ModulePattern> no_AsNeeded  = new ArrayList<>();
	private static List<ModulePattern> yes_AsSource = new ArrayList<>();
	private static List<ModulePattern> no_AsSource  = new ArrayList<>();

	private static Map<String,ModulePattern> patternCache = new HashMap<>();

	private static ModulePattern findPattern(String pattern) {
		ModulePattern p = patternCache.get(pattern);
		if (p == null) {
			if (pattern.indexOf('*') < 0) {
				p = new NoWildcards(pattern);
			} else {
				p = new Wildcards(pattern);
			}
			patternCache.put(pattern, p);
		}
		return p;
	}

	public static void clearSettings() {
		defaultAsSource = true;
		defaultAsNeeded = false;
		yes_AsNeeded.clear();
		no_AsNeeded.clear();
		yes_AsSource.clear();
		no_AsSource.clear();
	}

	public static void defaultAsSource(boolean b) {
		LOG.fine(b ? "Defaulting to load as source" : "Defaulting to load as class");
		defaultAsSource = b;
	}

	public static void defaultAsNeeded(boolean b) {
		if (b) {
			LOG.fine("Defaulting to load as needed");
		}
		defaultAsNeeded = b;
	} 

	public static boolean getDefaultAsSource() {
		return defaultAsSource;
	}

	public static boolean getDefaultAsNeeded() {
		return defaultAsNeeded;
	}

	public static void setAsNeeded(String modulePattern, boolean b) {
		final String msg = b ? "Loading as needed: " : "Loading required: ";
		LOG.fine(msg+modulePattern);
		List<ModulePattern> l = b ? yes_AsNeeded : no_AsNeeded; 
		ModulePattern p       = findPattern(modulePattern);
		l.add(p);
	}

	public static void setAsSource(String modulePattern, boolean b) {
		final String msg = b ? "Loading as source: " : "Loading as class: ";
		LOG.fine(msg+modulePattern);
		List<ModulePattern> l = b ? yes_AsSource : no_AsSource; 
		ModulePattern p       = findPattern(modulePattern);
		l.add(p);
	}

	private static boolean processPatterns(final String mod, List<ModulePattern> yes, List<ModulePattern> no, boolean defaultVal) {
		for (ModulePattern p : yes) {
			if (p.match(mod)) {
				return true;
			}
		}
		for (ModulePattern p : no) {
			if (p.match(mod)) {
				return false;
			}
		}
		return defaultVal;
	}

	public static boolean loadedAsNeeded(String module) {
		boolean rv = processPatterns(module, yes_AsNeeded, no_AsNeeded, defaultAsNeeded);
		//System.out.println(module+" as needed? "+rv);
		return rv;
	}
	public static boolean treatedAsSource(String module) {
		boolean rv = processPatterns(module, yes_AsSource, no_AsSource, defaultAsSource);
		//System.out.println(module+" as source? "+rv);
		return rv;
	}
	
	private interface ModulePattern {    
		boolean match(String s);
	}

	private static class NoWildcards implements ModulePattern {
		final String match;
		NoWildcards(String pattern) {
			this.match = pattern;
		}
		@Override
		public boolean match(String s) {
			return match.equals(s);
		}    
	}

	private static class Wildcards implements ModulePattern {
		final Pattern compiledPattern;
		Wildcards(String pattern) {      
			final String noDots    = pattern.replaceAll("\\.", "\\.");
			final String wildcards = noDots.replaceAll("\\*", ".*");
			compiledPattern = Pattern.compile(wildcards);
		}
		@Override
		public boolean match(String s) {
			Matcher m = compiledPattern.matcher(s);
			return m.matches();
		}    
	}

	private static final Map<CompUnitPattern, String> modulePatterns = new HashMap<>();

	/**
	 * @param name
	 *            The module to be created
	 * @param patterns
	 *            A comma-separated list of patterns
	 */
	public static void createModule(String proj, String name, String patterns) {
		StringTokenizer st = new StringTokenizer(patterns, ",");
		while (st.hasMoreTokens()) {
			String pat = st.nextToken().trim();
			Object old = modulePatterns.put(CompUnitPattern.create(proj, pat),
					name);
			if (old != null) {
				LOG.severe("Somehow displaced an existing module mapping for "
						+ old);
			}
		}
	}

	public static String getModule(ICodeFile cu) {
		return (String) JSureProperties.matchingPattern(modulePatterns, cu, REST_OF_THE_WORLD);
	}

	public static final String REST_OF_THE_WORLD = "Rest of the world";

	/**
	 * @param pkg
	 *            The fully qualified name of a package
	 * @return
	 */
	public static String mapToModule(String pkg, String path) {
		return (String) JSureProperties.matchingPattern(modulePatterns, pkg, path, REST_OF_THE_WORLD);
	}	
}
