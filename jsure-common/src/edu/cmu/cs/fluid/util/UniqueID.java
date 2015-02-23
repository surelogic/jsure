/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/UniqueID.java,v 1.10 2005/05/25 20:12:16 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

/** A probabilistically unique identifier that can be written to
 * and then read from a file or stream.
 * Each instantiation yields a newly selected number
 * that is very likely to be different from any other instantiation.
 * Currently the user name and current time (in milliseconds) are used with a
 * pseudo-random-number generator to generate a 64-bit long.
 */
public class UniqueID implements Serializable {
  private static final Random rand =
    new Random(System.getProperty("user.name").hashCode());

  /** The value used to distinguish two supposedly unique ids.
   * @serial
   */
  private final long id;

  /** Create a new unique identifier. */
  public UniqueID() {
    id = rand.nextLong() ^ System.currentTimeMillis();
  }
  private UniqueID(long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof UniqueID)) return false;
    UniqueID uid = (UniqueID)other;
    return id == uid.id;
  }

  @Override
  public int hashCode() {
    return (int)((id >>> 32)^id);
  }

  /** Return a new uniqueID combined in a deterministic and
   * unchanging way with another uniqueID.
   */
  public UniqueID combine(UniqueID other) {
    return new UniqueID(id + other.id);
  }

  /** Return a new uniqueID combined in a deterministic and
   * unchanging way with a long
   */
  public UniqueID combine(long val) {
    return new UniqueID(id + val);
  }

  public void write(DataOutput out) throws IOException {
    out.writeLong(id);
  }
  public static UniqueID read(DataInput di) throws IOException {
    return new UniqueID(di.readLong());
  }

  /** Create a readable version of the unique ID
   * that uses letters and numbers exclusively without
   * case sensitivity.
   * @see #parseUniqueID
   */
  @Override
  public String toString() {
    long nid = id;
    if (nid < 0) {
      nid = -nid;
			return "w" + Long.toString(nid,32);
    }
    return Long.toString(nid,32);
  }

  /** Parse a unique ID from the readable version
   * @see #toString()
   */
  public static UniqueID parseUniqueID(String s) {
    boolean negative = false;
    if (s.charAt(0) == 'w') {
      s = s.substring(1);
      negative = true;
    }
    long l = Long.parseLong(s,32);
    if (negative) l = -l;
    return new UniqueID(l);
  }
}
