package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.unparse.Keyword;
import edu.cmu.cs.fluid.unparse.Token;

/**
 * An interface defining SlotInfo-based storage for a parse/check rule
 * 
 * @author chance
 */
public interface IPromiseStorage<T> extends IPromiseRule {
  static final int BOOL = 0;
  static final int INT  = 1;
  static final int NODE = 2;
  static final int SEQ  = 3;

  /**
   * @return The name of the SlotInfo to be created
   */
  String name();

  /**
   * @return One of the constants defined in the interface
   */
  int type();

  TokenInfo<T> set(SlotInfo<T> si);  

  // From JavaPromise
  public static class TokenInfo<T> {
    public final String index;
    public final SlotInfo<T> si;
    public final Token token;
    
    /**
     * @param i The (String) index used to refer to it???
     * @param sinfo The Slotinfo
     * @param t The token printed out when the SlotInfo is unparsed
     */
    public TokenInfo(String i, SlotInfo<T> sinfo, String t) {
      index = i;
      si    = sinfo;
      token = new Keyword(t);
    }
  }
}
