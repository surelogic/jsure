package com.surelogic.javac;

import java.net.URI;

import javax.tools.JavaFileObject;

public final class JavaFileObjectWrapper {
	private final JavaFileObject file;
	private final URI uri;
	
	JavaFileObjectWrapper(JavaFileObject f) {
		file = f;
		uri = f.toUri();
	}
	
	JavaFileObject get() {
		return file;
	}
	
	@Override
	public final int hashCode() {
		return uri.hashCode();
	}
	
	@Override
	public final boolean equals(Object o) {
		if (o instanceof JavaFileObjectWrapper) {
			JavaFileObjectWrapper w = (JavaFileObjectWrapper) o;
			return uri.equals(w.uri);
		}
		if (o instanceof JavaFileObject) {
			JavaFileObject f = (JavaFileObject) o;
			return uri.equals(f.toUri());
		}
		return false;
	}
}
