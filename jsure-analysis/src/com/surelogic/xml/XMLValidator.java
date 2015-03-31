/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/XMLValidator.java,v 1.6 2007/07/30 20:15:08 swhitman Exp $*/
package com.surelogic.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.surelogic.common.logging.SLLogger;

public class XMLValidator extends DefaultHandler 
implements TestXMLParserConstants
{
	/** Keep a stack of the XML as we parse it */
	private static final Stack<SAXElement> elementStack = new Stack<SAXElement>();
	private static boolean strict = false;
	private static String pkgPath;

	public static void main(String[] args) {
		//validate(args[0],args[1].equalsIgnoreCase("true"));
		validate("C:\\Documents and Settings\\Spencer.Whitman\\Desktop\\XML Test stuff\\XML", true);
		System.out.println("Done!");
	}

	public static void validate(String path, boolean strictBool) {
		strict = strictBool;
		System.out.println("Validating path: " + path);

		String [] paths = path.split(";");
		for(int i = 0; i < paths.length; i++) {
			File f = new File(paths[i]);
			if(f.isDirectory())
				validatePath(f);
			else
				validateFile(f);
		}
	}

	//TODO input fileLocator instead of file
	public static void validateFile(File f) {
		XMLValidator handler = new XMLValidator();
		pkgPath = f.getPath();

		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(handler);
			reader.setErrorHandler(handler);
			reader.parse(PackageAccessor.readFile(f));
			System.out.println("File " + f.getName() + " is valid!");
			return;
		} catch (SAXParseException spe) {
			// Error generated by the parser
			LOG.severe("****************************");
			LOG.severe(f.getPath() +
					" XML promise parsing error"
					+ ", line " + spe.getLineNumber()
					+ ", uri " + spe.getSystemId());
			LOG.severe(spe.getMessage());

			// Unpack a delivered exception to get the exception it contains
//			Exception x = spe;
//			if(spe.getException() != null)
//			x = spe.getException();
//			x.printStackTrace();
		} catch (SAXException se) {
			// Error generated by this application
			LOG.severe("****************************");
			LOG.severe(f.getPath() +
			" Generated Exception");
			LOG.severe(se.getMessage());
		} catch (FileNotFoundException e) {
			SLLogger.getLogger().log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException ioe) {
			SLLogger.getLogger().log(Level.SEVERE, ioe.getMessage(), ioe);
		} 
		LOG.severe("File " + f.getName() + " is invalid");
	}

	public static void validatePath(File dir) {
		System.out.println("Validating dir " + dir.getName());
		File[] files = dir.listFiles();

		for(int i = 0; i < files.length; i++) {
			if(files[i].isDirectory())
				validatePath(files[i]);
			//Check if file is a promise file
			if(PackageAccessor.packageExtension(files[i]) != null)
				validateFile(files[i]);
		}

	}

	/** Validity checking bools */
	/* A valid xml promise file will have exactly one of the following */
	private boolean package_bool = false;
	private boolean class_bool = false;
	/* A valid xml promise file will have at most one of the following */
	private boolean classInit_bool = false;
	private boolean noParamConstructor_bool = false;
	/* Parameters may only appear in constructor and method elements */
	private boolean parameter_bool = false;

	@Override
	public void startElement(String namespaceURI,
			String sName, // simple name
			String qName, // qualified name
			Attributes attrs)
	throws SAXException 
	{
		String eName = sName;
		if("".equals(eName)) eName = qName; 

		/** Push this new XML element onto the elementStack */
		SAXElement e = new SAXElement(eName, attrs);
		elementStack.push(e);

		/** expected element is of the form <package name=<i>name</i>> */
		if(eName.equalsIgnoreCase(PACKAGE)) {

			/** Only one package is allowed per xml file */
			if(package_bool) 
				malformedXML("Only one package is allowed per xml promise file");

			/** Get the name of the package from the XML */
			String name = e.getAttribute(NAME_ATTRB);

			if(strict) {
				String [] actualPath = pkgPath.split("\\" + File.separator);
				String [] declPath = name.split("\\.");

				if(actualPath.length < declPath.length)
					malformedXML("Strict: Actual path " + pkgPath 
							+ " does not match declared path " + name);
				
				// Remove the expected file name from the path
				int j = actualPath.length - 2;

				for(int i = declPath.length - 1; i >= 0; i--) {
					if(j < 0)
						reportError("Actual path length error");

					if(!declPath[i].equalsIgnoreCase(actualPath[j]))
						malformedXML("Strict: Actual path " + pkgPath 
								+ " does not match declared path " + name);
					j--;
				}
			}

			/** Make sure the package element is well formed */
			if(name == null)
				malformedXML("Package element requires a name attribute");

			package_bool = true;

		} else if(package_bool) {

			if(eName.equalsIgnoreCase(CLASS)) {		

				/** Only one class is allowed per xml file */
				if(class_bool) 
					malformedXML("Only one class is allowed per xml promise file");

				String name = e.getAttribute(NAME_ATTRB);

				/** Make sure the class element is well formed */
				if(name == null)
					malformedXML("Class element requires a name attribute");

				class_bool = true;

			} else if(class_bool) {
				if(eName.equalsIgnoreCase(CLASSINIT)) {

					if(classInit_bool)
						reportWarn("More than one classInit entry declared");

					classInit_bool = true;

				} else if(eName.equalsIgnoreCase(CONSTRUCTOR)) {

					String params = e.getAttribute(PARAMS_ATTRB);


					if(params == null) {
						if(noParamConstructor_bool)
							malformedXML("Declared more than one constructor"
									+ " with no parameters");

						/** 
						 * No way to identify this node, so call it 
						 * 'constructor'.
						 */
						params = "constructor";
						noParamConstructor_bool = true;
					}

					parameter_bool = true;

					/** TODO: Consistancy check parameters (Only one constructor with
					 * the same parameters)
					 */

				} else if(eName.equalsIgnoreCase(FIELD)) {

					String name = e.getAttribute(NAME_ATTRB);

					/** Make sure the field element is well formed */
					if(name == null)
						malformedXML("Field element requires a name attribute");

				} else if(eName.equalsIgnoreCase(METHOD)) {

					String name = e.getAttribute(NAME_ATTRB);

					/** Make sure the method element is well formed */
					if(name == null)
						malformedXML("Method element requires a name attribute");

					parameter_bool = true;

				} else if(eName.equalsIgnoreCase(PARAMETER)) {

					if(!parameter_bool)
						malformedXML("Can not declare a parameter outside of a "
								+ "constructor element or method element");

					String index = e.getAttribute(INDEX_ATTRB);
					String name = e.getAttribute(NAME_ATTRB);

					/** Make sure the parameter element is well formed */
					if(index == null && name == null)
						malformedXML("Parameter element requires an index or "
								+ "name attribute");

				} else {
					/** TODO: think about this */
					//LOG.info("Ignoring start element " + eName);
				}
			} // if(class_bool)
		} // if(package_bool)
	}

	@Override
	public void endElement(String namespaceURI,
			String sName,
			String qName)
	throws SAXException
	{
		String eName = sName; // set the element name
		if("".equals(eName)) eName = qName; // namespaceAware = false

		/*SAXElement e =*/ elementStack.pop();

		/** Reciver and Retval entries are currently depreciated */
		if(eName.equalsIgnoreCase(RECEIVER)) {
			reportWarn("Receiver is depreciated; Ignoring and moving on");
		} else if(eName.equalsIgnoreCase(RETVAL)) {
			reportWarn("Retval is depreciated; Ignoring and moving on");


			/************************************
			 * Move down the nodeStack when we get to one of these end 
			 * elements 
			 *****/
		} else if(eName.equalsIgnoreCase(PACKAGE)) {
			// XXX do nothing?
		} else if(eName.equalsIgnoreCase(CLASS)) {
			// XXX do nothing?
		} else if(eName.equalsIgnoreCase(CLASSINIT)) {
			/** Classint (and possibly constructor) does not have any unique 
			 * (name or params) attribute with which to identify it */ 

		} else if(eName.equalsIgnoreCase(CONSTRUCTOR)) {
			parameter_bool = false;
		} else if(eName.equalsIgnoreCase(FIELD)) {
			// XXX do nothing?
		} else if(eName.equalsIgnoreCase(METHOD)) {
			parameter_bool = false;
		} else if(eName.equalsIgnoreCase(PARAMETER)) {		
			/************************************/
			return;

		} else {
			/** Log unknown element which is not inside a promise */
//			reportWarn("Unknown Element: " + eName);
//			}
		}
	}

	@Override
	public void characters(char buf[], int offset, int len) 
	throws SAXException {
		String s = new String(buf, offset, len);
		if(!s.trim().equals("")) {
			SAXElement e = elementStack.peek();
			e.addToCdata(s);
		}
	}

	@Override
	public void error(SAXParseException e) throws SAXParseException {
		throw e;
	}

	@Override
	public void warning(SAXParseException err) throws SAXParseException {
		System.out.println("** Warning" + ", line " + err.getLineNumber()
				+ ", uri " + err.getSystemId());
		System.out.println("   " + err.getMessage());
	}

	/** For errors and warnings; gives the line in the XML files */
	private static Locator locations;


	/** Make sure the locator is set up correctly */
	@Override
	public void startDocument() throws SAXException {
		if(locations == null)
			reportError("Locator was not created");
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		locations = locator;
	}

	/** Stub function for reporting an error */
	private static void reportError(String msg) throws SAXException {
		throw new SAXException(msg);
	}

	/** Stub function for reporting a warning (only to warn about issue with the
	 * XML file) */
	private static void reportWarn(String msg) {
		LOG.warning("Line " + locations.getLineNumber() + ": " + msg);
	}

	//TODO can there be an error other than "malformedXML" ?
	private static void malformedXML(String msg) throws SAXException {
		reportError("Malformed XML: " + "Line " + locations.getLineNumber()
				+ ": " + msg);
	}

}