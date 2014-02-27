package com.surelogic.jsure.tests;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.java.xml.JSureAnalysisXMLConstants;

public final class JSureAnalysisXMLReader implements JSureAnalysisXMLConstants {
	static final Logger LOG = SLLogger.getLogger();
	
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
	private static List<String> getList(Element root, String category, String item,
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
	private static Element findElement(Element root, String elementName) {
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
	 * Read persistent double-checker plugin information from
	 * <code>target</code> into Eclipse. Invoked from {@link #startup}.
	 * 
	 * @param target
	 *            {@link File}to read from
	 * 
	 * @see #writeStateTo(File)
	 */
	static void readStateFrom(File target) {
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
				//System.out.println("'Included' : "+id);
				EclipseUtility.setBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id, true);
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
				//System.out.println("'Excluded' : "+id);
				EclipseUtility.setBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id, false);
				// System.out.println("Excluded "+id);
			}
		} catch (JDOMException e) {
			LOG.log(Level.SEVERE, 
					"failure XML parsing plugin state from "+target.getAbsolutePath(), e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE,
					"failure reading plugin state from "
							+ target.getAbsolutePath()
							+ " perhaps because this is the first invocation of double-checker",
					e);
		}
	}
}
