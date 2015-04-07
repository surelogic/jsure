/*
 * Copyright (c) 2005 Brian Goetz and Tim Peierls
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 *
 * Any republication or derived work distributed in source code form
 * must include this copyright and license notice.
 * 
 * 
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
 * The type to which this annotation is applied is thread-safe. This means that
 * no sequences of accesses (reads and writes to public fields, calls to public
 * methods) may put the object into an invalid state, regardless of the
 * interleaving of those actions by the runtime, and without requiring any
 * additional synchronization or coordination on the part of the caller. Even if
 * one or more {@link RegionLock} models has been developed to document the
 * locking policy of a class this annotation can help to clarify that the
 * overall class is thread-safe.
 * <p>
 * This annotation <em>does not</em> imply that a sequence made up of calls to
 * methods of this class or accesses to fields of this class are atomic. It is
 * the responsibility of the caller to insure that such sequences execute
 * atomically.
 * <p>
 * This annotation is currently verified, <em>but not defined</em>, by
 * restricting how the fields of the class are declared and annotated. This is a
 * conservative, although easily understood, way to verify the
 * {@link ThreadSafe} assertion. Specifically, for a class annotated as
 * <code>&#064;ThreadSafe</code> to be verified, each field must be either
 * <ul>
 * <li>declared <code>final</code> and be "safe,"
 * <li>declared <code>volatile</code> and be "safe," or
 * <li>protected by a lock (by being a member of a region associated with a lock
 * via a <code>&#064;RegionLock</code> annotation) and be "safe."
 * </ul>
 * Where, by "safe" we mean that one of the following conditions is true:
 * <ul>
 * <li>The declared type is primitive.
 * <li>The declared type is annotated <code>&#064;ThreadSafe</code>.
 * <li>The declared type is annotated <code>&#064;Immutable</code>.
 * <li>The declared type is annotated <code>&#064;Containable</code> and the
 * field is either
 * <ul>
 * <li>Annotated <code>&#064;Unique</code> and the <code>Instance</code> region
 * is protected by a lock.
 * <li>Annotated <code>&#064;UniqueInRegion</code> such that each destination
 * region is protected by a lock.
 * </ul>
 * </ul>
 * <p>
 * A field with an array type is considered containable if the array is
 * 1-dimensional and the base type of the array is a primitive type.
 * <p>
 * Some implementations may not be "safe" as defined above, but still be
 * thread-safe. Such practices may or may not be verifiable by a tool, such as
 * SureLogic JSure. Use of the {@link Vouch} annotation can be used, especially
 * annotation of <tt>&#064;Vouch(&quot;ThreadSafe&quot;)</tt> or
 * <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> on a field, to suppress overly
 * conservative tool results.
 * <p>
 * {@link Immutable} objects are inherently thread-safe; they may be passed
 * between threads or published without synchronization. Therefore instances of
 * an {@link Immutable} type are also {@link ThreadSafe}, but not necessarily
 * vice versa.
 * <p>
 * Typically, subtypes of the annotated type must be explicitly annotated
 * <tt>&#064;ThreadSafe</tt> or (the more restrictive, but also thread-safe)
 * <tt>&#064;Immutable</tt>. It is a modeling error if they are not. This
 * annotation has two attributes, <tt>implementationOnly</tt> and
 * <tt>verify</tt>, that control how subtypes of an {@link ThreadSafe} type must
 * be annotated. The <tt>implementationOnly</tt> attribute indicates that the
 * implementation of the annotated class should be assured without making a
 * general statement about the visible behavior of the type or its subtypes.
 * There are several rules with regards to the <tt>implementationOnly</tt>
 * attribute on {@link ThreadSafe} types:
 * <ol>
 * <li>The <tt>implementationOnly</tt> attribute must be {@code false} when
 * {@link ThreadSafe} appears on an interface, because interfaces do not have an
 * implementation.</li>
 * <li>The subinterfaces of an interface annotated with {@link ThreadSafe} must
 * be annotated with {@link ThreadSafe}; classes that implement an interface
 * annotated with {@link ThreadSafe} must be annotated with
 * <tt>&#064;ThreadSafe(implementationOnly=false)</tt>.</li>
 * <li>The superclass of a class annotated with
 * <tt>&#064;ThreadSafe(implementationOnly=true)</tt> must be annotated with
 * <tt>&#064;ThreadSafe(implementationOnly=true)</tt>; there are no constraints
 * on the subclasses.</li>
 * <li>The superclass of a class annotated with
 * <tt>&#064;ThreadSafe(implementationOnly=false)</tt> must be annotated with
 * either <tt>&#064;ThreadSafe(implementationOnly=false)</tt> or
 * <tt>&#064;ThreadSafe(implementationOnly=true)</tt>; the subclasses must be
 * annotated with <tt>&#064;ThreadSafe(implementationOnly=false)</tt>.</li>
 * </ol>
 * Finally, it may be possible to implement a class that satisfies the semantics
 * of {@link ThreadSafe}, but that is not verifiable using the syntactic and
 * annotation constraints described above. For this case, we provide the
 * "escape hatch" of turning off tool verification for the annotation with the
 * <tt>verify</tt> attribute. For example,
 * <tt>&#064;ThreadSafe(verify=false)</tt> would skip tool verification
 * entirely.
 * 
 * <p>
 * A type may not be annotated with both <code>&#064;ThreadSafe</code> and
 * <code>&#064;NotThreadSafe</code> except in the case that one refers to the
 * static portion of the declaration and the other refers to the instance
 * portion of the declaration.
 * <p>
 * An annotation type declaration may be annotated with
 * <code>&#064;ThreadSafe</code> so that any static fields in the type are
 * verified as thread-safe. It is, however, a modeling error to restrict the
 * annotation to the instance part.
 * <p>
 * By default the static part of a class implementation is assumed to be
 * thread-safe as well as the instance part. It is possible to specify which
 * portions of the class implementation this annotation applies to using the
 * <tt>appliesTo</tt> attribute. This value can be one of
 * <ul>
 * <li><tt>&#064;ThreadSafe(appliesTo=Part.InstanceAndStatic)</tt> (default)
 * &mdash; The instance part and the static part are intended to be thread-safe.
 * </li>
 * <li><tt>&#064;ThreadSafe(appliesTo=Part.Instance)</tt> &mdash; The instance
 * part only is intended to be thread-safe.</li>
 * <li><tt>&#064;ThreadSafe(appliesTo=Part.Static)</tt> &mdash; The static part
 * only is intended to be thread-safe.</li>
 * </ul>
 * It is a modeling error to explicitly set <tt>appliesTo</tt> in anything but a
 * type declaration.
 * 
 * <h3>Relationship with <code>&#064;Immutable</code></h3>
 * 
 * <p>
 * Thread safety and immutability are two points along the same axis. This set
 * of annotations can actually describe three points along the axis:
 * <dl>
 * <dt><code>&#064;Mutable</code> and <code>&#064;NotThreadSafe</code>
 * <dd>This is the same as being unannotated, or just <code>&#064;Mutable</code>
 * , or just <code>&#064;NotThreadSafe</code>. The type contains mutable state
 * that is not safe to access concurrently from multiple threads.
 * 
 * <dt><code>&#064;Mutable</code> and <code>&#064;ThreadSafe</code>
 * <dd>This is the same as <code>&#064;ThreadSafe</code>. The type contains
 * mutable state that is safe to access concurrently from multiple threads.
 * 
 * <dt><code>&#064;Immutable</code> and <code>&#064;ThreadSafe</code>
 * <dd>This is the same as <code>&#064;Immutable</code>. The type contains no
 * mutable state, and is thus safe to access concurrently from multiple threads.
 * </dl>
 * 
 * <p>
 * The combination <code>&#064;Immutable</code> and
 * <code>&#064;NotThreadSafe</code> applied to the same state is a modeling
 * error because an immutable type is obviously thread safe.
 * 
 * <p>
 * An <code>&#064;Immutable</code> interface may extend a
 * <code>&#064;ThreadSafe</code> interface. An <code>&#064;Immutable</code>
 * class may implement a <code>&#064;ThreadSafe</code> interface.
 * 
 * <p>
 * A <code>&#064;ThreadSafe</code> class may extend a class annotated with
 * <code>&#064;Immutable(implementationOnly=true)</code>.
 * 
 * <h3>Semantics:</h3>
 * 
 * All accesses to a non-<code>final</code>, non-<code>volatile</code> region of
 * an instance of the annotated class occur either when
 * <ul>
 * <li>The instance is under construction and only accessible by a single
 * thread&mdash;the one that invoked the constructor&mdash;or
 * <li>The lock associated with the region is held by the thread making the
 * access.
 * </ul>
 * Further, this assertion imposes that all the state referenced by instances of
 * the annotated class, transitively, be likewise protected.
 * 
 * <h3>Examples:</h3>
 * 
 * The <code>Point</code> class is thread-safe because its fields are
 * <code>final</code> and of primitive type.
 * 
 * <pre>
 * &#064;ThreadSafe
 * &#064;Containable
 * public class Point {
 *   private final int x;
 * 
 *   private final int y;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Point(int x, int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   &#064;Borrowed(&quot;this&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   &#064;Unique(&quot;return&quot;)
 *   public Point translate(int dx, int dy) {
 *     return new Point(x + dx, y + dy);
 *   }
 * }
 * </pre>
 * 
 * <p>
 * The <code>Rectangle</code> class is thread-safe because its fields are
 * <code>final</code> and of a type annotated <code>&#064;ThreadSafe</code>:
 * 
 * <pre>
 * &#064;ThreadSafe
 * &#064;Containable
 * public class Rectangle {
 *   &#064;Unique
 *   private final Point topLeft;
 * 
 *   &#064;Unique
 *   private final Point bottomRight;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Rectangle(@Unique Point a, @Unique Point b) {
 *     topLeft = a;
 *     bottomRight = b;
 *   }
 * 
 *   &#064;Borrowed(&quot;this&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   &#064;Unique(&quot;return&quot;)
 *   public Rectangle translate(int dx, int dy) {
 *     return new Rectangle(topLeft.translate(dx, dy), bottomRight.translate(dx, dy));
 *   }
 * }
 * </pre>
 * 
 * <p>
 * Alternatively, we can create a mutable thread-safe <code>Rectangle</code>
 * class by protecting the state with a lock, and aggregating a containable
 * mutable, but not thread-safe, <code>Point</code> class:
 * 
 * <pre>
 * &#064;Containable
 * public class Point {
 *   private int x;
 * 
 *   private int y;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Point(int x, int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   &#064;Borrowed(&quot;this&quot;)
 *   &#064;RegionEffects(&quot;writes Instance&quot;)
 *   public void translate(int dx, int dy) {
 *     x += dx;
 *     y += dy;
 *   }
 * }
 * 
 * &#064;ThreadSafe
 * &#064;RegionLock(&quot;Lock is this protects Instance&quot;)
 * public class Rectangle {
 *   &#064;Unique
 *   private final Point topLeft;
 * 
 *   &#064;Unique
 *   private final Point bottomRight;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   public Rectangle(int x1, int y1, int x2, int y2) {
 *     topLeft = new Point(x1, y1);
 *     bottomRight = new Point(x2, y2);
 *   }
 * 
 *   &#064;Borrowed(&quot;this&quot;)
 *   &#064;RegionEffects(&quot;writes Instance&quot;)
 *   public synchronized void translate(int dx, int dy) {
 *     topLeft.translate(dx, dy);
 *     bottomRight.translate(dx, dy);
 *   }
 * }
 * </pre>
 * 
 * A <tt>&#064;Vouch(&quot;ThreadSafe&quot;)</tt> annotation is used to vouch
 * that a private array is thread safe after object construction. Because the
 * Java language does not allow the programmer to express that the contents of
 * the array are thread-safe, use of a {@link Vouch} is necessary in this
 * example.
 * 
 * <pre>
 * &#064;ThreadSafe
 * public class Aircraft {
 * 
 *   &#064;Vouch(&quot;ThreadSafe&quot;)
 *   private final Wing[] f_wings;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Aircraft() {
 *     f_wings = new Wing[2];
 *     f_wings[0] = new Wing();
 *     f_wings[1] = new Wing();
 *   }
 *   ...
 * }
 * </pre>
 * 
 * One area where the <tt>&#064;Vouch(&quot;ThreadSafe&quot;)</tt> annotation
 * (or the more restrictive <tt>&#064;Vouch(&quot;Immutable&quot;)</tt>
 * annotation) is often needed is when collections are used within an immutable
 * class. The code below shows an example where a collection is vouched for.
 * 
 * 
 * <pre>
 * &#064;ThreadSafe
 * public final class MutableEntityMgr {
 * 
 *   &#064;Vouch(&quot;Immutable&quot;)
 *   private final Map&lt;String, String&gt; f_defaults;
 * 
 *   &#064;Vouch(&quot;ThreadSafe&quot;)
 *   private final Map&lt;String, String&gt; f_additions;
 * 
 *   public MutableEntityMgr() {
 *     Map&lt;String, String&gt; known = new HashMap&lt;String, String&gt;();
 * 
 *     // add defaults
 *     known.put(&quot;&amp;lt;&quot;, &quot;&lt;&quot;);
 *     known.put(&quot;&amp;gt;&quot;, &quot;&gt;&quot;);
 * 
 *     f_defaults = Collections.unmodifiableMap(known);
 *     f_additions = Collections.synchronizedMap(new HashMap&lt;String, String&gt;());
 *   }
 * 
 *   public String convertIfRecognized(String entity) {
 *     String value = f_additions.get(entity);
 *     if (value == null)
 *       value = f_defaults.get(entity);
 *     return value == null ? entity : value;
 *   }
 * }
 * </pre>
 * 
 * <p>
 * As an example of treating the static and instance state of a class
 * differently, consider the <code>Point</code> class below. The instances of
 * the class are considered immutable, but the static state is mutable and
 * thread-safe. This is because the static state is used to maintain a cache of
 * instantiated points.
 * 
 * <pre>
 * &#064;Immutable(appliesTo = Part.Instance)
 * &#064;ThreadSafe(appliesTo = Part.Static)
 * &#064;Region(&quot;private static Points&quot;)
 * &#064;RegionLock(&quot;PointsLock is points protects Points&quot;)
 * public final class Point {
 *   &#064;UniqueInRegion(&quot;Points&quot;)
 *   private final static List&lt;Point&gt; points = new ArrayList&lt;Point&gt;();
 *   private final int x;
 *   private final int y;
 * 
 *   private Point(final int x, final int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   public boolean equals(final int x, final int y) {
 *     return this.x == x &amp;&amp; this.y == y;
 *   }
 * 
 *   public static Point getPoint(final int x, final int y) {
 *     synchronized (points) {
 *       for (final Point p : points) {
 *         if (p.equals(x, y))
 *           return p;
 *       }
 *       final Point p = new Point(x, y);
 *       points.add(p);
 *       return p;
 *     }
 *   }
 * 
 *   // ...
 * }
 * </pre>
 * 
 * <i>Implementation note:</i> This annotation is derived from
 * <code>&#064;ThreadSafe</code> proposed by Brian Goetz and Tim Peierls in the
 * book <i>Java Concurrency in Practice</i> (Addison-Wesley 2006) we have simply
 * adapted it to have semantics as a promise. Further, the annotation in
 * {@code net.jcip.annotations} may be used instead of this one with the same
 * tool behavior. One difference between the two annotations is that the
 * annotation in {@code com.surelogic} adds the <tt>implementationOnly</tt> and
 * <tt>verify</tt> attributes&mdash;these attributes can not be changed from
 * their default values if the the {@code net.jcip.annotations} annotation is
 * used.
 * 
 * @see AnnotationBounds
 * @see Containable
 * @see Immutable
 * @see NotThreadSafe
 * @see Mutable
 * @see Part
 * @see Region
 * @see RegionLock
 * @see Vouch
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ThreadSafe {
  /**
   * Indicates that the implementation of the annotated class should be assured
   * without making a general statement about the visible behavior of the type
   * or its subtypes.
   * 
   * @return {@code true} if only the annotated class should be assured without
   *         making a general statement about the visible behavior of the type
   *         or its subtypes, {@code false} otherwise. The default value for
   *         this attribute is {@code false}.
   */
  public boolean implementationOnly() default false;

  /**
   * Indicates whether or not tool verification should be attempted.
   * 
   * @return {@code true} if the claim should be verified by a tool, such as
   *         SureLogic JSure, {@code false} otherwise. The default value for
   *         this attribute is {@code true}.
   */
  public boolean verify() default true;

  /**
   * Indicates whether the instance state of the class, static state of the
   * class, or both are subject to this annotation.
   * 
   * @return what part of a type declaration is subject to this annotation.
   */
  public Part appliesTo() default Part.InstanceAndStatic;
}
