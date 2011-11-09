Why we get the results we do:

  public void definitelyAliased_fails1(final WithUnique other) {
    this.u.twiddleParameter(other);
  }

  @Borrowed("this")
  @RegionEffects("reads p:Instance")
  public void twiddleParameter(final Object p) {
    // do nothing
  }

 "Undefined value on stack borrowed"

At the start of handling the call to twiddleParamter, the Store is

Stack depth: 2
Objects:
  {}
  {1}
  {other,this,2,BORROWED,SHARED}
  {other,2,BORROWED,SHARED}
  {BORROWED}
  {UNDEFINED}
  {this,BORROWED,SHARED}
  {BORROWED,SHARED}
Store:
  {other,this,2,BORROWED,SHARED}.u = {1}
  {this,BORROWED,SHARED}.u = {1}
Summary:
  other: SHARED
  this: SHARED

The declared effects of the method call are [reads p:Instance].

When processing the effect "reads p:Instance", before calling opLoadReachable() 
the Store is 

Stack depth: 3
Objects:
  {}
  {1}
  {other,this,2,3,BORROWED,SHARED}
  {other,2,3,BORROWED,SHARED}
  {BORROWED}
  {UNDEFINED}
  {this,BORROWED,SHARED}
  {BORROWED,SHARED}
Store:
  {other,this,2,3,BORROWED,SHARED}.u = {1}
  {this,BORROWED,SHARED}.u = {1}
Summary:
  other: SHARED
  this: SHARED

opLoadReachable() kills everything reachable from any object that might be
referenced by the stack position 3.  Because 'this' and 'other' might be
aliased, we have the field store entry "{other,this,2,3,BORROWED,SHARED}.u = {1}".
This kills the object referenced by stack position 1:

Stack depth: 2
Objects:
  {}
  {other,this,2,BORROWED,SHARED}
  {other,2,BORROWED,SHARED}
  {BORROWED}
  {1,UNDEFINED}
  {this,BORROWED,SHARED}
  {BORROWED,SHARED}
Store:
  {other,this,2,BORROWED,SHARED}.u = {}
  {this,BORROWED,SHARED}.u = {}
Summary:
  other: SHARED
  this: SHARED

Popping the arguments doesn't do anything interesting:

Stack depth: 1
Objects:
  {}
  {other,this,BORROWED,SHARED}
  {other,BORROWED,SHARED}
  {BORROWED}
  {1,UNDEFINED}
  {this,BORROWED,SHARED}
  {BORROWED,SHARED}
Store:
  {other,this,BORROWED,SHARED}.u = {}
  {this,BORROWED,SHARED}.u = {}
Summary:
  other: SHARED
  this: SHARED

Popping the receiver, on the other hand, causes a problem because the receiver
is now undefined, and thus opBorrow() fails:

"Undefined value on stack borrowed"

================

  public void definitelyAliased_assures1(final WithUnique other) {
    this.u.twiddleReceiver(other);
  }

  @Borrowed("this")
  @RegionEffects("reads this:Instance")
  public void twiddleReceiver(final Object p) {
    // do nothing
  }

  Assures!

At the start of assruing the call "this.u.twiddleReceiver()" the Store is 

Stack depth: 2
Objects:
  {other,2,BORROWED,SHARED}
  {BORROWED,SHARED}
  {}
  {1}
  {UNDEFINED}
  {this,other,2,BORROWED,SHARED}
  {BORROWED}
  {this,BORROWED,SHARED}
Store:
  {this,other,2,BORROWED,SHARED}.u = {1}
  {this,BORROWED,SHARED}.u = {1}
Summary:
  other: SHARED
  this: SHARED

The declared effects of the call are [reads this:Instance].

When handling the effect "this:Instance", just before the call to
opLoadReachable() the store is 

Stack depth: 3
Objects:
  {other,2,BORROWED,SHARED}
  {BORROWED,SHARED}
  {}
  {1,3}
  {UNDEFINED}
  {this,other,2,BORROWED,SHARED}
  {BORROWED}
  {this,BORROWED,SHARED}
Store:
  {this,other,2,BORROWED,SHARED}.u = {1,3}
  {this,BORROWED,SHARED}.u = {1,3}
Summary:
  other: SHARED
  this: SHARED

opLoadReachable() does nothing because there aren't any objects reachable from
objects referenced by stack position 3:

Stack depth: 2
Objects:
  {other,2,BORROWED,SHARED}
  {BORROWED,SHARED}
  {}
  {1}
  {UNDEFINED}
  {this,other,2,BORROWED,SHARED}
  {BORROWED}
  {this,BORROWED,SHARED}
Store:
  {this,other,2,BORROWED,SHARED}.u = {1}
  {this,BORROWED,SHARED}.u = {1}
Summary:
  other: SHARED
  this: SHARED

So everything stays in a defined state and the call assures.

========================

  public void definitelyAliased_fails2(final WithUnique other) {
    this.u.twiddleBoth(other);
  }
    
  @Borrowed("this")
  @RegionEffects("reads this:Instance, p:Instance")
  public void twiddleBoth(final Object p) {
    // do nothing
  }

  "read undefined local: 1"

Before handling effects of the call "this.u.twiddleBoth(other)" the Store is 

Stack depth: 2
Objects:
  {this,other,2,SHARED,BORROWED}
  {this,SHARED,BORROWED}
  {}
  {1}
  {UNDEFINED}
  {BORROWED}
  {other,2,SHARED,BORROWED}
  {SHARED,BORROWED}
Store:
  {this,other,2,SHARED,BORROWED}.u = {1}
  {this,SHARED,BORROWED}.u = {1}
Summary:
  other: SHARED
  this: SHARED

The declared effects of the call are [reads p:Instance, reads this:Instance].

When handling the effect "reads p:Instance" the Store is 

Stack depth: 3
Objects:
  {this,other,2,3,SHARED,BORROWED}
  {this,SHARED,BORROWED}
  {}
  {1}
  {UNDEFINED}
  {BORROWED}
  {other,2,3,SHARED,BORROWED}
  {SHARED,BORROWED}
Store:
  {this,other,2,3,SHARED,BORROWED}.u = {1}
  {this,SHARED,BORROWED}.u = {1}
Summary:
  other: SHARED
  this: SHARED

just before calling opLoadReachable().  Because this and other are possibly
aliased we have the field store entry {this,other,2,3,SHARED,BORROWED}.u = {1}
which causes us to mark {1} as reachable from the stack position 3.  This
makes stack position 1 undefined:

Stack depth: 2
Objects:
  {this,other,2,SHARED,BORROWED}
  {this,SHARED,BORROWED}
  {}
  {1,UNDEFINED}
  {BORROWED}
  {other,2,SHARED,BORROWED}
  {SHARED,BORROWED}
Store:
  {this,other,2,SHARED,BORROWED}.u = {}
  {this,SHARED,BORROWED}.u = {}
Summary:
  other: SHARED
  this: SHARED

Just before calling opLoadReachable() for the effect "reads this:Instance" we
try to duplicate stack position 1 on the stack, but this fails in opGet()
(called by dup()) because 1 is undefined:

"read undefined local: 1"
