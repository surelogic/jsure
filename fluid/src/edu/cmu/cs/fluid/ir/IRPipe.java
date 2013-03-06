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
  @Override
  public void writeNode(IRNode node) {
    add(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writeCachedObject(java.lang.Object)
   */
  @Override
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
  @Override
  public void writeIRType(IRType ty) {
    add(ty);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writeSlotFactory(edu.cmu.cs.fluid.ir.SlotFactory)
   */
  @Override
  public void writeSlotFactory(SlotFactory sf) {
    add(sf);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#writePersistentReference(edu.cmu.cs.fluid.ir.IRPersistent)
   */
  @Override
  public void writePersistentReference(IRPersistent p) {
    add(p);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debug()
   */
  @Override
  public boolean debug() {
    return false;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debugBegin(java.lang.String)
   */
  @Override
  public void debugBegin(String x) {
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debugEnd(java.lang.String)
   */
  @Override
  public void debugEnd(String x) {
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IROutput#debugMark(java.lang.String)
   */
  @Override
  public void debugMark(String x) {
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(int)
   */
  @Override
  public void write(int b) {
    add((byte)b);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(byte[])
   */
  @Override
  public void write(byte[] b) {
    add(b.clone());
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#write(byte[], int, int)
   */
  @Override
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
  @Override
  public void writeBoolean(boolean v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeByte(int)
   */
  @Override
  public void writeByte(int v) {
    add((byte)v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeShort(int)
   */
  @Override
  public void writeShort(int v) {
    add((short)v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeChar(int)
   */
  @Override
  public void writeChar(int v) {
    add((char)v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeInt(int)
   */
  @Override
  public void writeInt(int v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeLong(long)
   */
  @Override
  public void writeLong(long v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeFloat(float)
   */
  @Override
  public void writeFloat(float v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeDouble(double)
   */
  @Override
  public void writeDouble(double v) {
    add(v);
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeBytes(java.lang.String)
   */
  @Override
  public void writeBytes(String s) {
    add(s.getBytes());
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeChars(java.lang.String)
   */
  @Override
  public void writeChars(String s) {
    add(s.toCharArray());
  }

  /* (non-Javadoc)
   * @see java.io.DataOutput#writeUTF(java.lang.String)
   */
  @Override
  public void writeUTF(String str) {
    add(str);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#getVersion()
   */
  @Override
  public int getVersion() {
    return IRPersistent.version;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#getRevision()
   */
  @Override
  public int getRevision() {
    return IRPersistent.revision;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readNode()
   */
  @Override
  public IRNode readNode() {
    return (IRNode)remove();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readCachedObject()
   */
  @Override
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
  @Override
  public void cacheReadObject(Object object) {
    readCache.add(object);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readIRType()
   */
  @Override
  public IRType readIRType() {
    return (IRType)remove();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readSlotFactory()
   */
  @Override
  public SlotFactory readSlotFactory() {
    return (SlotFactory)remove();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRInput#readPersistentReference()
   */
  @Override
  public IRPersistent readPersistentReference() {
    return (IRPersistent)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFully(byte[])
   */
  @Override
  public void readFully(byte[] b) {
    readFully(b,0,b.length);
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFully(byte[], int, int)
   */
  @Override
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
  @Override
  public int skipBytes(int n) {
    byte[] read = (byte[])remove();
    return read.length;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readBoolean()
   */
  @Override
  public boolean readBoolean() {
    return (Boolean)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readByte()
   */
  @Override
  public byte readByte() {
    return (Byte)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUnsignedByte()
   */
  @Override
  public int readUnsignedByte() {
    return readByte() & 255;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readShort()
   */
  @Override
  public short readShort() {
    return (Short)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUnsignedShort()
   */
  @Override
  public int readUnsignedShort() {
    return readShort() & 0xFFFF;
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readChar()
   */
  @Override
  public char readChar() {
    return (Character)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readInt()
   */
  @Override
  public int readInt() {
    return (Integer)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readLong()
   */
  @Override
  public long readLong() {
    return (Long)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readFloat()
   */
  @Override
  public float readFloat() {
    return (Float)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readDouble()
   */
  @Override
  public double readDouble() {
    return (Double)remove();
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readLine()
   */
  @Override
  public String readLine() {
    byte[] read = (byte[])remove();
    return new String(read);
  }

  /* (non-Javadoc)
   * @see java.io.DataInput#readUTF()
   */
  @Override
  public String readUTF() {
    return (String)remove();
  }
}
