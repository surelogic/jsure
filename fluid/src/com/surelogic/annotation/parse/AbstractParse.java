/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.annotation.parse;

import java.io.*;

import org.antlr.runtime.*;

import com.surelogic.annotation.*;
import com.surelogic.parse.*;

public abstract class AbstractParse<P extends Parser> implements IParserInit<P> {
	public final P initParser(String text) throws Exception { 
		@SuppressWarnings("deprecation")
		InputStream is = new StringBufferInputStream(text);

		// create a CharStream that reads from the stream above
		ANTLRInputStream input = new ANTLRInputStream(is);

		// create a lexer that feeds off of input CharStream
		TokenSource lexer = newLexer(input);

		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// create a parser that feeds off the tokens buffer
		P parser = newParser(tokens);
		return parser;
	}

	protected abstract TokenSource newLexer(CharStream input);
	protected abstract P newParser(TokenStream tokens);

	public static void printAST(Object node) {
		printAST(node, true);
	}

	public static void printAST(Object node, boolean asAST) {
		if (node == null) {
			System.out.println("Null node");
			return;
		}
		if (node instanceof TreeToken) {
			TreeToken t = (TreeToken) node;
			System.out.println("token = "+t.getText());
			return;
		}

		AbstractNodeAdaptor.Node root = (AbstractNodeAdaptor.Node) node;
		System.out.println(root.toStringTree()); 
		if (asAST) {
			System.out.println(root.finalizeAST(IAnnotationParsingContext.nullPrototype).unparse(true));
		} else {
			System.out.println(root.finalizeId());
		}
	}
}
