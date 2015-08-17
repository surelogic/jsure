package com.surelogic.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

/**
 * This code can be run to check if all our source code is UTF-8 and legal for
 * the javac (Oracle compiler). It seems Eclipse is more liberal about the
 * characters it accepts.
 * <p>
 * Note that these tests part of the regression tests.
 */
public class UnicodeChecker extends TestCase {

  public void testJavaSourcesAreUTF8() {
    final AtomicLong counter = new AtomicLong();
    final File topOfGit = new File("../..");
    System.out.println("Checking all .java files at UTF-8 in the SureLogic source code");
    System.out.println(" -- Searching under " + topOfGit.getAbsolutePath());
    go(topOfGit, counter);
    System.out.println(" -- " + SLUtility.toStringHumanWithCommas(counter.get()) + " .java files are UTF-8");
  }

  public void go(File path, AtomicLong counter) {
    if (path.isDirectory()) {
      for (File e : path.listFiles()) {
        go(e, counter);
      }
    } else {
      check(path);
      counter.incrementAndGet();
    }
  }

  public void check(File javaFile) {
    final String fn = javaFile.getAbsolutePath();
    if (javaFile.exists() && javaFile.isFile() && fn.endsWith(".java")) {
      read(javaFile, fn);
    }
  }

  public void read(File javaFile, String name) {
    FileInputStream fIn;
    FileChannel fChan;
    long fSize;
    ByteBuffer mBuf;

    try {
      fIn = new FileInputStream(javaFile);
      fChan = fIn.getChannel();
      fSize = fChan.size();
      mBuf = ByteBuffer.allocate((int) fSize);
      fChan.read(mBuf);
      mBuf.rewind();

      CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
      decoder.onMalformedInput(CodingErrorAction.REPORT);
      decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
      try {
        decoder.decode(mBuf);
      } catch (MalformedInputException e) {
        System.err.println(name + " MIE: " + e.getMessage() + " [bytepostion=" + mBuf.position() + "]");
      }

      fChan.close();
      fIn.close();
    } catch (IOException exc) {
      System.out.println(exc);
    }
  }
}
