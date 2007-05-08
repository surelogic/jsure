package test;

/* Test calling methods and constructors that require unique parameters.
 * Treat constructors and methods as separate cases.  Treat receiver
 * separately from regulat parameters.
 */
public class UniqueParams {
  /**
   * Good
   * @borrowed this
   */
  public UniqueParams() {
    // do nothing
  }
  
  /** 
   * Good.
   * @unique p
   */
  public UniqueParams(final Object p) {
    // do nothing
  }
  
  /**
   * Good
   * @unique this
   */
  public void needsUniqueReceiver() {
    // do nothing
  }
  
  /**
   * Good
   * @unique p
   */
  public static void needsUniqueParam(final Object p) {
    // do nothing
  }

  
  
  /**
   * Good!
   * @return {@unique}
   */
  private UniqueParams getUnique() {
    return new UniqueParams();
  }
  
  /**
   * Good.  Get a possibly shared object.
   */
  private UniqueParams getShared() {
    return null;
  }
  
  /**
   * Good.
   * Invalidates unique objects.
   */
  private void invalidateUniqueObject(final Object o) {
    // Do nothing
  }

  
  
  /** Good: Param gets a fresh object */
  public void goodUniqueParam1() {
    final Object o = new Object();
    needsUniqueParam(o);
  }
  
  /**
   * Good: Gets a unique parameter.
   * @unique o
   * @param o
   */
  public void goodUniqueParam2(final Object o) {
    needsUniqueParam(o);
  }
  
  /**
   * Good: Gets a unique return object.
   */
  public void goodUniqueParam3() {
    needsUniqueParam(getUnique());
  }
  
  /**
   * Good: gets a unique receiver
   * @unique this
   */
  public void goodUniqueParam4() {
    needsUniqueParam(this);
  }
  
  /**
   * BAD: Newly created object is compromised.
   */
  public void badUniqueParam1() {
    final Object o = new Object();
    invalidateUniqueObject(o);
    needsUniqueParam(o);
  }

  
  /**
   * BAD: unique parameter is compromised first.
   * @unique o
   * @param o
   */
  public void badUniqueParam2(final Object o) {
    invalidateUniqueObject(o);
    needsUniqueParam(o);
  }
  
  /**
   * BAD: unique return object is compromised first.
   */
  public void badUniqueParam3() {
    final Object o = getUnique();
    invalidateUniqueObject(o);
    needsUniqueParam(o);
  }
  
  /**
   * BAD: Object was never unique.
   */
  public void badUniqueParam4() {
    final Object o = getShared();
    needsUniqueParam(o);
  }

  /**
   * BAD: unique receiver is compromised first.
   * @unique this
   */
  public void badUniqueParam5() {
    invalidateUniqueObject(this);
    needsUniqueParam(this);
  }

  

  
  
  /** Good: Param gets a fresh object */
  public void goodUniqueReceiver1() {
    final UniqueParams o = new UniqueParams();
    o.needsUniqueReceiver();
  }
  
  /**
   * Good: Gets a unique parameter.
   * @unique o
   * @param o
   */
  public void goodUniqueReceiver2(final UniqueParams o) {
    o.needsUniqueReceiver();
  }
  
  /**
   * Good: Gets a unique return object.
   */
  public void goodUniqueReceiver3() {
    getUnique().needsUniqueReceiver();
  }
  
  /**
   * Good: gets a unique receiver
   * @unique this
   */
  public void goodUniqueReceiver4() {
    this.needsUniqueReceiver();
  }
  
  /**
   * BAD: Newly created object is compromised.
   */
  public void badUniqueReceiver1() {
    final UniqueParams o = new UniqueParams();
    invalidateUniqueObject(o);
    o.needsUniqueReceiver();
  }

  
  /**
   * BAD: unique parameter is compromised first.
   * @unique o
   * @param o
   */
  public void badUniqueReceiver2(final UniqueParams o) {
    invalidateUniqueObject(o);
    o.needsUniqueReceiver();
  }
  
  /**
   * BAD: unique return object is compromised first.
   */
  public void badUniqueReceiver3() {
    final UniqueParams o = getUnique();
    invalidateUniqueObject(o);
    o.needsUniqueReceiver();
  }
  
  /**
   * BAD: Object was never unique.
   */
  public void badUniqueReceiver4() {
    final UniqueParams o = getShared();
    o.needsUniqueReceiver();
  }

  /**
   * BAD: unique receiver is compromised first.
   * @unique this
   */
  public void badUniqueReceiver5() {
    invalidateUniqueObject(this);
    this.needsUniqueReceiver();
  }



  /** Good: Param gets a fresh object */
  public void goodConstructorUniqueParam1() {
    final Object o = new Object();
    new UniqueParams(o);
  }
  
  /**
   * Good: Gets a unique parameter.
   * @unique o
   * @param o
   */
  public void goodConstructorUniqueParam2(final Object o) {
    new UniqueParams(o);
  }
  
  /**
   * Good: Gets a unique return object.
   */
  public void goodConstructorUniqueParam3() {
    new UniqueParams(getUnique());
  }
  
  /**
   * Good: gets a unique receiver
   * @unique this
   */
  public void goodConstructorUniqueParam4() {
    new UniqueParams(this);
  }
  
  /**
   * BAD: Newly created object is compromised.
   */
  public void badConstructorUniqueParam1() {
    final Object o = new Object();
    invalidateUniqueObject(o);
    new UniqueParams(o);
  }

  
  /**
   * BAD: unique parameter is compromised first.
   * @unique o
   * @param o
   */
  public void badConstructorUniqueParam2(final Object o) {
    invalidateUniqueObject(o);
    new UniqueParams(o);
  }
  
  /**
   * BAD: unique return object is compromised first.
   */
  public void badConstructorUniqueParam3() {
    final Object o = getUnique();
    invalidateUniqueObject(o);
    new UniqueParams(o);
  }
  
  /**
   * BAD: Object was never unique.
   */
  public void badConstructorUniqueParam4() {
    final Object o = getShared();
    new UniqueParams(o);
  }

  /**
   * BAD: unique receiver is compromised first.
   * @unique this
   */
  public void badConstructorUniqueParam5() {
    invalidateUniqueObject(this);
    new UniqueParams(this);
  }
}
