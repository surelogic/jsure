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
 * Copyright (c) 2009 SureLogic, Inc.
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
import java.lang.annotation.Target;

/**
 * Declares that the annotated type is part of the named layers.
 * 
 * @see Layer
 */
@Documented
@Target(ElementType.TYPE)
public @interface InLayer {
  /**
   * The one or more layers that the type is part of. The
   * attribute is restricted to strings that match the following grammar:
   * 
   * <p>
   * value = layer_spec *("<tt>,</tt>" layer_spec)
   * 
   * <p>
   * layer_spec = dotted_name <i>; Layer name</i><br>
   * layer_spec /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("<tt>,</tt>" name) "<tt>}</tt>" <i>; Enumeration of layers</i>
   * 
   * <p>The braces "<tt>{</tt>" "<tt>}</tt>" are syntactic sugar
   * used to enumerate a list of layers that share the same prefix.
   */
  String value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
