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

/**
 * A utility class to cast object references to non-null references and nullable
 * references.
 */
@Utility
public final class Cast {

  /**
   * Casts any reference to a non-null reference which may be assigned to any
   * variable annotated to be {@link NonNull}. This cast is trusted, it is not
   * statically verified. However, a Java language assertion is used to check
   * that the passed reference is non null, e.g., <tt>assert ref != null</tt>.
   * <p>
   * This version of the cast does not allow an explanatory comment. Please use
   * {@link #toNonNull(Object, String)} when a reason can be provided why the
   * cast is valid.
   * <p>
   * In the somewhat contrived example below the partially initialized object
   * referenced by the parameter <code>myPeer</code> is "cast" with
   * {@link Cast#toNonNull(Object)} so that it can be assigned to the non-null
   * field <code>peer</code>.
   * 
   * <pre>
   * public class B {
   * 
   *   &#064;NonNull
   *   private final A peer;
   * 
   *   B(@Initialized(through = &quot;Object&quot;) A myPeer) {
   *     peer = Cast.toNonNull(myPeer);
   *   }
   * }
   * </pre>
   * 
   * A cast is required because the it is not generally safe to assign a
   * partially initialized object to a {@link NonNull} field. But in this case
   * it is safe because the reference is never dereferenced.
   * <p>
   * A second example is dealing with method calls during object construction.
   * This can be dangerous because methods that may dispatch into subtypes
   * should not be invoked.
   * 
   * <pre>
   * public final class KnownEvaluators {
   * 
   *   private KnownEvaluators() {
   *     register(new EvaluatorByte());
   *     register(new EvaluatorShort());
   *     ...
   *   }
   * 
   *   public void register(Evaluator&lt;?&gt; singleton) {
   *     // implementation
   *   }
   *   ...
   * }
   * </pre>
   * 
   * The analysis wants the <code>register</code> method to be annotated
   * <tt>&#064;Initialized(through = &quot;Object&quot;)</tt> but may be
   * undesirable for a public method intended to be invoked by clients of this
   * class. We can cast the receiver from partially initialized to non null in
   * one of two ways. The first option is to do the cast at each call.
   * 
   * <pre>
   * private KnownEvaluators() {
   *   Cast.toNonNull(this).register(new EvaluatorByte());
   *   Cast.toNonNull(this).register(new EvaluatorShort());
   *   ...
   * }
   * </pre>
   * 
   * The second option is to introduce a local variable.
   * 
   * <pre>
   * private KnownEvaluators() {
   *   final @NonNull KnownEvaluators iThis = Cast.toNonNull(this);
   *   iThis.register(new EvaluatorByte());
   *   iThis.register(new EvaluatorShort());
   *   ...
   * }
   * </pre>
   * 
   * Choosing between these two depends upon your code and which style you
   * prefer. Note that at the point you make this cast you should be sure that
   * all your fields are initialized and you do not invoke a method that might
   * dispatch into a subtype. In this example only one field (not shown) was
   * initialized (at its declaration) and the class is declared <tt>final</tt>
   * so there can be no subtypes to dispatch into. This latter fact is important
   * because <tt>register</tt> is a <tt>public</tt> method that could be
   * overridden in a subtype.
   * 
   * @param ref
   *          a non-null object reference.
   * @param <T>
   *          the type being cast.
   * @return the non-null object reference passed to the method as <tt>ref</tt>.
   * 
   * @throws AssertionError
   *           if the passed reference is <code>null</code> and Java language
   *           assertions are enabled at runtime&mdash;this is typically done by
   *           passing "<tt>-ea</tt>" to the Java virtual machine.
   * 
   * @see #toNonNull(Object, String)
   */
  @NonNull
  @Starts("nothing")
  @RegionEffects("none")
  @Vouch("This is a special analysis-trusted method")
  public static <T> T toNonNull(final T ref) {
    assert ref != null;
    return ref;
  }

  /**
   * Casts any reference to a non-null reference which may be assigned to any
   * variable annotated to be {@link NonNull}. This cast is trusted, it is not
   * statically verified. However, a Java language assertion is used to check
   * that the passed reference is non null, e.g., <tt>assert ref != null</tt>.
   * <p>
   * This version of the cast allows you to pass an explanatory comment as part
   * of the call. This parameter is for documentation purposes only, so that
   * 
   * <pre>
   * peer = Cast.toNonNull(myPeer, &quot;initialization of all fields is complete&quot;);
   * </pre>
   * 
   * has the same effect as
   * 
   * <pre>
   * peer = Cast.toNonNull(myPeer);
   * </pre>
   * 
   * @param ref
   *          a non-null object reference.
   * @param reason
   *          a comment explaining why the cast is valid. This parameter is for
   *          documentation purposes only.
   * @param <T>
   *          the type being cast.
   * @return the non-null object reference passed to the method as <tt>ref</tt>.
   * 
   * @throws AssertionError
   *           if the passed reference is <code>null</code> and Java language
   *           assertions are enabled at runtime&mdash;this is typically done by
   *           passing "<tt>-ea</tt>" to the Java virtual machine.
   * 
   * @see #toNonNull(Object)
   */
  @NonNull
  @Starts("nothing")
  @RegionEffects("none")
  public static <T> T toNonNull(final T ref, final String reason) {
    return toNonNull(ref);
  }

