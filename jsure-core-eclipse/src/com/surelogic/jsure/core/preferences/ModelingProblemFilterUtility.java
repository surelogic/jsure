package com.surelogic.jsure.core.preferences;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.Utility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;

@Utility
public final class ModelingProblemFilterUtility {

	/**
	 * The default filters: a series of regular expressions separated by
	 * newlines ('\n').
	 */
	public static final List<String> DEFAULT = Arrays.asList("com\\.apple.*",
			"com\\.oracle.*", "com\\.sun.*", ".*\\.internal.*",
			".*BakedArrayList\\.class", "oracle.*", "quicktime.*", "sun.*");

	public static final AtomicReference<List<String>> CACHE = new AtomicReference<List<String>>();

	public static void setPreference(final List<String> value) {
		if (value == null)
			throw new IllegalArgumentException(I18N.err(44, "value"));
		EclipseUtility.setStringListPreference(
				IDEPreferences.MODELING_PROBLEM_FILTERS, value);
		updateCache();
	}

	public static List<String> getPreference() {
		return EclipseUtility
				.getStringListPreference(IDEPreferences.MODELING_PROBLEM_FILTERS);
	}

	private static void updateCache() {
		CACHE.set(getPreference());
	}

	private static List<String> getCache() {
		List<String> result = CACHE.get();
		if (result == null)
			updateCache();
		result = CACHE.get(); // try again
		return result;
	}

	public static boolean showResource(final String name) {
		if (name == null)
			return false;
		List<String> filters = getCache();
		for (String regex : filters) {
			if (name.matches(regex))
				return false; // filter this resource out
		}
		return true; // show this resource
	}

	public static SeaSummary.Filter defaultFilter = new SeaSummary.Filter() {
		@Override
		public boolean showResource(String path) {
			return ModelingProblemFilterUtility.showResource(path);
		}

		@Override
		public boolean showResource(IDropInfo d) {
			ISrcRef ref = d.getSrcRef();
			if (ref != null) {
				return showResource(ref.getRelativePath());
			}
			return false;
		}
	};
}
