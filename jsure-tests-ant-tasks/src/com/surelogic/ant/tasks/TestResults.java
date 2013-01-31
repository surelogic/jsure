package com.surelogic.ant.tasks;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.*;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Usage: &lt;testresults&gt; Sets the given property to FAILED if any failures
 * are detected. Otherwise it doesn't set it.
 * <p>
 * Required parameters: file - the full path to the results file
 * <p>
 * Optional parameters: -- In this case, these two are mutually exclusive.
 * Defining both doesn't make any sense.
 * <ul>
 * <li>tests - A comma-separated list of tests to specifically report on. It
 * takes the form of: &lt;classname&gt;:&lt;test method&gt;
 * <li>ignore - A comma-separated list of tests to specifically ignore. It takes
 * the form of: &lt;classname>:&lt;test method&gt;
 * </ul>
 */
public class TestResults extends Task implements ContentHandler, ErrorHandler {

	private static String TESTCASE = "testcase";

	private static String CNAME = "classname";

	private static String NAME = "name";

	private static String FAILURE = "failure";

	private static String MAJORDOMO = "testMajordomo";

	private static String SUCCESS_RESULT = "SUCCESS";

	private static String FAILURE_RESULT = "FAILED";

	private File logfile = null;

	private BufferedWriter bWriter = null;

	// the filename of the test results file
	private File file = null;

	// a comman-separated list of <class>:<test> to ignore
	private String ignore = null;

	// a comman-separated list of <class>:<test> to look for
	private String tests = null;

	// determines whether or not we print the test name out
	private boolean printTestName = true;

	/**
	 * Name of the property to set
	 */
	private String property = null;

	private Hashtable<String, Vector<String>> testTable = null;

	private Hashtable<String, Vector<String>> ignoredTable = null;

	private boolean inTestCase = false;

	private void output(final String msg) {
		log(msg, Project.MSG_WARN);
		try {
			bWriter.append(msg);
			bWriter.newLine();
		} catch (IOException e) {
			log(e, Project.MSG_ERR);
			e.printStackTrace();
		}
	}
	
	@Override
  public void execute() {
		try {
			if (!logfile.exists()) {
				logfile.createNewFile();
			}
			bWriter = new BufferedWriter(new FileWriter(logfile, true));
		} catch(IOException e) {
			log(e, Project.MSG_ERR);
		}
		
		try {
			execute_internal();
		} finally {
			if (bWriter != null) {
				try {
					bWriter.close();
				} catch (IOException e) {
					log(e, Project.MSG_ERR);
				}
			}
		}
	}
	
