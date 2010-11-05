package testFieldDeclarations;

import com.surelogic.AggregateInRegion;
import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

@Regions({
		@Region("public LockProtected"),
		@Region("public NotProtected")
})
@RegionLock("L is this protects LockProtected")
@ThreadSafe
public class Test {
	@Unique("return")
	public Test() {
		super();
	}
	
	
	
	// =======================================================================
	// == Final fields
	// =======================================================================
	
  /* GOOD: final field; primitive type */
  protected final int final1 = 1;

  /* GOOD: final field; ThreadSafe type */
  protected final Safe final2 = new Safe();
  
  /* GOOD: final field; containable aggregated type */
  @Unique
  @AggregateInRegion("LockProtected")
  protected final ContainableType final3 = new ContainableType();
  
  
  
  /* BAD: final field; non-primitive, non-declared type */
  protected final int[] final4 = { 0, 1, 2 };
  
  /* BAD: final field; unsafe type */
  protected final NotSafe final5 = new NotSafe();
  
  /* BAD: final field; containable, but not aggregated */
  @Unique
  protected final ContainableType final6 = new ContainableType();
  
  /* BAD: final field; containable, but not unique */
  protected final ContainableType final7 = new ContainableType();
  
  /* BAD: final field; containable, unique, but not aggregated into a lock-protected region */
  @Unique
  @AggregateInRegion("NotProtected")
  protected final ContainableType final8 = new ContainableType();

  
  
	// =======================================================================
	// == Volatile fields
	// =======================================================================
	
  /* GOOD: volatile field; primitive type */
  protected volatile int volatile1 = 1;
  
  /* GOOD: volatile field; ThreadSafe type */
  protected volatile Safe volatile2 = new Safe();
  
  // volatile fields cannot be unique, so we cannot aggregate a Contained type
//  /* GOOD: volatile field; containable aggregated type */
//  @Unique
//  @AggregateInRegion("LockProtected")
//  protected volatile ContainableType volatile3 = new ContainableType();

  
  
  /* BAD: volatile field; non-primitive, non-declared type */
  protected volatile int[] volatile4 = { 0, 1, 2 };
  
  /* BAD: volatile field; unsafe type */
  protected volatile NotSafe volatile5 = new NotSafe();

  // volatile fields cannot be unique, so we cannot aggregate a Contained type
//  /* BAD: volatile field; containable, but not aggregated */
//  @Unique
//  protected volatile ContainableType volatile6 = new ContainableType();

  /* BAD: volatile field; containable, but not unique */
  protected volatile ContainableType volatile7 = new ContainableType();

  // volatile fields cannot be unique, so we cannot aggregate a Contained type
//  /* BAD: volatile field; containable, unique, but not aggregated into a lock-protected region */
//  @Unique
//  @AggregateInRegion("NotProtected")
//  protected volatile ContainableType volatile8 = new ContainableType();


  
	// =======================================================================
	// == Lock-protected fields
	// =======================================================================
	
  /* GOOD: lockProtected field; primitive type */
  @InRegion("LockProtected")
  protected int lockProtected1 = 1;

  /* GOOD: lockProtected field; ThreadSafe type */
  @InRegion("LockProtected")
  protected Safe lockProtected2 = new Safe();
  
  /* GOOD: lockProtected field; containable aggregated type */
  @InRegion("LockProtected")
  @Unique
  @AggregateInRegion("LockProtected")
  protected ContainableType lockProtected3 = new ContainableType();
  
  
  
  /* BAD: lockProtected field; non-primitive, non-declared type */
  @InRegion("LockProtected")
  protected int[] lockProtected4 = { 0, 1, 2 };
  
  /* BAD: lockProtected field; unsafe type */
  @InRegion("LockProtected")
  protected NotSafe lockProtected5 = new NotSafe();
  
  /* BAD: lockProtected field; containable, but not aggregated */
  @InRegion("LockProtected")
  @Unique
  protected ContainableType lockProtected6 = new ContainableType();
  
  /* BAD: lockProtected field; containable, but not unique */
  @InRegion("LockProtected")
  protected ContainableType lockProtected7 = new ContainableType();
  
  /* BAD: lockProtected field; containable, unique, but not aggregated into a lock-protected region */
  @InRegion("LockProtected")
  @Unique
  @AggregateInRegion("Instance")
  protected ContainableType lockProtected8 = new ContainableType();



	// =======================================================================
	// == Non-final, non-volatile, not-Lock-protected fields
	// =======================================================================
	
  /* BAD: primitive type */
  protected int bad1 = 1;

  /* BAD: ThreadSafe type */
  protected Safe bad2 = new Safe();
  
  /* Impossible case because there is no way to aggregate the object into 
   * a lock-protected region while still leaving field bad3 unprotected by a
   * lock. 
   */
//  /* BAD:containable aggregated type */
//  @Unique
//  @AggregateInRegion("LockProtected")
//  protected ContainableType bad3 = new ContainableType();
  
  
  
  /* BAD: non-primitive, non-declared type */
  protected int[] bad4 = { 0, 1, 2 };
  
  /* BAD: unsafe type */
  protected NotSafe bad5 = new NotSafe();
  
  /* BAD: containable, but not aggregated */
  @Unique
  protected ContainableType bad6 = new ContainableType();
  
  /* BAD: containable, but not unique */
  protected ContainableType bad7 = new ContainableType();
  
  /* BAD: containable, unique, but not aggregated into a lock-protected region */
  @Unique
  @AggregateInRegion("bad8")
  protected ContainableType bad8 = new ContainableType();
	
}
