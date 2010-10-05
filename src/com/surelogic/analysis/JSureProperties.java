/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.annotation.rules.ModuleRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.util.*;

public class JSureProperties {
	private static final Logger LOG = SLLogger.getLogger("analysis.JSureProperties");
	
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
			return EmptyIterator.prototype();
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
			return EmptyIterator.prototype();
		}
		final StringTokenizer st = new StringTokenizer(prop, ",");
		if (!st.hasMoreTokens()) {
			return EmptyIterator.prototype();
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
}
