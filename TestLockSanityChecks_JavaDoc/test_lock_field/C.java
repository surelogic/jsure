package test_lock_field;

/**
 * Test that lock field must be "this", "class", or a final field.
 * Test for both state and policy locks.  Here we don't worry about
 * inherited fields or fields from other objects.
 * 
 * @Region R1
 * @Region R2
 * @Region R3
 * @Region R4
 * @Region R5
 * @Region R6
 * @Region R10
 * @Region R11
 * @Region R12
 * @Region R13
 * @Region R14
 * @Region R15
 * @Region R16
 * @Region R17
 * @Region static S1
 * @Region static S2
 * @Region static S3
 * @Region static S4
 * @Region static S5
 * 
 * @TestResult is UNBOUND: Field is undefined (Instance Region)
 * @RegionLock L1 is unknownField protects R1
 * @TestResult is CONSISTENT: receiver (Instance Region)
 * @RegionLock L2 is this protects R2
 * @TestResult is CONSISTENT: known final field (Instance Region)
 * @RegionLock L3 is goodField protects R3
 * @TestResult is UNASSOCIATED: field is non-final (Instance Region)
 * @RegionLock L4 is badField protects R4
 * @TestResult is UNBOUND: Field is undefined (Static region)
 * @RegionLock L5 is unknownField protects S1
 * @TestResult is CONSISTENT: class (Static region)
 * @RegionLock L6 is class protects S2
 * @TestResult is CONSISTENT: known final static field (static region)
 * @RegionLock L7 is goodStatic protects S3
 * @TestResult is UNASSOCIATED: non-final field (static region)
 * @RegionLock L8 is badStatic protects S4
 * @TestResult is UNASSOCIATED: primitively typed field
 * @RegionLock L9 is nonObjectField protects R5
 * @TestResult is CONSISTENT: Static locks can use fields from other classes
 * @RegionLock L10 is test_lock_field.Other.staticField protects S5
 * @TestResult is UNASSOCIATED: Instance locks cannot use fields from other classes
 * @RegionLock L11 is test_lock_field.Other.otherField protects R6
 * @TestResult is CONSISTENT: Can use field from GP
 * @RegionLock L12 is fieldFromA protects R10
 * @TestResult is CONSISTENT: Can use field from Parent
 * @RegionLock L13 is fieldFromB protects R11
 * @TestResult is UNBOUND: Field is unknown
 * @RegionLock L14 is fieldFromD protects R12
 * @TestResult is UNASSOCIATED: Cannot use a field from a child
 * @RegionLock L15 is test_lock_field.D.fieldFromD protects R13
 * @TestResult is UNASSOCIATED: non-final field from GP
 * @RegionLock L16 is badFieldFromA protects R14
 * @TestResult is UNASSOCIATED: non-final field from parent
 * @RegionLock L17 is badFieldFromB protects R15
 * @TestResult is UNBOUND: unknown field
 * @RegionLock L18 is badFieldFromD protects R16
 * @TestResult is UNASSOCIATED: non-final field from child
 * @RegionLock L19 is test_lock_field.D.badFieldFromD protects R17
 * 
 * @Region R100 
 * @Region R101
 * @Region R102
 * @Region R103
 * 
 * @TestResult is UNASSOCIATED: class cannot protect an instance region
 * @RegionLock L100 is class protects R100
 * @TestResult is UNASSOCIATED: static field cannot protect an instance region
 * @RegionLock L101 is goodStatic protects R101
 * @TestResult is UNASSOCIATED: static field cannot protect an instance region
 * @RegionLock L102 is test_lock_field.C.goodStatic protects R102
 * @TestResult is UNASSOCIATED: static field cannot protect an instance region
 * @RegionLock L103 is test_lock_field.Other.staticField protects R103
 * 
 * @TestResult is UNBOUND: Field is undefined (Instance Region)
 * @PolicyLock P1 is unknownField
 * @TestResult is CONSISTENT: receiver (Instance Region)
 * @PolicyLock P2 is this
 * @TestResult is CONSISTENT: known final field (Instance Region)
 * @PolicyLock P3 is goodField
 * @TestResult is UNASSOCIATED: field is non-final (Instance Region)
 * @PolicyLock P4 is badField
 * @TestResult is CONSISTENT: class (Static region)
 * @PolicyLock P6 is class
 * @TestResult is CONSISTENT: known final static field (static region)
 * @PolicyLock P7 is goodStatic
 * @TestResult is UNASSOCIATED: non-final field (static region)
 * @PolicyLock P8 is badStatic
 * @TestResult is UNASSOCIATED: primitively typed field
 * @PolicyLock P9 is nonObjectField
 * @TestResult is CONSISTENT: Static locks can use fields from other classes
 * @PolicyLock P10 is test_lock_field.Other.staticField  
 * @TestResult is UNASSOCIATED: Instance locks cannot use fields from other classes
 * @PolicyLock P11 is test_lock_field.Other.otherField 
 * @TestResult is CONSISTENT: Can use field from GP
 * @PolicyLock P12 is fieldFromA  
 * @TestResult is CONSISTENT: Can use field from Parent
 * @PolicyLock P13 is fieldFromB  
 * @TestResult is UNBOUND: Field is unknown
 * @PolicyLock P14 is fieldFromD  
 * @TestResult is UNASSOCIATED: Cannot use a field from a child
 * @PolicyLock P15 is test_lock_field.D.fieldFromD  
 * @TestResult is UNASSOCIATED: non-final field from GP
 * @PolicyLock P16 is badFieldFromA  
 * @TestResult is UNASSOCIATED: non-final field from parent
 * @PolicyLock P17 is badFieldFromB  
 * @TestResult is UNBOUND: unknown field
 * @PolicyLock P18 is badFieldFromD  
 * @TestResult is UNASSOCIATED: non-final field from child
 * @PolicyLock P19 is test_lock_field.D.badFieldFromD  
 */
public class C extends B {
  final Object goodField = new Object();
  Object badField = new Object();
  final int nonObjectField = 1;
  
  final static Object goodStatic = new Object();
  static Object badStatic = new Object();
}
