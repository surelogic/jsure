package test;

/**
 * Class meant for testing the uniqueness/borrowed assurances.
 * This is mean to hit every case that should be assured and 
 * every case that should fail.  We separate annotated parameters
 * from annotated receiver ('this') because of problems that have
 * occurred in the implementation.  We test constructors separately
 * from methods as well.
 */
public class UniquenessTests {
  /**
   * An unshared field.
   * @Unique
   */
  private Object unshared;
  
  /** A shared field. */
  private Object shared;
  
  
  
  /*
   * 
   * Constructors, all correct
   * 
   */
   
  /**
   * Error-free: initialize both fields to null.
   */
  public UniquenessTests() {
    unshared = null;
    shared = null;
  }
  
  /**
   * Error-free: initialize unshared field to fresh object.
   */
  public UniquenessTests( int bogus ) {
    unshared = new Object();
    shared = null;
  }
  
  /**
   * Error-free: initialize unshared field from a unique parameter.
   * @Unique p
   */
  public UniquenessTests( Object p ) {
    unshared = p;
    shared = null;
  }
  
  /**
   * Error-free: initialize unshared field from a unique receiver.
   * (In general, this is stupid, but it is legal.  The returned object
   * will be unusable at the call site.)
   * @Unique this
   */
  public UniquenessTests( int bogus1, int bogus2 ) {
    shared = null;
    unshared = this;
  }
  
  
  /**
   * Error: the receiver will become useless after being assigned to 
   * field "unshared", so we cannot reference "this.shared".
   * @Unique this
   */
  public UniquenessTests( int bogus1, int bogus2, int b3, int b4 ) {
    unshared = this;
    shared = null;
  }

  /**
   * Error-free: initialize unshared field from a method declared to return
   * a unique value.
   */
  public UniquenessTests( int b1, int b2, int b3 ) {
    unshared = uniqueValue1();
    shared = null;
  }
  
  
  
  /*
   * 
   * Methods that correctly assign a unique value to an unshared field
   * 
   */
   
  /**
   * Error-free: assign null.
   */
  public void setUnshared1() {
    unshared = null;
  }
  
  /**
   * Error-free: assign a fresh object.
   */
  public void setUnshared2() {
    unshared = new Object();
  }
  
  /**
   * Error-free: assign from a unique parameter.
   * @Unique p
   */
  public void setUnshared3( Object p ) {
    unshared = p;
  }
  
  /**
   * Error-free: assign from a unique receiver.
   * (In general, this is stupid, but it is legal.  The received becomes
   * useless at the call site.)
   * @Unique this
   */
  public void setUnshared4() {
    shared = null;
    unshared = this;
  }
  
  /**
   * Error-free: set from a method declared to return a unique value.
   */
  public void setUnshared5() {
    unshared = uniqueValue1();
  }



  /*
   * 
   * Methods that correctly return a unique value.
   * 
   */
   
  /**
   * Error-free: method returns a newly created object.
   * @Unique return
   */
  public Object uniqueValue1() {
    return new Object();
  }
  
  /**
   * Error-free: method returns null
   * @Unique return
   */
  public Object uniqueValue2() {
    return null;
  }
  
  /**
   * Error-free: method returns a unique parameter.
   * @Unique p, return
   */
  public Object uniqueValue3( Object p ) {
    return p;
  }
  
  /**
   * Error-free: method returns a unique receiver.
   * @Unique this, return
   */
  public Object uniqueValue4() {
    return this;
  }
  
  /**
   * Error-free: calls another unique-value returning method.
   * @Unique return
   */
  public Object uniqueValue5() {
    return uniqueValue1();
  }
  
  
  
  /*
   * 
   * Methods that incorrectly return a shared value.
   * 
   */

  /**
   * Error: returns a shared field.
   * @Unique return
   */
  public Object bad_notUniqueValue1() {
    return shared;
  }

  /**
   * Error: returns a shared parameter.
   * @Unique return
   */
  public Object bad_notUniqueValue2( Object p ) {
    return p;
  }

  /**
   * Error: returns a shared method return value.
   * @Unique return
   */
  public Object bad_notUniqueValue3() {
    return sharedValue();
  }

  /**
   * Error: returns a shared receiver.
   * @Unique return
   */
  public Object bad_notUniqueValue4() {
    return this;
  }
  
  
  
  /*
   * 
   * Method that correctly returns a shared value
   * 
   */

