package edu.cmu.cs.fluid.dc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.osgi.framework.BundleContext;

import com.surelogic.analysis.IIRAnalysis;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.Activator;

import edu.cmu.cs.fluid.analysis.util.WholeAnalysisModule;

/**
 * The Eclipse plugin class for double-checker. This plugin drives the Eclipse
 * <i>builder</i> that drives assurance analysis. The class also provides a
 * facade to the Eclipse error reporting facilities.
 */
public class Plugin {

	public static final boolean testing = System.getProperty("dc.testing",
			"false").equals("true");

	// //////////////////////////////////////////////////////////////////////
	//
	// XML ELEMENT CONSTANTS
	//
	// //////////////////////////////////////////////////////////////////////

	private static final String SF_PREFS = "preferences";

	private static final String SF_EXCLUDED_ANALYSIS_MODULES = "excluded-analysis-modules";

	private static final String SF_INCLUDED_ANALYSIS_MODULES = "included-analysis-modules";

	private static final String SF_ID = "id";

	// //////////////////////////////////////////////////////////////////////
	//
	// PLUGIN FIELDS AND CONSTANTS
	//
	// //////////////////////////////////////////////////////////////////////

	/**
	 * The double-checker plugin identifier <i>must</i> match the plugin
	 * manifest.
	 */
	public static final String DOUBLE_CHECKER_PLUGIN_ID = "com.surelogic.jsure.client.eclipse";

	/**
	 * The double-checker analysis module extension point identifier <i>must</i>
	 * match the plugin manifest.
	 */
	public static final String ANALYSIS_MODULE_EXTENSION_POINT_ID = "analysisModule";

	/**
	 * The {@link Logger}for this class (named <code>edu.cmu.cs.fluid.dc</code>).
	 */
	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	/**
	 * The list of <i>all</i> registered analysis module extensions reflecting
	 * what was read from the plugin manifest. Extensions are not in any special
	 * order.
	 */
	IExtension[] allAnalysisExtensions;

	/**
	 * Flags if a valid preference file for the plugin was found at startup. If
	 * not then we want to exclude all analysis module extensions which are
	 * noted to be production="false" in the extension point XML definition.
	 */
	boolean m_validPluginStateFile = true;

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
	Set<IExtension> m_nonProductionAnalysisExtensions = new HashSet<IExtension>();

	/**
	 * The list of non-excluded registered analysis modules. This is a subset of
	 * {@link #allAnalysisExtensions}. Extensions are not in any special order.
	 */
	IExtension[] analysisExtensions;

	/**
	 * The list of analysis levels containing sets of analysis module extensions
	 * at each level. This List is built by {@link #initializeAnalysisLevels}.
	 */
	final List<Set<IExtension>> m_analysisExtensionSets = new ArrayList<Set<IExtension>>();

	/**
	 * Cache managed by {@link #getAnalysisModule}to ensure that obtaining the
	 * {@link IAnalysis}object defined by the analysis module extension point
	 * is only done a single time (i.e., the analysis modules are managed as
	 * singleton objects).
	 */
	Map<IExtension, IAnalysis> m_analysisModuleCache = new HashMap<IExtension, IAnalysis>();

	/**
	 * Whether we should build only when the user says so, or when Eclipse says
	 * to do so
	 */
	boolean buildManually = false;

	/**
	 * Returns the shared double-checker plugin instance to invoke plugin
	 * methods.
	 * 
	 * @return the shared double-checker plugin instance
	 */
	public static Plugin getDefault() {
		return Activator.getDefault().getDoubleChecker();
	}

