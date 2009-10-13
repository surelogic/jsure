// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/ZipFileLocator.java,v 1.10 2007/04/16 17:15:56 chance Exp $
package edu.cmu.cs.fluid.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @region private State
 * @lock StateLock is this protects State
 */
public class ZipFileLocator extends AbstractFileLocator {
  private final ZipOutputStream outZip;  
  /**
   * @mapInto State
   * @unique
   * @aggregate Instance into State
   */
  private final List<EntryOutputStream> pendingEntries;
  private final ZipFile inZip;
  private final File file;
  
  public static final int READ = 1;
  public static final int WRITE = 2;
  
  /**
   * @singleThreaded
   * @borrowed this
   * @throws IOException
   */
  public ZipFileLocator(File f, int mode) throws IOException {
    file = f;
    
    switch (mode) {
    default:
      throw new IOException("Unknown zip file mode " + mode);
    case READ:
      outZip = null;
      pendingEntries = null;
      inZip = new ZipFile(f);
      break;
    case WRITE:
      inZip = null;      
      outZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(f), 32768));
      pendingEntries = new ArrayList<EntryOutputStream>();
      break;
    }
  }

  public File getCorrespondingFile(){
    return file;
  }
  
  public File locateFile(String name, boolean mustExist) {
    return null;
  }

  @Override
  public OutputStream openFileWriteOrNull(String name) {
    if (outZip == null) return null;
    ZipEntry ze = new ZipEntry(name);
    return new EntryOutputStream(ze);
  }

  @Override
  public InputStream openFileReadOrNull(String name) {
    try {
      ZipEntry ze = inZip.getEntry(name);
      if (ze == null) {
        return null;
      }
      return new BufferedInputStream(inZip.getInputStream(ze), 32768);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public synchronized void commit() throws IOException {
    if (outZip != null) {
      while (!pendingEntries.isEmpty()) {
        EntryOutputStream eos = pendingEntries.get(pendingEntries.size()-1);
        eos.close();
      }
      outZip.close();
      // outZip = null;
    }
  }

  class EntryOutputStream extends ByteArrayOutputStream {
    ZipEntry entry;
    EntryOutputStream(ZipEntry ze) {
      super();
      entry = ze;
      synchronized (ZipFileLocator.this) {
        pendingEntries.add(this);
      }
    }
    @Override
    public synchronized void close() throws IOException {
      if (entry != null) {
        synchronized (ZipFileLocator.this) {
          outZip.putNextEntry(entry);
          writeTo(outZip);
          outZip.closeEntry();
          entry = null;
          pendingEntries.remove(this);
        }
      } else {
        throw new IOException("already closed");
      }
    }
  }
}

