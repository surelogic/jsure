package com.surelogic.jsure.client.eclipse.analysis;

import com.surelogic.fluid.javac.Config;

import edu.cmu.cs.fluid.sea.Drop;

public class JavacRunDrop extends Drop {
    final Config config;
    
    JavacRunDrop(Config cfg) {
        config = cfg;
    }    
}
