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
 * Declares that the method to which this annotation is applied must be invoked
 * by any method that directly overrides it.
 * <p>
 * Any method that overrides a method annotated
 * <code>&#064;MustInvokeOnOverride</code> must also be annotated
 * <code>&#064;MustInvokeOnOverride</code> unless it is declared to be
 * <tt>final</tt> or declared within a <tt>final</tt> class. It is a modeling
 * error if it is not.
 * <p>
 * It is a modeling error to place this annotation on a method declared to be
 * <tt>final</tt> or declared within a <tt>final</tt> class
 * 
 * <h3>Semantics:</h3>
 * 
 * Mandates that the annotated method is invoked at runtime when invoked even if
 * subtypes override the method.
 * 
 * <h3>Examples:</h3>
 * 
 * The {@code Activity} class requires that "<i>derived classes must call
 * through to the super class's implementation</i>" for several methods. This
 * requirement can be documented with this annotation as shown below for the
 * <tt>onCreate</tt> method.
 * 
 * <pre>
 * package android.app;
 * 
 * public class Activity extends ... {
 *   &#064;MustInvokeOnOverride
 *   protected void onCreate(Bundle savedInstanceState) {
 *     ...
 *   }
 *   ...
 * }
 * </pre>
 * 
 * The below code shows a derived class, <tt>Earthquake</tt>, that correctly
 * invokes the <tt>onCreate</tt> method in its super class. Note that the
 * <tt>onCreate</tt> method in the <tt>Earthquake</tt> class is not annotated
 * <tt>&#064;MustInvokeOnOverride</tt> because the class is declared to be
 * final.
 * 
 * <pre>
 * public final class Earthquake extends Activity {
 *   &#064;Override
 *   protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     
 *     setContentView(R.layout.main);
 *   }
 *   ...
 * }
 * </pre>
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface MustInvokeOnOverride {
  // marker annotation
}
