package com.surelogic.jsecure.client.eclipse.adhoc;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.adhoc.IAdHocDataSource;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsecure.client.eclipse.Activator;
import com.surelogic.jsecure.client.eclipse.JSecureScan;
import com.surelogic.jsecure.client.eclipse.views.adhoc.QueryEditorView;
import com.surelogic.jsecure.client.eclipse.views.adhoc.QueryResultsView;
import com.surelogic.jsecure.client.eclipse.views.adhoc.QuerydocView;

public class JSecureDataSource extends AdHocManagerAdapter implements IAdHocDataSource, ILifecycle {
	private static final JSecureDataSource INSTANCE = new JSecureDataSource();

	static {
		INSTANCE.init();
	}

	public static JSecureDataSource getInstance() {
		return INSTANCE;
	}

	private JSecureDataSource() {
		// singleton
	}

	@NonNull
	public static AdHocManager getManager() {
		final AdHocManager manager =  AdHocManager.getInstance(INSTANCE);
		if (!manager.getGlobalVariableValues().containsKey(AdHocManager.DATABASE)) {
			// TODO Hack for now
			File scanDir = null;
			for(File f : EclipseUtility.getJSecureDataDirectory().listFiles()) {
				if (f.isDirectory()) {
					scanDir = f;
					break;
				}
			}
			if (scanDir != null) {
				INSTANCE.setSelectedRun(new JSecureScan(scanDir));
				manager.setGlobalVariableValue(AdHocManager.DATABASE, scanDir.getAbsolutePath());
			}
		}
		return manager;
	}

	private boolean isValid() {
		return Activator.getDefault() != null;
	}
	
	public void init() {
		if (isValid()) {
			getManager().addObserver(this);
		}
	}

	public void dispose() {
		if (isValid()) {
			getManager().removeObserver(this);
			AdHocManager.shutdown(); // Is this right for all tools?
		}
	}
	  
	@Override
	public File getQuerySaveFile() {
	    return new File(EclipseUtility.getJSecureDataDirectory(), "jsecure-queries.xml");
	}

	@Override
	public URL getDefaultQueryUrl() {
	    return Thread.currentThread().getContextClassLoader().getResource("/lib/adhoc/default-jsecure-queries.xml");
	}

	@Override
	public void badQuerySaveFileNotification(Exception e) {
		if (getSelectedScan() == null) {
			SLLogger.getLogger().log(Level.SEVERE, "No scan for JSecure query save", e);			
		} else {
			SLLogger.getLogger().log(Level.SEVERE, "Problem with JSecure query save for "+getSelectedScan().getId(), e);
		}
	}

	/**
	 * The currently selected scan, may be {@code null} which indicates that no scan
	 * is selected.
	 */
	private volatile JSecureScan f_selectedScan;

	/**
	 * Gets the currently selected scan.
	 * 
	 * @return the currently selected scan, or {@code null} if no scan is selected.
	 */
	public JSecureScan getSelectedScan() {
		return f_selectedScan;
	}

	/**
	 * Sets the currently selected scan.
	 * 
	 * @param scanDescription
	 *          the scan that is now selected, or {@code null} if no scan is now
	 *          selected.
	 */
	public void setSelectedRun(final JSecureScan scanDescription) {
		f_selectedScan = scanDescription;
	}
	
	@Override
	public DBConnection getDB() {
		final JSecureScan desc = getSelectedScan();
	    return desc == null ? null : desc.getDB();
	}

	@Override
	public int getMaxRowsPerQuery() {
		// TODO Auto-generated method stub
		return 50;
	}

	@Override
	public String[] getCurrentAccessKeys() {
		final JSecureScan scanDir = getSelectedScan();
	    if (scanDir == null) {
	      return null;
	    }	    
	    return new String[] { scanDir.getId(), "JSecure.query" };
	}

	@Override
	public boolean queryResultWillBeEmpty(AdHocQuery query) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getCustomDisplay(String className) throws Exception {
	    throw new UnsupportedOperationException();
	}

	@Nullable
	public URL getQuerydocImageURL(String imageName) {
	    return CommonImages.getImageURL(imageName);
	}

	@NonNull
	public String getQueryEditorViewId() {
		return QueryEditorView.class.getName();
	}

	@NonNull
	public String getQueryResultsViewId() {
		return QueryResultsView.class.getName();
	}

	@NonNull
	public String getQueryDocViewId() {
		return QuerydocView.class.getName();
	}
}
