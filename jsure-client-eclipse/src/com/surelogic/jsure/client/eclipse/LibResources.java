package com.surelogic.jsure.client.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.surelogic.common.i18n.I18N;

public final class LibResources {

  public static final String PATH = "/lib/";

  /**
   * The name of the archive that contains the JSure Ant tasks.
   * <p>
   * Within this Zip should be a single directory of the form
   * <tt>jsure-ant</tt>. The name of the Zip file is versioned when it is saved
   * to the disk, e.g., <tt>jsure-ant-5.6.0.zip</tt>.
   */
  public static final String ANT_TASK_ZIP = "jsure-ant.zip";

  /**
   * Full path to the JSure Ant tasks within this Eclipse plugin.
   */
  public static final String ANT_TASK_ZIP_PATHNAME = PATH + ANT_TASK_ZIP;

  /**
   * The name of the archive that contains the JSure Maven plugin.
   * <p>
   * Within this Zip should be a single directory of the form
   * <tt>jsure-maven</tt>. The name of the Zip file is versioned when it is
   * saved to the disk, e.g., <tt>jsure-maven-5.6.0.zip</tt>.
   */
  public static final String MAVEN_PLUGIN_ZIP = "jsure-maven.zip";

  /**
   * Full path to the JSure Maven plugin within this Eclipse plugin.
   */
  public static final String MAVEN_PLUGIN_ZIP_PATHNAME = PATH + MAVEN_PLUGIN_ZIP;

  public static InputStream getStreamFor(String pathname) throws IOException {
    final URL url = LibResources.class.getResource(pathname);
    if (url == null)
      throw new IOException(I18N.err(323, pathname, Activator.PLUGIN_ID));
    final InputStream is = url.openStream();
    return is;
  }
}