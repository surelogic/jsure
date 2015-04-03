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
 * Instances of the type (class or interface) to which this annotation is
 * applied are immutable. This means that its state cannot be seen to change by
 * callers, which implies that
 * <ul>
 * <li>all public fields are {@code final},</li>
 * <li>all public final reference fields refer to other immutable objects, and</li>
 * <li>constructors and methods do not publish references to any internal state
 * which is potentially mutable by the implementation.</li>
 * </ul>
 * Immutable objects may still have internal mutable state for purposes of
 * performance optimization; some state variables may be lazily computed, so
 * long as they are computed from immutable state and that callers cannot tell
 * the difference. Such practices may or may not be verifiable by a tool, such
 * as SureLogic JSure. Use of the {@link Vouch} annotation can be used,
 * especially annotation of <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> on a
 * field, to suppress overly conservative tool results.
 * <p>
 * This annotation is currently verified, <em>but not defined</em>, by
 * restricting how the fields of the class are declared and annotated. This is a
 * conservative, although easily understood, way to verify the {@link Immutable}
 * assertion. Specifically, for a class annotated as
 * <code>&#064;Immutable</code> to be verified, each field must be
 * <ul>
 * <li>Declared <code>final</code>; and
 * <li>Be of a primitive type or of a type annotated
 * <code>&#064;Immutable</code>.
 * </ul>
 * 
 * <p>
 * Immutable objects are inherently thread-safe; they may be passed between
 * threads or published without synchronization. Therefore instances of an
 * {@link Immutable} type are also {@link ThreadSafe}, but not necessarily vice
 * versa.
 * <p>
 * Typically, subtypes of the annotated type must be explicitly annotated
 * <code>&#064;Immutable</code> as well. It is a modeling error if they are not.
 * This annotation has two attributes, <tt>implementationOnly</tt> and
 * <tt>verify</tt>, that control how subtypes of an {@link Immutable} type must
 * be annotated. The <tt>implementationOnly</tt> attribute indicates that the
 * implementation of the annotated class should be assured without making a
 * general statement about the visible behavior of the type or its subtypes.
 * There are several rules with regards to the <tt>implementationOnly</tt>
 * attribute on {@link Immutable} types:
 * <ol>
 * <li>The <tt>implementationOnly</tt> attribute must be {@code false} when
 * {@link Immutable} appears on an interface, because interfaces do not have an
 * implementation.</li>
 * <li>The subinterfaces of an interface annotated with {@link Immutable} must
 * be annotated with {@link Immutable}; classes that implement an interface
 * annotated with {@link Immutable} must be annotated with
 * <tt>&#064;Immutable(implementationOnly=false)</tt>.</li>
 * <li>The superclass of a class annotated with
 * <tt>&#064;Immutable(implementationOnly=true)</tt> must be annotated with
 * <tt>&#064;Immutable(implementationOnly=true)</tt>; there are no constraints
 * on the subclasses.</li>
 * <li>The superclass of a class annotated with
 * <tt>&#064;Immutable(implementationOnly=false)</tt> must be annotated with
 * either <tt>&#064;Immutable(implementationOnly=false)</tt> or
 * <tt>&#064;Immutable(implementationOnly=true)</tt>; the subclasses must be
 * annotated with <tt>&#064;Immutable(implementationOnly=false)</tt>.</li>
 * </ol>
 * Finally, it may be possible to implement a class that satisfies the semantics
 * of {@link Immutable}, but that is not verifiable using the syntactic and
 * annotation constraints described above. For this case, we provide the
 * "escape hatch" of turning off tool verification for the annotation with the
 * <tt>verify</tt> attribute. For example,
 * <tt>&#064;Immutable(verify=false)</tt> would skip tool verification entirely.
 * 
 * <p>
 * A type may not be annotated with both <code>&#064;Immutable</code> and
 * <code>&#064;Mutable</code> except in the case that one refers to the static
 * portion of the declaration and the other refers to the instance portion of
 * the declaration.
 * <p>
 * An annotation type declaration may be annotated with
 * <code>&#064;Immutable</code> so that any static fields in the type are
 * verified as immutable. It is, however, a modeling error to restrict the
 * annotation to the instance part.
 * <p>
 * By default the static part of a class implementation is assumed to be
 * immutable as well as the instance part. It is possible to specify which
 * portions of the class implementation this annotation applies to using the
 * <tt>appliesTo</tt> attribute. This value can be one of
 * <ul>
 * <li><tt>&#064;Immutable(appliesTo=Part.InstanceAndStatic)</tt> (default)
 * &mdash; The instance part and the static part are intended to be immutable.</li>
 * <li><tt>&#064;Immutable(appliesTo=Part.Instance)</tt> &mdash; The instance
 * part only is intended to be immutable.</li>
 * <li><tt>&#064;Immutable(appliesTo=Part.Static)</tt> &mdash; The static part
 * only is intended to be immutable.</li>
 * </ul>
 * It is a modeling error to explicitly set <tt>appliesTo</tt> in anything but a
 * type declaration.
 * 
 * <h3>Relationship with <code>&#064;ThreadSafe</code></h3>
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
 * Instances of the type to which this annotation is applied are thread-safe and
 * their state cannot be seen to change by callers.
 * 
 * <h3>Examples:</h3>
 * 
 * The immutable {@code Point} class below is considered thread-safe.
 * 
 * <pre>
 * &#064;Immutable
 * public class Point {
 * 
 *   final int f_x;
 *   final int f_y;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   &#064;RegionEffects(&quot;none&quot;)
 *   public Point(int x, int y) {
 *     f_x = x;
 *     f_y = y;
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads Instance&quot;)
 *   public int getX() {
 *     return f_x;
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads Instance&quot;)
 *   public int getY() {
 *     return f_y;
 *   }
 * }
 * </pre>
 * 
 * A <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> annotation is used to vouch
 * that a private array is immutable after object construction. Because the Java
 * language does not allow the programmer to express that the contents of the
 * array are unchanging, use of a {@link Vouch} is necessary in this example.
 * 
 * <pre>
 * &#064;Immutable
 * public class Aircraft {
 * 
 *   &#064;Vouch(&quot;Immutable&quot;)
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
 * In the example below a <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> annotation
 * is used to vouch that a parameterized type references an immutable object.
 * 
 * <pre>
 * &#064;Immutable
 * public class Frame&lt;T&gt; {
 *   &#064;Vouch(&quot;Immutable&quot;)
 *   private final T value;
 * 
 *   private final double timeFraction;
 *   ...
 * }
 * </pre>
 * 
 * It is possible to avoid the need to annotate the <tt>value</tt> field
 * <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> if the parameterized type
 * <tt>T</tt> extends a type annotated to be immutable. For example, if there
 * exists a type <tt>Base</tt> that is annotated <tt>&#064;Immutable</tt>, then
 * the listing above could be modified to avoid the need for a vouch.
 * 
 * <pre>
 * &#064;Immutable
 * public class Frame&lt;T extends Base&gt; {
 *   private final T value;
 * 
 *   private final double timeFraction;
 *   ...
 * }
 * </pre>
 * 
 * One area where the <tt>&#064;Vouch(&quot;Immutable&quot;)</tt> annotation is
 * often needed is when collections are used within an immutable class. The code
 * below shows an example where a collection is vouched for.
 * 
 * <pre>
 * &#064;Immutable
 * public final class EntityMgr {
 * 
 *   &#064;Vouch(&quot;Immutable&quot;)
 *   private final Map&lt;String, String&gt; f_known;
 * 
 *   public EntityMgr(Map&lt;String, String&gt; defs) {
 *     Map&lt;String, String&gt; known = new HashMap&lt;String, String&gt;();
 * 
 *     // add defaults
 *     known.put(&quot;&amp;lt;&quot;, &quot;&lt;&quot;);
 *     known.put(&quot;&amp;gt;&quot;, &quot;&gt;&quot;);
 * 
 *     known.putAll(defs);
 * 
 *     f_known = Collections.unmodifiableMap(known);
 *   }
 * 
 *   public EntityMgr() {
 *     this(Collections.&lt;String, String&gt; emptyMap());
 *   }
 * 
 *   public String convertIfRecognized(String entity) {
 *     String value = f_known.get(entity);
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
 * <code>&#064;Immutable</code> proposed by Brian Goetz and Tim Peierls in the
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
 * @see ThreadSafe
 * @see Mutable
 * @see Part
 * @see Vouch
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Immutable {
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
   * class, or both are subject to this annotation. This attribute only applies
   * when the annotation is used on a type declaration.
   * 
   * @return what part of a type declaration is subject to this annotation.
   */
  public Part appliesTo() default Part.InstanceAndStatic;
}
