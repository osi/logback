package ch.qos.logback.classic.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import junit.framework.TestCase;
import ch.qos.logback.classic.net.testObjectBuilders.Builder;
import ch.qos.logback.classic.net.testObjectBuilders.LoggingEvent2Builder;
import ch.qos.logback.classic.net.testObjectBuilders.LoggingEventBuilder;
import ch.qos.logback.classic.net.testObjectBuilders.MinimalExtBuilder;
import ch.qos.logback.classic.net.testObjectBuilders.MinimalSerBuilder;

public class SerializationPerfsTest extends TestCase {

	ObjectOutputStream oos;
	MockSocketServer mockServer;

	int loopNumber = 10000;
	int resetFrequency = 100;
	/**
	 * Run the test with a MockSocketServer or with a NOPOutputStream
	 */
	boolean runWithMockServer = true;
	/**
	 * <p>
	 * Run with external mock can be done using the
	 * ExternalMockSocketServer. It needs to be launched
	 * from a separate JVM.
	 * </p>
	 * <p>
	 * For example, with 4 test methods and a loopNumber of 10000,
	 * you can launch the ExternalMockSocketServer this way:
	 * </p>
	 * <p>
	 * <code>java ch.qos.logback.classic.net.ExternalMockSocketServer 4 20000</code>
	 * </p>
	 * <p>
	 * (20000 because each methods iterate twice).
	 * </p>
	 */
	boolean runWithExternalMockServer = true;

	/**
	 * Last results:
	 * 
	 * NOPOutputStream: 
	 * Minimal Object externalization: average time = 6511 after 10000 writes. 
	 * Minimal Object serialization: average time = 7883 after 10000 writes. 
	 * LoggingEvent object externalization: average time = 9641 after 10000 writes.
	 * LoggingEvent object serialization: average time = 25729 after 10000 writes.
	 * 
	 * Internal MockServer: 
	 * Minimal object externalization : average time = 62040 after 10000 writes.
	 * Minimal object serialization : average time = 76237 after 10000 writes.
	 * LoggingEvent object externalization : average time = 122714 after 10000 writes.
	 * LoggingEvent object serialization : average time = 121711 after 10000 writes.
	 * 
	 * External MockServer: 
	 * Minimal object externalization : average time = 55577 after 10000 writes.
	 * Minimal object serialization : average time = 56669 after 10000 writes.
	 * LoggingEvent object externalization : average time = 121477 after 10000 writes.
	 * LoggingEvent object serialization : average time = 111148 after 10000 writes.
	 */

	public void setUp() throws Exception {
		super.setUp();
		if (runWithMockServer) {
			if (!runWithExternalMockServer) {
				mockServer = new MockSocketServer(loopNumber * 2);
				mockServer.start();
			}
			oos = new ObjectOutputStream(new Socket("localhost",
					MockSocketServer.PORT).getOutputStream());
		} else {
			oos = new ObjectOutputStream(new NOPOutputStream());
		}
	}

	public void tearDown() throws Exception {
		super.tearDown();
		oos.close();
		oos = null;
		mockServer = null;
	}

	public void runPerfTest(Builder builder, String label) throws Exception {

		// first run for just in time compiler
		int counter = 0;
		for (int i = 0; i < loopNumber; i++) {
			try {
				oos.writeObject(builder.build(i));
				oos.flush();
				if (++counter >= resetFrequency) {
					oos.reset();
				}
			} catch (IOException ex) {
				fail(ex.getMessage());
			}
		}

		// second run
		Long t1;
		Long t2;
		Long total = 0L;
		counter = 0;
		// System.out.println("Beginning mesured run");
		t1 = System.nanoTime();
		for (int i = 0; i < loopNumber; i++) {
			try {
				oos.writeObject(builder.build(i));
				oos.flush();
				if (++counter >= resetFrequency) {
					oos.reset();
				}
			} catch (IOException ex) {
				fail(ex.getMessage());
			}
		}
		t2 = System.nanoTime();
		total += (t2 - t1);
		System.out.println(label + " : average time = " + total / loopNumber
				+ " after " + loopNumber + " writes.");

		if (runWithMockServer && !runWithExternalMockServer) {
			mockServer.join(1000);
			assertTrue(mockServer.finished);
		}
	}

	public void testWithMinimalExternalization() throws Exception {
		Builder builder = new MinimalExtBuilder();
		runPerfTest(builder, "Minimal object externalization");
	}

	public void testWithMinimalSerialization() throws Exception {
		Builder builder = new MinimalSerBuilder();
		runPerfTest(builder, "Minimal object serialization");
	}

	public void testWithExternalization() throws Exception {
		Builder builder = new LoggingEventBuilder();
		runPerfTest(builder, "LoggingEvent object externalization");
	}

	public void testWithSerialization() throws Exception {
		Builder builder = new LoggingEvent2Builder();
		runPerfTest(builder, "LoggingEvent object serialization");
	}
}