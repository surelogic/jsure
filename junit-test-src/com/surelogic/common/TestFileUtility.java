package com.surelogic.common;

import java.io.File;

import junit.framework.TestCase;

public class TestFileUtility extends TestCase {

	private final File f_tmpDir = new File(System.getProperty("java.io.tmpdir"));
	private final String f_lf = SLUtility.PLATFORM_LINE_SEPARATOR;

	public void testFileReadWrite() {
		final File f = new File(f_tmpDir, "test01.txt");
		if (f.exists())
			f.delete();
		assertFalse(f.exists());

		String text = f.getAbsolutePath();
		FileUtility.putStringIntoAFile(f, text);
		String actual = FileUtility.getFileContentsAsString(f);
		assertEquals(text, actual);
		assertTrue(f.exists());

		/*
		 * To test line feeds we need to use the platform-specific line
		 * separator because the get method uses that for any line feeds.
		 * 
		 * Also, it doesn't remember extra line feeds at the end.
		 */
		text = "foo" + f_lf + f_lf + "\tbar\\";
		FileUtility.putStringIntoAFile(f, text);
		actual = FileUtility.getFileContentsAsString(f);
		assertEquals(text, actual);
		assertTrue(f.exists());

		assertTrue(FileUtility.recursiveDelete(f));
		assertFalse(f.exists());
	}

	public void testDirectoryOperations() {
		final File f = new File(f_tmpDir, "test01.txt");
		if (f.exists())
			FileUtility.recursiveDelete(f);
		assertTrue(FileUtility.createDirectory(f));
		assertTrue(f.exists());

		for (int i = 0; i < 10; i++) {
			final File c = new File(f, "ooo" + i);
			FileUtility.putStringIntoAFile(c, c.getAbsolutePath());
			assertTrue(c.exists());
			final File sd = new File(f, "subdir.dir");
			assertTrue(FileUtility.createDirectory(sd));
			assertTrue(sd.exists());
			assertTrue(sd.isDirectory());
			for (int j = 0; j < 10; j++) {
				final File sdc = new File(sd, "ooo" + i);
				FileUtility.putStringIntoAFile(sdc, sdc.getAbsolutePath());
				assertTrue(sdc.exists());
			}
		}

		assertTrue(FileUtility.recursiveDelete(f));
		assertFalse(f.exists());
	}

	public void testCopy() {
		final File f = new File(f_tmpDir, "test01.txt");
		final String text = f.getAbsolutePath();
		FileUtility.putStringIntoAFile(f, text);
		assertTrue(f.exists());

		final File d = new File(f_tmpDir, "test02.txt");
		assertTrue(FileUtility.copy(f, d));
		assertTrue(f.exists());
		assertTrue(d.exists());

		assertEquals(text, FileUtility.getFileContentsAsString(d));
		assertEquals(FileUtility.getFileContentsAsString(f),
				FileUtility.getFileContentsAsString(d));

		assertTrue(FileUtility.recursiveDelete(f));
		assertFalse(f.exists());
		assertTrue(FileUtility.recursiveDelete(d));
		assertFalse(d.exists());
	}

	public void testBytesToHumanReadableString() {
		assertEquals("0 Bytes", FileUtility.bytesToHumanReadableString(0L));
		assertEquals("10 Bytes", FileUtility.bytesToHumanReadableString(10L));
		assertEquals("1023 Bytes",
				FileUtility.bytesToHumanReadableString(1023L));
		assertEquals("1024 Bytes",
				FileUtility.bytesToHumanReadableString(1024L));
		assertEquals("1 KB", FileUtility.bytesToHumanReadableString(1025L));
		assertEquals("1.9 KB", FileUtility.bytesToHumanReadableString(2025L));
		assertEquals("2 KB", FileUtility.bytesToHumanReadableString(2048L));
		assertEquals("2 MB",
				FileUtility.bytesToHumanReadableString(1024L * 1024L * 2L));
		assertEquals("1.2 MB", FileUtility.bytesToHumanReadableString(1269764L));
		assertEquals("1.9 GB",
				FileUtility.bytesToHumanReadableString(2043253448L));
		assertEquals("1903.9 GB",
				FileUtility.bytesToHumanReadableString(2044325355448L));
	}
}
