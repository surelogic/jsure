package test_lock_region;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLocks;
import com.surelogic.Region;
import com.surelogic.Regions;
//import com.surelogic.ReturnsLock;
import com.surelogic.ReturnsLock;


@Regions({
  @Region("private privateRegion1"),
  @Region("defaultRegion1"),
  @Region("protected protectedRegion1"),
  @Region("public publicRegion1"),
  @Region("private privateRegion2"),
  @Region("defaultRegion2"),
  @Region("protected protectedRegion2"),
  @Region("public publicRegion2"),
  @Region("private privateRegion3"),
  @Region("defaultRegion3"),
  @Region("protected protectedRegion3"),
  @Region("public publicRegion3"),
  @Region("private privateRegion4"),
  @Region("defaultRegion4"),
  @Region("protected protectedRegion4"),
  @Region("public publicRegion4"),
  @Region("private privateRegion5"),
  @Region("defaultRegion5"),
  @Region("protected protectedRegion5"),
  @Region("public publicRegion5"),
  @Region("private privateRegion6"),
  @Region("defaultRegion6"),
  @Region("protected protectedRegion6"),
  @Region("public publicRegion6"),
  @Region("private privateRegion7"),
  @Region("defaultRegion7"),
  @Region("protected protectedRegion7"),
  @Region("public publicRegion7"),
  @Region("public publicRegion100"),
  @Region("public static publicStaticRegion")
})
@RegionLocks({
  @com.surelogic.RegionLock("L1_good is privateField protects privateRegion1" /* is CONSISTENT: private field >= private region */),
  @com.surelogic.RegionLock("L2_bad is privateField protects defaultRegion1" /* is CONSISTENT: WARN: private field < default region */),
  @com.surelogic.RegionLock("L3_bad is privateField protects protectedRegion1" /* is CONSISTENT: WARN: private field < protected region */),
  @com.surelogic.RegionLock("L4_bad is privateField protects publicRegion1" /* is CONSISTENT: WARN: private field < public region */),
  @com.surelogic.RegionLock("L5_good is defaultField protects privateRegion2" /* is CONSISTENT: default field >= private region */),
  @com.surelogic.RegionLock("L6_good is defaultField protects defaultRegion2" /* is CONSISTENT: default field >= default region */),
  @com.surelogic.RegionLock("L7_bad is defaultField protects protectedRegion2" /* is CONSISTENT: WARN: default field < protected region */),
  @com.surelogic.RegionLock("L8_bad is defaultField protects publicRegion2" /* is CONSISTENT: WARN: default field < public region */),
  @com.surelogic.RegionLock("L9_good is protectedField protects privateRegion3" /* is CONSISTENT: protected field >= private region */),
  @com.surelogic.RegionLock("L10_good is protectedField protects defaultRegion3" /* is CONSISTENT: protected field >= default region */),
  @com.surelogic.RegionLock("L11_good is protectedField protects protectedRegion3" /* is CONSISTENT: protected field >= protected region */),
  @com.surelogic.RegionLock("L12_bad is protectedField protects publicRegion3" /* is CONSISTENT: WARN: protected field < public region */),
  @com.surelogic.RegionLock("L13_good is publicField protects privateRegion4" /* is CONSISTENT: public field >= private region */),
  @com.surelogic.RegionLock("L14_good is publicField protects defaultRegion4" /* is CONSISTENT: public field >= default region */),
  @com.surelogic.RegionLock("L15_good is publicField protects protectedRegion4" /* is CONSISTENT: public field >= protected region */),
  @com.surelogic.RegionLock("L16_good is publicField protects publicRegion4" /* is CONSISTENT: public field >= public region */),
  @com.surelogic.RegionLock("L200_good is this protects publicRegion100" /* is CONSISTENT: Receiver is public */),
  @com.surelogic.RegionLock("L201_good is class protects publicStaticRegion" /* is CONSISTENT: Class reference is public */),
  @com.surelogic.RegionLock("L100_good is privateFieldMadeDefault protects privateRegion5" /* is CONSISTENT: effectively default private field >= private region */),
  @com.surelogic.RegionLock("L101_good is privateFieldMadeDefault protects defaultRegion5" /* is CONSISTENT: effectively default private field >= default region */),
  @com.surelogic.RegionLock("L104_good is privateFieldMadeProtected protects privateRegion6" /* is CONSISTENT: effectively protected private field >= private region */),
  @com.surelogic.RegionLock("L105_good is privateFieldMadeProtected protects defaultRegion6" /* is CONSISTENT: effectively protected private field >= default region */),
  @com.surelogic.RegionLock("L106_good is privateFieldMadeProtected protects protectedRegion6" /* is CONSISTENT: effectively protected private field >= protected region */),
  @com.surelogic.RegionLock("L108_good is privateFieldMadePublic protects privateRegion7" /* is CONSISTENT: effectively public private field >= private region */),
  @com.surelogic.RegionLock("L109_good is privateFieldMadePublic protects defaultRegion7" /* is CONSISTENT: effectively public private field >= default region */),
  @com.surelogic.RegionLock("L110_good is privateFieldMadePublic protects protectedRegion7" /* is CONSISTENT: effectively public private field >= protected region */),
  @com.surelogic.RegionLock("L111_good is privateFieldMadePublic protects publicRegion7" /* is CONSISTENT: effectively public private field >= public region */)
})
public class LockVizJUC {
  @SuppressWarnings("unused")
  private final Lock privateField = new ReentrantLock();
  private final Lock privateFieldMadeDefault = new ReentrantLock();
  private final Lock privateFieldMadeProtected = new ReentrantLock();
  private final Lock privateFieldMadePublic = new ReentrantLock();
  
  final Object defaultField = new Object();
  final protected Object protectedField = new Object();
  final public Object publicField = new Object();
 
  
  
  @ReturnsLock("L100_good")
  Lock getPrivateMadeDefault1() {
    return privateFieldMadeDefault;
  }

  @ReturnsLock("L101_good")
  Lock getPrivateMadeDefault2() {
    return privateFieldMadeDefault;
  }



  @ReturnsLock("L104_good")
  protected Lock getPrivateMadeProtected1() {
    return privateFieldMadeProtected;
  }

  @ReturnsLock("L105_good")
  protected Lock getPrivateMadeProtected2() {
    return privateFieldMadeProtected;
  }

  @ReturnsLock("L106_good")
  protected Lock getPrivateMadeProtected3() {
    return privateFieldMadeProtected;
  }



  @ReturnsLock("L108_good")
  public Lock getPrivateMadePublic1() {
    return privateFieldMadePublic;
  }

  @ReturnsLock("L109_good")
  public Lock getPrivateMadePublic2() {
    return privateFieldMadePublic;
  }

  @ReturnsLock("L110_good")
  public Lock getPrivateMadePublic3() {
    return privateFieldMadePublic;
  }

  @ReturnsLock("L111_good")
  public Lock getPrivateMadePublic4() {
    return privateFieldMadePublic;
  }
}
