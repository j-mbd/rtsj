package rtsj.sandbox.aperiodic_service.deferrable_server;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.Timed;

import rtsj.sandbox.aperiodic_service.common.AperiodicEventPriorityQueue;
import rtsj.sandbox.aperiodic_service.common.InterruptibleAperiodicEvent;

/**
 * THIS SOFTWARE IS PROVIDED BY Savvas Moysidis “AS IS” AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL Savvas Moysidis BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * IMPORTANT NOTE: COULD NOT BE TESTED AS PERSONAL EDITION VMs ARE NO LONGER
 * FREELY (OR EVEN NOT FREELY) AVAILABLE.
 * 
 * 
 * Together with DeferrableServerBudgetReplenisher forms a logical
 * implementation of the Deferrable-Server algorithm for aperiodic service.
 * Responsible for handling each aperiodic event and managing the aperiodic
 * events queue.
 * 
 * In order for the schedulability analysis to be correct under RM, it's
 * priority must be equal to the highest priority of the periodic task-set
 * running alongside this server (i.e. task with shortest period) _only_ if
 * priority ties are resolved in favour of the server. If they aren't, then this
 * server must be given a higher priority.
 * 
 * INVARIANTS:
 * 
 * 1) (remainingBudget >= 0) && (remainingBudget <= totalBudget)
 * 
 */
public class DeferrableServerEventHandler extends AsyncEventHandler {

	private final Timed timed;
	private final Clock clk;
	// volatile since it is assigned in a non-synchronised manner at least once
	private volatile Thread handlerThread;

	// These values may be modified from within scoped memory (through the set*()
	// methods), hence it's a good idea if all assignments are made in constructor
	// and are final
	private final AperiodicEventPriorityQueue<InterruptibleAperiodicEvent> eventQueue;
	private final RelativeTime totalBudget;
	private final RelativeTime remainingBudget;
	private final RelativeTime totalProccessingCost;

	private final AbsoluteTime eventProcessingStart;
	private final AbsoluteTime eventProcessingEnd;

	public DeferrableServerEventHandler(AperiodicEventPriorityQueue<InterruptibleAperiodicEvent> eventQueue,
			RelativeTime totalBudget, int priority, MemoryArea memoryArea, boolean noHeap) {
		super(new PriorityParameters(priority), null, null, memoryArea, null, noHeap);
		this.eventQueue = eventQueue;
		this.totalBudget = new RelativeTime(totalBudget);

		// start with a full budget
		this.remainingBudget = new RelativeTime(this.totalBudget);
		timed = new Timed(this.totalBudget);

		this.totalProccessingCost = new RelativeTime();
		this.eventProcessingStart = new AbsoluteTime();
		this.eventProcessingEnd = new AbsoluteTime();

		clk = Clock.getRealtimeClock();
	}

