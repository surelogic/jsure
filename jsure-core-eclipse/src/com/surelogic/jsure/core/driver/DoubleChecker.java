package com.surelogic.jsure.core.driver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.osgi.framework.BundleContext;

import com.surelogic.analysis.AnalysisDefaults;
import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.Activator;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ide.IDERoot;
import edu.cmu.cs.fluid.CommonStrings;

/**
 * The Eclipse plugin class for double-checker. This plugin drives the Eclipse
 * <i>builder</i> that drives assurance analysis. The class also provides a
 * facade to the Eclipse error reporting facilities.
 */
public class DoubleChecker implements IAnalysisContainer {

	public static final boolean testing = XUtil.testing;

	// //////////////////////////////////////////////////////////////////////
	//
	// PLUGIN FIELDS AND CONSTANTS
	//
	// //////////////////////////////////////////////////////////////////////

	/**
	 * The double-checker analysis module extension point identifier <i>must</i>
	 * match the plugin manifest.
	 */
	public static final String ANALYSIS_MODULE_EXTENSION_POINT_ID = "analysisModule";

	/**
	 * The preference prefix for whether an analysis is on
	 */
	public static final String ANALYSIS_ACTIVE_PREFIX = IDEPreferences.ANALYSIS_ACTIVE_PREFIX;

	/**
	 * The {@link Logger}for this class (named <code>edu.cmu.cs.fluid.dc</code>
	 * ).
	 */
	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	/**
	 * The list of <i>all</i> registered analysis module extensions reflecting
	 * what was read from the plugin manifest. Extensions are not in any special
	 * order.
	 */
	IAnalysisInfo[] allAnalysisExtensions;

	Map<String, IAnalysisInfo> idToInfoMap;

	/**
	 * The list of included (by the user) analysis module extensions. All
	 * elements of this list must be interned. Extensions are not in any special
	 * order. This list is updated by the preference dialog for the plugin
	 * {@link PreferencePage}.
	 */
	final Set<String> m_includedExtensions = new HashSet<String>();

	/**
	 * The list of non-production registered analysis modules. This is a subset
	 * of {@link #allAnalysisExtensions}. Extensions are not in any special
	 * order.
	 */
	Set<IAnalysisInfo> m_nonProductionAnalysisExtensions = new HashSet<IAnalysisInfo>();

	/**
	 * The list of non-excluded registered analysis modules. This is a subset of
	 * 
	 * {@link #allAnalysisExtensions}. Extensions are not in any special order.
	 */
	IAnalysisInfo[] analysisExtensions;

	/**
	 * The list of analysis levels containing sets of analysis module extensions
	 * at each level. This List is built by {@link #initializeAnalysisLevels}.
	 */
	final List<Set<IAnalysisInfo>> m_analysisExtensionSets = new ArrayList<Set<IAnalysisInfo>>();

	/**
	 * Returns the shared double-checker plugin instance to invoke plugin
	 * methods.
	 * 
	 * @return the shared double-checker plugin instance
	 */
	public static DoubleChecker getDefault() {
		return Activator.getDefault().getDoubleChecker();
	}

