/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IndependentIRNode.java,v 1.8 2006/03/28 20:19:02 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.util.UniqueID;

/** An IRNode which is referenced by a unique ID.
 * It lives in a region by itself.
 */
@ThreadSafe
public class IndependentIRNode extends IRRegion implements IRNode {
  private static final int magic = 0x4949524e; // IIRN

  /* storage kind */

  private static IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      ((IndependentIRNode) p).getID().write(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      UniqueID id = UniqueID.read(in);
      IRPersistent p = find(id);
      if (p == null)
        p = new IndependentIRNode(id);
      return p;
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x49); // 'I'
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  /* instance variables */

  private final IRNode base = new PlainIRNode(null);

  protected IRNode getBase() {
    return base;
  }

  /* constructors */

  public IndependentIRNode() {
    super(magic);
    super.complete(new IRNode[] { this });
  }

  protected IndependentIRNode(UniqueID id) {
    super(magic, id);
    super.complete(new IRNode[] { this });
  }

  /* Object methods */

  @Override
  public boolean equals(Object other) {
    return base.equals(other);
  }

  @Override
  public final int hashCode() {
    return base.hashCode();
  }

  /* IR node methods */

  public Object identity() {
    return base;
  }

  public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException {
    return base.getSlotValue(si);
  }

  public <T> void setSlotValue(SlotInfo<T> si, T newValue)
    throws SlotImmutableException {
    base.setSlotValue(si, newValue);
  }

  public int getIntSlotValue(SlotInfo<Integer> si) throws SlotUndefinedException {
    return base.getIntSlotValue(si);
  }

  public void setSlotValue(SlotInfo<Integer> si, int newValue)
    throws SlotImmutableException {
    base.setSlotValue(si, newValue);
  }

  public <T> boolean valueExists(SlotInfo<T> si) {
    return base.valueExists(si);
  }
}