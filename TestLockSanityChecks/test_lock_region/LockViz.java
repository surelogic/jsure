package test_lock_region;


/**
 * @region private privateRegion1
 * @region defaultRegion1
 * @region protected protectedRegion1
 * @region public publicRegion1
 * 
 * @region private privateRegion2
 * @region defaultRegion2
 * @region protected protectedRegion2
 * @region public publicRegion2
 * 
 * @region private privateRegion3
 * @region defaultRegion3
 * @region protected protectedRegion3
 * @region public publicRegion3
 * 
 * @region private privateRegion4
 * @region defaultRegion4
 * @region protected protectedRegion4
 * @region public publicRegion4
 * 
 * @region private privateRegion5
 * @region defaultRegion5
 * @region protected protectedRegion5
 * @region public publicRegion5
 * 
 * @region private privateRegion6
 * @region defaultRegion6
 * @region protected protectedRegion6
 * @region public publicRegion6
 * 
 * @region private privateRegion7
 * @region defaultRegion7
 * @region protected protectedRegion7
 * @region public publicRegion7
 * 
 * @region public publicRegion100
 * @region public static publicStaticRegion
 * 
 * @lock L1_good is privateField protects privateRegion1
 * @lock L2_bad is privateField protects defaultRegion1
 * @lock L3_bad is privateField protects protectedRegion1
 * @lock L4_bad is privateField protects publicRegion1
 * 
 * @lock L5_good is defaultField protects privateRegion2
 * @lock L6_good is defaultField protects defaultRegion2
 * @lock L7_bad is defaultField protects protectedRegion2
 * @lock L8_bad is defaultField protects publicRegion2
 * 
 * @lock L9_good is protectedField protects privateRegion3
 * @lock L10_good is protectedField protects defaultRegion3
 * @lock L11_good is protectedField protects protectedRegion3
 * @lock L12_bad is protectedField protects publicRegion3
 * 
 * @lock L13_good is publicField protects privateRegion4
 * @lock L14_good is publicField protects defaultRegion4
 * @lock L15_good is publicField protects protectedRegion4
 * @lock L16_good is publicField protects publicRegion4
 * 
 * @lock L100_good is privateFieldMadeDefault protects privateRegion5
 * @lock L101_good is privateFieldMadeDefault protects defaultRegion5
 * 
 * @lock L104_good is privateFieldMadeProtected protects privateRegion6
 * @lock L105_good is privateFieldMadeProtected protects defaultRegion6
 * @lock L106_good is privateFieldMadeProtected protects protectedRegion6
 * 
 * @lock L108_good is privateFieldMadePublic protects privateRegion7
 * @lock L109_good is privateFieldMadePublic protects defaultRegion7
 * @lock L110_good is privateFieldMadePublic protects protectedRegion7
 * @lock L111_good is privateFieldMadePublic protects publicRegion7
 * 
 * @lock L200_good is this protects publicRegion100
 * @lock L201_good is class protects publicStaticRegion
 */
public class LockViz {
  @SuppressWarnings("unused")
  private final Object privateField = new Object();
  private final Object privateFieldMadeDefault = new Object();
  private final Object privateFieldMadeProtected = new Object();
  private final Object privateFieldMadePublic = new Object();
  
  final Object defaultField = new Object();
  final protected Object protectedField = new Object();
  final public Object publicField = new Object();
 
  
  
  /**
   * @returnsLock L100_good
   */
  Object getPrivateMadeDefault1() {
    return privateFieldMadeDefault;
  }

  /**
   * @returnsLock L101_good
   */
  Object getPrivateMadeDefault2() {
    return privateFieldMadeDefault;
  }



  /**
   * @returnsLock L104_good
   */
  protected Object getPrivateMadeProtected1() {
    return privateFieldMadeProtected;
  }

  /**
   * @returnsLock L105_good
   */
  protected Object getPrivateMadeProtected2() {
    return privateFieldMadeProtected;
  }

  /**
   * @returnsLock L106_good
   */
  protected Object getPrivateMadeProtected3() {
    return privateFieldMadeProtected;
  }



  /**
   * @returnsLock L108_good
   */
  public Object getPrivateMadePublic1() {
    return privateFieldMadePublic;
  }

  /**
   * @returnsLock L109_good
   */
  public Object getPrivateMadePublic2() {
    return privateFieldMadePublic;
  }

  /**
   * @returnsLock L110_good
   */
  public Object getPrivateMadePublic3() {
    return privateFieldMadePublic;
  }

  /**
   * @returnsLock L111_good
   */
  public Object getPrivateMadePublic4() {
    return privateFieldMadePublic;
  }
}
