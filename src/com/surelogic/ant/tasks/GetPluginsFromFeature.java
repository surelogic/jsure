/**
 * Reads an eclipse feature.xml file and returns a comma-separated list of the included plugins.
 * Usage:
 * <getpluginsfromfeature property="name of property to store list" featurefile="/path/to/the/feature.xml"/>
 */
package com.surelogic.ant.tasks;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author ethan
 *
 */
public class GetPluginsFromFeature extends Task implements ContentHandler, ErrorHandler {
		
		private File featureFile = null;
		private String property = null;
		private final List<String> pluginList = new ArrayList<String>();
		private static final String FEATURE = "feature";
		private static final String XML = "xml";
		private static final String FEATURE_XML = FEATURE + "." + XML;
		private static final String PLUGIN = "plugin";
		private static final String DELIMITER = ",";
		
		public void execute(){
				validateParameters();
				parseFeatureFile();
				
				StringBuilder sb = new StringBuilder();
				int size = pluginList.size();
				for (int i = 0; i < size; i++){
						sb.append(pluginList.get(i));
						if(i < size - 1){
								sb.append(DELIMITER);
						}
				}
				
				getProject().setProperty(property, sb.toString());
		}
		
		/**
		 * 
		 */
		private void parseFeatureFile() {
				try {
						InputSource input = new InputSource(new FileReader(featureFile));
						XMLReader reader = XMLReaderFactory.createXMLReader();
						reader.setContentHandler(this);
						reader.setErrorHandler(this);
						reader.parse(input);
						
				} catch (SAXException e) {
						throw new BuildException("Could not create the SAX parser to parse the feature.xml", e);
				} catch (FileNotFoundException e) {
						throw new BuildException("Could not find the feature.xml at: " + featureFile, e);
				} catch (IOException e) {
						throw new BuildException("Could not parse the feature.xml at: " + featureFile, e);
				}
		}

		private void validateParameters(){
				if(featureFile == null){
						throw new BuildException("The featurefile attribute is required.");
				}else if(featureFile.isDirectory()){
						File[] feature = featureFile.listFiles(new UberFileFilter(FEATURE, XML));
						if(feature.length != 0){
								throw new BuildException("The featurefile attribute must be either the feature.xml desired, or a directory containing exactly one feature.xml file.");
						}
						featureFile = feature[0];
				}
				else{
						if(!FEATURE_XML.equals(featureFile.getName())){
								throw new BuildException("The featurefile attribute must point to a valid feature.xml file.");
						}
				}
				
				if(property == null || "".equals(property.trim())){
						throw new BuildException("The property attribute is required and cannot be blank.");
				}
		}

		/**
		 * @return the featureFile
		 */
		public final File getFeatureFile() {
				return featureFile;
		}

		/**
		 * @param featureFile the featureFile to set
		 */
		public final void setFeatureFile(File featureFile) {
				this.featureFile = featureFile;
		}

		/**
		 * @return the property
		 */
		public final String getProperty() {
				return property;
		}

		/**
		 * @param property the property to set
		 */
		public final void setProperty(String property) {
				this.property = property;
		}
		
		/* **********************************************************************************
		 * @see org.xml.sax.ContentHandler
		 * @see org.xml.sax.ErrorHandler
		 ************************************************************************************/

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
		 */
		public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#endDocument()
		 */
		public void endDocument() throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		public void endElement(String arg0, String arg1, String arg2)
						throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
		 */
		public void endPrefixMapping(String arg0) throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
		 */
		public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
						throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
		 */
		public void processingInstruction(String arg0, String arg1)
						throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
		 */
		public void setDocumentLocator(Locator arg0) {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
		 */
		public void skippedEntity(String arg0) throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#startDocument()
		 */
		public void startDocument() throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String samespaceURI, String simpleName, String qualifiedName,
						Attributes attrs) throws SAXException {
				if("plugin".equalsIgnoreCase(simpleName)){
						for(int i = 0; i < attrs.getLength(); i++){
								if("id".equals(attrs.getLocalName(i))){
										pluginList.add(attrs.getValue(i));
								}
						}
				}
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
		 */
		public void startPrefixMapping(String arg0, String arg1)
						throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException arg0) throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		public void fatalError(SAXParseException arg0) throws SAXException {
				// TODO Auto-generated method stub
				
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		public void warning(SAXParseException arg0) throws SAXException {
				// TODO Auto-generated method stub
				
		}
		
}
