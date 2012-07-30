package edu.cmu.cs.fluid.sea.drops;

import com.surelogic.analysis.IIRProject;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.*;

/**
 * This doesn't work for fluid-javac.  Use ProjectsDrop instead
 * 
 * @author Edwin
 */
@Deprecated()
public class ProjectDrop extends Drop {
  final String projectName;
  final IIRProject project;
  
  private ProjectDrop(String name, IIRProject p) {
	  projectName = name;
	  project = p;
  }

  public static ProjectDrop ensureDrop(String project, IIRProject p) {
	  for(ProjectDrop pd : Sea.getDefault().getDropsOfExactType(ProjectDrop.class)) {
		  if (project.equals(pd.getName())) {
			  return pd;
		  }
	  }
	  return new ProjectDrop(project, p);
  }
  
  @Override
  public String getEntityName() {
	  return "project-drop";
  }	
  
  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
	  super.snapshotAttrs(s);
	  s.addAttribute("name", projectName);
  }

  public String getName() {
	  return projectName;
  }
  
  public IIRProject getIIRProject() {
	  return project;
  }
  
  static ProjectDrop getDrop() {
	  ProjectDrop p = null;
	  for (ProjectDrop pd : Sea.getDefault().getDropsOfExactType(
			  ProjectDrop.class)) {
		  if (p == null) {
			  p = pd;
		  } else {
			  LOG.info("Multiple projects analyzed for JSure: "
					  + pd.getName());
		  }
	  }
	  return p;
  }
  
  static String getProject() {
	  ProjectDrop pd = getDrop();
	  if (pd == null) {
		  return null;
	  }
	  return pd.getName();
  }
}