package rtsj.sandbox.atc.experiment;

import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.Interruptible;
import javax.realtime.PeriodicParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.ReleaseParameters;

public class InterrupteeInNonATCDeferredBlocking extends RealtimeThread implements Interruptible {

	private AsynchronouslyInterruptedException exception;

	public InterrupteeInNonATCDeferredBlocking(AsynchronouslyInterruptedException exception) {

		this.exception = exception;

		RelativeTime period = new RelativeTime(5_000, 0);
		ReleaseParameters rel = new PeriodicParameters(period);
		setReleaseParameters(rel);
	}

	@Override
	public void run() {

		exception.doInterruptible(this);
	}

	@Override
	public void interruptAction(AsynchronouslyInterruptedException exception) {

		exception.clear();
		System.out.println("Interruptee: inside interruptAction()");
	}

	@Override
	public void run(AsynchronouslyInterruptedException exception) throws AsynchronouslyInterruptedException {

		// ps: this.exception == exception
		System.out.println("Exception enabled ? " + this.exception.isEnabled());

		while (true) {

			System.out.println("Interruptee: inside loop...");

			// non ATC-deferred
			waitForNextPeriod();

			System.out.println("Was interrupted ? " + (isInterrupted() ? "Yes" : "No"));
		}
	}
}