	/**
	 * Returns a image descriptor from the "icons/" directory within this
	 * plugin.
	 * 
	 * @param fileName
	 *            the name of the image file (e.g., <code>image.gif</code>)
	 * @return a image descriptor from the <code>icons/</code> directory
	 *         within this plugin
	 */
	public ImageDescriptor getImageDescriptor(String fileName) {
		String iconPath = "icons/"; // relative to the plugin location
		try {
			URL installURL = Activator.getDefault().getBundle().getEntry("/");
			URL url = new URL(installURL, iconPath + fileName);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	/**
	 * Returns the workspace instance.
	 * 
	 * @return the workspace instance associated with the double-checker plugin
	 */
	public static IWorkspace getWorkspace() {
		return Activator.getWorkspace();
	}

	/**
	 * Constructor for the double-checker plugin. This constructor is intended
	 * to be invoked only by the Eclipse platform core.
	 */
	public Plugin() {
		super();
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("double-checker plugin constructed");
		}
	}

	/**
	 * Because JSure does not persist its result we use this method to invoke an
	 * AUTO_BUILD on each open project in the workspace that has JSure enabled
	 * for it.
	 */
	public void autoBuildJSureProjects() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("autoBuildJSureProjects() called");
		}
		IProject[] projects = getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			final IProject p = projects[i];
			if (p.isOpen() && Nature.hasNature(p)) {
				Activator.getDefault().getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						refreshProjectAndScheduleInitialAnalysis(p);
					}
				});
			}
		}
	}

	/**
	 * Invoke an AUTO_BUILD on the project provided using a created
	 * ProgressMonitorDialog to track progress.
	 * 
	 * @param project
	 *            the project to build
	 */
	private void refreshProjectAndScheduleInitialAnalysis(final IProject project) {
		if (testing) {
			// System.out.println("Skipping first time refresh/analysis while
			// testing");
			return;
		}
		/*
		 * First we refresh the project as a workaround to oddball Eclipse
		 * behaviour. This need came up in Nov 2006 when Edwin was packaging up
		 * special workspaces for demos by Bill. Sometimes when these workspaces
		 * were moved from computer to computer (with slightly different JDK
		 * minor version numbers, Windows configurations, etc.) a refresh
		 * appears to have been needed which was not detected by Eclipse.
		 */
		new FirstTimeRefresh(project).schedule();

		/*
		 * Schedule Eclipse to run our initial analysis (an AUTO_BUILD of
		 * project). We need this because we don't (currently) persist our
		 * results. Note that this job should run after the one above (I belive
		 * this is what is occurring in Eclipse).
		 */
		new FirstTimeAnalysis(project).schedule();
	}

	/**
	 * Invoked at plugin startup.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		ISaveParticipant saveParticipant = new SaveParticipant();
		ISavedState lastState = 
			ResourcesPlugin.getWorkspace().addSaveParticipant(Activator.getDefault(), 
					                                          saveParticipant);
		if (lastState != null) {
			IPath location = lastState.lookup(new Path("save"));
			if (location != null) {
				// the plugin instance should read any important state from the
				// file.
				File f = Activator.getDefault().getStateLocation().append(location).toFile();
				readStateFrom(f);
			} else {
				invalidatePluginState();
			}
		} else {
			invalidatePluginState();
		}
		if (m_validPluginStateFile) {
			if (LOG.isLoggable(Level.FINE))
				LOG
						.fine("double-checker read saved XML analysis configuration");
		} else {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("double-checker was unable to load saved analysis"
						+ "configuration from XML");
		}
		readAnalysisModuleExtensionPoints();
		analysisExtensionPointsExcludeNonProduction();
		if (analysisExtensionPointsPrerequisitesOK()) {
			initializeAnalysisLevels();
		}

		autoBuildJSureProjects();
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

	/**
	 * Read persistent double-checker plugin information from
	 * <code>target</code>. Invoked from {@link #startup}.
	 * 
	 * @param target
	 *            {@link File}to read from
	 * 
	 * @see #writeStateTo(File)
	 */
	private void readStateFrom(File target) {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Read state from the file " + target.getAbsolutePath());
		}
		SAXBuilder parser = new SAXBuilder();
		try {
			Document pluginSaveInformation = parser.build(target);
			Element root = pluginSaveInformation.getRootElement();
			List<String> includedAnalysisModules = getList(root, SF_PREFS,
					SF_INCLUDED_ANALYSIS_MODULES, SF_ID);
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("analysis module inclusion list "
						+ includedAnalysisModules);
			for (String id : includedAnalysisModules) {
				m_includedExtensions.add(id.intern());
				// System.out.println("Included "+id);

				if (allAnalysisExtensions != null) {
					ensureAnalysisPrereqsAreIncluded(id);
				}
			}

			List<String> excludedAnalysisModules = getList(root, SF_PREFS,
					SF_EXCLUDED_ANALYSIS_MODULES, SF_ID);
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("analysis module exclusion list "
						+ excludedAnalysisModules);
			for (String id : excludedAnalysisModules) {
				if (includedAnalysisModules.contains(id)) {
					LOG.warning("Both included and excluded: " + id);
				}
				m_includedExtensions.remove(id.intern());
				// System.out.println("Excluded "+id);
			}
		} catch (JDOMException e) {
			elog(null, IStatus.ERROR, "failure XML parsing plugin state from "
					+ target.getAbsolutePath(), e);
			invalidatePluginState();
		} catch (IOException e) {
			elog(
					null,
					IStatus.WARNING,
					"failure reading plugin state from "
							+ target.getAbsolutePath()
							+ " perhaps because this is the first invocation of double-checker",
					e);
			invalidatePluginState();
		}
	}

	private void ensureAllIncludedPrereqsAreIncluded() {
		for (String id : new ArrayList<String>(m_includedExtensions)) {
			ensureAnalysisPrereqsAreIncluded(id);
		}
	}

	private void ensureAnalysisPrereqsAreIncluded(String id) {
		// Make sure prerequisites are included
		IExtension e = getAnalysisModuleExtensionPoint(id);
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

	public void initAnalyses(File analyses) {
		this.readStateFrom(analyses);
		if (analysisExtensionPointsPrerequisitesOK()) {
			initializeAnalysisLevels();
		}
	}

	private void invalidatePluginState() {
		m_validPluginStateFile = false;
		m_includedExtensions.clear();
	}

	/**
	 * XML convenience routine that returns a list of strings found in the XML
	 * tree defined by <code>root</code>. Consider the following XML:
	 * 
	 * <pre>
	 *             &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
	 *             &lt;preferences&gt;
	 *               &lt;excluded-analysis-modules&gt;
	 *                 &lt;id&gt;bogas.id.1&lt;/id&gt;
	 *                 &lt;id&gt;bogas.id.2&lt;/id&gt;
	 *               &lt;/excluded-analysis-modules&gt;
	 *             &lt;/preferences&gt;
	 *             
	 * </pre>
	 * 
	 * A call to
	 * <code>getList("preferences", "excluded-analysis-modules", "id")</code>
	 * would return a list containing the strings "bogas.id.1" and "bogas.id.2".
	 * 
	 * @param root
	 *            the JDOM tree to search
	 * @param category
	 *            name of the XML element the <code>item</code> is within
	 * @param item
	 *            name of the XML element the <code>data</code> is directly
	 *            contained within
	 * @param data
	 *            the element name to return the contents of
	 * @return a list containing {@link String}items containing the text of
	 *         each <code>data</code> element found
	 * 
	 * @see #readStateFrom(File)
	 */
	private List<String> getList(Element root, String category, String item,
			String data) {
		List<String> result = new LinkedList<String>();
		Element categoryElement = findElement(root, category);
		Element itemElement = findElement(categoryElement, item);
		if (itemElement == null) {
			return Collections.emptyList();
		}
		@SuppressWarnings("unchecked")
		List<Element> children = itemElement.getChildren(data);
		for (Iterator<Element> i = children.iterator(); i.hasNext();) {
			Element element = i.next();
			result.add(element.getText());
		}
		return result;
	}

	/**
	 * XML convenience routine that finds the first instance of an
	 * {@link Element} with <code>elementName</code> through a search of the
	 * tree defined by <code>root</code>.
	 * 
	 * @param root
	 *            the JDOM tree to search
	 * @param elementName
	 *            the name to search for an {@link Element}using
	 * @return the element if it is found in <code>root</code>, otherwise
	 *         <code>null</code>
	 * 
	 * @see #readStateFrom(File)
	 */
	private Element findElement(Element root, String elementName) {
		if (root == null) {
			return null;
		}
		if (root.getName().equals(elementName)) {
			return root;
		}
		@SuppressWarnings("unchecked")
		List<Element> children = root.getChildren();
		for (Iterator<Element> i = children.iterator(); i.hasNext();) {
			Element element = i.next();
			Element found = findElement(element, elementName);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Saves persistent double-checker plugin information to <code>target</code>.
	 * Invoked as part of the save process within {@link SaveParticipant}.
	 * 
	 * @param target
	 *            {@link File}to write state into
	 * 
	 * @see #readStateFrom(File)
	 */
	void writeStateTo(File target) {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Write state to the file " + target.getAbsolutePath());
		}
		Element preferences = new Element(SF_PREFS);
		Element includedAnalysisModules = new Element(
				SF_INCLUDED_ANALYSIS_MODULES);
		preferences.addContent(includedAnalysisModules);
		for (String id : m_includedExtensions) {
			Element cur = new Element(SF_ID);
			cur.setText(id);
			includedAnalysisModules.addContent(cur);
		}
		Document pluginSaveInformation = new Document(preferences);
		try {
			// construct plugin save file OutputStream
			OutputStream pluginSaveFile = new BufferedOutputStream(
					new FileOutputStream(target));
			// XML output with two-space indentation and newlines after elements
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			// actually output the XML to our plugin save file
			outputter.output(pluginSaveInformation, pluginSaveFile);
		} catch (IOException e) {
			elog(null, IStatus.ERROR, "failure saving plugin state to "
					+ target.getAbsolutePath(), e);
		}
	}

	// //////////////////////////////////////////////////////////////////////
	//
	// ECLIPSE ERROR REPORTING METHODS (VISIBLE THROUGH THE ECLIPSE UI)
	//
	// //////////////////////////////////////////////////////////////////////

	/**
	 * Sends a log message to the Eclipse error log. This log is visible within
	 * the IDE via the built in <i>PDE Runtime Error Log </i> view. This method
	 * is a wrapper to simplify the Eclipse API for reporting to this log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the log, or <code>null</code> if
	 *            the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * 
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	public void elog(org.eclipse.core.runtime.Plugin from, int severity,
			String logMessage) {
		elog(from, severity, logMessage, null);
	}

	/**
	 * Sends a log message to the Eclipse error log. This log is visible within
	 * the IDE via the built in <i>PDE Runtime Error Log </i> view. This method
	 * is a wrapper to simplify the Eclipse API for reporting to this log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the log, or <code>null</code> if
	 *            the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not
	 *            applicable
	 * 
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	public void elog(org.eclipse.core.runtime.Plugin from, int severity,
			String logMessage, Throwable exception) {
		// build up log information
		if (from == null) {
			from = Activator.getDefault(); // default to this plugin if none was provided
		}
		Status logContent = new Status(severity, from.getBundle()
				.getSymbolicName(), severity, logMessage, exception);
		Activator.getDefault().getLog().log(logContent);
	}

	/**
	 * Displays an error dialog to the user and logs a issue to the Eclipse log.
	 * The error dialog will include detailed plugin information. The logged
	 * message is visible within the IDE via the built in <i>PDE Runtime Error
	 * Log </i> view. This method is a wrapper to simplify the Eclipse API for
	 * reporting to the user and the log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the message, or <code>null</code>
	 *            if the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param dialogTitle
	 *            a human-readable title for the UI dialog
	 * @param dialogMessage
	 *            a human-readable message to describe the issue to the user
	 *            within the dialog
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 */
	public void elogPrompt(org.eclipse.core.runtime.Plugin from, int severity,
			String logMessage, String dialogTitle, String dialogMessage) {
		elogPrompt(from, severity, logMessage, dialogTitle, dialogMessage, null);
	}

	/**
	 * Displays an error dialog to the user and logs a issue to the Eclipse log.
	 * The error dialog will include detailed plugin information and any
	 * exception information provided. The logged message is visible within the
	 * IDE via the built in <i>PDE Runtime Error Log </i> view. This method is a
	 * wrapper to simplify the Eclipse API for reporting to the user and the
	 * log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the message, or <code>null</code>
	 *            if the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param dialogTitle
	 *            a human-readable title for the UI dialog
	 * @param dialogMessage
	 *            a human-readable message to describe the issue to the user
	 *            within the dialog
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not
	 *            applicable
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 */
	public void elogPrompt(final org.eclipse.core.runtime.Plugin from,
			final int severity, final String logMessage,
			final String dialogTitle, final String dialogMessage,
			final Throwable exception) {
		// log the issue
		elog(from, severity, logMessage, exception);
		// need to update our view (if it still exists)
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				// build up dialog information
				org.eclipse.core.runtime.Plugin fromPlugin = from;
				if (fromPlugin == null) {
					fromPlugin = Activator.getDefault();
					// default to this plugin if none was provided
				}
				MultiStatus dialogContent = new MultiStatus(fromPlugin
						.getBundle().getSymbolicName(), severity, logMessage,
						exception);
				addServiceInfo(dialogContent, fromPlugin);
				dialogContent.add(new Status(severity, fromPlugin.getBundle()
						.getSymbolicName(), severity, "Problem: " + logMessage,
						exception));
				// add exception information to the dialog if any exists
				if (exception != null) {
					dialogContent.add(new Status(severity, fromPlugin
							.getBundle().getSymbolicName(), severity, "> "
							+ exception.getClass().getName()
							+ " thrown"
							+ (exception.getMessage() != null ? " ["
									+ exception.getMessage() + "]" : ""),
							exception));
					for (int i = 0; i < exception.getStackTrace().length; i++) {
						dialogContent.add(new Status(severity, fromPlugin
								.getBundle().getSymbolicName(), severity, "> "
								+ exception.getStackTrace()[i], exception));
					}
				}
				// prompt the user
				ErrorDialog.openError((Shell) null, dialogTitle, dialogMessage,
						dialogContent);
			}
		});
	}

	/**
	 * Adds plugin information to a {@link MultiStatus}object for use in an
	 * error dialog. This routine simply provides support the
	 * <code>elogPrompt</code> methods.
	 * 
	 * @param dialogContent
	 *            the
	 * @{link MultiStatus} object to add information to
	 * @param from
	 *            the plugin to extract the information from
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	private void addServiceInfo(MultiStatus dialogContent,
			org.eclipse.core.runtime.Plugin from) {
		@SuppressWarnings("unchecked")
		Dictionary<String, ?> headers = from.getBundle().getHeaders();

		dialogContent
				.add(new Status(
						IStatus.INFO,
						from.getBundle().getSymbolicName(),
						IStatus.INFO,
						"Plug-in Provider: "
								+ headers
										.get(org.osgi.framework.Constants.BUNDLE_VENDOR),
						null));
		dialogContent.add(new Status(IStatus.INFO, from.getBundle()
				.getSymbolicName(), IStatus.INFO, "Plug-in Name: "
				+ headers.get(org.osgi.framework.Constants.BUNDLE_NAME), null));
		dialogContent.add(new Status(IStatus.INFO, from.getBundle()
				.getSymbolicName(), IStatus.INFO, "Plug-in ID: "
				+ from.getBundle().getSymbolicName(), null));
		dialogContent.add(new Status(IStatus.INFO, from.getBundle()
				.getSymbolicName(), IStatus.INFO, "Version: "
				+ headers.get(org.osgi.framework.Constants.BUNDLE_VERSION),
				null));
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
		IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = pluginRegistry.getExtensionPoint(
				Plugin.DOUBLE_CHECKER_PLUGIN_ID,
				Plugin.ANALYSIS_MODULE_EXTENSION_POINT_ID);
		allAnalysisExtensions = extensionPoint.getExtensions();
		ensureAllIncludedPrereqsAreIncluded();
	}

	/**
	 * Builds an array of all all analysis extension points that are marked in
	 * the XML as being non-production (i.e., production="false"). Adds each
	 * non-production unique identifier into the (probably empty) String array
	 * <code>m_excludedExtensions</code> field.
	 */
	private void analysisExtensionPointsExcludeNonProduction() {
		for (int i = 0; i < allAnalysisExtensions.length; i++) {
			String uid = allAnalysisExtensions[i].getUniqueIdentifier();
			IConfigurationElement[] configElements = allAnalysisExtensions[i]
					.getConfigurationElements();
			for (int j = 0; j < configElements.length; j++) {
				String production = configElements[j]
						.getAttribute("production");
				if (production != null && production.equals("false")) {
					if (LOG.isLoggable(Level.FINE))
						LOG.fine("analysis module extension point " + uid
								+ " is not production and is being excluded");
					// add to list of non-production
					m_nonProductionAnalysisExtensions
							.add(allAnalysisExtensions[i]);
				} else if (!m_validPluginStateFile
						&& !m_nonProductionAnalysisExtensions
								.contains(allAnalysisExtensions[i])) {
					// turn it on if no previous settings
					m_includedExtensions.add(uid.intern());
				}
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
		Set<IExtension> ams = new HashSet<IExtension>();
		for (int i = 0; i < allAnalysisExtensions.length; ++i) {
			if (isExtensionIncluded(allAnalysisExtensions[i])) {
				// user preference exclude
				ids
						.add(allAnalysisExtensions[i].getUniqueIdentifier()
								.intern());
				ams.add(allAnalysisExtensions[i]);
			}
		}
		// check that all prerequisites are in that list
		for (IExtension analysisModule : ams) {
			IConfigurationElement[] analysisConfigElements = analysisModule
					.getConfigurationElements();
			for (int j = 0; j < analysisConfigElements.length; ++j) {
				IConfigurationElement currentConfigElement = analysisConfigElements[j];
				if (currentConfigElement.getName().equalsIgnoreCase(
						"prerequisite")) {
					if (!ids.contains(currentConfigElement.getAttribute("id")
							.intern())) {
						String logMessage = "The identified prerequisite \""
								+ currentConfigElement.getAttribute("id")
								+ "\" for \""
								+ analysisModule.getLabel()
								+ "\" (id = \""
								+ analysisModule.getUniqueIdentifier()
								+ "\") does not reference any known analysis module";
						String title = "Unknown Prerequisite";
						String dialogMessage = "Unknown analysis module \""
								+ currentConfigElement.getAttribute("id")
								+ "\" given as a prerequisite below:\n"
								+ analysisModuleInfo();
						elogPrompt(null, IStatus.ERROR, logMessage, title,
								dialogMessage);
						result = false; // found a problem
					}
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

		// construct the analysis levels based upon the prerequisites provided
		// in the plugin manifest (held in the analysisExtensions field)
		// filtering
		// out those that the user has specifically excluded
		int bailOutLevel = 0;
		final Set<IExtension> remainingAnalyses = new HashSet<IExtension>();
		for (int i = 0; i < allAnalysisExtensions.length; i++) {
			if (isExtensionIncluded(allAnalysisExtensions[i])) {
				// user preferences exclude
				remainingAnalyses.add(allAnalysisExtensions[i]);
			}
		}
		// remainingAnalyses is the set of all non-excluded analysis modules so
		// use it to set the field analysisExtensions
		analysisExtensions = remainingAnalyses
				.toArray(new IExtension[remainingAnalyses.size()]);
		final Set<String> lowerLevels = new HashSet<String>(); // of analysis
		// ids
		Set<IExtension> thisLevel = new HashSet<IExtension>();
		while (remainingAnalyses.size() > 0) {
			if (bailOutLevel++ > 25) {
				String logMessage = "Bailed out after 25 levels trying to order analysis modules"
						+ "...probable loop in analysis module dependencies";
				String title = "Analysis Module Prerequisite Problem";
				String dialogMessage = "Unable to order analysis modules:\n"
						+ analysisModuleInfo();
				elogPrompt(null, IStatus.ERROR, logMessage, title,
						dialogMessage);
				m_analysisExtensionSets.clear(); // zero out
				return;
			}
			for (IExtension cur : remainingAnalyses) {
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
			for (IExtension cur : thisLevel) {
				lowerLevels.add(cur.getUniqueIdentifier().intern());
			}
			thisLevel = new HashSet<IExtension>();
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
			IExtension analysisExtension) {
		if (analysisExtension == null) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<String>();
		IConfigurationElement[] analysisConfigElements = analysisExtension
				.getConfigurationElements();
		for (int j = 0; j < analysisConfigElements.length; j++) {
			IConfigurationElement currentConfigElement = analysisConfigElements[j];
			if (currentConfigElement.getName().equalsIgnoreCase("prerequisite")) {
				result.add(currentConfigElement.getAttribute("id").intern());
			}
		}
		return result;
	}

	/**
	 * Attaches to the actual class that implements an analysis module defined
	 * by an analysis module extension point an returns an {@link IAnalysis}
	 * representing the analysis module.
	 * 
	 * @param analysisExtension
	 *            the analysis module extension point to attach to
	 * @return the analysis module, or <code>null</code> if attachment failed
	 */
	IAnalysis getAnalysisModule(IExtension analysisExtension) {
		// is it in the cache?
		IAnalysis result = m_analysisModuleCache.get(analysisExtension);
		if (result == null) { // was not in the cache
			IConfigurationElement[] analysisConfigElements = analysisExtension
					.getConfigurationElements();
			for (int j = 0; j < analysisConfigElements.length; ++j) {
				IConfigurationElement currentConfigElement = analysisConfigElements[j];
				if (currentConfigElement.getName().equalsIgnoreCase("run")) {
					try {
						Object temp = currentConfigElement.createExecutableExtension("class");
						if (temp instanceof IIRAnalysis) {							
							result = new WholeAnalysisModule((IIRAnalysis) temp);
						} else {
							result = (IAnalysis) temp;
						}
						/*
						 * note that "createExecutableExtension()" *ALWAYS*
						 * creates a new object instance -- we want analysis
						 * modules to be singletons so we need to cache the
						 * result in the field "analysisModuleCache"
						 */
						m_analysisModuleCache.put(analysisExtension, result); // add
						// to
						// cache
					} catch (CoreException e) {
						String logMessage = "Unable to load class "
								+ currentConfigElement.getAttribute("class")
								+ " for analysis module "
								+ analysisExtension.getLabel();
						String title = "Analysis Module Class Missing";
						String dialogMessage = logMessage;
						elogPrompt(null, IStatus.ERROR, logMessage, title,
								dialogMessage, e);
					}
				}
			}
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
	Set<IExtension> getPrerequisiteAnalysisExtensionPoints(
			IExtension analysisExtension) {
		Set<IExtension> result = new HashSet<IExtension>();
		Set<String> ids = getPrerequisiteAnalysisIdSet(analysisExtension);
		for (String id : ids) {
			IExtension ext = getAnalysisModuleExtensionPoint(id);
			if (ext == null) {
				// System.out.println("null");
				continue;
			}
			result.add(ext);
		}
		return result;
	}

	/**
	 * Looks up the {@link IExtension}given an analysis module extension point
	 * identifier.
	 * 
	 * @param id
	 *            the analysis module extension point identifier to lookup
	 * @return the {@link IExtension}for <code>id</code>, or
	 *         <code>null</code> if <code>id</code> does not exist as an
	 *         analysis module extension point
	 */
	private IExtension getAnalysisModuleExtensionPoint(String id) {
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
			IConfigurationElement[] analysisConfigElements = allAnalysisExtensions[i]
					.getConfigurationElements();
			result.append(allAnalysisExtensions[i].getLabel() + " ("
					+ allAnalysisExtensions[i].getUniqueIdentifier() + ")\n");
			for (int j = 0; j < analysisConfigElements.length; ++j) {
				IConfigurationElement currentConfigElement = analysisConfigElements[j];
				if (currentConfigElement.getName().equalsIgnoreCase(
						"prerequisite")) {
					result.append("+ prerequisite: \""
							+ currentConfigElement.getAttribute("id") + "\"\n");
				}
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
	 * @return <code>true</code> if <code>analysisModule</code> exists in
	 *         the plugin's list of included analysis module extension points,
	 *         <code>false</code> otherwise
	 */
	boolean isExtensionIncluded(IExtension analysisModule) {
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
	void updateIncludedExtensions(Set<String> includedExtensions) {
		// Have we really changed anything?
		if (isIncludedExtensionsChanged(includedExtensions)) {
			m_includedExtensions.clear();
			m_includedExtensions.addAll(includedExtensions);
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
	 * @return <code>true</code> if the parameter is different than the
	 *         current plugin state, <code>false</code> if they are the same
	 */
	boolean isIncludedExtensionsChanged(Set<String> includedExtensions) {
		return !m_includedExtensions.equals(includedExtensions);
	}

	/**
	 * Checks if a single analysis module extension point identifier is included
	 * within the list of included identifiers in the
	 * {@link #m_includedExtensions} field.
	 * 
	 * @param id
	 *            the analysis module extension point identifier to check for
	 * @return <code>true</code> if <code>id</code> exists in the plugin's
	 *         list of included analysis module extension points,
	 *         <code>false</code> otherwise
	 */
	private boolean isExtensionIncluded(String id) {
		return m_includedExtensions.contains(id.intern());
	}

	public Iterable<String> getIncludedExtensions() {
		return m_includedExtensions;
	}
}