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
 * The argument determines which lock guards the method or constructor.  <em>The
 * current analysis implemented in JSure supports the following lock
 * specifications</em>:
 * <ul>
 * <li>
 * <em>lock-name</em>: The referenced <em>lock-name</em>, which is defined by a
 * {@link RegionLock} annotation, must be held when invoking the method or
 * constructor.  If <em>lock-name</em> is a non-<code>static</code> lock, the lock
 * is implicitly qualified by the receiver <code>this</code>, meaning the lock
 * must be held for the receiver of the method being called.
 * </li>
 * <li>
 * <code>this:</code><em>lock-name</em>: The referenced <em>lock-name</em>,
 * which is defined by a {@link RegionLock} annotation must be held for the
 * receiver when invoking the method.
 * </li>
 * <li>
 * <em>p</em><code>.</code><em>lock-name</em>: The referenced <em>lock-name</em>,
 * which is defined by a {@link RegionLock} annotation, must be held for the
 * object that is passed to the formal parameter <em>p</em> before 
 * invoking the method/constructor.
 * </li>
 * <li>
 * <em>class-name</em><code>:</code><em>lock-name</em>: The referenced
 * <code>static</code> <em>lock-name</em>, which is defined by a
 * {@link RegionLock} annotation on class <em>class-name</em>, must be held for the
 * object before invoking the method/constructor.
 * </li>
 * <li>
 * <em>class-name</em><code>.this:</code><em>lock-name</em>: The referenced
 * <em>lock-name</em>, which is defined by a
 * {@link RegionLock} annotation, must be held for the qualified receiver
 * object before invoking the method/constructor.
 * </li>
 * </ul>
 * 
 * <p>More generally, the annotation also supports lock preconditions in the 
 * style of the {@link GuardedBy} annotation, <em>but these are not supported 
 * by the current implementation of JSure</em>:
 * 
 * <ul>
 * <li><code>this</code>: The intrinsic lock of object used as the receiver
 * must be held before invoking the method.
 * </li> 
 * <li>
 * <em>class-name</em><code>.this</code>: The intrinsic lock of the qualified receiver
 * must be held before invoking the method.
 * </li>
 * <li>
 * <em>class-name</em><tt>.class</tt>: The intrinsic lock of the {@link Class} object for the
 * specified class must be held before calling the annotated method.
 * <li>
 * <code>this.</code><em>field-name</em>: The lock object referenced by 
 * the field <em>field-name</em> of the receiver object must be held before
 * calling the method.  The field <em>field-name</em>
 * must be {@code final} and be declared in the same class as,
 * or be a visible
 * field declaration in a superclass of, the class in which the annotated method 
 * is declared.</li>
 * <li>
 * <em>p</em><code>.</code><em>field-name</em>: The lock object referenced by 
 * the field <em>field-name</em> of the object referenced by the formal parameter
 * <em>p</em> must be held before
 * calling the method.</li>
 * <li>
 * <em>class-name</em><tt>.</tt><em>field-name</em>: The lock 
 * reference by the static field specified by <em>class-name</em><tt>.</tt>
 * <em>field-name</em> must be held before invoking the method/constructor. The static field that references the lock must be
 * {@code final}..</li>
 * <li>
 * <code>this.</code><em>method-name</em><tt>()</tt>: The lock object is returned by calling the
 * named nil-ary method of the receiver object. The named method must be declared in
 * the same class (or be a visible method declaration in a superclass) as the
 * method on which this annotation appears. Note that
 * <em>method-name</em><tt>()</tt> is trusted to return a consistent non-<code>null</code> object to
 * guard the annotated field&mdash;so take particular care with its
 * implementation.  The named method must return a reference type.
 * </li>
 * <li>
 * <em>p</em><code>.</code><em>method-name</em><tt>()</tt>: The lock object is returned by calling the
 * named nil-ary method of the object passed to formal parameter <em>p</em>.
 * </li>
 * <li>
 * <em>class-name</em><code>.</code><em>method-name</em><tt>()</tt>: The lock object is returned by calling the
 * named <code>static</code> nil-ary method.
 * </li>
 * </ul>
 * 
 * <p>Furthermore, when the specified lock is a
 * {@link java.util.concurrent.locks.ReadWriteLock}, the lock-specification must
 * indiciate whether the read or write component is required: e.g., 
 * <code>this:Lock.readLock()</code> or
 * <code>com.foo.System:GlobalLock.writeLock()</code>.
 * <p>
 * A comma separated list may be
 * provided if more than one lock needs to be held, e.g.,
 * <tt>@RequiresLock("MyLock, o:ItsLock")</tt>.
 * <p>
 * The list of locks is allowed to be empty, in which case it means that the
 * method/constructor does not require any locks to be held by the caller.  This
 * is useful when overriding a method that has a lock precondition that is not
 * required by the overriding implementation (see next paragraph).
 * <p>
 * Methods that override a method with a <code>&#64;RequiresLock</code>
 * annotation may remove locks from the set of required locks, but may not add
 * any locks to the set. That is, the set of required locks is contravariant.
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
 * @see GuardedBy
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
