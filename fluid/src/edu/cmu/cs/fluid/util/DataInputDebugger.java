/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/DataInputDebugger.java,v 1.1 2004/09/01 00:23:23 boyland Exp $
 */
package edu.cmu.cs.fluid.util;

import java.io.DataInput;
import java.io.IOException;


/**
 * A class useful for debugging DataInput streams
 * @author boyland
 */
public class DataInputDebugger implements DataInput {

  private final DataInput base;
  
  /**
   * Create a wrapper around a data input stream that echoes to stdout everything 
   * that is read from the stream.
   */
  public DataInputDebugger(DataInput di) {
    base = di;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readByte()
   */
  @Override
  public byte readByte() throws IOException {
    byte result = base.readByte();
    System.out.println("byte: 0x" + Integer.toHexString(result));
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readChar()
   */
  @Override
  public char readChar() throws IOException {
    char result = base.readChar();
    System.out.println("char: " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readDouble()
   */
  @Override
  public double readDouble() throws IOException {
    double result = base.readDouble();
    System.out.println("double: " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFloat()
   */
  @Override
  public float readFloat() throws IOException {
    float result = base.readFloat();
    System.out.println("float: " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readInt()
   */
  @Override
  public int readInt() throws IOException {
    int result = base.readInt();
    System.out.println("int: " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUnsignedByte()
   */
  @Override
  public int readUnsignedByte() throws IOException {
    int result = base.readUnsignedByte();
    System.out.println("u byte: 0x" + Integer.toHexString(result));
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUnsignedShort()
   */
  @Override
  public int readUnsignedShort() throws IOException {
    int result = base.readUnsignedShort();
    System.out.println("u short: 0x" + Integer.toHexString(result));
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readLong()
   */
  @Override
  public long readLong() throws IOException {
    long result = base.readLong();
    System.out.println("long: 0x" + Long.toHexString(result));
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readShort()
   */
  @Override
  public short readShort() throws IOException {
    short result = base.readShort();
    System.out.println("short: 0x" + Integer.toHexString(result));
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readBoolean()
   */
  @Override
  public boolean readBoolean() throws IOException {
    boolean result = base.readBoolean();
    System.out.println("boolean: " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#skipBytes(int)
   */
  @Override
  public int skipBytes(int n) throws IOException {
    int result = base.skipBytes(n);
    System.out.println("skipBytes(" + n + ") = " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFully(byte[])
   */
  @Override
  public void readFully(byte[] b) throws IOException {
    readFully(b,0,b.length);
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFully(byte[], int, int)
   */
  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    base.readFully(b,off,len);
    System.out.print("readFully ");
    for (int i=0; i < len; ++i) {
      if (i == 0) System.out.print("[");
      else System.out.println(",");
      System.out.print(Integer.toHexString(b[i+off]));
    }
    System.out.println("]");
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readLine()
   */
  @Override
  public String readLine() throws IOException {
    String result = base.readLine();
    System.out.println("string: " + result);
    return result;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUTF()
   */
  @Override
  public String readUTF() throws IOException {
    String result = base.readUTF();
    System.out.println("utf string: " + result);
    return result;
  }

}
