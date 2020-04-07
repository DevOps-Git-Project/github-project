/*
 * Copyright (C) 2008, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.treewalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.FileMode;
import org.junit.Test;

public class NameConflictTreeWalkTest extends RepositoryTestCase {
	private static final FileMode TREE = FileMode.TREE;

	private static final FileMode SYMLINK = FileMode.SYMLINK;

	private static final FileMode MISSING = FileMode.MISSING;

	private static final FileMode REGULAR_FILE = FileMode.REGULAR_FILE;

	private static final FileMode EXECUTABLE_FILE = FileMode.EXECUTABLE_FILE;

	@Test
	public void testNoDF_NoGap() throws Exception {
		final DirCache tree0 = db.readDirCache();
		final DirCache tree1 = db.readDirCache();
		{
			final DirCacheBuilder b0 = tree0.builder();
			final DirCacheBuilder b1 = tree1.builder();

			b0.add(createEntry("a", REGULAR_FILE));
			b0.add(createEntry("a.b", EXECUTABLE_FILE));
			b1.add(createEntry("a/b", REGULAR_FILE));
			b0.add(createEntry("a0b", SYMLINK));

			b0.finish();
			b1.finish();
			assertEquals(3, tree0.getEntryCount());
			assertEquals(1, tree1.getEntryCount());
		}

		try (TreeWalk tw = new TreeWalk(db)) {
			tw.addTree(new DirCacheIterator(tree0));
			tw.addTree(new DirCacheIterator(tree1));

			assertModes("a", REGULAR_FILE, MISSING, tw);
			assertModes("a.b", EXECUTABLE_FILE, MISSING, tw);
			assertModes("a", MISSING, TREE, tw);
			tw.enterSubtree();
			assertModes("a/b", MISSING, REGULAR_FILE, tw);
			assertModes("a0b", SYMLINK, MISSING, tw);
		}
	}

	@Test
	public void testDF_NoGap() throws Exception {
		final DirCache tree0 = db.readDirCache();
		final DirCache tree1 = db.readDirCache();
		{
			final DirCacheBuilder b0 = tree0.builder();
			final DirCacheBuilder b1 = tree1.builder();

			b0.add(createEntry("a", REGULAR_FILE));
			b0.add(createEntry("a.b", EXECUTABLE_FILE));
			b1.add(createEntry("a/b", REGULAR_FILE));
			b0.add(createEntry("a0b", SYMLINK));

			b0.finish();
			b1.finish();
			assertEquals(3, tree0.getEntryCount());
			assertEquals(1, tree1.getEntryCount());
		}

		try (NameConflictTreeWalk tw = new NameConflictTreeWalk(db)) {
			tw.addTree(new DirCacheIterator(tree0));
			tw.addTree(new DirCacheIterator(tree1));

			assertModes("a", REGULAR_FILE, TREE, tw);
			assertTrue(tw.isDirectoryFileConflict());
			assertTrue(tw.isSubtree());
			tw.enterSubtree();
			assertModes("a/b", MISSING, REGULAR_FILE, tw);
			assertTrue(tw.isDirectoryFileConflict());
			assertModes("a.b", EXECUTABLE_FILE, MISSING, tw);
			assertFalse(tw.isDirectoryFileConflict());
			assertModes("a0b", SYMLINK, MISSING, tw);
			assertFalse(tw.isDirectoryFileConflict());
		}
	}

	@Test
	public void testDF_GapByOne() throws Exception {
		final DirCache tree0 = db.readDirCache();
		final DirCache tree1 = db.readDirCache();
		{
			final DirCacheBuilder b0 = tree0.builder();
			final DirCacheBuilder b1 = tree1.builder();

			b0.add(createEntry("a", REGULAR_FILE));
			b0.add(createEntry("a.b", EXECUTABLE_FILE));
			b1.add(createEntry("a.b", EXECUTABLE_FILE));
			b1.add(createEntry("a/b", REGULAR_FILE));
			b0.add(createEntry("a0b", SYMLINK));

			b0.finish();
			b1.finish();
			assertEquals(3, tree0.getEntryCount());
			assertEquals(2, tree1.getEntryCount());
		}

		try (NameConflictTreeWalk tw = new NameConflictTreeWalk(db)) {
			tw.addTree(new DirCacheIterator(tree0));
			tw.addTree(new DirCacheIterator(tree1));

			assertModes("a", REGULAR_FILE, TREE, tw);
			assertTrue(tw.isSubtree());
			assertTrue(tw.isDirectoryFileConflict());
			tw.enterSubtree();
			assertModes("a/b", MISSING, REGULAR_FILE, tw);
			assertTrue(tw.isDirectoryFileConflict());
			assertModes("a.b", EXECUTABLE_FILE, EXECUTABLE_FILE, tw);
			assertFalse(tw.isDirectoryFileConflict());
			assertModes("a0b", SYMLINK, MISSING, tw);
			assertFalse(tw.isDirectoryFileConflict());
		}
	}

	@Test
	public void testDF_SkipsSeenSubtree() throws Exception {
		final DirCache tree0 = db.readDirCache();
		final DirCache tree1 = db.readDirCache();
		{
			final DirCacheBuilder b0 = tree0.builder();
			final DirCacheBuilder b1 = tree1.builder();

			b0.add(createEntry("a", REGULAR_FILE));
			b1.add(createEntry("a.b", EXECUTABLE_FILE));
			b1.add(createEntry("a/b", REGULAR_FILE));
			b0.add(createEntry("a0b", SYMLINK));
			b1.add(createEntry("a0b", SYMLINK));

			b0.finish();
			b1.finish();
			assertEquals(2, tree0.getEntryCount());
			assertEquals(3, tree1.getEntryCount());
		}

		try (NameConflictTreeWalk tw = new NameConflictTreeWalk(db)) {
			tw.addTree(new DirCacheIterator(tree0));
			tw.addTree(new DirCacheIterator(tree1));

			assertModes("a", REGULAR_FILE, TREE, tw);
			assertTrue(tw.isSubtree());
			assertTrue(tw.isDirectoryFileConflict());
			tw.enterSubtree();
			assertModes("a/b", MISSING, REGULAR_FILE, tw);
			assertTrue(tw.isDirectoryFileConflict());
			assertModes("a.b", MISSING, EXECUTABLE_FILE, tw);
			assertFalse(tw.isDirectoryFileConflict());
			assertModes("a0b", SYMLINK, SYMLINK, tw);
			assertFalse(tw.isDirectoryFileConflict());
		}
	}

	@Test
	public void testDF_DetectConflict() throws Exception {
		final DirCache tree0 = db.readDirCache();
		final DirCache tree1 = db.readDirCache();
		{
			final DirCacheBuilder b0 = tree0.builder();
			final DirCacheBuilder b1 = tree1.builder();

			b0.add(createEntry("0", REGULAR_FILE));
			b0.add(createEntry("a", REGULAR_FILE));
			b1.add(createEntry("0", REGULAR_FILE));
			b1.add(createEntry("a.b", REGULAR_FILE));
			b1.add(createEntry("a/b", REGULAR_FILE));
			b1.add(createEntry("a/c/e", REGULAR_FILE));

			b0.finish();
			b1.finish();
			assertEquals(2, tree0.getEntryCount());
			assertEquals(4, tree1.getEntryCount());
		}

		try (NameConflictTreeWalk tw = new NameConflictTreeWalk(db)) {
			tw.addTree(new DirCacheIterator(tree0));
			tw.addTree(new DirCacheIterator(tree1));

			assertModes("0", REGULAR_FILE, REGULAR_FILE, tw);
			assertFalse(tw.isDirectoryFileConflict());
			assertModes("a", REGULAR_FILE, TREE, tw);
			assertTrue(tw.isSubtree());
			assertTrue(tw.isDirectoryFileConflict());
			tw.enterSubtree();
			assertModes("a/b", MISSING, REGULAR_FILE, tw);
			assertTrue(tw.isDirectoryFileConflict());
			assertModes("a/c", MISSING, TREE, tw);
			assertTrue(tw.isDirectoryFileConflict());
			tw.enterSubtree();
			assertModes("a/c/e", MISSING, REGULAR_FILE, tw);
			assertTrue(tw.isDirectoryFileConflict());

			assertModes("a.b", MISSING, REGULAR_FILE, tw);
			assertFalse(tw.isDirectoryFileConflict());
		}
	}

	private static void assertModes(final String path, final FileMode mode0,
			final FileMode mode1, final TreeWalk tw) throws Exception {
		assertTrue("has " + path, tw.next());
		assertEquals(path, tw.getPathString());
		assertEquals(mode0, tw.getFileMode(0));
		assertEquals(mode1, tw.getFileMode(1));
	}
}
