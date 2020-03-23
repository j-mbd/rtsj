package rtsj.sandbox.aperiodic_service.polling_server;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.Clock;
import javax.realtime.RelativeTime;

/**
 * Keeps track of how much of its cost has been used-up and how much is
 * remaining. This way, if processing is asynchronously interrupted, the
 * remaining processing cost is calculated and applied on the next run.
 * 
 * @author savvas
 *
 */
public class RestartableAperiodicEvent extends RunnableAperiodicEvent implements Comparable<RestartableAperiodicEvent> {

	// These values may be modified from within scoped memory (through the set*()
	// methods), hence it's a good idea if all assignments are made here
	private RelativeTime remainingCost = new RelativeTime();
	private AbsoluteTime processingStart = new AbsoluteTime();
	private AbsoluteTime processingInterruptedEnd = new AbsoluteTime();

	public RestartableAperiodicEvent(RelativeTime cost, RelativeTime deadline, String name) {
		super(cost, deadline, name);
		remainingCost = new RelativeTime(totalCost);
	}

	@Override
	public void interruptAction(AsynchronouslyInterruptedException exception) {
		System.out.println("Processing event " + name + " interrupted at " + Clock.getRealtimeClock().getTime());
		wasInterrupted = true;
		Clock.getRealtimeClock().getTime(processingInterruptedEnd);
		recalculateRemainingCost();
		if (remainingCost.compareToZero() <= 0 || deadlineMissed()) {
			canRestart = false;
		} else {
			canRestart = true;
		}
		if (deadlineMissed()) {
			handleDeadlineMiss();
		}
	}

	@Override
	public void run(AsynchronouslyInterruptedException exception) throws AsynchronouslyInterruptedException {
		Clock.getRealtimeClock().getTime(processingStart);
		System.out.println("Processing event " + name + " at " + Clock.getRealtimeClock().getTime());
		TimeUtils.spinWait(remainingCost);
		wasInterrupted = false;
		System.out.println(
				"Processing of event " + name + " complteted successfully at " + Clock.getRealtimeClock().getTime());
	}

	@Override
	public String toString() {
		return "RestartableAperiodicEvent [absoluteDeadline=" + absoluteDeadline + ", cost=" + totalCost + ", deadline="
				+ deadline + ", creationTime=" + creationTime + ", name=" + name + "]";
	}

	@Override
	public boolean isRestartable() {
		return true;
	}

	@Override
	public RelativeTime remainingCost() {
		return new RelativeTime(remainingCost);
	}

	private void recalculateRemainingCost() {
		RelativeTime totalCostToInterruprion = processingInterruptedEnd.subtract(processingStart);
		remainingCost.subtract(totalCostToInterruprion, remainingCost);
	}

	private void handleDeadlineMiss() {
		// Could also create a separate class whose instance is wired and used here
		System.out.println("Deadline missed for event " + this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(RestartableAperiodicEvent other) {
		return this.remainingCost.compareTo(other.remainingCost);
	}
}
