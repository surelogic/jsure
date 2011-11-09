/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/XMLStringWriter.java,v 1.3 2007/07/25 20:45:59 swhitman Exp $*/
package com.surelogic.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

public class XMLStringWriter {

//	private static XMLOutputFactory fac = XMLOutputFactory.newInstance();
	//private static Writer s = new StringWriter();
//	private static XMLStreamWriter out;

	public static String writeXMLCharacters(String text) { 
		Writer s = new StringWriter();

//		try {
//		XMLStreamWriter out = fac.createXMLStreamWriter(s);
//		out.writeCharacters(text);
//		out.flush();
//		} catch (XMLStreamException e) {
//		/** This should never happen as StringWriters do not generate 
//		* IOExceptions
//		*/
//		e.printStackTrace();
//		}

		try {
			s.write(processString(text));
		} catch (IOException e) {
			/** This should never happen as StringWriters do not generate 
			 * IOExceptions
			 */
			SLLogger.getLogger().log(Level.SEVERE, "This should never happen", e);
		}

		return s.toString();
	}

	public static String writeXMLEmptyElement(String name) {
		Writer s = new StringWriter();

//		try {
//		XMLStreamWriter out = fac.createXMLStreamWriter(s);
//		out.writeEmptyElement(name);
//		//		out.writeEndElement();
//		out.writeEndDocument();
//		out.flush();			
//		out.close();
//		} catch (XMLStreamException e) {
//		/** This should never happen as StringWriters do not generate 
//		* IOExceptions
//		*/
//		e.printStackTrace();
//		}

		try {
			s.write("<" + name + "/>");
		} catch (IOException e) {
			/** This should never happen as StringWriters do not generate 
			 * IOExceptions
			 */
			SLLogger.getLogger().log(Level.SEVERE, "This should never happen", e);
		}

		return s.toString() + "\n";
	}

	public static String writeXMLDataElement(String name, String data, String space) {
		Writer s = new StringWriter();

//		try {
//		XMLStreamWriter out = fac.createXMLStreamWriter(s);
//		out.writeStartElement(name);
//		out.writeCharacters(data);
//		out.writeEndElement();
//		out.flush();			
//		out.close();
//		} catch (XMLStreamException e) {
//		/** This should never happen as StringWriters do not generate 
//		* IOExceptions
//		*/
//		e.printStackTrace();
//		}

		try {
			s.write(space + "<" + name + ">");
			s.write(writeXMLCharacters(data));
			s.write("</" + name + ">");
		} catch (IOException e){
			/** This should never happen as StringWriters do not generate 
			 * IOExceptions
			 */
			SLLogger.getLogger().log(Level.SEVERE, "This should never happen", e);
		}

		return s.toString() + "\n";
	}

	public static String writeElement(String name,
			HashMap<String,String> params, String text, String space) {
		Writer s = new StringWriter();

		try {
			s.write(space + "<" + name);
			if(params != null) {
				for(String param : params.keySet()) {
					s.write(" " + param + "=\"" 
							+ processString(params.get(param)) + "\"");
				}
			}
			s.write(">\n");
			s.write(text);
			s.write(space + "</" + name + ">");
		} catch (IOException e){
			/** This should never happen as StringWriters do not generate 
			 * IOExceptions
			 */
			SLLogger.getLogger().log(Level.SEVERE, "This should never happen", e);
		}
		return s.toString() + "\n";
	}

	private static String processString(String param) {		
		return param.replaceAll("&","&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
	}

	public static String writeEndElement(String name) {
		return "</" + name + ">\n";
	}
}