  /**
   * Casts any reference to a nullable reference which may be assigned to any
   * variable annotated to be {@link Nullable} or unannotated. This cast is
   * trusted, it is not statically verified.
   * <p>
   * This version of the cast does not allow an explanatory comment. Please use
   * {@link #toNullable(Object, String)} when a reason can be provided why the
   * cast is valid.
   * <p>
   * In the somewhat contrived example below the partially initialized object
   * referenced by the parameter <code>myPeer</code> is "cast" with
   * {@link Cast#toNullable(Object)} so that it can be assigned to the nullable
   * field <code>peer</code> and the unannotated field <code>peerNoAnno</code>.
   * 
   * <pre>
   * public class B {
   * 
   *   &#064;Nullable
   *   private final A peer;
   * 
   *   private final A peerNoAnno;
   * 
   *   B(@Initialized(through = &quot;Object&quot;) A myPeer) {
   *     peer = Cast.toNullable(myPeer);
   *     peerNoAnno = Cast.toNullable(myPeer);
   *   }
   * }
   * </pre>
   * 
   * A cast is required because the it is not generally safe to assign a
   * partially initialized object to a {@link Nullable} field. But in this case
   * it is safe because the reference is never dereferenced.
   * 
   * @param ref
   *          an object reference, may be null.
   * @param <T>
   *          the type being cast.
   * @return the object reference, which may be null, passed to the method as
   *         <tt>ref</tt>.
   * 
   * @see #toNullable(Object, String)
   */
  @Nullable
  @Starts("nothing")
  @RegionEffects("none")
  public static <T> T toNullable(final T ref) {
    return ref;
  }

  /**
   * Casts any reference to a nullable reference which may be assigned to any
   * variable annotated to be {@link Nullable} or unannotated. This cast is
   * trusted, it is not statically verified.
   * <p>
   * This version of the cast allows you to pass an explanatory comment as part
   * of the call. This parameter is for documentation purposes only, so that
   * 
   * <pre>
   * peer = Cast.toNullable(myPeer, &quot;initialization of all fields is complete&quot;);
   * </pre>
   * 
   * has the same effect as
   * 
   * <pre>
   * peer = Cast.toNullable(myPeer);
   * </pre>
   * 
   * @param ref
   *          an object reference, may be null.
   * @param reason
   *          a comment explaining why the cast is valid. This parameter is for
   *          documentation purposes only.
   * @param <T>
   *          the type being cast.
   * @return the object reference, which may be null, passed to the method as
   *         <tt>ref</tt>.
   * 
   * @see Cast#toNullable(Object)
   */
  @Nullable
  @Starts("nothing")
  @RegionEffects("none")
  public static <T> T toNullable(final T ref, final String reason) {
    return toNullable(ref);
  }

  /**
   * Casts any reference to a unique reference which may be assigned to any
   * field annotated to be {@link Unique}. Note that the reference passed and
   * returned may be {@code null}. This cast is trusted, it is not statically
   * verified.
   * <p>
   * This version of the cast does not allow an explanatory comment. Please use
   * {@link #toUniqueReference(Object, String)} when a reason can be provided
   * why the cast is valid.
   * <p>
   * In the somewhat contrived example below the partially initialized object
   * referenced by the parameter <code>myPeer</code> is "cast" with
   * {@link Cast#toNonNull(Object)} so that it can be assigned to the non-null
   * field <code>peer</code>.
   * 
   * <pre>
   * public class B {
   * 
   *   &#064;Unique
   *   private final List&lt;String&gt; sides;
   * 
   *   B() {
   *     List&lt;String&gt; temp = new ArrayList&lt;String&gt;();
   *     temp.add(&quot;heads&quot;);
   *     temp.add(&quot;tails&quot;);
   *     sides = Cast.toUniqueReference(Collections.unmodifiableList(temp));
   *   }
   * }
   * </pre>
   * 
   * A cast is required because when calling
   * {@link java.util.Collections#unmodifiableList(java.util.List)} it is not
   * generally safe to assume the passed list is not aliased. But in this case
   * it is safe because {@code temp} is unaliased, making {@code sides} (and the
   * wrapped {@link java.util.ArrayList}) unique after the constructor
   * completes.
   * 
   * @param ref
   *          an object reference or {@code null}.
   * @param <T>
   *          the type being cast.
   * @return the object reference or {@code null} passed to the method as
   *         <tt>ref</tt>.
   * 
   * @see #toUniqueReference(Object, String)
   */
  @Unique("return")
  @Starts("nothing")
  @RegionEffects("none")
  @Vouch("This is a special analysis-trusted method")
  public static <T> T toUniqueReference(@Nullable final T ref) {
    return ref;
  }

  /**
   * Casts any reference to a unique reference which may be assigned to any
   * field annotated to be {@link Unique}. Note that the reference passed and
   * returned may be {@code null}. This cast is trusted, it is not statically
   * verified.
   * <p>
   * This version of the cast allows you to pass an explanatory comment as part
   * of the call. This parameter is for documentation purposes only, so that
   * 
   * <pre>
   * peer = Cast.toUniqueReference(myPeer, &quot;myPeer is in fact unique or null&quot;);
   * </pre>
   * 
   * has the same effect as
   * 
   * <pre>
   * peer = Cast.toUniqueReference(myPeer);
   * </pre>
   * 
   * @param ref
   *          an object reference or {@code null}.
   * @param reason
   *          a comment explaining why the cast is valid. This parameter is for
   *          documentation purposes only.
   * @param <T>
   *          the type being cast.
   * @return the object reference or {@code null} passed to the method as
   *         <tt>ref</tt>.
   * 
   * @see #toUniqueReference(Object)
   */
  @Unique("return")
  @Starts("nothing")
  @RegionEffects("none")
  public static <T> T toUniqueReference(@Nullable final T ref, final String reason) {
    return toUniqueReference(ref);
  }

  private Cast() {
    throw new AssertionError();
  }
}
