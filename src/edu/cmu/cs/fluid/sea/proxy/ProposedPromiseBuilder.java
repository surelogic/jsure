/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.proxy;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public class ProposedPromiseBuilder implements IDropBuilder {
	private final String annotation; 
	private final String contents;
	private final IRNode at; 
	private final IRNode from;
	
	public ProposedPromiseBuilder(String anno, String contents, IRNode at, IRNode from) {
		annotation = anno;
		this.contents = contents;
		this.at = at;
		this.from = from;
	}
	
	public ProposedPromiseDrop buildDrop() {
		//System.out.println("\tCreating proposal: "+annotation+" "+contents+"  from  "+DebugUnparser.toString(from));
		return new ProposedPromiseDrop(annotation, contents, at, from);
	}
	
	public int build() {
		buildDrop();
		return 1;
	}
}
