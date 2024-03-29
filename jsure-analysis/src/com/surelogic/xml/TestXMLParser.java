/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/TestXMLParser.java,v 1.36 2008/11/06 18:41:16 chance Exp $*/
package com.surelogic.xml;

import java.io.*;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * assumes running JSure from a directory with "test.Test.promises.xml" in it,
 * and "analyzing" a project with a file test/Test.java
 */

/**
 * This class is not thread safe! (You can not run multiple threads on the same
 * TestXMLParser object in a thread safe manner). In addition, this class
 * manipulates the AAST
 */
class TestXMLParser extends DefaultHandler implements
		TestXMLParserConstants {
	private final ITypeEnvironment tEnv;

	/** Keep a stack of the XML as we parse it */
	private final Stack<SAXElement> elementStack = new Stack<SAXElement>();
	/** Keep a stack of the IRNode's as we parse the XML */
	private final Stack<NodeElement> nodeStack = new Stack<NodeElement>();
	/** Keep a flat iteration of the IRNode tree structure */
	private Iterator<IRNode> nodeIterator = null;
	/** AnnotationVisitor is used to insert a promise into the AAST */
	private final AnnotationVisitor annoVis;
	private int numAnnotationsAdded = 0;
	private String id;
	
	TestXMLParser(ITypeEnvironment typeEnv) {
		tEnv = typeEnv;
		annoVis = new AnnotationVisitor(tEnv, "XML Parser");
	}

	/**
	 * @param root
	 *            A CompilationUnit
	 * @param xml
	 *            The name of the promises.xml file to parse in
	 * @return The number of annotations added
	 */
	public static int process(ITypeEnvironment tEnv, IRNode root, String xml)
			throws Exception {
		final TestXMLParser handler = new TestXMLParser(tEnv);
		return handler.processAST(root, xml);
	}
	
	public int processAST(IRNode root, String xml) throws Exception {
		String pkgName = VisitUtil.getPackageName(root);
		numAnnotationsAdded = 0;
		id = xml;

		/** Initalize the node stack */
		nodeStack.push(new NodeElement(pkgName, NodeKind.ROOT, root));
		/** Initalize the node iterator */
		nodeIterator = VisitUtil.getTypeDecls(root);

		InputSource in = PackageAccessor.readPackage(pkgName,
				PackageAccessor.promiseFileName(xml));
		/*
		if (false) {
			PromisesXMLReader r = new PromisesXMLReader();
			r.read(in.getByteStream());
			File f = File.createTempFile(xml, TestXMLParserConstants.SUFFIX);
			PromisesXMLWriter w = new PromisesXMLWriter(f);
			w.write(r.getPackage());
			f.deleteOnExit();
			return 0;
		}
		*/
		if (in == null) {
			throw new FileNotFoundException(xml + " doesn't exist");
		}
		try {
			if (SLLogger.getLogger().isLoggable(Level.FINER)) {
				SLLogger.getLogger().finer("Parsing the XML file " + xml);
			}
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(this);
			reader.setErrorHandler(this);
			reader.parse(in);
		} catch (SAXParseException spe) {
			// Error generated by the parser
			String msg = "\nXML promise parsing error" + ", line "
					+ spe.getLineNumber() + ", uri " + spe.getSystemId()
					+ "\n   " + spe.getMessage();

			// Unpack a delivered exception to get the exception it contains
			Exception x = spe;
			if (spe.getException() != null)
				x = spe.getException();
			SLLogger.getLogger().log(Level.SEVERE, msg, x);
		} catch (SAXException sxe) {
			// Error generated by this application
			Exception x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			SLLogger.getLogger().log(Level.SEVERE, "Error generated by this application", x);
		} catch (IOException ioe) {
			// I/O error
			SLLogger.getLogger().log(Level.SEVERE, ioe.getMessage(), ioe);
		}
		return numAnnotationsAdded;
	}

	private static final SAXElement IGNORE_ELEMENT = new SAXElement("IGNORE");

	private boolean isStackTop(NodeKind kind) {
		return nodeStack.peek().getKind().equals(kind);
	}
	
	@Override
	public void startElement(String namespaceURI, String sName, // simple name
			String qName, // qualified name
			Attributes attrs) throws SAXException {
		String eName = sName;
		if ("".equals(eName))
			eName = qName;

		/**
		 * If an IGNORE_ELEMENT has been pushed, ignore everything under that
		 * element.
		 */
		try {
			if (elementStack.peek().equals(IGNORE_ELEMENT)) {
				elementStack.push(IGNORE_ELEMENT);
				return;
			}
		} catch (EmptyStackException s) {
			// Don't worry if the stack is empty
		}

		/** Push this new XML element onto the elementStack */
		SAXElement e = new SAXElement(eName, attrs);
		elementStack.push(e);

		/** expected element is of the form <package name=<i>name</i>> */
		if (eName.equalsIgnoreCase(PACKAGE)) {

			/** Only one package is allowed per xml file */
			if (!isStackTop(NodeKind.ROOT))
				malformedXML("Only one package is allowed per xml promise file");

			/** Get the name of the package from the XML */
			String name = e.getAttribute(NAME_ATTRB);
			/**
			 * Get the name of the package from the AAST (This should be the
			 * root of the AAST and thus the first and current top element of
			 * the nodeStack)
			 */
			String pName = nodeStack.peek().getName();

			/** Make sure the package element is well formed */
			if (name == null) {
				malformedXML("Package element requires a name attribute");
				return;
			}

			/**
			 * Make sure the two package names match. If they don't, something
			 * is wrong
			 */
			if (!name.equalsIgnoreCase(pName))
				reportError("Package XML name " + name
						+ " does not equal AAST name " + pName);

			nodeStack.push(new NodeElement(name, NodeKind.PKG, CompilationUnit.getPkg(nodeStack.peek().getNode())));

		} else if (isStackTop(NodeKind.PKG)) {
			if (eName.equalsIgnoreCase(CLASS)) {
				handleClass(e);
			}
		} else if (isStackTop(NodeKind.TYPE)) {
			if (eName.equalsIgnoreCase(CLASS)) {
				handleClass(e);
			} 
			else if (eName.equalsIgnoreCase(CLASSINIT)) {
				IRNode classInit = JavaPromise.getClassInitOrNull(nodeStack
						.peek().getNode());
				if (classInit != null) {
					/**
					 * ClassInit elements have no attributes, so use the
					 * static tag 'classInit'
					 */
					nodeStack.push(new NodeElement("classInit", NodeKind.CLASS_INIT, classInit));
				} else
					malformedXML("A class initializer was declared in the "
							+ "xml but does not exist for "
							+ JavaNames.getQualifiedTypeName(nodeStack
									.peek().getNode()));

			} else if (eName.equalsIgnoreCase(CONSTRUCTOR)) {

				String params = e.getAttribute(PARAMS_ATTRB);

				final IRNode here = nodeStack.peek().getNode();
				IRNode con = TreeAccessor.findConstructor(params, here, tEnv);
				if (params == null) {
					/*
						if (noParamConstructor_bool)
							malformedXML("Declared more than one constructor"
									+ " with no parameters");
					 */

					/**
					 * No way to identify this node, so call it
					 * 'constructor'.
					 */
					params = "constructor";
					//noParamConstructor_bool = true;
				}
				if (con != null || here == null) {
					nodeStack.push(new NodeElement(params, NodeKind.CONSTRUCTOR, con));
				} else {
					reportWarn(
							"XML declared but could not find constructor"
							+ " node " + e.getAttribute(NAME_ATTRB)/*,
							"constructor", nodeStack.peek().getNode()*/, false);
					
					nodeStack.push(new NodeElement(params, NodeKind.CONSTRUCTOR, null));
				}

			} else if (eName.equalsIgnoreCase(FIELD)) {

				String name = e.getAttribute(NAME_ATTRB);

				/** Make sure the field element is well formed */
				if (name == null)
					malformedXML("Field element requires a name attribute");

				final IRNode here = nodeStack.peek().getNode();
				IRNode f = TreeAccessor.findField(name, here);
				if (f != null || here == null) {
					nodeStack.push(new NodeElement(name, NodeKind.FIELD, f));
				} else {
					malformedStartElement("Could not find field node "
							+ name, name, nodeStack.peek().getNode());
				}

			} else if (eName.equalsIgnoreCase(METHOD)) {

				String name = e.getAttribute(NAME_ATTRB);

				/** Make sure the method element is well formed */
				if (name == null)
					malformedXML("Method element requires a name attribute");

				final IRNode here = nodeStack.peek().getNode();
				IRNode m = TreeAccessor.findMethod(here, name, 
						e.getAttribute(PARAMS_ATTRB),
						tEnv);
				if (m != null || here == null) {
					nodeStack.push(new NodeElement(name, NodeKind.METHOD, m));
				} else {
					/*
					TreeAccessor.findMethod(here, name, 
							e.getAttribute(PARAMS_ATTRB), tEnv);
							*/
					reportWarn("Could not find method node "
							+ name/*, name, here*/, false);
					nodeStack.push(new NodeElement(name, NodeKind.METHOD, null));
				}
			}
		} else if (isStackTop(NodeKind.METHOD) || isStackTop(NodeKind.CONSTRUCTOR)) {
			if (eName.equalsIgnoreCase(PARAMETER)) {

				String index = e.getAttribute(INDEX_ATTRB);
				String name = e.getAttribute(NAME_ATTRB);

				/** Make sure the parameter element is well formed */
				if (index == null && name == null)
					malformedXML("Parameter element requires an index or "
							+ "name attribute");

				final IRNode here = nodeStack.peek().getNode();
				IRNode p = TreeAccessor.findParameter(index, name, here);
				if (p != null || here == null)
					nodeStack.push(new NodeElement((index != null) ? index
							: name, NodeKind.PARAM, p));
				else {
					malformedStartElement(
							"Could not find parameter node with index "
							+ index + " and name " + name, name,
							nodeStack.peek().getNode());
				}
			}
			// } else if(eName.equalsIgnoreCase(PROMISE)) {

			// if(!(e.getAttribute(KEYWORD_ATTRB) == null) ||
			// !(e.getAttribute(CONTENTS_ATTRB) == null))
			// malformedXML("Detected old style of XML");

			// promise_bool = true;
		} else if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Ignoring start element " + eName);
		}
	} // if(class_bool)
		
	private void handleClass(SAXElement e) throws SAXException {
		String name = e.getAttribute(NAME_ATTRB);

		/** Make sure the class element is well formed */
		if (name == null)
			malformedXML("Class element requires a name attribute");

		/** Find this class' AAST node */
		IRNode next = null;
		final boolean isNested;
		if (isStackTop(NodeKind.PKG)) {
			isNested = false;
			
			while (nodeIterator.hasNext()) {
				next = nodeIterator.next();

				if (name.equals(JJNode.getInfoOrNull(next))) {
					break;
				}
			}
		} else {
			next = TreeAccessor.findNestedClass(name, nodeStack.peek().getNode());
			isNested = true;
		}

		if (next == null) {
			reportWarn("Could not find class node " + name, !isNested);			
		}
		nodeStack.push(new NodeElement(name, NodeKind.TYPE, next));
	}

	@Override
	public void endElement(String namespaceURI, String sName, String qName)
			throws SAXException {
		String eName = sName; // set the element name
		if ("".equals(eName))
			eName = qName; // namespaceAware = false

		if (elementStack.peek().equals(IGNORE_ELEMENT)) {
			elementStack.pop();
			return;
		}

		SAXElement e = elementStack.pop();

		// if(eName.equalsIgnoreCase(PROMISE)) {
		/** The following code allows this parser to read the old XML format */
		// String keyword = e.getAttribute(KEYWORD_ATTRB);
		// String contents = e.getAttribute(CONTENTS_ATTRB);
		// if(!"".equals(keyword) && keyword != null) {
		// /**
		// * If a promise has cdata and no contents, set contents to
		// * the cdata (or an empty String object if there is no cdata
		// * either)
		// */
		// if(contents == null || "".equals(contents))
		// contents = new String(e.getCdata());
		// LOG.info("Got promise " + keyword +
		// (!"".equals(contents) ? (" content " + contents) : "") + " at " +
		// DebugUnparser.toString(nodeStack.peek().getNode()));
		// /** Add this promise to the AAST */
		// annoVis.handleXMLPromise(nodeStack.peek().getNode(), keyword,
		// contents);
		// }
		/** We are no longer in a promise context */
		// promise_bool = false;
		/** Reciver and Retval entries are currently depreciated */
		/* } else */if (eName.equalsIgnoreCase(RECEIVER)) {
			reportWarn("Receiver is depreciated; Ignoring and moving on", true);
		} else if (eName.equalsIgnoreCase(RETVAL)) {
			reportWarn("Retval is depreciated; Ignoring and moving on", true);

			/*******************************************************************
			 * Move down the nodeStack when we get to one of these end elements
			 ******************************************************************/
		} else if (eName.equalsIgnoreCase(PACKAGE)) {
			popNode(e.getAttribute(NAME_ATTRB));
		} else if (eName.equalsIgnoreCase(CLASS)) {
			popNode(e.getAttribute(NAME_ATTRB));
		} else if (eName.equalsIgnoreCase(CLASSINIT)) {
			/**
			 * Classint (and possibly constructor) does not have any unique
			 * (name or params) attribute with which to identify it
			 */
			popNode("classInit");
		} else if (eName.equalsIgnoreCase(CONSTRUCTOR)) {
			if (e.getAttribute(PARAMS_ATTRB) == null)
				popNode("constructor");
			else
				popNode(e.getAttribute(PARAMS_ATTRB));
		} else if (eName.equalsIgnoreCase(FIELD)) {
			popNode(e.getAttribute(NAME_ATTRB));
		} else if (eName.equalsIgnoreCase(METHOD)) {
			popNode(e.getAttribute(NAME_ATTRB));
		} else if (eName.equalsIgnoreCase(PARAMETER)) {
			popNode((e.getAttribute(INDEX_ATTRB) != null) ? e
					.getAttribute(INDEX_ATTRB) : e.getAttribute(NAME_ATTRB));
			/** ********************************* */

		} else {
			/**
			 * If this element is surrounded by <promise> tags, try to add this
			 * element as a promise. Otherwise ignore it. (NOTE: <promise> tags
			 * are depreciated)
			 */
			// if(promise_bool) {
			// String contents = e.getAttribute(CONTENTS_ATTRB);
			// /**
			// * If a promise has cdata and no contents, set contents to
			// * the cdata (or an empty String object if there is no cdata
			// * either)
			// */
			// if(contents == null || "".equals(contents))
			// contents = new String(e.getCdata());
			final String contents = e.getCdata();
			IRNode annotatedNode = nodeStack.peek().getNode();
			if (annotatedNode != null) {
				if (CompilationUnit.prototype.includes(annotatedNode)) {
					// Hack to annotate the package declaration, instead of the comp unit
					annotatedNode = CompilationUnit.getPkg(annotatedNode);
				}
				if (LOG.isLoggable(Level.FINER)) {
					LOG
					.finer("Got promise "
							+ eName
							+ (!"".equals(contents) ? (" content " + contents)
									: "")
									+ " at "
									+ DebugUnparser.toString(annotatedNode));
				}

				/** Add this promise to the AAST */
				final boolean ignore = "true".equals(e.getAttribute(TestXMLParserConstants.DELETE_ATTRB));
				if (!ignore) {
					boolean added = annoVis.handleXMLPromise(annotatedNode, eName, contents, 
							                                 e.getAttributes(), JavaNode.ALL_FALSE);
					if (added) {
						numAnnotationsAdded++;
					}
				}
			}
			// } else {
			/** Log unknown element which is not inside a promise */
			// reportWarn("Unknown Element: " + eName);
			// }
		}
	}

	@Override
	public void characters(char buf[], int offset, int len) throws SAXException {
		String s = new String(buf, offset, len);
		if (!s.trim().equals("")) {
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

	/**
	 * Pop a node from the nodeStack; report a warning if the stack appears
	 * malformed. (This is not necessarly an error)
	 */
	private void popNode(String name) throws SAXException {

		if (nodeStack.peek().getName().equals(name))
			nodeStack.pop().getNode();
		else
			reportWarn("Expected node named " + name + " but node named "
					+ nodeStack.peek().getName() + " is current stack top", true);
	}

	/** For errors and warnings; gives the line in the XML files */
	private Locator locations;

	/** Make sure the locator is set up correctly */
	@Override
	public void startDocument() throws SAXException {
		if (locations == null)
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

	/**
	 * Stub function for reporting a warning (only to warn about issue with the
	 * XML file)
	 */
	private void reportWarn(String msg, boolean log) {
		final String s = "Line " + locations.getLineNumber() + " of " +id+ ": " + msg;
		if (log) {
			LOG.warning(s);
		} else {
			System.err.println(s);
		}
	}

	private void malformedXML(String msg) throws SAXException {
		reportError("Malformed XML: " + "Line " + locations.getLineNumber()
				+ ": " + msg);
	}

	private void malformedStartElement(String msg, String name,
			IRNode here) {
		IRNode root = VisitUtil.findRoot(here);

		reportWarn(msg, true);
		elementStack.pop();
		elementStack.push(IGNORE_ELEMENT);

		// XXX This is not enough and does not work. Parameters must be matched
		// too
		System.err.println("\n" + XMLGenerator.generateStringXML(root, true));
	}

	enum NodeKind {
		ROOT, PKG, TYPE, CLASS_INIT, FIELD, METHOD, PARAM, CONSTRUCTOR
	}
	
	/** NodeElements are named AAST nodes */
	private static class NodeElement {
		private final String name;
		private final IRNode node;
		private final NodeKind kind;
		
		NodeElement(String name, NodeKind k, IRNode node) {
			this.name = name;
			this.node = node;
			kind = k;
		}

		public NodeKind getKind() {
			return kind;
		}

		public String getName() {
			return name;
		}

		public IRNode getNode() {
			return node;
		}
	}

}