	private void execute_internal() {
		if (file == null) {
			getProject().setProperty(property, FAILURE_RESULT);
			output(FAILURE_RESULT+"... null file");
			return;
		}

		populateCollection(testTable, tests);
		populateCollection(ignoredTable, ignore);		

		if (file.exists() && file.isFile()) {
			if (file.length() == 0) {
				// Nothing to look at, so fail
				getProject().setProperty(property, FAILURE_RESULT);
				output(FAILURE_RESULT+"... zero-length file");
				return;
			}
			if ((testTable != null && ignoredTable == null)
					|| (testTable == null && ignoredTable != null)
					|| (testTable == null && ignoredTable == null)) {
				try {
					log("###############################################################",
							Project.MSG_WARN);
					log(file.getParent(), Project.MSG_WARN);
					log("---------------------------------------------------",
							Project.MSG_WARN);
					bWriter.append("###############################################################\n"
							+ file.getParent()
							+ "\n--------------------------------------------------");
					bWriter.newLine();

					// try {
					// InputSource input = new InputSource(new
					// FileReader(file));
					// XMLReader reader = XMLReaderFactory.createXMLReader();
					// reader.setContentHandler(this);
					// reader.setErrorHandler(this);
					// reader.parse(input);
					//
					// } catch (SAXException e) {
					// System.err.println("Could not create the SAX parser to parse the file "
					// + file);
					// e.printStackTrace();
					// throw new BuildException(
					// "Could not create the SAX parser to parse the file " +
					// file, e);
					// } catch (FileNotFoundException e) {
					// System.err.println("Could not create the SAX parser to parse the file "
					// + file);
					// e.printStackTrace();
					// throw new
					// BuildException("Could not find the XML file at: " + file,
					// e);
					// } catch (IOException e) {
					// System.err.println("Could not create the SAX parser to parse the file "
					// + file);
					// e.printStackTrace();
					// throw new BuildException(
					// "Could not parse the XML file at: " + file, e);
					// }

					DocumentBuilder db = DocumentBuilderFactory.newInstance()
							.newDocumentBuilder();
					Document doc = db.parse(file);
					Element root = doc.getDocumentElement();
					NodeList nodes = root.getElementsByTagName(TESTCASE);
					Node tmp = null;
					Node cnameAttr = null;
					Node nameAttr = null;
					NamedNodeMap attrs = null;

					for (int i = 0; i < nodes.getLength(); i++) {
						tmp = nodes.item(i);
						// check the <testcase> nodes
						if (tmp.getNodeType() == Node.ELEMENT_NODE) {
							attrs = tmp.getAttributes();
							cnameAttr = attrs.getNamedItem(CNAME);
							nameAttr = attrs.getNamedItem(NAME);

							if (testTable != null) {
								// see if this testcase node is for one of our
								// classes
								if (testTable.containsKey(cnameAttr
										.getNodeValue())) {
									// if so, get the children nodes to see if
									// there is a <failure> node
									Vector<String> vect = testTable
											.get(cnameAttr.getNodeValue());
									if (vect.isEmpty()
											|| vect.contains(nameAttr
													.getNodeValue())) {
										determineIfTestFailed(tmp);
									}
								}
							} else if (ignoredTable != null) {
								// Report everything that's not in ignoredTable
								if (ignoredTable.containsKey(cnameAttr
										.getNodeValue())) {
									Vector<String> vect = ignoredTable
											.get(cnameAttr.getNodeValue());

									if (vect.isEmpty()
											|| vect.contains(nameAttr
													.getNodeValue())) {
										continue;
									} else {
										// report
										determineIfTestFailed(tmp);
									}
								} else {
									// report
									determineIfTestFailed(tmp);
								}
							} else {
								// Report
								// everything
								determineIfTestFailed(tmp);
							}
						}
					}
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
					log("Error creating XML Document.", e, Project.MSG_ERR);
				} catch (SAXException e) {
					e.printStackTrace();
					log(e.getMessage() + " reading XML Document: "
							+ file.getAbsolutePath(), e, Project.MSG_ERR);
				} catch (IOException e) {
					e.printStackTrace();
					log(e.getMessage() + " reading XML Document: "
							+ file.getAbsolutePath(), e, Project.MSG_ERR);
				}
			} else {
				log("tests and ignore cannot both be set.", Project.MSG_ERR);
			}


		} else {
			log(file.getAbsolutePath() + " is not a valid log file.",
					Project.MSG_ERR);
		}
	}

	private void determineIfTestFailed(Node parent) {
		NodeList children = parent.getChildNodes();
		Node childNode = null;

		NamedNodeMap attrs = parent.getAttributes();
		Node cnameAttr = attrs.getNamedItem(CNAME);
		Node nameAttr = attrs.getNamedItem(NAME);
		StringBuffer logmsg = new StringBuffer();
		if (children.getLength() > 0) {

			for (int j = 0; j < children.getLength(); j++) {
				childNode = children.item(j);
				if (FAILURE.equalsIgnoreCase(childNode.getNodeName())) {
					getProject().setProperty(property, FAILURE_RESULT);
					logmsg.append(FAILURE_RESULT);
					logmsg.append("...");

					if (printTestName) {
						logmsg.append(cnameAttr.getNodeValue());
						logmsg.append(":");
						logmsg.append(nameAttr.getNodeValue());
						logmsg.append("\n");
					}
					if (getProject().getProperty(property) == null) {
						getProject().setProperty(property, FAILURE_RESULT);
					}
					output(logmsg.toString());
					break;
				}
			}
		} else {
			logmsg.append(SUCCESS_RESULT);
			logmsg.append("...");
			if (printTestName) {
				logmsg.append(cnameAttr.getNodeValue());
				logmsg.append(":");
				logmsg.append(nameAttr.getNodeValue());
				logmsg.append("\n");
			}
			output(logmsg.toString());
		}
	}