	/**
	 * Returns the workspace instance.
	 * 
	 * @return the workspace instance associated with the double-checker plugin
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Constructor for the double-checker plugin. This constructor is intended
	 * to be invoked only by the Eclipse platform core.
	 */
	public DoubleChecker() {
		super();
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("double-checker plugin constructed");
		}
	}

	/**
	 * Invoked at plugin startup.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		readAnalysisModuleExtensionPoints();
		analysisExtensionPointsExcludeNonProduction();
		initAnalysisDefaults();
		readStateFromPrefs();
		ensureAllIncludedPrereqsAreIncluded();

		if (analysisExtensionPointsPrerequisitesOK()) {
			initializeAnalysisLevels();
		}
	}

	private void initAnalysisDefaults() {
		for (IAnalysisInfo ext : allAnalysisExtensions) {
			final String id = ext.getUniqueIdentifier();
			final boolean active = !m_nonProductionAnalysisExtensions.contains(ext);
			//System.out.println("Re-defaulting "+id+" to "+active);
			EclipseUtility.setDefaultBooleanPreference(ANALYSIS_ACTIVE_PREFIX
					+ id, active);
		}
	}

	/**
	 * Invokes the garbage collector and calculates roughly the amount of memory
	 * left after garbage collection is done.
	 * 
	 * @return free memory measured in bytes
	 */
	public static long memoryUsed() {
		if (System.getProperty("fluid.gc") != null) {
			System.gc();
		}
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}

	// //////////////////////////////////////////////////////////////////////
	//
	// PLUGIN PERSISTENT STATE METHODS
	//
	// //////////////////////////////////////////////////////////////////////

	private boolean isActive(String id) {
		return EclipseUtility.getBooleanPreference(ANALYSIS_ACTIVE_PREFIX + id);
	}

	/**
	 * Read persistent double-checker plugin information. Invoked from
	 * {@link #startup}.
	 * 
	 * @see #writeStateToPrefs()
	 */
	private void readStateFromPrefs() {
		m_includedExtensions.clear();

		for (IAnalysisInfo ext : allAnalysisExtensions) {
			final String id = ext.getUniqueIdentifier();
			final boolean active = isActive(id);
			if (active) {
				// System.out.println("Really Included : "+id);
				m_includedExtensions.add(CommonStrings.intern(id));

				if (allAnalysisExtensions != null) {
					ensureAnalysisPrereqsAreIncluded(id);
				}
			} else {
				// System.out.println("Really Excluded : "+id);
			}
		}
	}

	private void ensureAllIncludedPrereqsAreIncluded() {
		for (String id : new ArrayList<String>(m_includedExtensions)) {
			ensureAnalysisPrereqsAreIncluded(id);
		}
	}

	private void ensureAnalysisPrereqsAreIncluded(String id) {
		// Make sure prerequisites are included
		IAnalysisInfo e = getAnalysisModuleExtensionPoint(id);
		Set<String> s = getPrerequisiteAnalysisIdSet(e);
		for (String prereq : s) {
			if (m_includedExtensions.contains(prereq)) {
				continue;
			}
			// System.out.println("Included "+prereq+" because of "+id);
			ensureAnalysisPrereqsAreIncluded(prereq);
		}
		m_includedExtensions.addAll(s);
	}

	/**
	 * Only used for regression testing
	 */
	public void initAnalyses() {
		readStateFromPrefs();
		if (analysisExtensionPointsPrerequisitesOK()) {
			initializeAnalysisLevels();
		}
	}

	/**
	 * Saves persistent double-checker plugin information. Invoked as part of
	 * the save process within {@link SaveParticipant}.
	 * 
	 * @see #readStateFromPrefs()
	 */
	void writeStateToPrefs() {
		for (IAnalysisInfo ext : allAnalysisExtensions) {
			final String id = ext.getUniqueIdentifier();
			EclipseUtility.setBooleanPreference(ANALYSIS_ACTIVE_PREFIX + id,
					m_includedExtensions.contains(id));
		}
	}

	public void writePrefsToXML(File settings) throws FileNotFoundException {
		final PrintWriter pw = new PrintWriter(settings);
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.println("<preferences>");
		pw.println("  <included-analysis-modules>");
		for (String id : m_includedExtensions) {
			pw.println("    <id>" + id + "</id>");
		}
		pw.println("  </included-analysis-modules>");
		pw.println("  <excluded-analysis-modules>");
		for (IAnalysisInfo ext : m_nonProductionAnalysisExtensions) {
			final String id = ext.getUniqueIdentifier();
			pw.println("    <id>" + id + "</id>");
		}
		pw.println("  </excluded-analysis-modules>");
		pw.println("</preferences>");
		pw.close();
	}

	// //////////////////////////////////////////////////////////////////////
	//
	// ANALYSIS MODULE EXTENSION POINT METHODS
	//
	// //////////////////////////////////////////////////////////////////////

	/**
	 * Reads into the field {@link #allAnalysisExtensions}all defined analysis
	 * module extension points defined in the plugin manifest.
	 * 
	 * @see #allAnalysisExtensions
	 */
	private void readAnalysisModuleExtensionPoints() {
		/*
		IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = pluginRegistry.getExtensionPoint(
				Plugin.DOUBLE_CHECKER_PLUGIN_ID,
				Plugin.ANALYSIS_MODULE_EXTENSION_POINT_ID);
		allAnalysisExtensions = extensionPoint.getExtensions();
		*/
		Collection<? extends IAnalysisInfo> temp = AnalysisDefaults.getDefault().getAnalysisInfo();
		allAnalysisExtensions = new IAnalysisInfo[temp.size()+1];
		allAnalysisExtensions[0] = new IAnalysisInfo() {			
			@Override
			public boolean isProduction() {
				return false;
			}			
			@Override
			public boolean isIncluded() {
				return false;
			}			
			@Override
			public String getUniqueIdentifier() {
				return "com.surelogic.jsure.client.eclipse.AnalysisDriver";
			}			
			@Override
			public String[] getPrerequisiteIds() {
				return SLUtility.EMPTY_STRING_ARRAY;
			}			
			@Override
			public String getLabel() {
				return "Analysis Driver";
			}			
			@Override
			public String getCategory() {
				return null;
			}			
			@Override
			public String getAnalysisClassName() {
				return AnalysisDriver.class.getName();
			}
			@Override
			public boolean runsUniqueness() {
				return false;
			}
			@Override
			public boolean isActive(List<IAnalysisInfo> activeAnalyses) {
				return false;
			}
		};
		int i=1;
		for(IAnalysisInfo ai : temp) {
			allAnalysisExtensions[i] = ai;
			i++;
		}
		idToInfoMap = convertFromExtensions(allAnalysisExtensions);
	}

	private Map<String, IAnalysisInfo> convertFromExtensions(IAnalysisInfo[] all) {
		Map<String, IAnalysisInfo> map = new HashMap<String, IAnalysisInfo>();
		for (IAnalysisInfo info : all) {
			map.put(info.getUniqueIdentifier(), info);
		}
		return map;
	}

	/*
	private IAnalysisInfo createAnalysisInfo(IExtension am) {
		IConfigurationElement[] cfgs = am.getConfigurationElements();
		for (int i = 0; i < cfgs.length; i++) {
			if (cfgs[i].getName().equalsIgnoreCase("run")) {
				final String production = cfgs[i].getAttribute("production");
				final boolean isProduction = production == null
						|| !production.equals("false");
				final String category = cfgs[i].getAttribute("category");
				return new AnalysisInfo(am) {
					@Override
					public boolean isProduction() {
						return isProduction;
					}

					@Override
					public String getCategory() {
						return category;
					}
				};
			}
		}
		return new AnalysisInfo(am);
	}

	class AnalysisInfo implements IAnalysisInfo {
		final IExtension ext;

		AnalysisInfo(IExtension e) {
			ext = e;
		}

		public boolean isProduction() {
			return true;
		}

		public boolean isIncluded() {
			return m_includedExtensions.contains(ext.getUniqueIdentifier());
		}

		public String getUniqueIdentifier() {
			return ext.getUniqueIdentifier();
		}

		public String getLabel() {
			return ext.getLabel();
		}

		public String getCategory() {
			return null;
		}
	}
    */

	private IAnalysisInfo getAnalysisInfo(String id) {
		return idToInfoMap.get(id);
	}

	@Override
	public Iterable<IAnalysisInfo> getAllAnalysisInfo() {
		return idToInfoMap.values();
	}

	/**
	 * Builds an array of all all analysis extension points that are marked in
	 * the XML as being non-production (i.e., production="false"). Adds each
	 * non-production unique identifier into the (probably empty) String array
	 * <code>m_excludedExtensions</code> field.
	 */
	private void analysisExtensionPointsExcludeNonProduction() {
		m_nonProductionAnalysisExtensions.clear();
		for (IAnalysisInfo info : getAllAnalysisInfo()) {
			if (!info.isProduction()) {
				m_nonProductionAnalysisExtensions.add(info);
			}
		}
	}

	/**
	 * Checks that all prerequisites listed in every analysis module are known.
	 * 
	 * @return <code>true</code> if everything is OK, <code>false</code>
	 *         otherwise
	 * 
	 * @see #allAnalysisExtensions
	 */
	private boolean analysisExtensionPointsPrerequisitesOK() {
		boolean result = true; // assume OK
		// build up a set of known analysis module identifiers filtering out
		// those that the user has specifically excluded
		Set<String> ids = new HashSet<String>(); // analysis module ids
		Set<IAnalysisInfo> ams = new HashSet<IAnalysisInfo>();
		for (int i = 0; i < allAnalysisExtensions.length; ++i) {
			if (isExtensionIncluded(allAnalysisExtensions[i])) {
				// user preference exclude
				ids.add(CommonStrings.intern(allAnalysisExtensions[i]
						.getUniqueIdentifier()));
				ams.add(allAnalysisExtensions[i]);
			}
		}
		// check that all prerequisites are in that list
		for (IAnalysisInfo analysisModule : ams) {
			for(String prereq : analysisModule.getPrerequisiteIds()) {
				if (prereq == null) {
					continue;
				}
				if (!ids.contains(CommonStrings.intern(prereq))) {
					String logMessage = "The identified prerequisite \""
						+ prereq
						+ "\" for \""
						+ analysisModule.getLabel()
						+ "\" (id = \""
						+ analysisModule.getUniqueIdentifier()
						+ "\") does not reference any known analysis module";
					/* TODO this used to put up a dialog
					String title = "Unknown Prerequisite";
					String dialogMessage = "Unknown analysis module \""
						+ prereq
						+ "\" given as a prerequisite below:\n"
						+ analysisModuleInfo();
						*/
					SLLogger.getLogger().severe(logMessage);		
					result = false; // found a problem
				}
			}
		}
		return result;
	}

	/**
	 * Determines, based upon the plugin manifests that define analysis module
	 * extension points, the composition of the analysis levels. Analysis
	 * modules that can be executed during the same pass through project
	 * resources are grouped together to minimize the number of passes.
	 * 
	 * @see #allAnalysisExtensions
	 */
	private void initializeAnalysisLevels() {
		m_analysisExtensionSets.clear(); // start with an empty list

		if (IDERoot.useJavac) {
			// Just run AnalysisDriver
			for (IAnalysisInfo ext : allAnalysisExtensions) {
				if (AnalysisDriver.ID.equals(ext.getUniqueIdentifier())) {
					m_analysisExtensionSets.add(Collections.singleton(ext));
					analysisExtensions = new IAnalysisInfo[1];
					analysisExtensions[0] = ext;
					SLLogger.getLogger().fine("Found " + ext.getUniqueIdentifier());
					return;
				}
			}
		}

		// construct the analysis levels based upon the prerequisites provided
		// in the plugin manifest (held in the analysisExtensions field)
		// filtering
		// out those that the user has specifically excluded
		int bailOutLevel = 0;
		final Set<IAnalysisInfo> remainingAnalyses = new HashSet<IAnalysisInfo>();
		for (int i = 0; i < allAnalysisExtensions.length; i++) {
			if (isExtensionIncluded(allAnalysisExtensions[i])) {
				// user preferences exclude
				remainingAnalyses.add(allAnalysisExtensions[i]);
			}
		}
		// remainingAnalyses is the set of all non-excluded analysis modules so
		// use it to set the field analysisExtensions
		analysisExtensions = remainingAnalyses
				.toArray(new IAnalysisInfo[remainingAnalyses.size()]);
		final Set<String> lowerLevels = new HashSet<String>(); // of analysis
		// ids
		Set<IAnalysisInfo> thisLevel = new HashSet<IAnalysisInfo>();
		while (remainingAnalyses.size() > 0) {
			if (bailOutLevel++ > 25) {
				String logMessage = "Bailed out after 25 levels trying to order analysis modules"
						+ "...probable loop in analysis module dependencies";
				/* TODO this used to put up a dialog
				String title = "Analysis Module Prerequisite Problem";
				String dialogMessage = "Unable to order analysis modules:\n"
						+ analysisModuleInfo();
						*/
				SLLogger.getLogger().severe(logMessage);
				m_analysisExtensionSets.clear(); // zero out
				return;
			}
			for (IAnalysisInfo cur : remainingAnalyses) {
				Set<String> curPrereq = getPrerequisiteAnalysisIdSet(cur);
				if (curPrereq.size() == 0 || lowerLevels.containsAll(curPrereq)) {
					thisLevel.add(cur);
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("analysis module \"" + cur.getLabel()
								+ "\" added to level "
								+ m_analysisExtensionSets.size());
					}
				}
			}
			m_analysisExtensionSets.add(thisLevel);
			remainingAnalyses.removeAll(thisLevel);
			for (IAnalysisInfo cur : thisLevel) {
				lowerLevels
						.add(CommonStrings.intern(cur.getUniqueIdentifier()));
			}
			thisLevel = new HashSet<IAnalysisInfo>();
		}
	}

	/**
	 * Creates and returns the set of prerequisite analysis modules for a
	 * specific analysis module. The set contains the identifiers as
	 * {@link String}values that have been intern'ed.
	 * 
	 * @param analysisExtension
	 *            the analysis module extension information
	 * @return a {@link Set}containing all prerequisite analysis module
	 *         identifiers from the plugin manifest
	 */
	private Set<String> getPrerequisiteAnalysisIdSet(
			IAnalysisInfo analysisExtension) {
		if (analysisExtension == null) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<String>();
		for(String prereq : analysisExtension.getPrerequisiteIds()) {
			result.add(prereq);
		}
		return result;
	}

	/**
	 * Looks up the list of {@link IExtension}s that are direct prerequisites
	 * for the provided analysis module extension point.
	 * 
	 * @param analysisExtension
	 *            the analysis module extension point
	 * @return the set of analysis module extension points that are
	 *         prerequisites for <code>analysisExtension</code>
	 */
	@Override
	public Set<IAnalysisInfo> getPrerequisiteAnalysisExtensionPoints(
			IAnalysisInfo info) {
		Set<IAnalysisInfo> result = new HashSet<IAnalysisInfo>();
		IAnalysisInfo ext = getAnalysisModuleExtensionPoint(info
				.getUniqueIdentifier());
		Set<String> ids = getPrerequisiteAnalysisIdSet(ext);
		for (String id : ids) {
			IAnalysisInfo ext2 = getAnalysisInfo(id);
			if (ext2 == null) {
				// System.out.println("null");
				continue;
			}
			result.add(ext2);
		}
		return result;
	}

	/**
	 * Looks up the {@link IExtension}given an analysis module extension point
	 * identifier.
	 * 
	 * @param id
	 *            the analysis module extension point identifier to lookup
	 * @return the {@link IExtension}for <code>id</code>, or <code>null</code>
	 *         if <code>id</code> does not exist as an analysis module extension
	 *         point
	 */
	private IAnalysisInfo getAnalysisModuleExtensionPoint(String id) {
		for (int i = 0; i < allAnalysisExtensions.length; ++i) {
			if (allAnalysisExtensions[i].getUniqueIdentifier().equals(id)) {
				return allAnalysisExtensions[i];
			}
		}
		return null;
	}

	/**
	 * A string list of analysis modules and prerequisites suitable for use in
	 * error messages or debugging.
	 * 
	 * @return all analysis module extension points and defined prerequisites
	 * 
	 * @see #allAnalysisExtensions
	 */
	String analysisModuleInfo() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < allAnalysisExtensions.length; ++i) {
			result.append(allAnalysisExtensions[i].getLabel() + " ("
					+ allAnalysisExtensions[i].getUniqueIdentifier() + ")\n");
			for(String prereq : allAnalysisExtensions[i].getPrerequisiteIds()) {
				result.append("+ prerequisite: \"" + prereq + "\"\n");

			}
		}
		return result.toString();
	}

	// //////////////////////////////////////////////////////////////////////
	//
	// INCLUDING ANALYSIS MODULE EXTENSION POINT METHODS
	//
	// //////////////////////////////////////////////////////////////////////

	/**
	 * Checks if the given analysis module extension point is currently included
	 * by a user preference setting.
	 * 
	 * @param analysisModule
	 *            the analysis module extension point identifier to check for
	 * @return <code>true</code> if <code>analysisModule</code> exists in the
	 *         plugin's list of included analysis module extension points,
	 *         <code>false</code> otherwise
	 */
	boolean isExtensionIncluded(IAnalysisInfo analysisModule) {
		return isExtensionIncluded(analysisModule.getUniqueIdentifier());
	}

	/**
	 * Updates {@link #m_includedExtensions}to the values passed into this
	 * method. {@link #m_includedExtensions}is only updated if it is different.
	 * 
	 * @param includedExtensions
	 *            the set of included extension identifiers (all must be
	 *            interned)
	 */
	@Override
	public void updateIncludedExtensions(Set<String> includedExtensions) {
		// Have we really changed anything?
		if (isIncludedExtensionsChanged(includedExtensions)) {
			m_includedExtensions.clear();
			m_includedExtensions.addAll(includedExtensions);
			writeStateToPrefs();

			if (analysisExtensionPointsPrerequisitesOK()) {
				initializeAnalysisLevels();
			}
		}
	}

	/**
	 * Checks if the list of analysis module extension points passed into the
	 * method is different than the list the plugin currently has in the
	 * {@link #m_includedExtensions}field.
	 * 
	 * @param includedExtensions
	 *            the list of inclusions to compare with the plugin state (all
	 *            elements must be interned)
	 * @return <code>true</code> if the parameter is different than the current
	 *         plugin state, <code>false</code> if they are the same
	 */
	@Override
	public boolean isIncludedExtensionsChanged(Set<String> includedExtensions) {
		return !m_includedExtensions.equals(includedExtensions);
	}

	/**
	 * Checks if a single analysis module extension point identifier is included
	 * within the list of included identifiers in the
	 * {@link #m_includedExtensions} field.
	 * 
	 * @param id
	 *            the analysis module extension point identifier to check for
	 * @return <code>true</code> if <code>id</code> exists in the plugin's list
	 *         of included analysis module extension points, <code>false</code>
	 *         otherwise
	 */
	private boolean isExtensionIncluded(String id) {
		return m_includedExtensions.contains(id);
	}

	public Iterable<String> getIncludedExtensions() {
		return m_includedExtensions;
	}
}