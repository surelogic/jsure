package test_lock_field;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.Region;
import com.surelogic.Regions;

/**
 * Test that lock field must be "this", "class", or a final field.
 * Test for both state and policy locks.  Here we don't worry about
 * inherited fields or fields from other objects.
 */
@Regions({
  @Region("R1"),
  @Region("R2"),
  @Region("R3"),
  @Region("R4"),
  @Region("R5"),
  @Region("R6"),
  @Region("R10"),
  @Region("R11"),
  @Region("R12"),
  @Region("R13"),
  @Region("R14"),
  @Region("R15"),
  @Region("R16"),
  @Region("R17"),
  @Region("static S1"),
  @Region("static S2"),
  @Region("static S3"),
  @Region("static S4"),
  @Region("static S5"),
  @Region("R100 "),
  @Region("R101"),
  @Region("R102"),
  @Region("R103")
})
@RegionLocks({
  @RegionLock("L1 is unknownField protects R1" /* is UNBOUND: Field is undefined (Instance Region) */),
  @RegionLock("L2 is this protects R2" /* is CONSISTENT: receiver (Instance Region) */),
  @RegionLock("L3 is goodField protects R3" /* is CONSISTENT: known final field (Instance Region) */),
  @RegionLock("L4 is badField protects R4" /* is UNASSOCIATED: field is non-final (Instance Region) */),
  @RegionLock("L5 is unknownField protects S1" /* is UNBOUND: Field is undefined (Static region) */),
  @RegionLock("L6 is class protects S2" /* is CONSISTENT: class (Static region) */),
  @RegionLock("L7 is goodStatic protects S3" /* is CONSISTENT: known final static field (static region) */),
  @RegionLock("L8 is badStatic protects S4" /* is UNASSOCIATED: non-final field (static region) */),
  @RegionLock("L9 is nonObjectField protects R5" /* is UNASSOCIATED: primitively typed field */),
  @RegionLock("L10 is test_lock_field.Other.staticField protects S5" /* is CONSISTENT: Static locks can use fields from other classes */),
  @RegionLock("L11 is test_lock_field.Other.otherField protects R6" /* is UNASSOCIATED: Instance locks cannot use fields from other classes */),
  @RegionLock("L12 is fieldFromA protects R10" /* is CONSISTENT: Can use field from GP */),
  @RegionLock("L13 is fieldFromB protects R11" /* is CONSISTENT: Can use field from Parent */),
  @RegionLock("L14 is fieldFromD protects R12" /* is UNBOUND: Field is unknown */),
  @RegionLock("L15 is test_lock_field.D.fieldFromD protects R13" /* is UNASSOCIATED: Cannot use a field from a child */),
  @RegionLock("L16 is badFieldFromA protects R14" /* is UNASSOCIATED: non-final field from GP */),
  @RegionLock("L17 is badFieldFromB protects R15" /* is UNASSOCIATED: non-final field from parent */),
  @RegionLock("L18 is badFieldFromD protects R16" /* is UNBOUND: unknown field */),
  @RegionLock("L19 is test_lock_field.D.badFieldFromD protects R17" /* is UNASSOCIATED: non-final field from child */),
  @RegionLock("L100 is class protects R100" /* is UNASSOCIATED: class cannot protect an instance region */),
  @RegionLock("L101 is goodStatic protects R101" /* is UNASSOCIATED: static field cannot protect an instance region */),
  @RegionLock("L102 is test_lock_field.C.goodStatic protects R102" /* is UNASSOCIATED: static field cannot protect an instance region */),
  @RegionLock("L103 is test_lock_field.Other.staticField protects R103" /* is UNASSOCIATED: static field cannot protect an instance region */)
})
@PolicyLocks({
  @PolicyLock("P1 is unknownField" /* is UNBOUND: Field is undefined (Instance Region) */),
  @PolicyLock("P2 is this" /* is CONSISTENT: receiver (Instance Region) */),
  @PolicyLock("P3 is goodField" /* is CONSISTENT: known final field (Instance Region) */),
  @PolicyLock("P4 is badField" /* is UNASSOCIATED: field is non-final (Instance Region) */),
  @PolicyLock("P6 is class" /* is CONSISTENT: class (Static region) */),
  @PolicyLock("P7 is goodStatic" /* is CONSISTENT: known final static field (static region) */),
  @PolicyLock("P8 is badStatic" /* is UNASSOCIATED: non-final field (static region) */),
  @PolicyLock("P9 is nonObjectField" /* is UNASSOCIATED: primitively typed field */),
  @PolicyLock("P10 is test_lock_field.Other.staticField  " /* is CONSISTENT: Static locks can use fields from other classes */),
  @PolicyLock("P11 is test_lock_field.Other.otherField " /* is UNASSOCIATED: Instance locks cannot use fields from other classes */),
  @PolicyLock("P12 is fieldFromA  " /* is CONSISTENT: Can use field from GP */),
  @PolicyLock("P13 is fieldFromB  " /* is CONSISTENT: Can use field from Parent */),
  @PolicyLock("P14 is fieldFromD  " /* is UNBOUND: Field is unknown */),
  @PolicyLock("P15 is test_lock_field.D.fieldFromD  " /* is UNASSOCIATED: Cannot use a field from a child */),
  @PolicyLock("P16 is badFieldFromA  " /* is UNASSOCIATED: non-final field from GP */),
  @PolicyLock("P17 is badFieldFromB  " /* is UNASSOCIATED: non-final field from parent */),
  @PolicyLock("P18 is badFieldFromD  " /* is UNBOUND: unknown field */),
  @PolicyLock("P19 is test_lock_field.D.badFieldFromD  " /* is UNASSOCIATED: non-final field from child */)
})
public class C extends B {
  final Object goodField = new Object();
  Object badField = new Object();
  final int nonObjectField = 1;
  
  final static Object goodStatic = new Object();
  static Object badStatic = new Object();
}
