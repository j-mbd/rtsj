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
 * AVAILABLE.
 * 
 * 
 * Implements the Polling-Server algorithm for aperiodic service.
 * 
 * INVARIANTS:
 * 
 * 1) (remainingBudget >= 0) && (remainingBudget <= totalBudget)
 *
 */
public class PollingServer extends RealtimeThread {

	private static final int SCOPED_MEM_SIZE = 10_000_000;

	private final Clock clk;
	private final RelativeTime totalBudget;
	private final Timed timed;
	private final AperiodicEventPriorityQueue<InterruptibleAperiodicEvent> eventQueue;
	// These values may be modified from within scoped memory (through the set*()
	// methods), hence it's a good idea if all assignments are made here and are
	// final.
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
			AperiodicEventPriorityQueue<InterruptibleAperiodicEvent> eventQueue) {
		assert period.compareTo(budget) >= 0 : "period must not be less than capacity";
		assert priority >= 0 : "priority must not be negative";
		clk = Clock.getRealtimeClock();
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

	@Override
	public void run() {
		while (true) {
			mem.enter(() -> {
				for (InterruptibleAperiodicEvent event = eventQueue.pop(); canProcessEvent(
						event); event = eventQueue.pop()) {
					clk.getTime(eventProcessingStart);
					timed.doInterruptible(event);
					clk.getTime(eventProcessingEnd);
					adjustRemainingBudget();
					// re-push event if interrupted and it can restart so that event is processed in
					// subsequent runs
					if (event.wasInterrupted() && event.canRestart()) {
						event.reset();
						eventQueue.push(event);
					}
				}

			});
			resetForNextPeriod();
			waitForNextPeriod();
		}
	}

	private boolean canProcessEvent(InterruptibleAperiodicEvent event) {
		return (event != null) && (remainingBudget.compareToZero() > 0);
	}

	@SuppressWarnings("unchecked")
	private void adjustRemainingBudget() {
		eventProcessingEnd.subtract(eventProcessingStart, totalProcessingCost);
		remainingBudget.subtract(totalProcessingCost, remainingBudget);
		// maintain class invariant
		if (remainingBudget.compareToZero() < 0) {
			remainingBudget.set(0, 0);
		}
		// adjust new interrupt timeout
		timed.resetTime(remainingBudget);
		assert (remainingBudget.compareToZero() >= 0 && remainingBudget
				.compareTo(totalBudget) <= 0) : "remainingBudget must not be negative or more than totalBudget";
	}

	private void resetForNextPeriod() {
		// refill budget and adjust new interruption timeout
		remainingBudget.set(totalBudget.getMilliseconds(), totalBudget.getNanoseconds());
		timed.resetTime(totalBudget);
	}
}