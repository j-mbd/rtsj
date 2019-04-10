package rtsj.sandbox.atc.experiment;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.Clock;
import javax.realtime.Interruptible;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;

public class InterrupteeInATCDeferredNonBlocking extends RealtimeThread implements Interruptible {

	private AsynchronouslyInterruptedException exception;

	private Clock clock;

	public InterrupteeInATCDeferredNonBlocking(AsynchronouslyInterruptedException exception) {

		this.exception = exception;
		this.clock = Clock.getRealtimeClock();
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

		RelativeTime waitTime = new RelativeTime(5_000, 0);

		while (true) {

			System.out.println("Interruptee: inside loop...");

			busyWaitBlock(waitTime);

			System.out.println("Was interrupted ? " + (isInterrupted() ? "Yes" : "No"));
		}
	}

	private void busyWaitBlock(RelativeTime waitTime) throws AsynchronouslyInterruptedException {

		System.out.println("Busy waiting...");

		AbsoluteTime now = clock.getTime();
		AbsoluteTime waitUntil = now.add(waitTime);

		while (now.compareTo(waitUntil) < 0) {
			now = clock.getTime();
		}

		System.out.println("Finished busy waiting...");
	}
}