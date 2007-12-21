package test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;

@Regions({
  @Region("protected static StaticState1"),
  @Region("protected static StaticState2"),
  @Region("protected static StaticState3")
})
@RegionLocks({
  @RegionLock("SL1 is staticLockField1 protects StaticState1"),
  @RegionLock("SL2 is staticLockField2 protects StaticState2"),
  @RegionLock("SL3 is staticLockField3 protects StaticState3")
})
public class StaticTest {
  protected static final Object staticLockField1 = new Object();
  protected static final Lock staticLockField2 = new ReentrantLock();
  protected static final ReadWriteLock staticLockField3 = new ReentrantReadWriteLock();
  
  @InRegion("StaticState1")
  private static int fieldInit = 0;  // GOOD
  
  @InRegion("StaticState2")
  private static int blockInit1; 
  
  @InRegion("StaticState3")
  private static int blockInit2;  
  
  static {
    blockInit1 = 10;  // GOOD
  }
  
  static {
    blockInit2 = 100;  // GOOD
  }
  
  public static int bad() {
    return fieldInit +  // BAD 
      blockInit1 +   // BAD
      blockInit2;  // BAD
  }
}
