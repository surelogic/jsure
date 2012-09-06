package edu.cmu.cs.fluid.sea.drops;

import com.surelogic.analysis.IIRProjects;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;

public class ProjectsDrop extends Drop {
	static private ProjectsDrop active = null;
	private IIRProjects projects;

	private ProjectsDrop(IIRProjects p) {
		projects = p;
		synchronized (ProjectsDrop.class) {
			active = null;
		}
	}

	public static ProjectsDrop ensureDrop(IIRProjects p) {
		ProjectsDrop match = null;
		for (ProjectsDrop pd : Sea.getDefault().getDropsOfExactType(
				ProjectsDrop.class)) {
			if (pd.projects != p) {
				pd.invalidate();
			} else {
				match = pd;
			}
		}
		if (match != null) {
			return match;
		}
		return new ProjectsDrop(p);
	}

	@Override
	public String getXMLElementName() {
		return "projects-drop";
	}

	@Override
	public void snapshotAttrs(XMLCreator.Builder s) {
		super.snapshotAttrs(s);
		s.addAttribute(AbstractXMLReader.PROJECTS, projects.getLabel());
	}

	public IIRProjects getIIRProjects() {
		return projects;
	}

	public void setProjects(IIRProjects p) {
		projects = p;
	}

	public static synchronized ProjectsDrop getDrop() {
		if (active != null) {
			if (active.isValid()) {
				return active;
			}
			/*
			 * If the drop is not valid null out our reference.
			 */
			active = null;
		}
		ProjectsDrop p = null;
		for (ProjectsDrop pd : Sea.getDefault().getDropsOfExactType(
				ProjectsDrop.class)) {
			if (p == null) {
				p = pd;
			} else {
				LOG.warning("Multiple projects analyzed for JSure: "
						+ pd.getIIRProjects().getLabel());
			}
		}
		active = p;
		return p;
	}

	public static IIRProjects getProjects() {
		ProjectsDrop pd = getDrop();
		if (pd == null)
			return null;
		else
			return pd.getIIRProjects();
	}
}