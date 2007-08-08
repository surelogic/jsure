package test_lock_region;


/**
 * @Region private privateRegion1
 * @Region defaultRegion1
 * @Region protected protectedRegion1
 * @Region public publicRegion1
 * 
 * @Region private privateRegion2
 * @Region defaultRegion2
 * @Region protected protectedRegion2
 * @Region public publicRegion2
 * 
 * @Region private privateRegion3
 * @Region defaultRegion3
 * @Region protected protectedRegion3
 * @Region public publicRegion3
 * 
 * @Region private privateRegion4
 * @Region defaultRegion4
 * @Region protected protectedRegion4
 * @Region public publicRegion4
 * 
 * @Region private privateRegion5
 * @Region defaultRegion5
 * @Region protected protectedRegion5
 * @Region public publicRegion5
 * 
 * @Region private privateRegion6
 * @Region defaultRegion6
 * @Region protected protectedRegion6
 * @Region public publicRegion6
 * 
 * @Region private privateRegion7
 * @Region defaultRegion7
 * @Region protected protectedRegion7
 * @Region public publicRegion7
 * 
 * @Region public publicRegion100
 * @Region public static publicStaticRegion
 * 
 * @TestResult is CONSISTENT: private field >= private region
 * @Lock L1_good is privateField protects privateRegion1
 * @TestResult is CONSISTENT: WARNING: private field < default region
 * @Lock L2_bad is privateField protects defaultRegion1
 * @TestResult is CONSISTENT: WARNING: private field < protected region
 * @Lock L3_bad is privateField protects protectedRegion1
 * @TestResult is CONSISTENT: WARNING: private field < public region
 * @Lock L4_bad is privateField protects publicRegion1
 * 
 * @TestResult is CONSISTENT: default field >= private region
 * @Lock L5_good is defaultField protects privateRegion2
 * @TestResult is CONSISTENT: default field >= default region
 * @Lock L6_good is defaultField protects defaultRegion2
 * @TestResult is CONSISTENT: WARNING: default field < protected region
 * @Lock L7_bad is defaultField protects protectedRegion2
 * @TestResult is CONSISTENT: WARNING: default field < public region
 * @Lock L8_bad is defaultField protects publicRegion2
 * 
 * @TestResult is CONSISTENT: protected field >= private region
 * @Lock L9_good is protectedField protects privateRegion3
 * @TestResult is CONSISTENT: protected field >= default region
 * @Lock L10_good is protectedField protects defaultRegion3
 * @TestResult is CONSISTENT: protected field >= protected region
 * @Lock L11_good is protectedField protects protectedRegion3
 * @TestResult is CONSISTENT: WARNING: protected field < public region
 * @Lock L12_bad is protectedField protects publicRegion3
 * 
 * @TestResult is CONSISTENT: public field >= private region
 * @Lock L13_good is publicField protects privateRegion4
 * @TestResult is CONSISTENT: public field >= default region
 * @Lock L14_good is publicField protects defaultRegion4
 * @TestResult is CONSISTENT: public field >= protected region
 * @Lock L15_good is publicField protects protectedRegion4
 * @TestResult is CONSISTENT: public field >= public region
 * @Lock L16_good is publicField protects publicRegion4
 *
 * @TestResult is CONSISTENT: Receiver is public
 * @Lock L200_good is this protects publicRegion100
 * @TestResult is CONSISTENT: Class reference is public
 * @Lock L201_good is class protects publicStaticRegion
 * 
 * @TestResult is CONSISTENT: effectively default private field >= private region
 * @Lock L100_good is privateFieldMadeDefault protects privateRegion5
 * @TestResult is CONSISTENT: effectively default private field >= default region
 * @Lock L101_good is privateFieldMadeDefault protects defaultRegion5
 *
 * @TestResult is CONSISTENT: effectively protected private field >= private region
 * @Lock L104_good is privateFieldMadeProtected protects privateRegion6
 * @TestResult is CONSISTENT: effectively protected private field >= default region
 * @Lock L105_good is privateFieldMadeProtected protects defaultRegion6
 * @TestResult is CONSISTENT: effectively protected private field >= protected region
 * @Lock L106_good is privateFieldMadeProtected protects protectedRegion6
 * 
 * @TestResult is CONSISTENT: effectively public private field >= private region
 * @Lock L108_good is privateFieldMadePublic protects privateRegion7
 * @TestResult is CONSISTENT: effectively public private field >= default region
 * @Lock L109_good is privateFieldMadePublic protects defaultRegion7
 * @TestResult is CONSISTENT: effectively public private field >= protected region
 * @Lock L110_good is privateFieldMadePublic protects protectedRegion7
 * @TestResult is CONSISTENT: effectively public private field >= public region
 * @Lock L111_good is privateFieldMadePublic protects publicRegion7
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
   * @ReturnsLock L100_good
   */
  Object getPrivateMadeDefault1() {
    return privateFieldMadeDefault;
  }

  /**
   * @ReturnsLock L101_good
   */
  Object getPrivateMadeDefault2() {
    return privateFieldMadeDefault;
  }



  /**
   * @ReturnsLock L104_good
   */
  protected Object getPrivateMadeProtected1() {
    return privateFieldMadeProtected;
  }

  /**
   * @ReturnsLock L105_good
   */
  protected Object getPrivateMadeProtected2() {
    return privateFieldMadeProtected;
  }

  /**
   * @ReturnsLock L106_good
   */
  protected Object getPrivateMadeProtected3() {
    return privateFieldMadeProtected;
  }



  /**
   * @ReturnsLock L108_good
   */
  public Object getPrivateMadePublic1() {
    return privateFieldMadePublic;
  }

  /**
   * @ReturnsLock L109_good
   */
  public Object getPrivateMadePublic2() {
    return privateFieldMadePublic;
  }

  /**
   * @ReturnsLock L110_good
   */
  public Object getPrivateMadePublic3() {
    return privateFieldMadePublic;
  }

  /**
   * @ReturnsLock L111_good
   */
  public Object getPrivateMadePublic4() {
    return privateFieldMadePublic;
  }
}
