/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public class SimpleAnalysisResult extends AbstractAnalysisResult {
	private final int messageCode;
	private final String[] args;
	
	public <T extends IAASTRootNode> SimpleAnalysisResult(PromiseDrop<T> about, IRNode location, int code, Object... args) {
		super(about, location);
		messageCode = code;
		this.args = new String[args.length];
		for(int i=0; i<args.length; i++) {
			this.args[i] = args[i].toString();
		}
	}

	@Override
	protected void attributesToXML(XmlCreator.Builder b) {
		//Entities.newLine(b, indent);
		b.addAttribute(PersistenceConstants.MESSAGE, I18N.res(messageCode, (Object[]) args));
		b.addAttribute(PersistenceConstants.MESSAGE_CODE, messageCode);
		if (args.length == 0) {
			b.addAttribute(PersistenceConstants.MESSAGE_ARGS, "");
		} else {
			StringBuilder argsB = new StringBuilder();
			boolean first = true;
			for(String arg : args) {
				if (first) {
					first = false;
				} else {
					argsB.append(", "); // TODO what if the args contain a comma?
				}
				argsB.append(arg);
			}
			b.addAttribute(PersistenceConstants.MESSAGE_ARGS, argsB.toString());
		}
	}
}
