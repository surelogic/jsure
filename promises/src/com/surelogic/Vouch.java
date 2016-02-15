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
import java.util.Collections;
import java.util.List;

/**
 * Vouches for any inconsistent analysis result within the scope of code that
 * the annotation appears on. This means that any inconsistent result will be
 * changed to a consistent result. Its use is for documentation and to quiet
 * overly conservative analysis results.
 * <p>
 * This annotation is trusted, i.e., it is <em>not verified</em>.
 * <p>
 * When used on a type, method, or constructor declaration, the {@link Vouch}
 * annotation requires a brief programmer explanation for the vouch being made
 * (via the {@code value} attribute).
 * <p>
 * When used on a field, parameter, or local variable declaration, the
 * {@code value} attribute is constrained to be one of the following:
 * <ul>
 * <li><tt>&#064;Vouch(&quot;Containable&quot;)</tt> &ndash; (field
 * declarations) Vouches that the object referenced by the annotated field is
 * containable. The object is treated as if its class is annotated with
 * {@link Containable}. It is a modeling error if the type of the field is
 * primitive. It is also a modeling error if the reference type is explicitly
 * annotated {@link NotContainable}.</li>
 * <li><tt>&#064;Vouch(&quot;final&quot;)</tt> &ndash; (field declarations)
 * Vouches that the annotated field reference acts as if it is constant, or
 * final, even though this cannot be proven by the Java compiler. It is
 * recommended that, wherever possible, the program's code be refactored to
 * avoid the use of this annotation&mdash;and allow the field to be declared
 * {@code final}. It is a modeling error if the field is declared to be
 * {@code final}</li>
 * <li><tt>&#064;Vouch(&quot;Immutable&quot;)</tt> &ndash; (field declarations)
 * Vouches that the object referenced by the annotated field is immutable. The
 * object is treated as if its class is annotated with {@link Immutable}. It is
 * a modeling error if the type of the field is primitive. It is also a modeling
 * error if the reference type is explicitly annotated {@link Mutable}.</li>
 * <li><tt>&#064;Vouch(&quot;ThreadSafe&quot;)</tt> &ndash; (field declarations)
 * Vouches that the object referenced by the annotated field is thread-safe. The
 * object is treated as if its class is annotated with {@link ThreadSafe}. It is
 * a modeling error if the type of the field is primitive. It is also a modeling
 * error if the reference type is explicitly annotated {@link NotThreadSafe}.</li>
 * <li><tt>&#064;Vouch(&quot;AnnotationBounds&quot;)</tt> &ndash; (field, local
 * variable, and parameter declarations) Vouches that the constructor, field,
 * local variable, method, or parameter declaration should be treated as though
 * all annotation bounds constraints are met. It is a modeling error if the type
 * of the variable is primitive.</li>
 * </ul>
 * When one of the special {@code value} attributes of the {@link Vouch}
 * annotation listed above is used the {@code reason} attribute is used to
 * capture the programmer explanation for the vouch.
 * 
 * <h3>Semantics:</h3>
 * 
 * This annotation acts upon the results reported from tools, such as SureLogic
 * JSure, that statically verify the assertions imposed by promise annotations.
 * It allows programmers to quiet overly conservative analysis results being
 * reported by the tool.
 * <p>
 * Note that {@link Vouch} serves a different role than {@link Assume}. Vouches
 * made by the programmer are typically applied in situations where a static
 * verification tool, such as JSure, is unable to verify the programmer's
 * implementation to be consistent with the expressed model. For example, due
 * the fundamental limitations of static program analysis imposed by <a
 * href="http://en.wikipedia.org/wiki/Rice%27s_theorem">Rice's theorem</a> or
 * because the programmer is unwilling, perhaps due to an organizational
 * constraint of some sort, to refactor the implementation to facilitate a
 * static verification. In contrast, assumptions are used to express an
 * expectation by the programmer about the evolving model&mdash;irrespective of
 * whether or not the expectation can be verified.
 * 
 * <h3>Examples:</h3>
 * 
 * In the example code below an {@code init} method is used to set state,
 * perhaps due to an API restriction about using constructors, and then
 * {@code CentralControl} instances are safely published. A {@link Vouch} is
 * used to explain that the {@code init} method needs to be an exception to the
 * lock policy.
 * 
 * <pre>
 * &#064;Region(&quot;private ControlRegion&quot;)
 * &#064;RegionLock(&quot;ControlLock is lock protects ControlRegion&quot;)
 * public class CentralControl {
 * 
 *   private final Object lock = new Object();
 * 
 *   &#064;InRegion(&quot;ControlRegion&quot;)
 *   private String f_id;
 * 
 *   &#064;Vouch(&quot;Instances are thread confined until after init(String) is called.&quot;)
 *   public void init(String id) {
 *     f_id = id;
 *   }
 * 
 *   public String getId() {
 *     synchronized (lock) {
 *       return f_id;
 *     }
 *   }
 * 
 *   public void setId(String value) {
 *     synchronized (lock) {
 *       f_id = value;
 *     }
 *   }
 * }
 * </pre>
 * 
 * A {@link Vouch} is used to explain that the {@code SmokeTest} class is test
 * code.
 * 
 * <pre>
 * &#064;Vouch("Test code that is intentionally inconsistent with models")
 * public class SmokeTest extends ... {
 *   ...
 * }
 * </pre>
 * 
 * A {@link Vouch} is used to explain that a class is using the <i>cheap
 * read-write lock trick</i> described by <a
 * href="http://www.ibm.com/developerworks/java/library/j-jtp06197.html"
 * >Goetz</a>. Without the {@link Vouch} the read of the {@code value} field in
 * the {@code getValue} method would be reported to be inconsistent with the
 * declared locking policy.
 * 
 * <pre>
 * &#064;ThreadSafe
 * public class Counter {
 * 
 *   // Employs the cheap read-write lock trick
 *   // All mutative operations MUST be done with the &quot;this&quot; lock
 *   &#064;GuardedBy(&quot;this&quot;)
 *   private volatile int value;
 * 
 *   &#064;Vouch(&quot;cheap read-write lock trick&quot;)
 *   public int getValue() {
 *     return value;
 *   }
 * 
 *   public synchronized int increment() {
 *     return value++;
 *   }
 * }
 * </pre>
 * 
 * A <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> annotation is used to express
 * that a list made unmodifiable by invoking
 * {@link Collections#unmodifiableList(java.util.List)} is immutable after
 * object construction. The use of {@link Vouch} is necessary in this example
 * because, in general, {@link List} instances are mutable.
 * 
 * <pre>
 * &#064;Immutable
 * public class KeyTimes implements Iterable&lt;Double&gt; {
 * 
 *   &#064;Vouch(&quot;Immutable&quot;)
 *   private final List&lt;Double&gt; f_keyTimes;
 * 
 *   private KeyTimes(double... times) {
 *     final List&lt;Double&gt; timesList = new ArrayList&lt;Double&gt;();
 *     // fills in the collection from the passed varargs
 *     ...
 *     f_keyTimes = Collections.unmodifiableList(timesList);
 *   }
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * The same example, but with an explanation for the vouch explicitly captured
 * in the annotation
 * 
 * <pre>
 * &#064;Immutable
 * public class KeyTimes implements Iterable&lt;Double&gt; {
 * 
 *   &#064;Vouch(value=&quot;Immutable&quot;,
 *          reason=&quot;Initialized with an immutable list via Collections.unmodifiableList()&quot;)
 *   private final List&lt;Double&gt; f_keyTimes;
 * 
 *   private KeyTimes(double... times) {
 *     final List&lt;Double&gt; timesList = new ArrayList&lt;Double&gt;();
 *     // fills in the collection from the passed varargs
 *     ...
 *     f_keyTimes = Collections.unmodifiableList(timesList);
 *   }
 *   ...
 * }
 * </pre>
 * 
 * A <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> annotation is used to express
 * that a private array is immutable after object construction. Because the Java
 * language does not allow the programmer to express that the contents of the
 * array are unchanging, use of a {@link Vouch} is necessary in this example.
 * 
 * <pre>
 * &#064;Immutable
 * public class Aircraft {
 * 
 *   &#064;Vouch(value=&quot;Immutable&quot;,
 *          reason=&quot;Initialized in the constructor; immutable thereafter&quot;)
 *   private final Wing[] f_wings;
 * 
 *   public Aircraft() {
 *     f_wings = new Wing[2];
 *     f_wings[0] = new Wing();
 *     f_wings[1] = new Wing();
 *   }
 *   ...
 * }
 * </pre>
 * 
 * A <tt>&#064;Vouch(&quot;ThreadSafe&quot;)</tt> annotation is used to express
 * that a list made unmodifiable by invoking
 * {@link Collections#synchronizedList(List)} is thread-safe . The vouch is
 * necessary because, in general, {@link List} instances are not thread-safe.
 * 
 * <pre>
 * &#064;ThreadSafe
 * public class KeyValues&lt;T&gt; {
 * 
 *   &#064;Vouch(&quot;ThreadSafe&quot;)
 *   private final List&lt;T&gt; f_values;
 *   
 *   private KeyValues(List&lt;T&gt; values) {
 *     f_values = Collections.synchronizedList(values);
 *   }
 *   ...
 * }
 * </pre>
 * 
 * The below example, which is a highly questionable protocol, sets the lock
 * once after object construction and uses
 * <tt>&#064;Vouch(&quot;final&quot;)</tt> to state this policy.
 * 
 * <pre>
 * &#064;Region(&quot;private BadFinalRegion&quot;)
 * &#064;RegionLock(&quot;BadFinalLock is lock protects BadFinalRegion&quot;)
 * public class BadFinal {
 * 
 *   &#064;Vouch(&quot;final&quot;)
 *   private Object lock;
 * 
 *   // Called only once at startup
 *   public void setLock(Object value) {
 *     lock = value;
 *   }
 *   ...
 *   &#064;InRegion(&quot;BadFinalRegion&quot;)
 *   private int x, y;
 * 
 *   public void tick() {
 *     synchronized (lock) {
 *       x++;
 *       y++;
 *     }
 *   }
 *   ...
 * }
 * </pre>
 * 
 * An improved implementation would set the lock via the constructor and avoid
 * the assumption about the lock reference. In the example below, the
 * {@code BadFinal} code is changed so that the lock field can be declared to be
 * {@code final} (and checked by the compiler).
 * 
 * <pre>
 * &#064;Region(&quot;private GoodFinalRegion&quot;)
 * &#064;RegionLock(&quot;GoodFinalLock is lock protects GoodFinalRegion&quot;)
 * public class GoodFinal {
 * 
 *   private final Object lock;
 * 
 *   public GoodFinal(Object value) {
 *     lock = value;
 *   }
 *   ...
 *   &#064;InRegion(&quot;GoodFinalRegion&quot;)
 *   private int x, y;
 * 
 *   public void tick() {
 *     synchronized (lock) {
 *       x++;
 *       y++;
 *     }
 *   }
 *   ...
 * }
 * </pre>
 * 
 * In general, use of <tt>&#064;Vouch(&quot;final&quot;)</tt> should be avoided
 * where it is possible to refactor the code in a manner similar to the example
 * above.
 * 
 * <p>
 * A <tt>&#064;Vouch(&quot;AnnotationBounds&quot;)</tt> annotation is used below
 * to allow <tt>Runnable</tt> to satisfy the conditional bounds of
 * <tt>java.util.ArrayList</tt>, so that <tt>ArrayList&lt;Runnable&gt;</tt> can
 * be considered to be containable. Ultimately this allows the class below to be
 * assured as thread-safe.
 * 
 * <pre>
 * &#064;ThreadSafe
 * &#064;Region(&quot;State&quot;)
 * &#064;RegionLock(&quot;StateLock is this protects State&quot;)
 * public class VouchContainableCollection {
 * 
 *   &#064;Vouch(&quot;AnnotationBounds&quot;)
 *   &#064;UniqueInRegion(&quot;State&quot;)
 *   private final ArrayList&lt;Runnable&gt; f_tasks = new ArrayList&lt;Runnable&gt;();
 * 
 *   // ...
 * }
 * </pre>
 * 
 * <p>
 * The vouch is necessary because <code>java.lang.Runnable</code> is too general
 * a class to be annotated as <code>&#064;ThreadSafe</code> or
 * <code>&#064;ReferenceObject</code>, as it would need to be to satisfy the
 * conditional bounds required the <code>&#064;Containable</code> annotation of
 * <code>java.util.ArrayList</code>. In this case, the programmer is vouching
 * that the <code>Runnable</code> objects are not going to do anything that
 * would interfere with the containability of the list object.
 * 
 * @see Assume
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER,
    ElementType.LOCAL_VARIABLE })
public @interface Vouch {
  /**
   * When the annotation applies to a type, method, or constructor then the
   * value of this attribution must contain a brief programmer explanation of
   * why the annotated code should have its inconsistent assurance results
   * suppressed.
   * <p>
   * When the annotation applies to a vouch with special semantics, then the
   * value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = &quot;Containable&quot; / &quot;final&quot; / &quot;Immutable&quot; / &quot;ThreadSafe&quot; / &quot;AnnotationBounds&quot;
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  public String value();

  /**
   * When the annotation applies to a field declaration, this attribute contains
   * a brief programmer explanation of why the annotated code should have its
   * inconsistent assurance results suppressed.
   * 
   * @return a brief programmer explanation of why the annotated code should
   *         have its inconsistent assurance results suppressed.
   */
  public String reason() default "";
}