	/**
	 * The budget replenisher is a periodic thread
	 * (DeferrableServerBudgetReplenisher) which runs at a higher priority than this
	 * handler, preempts it and calls replenishBudget() on this object. When this
	 * happens there are two (notional) states this handler could be in: waiting, if
	 * it's budget has been exceeded or there are no more events to process or it
	 * could be running (note, it cannot have been preempted or blocked since it
	 * runs at a priority higher than the periodic task set and doesn't synchronise
	 * on any common resource). If it is running, then processing needs to be
	 * paused, the budget replenished and processing of the _same_ event resumed.
	 * 
	 * This becomes important when the events queue applies some ordering other than
	 * arrival time:
	 * 
	 * Assume (tc) is the current time, (tr) the next replenish time and the
	 * remaining server budget is (tr + a -tc) where tc < tr < (tr + a), so current
	 * processing would be pushed to just over the next replenish time. Then, if the
	 * remaining event cost is greater than (tr + a - tc) the event would be
	 * interrupted at exactly that time ( i.e. tr + a), it would be put back into
	 * the queue (assuming it's restartable) and a newer event with a smaller cost
	 * (assuming ordering by cost) would effectively "preempt" it. To prevent this
	 * from happening, the event is interrupted upon each replenish event, the
	 * budget is replenished and the same event is subsequently restarted. This will
	 * guarantee a continuation in the handler's behaviour.
	 * 
	 * NOTE: The reason the above analysis is relevant is due to the fact that this
	 * application is compiled against a 1.1 version of the spec and relies on the
	 * resetTime(...) method of Timed class which resets the time for the _next_
	 * invocation of doInterruptible(..). The newest 2.0 spec supports a method
	 * called restart(...) which adjusts the timeout and restarts the timer _whilst_
	 * the Timed object is running, therefore making all this logic and checking the
	 * different interrupt types in events, redundant.
	 * 
	 * NOTE: Will run in memory area passed in the constructor.
	 */
	@Override
	public void handleAsyncEvent() {
		try {
			// stash handling thread in a var so that the replenisher can interrupt it
			// (typically a single thread per priority in non-BoundAsyncEventHandler)
			handlerThread = RealtimeThread.currentThread();
			// pending fire-count is not important here as this handler is driven
			// exclusively by the events queue - clear this for consistency
			getAndClearPendingFireCount();

			while (canProcessEvent()) {
				InterruptibleAperiodicEvent event = eventQueue.pop();
				runAndAdjustRemainingBudget(event);
				if (event.wasGenericInterrupted()) {
					// event was interrupted by budget-replenish event. With budget now replenished,
					// try running the _same_ event to completion
					runAndAdjustRemainingBudget(event);
				}
				if (event.wasInterrupted() && event.canRestart()) {
					// event was interrupted by budget-depletion event. Re-push so that it is
					// processed in subsequent runs
					event.reset();
					eventQueue.push(event);
				}
			}
		} finally {
			synchronized (this) {
				// clear any potential interrupt flag set by the replenisher but didn't
				// cause an event interrupt
				RealtimeThread.interrupted();
				// null-out handler thread reference so that the replenisher doesn't set the
				// interrupt flag to a thread that's no longer servicing this handler
				handlerThread = null;
			}
		}
	}

	/**
	 * NOTE: The replenisher will cause a generic AIE to be raised (since it's
	 * interrupting via interrupt()) and it is possible that when this happens there
	 * is already a pending AIE in the stack (i.e. Timed has fired only slightly
	 * sooner). This however will not affect the expected behaviour of the
	 * replenisher getting priority, as based on the AIE nesting rules the generic
	 * AIE will overrule (and replace) any pending AIEs.
	 */
	@SuppressWarnings("unchecked")
	public synchronized void replenishBudget() {
		if (remainingBudget.compareTo(totalBudget) == 0) {
			// no events processed since last replenish time - nothing to change
			return;
		}
		// maintain class invariant
		remainingBudget.set(totalBudget.getMilliseconds(), totalBudget.getNanoseconds());
		timed.resetTime(totalBudget);
		if (handlerThread != null) {
			// interrupt status for thread is cleared in event (RestartableAperiodicEvent)
			// or when this handler completes a run
			handlerThread.interrupt();
		} else {
			// NOP: this handler hasn't processed any events yet
		}
		assertClassInvariants();
	}

	/**
	 * Is there an event in the queue and is there budget to run it?
	 * 
	 * @return
	 */
	private synchronized boolean canProcessEvent() {
		// as this method is synchronised (protecting "remainingBudget" from
		// budget-replenisher) and this handler is meant to be the _only_ consumer of
		// the queue the following predicates are race-free.
		return (!eventQueue.isEmpty()) && (remainingBudget.compareToZero() > 0);
		// _if_ however there can be more than one consumers then an event needs
		// to be popped and used in the following predicates ("event" is passed as
		// argument) return (event != null) && (remainingBudget.compareToZero() > 0);
	}

	private void runAndAdjustRemainingBudget(InterruptibleAperiodicEvent event) {
		clk.getTime(eventProcessingStart);
		timed.doInterruptible(event);
		clk.getTime(eventProcessingEnd);
		eventProcessingEnd.subtract(eventProcessingStart, totalProccessingCost);
		adjustForNextRun();
	}

	private synchronized void adjustForNextRun() {
		remainingBudget.subtract(totalProccessingCost, remainingBudget);
		// maintain class invariant
		if (remainingBudget.compareToZero() < 0) {
			remainingBudget.set(0, 0);
		}
		// adjust new interrupt timeout
		timed.resetTime(remainingBudget);
		assertClassInvariants();
	}

	@SuppressWarnings("unchecked")
	private void assertClassInvariants() {
		assert (remainingBudget.compareToZero() >= 0 && remainingBudget
				.compareTo(totalBudget) <= 0) : "remainingBudget must not be negative or more than totalBudget";
	}
}
