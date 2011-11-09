/*
 * CompSci 552
 * Homework #3
 * Solution
 * John Boyland
 * Fall 2005
 */
package edu.cmu.cs.fluid.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A class that can server as an IRInput or IROutput.
 * The data written or read is simply stored locally.
 * This class is used for testing and debugging only.
 */
public class IRPipe implements IROutput, IRInput {

  private List<Object> contents = new LinkedList<Object>();
  private Map<Object,Integer> writeCache = new HashMap<Object,Integer>();
  private List<Object> readCache = new ArrayList<Object>();
  
  
  private void add(Object x) {
    contents.add(x);
  }
  private Object remove() {
    return contents.remove(0);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writeNode(edu.cmu.cs.fluid.ir.IRNode)
   */
  public void writeNode(IRNode node) {
    add(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writeCachedObject(java.lang.Object)
   */
  public boolean writeCachedObject(Object object) {
    if (!writeCache.containsKey(object)) {
      int size = writeCache.size();
      writeCache.put(object,size);
      add(size);
      return false;
    }
    add(writeCache.get(object));
    return true;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writeIRType(edu.cmu.cs.fluid.ir.IRType)
   */
  public void writeIRType(IRType ty) {
    add(ty);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writeSlotFactory(edu.cmu.cs.fluid.ir.SlotFactory)
   */
  public void writeSlotFactory(SlotFactory sf) {
    add(sf);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writePersistentReference(edu.cmu.cs.fluid.ir.IRPersistent)
   */
  public void writePersistentReference(IRPersistent p) {
    add(p);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debug()
   */
  public boolean debug() {
    return false;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debugBegin(java.lang.String)
   */
  public void debugBegin(String x) {
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debugEnd(java.lang.String)
   */
  public void debugEnd(String x) {
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debugMark(java.lang.String)
   */
  public void debugMark(String x) {
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(int)
   */
  public void write(int b) {
    add((byte)b);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(byte[])
   */
  public void write(byte[] b) {
    add(b.clone());
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(byte[], int, int)
   */
  public void write(byte[] b, int off, int len) {
    byte[] copy = new byte[len];
    for (int i=0; i < len; ++i) {
      copy[i] = b[off+len];
    }
    add(copy);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeBoolean(boolean)
   */
  public void writeBoolean(boolean v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeByte(int)
   */
  public void writeByte(int v) {
    add((byte)v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeShort(int)
   */
  public void writeShort(int v) {
    add((short)v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeChar(int)
   */
  public void writeChar(int v) {
    add((char)v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeInt(int)
   */
  public void writeInt(int v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeLong(long)
   */
  public void writeLong(long v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeFloat(float)
   */
  public void writeFloat(float v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeDouble(double)
   */
  public void writeDouble(double v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeBytes(java.lang.String)
   */
  public void writeBytes(String s) {
    add(s.getBytes());
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeChars(java.lang.String)
   */
  public void writeChars(String s) {
    add(s.toCharArray());
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeUTF(java.lang.String)
   */
  public void writeUTF(String str) {
    add(str);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#getVersion()
   */
  public int getVersion() {
    return IRPersistent.version;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#getRevision()
   */
  public int getRevision() {
    return IRPersistent.revision;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readNode()
   */
  public IRNode readNode() {
    return (IRNode)remove();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readCachedObject()
   */
  public Object readCachedObject() {
    int index = (Integer)remove();
    if (index < readCache.size()) {
      return readCache.get(index);
    }
    return null;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#cacheReadObject(java.lang.Object)
   */
  public void cacheReadObject(Object object) {
    readCache.add(object);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readIRType()
   */
  public IRType readIRType() {
    return (IRType)remove();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readSlotFactory()
   */
  public SlotFactory readSlotFactory() {
    return (SlotFactory)remove();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readPersistentReference()
   */
  public IRPersistent readPersistentReference() {
    return (IRPersistent)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFully(byte[])
   */
  public void readFully(byte[] b) {
    readFully(b,0,b.length);
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFully(byte[], int, int)
   */
  public void readFully(byte[] b, int off, int len) {
    // We may have to permit partial reads, or reads of multiple byte arrays
    byte[] read = (byte[])remove();
    if (read.length != len) {
      throw new IllegalArgumentException("Expected byte array of same size");
    }
    for (int i=0; i < read.length; ++i) {
      b[off+i] = read[i];
    }
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#skipBytes(int)
   */
  public int skipBytes(int n) {
    byte[] read = (byte[])remove();
    return read.length;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readBoolean()
   */
  public boolean readBoolean() {
    return (Boolean)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readByte()
   */
  public byte readByte() {
    return (Byte)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUnsignedByte()
   */
  public int readUnsignedByte() {
    return readByte() & 255;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readShort()
   */
  public short readShort() {
    return (Short)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUnsignedShort()
   */
  public int readUnsignedShort() {
    return readShort() & 0xFFFF;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readChar()
   */
  public char readChar() {
    return (Character)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readInt()
   */
  public int readInt() {
    return (Integer)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readLong()
   */
  public long readLong() {
    return (Long)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFloat()
   */
  public float readFloat() {
    return (Float)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readDouble()
   */
  public double readDouble() {
    return (Double)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readLine()
   */
  public String readLine() {
    byte[] read = (byte[])remove();
    return new String(read);
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUTF()
   */
  public String readUTF() {
    return (String)remove();
  }
}
