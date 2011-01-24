/**
 * 
 */
package com.surelogic.ant.tasks;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.*;
import org.netbeans.lib.cvsclient.command.status.*;
import org.netbeans.lib.cvsclient.command.status.StatusInformation.SymName;
import org.netbeans.lib.cvsclient.commandLine.BasicListener;
import org.netbeans.lib.cvsclient.connection.*;
import org.netbeans.lib.cvsclient.event.CVSAdapter;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;

/**
 * @author Ethan.Urie
 *
 */
public class GetLatestCVSTag extends Task
{
	private static final String HEAD = "HEAD";
	private static final String BASE = "BASE";
	
	private String tag = null;

	private String property = null;

	private File path = null;
	
	private boolean caseSensitive = true;

	private CVSRoot cvsroot = null;

	private GlobalOptions globalOptions = null;

	private CvsListener listener = null;

	/**
	 * @see org.apache.tools.ant.Task
	 */
	public void execute()
	{
		if (validParams())
		{
			globalOptions = new GlobalOptions();
			globalOptions.setCVSRoot(getCVSRoot());
			cvsroot = CVSRoot.parse(globalOptions.getCVSRoot());

			try
			{
				Connection connection = connect();
				Client client = new Client(connection,
					new StandardAdminHandler());
				client.setLocalPath(path.getParentFile().getCanonicalPath());
				listener = new CvsListener();
				client.getEventManager().addCVSListener(listener);
				client.getEventManager().addCVSListener(new BasicListener());
				getStatus(client);
			}
			catch (CommandAbortedException e)
			{
				e.printStackTrace();
				log("Connection problem with the CVS repository: "
					+ cvsroot.toString(), e, Project.MSG_ERR);
			}
			catch (CommandException e)
			{
				e.printStackTrace();
				log("Problem executing a command.", e, Project.MSG_ERR);
			}
			catch (AuthenticationException e)
			{
				e.printStackTrace();
				log("Authentication problem", e, Project.MSG_ERR);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				log("Error getting the canonical path for " + path.getAbsolutePath(), 
					e, Project.MSG_ERR);
			}
		}
	}

	/**
	 * Queries the status of the given file
	 * @param client The Client object to use to run the command
	 * @throws CommandAbortedException
	 * @throws CommandException
	 * @throws AuthenticationException
	 * @throws IOException
	 */
	private void getStatus(Client client) throws CommandAbortedException,
		CommandException, AuthenticationException, IOException
	{
		//As long as the tag doesn't equal HEAD or BASE
		if(!HEAD.equals(tag) || !BASE.equals(tag))
		{
			StatusCommand statusCmd = new StatusCommand();
			statusCmd.setIncludeTags(true);
			statusCmd.setFiles(new File[]{path});
			
			StatusBuilder builder = (StatusBuilder) statusCmd.createBuilder(client
				.getEventManager());
			statusCmd.setBuilder(builder);
			
			client.executeCommand(statusCmd, globalOptions);
			
	
			//determine the most recent tag that matches the tag passed in
			getProject().setProperty(property, getLatestTag());
		}
		else
		{
			getProject().setProperty(property, tag);
		}
	}

	/**
	 * Finds the most recent tag matching the regular expression
	 * @return The String containing the tag
	 */
	private String getLatestTag()
	{
		Pattern pattern = Pattern.compile(tag, Pattern.CASE_INSENSITIVE);
		Matcher matcher = null;
		
		@SuppressWarnings("unchecked")
		List<SymName> tags = (List<SymName>) listener.getStatusInformation()
			.getAllExistingTags();
		SymName latestTag = null;
		
		for (SymName symName : tags)
		{
			String curTag = symName.getTag();
			log(symName.toString(), Project.MSG_INFO);
			
			matcher = pattern.matcher(curTag);
			if (matcher.matches())
			{
				double tmpRev =
				   getRevisionNumber(symName);
				double latestRev =
					getRevisionNumber(latestTag);
				if(latestTag == null || (tmpRev > latestRev))
				{
					latestTag = symName;
				}
				else if(tmpRev == latestRev)
				{
					log("Two tags with the same revision: " + symName, Project.MSG_INFO);
				}
			}
		}
		if(latestTag == null)
		{
			return HEAD;
		}
		return latestTag.getTag();
	}