  /**
   * Error-free: returns a shared value;
   * not declared to return a unique value.
   */
  public Object sharedValue() {
    return shared;
  }
  
  
  
  /*
   * 
   * Methods that assign shared values to unshared fields.
   * 
   */
   
  /**
   * Error: assigns a shared field to the unshared field.
   */
  public void bad_assignSharedToUnshared1() {
    unshared = shared;
  }
   
  /**
   * Error: assigns a shared parameter to the unshared field.
   */
  public void bad_assignSharedToUnshared2( Object p ) {
    unshared = p;
  }
   
  /**
   * Error: assigns a shared receiver to the unshared field.
   */
  public void bad_assignSharedToUnshared3() {
    unshared = this;
  }
   
  /**
   * Error: assigns a shared return-value to the unshared field.
   */
  public void bad_assignSharedToUnshared4() {
    unshared = sharedValue();
  }
  
  
  
  /*
   * 
   * Methods that compromise an unshared field.
   * 
   */

  /**
   * Error: Assign an unshared field to shared field.
   */
  public void bad_compromiseUnsharedField1() {
    shared = unshared;
  }

  /**
   * Error: return an unshared field
   */
  public Object bad_compromiseUnsharedField2() {
    return unshared;
  }



  /*
   * 
   * Methods that do not compromise an unshared field even though
   * they assign from it.
   * 
   */

  /**
   * Error-free: Temporarily alias an unshared field in a local.
   */
  public void okay1() {
//    @SuppressWarnings("unused")
    Object o = unshared;
  }
  
  /**
   * Error-free: Destructive read into a shared field.
   */
  public void okay2() {
    Object o = unshared;
    unshared = new Object();
    shared = o;
  }



  /*
   * 
   * Methods that compromise borrowed parameters
   * 
   */

  /**
   * Error: Assign an borrowed parameter to a shared field.
   * (Also an error to assign to an unshared field, but doing so is also
   * erroneous because it assigns a potentially shared value to the unshared
   * field.)
   * @Borrowed p
   */
  public void bad_compromiseBorrowedParam1( Object p ) {
    shared = p;
  }

  /**
   * Error: return a borrowed param
   * @Borrowed p
   */
  public Object bad_compromiseBorrowedParam2( Object p ) {
    return p;
  }
  
  /**
   * Error: No such thing as a destructive read of a borrowed.
   * @Borrowed p
   */
  public void bad_compromiseBorrowedParam3( Object p ) {
    Object o = p;
    p = new Object();
    shared = o;
  }



  /*
   * 
   * Methods that do not compromise a borrowed parameter even though
   * they assign from it.
   * 
   */

  /**
   * Error-free: Temporarily alias a borrowed parameter in a local.
   * @Borrowed p
   */
  public void okay3( Object p ) {
//    @SuppressWarnings("unused")
    Object o = p;
  }
  
  
  
  /*
   * 
   * Methods that compromise borrowed receivers
   * 
   */

  /**
   * Error: Assign an borrowed receiver to a shared field.
   * (Also an error to assign to an unshared field, but doing so is also
   * erroneous because it assigns a potentially shared value to the unshared
   * field.)
   * @Borrowed this
   */
  public void bad_compromiseBorrowedRcvr1() {
    shared = this;
  }

  /**
   * Error: return a borrowed receiver
   * @Borrowed this
   */
  public Object bad_compromiseBorrowedRcvr2() {
    return this;
  }

  /*
   * 
   * Methods that compromise unique receivers
   * 
   */

  /**
   * Error: The receiver becomes useless after being assigned to unshared
   * @Unique this
   */
  public void bad_compromiseUniqueReceiver() {
    unshared = this;
    shared = null;
  }


  /*
   * 
   * Methods that do not compromise a borrowed receiver even though
   * they assign from it.
   * 
   */

  /**
   * Error-free: Temporarily alias a borrowed receiver in a local.
   * @Borrowed this
   */
  public void okay4() {
//    @SuppressWarnings("unused")
    Object o = this;
  }
  
  
  
  /**
   * Error-free
   */
  public void fancyControlFlow() {
    // Create a new unshared object, and pass it around
    Object o = new Object();
    Object o2 = o;
    o = null;
    Object o3 = o2;
    o2 = null;
    okay3(o3);
    // o3 is still unique
    this.unshared = o3;
  }
}
