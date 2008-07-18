package com.surelogic.jsure.schema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.surelogic.common.jdbc.SchemaAction;
import com.surelogic.common.jdbc.SchemaData;

public class JSureSchemaData implements SchemaData {

	private final ClassLoader loader;
	private final String schemaPackage;

	public JSureSchemaData() {
		loader = Thread.currentThread().getContextClassLoader();
		schemaPackage = "com.surelogic.jsure.schema";
	}

	public SchemaAction getSchemaAction(final String action) {
		try {
			return (SchemaAction) loader
					.loadClass(schemaPackage + "." + action).newInstance();
		} catch (final InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}

	public URL getSchemaResource(final String name) {
		return loader.getResource(getSchemaResourcePath(name));
	}

	public int getVersion() {
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(
						loader
								.getResourceAsStream(getSchemaResourcePath("version.txt"))));
		try {
			try {
				return Integer.valueOf(reader.readLine().trim());
			} finally {
				reader.close();
			}
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String getSchemaResourcePath(final String resource) {
		return "/" + schemaPackage.replace(".", "/") + "/" + resource;
	}

}
