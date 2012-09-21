package com.surelogic.jsure.core.listeners;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.ProjectsDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.dropsea.irfree.drops.IRFreeDrop;
import com.surelogic.javac.Projects;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class PersistentDropInfo {
  private static final String NAME = "snapshot" + RegressionUtility.JSURE_SNAPSHOT_SUFFIX;

  private long timestamp = Long.MIN_VALUE;
  private List<IDrop> dropInfo = Collections.emptyList();
  private final File location;

  private PersistentDropInfo() {
    File location = null;
    try {
      final File jsureData = JSurePreferencesUtility.getJSureDataDirectory();
      if (jsureData != null) {
        location = new File(jsureData, NAME);
      } else {
        location = File.createTempFile("snapshot", RegressionUtility.JSURE_SNAPSHOT_SUFFIX);
      }
      // System.out.println("Using location: "+location);
    } catch (IOException e) {
      // Nothing to do
    }
    this.location = location;
    System.out.println("Drop location = " + location);
  }

  private static final PersistentDropInfo instance = new PersistentDropInfo();

  public static PersistentDropInfo getInstance() {
    return instance;
  }

  public boolean load() {
    try {
      final ProjectsDrop drop = ProjectsDrop.getDrop();
      // TODO NPE since it's built externally
      if (drop != null) {
        final Projects projects = (Projects) drop.getIIRProjects();
        final File results = new File(projects.getRunDir(), RemoteJSureRun.RESULTS_XML);
        if (results.exists() && results.length() > 0) {
          if (location != null) {
            FileUtility.copy(results, location);
            System.out.println("Copying results from " + results);
          }
        } else if (projects.getDate().after(new Date(timestamp))) {
          // Persist the Sea, and then load the info
          new SeaSnapshot(location).snapshot(projects.getLabel(), Sea.getDefault());
        }
      }
      if (location != null && location.exists()) {
        final long lastModified = location.lastModified();
        if (dropInfo.isEmpty() || lastModified > timestamp) {
          System.out.println("Loading info at " + lastModified);
          final long start = System.currentTimeMillis();
          dropInfo = SeaSnapshot.loadSnapshot(location);
          timestamp = lastModified;
          final long end = System.currentTimeMillis();
          System.out.println("Finished loading info = " + (end - start) + " ms");
        }
        return true;
      } else {
        dropInfo = Collections.emptyList();
      }
    } catch (Exception e) {
      dropInfo = Collections.emptyList();
    }
    return false;
  }

  public synchronized boolean isEmpty() {
    return dropInfo.isEmpty();
  }

  public synchronized Collection<? extends IDrop> getRawInfo() {
    return dropInfo;
  }

  public synchronized boolean dropsExist(Class<? extends Drop> type) {
    for (IDrop i : dropInfo) {
      if (i.instanceOfIRDropSea(type)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public synchronized <T extends IDrop, T2 extends Drop> Set<T> getDropsOfType(Class<T2> dropType) {
    if (!dropInfo.isEmpty()) {
      final Set<T> result = new HashSet<T>();
      for (IDrop i : dropInfo) {
        if (i.instanceOfIRDropSea(dropType)) {
          result.add((T) i);
        }
      }
      return result;
    }
    return Collections.emptySet();
  }

}
