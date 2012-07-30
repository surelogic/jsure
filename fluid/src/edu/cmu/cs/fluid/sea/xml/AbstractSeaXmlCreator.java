/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.*;
import static com.surelogic.common.jsure.xml.JSureXMLReader.SOURCE_REF;

import java.io.*;
import java.net.URI;
import java.util.*;

import com.surelogic.common.xml.*;
import com.surelogic.persistence.JavaIdentifier;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.JavaPromiseOpInterface;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Really a JSure-specific XML creator
 */
public class AbstractSeaXmlCreator extends XMLCreator {	
	final Map<IRNode,Long> hashes = new HashMap<IRNode, Long>();
	
	Long getHash(IRNode n) {
		Long hash = hashes.get(n);
		if (hash == null) {
			hash = SeaSummary.computeHash(n);
			hashes.put(n, hash);
		}
		return hash;
	}
	
	protected AbstractSeaXmlCreator(OutputStream out) throws IOException {
		super(out);
	}
	
	AbstractSeaXmlCreator(File location) throws IOException {
		super(location != null ? new FileOutputStream(location) : null);
	}
	
	public void addSrcRef(Builder outer, IRNode context, ISrcRef s, int indent, String flavor) {
		if (s == null) {
			return;
		}
		Builder b = outer.nest(SOURCE_REF);
		/*
		b.append(indent);
		Entities.start(SOURCE_REF, b);
		*/
		//b.start(SOURCE_REF);
		addLocation(b, s);		
		if (flavor != null) {
			b.addAttribute(FLAVOR_ATTR, flavor);
		}
		
		try {
			if (context instanceof MarkedIRNode) {
				System.out.println("Skipping decl for "+context);
			} else {
				final Operator op = JJNode.tree.getOperator(context);
				boolean onDecl = Declaration.prototype.includes(op);
				IRNode decl = context;
				if (!onDecl) {
					if (op instanceof JavaPromiseOpInterface) {
						// Deal with promise nodes hanging off of a decl
						decl = JavaPromise.getPromisedForOrNull(context);

						if (decl != null && Declaration.prototype.includes(decl)) {
							onDecl = true;
						} else {
							decl = VisitUtil.getEnclosingDecl(context);
						}
					} else {
						decl = VisitUtil.getEnclosingDecl(context);
					}
				}
				if (onDecl) {
					b.addAttribute(JAVA_ID_ATTR, JavaIdentifier.encodeDecl(decl));
				} else {
					b.addAttribute(WITHIN_DECL_ATTR, JavaIdentifier.encodeDecl(decl));
				}
				//addAttribute("unparse", DebugUnparser.unparseCode(context));
			}
			b.addAttribute(HASH_ATTR, getHash(context));			
			b.addAttribute(CUNIT_ATTR, s.getCUName());
			b.addAttribute(PKG_ATTR, s.getPackage());
			b.addAttribute(PROJECT_ATTR, s.getProject());
		} finally {
			b.end();
		}
	}
	
	protected void addLocation(Builder b, ISrcRef ref) {
		if (ref.getOffset() > 0) {
			b.addAttribute(OFFSET_ATTR, (long) ref.getOffset());
		}
		if (ref.getLength() > 0) {
			b.addAttribute(LENGTH_ATTR, (long) ref.getLength());
		}			
		b.addAttribute(LINE_ATTR, (long) ref.getLineNumber());
		String path = ref.getRelativePath();
		if (path != null) {
			b.addAttribute(PATH_ATTR, path);
		}
		URI loc = ref.getEnclosingURI();
		if (loc != null) {
			b.addAttribute(URI_ATTR, loc.toString());
		}
		Object o = ref.getEnclosingFile();
		if (o != null) {
			b.addAttribute(FILE_ATTR, o.toString());		
		}
	}
}
