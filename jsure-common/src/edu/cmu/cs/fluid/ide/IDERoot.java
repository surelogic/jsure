package edu.cmu.cs.fluid.ide;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.surelogic.analysis.AnalysisDefaults;
import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.XUtil;

import edu.cmu.cs.fluid.util.IntegerTable;

public abstract class IDERoot {
	public static final String JAVAC_PROPS = "javac.properties";	
	public static final boolean useJavac = true;
	
	protected IDERoot() {
		// Nothing to do
	}

	private static IDERoot instance;
	
	public static synchronized IDERoot getInstance() {
		return instance;
	}

	protected static synchronized void initInstance(IDERoot i) {
		if (instance != null) {
			System.err.println("Warning: ignoring "+i);
			return;
		}
		if (/*instance != null ||*/ i == null) {
			throw new IllegalArgumentException();
		}
		instance = i;
	}
	
	public abstract URL getResourceRoot();
	
	private final ConcurrentMap<String, Object> prefs = new ConcurrentHashMap<String, Object>();
	{
		setPreference(IDEPreferences.DEFAULT_JRE, "");
		initPrefs();
	}
	
	protected void initPrefs() {
		setPreference(IDEPreferences.ANALYSIS_THREAD_COUNT, Runtime
				.getRuntime().availableProcessors());
		for (IAnalysisInfo analysis : AnalysisDefaults.getDefault().getAnalysisInfo()) {
			setPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX+analysis.getUniqueIdentifier(), 
					analysis.isProduction());
		}
	}

	/**
	 * Looks up a boolean preference.
	 * 
	 * @param key
	 *            the key for the desired preference.
	 * @return the value.
	 */
	public boolean getBooleanPreference(String key) {
		final Boolean val = (Boolean) prefs.get(key);
		return val == null ? false : val;
	}

	/**
	 * Looks up a int preference.
	 * 
	 * @param key
	 *            the key for the desired preference.
	 * @return the value.
	 */
	public int getIntPreference(String key) {
		final Integer val = (Integer) prefs.get(key);
		return val == null ? 0 : val;
	}

	/**
	 * Looks up a string preference.
	 * 
	 * @param key
	 *            the key for the desired preference.
	 * @return the value.
	 */
	public String getStringPreference(String key) {
		return (String) prefs.get(key);
	}

	public void setPreference(String key, boolean value) {
		// System.out.println("Setting "+key+" to "+value);
		if (XUtil.testing) {
			System.out.println(this.getClass().getSimpleName() + " set " + key
					+ " to " + (value ? "true" : "false"));
		}
		prefs.put(key, value);
	}

	public void setPreference(String key, Object value) {
		if (XUtil.testing) {
			System.out.println(this.getClass().getSimpleName() + " set " + key
					+ " to " + value);
		}
		prefs.put(key, value);
	}
	
	public synchronized void savePreferences(File runDir) throws IOException {
		final PrintWriter pw = new PrintWriter(new File(runDir, JAVAC_PROPS));
		try {
			for (Map.Entry<String, Object> e : prefs.entrySet()) {
				pw.println(e.getKey() + "=" + e.getValue().toString().replace("\\", "\\\\"));
			}
		} finally {
			pw.close();
		}
	}

	public synchronized void loadPreferences(File runDir) throws IOException {
		File f = new File(runDir, JAVAC_PROPS);
		if (!f.isFile()) {
			return; // No file to read
		}
		Properties p = new Properties();
		Reader r = new FileReader(f);
		try {
			p.load(r);
			prefs.clear();
			for (Map.Entry<Object, Object> e : p.entrySet()) {
				String key = e.getKey().toString();
				String val = e.getValue().toString();
				System.out.println("Loading "+key+" = "+val);
				
				// Check if boolean
				if ("true".equals(val) || "false".equals(val)) {
					prefs.put(key, Boolean.parseBoolean(val));
				} else {
					// Check if integer
					try {
						int i = Integer.parseInt(val);
						prefs.put(key, IntegerTable.newInteger(i));
					} catch (NumberFormatException ex) {
						// Otherwise use as String
						prefs.put(key, val);
					}
				}
			}
		} finally {
			r.close();
		}
	}
}
