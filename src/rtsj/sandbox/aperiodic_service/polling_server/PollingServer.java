package rtsj.sandbox.aperiodic_service.polling_server;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.Timed;
import javax.realtime.memory.LTMemory;
import javax.realtime.memory.ScopedMemory;

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
 * 
 * Implements the PollingServer algorithm for aperiodic service.
 * 
 * INVARIANTS:
 * 
 * 1) remainingBudget >= 0
 * 
 * @author savvas
 *
 */
public class PollingServer extends RealtimeThread {

	private static final int SCOPED_MEM_SIZE = 10_000_000;

	private final RelativeTime totalBudget;
	private final Timed timed;
	private final AperiodicEventPriorityQueue<RunnableAperiodicEvent> eventQueue;
	// These values may be modified from within scoped memory (through the set*()
	// methods), hence it's a good idea if all assignments are made here
	private final AbsoluteTime eventProcessingStart;
	private final AbsoluteTime eventProcessingEnd;
	private final RelativeTime totalProcessingCost;

	private final RelativeTime remainingBudget;
	private final ScopedMemory mem;

	/**
	 * REQUIRES:
	 * 
	 * 1) budget <= period
	 * 
	 * 2) priority >= 0
	 * 
	 */
	@SuppressWarnings("unchecked")
	public PollingServer(int priority, RelativeTime period, RelativeTime budget,
			AperiodicEventPriorityQueue<RunnableAperiodicEvent> eventQueue) {
		assert period.compareTo(budget) >= 0 : "period must not be less than capacity";
		assert priority >= 0 : "priority must not be negative";
		setSchedulingParameters(new PriorityParameters(priority));
		setReleaseParameters(new PeriodicParameters(period));
		setName("PollingServer");
		eventProcessingStart = new AbsoluteTime();
		eventProcessingEnd = new AbsoluteTime();
		totalProcessingCost = new RelativeTime();
		totalBudget = new RelativeTime(budget);
		remainingBudget = new RelativeTime(totalBudget);
		timed = new Timed(remainingBudget);
		this.eventQueue = eventQueue;
		mem = new LTMemory(SCOPED_MEM_SIZE);
	}

	/**
	 * TODO: clone() in *Time classes does not work??
	 */
	@Override
	public void run() {
		while (true) {
			// TODO: enter() can only wrap the doInterruptible(..) part?
			mem.enter(new Runnable() {
				@Override
				public void run() {
					for (RunnableAperiodicEvent event = eventQueue.pop(); canProcessEvent(
							event); event = eventQueue.pop()) {
						Clock.getRealtimeClock().getTime(eventProcessingStart);
						timed.doInterruptible(event);
						Clock.getRealtimeClock().getTime(eventProcessingEnd);
						eventProcessingEnd.subtract(eventProcessingStart, totalProcessingCost);
						amendRemainingBudget();
						assert (remainingBudget.compareToZero() >= 0) : "remainingBudget must not be negative";
						// adjust new interrupt timeout
						timed.resetTime(remainingBudget);
						// re-push if interrupted and can restart so that event is processed in
						// subsequent runs
						if (event.wasInterrupted() && event.canRestart()) {
							eventQueue.push(event);
						}
					}
				}
			});
			resetForNextPeriod();
			waitForNextPeriod();
		}
	}

	private boolean canProcessEvent(RunnableAperiodicEvent event) {
		return (event != null) && (remainingBudget.compareToZero() > 0);
	}

	private void amendRemainingBudget() {
		remainingBudget.subtract(totalProcessingCost, remainingBudget);
		// maintain class invariant
		if (remainingBudget.compareToZero() < 0) {
			remainingBudget.set(0, 0);
		}
	}

	private void resetForNextPeriod() {
		// refill budget and adjust new interruption timeout
		remainingBudget.set(totalBudget.getMilliseconds(), totalBudget.getNanoseconds());
		timed.resetTime(totalBudget);
	}
}