/**
 * 
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import java.util.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
/**
 * @author WR
 *
 */

public final class ASTDropGenerator{
    
  private final Map<IRNode, ASTDrop> cachedDrops;
  
  public ASTDropGenerator(){
    cachedDrops = new HashMap<IRNode,ASTDrop>();
  }
  
  public static final class ASTDrop extends PromiseDrop {
  
    private String getMsg(IRNode n){
      String nn;
      if(n == null){
        nn = "[null]";
      }else{
        nn = DebugUnparser.toString(n);
      }
      return "Control flow across " + nn;
    }
    private ASTDrop(IRNode astNode){
      this.setMessage(getMsg(astNode));
      this.setNode(astNode);
      this.setCategory(JavaGlobals.UNCATEGORIZED);
      this.dependUponCompilationUnitOf(astNode);
    }
    @Override
    public boolean isIntendedToBeCheckedByAnalysis() {
      return false;
    }  
  }

  public final ASTDrop dropForNode(IRNode n){
    ASTDrop AD = cachedDrops.get(n);
    if(AD == null){
      AD = new ASTDrop(n);
      cachedDrops.put(n,AD);
    }
    return AD;
  }
}