	private void populateCollection(Hashtable<String, Vector<String>> table,
			String prop) {
		if (prop != null) {
			String[] first = prop.split("\\s*,\\s*");
			for (int i = 0; i < first.length; i++) {
				String[] second = first[i].split(":");
				if (second.length > 2 || second.length <= 0) {
					log("Expected <classname>:<test method>", Project.MSG_ERR);
					break;
				}
				// the first element has to be the class
				if (second.length == 1) {
					if (table.containsKey(second[0])) {
						log("Error. Methods already added for test class: "
								+ second[0] + ". Cannot put wildcard.",
								Project.MSG_ERR);
					} else {
						Vector<String> tmp = new Vector<String>();
						table.put(second[0], tmp);
					}
				} else {

					if (table.containsKey(second[0])) {
						table.get(second[0]).add(second[1]);
					} else {
						Vector<String> tmp = new Vector<String>();
						tmp.add(second[1]);
						table.put(second[0], tmp);
					}
				}
			}
		}
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getIgnore() {
		return ignore;
	}

	public void setIgnore(String ignore) {
		ignoredTable = new Hashtable<String, Vector<String>>();
		this.ignore = ignore;
	}

	public String getTests() {
		return tests;
	}

	public void setTests(String tests) {
		testTable = new Hashtable<String, Vector<String>>();
		this.tests = tests;
	}

	public boolean isPrintTestName() {
		return printTestName;
	}

	public void setPrintTestName(boolean printTestName) {
		this.printTestName = printTestName;
	}

	public File getLogfile() {
		return logfile;
	}

	public void setLogfile(File logfile) {
		this.logfile = logfile;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

	public void endDocument() throws SAXException {
	}

	public void endElement(String uri, String localName, String name)
			throws SAXException {
	}

	public void endPrefixMapping(String prefix) throws SAXException {
	}

	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
	}

	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void skippedEntity(String name) throws SAXException {
	}

	public void startDocument() throws SAXException {
	}

	public void startElement(String uri, String simpleName,
			String qualifiedName, Attributes attrs) throws SAXException {
		if (TESTCASE.equalsIgnoreCase(simpleName)) {
			String classname = getAttrValue(attrs, CNAME);
			if ((testTable != null && testTable.contains(classname))
					|| (ignoredTable != null && !ignoredTable
							.contains(classname))
					|| (testTable == null && ignoredTable == null)) {
				inTestCase = true;
			}
		} else if (FAILURE.equalsIgnoreCase(simpleName) && inTestCase) {
			StringBuilder logmsg = new StringBuilder();
			if (MAJORDOMO.equals(getAttrValue(attrs, NAME))) {
				getProject().setProperty(property, FAILURE_RESULT);
				logmsg.append(FAILURE_RESULT);
				logmsg.append("...");

				if (printTestName) {
					// logmsg.append(cnameAttr.getNodeValue());
					logmsg.append(":");
					// logmsg.append(nameAttr.getNodeValue());
					logmsg.append("\n");
				}
				log(logmsg.toString(), Project.MSG_WARN);
				try {
					bWriter.write(logmsg.toString());
					bWriter.newLine();
					if (getProject().getProperty(property) == null) {
						getProject().setProperty(property, FAILURE_RESULT);
					}
				} catch (IOException e) {
					log(e, Project.MSG_ERR);
					e.printStackTrace();
				}
			}
		}
	}

	private String getAttrValue(Attributes attrs, String name2) {
		String ret = null;
		assert (name2 != null);

		for (int i = 0; i < attrs.getLength(); i++) {
			if (attrs.getLocalName(i).equals(name2)) {
				ret = attrs.getValue(i);
				break;
			}
		}
		return ret;
	}

	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	public void error(SAXParseException exception) throws SAXException {
	}

	public void fatalError(SAXParseException exception) throws SAXException {
	}

	public void warning(SAXParseException exception) throws SAXException {
	}
}
