/**
 * Usage:
 * <createsitexml dir="update_site_directory" [defaultcategory="A default category name"][categorylist=[category1:project1,project2,project3;category2:project4]
 * NOTE: The project names used in the categorylist parameter must match the name of the project's jar file minus the version number and extension.
 */
package com.surelogic.ant.tasks;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Ethan.Urie
 *
 */
public class CreateSiteXml extends Task
{
	private static final String FEATURES = "features";
	private File dir = null;
	private Hashtable<String, String> categories = new Hashtable<String, String>();
	private String categoryList = null;
	private String defaultCategory = "JSure";
	
	public void execute()
	{
		File[] dirs = null;
		File[] features = null;
		
		if(dir != null && dir.isDirectory())
		{
			dirs = dir.listFiles(new DirectoryFilter());
			for (File file : dirs)
			{
				if(file.isDirectory())
				{
					if(FEATURES.equals(file.getName()))
					{
						features = file.listFiles(new JarFilter());
						createSiteXml(features);
					}
				}
			}
		}
		else
		{
			log(dir.getAbsolutePath() + " is not a valid directory", Project.MSG_ERR);
		}
	}

	private void createSiteXml(File[] features) 
	{
		try
		{
			Vector<String> categoryV = new Vector<String>();
			
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element root = doc.createElement("site");
			for (File file : features)
			{
				Element node = doc.createElement("feature");
				String filename = file.getName();
				node.setAttribute("url", "features/" + filename);
				String[] parts = filename.substring(0, filename.lastIndexOf(".")).split("_");
				node.setAttribute("id", parts[0]);
				node.setAttribute("version", parts[1]);
				
				String cat = categories.get(filename);
				if(cat == null)
				{
					cat = defaultCategory;
				}
				
				if(!categoryV.contains(cat))
				{
					categoryV.add(cat);
				}
				Element catNode = doc.createElement("category");
				catNode.setAttribute("name", cat);
				node.appendChild(catNode);
				root.appendChild(node);
			}
			
			for (String cat : categoryV)
			{
				Element catDefNode = doc.createElement("category-def");
				catDefNode.setAttribute("name", cat);
				catDefNode.setAttribute("label", cat);
				root.appendChild(catDefNode);
			}
			doc.appendChild(root);
			
			
			//now output the xml file
			Source source = new DOMSource(doc);
			File file = new File(dir, "site.xml");
			if(file.exists())
			{
				file.delete();
			}
			Result result = new StreamResult(file);
			
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);
			
		}
		catch(ParserConfigurationException pe)
		{
			log("Error parsing new document.", pe, Project.MSG_ERR);
			pe.printStackTrace();
		}
		catch (TransformerConfigurationException e)
		{
			e.printStackTrace();
			log("Error site.xml file on disk.", e, Project.MSG_ERR);
		}
		catch (TransformerFactoryConfigurationError e)
		{
			e.printStackTrace();
			log("Error creating site.xml file on disk.", e, Project.MSG_ERR);
		}
		catch (TransformerException e)
		{
			e.printStackTrace();
			log("Error creating site.xml file on disk.", e, Project.MSG_ERR);
		}
	}

	public File getDir()
	{
		return dir;
	}

	public void setDir(File dir)
	{
		this.dir = dir;
	}

	public String getCategoryList()
	{
		return categoryList;
	}

	public void setCategoryList(String categoryList)
	{
		this.categoryList = categoryList;
		String[] catsAndProjs = categoryList.split(";");
		for (String string : catsAndProjs)
		{
			String[] cats = string.split(":");
			for (String category : cats)
			{
				String[] projs = category.split(",");
				for (String project : projs)
				{
					categories.put(project, category);
				}
			}
		}
	}

	public String getDefaultCategory()
	{
		return defaultCategory;
	}

	public void setDefaultCategory(String defaultCategory)
	{
		this.defaultCategory = defaultCategory;
	}

}
