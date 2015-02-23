package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.util.UniqueID;
import com.surelogic.ThreadSafe;

@ThreadSafe
public class IRPersistentReferenceType<T extends IRPersistent> implements IRType<T>
{
  private IRPersistentReferenceType() {}
  public static final IRPersistentReferenceType prototype =
    new IRPersistentReferenceType();
  static {
    IRPersistent.registerIRType(prototype,'P');
  }
  
  @SuppressWarnings("unchecked")
  public static <T extends IRPersistent> IRPersistentReferenceType<T> getInstance() {
    return prototype;
  }

  public boolean isValid(Object ref) {
    return ref instanceof IRPersistent || ref == null;
  }

  public void writeValue(T ref, IROutput out) 
    throws IOException
  {
    out.writePersistentReference(ref);
  }

  @SuppressWarnings("unchecked")
  public T readValue(IRInput in) throws IOException
  {
    return (T) in.readPersistentReference();
  }

  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('P');
  }

  public IRType<T> readType(IRInput in)
  {
    return this;
  }

  public Comparator<T> getComparator() {
    return null;
  }

  public String toString(T o) {
    if (o == null) return "";
    else return o.getID().toString();
  }

  /** 
   * Return the entity given a unique identifier.
   * The code is unable to check that the entity has the correct type.
   * @see edu.cmu.cs.fluid.ir.IRType#fromString(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public T fromString(String s) {
    return (T) IRPersistent.find(UniqueID.parseUniqueID(s));
  }
}
