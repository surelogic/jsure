/*
 * Copyright (c) 2015 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the method or constructor to which this annotation applies
 * should be invoked holding one or more locks. Verification of the
 * method/constructor proceeds as if the named locks were held; call sites of
 * the method are scrutinized to determine if the precondition is satisfied.
 * <p>
 * * The argument determines which lock guards the method or constructor:
 * <ul>
 * <li>
 * <tt>this</tt>: The intrinsic lock of the object in whose class the annotated
 * method or constructor is defined.</li>
 * <li>
 * <em>field-name</em>: The lock object is referenced by the (instance or
 * static) field specified by <em>field-name</em>. The field that references the
 * lock must be {@code final} and be declared in the same class (or be a visible
 * field declaration in a superclass) as the method or constructor on which this
 * annotation appears. In the case that both a <em>field-name</em> and a
 * <em>lock-name</em> (see below) have the same name, the binding is to the
 * <em>lock-name</em>.</li>
 * <li>
 * <em>lock-name</em>: The referenced <em>lock-name</em>, which is defined by
 * {@link RegionLock} annotation must be held when invoking the method or
 * constructor. In this use, <tt>@RequiresLock("</tt><em>lock-name</em>
 * <tt>")</tt> is equivalent to the annotation <tt>@GuardedBy("</tt>
 * <em>lock-name</em><tt>")</tt>. In the case that both a <em>field-name</em>
 * and a <em>lock-name</em> have the same name, the binding is to the
 * <em>lock-name</em>.</li>
 * <li>
 * <em>method-name</em><tt>()</tt>: The lock object is returned by calling the
 * named nil-ary method. The method that returns the lock must be declared in
 * the same class (or be a visible method declaration in a superclass) as the
 * method or constructor on which this annotation appears. Note that
 * <em>method-name</em><tt>()</tt> is trusted to return a consistent object to
 * guard the annotated field or method&mdash;so take particular care with its
 * implementation.</li>
 * <li>
 * <em>class-name</em><tt>.class</tt>: The {@link Class} object for the
 * specified class should be used as the lock object.</li>
 * <li>
 * <em>class-name</em><tt>.</tt><em>field-name</em>: The lock object is
 * reference by the static field specified by <em>class-name</em><tt>.</tt>
 * <em>field-name</em>. The static field that references the lock must be
 * {@code final}.</li>
 * <li>
 * <em>class-name</em><tt>.this</tt>: For inner classes, it may be necessary to
 * disambiguate 'this'; the <em>class-name</em><tt>.this</tt> designation allows
 * you to specify which 'this' reference is intended.</li>
 * </ul>
 * <p>
 * When this annotation is applied to a method a comma separated list may be
 * provided if more than one lock needs to be held, e.g.
 * <tt>@RequiresLock("this, C.class, myLock")</tt>.
 * <p>
 * The list of locks is allowed to be empty, in which case it means that the
 * method/constructor does not require any locks to be held by the caller.
 * <p>
 * Methods that override a method with a <code>&#64;RequiresLock</code>
 * annotation may remove locks from the set of required locks, but may not add
 * any locks to the set. That is, the set of required locks is contravariant.
 * <p>
 * This annotation interacts with {@link RegionLock} defined locks when applied
 * to methods or constructors. As noted above, holding <em>lock-name</em> may be
 * expressed as a prerequisite to invoking the method. Also, non-
 * <em>lock-name</em> lock preconditions expressed by {@link GuardedBy} should
 * be resolved to named locks as applicable. This is best illustrated by
 * example. In the listing below the lock preconditions on <tt>m1()</tt>,
 * <tt>m2()</tt>, and <tt>m3()</tt> are semantically equivalent. In particular,
 * a lock precondition on <tt>this</tt> resolves to both named locks:
 * <tt>l1</tt> and <tt>l2</tt>
 * 
 * <pre>
 * &#064;RegionLocks({ @RegionLock(&quot;l1 is this protects f1&quot;), @RegionLock(&quot;l2 is this protects f2&quot;) })
 * public class C {
 * 
 *   int f1;
 * 
 *   int f2;
 * 
 *   &#064;GuardedBy(&quot;this&quot;)
 *   void m1() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * 
 *   &#064;GuardedBy(&quot;l1, l2&quot;)
 *   void m2() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * 
 *   &#064;RequiresLock(&quot;this&quot;)
 *   void m3() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * 
 *   &#064;RequiresLock(&quot;l1, l2&quot;)
 *   void m4() {
 *     f1 = 4;
 *     f2 = 5;
 *   }
 * }
 * </pre>
 * <p>
 * {@link GuardedBy} some <em>expression</em> expressed on a method may be
 * thought of as semantically equivalent to a {@link RequiresLock} annotation
 * with the same <em>expression</em>. Which annotation you choose to use is a
 * style preference.
 * <p>
 * A subtlety of use of this annotation with {@link RegionLock} defined locking
 * models as opposed to {@link GuardedBy} defined locking models is the use a
 * method (or constructor) precondition that gets the required lock via a method
 * call. In this case of a {@link RegionLock} defined locking model the method
 * that returns the lock <em>must</em> be annotated with a {@link ReturnsLock}
 * annotation. The listing below shows an example. The {@link RegionLock}
 * defined locking model <tt>dLock</tt> would not verify without the
 * {@link ReturnsLock} annotation on the <tt>getLock1()</tt> method because the
 * precondition on <tt>getState1Helper()</tt> would not be known to return the
 * needed lock. In the case of the {@link GuardedBy} defined locking model on
 * the field <tt>state2</tt> the {@link ReturnsLock} annotation is not required.
 * The {@link RegionLock} model is a stronger result because it verifies the
 * correct lock is returned. In the case of the {@link GuardedBy} defined
 * locking model on the field <tt>state2</tt> it is marked with as contingent on
 * the implementation of <tt>getLock2</tt> because the "correct" lock is unknown
 * to this model.
 * 
 * <pre>
 * &#064;RegionLock(&quot;dLock is lock protects state&quot;)
 * public class D {
 * 
 *   private final Object lock = new Object();
 * 
 *   &#064;ReturnsLock(&quot;dlock&quot;)
 *   Object getLock1() {
 *     return lock;
 *   }
 * 
 *   private int state1;
 * 
 *   &#064;RequiresLock(&quot;getLock1()&quot;)
 *   private int getState1Helper() {
 *     return state1;
 *   }
 * 
 *   public int getState1() {
 *     final int result;
 *     synchronized (getLock1()) {
 *       result = getState1Helper();
 *     }
 *     return result;
 *   }
 * 
 *   Object getLock2() {
 *     return lock;
 *   }
 * 
 *   &#064;GuardedBy(&quot;getLock2()&quot;)
 *   private int state2;
 * 
 *   &#064;RequiresLock(&quot;getLock2()&quot;)
 *   private int getState2Helper() {
 *     return state2;
 *   }
 * 
 *   public int getState2() {
 *     final int result;
 *     synchronized (getLock2()) {
 *       result = getState2Helper();
 *     }
 *     return result;
 *   }
 * }
 * </pre>
 * 
 * <h3>Semantics:</h3>
 * 
 * Each thread that invokes an annotated method or constructor must hold the
 * lock on each object denoted by the {@code lockSpecification}s in the
 * annotation.
 * 
 * <h3>Examples:</h3>
 * 
 * A locking policy, named {@code StateLock}, that indicates that synchronizing
 * on the field {@code stateLock} (which must be declared to be {@code final})
 * protects the two {@code long} fields use to represent the position of the
 * object. The {@link RequiresLock} annotation is used specify that
 * {@code StateLock} must be held when invoking the {@code setX} or {@code setY}
 * methods.
 * 
 * <pre>
 * &#064;Region(&quot;private AircraftState&quot;)
 * &#064;RegionLock(&quot;StateLock is stateLock protects AircraftState&quot;)
 * public class Aircraft {
 *   private final Object stateLock = new Object();
 * 
 *   &#064;InRegion(&quot;AircraftState&quot;)
 *   private long x, y;
 * 
 *   public void setPosition(long x, long y) {
 *     synchronized(stateLock)
 *       setX(x);
 *       setY(y);
 *     }
 *   }
 * 
 *   &#064;RequiresLock(&quot;StateLock&quot;)
 *   private void setX(long value) {
 *     x = value;
 *   }
 * 
 *   &#064;RequiresLock(&quot;StateLock&quot;)
 *   private void setY(long value) {
 *     y = value;
 *   }
 * }
 * </pre>
 * 
 * @see RegionLock
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface RequiresLock {
  /**
   * A comma-separated list of zero or more lock names. The value of this
   * attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = lockSpecification *(&quot;,&quot; lockSpecification)
   * 
   * lockSpecification = qualifiedLockSpecification / simpleLockSpecification
   * 
   * simpleLockSpecification = simpleLockName [&quot;.&quot; (&quot;readLock&quot; / &quot;writeLock&quot;)]
   * 
   * qualifiedLockSpecification = qualifiedLockName [&quot;.&quot; (&quot;readLock&quot; / &quot;writeLock&quot;)]
   * 
   * simpleLockName = IDENTIFIER  ; Lock from the receiver (same as &quot;this:IDENTIFIER&quot;)
   * 
   * qualifiedLockName = parameterLockName / typeQualifiedLockName / innerClassLockName
   * 
   * parameterLockName = simpleExpression &quot;:&quot; IDENTIFIER  ; Lock from a method/constructor parameter
   * 
   * simpleExpression = &quot;this&quot; / IDENTIFER  ; Receiver or parameter name
   * 
   * typeQualifiedLockName = typeExpression &quot;:&quot; IDENTIFIER  ; Static lock qualified by a class name
   * 
   * typeExpression = IDENTIFIER *(&quot;.&quot; IDENTIFIER)
   * 
   * innerClassLockName = namedType &quot;.&quot; &quot;this&quot; &quot;:&quot; IDENTIFIER ; Lock from an enclosing instance
   * 
   * namedType = IDENTIFIER *(&quot;.&quot; IDENTIFIER)
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
