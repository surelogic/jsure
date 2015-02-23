/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/DataOutputDebugger.java,v 1.1 2004/09/01 00:23:23 boyland Exp $
 */
package edu.cmu.cs.fluid.util;

import java.io.DataOutput;
import java.io.IOException;


/**
 * TODO Fill in purpose.
 * @author boyland
 */
public class DataOutputDebugger implements DataOutput {

  private final DataOutput base;
  
  /**
   * 
   */
  public DataOutputDebugger(DataOutput out) {
    base = out;
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeDouble(double)
   */
  public void writeDouble(double v) throws IOException {
    System.out.println("double: " + v);
    base.writeDouble(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeFloat(float)
   */
  public void writeFloat(float v) throws IOException {
    System.out.println("float: " + v);
    base.writeFloat(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(int)
   */
  public void write(int b) throws IOException {
    System.out.println("u byte: 0x" + Integer.toHexString((255&b)));
    base.write(b);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeByte(int)
   */
  public void writeByte(int v) throws IOException {
    System.out.println("byte: 0x" + Integer.toHexString(v));
    base.writeByte(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeChar(int)
   */
  public void writeChar(int v) throws IOException {
    System.out.println("char: " + v);
    base.writeChar(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeInt(int)
   */
  public void writeInt(int v) throws IOException {
    System.out.println("int: " + v);
    base.writeInt(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeShort(int)
   */
  public void writeShort(int v) throws IOException {
    System.out.println("short: 0x" + Integer.toHexString(v));
    base.writeShort(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeLong(long)
   */
  public void writeLong(long v) throws IOException {
    System.out.println("long: 0x" + Long.toHexString(v));
    base.writeLong(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeBoolean(boolean)
   */
  public void writeBoolean(boolean v) throws IOException {
    System.out.println("boolean: " + v);
    base.writeBoolean(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(byte[])
   */
  public void write(byte[] b) throws IOException {
    write(b,0,b.length);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(byte[], int, int)
   */
  public void write(byte[] b, int off, int len) throws IOException {
    System.out.print("bytes: [");
    for (int i=0; i < len; ++i) {
      if (i != 0) System.out.print(",");
      System.out.print(Integer.toHexString(b[off+i]));
    }
    System.out.println("]");
    base.write(b,off,len);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeBytes(java.lang.String)
   */
  public void writeBytes(String s) throws IOException {
    System.out.println("bytes: " + s);
    base.writeBytes(s);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeChars(java.lang.String)
   */
  public void writeChars(String s) throws IOException {
    System.out.println("chars: " + s);
    base.writeChars(s);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeUTF(java.lang.String)
   */
  public void writeUTF(String str) throws IOException {
    System.out.println("u string: " + str);
    base.writeUTF(str);
  }

}
