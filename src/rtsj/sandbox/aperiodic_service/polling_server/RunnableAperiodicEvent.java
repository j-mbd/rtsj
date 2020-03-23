package rtsj.sandbox.aperiodic_service.polling_server;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.Interruptible;
import javax.realtime.RelativeTime;

/**
 * RunnableAperiodicEvents implement Interruptible so they can be used as the
 * target of AsynchronouslyInterruptedException* classes where the "run" method
 * specifies main processing and the "interruptAction" the logic to apply when
 * the handler's budget is about to be exceeded and processing hasn't finished
 * yet.
 * 
 * Not thread-safe.
 * 
 * @author savvas
 *
 */

public abstract class RunnableAperiodicEvent implements Interruptible {

	protected final RelativeTime totalCost;
	protected final RelativeTime deadline;
	protected final AbsoluteTime absoluteDeadline;
	protected final AbsoluteTime creationTime;
	protected final String name;

	protected boolean wasInterrupted;
	protected boolean canRestart;
	protected RelativeTime remainingCost;

	protected RunnableAperiodicEvent(RelativeTime cost, RelativeTime deadline, String name) {
		this.totalCost = cost;
		this.deadline = deadline;
		absoluteDeadline = Clock.getRealtimeClock().getTime().add(deadline);
		creationTime = Clock.getRealtimeClock().getTime();
		this.name = name;
		wasInterrupted = false;
		canRestart = true;
		remainingCost = new RelativeTime(totalCost);
	}

	// ****************************STATE REPORT****************************
	/**
	 * What is the total cost of this event?
	 * 
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public RelativeTime cost() {
		return new RelativeTime(totalCost);
	}

	/**
	 * What is the remaining cost of this event after some processing?
	 * 
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public RelativeTime remainingCost() {
		return new RelativeTime(remainingCost);
	}

	/**
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public RelativeTime relativeDeadline() {
		return new RelativeTime(deadline);
	}

	/**
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public AbsoluteTime absoluteDeadline() {
		return new AbsoluteTime(absoluteDeadline);
	}

	/**
	 * Not necessarily same as "arrival time".
	 * 
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public AbsoluteTime creationTime() {
		return new AbsoluteTime(creationTime);
	}

	public boolean wasInterrupted() {
		return wasInterrupted;
	}

	/**
	 * Can processing for this event restart after being interrupted?
	 * 
	 * "true" implies (isRestartable() == true)
	 * 
	 * @return
	 */
	public boolean canRestart() {
		return canRestart;
	}

	/**
	 * Is this event of type "restartable"?
	 * 
	 * @return
	 */
	public boolean isRestartable() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean deadlineMissed() {
		return Clock.getRealtimeClock().getTime().compareTo(absoluteDeadline) > 0;
	}

	// ****************************STATE CHANGE****************************

	public void reset() {
		wasInterrupted = false;
		canRestart = true;
	}
}
