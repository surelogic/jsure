/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/TestXMLParserConstants.java,v 1.7 2008/06/24 19:13:18 thallora Exp $*/
package com.surelogic.xml;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

public interface TestXMLParserConstants {
	/**
	 * Logger for this class
	 */
	static final Logger LOG = SLLogger.getLogger("ECLIPSE.xml");

	public static final String PACKAGE = "package";
	public static final String CLASS = "class";
	public static final String CLASSINIT = "classinit";
	public static final String CONSTRUCTOR = "constructor";
	public static final String FIELD = "field";
	public static final String METHOD = "method";
	public static final String PARAMETER = "parameter";
	public static final String PROMISE = "promise";
	public static final String RECEIVER = "receiver";
	public static final String RETVAL = "retval";

	public static final String NAME = "name";
	public static final String PARAMS = "params";
	public static final String INDEX = "index";
	public static final String TYPE = "type";
	public static final String KEYWORD = "keyword";
	public static final String CONTENTS = "contents";

	public static final String SUFFIX = ".promises.xml";
	public static final String DTD_NAME = "promises.dtd";

	public static final String UID_ATTRB = "uid";
	public static final String NAME_ATTRB = "name";
	public static final String PARAMS_ATTRB = "params";
	public static final String GENERIC_PARAMS_ATTRB = "genericParams";
	public static final String INDEX_ATTRB = "index";
	public static final String TYPE_ATTRB = "type";
	public static final String KEYWORD_ATTRB = "keyword";
	public static final String CONTENTS_ATTRB = "contents";
	public static final String RELEASE_VERSION_ATTRB = "release";
	public static final String MODIFIED_BY_TOOL_USER_ATTRB = "modifiedByToolUser";
	public static final String DELETE_ATTRB = "delete";

	public static final int HASH_MAP_LOAD = 3;

	public static final String DIR_PREFIX = System.getProperty("user.dir")
			+ File.separator;

	public static final String PROMISES_XML_PATH = "lib/promises";
	public static final String LOCAL_XML_PATH = "promises-xml";

	public static final FileFilter XML_FILTER = new FileFilter() {
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(SUFFIX);
		}
	};
}
