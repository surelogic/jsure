package test_lock_field;

/**
 * Test that lock field must be "this", "class", or a final field.
 * Test for both state and policy locks.  Here we don't worry about
 * inherited fields or fields from other objects.
 * 
 * L1 is bad: the field is not defined
 * L2 is good: this
 * L3 is good: final field
 * L4 is bad: non-final field
 * L5 is bad: the field is not defined
 * L6 is good: class
 * L7 is good: final static field
 * L8 is bad: non-final static field
 * L9 is bad: the field has a primitive type
 * (same for the policy locks)
 * L10 is good: static locks can use static fields from other classes
 * L11 is bad: non-static locks can not use non-static fields from other classes
 * L12 is good: final field from ancestor class
 * L13 is good: final field from ancestor class
 * L14 is bad: final field from descendent class (should show as undefined)
 * L15 is bad: final field from descendent class (should show as non-ancestor)
 * L16 is bad: non-final field from ancestor class
 * L17 is bad: non-final field from ancestor class
 * L18 is bad: non-final field from non-ancestor class (should show as undefined)
 * L19 is bad: non-final field from non-ancestor class (should show as non-finla & non-ancestor)
 * 
 * L100 is bad: static field protecting an instance region
 * L101 is bad: static field protecting an instance region
 * L102 is bad: static field protecting an instance region
 * L103 is bad: static field protecting an instance region
 * 
 * SL100 is good: static field protected a static region
 * SL101 is good: static field protected a static region
 * SL102 is good: static field protected a static region
 * SL103 is good: static field protected a static region
 * 
 * PL100 is good: static policy lock
 * PL101 is good: static policy lock
 * PL102 is good: static policy lock
 * PL103 is good: static policy lock
 * PL200 is good: instance policy lock
 * PL201 is good: instance policy lock
 * 
 * @region R1
 * @region R2
 * @region R3
 * @region R4
 * @region R5
 * @region R6
 * @region R10
 * @region R11
 * @region R12
 * @region R13
 * @region R14
 * @region R15
 * @region R16
 * @region R17
 * @region static S1
 * @region static S2
 * @region static S3
 * @region static S4
 * @region static S5
 * 
 * @lock L1 is unknownField protects R1
 * @lock L2 is this protects R2
 * @lock L3 is goodField protects R3
 * @lock L4 is badField protects R4
 * @lock L5 is unknownField protects S1
 * @lock L6 is class protects S2
 * @lock L7 is goodStatic protects S3
 * @lock L8 is badStatic protects S4
 * @lock L9 is nonObjectField protects R5
 * @lock L10 is test_lock_field.Other:staticField protects S5
 * @lock L11 is test_lock_field.Other:otherField protects R6
 * @lock L12 is fieldFromA protects R10
 * @lock L13 is fieldFromB protects R11
 * @lock L14 is fieldFromD protects R12
 * @lock L15 is test_lock_field.D:fieldFromD protects R13
 * @lock L16 is badFieldFromA protects R14
 * @lock L17 is badFieldFromB protects R15
 * @lock L18 is badFieldFromD protects R16
 * @lock L19 is test_lock_field.D:badFieldFromD protects R17
 * 
 * @region R100 
 * @region R101
 * @region R102
 * @region R103
 * 
 * @region static S100
 * @region static S101
 * @region static S102
 * @region static S103
 * 
 * @lock L100 is class protects R100
 * @lock L101 is goodStatic protects R101
 * @lock L102 is test_lock_field.C:goodStatic protects R102
 * @lock L103 is test_lock_field.Other:staticField protects R103
 * 
 * @lock L100S is class protects S100
 * @lock L101S is goodStatic protects S101
 * @lock L102S is test_lock_field.C:goodStatic protects S102
 * @lock L103S is test_lock_field.Other:staticField protects S103
 * 
 * @policyLock P1 is unknownField
 * @policyLock P2 is this
 * @policyLock P3 is goodField
 * @policyLock P4 is badField
 * @policyLock P6 is class
 * @policyLock P7 is goodStatic
 * @policyLock P8 is badStatic
 * @policyLock P9 is nonObjectField
 * @policyLock P10 is test_lock_field.Other:staticField  
 * @policyLock P11 is test_lock_field.Other:otherField 
 * @policyLock P12 is fieldFromA  
 * @policyLock P13 is fieldFromB  
 * @policyLock P14 is fieldFromD  
 * @policyLock P15 is test_lock_field.D:fieldFromD  
 * @policyLock P16 is badFieldFromA  
 * @policyLock P17 is badFieldFromB  
 * @policyLock P18 is badFieldFromD  
 * @policyLock P19 is test_lock_field.D:badFieldFromD  
 *
 * @policyLock PL100 is class 
 * @policyLock PL101 is goodStatic 
 * @policyLock PL102 is test_lock_field.C:goodStatic
 * @policyLock PL103 is test_lock_field.Other:staticField 
 * @policyLock PL200 is this
 * @policyLock PL201 is goodField
 */
public class C extends B {
  final Object goodField = new Object();
  Object badField = new Object();
  final int nonObjectField = 1;
  
  final static Object goodStatic = new Object();
  static Object badStatic = new Object();
}
