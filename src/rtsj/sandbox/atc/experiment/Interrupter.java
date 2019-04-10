package rtsj.sandbox.atc.experiment;

import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;

public class Interrupter extends RealtimeThread {

	private AsynchronouslyInterruptedException exception;
	private RelativeTime interruptTimeout;

	public Interrupter(AsynchronouslyInterruptedException exception) {

		this.exception = exception;
		// Default interrupt time - 10 seconds
		interruptTimeout = new RelativeTime(12_000, 0);
	}

	public void setException(AsynchronouslyInterruptedException exception) {

		this.exception = exception;
	}

	public void setInterruptTimeout(RelativeTime interruptTimeout) {

		this.interruptTimeout = interruptTimeout;
	}

	@Override
	public void run() {

		try {

			sleep(interruptTimeout);

			System.out.println("Timeout expired, firing exception");
			exception.fire();
		} catch (InterruptedException e) {
		}
	}
}
