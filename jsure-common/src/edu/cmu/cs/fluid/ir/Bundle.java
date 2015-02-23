/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/Bundle.java,v 1.12 2007/04/12 19:21:48 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.UniqueID;
import com.surelogic.ThreadSafe;

/** A bundle is a group of persistable attributes that
 * are saved together.  Bundles are disjoint; every attribute
 * belongs to at most one bundle.  Bundles can be stored
 * persistently.  Every bundle has a unique id.
 */
@ThreadSafe
@SuppressWarnings("unchecked")
public class Bundle extends IRPersistent {
  private static final int magic = 0x49524200; // "IRB\0"

  /** Create a new bundle and add to it. */
  public Bundle() {
    super(magic,true);
  }

  /** Create a bundle to be loaded. */
  protected Bundle(UniqueID id) {
    super(magic,id);
  }

  public static Bundle loadBundle(UniqueID id, FileLocator floc)
       throws IOException
  {
    Bundle b = (Bundle)find(id);
    if (b == null) {
      b = new Bundle(id);
    }
    b.load(floc);
    return b;
  }

  public static Bundle findBundle(UniqueID id) {
	return (Bundle) find(id);
  }
    
  private Vector<SlotInfo> attributes = new Vector<SlotInfo>();

  /** Return number of attributes in bundle. */
  public int getNumAttributes() {
    return attributes.size();
  }

  /** Return attribute #1 (one based. */
  public SlotInfo getAttribute(int index) {
    return attributes.elementAt(index-1);
  }

  /** Return an enumeration of attributes. */
  public Enumeration attributes() {
    return attributes.elements();
  }

  public boolean isInBundle(SlotInfo si) {
    return attributes.contains(si);
  }
  
  /** Ensure attribute in bundle.
   * @throws FluidRuntimeException if bundle complete or attribute
   * already in a different bundle or if attribute not persistent.
   */
  public boolean saveAttribute(SlotInfo attr) {
    if (attributes.contains(attr)) return false;
    if (isComplete()) {
      throw new FluidRuntimeException("bundle cannot be added to");
    }
    if (!(attr instanceof PersistentSlotInfo)) {
      throw new FluidRuntimeException("attribute not persistent: "+attr.getClass().getName());
    }
    attr.setBundle(this);
    attributes.addElement(attr);
    return true;
  }


  /** The file name for saving information on attributes. */
  @Override
  protected String getFileName() {
    return getID().toString() + ".ab";
  }

  /** Writing the bundle concists of writing information
   * on attributes.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void write(IROutput out) throws IOException {
    complete();
    int n = getNumAttributes();
    out.writeInt(n);
    for (int i=1; i <= n; ++i) {
      SlotInfo si = getAttribute(i);
      PersistentSlotInfo psi = (PersistentSlotInfo)si;
      out.writeUTF(si.name());
      out.writeIRType(si.type());
      if (psi.getSlotFactory() == null) {
        LOG.severe("Attribute has null sf: " + si);
      }
      out.writeSlotFactory(psi.getSlotFactory());
      if (psi.isPredefined()) {
	out.writeBoolean(true);
	si.type().writeValue(psi.getDefaultValue(),out);
      } else {
	out.writeBoolean(false);
      }
    }
  }

  /** Reading the bundle consists of reading names etc of attributes.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void read(IRInput in) throws IOException {
    int n = in.readInt();
    for (int i=1; i <= n; ++i) {
      String name = in.readUTF();
      IRType ty = in.readIRType();
      SlotFactory sf = in.readSlotFactory();
      boolean hasDefault = in.readBoolean();
      Object val = null;
      if (hasDefault) {
	val = ty.readValue(in);
      }
      SlotInfo si;
      try {
	si = SlotInfo.findSlotInfo(name);
      } catch (SlotNotRegisteredException e) {
	try {
	  si = hasDefault ?
	    sf.newAttribute(name,ty,val) :
	    sf.newAttribute(name,ty);
	} catch (SlotAlreadyRegisteredException ex) {
	  throw new FluidError("impossible");
	}
      }
      saveAttribute(si);
    }
  }

  /* Unloading */

  @Override
  public void undefine()
  {
    Iterator<SlotInfo> it;
    synchronized (this) {
      it = attributes.iterator();
      attributes = new Vector<SlotInfo>();
    }
    while (it.hasNext()) {
      SlotInfo si = it.next();
      si.setBundle(null);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  /* persistent kind */

  
  private static final IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException
    {
      p.getID().write(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException
    {
      UniqueID id = UniqueID.read(in);
      IRPersistent p = find(id);
      if (p == null) {
	p = new Bundle(id);	
      }
      return p;
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind,0x22); // '"' (accidentally)
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    int n = getNumAttributes();
    out.println("Bundle contains " + n +
		(n == 1 ? " attribute" : " attributes"));
    for (int i=1; i <= n; ++i) {
      out.println("  " + i + ": " + getAttribute(i).name());
    }
  }

  public static void ensureLoaded() {}
}
