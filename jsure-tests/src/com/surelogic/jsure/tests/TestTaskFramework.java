/**
 * 
 */
package com.surelogic.jsure.tests;

import java.util.Vector;
import java.util.concurrent.*;

import junit.framework.TestCase;
import com.surelogic.task.*;

/**
 * @author ethan
 * 
 */
public class TestTaskFramework extends TestCase {

	private static final String ONE = "One";

	private static final String TWO = "Two";

	private static final String THREE = "Three";

	private static final String FOUR = "Four";

	private static final String FIVE = "Five";

	private static final String SIX = "Six";

	private static final String SEVEN = "Seven";

	private TestTask task1;

	private TestTask task2;

	private TestTask task3;

	private TestTask task4;

	private TestTask task5;

	private TestTask task6;

	private TestTask task7;

	private BufferingOutput output = null;

	/**
	 * @param name
	 */
	public TestTaskFramework(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		output = new BufferingOutput();
		task1 = new TestTask(ONE, output);
		task2 = new TestTask(TWO, output);
		task3 = new TestTask(THREE, output);
		task4 = new TestTask(FOUR, output);
		task5 = new TestTask(FIVE, output);
		task6 = new TestTask(SIX, output);
		task7 = new TestTask(SEVEN, output);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test running the graph in a single, asynchronous thread
	 */
	public void testSingleThreadedAsynchronousRun() {
		System.out
				.println("--------------testSingleThreadedAsynchronousRun----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		final CountDownLatch latch = new CountDownLatch(1);

		long start = System.currentTimeMillis();
		mgr.addListener(new TaskListenerAdapter() {
			@Override
			public void allTasksComplete() {
				Vector<String> o = output.getOutput();
				assertEquals(o.size(), 6);
				assertEquals(o.elementAt(0), ONE);
				assertEquals(o.elementAt(1), TWO);
				assertEquals(o.elementAt(2), THREE);
				assertEquals(o.elementAt(3), FOUR);
				assertEquals(o.elementAt(4), FIVE);
				assertEquals(o.elementAt(5), SIX);
				latch.countDown();
			}
		});

		try {
			mgr.execute(false);
			long end = System.currentTimeMillis();
			System.out.println("Time 1: " + (end - start));
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		try {
			latch.await();
			long end = System.currentTimeMillis();
			System.out.println("Time 2: " + (end - start));
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		mgr.shutdown();

	}

	public void testThreadThrowing() {
		System.out.println("--------------testThreadThrowing----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), true);
		createGraphNoCycles(mgr);
		Runnable seven = new Runnable() {
			@Override
			public void run() {
				System.out.println(SEVEN);
				throw new RuntimeException("This should not hang the system");
			}
		};
		try {
			mgr.addTask(SEVEN, seven);
		} catch (DuplicateTaskNameException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (IllegalStateException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		}

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		Vector<String> o = output.getOutput();
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);

		mgr.shutdown();
	}

	/**
	 * This test throws a ThreadDeath Error, which <em>should not</em> hang the
	 * system
	 */
	public void testThreadThrowing2() {
		System.out.println("--------------testThreadThrowing2----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), true);
		createGraphNoCycles(mgr);
		Runnable seven = new Runnable() {
			@Override
			public void run() {
				System.out.println(SEVEN);
				throw new ThreadDeath();
			}
		};
		try {
			mgr.addTask(SEVEN, seven);
			mgr.addDependency(ONE, SEVEN);
		} catch (DuplicateTaskNameException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (IllegalStateException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		}

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		Vector<String> o = output.getOutput();
		for(String val : o) {
			System.out.println("Got "+val);
		}
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);

		mgr.shutdown();
	}

	public void testThreadThrowing3() {
		System.out.println("--------------testThreadThrowing3----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), true);
		createGraphNoCycles(mgr);
		Runnable seven = new Runnable() {
			@Override
			public void run() {
				System.out.println(SEVEN);
				throw new IllegalStateException("This should not hang the system");
			}
		};
		try {
			mgr.addTask(SEVEN, seven);
		} catch (DuplicateTaskNameException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (IllegalStateException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		}

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		Vector<String> o = output.getOutput();
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);

		mgr.shutdown();
	}

	/**
	 * Test running an empty graph
	 */
	public void testEmptyGraph() {
		System.out.println("--------------testEmptyGraph----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());

		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		@SuppressWarnings("unused")
		Vector<String> o = output.getOutput();
		mgr.shutdown();
	}

	/**
	 * Test running the graph in the calling thread
	 */
	public void testSingleThreadedRun() {
		System.out.println("--------------testSingleThreadedRun----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		Vector<String> o = output.getOutput();
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);

		mgr.shutdown();
	}

	/**
	 * Tests modifying the graph while the system is running
	 */
	public void testModifyWhileRunning() {
		System.out.println("--------------testModifyWhileRunning----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		final CountDownLatch latch = new CountDownLatch(1);

		long start = System.currentTimeMillis();
		mgr.addListener(new TaskListenerAdapter() {
			@Override
			public void allTasksComplete() {
				Vector<String> o = output.getOutput();
				assertEquals(o.size(), 6);
				assertEquals(o.elementAt(0), ONE);
				assertEquals(o.elementAt(1), TWO);
				assertEquals(o.elementAt(2), THREE);
				assertEquals(o.elementAt(3), FOUR);
				assertEquals(o.elementAt(4), FIVE);
				assertEquals(o.elementAt(5), SIX);
				latch.countDown();
			}
		});

		TestTask seven = new TestTask(SEVEN, output);

		try {
			mgr.execute(false);
			long end = System.currentTimeMillis();
			System.out.println("Time 1: " + (end - start));
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		// All of these should throw an exception

		try {
			mgr.addTask(SEVEN, seven);
			assertTrue(false);
		} catch (IllegalStateException e1) {
			// Expected exception
		} catch (DuplicateTaskNameException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		}

		try {
			mgr.addDependency(ONE, SEVEN);
			assertTrue(false);
		} catch (IllegalStateException e) {
			// expected
		}

		try {
			mgr.removeTask(SEVEN);
			assertTrue(false);
		} catch (IllegalStateException e) {
			// expected
		}

		try {
			mgr.removeDependency(ONE, SEVEN);
			assertTrue(false);
		} catch (IllegalStateException e) {
			// expected
		}

		try {
			mgr.execute(false);
			assertTrue(false);
		} catch (IllegalStateException e) {
			// expected
		} catch (UndefinedDependencyException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (CycleFoundException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (InterruptedException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (BrokenBarrierException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		} catch (TimeoutException e1) {
			fail("Unexpected exception thrown " + e1.getMessage());
		}

		try {
			latch.await();
			long end = System.currentTimeMillis();
			System.out.println("Time 2: " + (end - start));
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		mgr.shutdown();

	}

	/**
	 * Test the timeout of the execute() method
	 */
	public void testSingleThreadedRunTimeout() {
		System.out
				.println("--------------testSingleThreadedRunTimeout---------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true, 100, TimeUnit.MILLISECONDS);
			fail("Execute() was supposed to time out.");
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			// expected
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		// Vector<String> o = output.getOutput();
		// assertEquals(o.size(), 1);
		// assertEquals(o.elementAt(0), ONE);

		mgr.shutdown();
	}

	/**
	 * Tests running the graph with multiple threads
	 */
	public void testMultithreadedRun() {

		System.out.println("--------------testMultithreadedRun----------------");
		TaskManager mgr = new TaskManager(2, 4, 60, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		final CountDownLatch latch = new CountDownLatch(1);

		mgr.addListener(new TaskListenerAdapter() {
			@Override
			public void allTasksComplete() {
				Vector<String> o = output.getOutput();
				assertEquals(o.size(), 6);
				assertEquals(o.elementAt(0), ONE);

				String e2 = o.elementAt(1);
				String e3 = o.elementAt(2);

				assertTrue((e2.equals(TWO) && e3.equals(THREE))
						|| (e2.equals(THREE) && e3.equals(TWO)));

				String e4 = o.elementAt(3);
				String e5 = o.elementAt(4);

				assertTrue((e4.equals(FOUR) && e5.equals(FIVE))
						|| (e4.equals(FIVE) && e5.equals(FOUR)));
				assertEquals(o.elementAt(5), SIX);
				latch.countDown();
			}
		});

		long start = System.currentTimeMillis();
		try {
			mgr.execute(false);
			long end = System.currentTimeMillis();
			System.out.println("Time 1: " + (end - start));
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		try {
			latch.await();
			long end = System.currentTimeMillis();
			System.out.println("Time 2: " + (end - start));
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		mgr.shutdown();
	}

	/**
	 * Tests running the graph with a much bigger graph and more, multiple threads
	 */
	@SuppressWarnings("unused")
	public void testMultithreadedRun2() {

		System.out.println("--------------testMultithreadedRun2----------------");
		// TaskManager mgr = new TaskManager(new LinkedBlockingQueue<Runnable>());
		TaskManager mgr = new TaskManager(4, 8, 60, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		createLargeGraphNoCycles(mgr);

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		Vector<String> o = output.getOutput();
		assertEquals(o.size(), 17);

		String e1 = o.elementAt(0);
		String e2 = o.elementAt(1);
		String e3 = o.elementAt(2);
		String e4 = o.elementAt(3);
		String e5 = o.elementAt(4);
		String e6 = o.elementAt(5);
		String e7 = o.elementAt(6);
		String e8 = o.elementAt(7);
		String e9 = o.elementAt(8);
		String e10 = o.elementAt(9);
		String e11 = o.elementAt(10);
		String e12 = o.elementAt(11);
		String e13 = o.elementAt(12);
		String e14 = o.elementAt(13);
		String e15 = o.elementAt(14);
		String e16 = o.elementAt(15);
		String e17 = o.elementAt(16);

		mgr.shutdown();
	}

	/**
	 * Creates a graph with a cycle and makes sure the TaskManager detects it
	 * 
	 */
	public void testCycleDetection() {
		System.out.println("--------------testCycleDetection----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphWithCycles(mgr);
		try {
			mgr.execute(false);
			assertTrue(false);
		} catch (CycleFoundException e) {
			// Expected exception
		} catch (UndefinedDependencyException e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		}
		mgr.shutdown();
	}

	/**
	 * Adds a node, SEVEN that depends on node FIVE but node THREE depends on
	 * 
	 */
	public void testCycleDetection2() {
		System.out.println("--------------testCycleDetection2----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphWithCycles2(mgr);
		try {
			mgr.execute(false);
			assertTrue(false);
		} catch (CycleFoundException e) {
			// Expected exception
		} catch (UndefinedDependencyException e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		}
		mgr.shutdown();
	}

	/**
	 * Adds a dependency to node ONE on node SIX
	 * 
	 */
	public void testCycleDetection3() {
		System.out.println("--------------testCycleDetection3----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);
		try {
			mgr.addDependency(ONE, SIX);
			mgr.execute(false);
			assertTrue(false);
		} catch (CycleFoundException e) {
			// Expected exception
		} catch (UndefinedDependencyException e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		}
		mgr.shutdown();
	}

	/**
	 * Tests the TaskManager's handling of adding tasks with the same name
	 */
	public void testNameClashes() {
		System.out.println("--------------testNameClashes----------------");
		TaskManager tm = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(tm);
		try {
			tm.addTask(ONE, new TestTask(ONE, output));
			assertTrue(false);
		} catch (DuplicateTaskNameException e) {
			// expected
		} catch (IllegalStateException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		tm.shutdown();
	}

	/**
	 * Tests that the TaskManager rejects assigning a task as a dependency to
	 * itself
	 */
	public void testTaskDependingOnSelf() {
		System.out.println("--------------testTaskDependingOnSelf----------------");
		TaskManager tm = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(tm);
		try {
			tm.addDependency(SIX, SIX);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			// expected
		} catch (IllegalStateException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		tm.shutdown();
	}

	/**
	 * Tests the TaskManager's handling of undefined dependencies
	 */
	public void testUndefinedDependencies() {
		System.out
				.println("--------------testUndefinedDependencies----------------");
		TaskManager tm = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(tm);

		try {
			tm.addDependency(TWO, "SEVEN");
			tm.execute(false);
			assertTrue(false);
		} catch (UndefinedDependencyException e) {
			// correct action
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		tm.shutdown();
	}

	/**
	 * Tests the TaskManager's placeholder system
	 */
	public void testOutOfOrderDependencyDeclarations() {
		System.out
				.println("--------------testOutOfOrderDependency----------------");
		TaskManager tm = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());

		try {
			tm.addTask(task2.getName(), task2);
			tm.addTask(task3.getName(), task3);
			tm.addDependency(TWO, ONE);
			tm.addDependency(THREE, ONE);
			tm.addTask(task4.getName(), task4);
			tm.addTask(task5.getName(), task5);
			tm.addDependencies(FOUR, new String[] { TWO, THREE });
			tm.addDependencies(FIVE, new String[] { TWO, THREE });
			tm.addTask(task6.getName(), task6);
			tm.addDependencies(SIX, new String[] { ONE, TWO, THREE, FOUR, FIVE });
			tm.addTask(task1.getName(), task1);
		} catch (Exception e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		final CountDownLatch latch = new CountDownLatch(1);

		tm.addListener(new TaskListenerAdapter() {
			@Override
			public void allTasksComplete() {
				Vector<String> o = output.getOutput();
				assertEquals(o.size(), 6);
				assertEquals(o.elementAt(0), ONE);
				assertEquals(o.elementAt(1), TWO);
				assertEquals(o.elementAt(2), THREE);
				assertEquals(o.elementAt(3), FOUR);
				assertEquals(o.elementAt(4), FIVE);
				assertEquals(o.elementAt(5), SIX);
				latch.countDown();
			}
		});

		try {
			tm.execute(false);
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		tm.shutdown();

	}

	/**
	 * Tests running the framework multiple times
	 */
	public void testMultipleRuns() {
		System.out.println("--------------testMultipleRuns----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		long start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		}
		long end = System.currentTimeMillis();
		System.out.println("Time (Run 1): " + (end - start));

		Vector<String> o = output.getOutput();
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);

		output.clear();

		start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Unexpected error thrown " + e.getMessage());
		}
		end = System.currentTimeMillis();
		System.out.println("Time (Run 2): " + (end - start));

		o = output.getOutput();
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);

		output.clear();

		start = System.currentTimeMillis();
		try {
			mgr.execute(true);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception thrown " + e.getMessage());
		}
		end = System.currentTimeMillis();
		System.out.println("Time (Run 3): " + (end - start));

		o = output.getOutput();
		assertEquals(o.size(), 6);
		assertEquals(o.elementAt(0), ONE);
		assertEquals(o.elementAt(1), TWO);
		assertEquals(o.elementAt(2), THREE);
		assertEquals(o.elementAt(3), FOUR);
		assertEquals(o.elementAt(4), FIVE);
		assertEquals(o.elementAt(5), SIX);
		mgr.shutdown();
	}

	/**
	 * Ensures that all listeners are being called properly
	 * 
	 */
	public void testTaskListeners() {
		System.out.println("--------------testTaskListeners----------------");
		TaskManager mgr = new TaskManager(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		createGraphNoCycles(mgr);

		final CountDownLatch latch = new CountDownLatch(4);

		TaskListener listener = new TaskListener(latch, 6);

		mgr.addListener(listener);

		try {
			mgr.execute(false);
		} catch (CycleFoundException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (UndefinedDependencyException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (BrokenBarrierException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
		assertTrue(listener.allStartingCalled);
		assertTrue(listener.taskStartedCalled);
		assertTrue(listener.completedCalled);
		assertTrue(listener.allCompletedCalled);

		mgr.shutdown();
	}

	private void createGraphNoCycles(TaskManager mgr) {
		addTasks(mgr);
		try {
			mgr.addDependency(TWO, ONE);
			mgr.addDependency(THREE, ONE);

			mgr.addDependencies(FOUR, new String[] { TWO, THREE });
			mgr.addDependencies(FIVE, new String[] { TWO, THREE });

			mgr.addDependencies(SIX, new String[] { ONE, TWO, THREE, FOUR, FIVE });
		} catch (Exception e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
	}

	private void createLargeGraphNoCycles(TaskManager mgr) {
		addTasks(mgr); // add tasks 1-6
		TestTask task8 = new TestTask("EIGHT", output);
		TestTask task9 = new TestTask("NINE", output);
		TestTask task10 = new TestTask("TEN", output);
		TestTask task11 = new TestTask("ELEVEN", output);
		TestTask task12 = new TestTask("TWELVE", output);
		TestTask task13 = new TestTask("THIRTEEN", output);
		TestTask task14 = new TestTask("FOURTEEN", output);
		TestTask task15 = new TestTask("FIFTEEN", output);
		TestTask task16 = new TestTask("SIXTEEN", output);
		TestTask task17 = new TestTask("SEVENTEEN", output);

		try {
			mgr.addTask(task7.getName(), task7);
			mgr.addTask(task8.getName(), task8);
			mgr.addTask(task9.getName(), task9);
			mgr.addTask(task10.getName(), task10);
			mgr.addTask(task11.getName(), task11);
			mgr.addTask(task12.getName(), task12);
			mgr.addTask(task13.getName(), task13);
			mgr.addTask(task14.getName(), task14);
			mgr.addTask(task15.getName(), task15);
			mgr.addTask(task16.getName(), task16);
			mgr.addTask(task17.getName(), task17);
			mgr.addDependencies(THREE, new String[] { ONE, "FIFTEEN" });
			mgr.addDependencies(FOUR, new String[] { ONE, TWO });
			mgr.addDependencies(FIVE, new String[] { TWO, "SIXTEEN" });
			mgr.addDependencies(SIX, new String[] { THREE });
			mgr.addDependencies(SEVEN, new String[] { THREE, FOUR });
			mgr.addDependencies("EIGHT", new String[] { FOUR, FIVE });
			mgr.addDependencies("NINE", new String[] { FIVE });
			mgr.addDependencies("TEN", new String[] { SIX });
			mgr.addDependencies("ELEVEN", new String[] { SIX, SEVEN });
			mgr.addDependencies("TWELVE", new String[] { SEVEN, "EIGHT" });
			mgr.addDependencies("THIRTEEN", new String[] { "EIGHT", "NINE" });
			mgr.addDependencies("FOURTEEN", new String[] { "NINE" });
		} catch (DuplicateTaskNameException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (IllegalStateException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}

	}

	private void addTasks(TaskManager mgr) {
		try {
			mgr.addTask(task1.getName(), task1);
			mgr.addTask(task2.getName(), task2);
			mgr.addTask(task3.getName(), task3);
			mgr.addTask(task4.getName(), task4);
			mgr.addTask(task5.getName(), task5);
			mgr.addTask(task6.getName(), task6);
		} catch (Exception e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
	}

	/**
	 * Creates a dependency graph with cycles that should make it illegal
	 * 
	 * @param mgr
	 */
	private void createGraphWithCycles(TaskManager mgr) {
		addTasks(mgr);
		try {
			mgr.addDependency(TWO, ONE);
			mgr.addDependency(THREE, ONE);

			// Here there be cycles
			mgr.addDependencies(FOUR, new String[] { TWO, THREE, FIVE });
			mgr.addDependencies(FIVE, new String[] { TWO, THREE, FOUR });

			mgr.addDependencies(SIX, new String[] { ONE, TWO, THREE, FOUR, FIVE });
		} catch (IllegalStateException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
	}

	private void createGraphWithCycles2(TaskManager mgr) {
		addTasks(mgr);
		try {
			mgr.addTask(task7.getName(), task7);
			mgr.addDependency(TWO, ONE);
			mgr.addDependencies(THREE, new String[] { ONE, SEVEN });

			mgr.addDependencies(FOUR, new String[] { TWO, THREE });
			mgr.addDependencies(FIVE, new String[] { TWO, THREE });

			// mgr.addDependencies(SIX, new String[]{ONE, TWO, THREE, FOUR,
			// FIVE});
			mgr.addDependencies(SIX, new String[] { FOUR, FIVE });
			mgr.addDependencies(SEVEN, new String[] { FIVE });
		} catch (DuplicateTaskNameException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		} catch (IllegalStateException e) {
			fail("Unexpected exception thrown " + e.getMessage());
		}
	}

	private static class TestTask implements Runnable {
		private final String name;

		private final BufferingOutput output;

		public TestTask(String name, BufferingOutput output) {
			super();
			this.name = name;
			this.output = output;
		}

		public String getName() {
			return name;
		}
	    @Override
		public void run() {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			    // Ignore
			}
			System.out.println(name);
			output.append(name);
		}
	}

	private static class BufferingOutput {
		private final Vector<String> output;

		// private final static BufferingOutput instance = new
		// BufferingOutput();

		public BufferingOutput() {
			output = new Vector<String>();
		}

		// public static BufferingOutput getInstance()
		// {
		// return instance;
		// }

		public synchronized void append(String string) {
			output.add(string);
		}

		public synchronized Vector<String> getOutput() {
			return output;
		}

		public synchronized void clear() {
			output.clear();
		}

	}

	private static class TaskListener implements ITaskListener {
		public boolean allCompletedCalled = false;

		public boolean allStartingCalled = false;

		public boolean completedCalled = false;

		public boolean taskStartedCalled = false;

		private final CountDownLatch latch;

		private int taskCompletedCount;

		private int taskStartedCount;

		public TaskListener(CountDownLatch latch, int taskCount) {
			this.latch = latch;
			this.taskCompletedCount = taskCount;
			this.taskStartedCount = taskCount;
		}
	    @Override
		public void allTasksComplete() {
			allCompletedCalled = true;
			assertTrue(allCompletedCalled && allStartingCalled && completedCalled
					&& taskStartedCalled);
			latch.countDown();
		}
	    @Override
		public void startingTasks() {
			allStartingCalled = true;
			assertTrue(!allCompletedCalled && allStartingCalled && !completedCalled
					&& !taskStartedCalled);
			latch.countDown();
		}
	    @Override
		public void taskCompleted(Runnable task) {
			taskCompletedCount--;
			if (taskCompletedCount == 0) {
				completedCalled = true;
				assertTrue(!allCompletedCalled && allStartingCalled && completedCalled
						&& taskStartedCalled);
				latch.countDown();
			}
		}
	    @Override
		public void taskStarted(Runnable task) {
			taskStartedCount--;
			if (taskStartedCount == 0) {
				taskStartedCalled = true;
				assertTrue(!allCompletedCalled && allStartingCalled && !completedCalled
						&& taskStartedCalled);
				latch.countDown();
			}
		}

	}
}
