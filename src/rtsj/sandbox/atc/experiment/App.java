package rtsj.sandbox.atc.experiment;

import javax.realtime.AsynchronouslyInterruptedException;

public class App {

	public static void main(String... args) throws Exception {

		// All code executed in Heap-Memory

		AsynchronouslyInterruptedException exception = new AsynchronouslyInterruptedException();

		// new InterrupteeInNonATCDeferredBlocking(exception).start();
		// new InterrupteeInATCDeferredBlocking(exception).start();
		// new InterrupteeInNonATCDeferredNonBlocking(exception).start();
		new InterrupteeInATCDeferredNonBlocking(exception).start();
		new Interrupter(exception).start();
	}
}
