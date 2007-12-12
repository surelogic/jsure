The classes in this package are designed to test the region-related aspects of 
the "WFLockDefs" rules for lock declarations
- Class C tests the rules
    (3) the associated region must exist
    (4) instance field or this cannot be associated with a static region  
    (4a) Static region must be from the same class as the declaration
  Classes B, D, and Other are used by the tests in C.
  
- Class LockViz tests that the lock field is at least as visible as the 
  region being protected.  In particular, it tests that @returnsLock methods
  affect the visibility of the lock field.

- Class ProtectInheritedRegion tests the rule that an inherited region
  may only be associated with a lock if the region contains no fields.
  Classed GreatGrandparent, Grandparent, and Parent assist.
  