	/**
	 * Connects to the CVS server - supports just pserver connections currently
	 * @return a Connection object
	 * @throws CommandAbortedException
	 * @throws AuthenticationException
	 */
	private Connection connect() throws CommandAbortedException,
		AuthenticationException
	{
		Connection connection = null;
		if (cvsroot.getMethod() == CVSRoot.METHOD_PSERVER)
		{
			PServerConnection c = new PServerConnection(cvsroot);
			connection = c;
			c.setEncodedPassword(PasswordsFile.findPassword(cvsroot
					.toString()));
			
			if(PasswordsFile.findPassword(cvsroot.toString()) == null)
			{
				System.out.println("Password is NULL");
			}
			c.open();
		}
		return connection;
	}

	/**
	 * Code from netbeans.org. Finds the CVS root from the CVS/Root file or from the CVS_ROOT environment variable
	 * @return The CVS Root string of the form :type:username@server:/path/to/cvs/repo
	 */
	private String getCVSRoot()
	{
		String root = null;
		BufferedReader r = null;
		try
		{
			File f = path.getParentFile();

			File rootFile = new File(f, "CVS/Root");
			if (rootFile.exists())
			{
				r = new BufferedReader(new FileReader(rootFile));
				root = r.readLine();
			}
		}
		catch (IOException e)
		{
			// ignore
		}
		finally
		{
			try
			{
				if (r != null)
					r.close();
			}
			catch (IOException e)
			{
				System.err.println("Warning: could not close CVS/Root file!");
			}
		}
		if (root == null)
		{
			root = System.getProperty("cvs.root");
		}
		return root;
	}

	/**
	 * validates our parameters
	 * @return true if the parameters are valid
	 */
	private boolean validParams()
	{
		boolean ret = true;
		if (path == null || !path.exists() || !path.isFile())
		{
			log(path.getAbsolutePath() + " is not a valid file. It should be a version-controlled file.",
				Project.MSG_ERR);
			ret = false;
		}
		if (tag == null || "".equals(tag.trim()))
		{
			log("Not a valid tag. The tag should be at least the first part of an existent CVS tag.", Project.MSG_ERR);
		}
		return ret;
	}
	
	/**
	 * Gets the revision number out of a string
	 * @param symName The SymName object containing the revision as a string of the form "revision: 0.00"
	 * @return A double with the actual revision number
	 */
	private double getRevisionNumber(SymName symName)
	{
		String rev = symName.getRevision();
		String ret = "0.0";
		if(rev.startsWith("revision: "))
		{
			String[] split = rev.split(" ");
			ret = split[1];
		}
		return Double.parseDouble(ret);
	}

	public String getTag()
	{
		return tag;
	}

	public void setTag(String tag)
	{
		this.tag = tag;
	}

	public File getPath()
	{
		return path;
	}

	public void setPath(File path)
	{
		this.path = path;
	}

	/**
	 * A listener class that captures the results to the status command
	 * @author Ethan.Urie
	 *
	 */
	class CvsListener extends CVSAdapter
	{
		private StatusInformation si = null;

		@Override
		public void fileInfoGenerated(FileInfoEvent e)
		{
			FileInfoContainer fileInfo = e.getInfoContainer();
			if (fileInfo.getClass().equals(StatusInformation.class))
			{
				log("A file status event was received.", Project.MSG_INFO);
				log("The status information object is: "
					+ fileInfo, Project.MSG_INFO);
			}

			si = (StatusInformation) e.getInfoContainer();
		}

		/**
		 * Returns the StatusInformation from the last Status command
		 * @return
		 */
		public StatusInformation getStatusInformation()
		{
			return si;
		}
	}

	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}

	public boolean isCaseSensitive()
	{
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;
	}
}
