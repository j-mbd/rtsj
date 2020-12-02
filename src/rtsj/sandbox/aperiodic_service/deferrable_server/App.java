package rtsj.sandbox.aperiodic_service.deferrable_server;

import java.util.Random;

import javax.realtime.AsyncEvent;
import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;
import javax.realtime.memory.LTMemory;
import javax.realtime.memory.ScopedMemory;

import rtsj.sandbox.aperiodic_service.common.AperiodicEventPriorityQueue;
import rtsj.sandbox.aperiodic_service.common.InterruptibleAperiodicEvent;
import rtsj.sandbox.aperiodic_service.common.PeriodicTask;
import rtsj.sandbox.aperiodic_service.common.RestartableAperiodicEvent;

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
 * NOTE: Values for all components are largely random.
 * 
 */

public class App {

	public static void main(String... args) {

		int maxPriority = PriorityScheduler.instance().getMaxPriority();

		// The task-set
		Thread t1 = new PeriodicTask(maxPriority - 2, 40, new RelativeTime(10, 0), "Thread#1");
		// higher period, lower priority
		Thread t2 = new PeriodicTask(maxPriority - 3, 60, new RelativeTime(20, 0), "Thread#2");
		t1.start();
		t2.start();

		AperiodicEventPriorityQueue<InterruptibleAperiodicEvent> q = new AperiodicEventPriorityQueue<InterruptibleAperiodicEvent>();
		AsyncEvent apeEvent = new AsyncEvent();

		// assigning deferrable-server highest priority (relative to the task-set) as we
		// can't guarantee ties will be broken in favour of the server
		ScopedMemory mem = new LTMemory(2048);
		int backgroundPriority = PriorityScheduler.instance().getMinPriority();
		DeferrableServerEventHandler eventHandler = new DeferrableServerEventHandler(q, new RelativeTime(50, 0),
				maxPriority - 1, backgroundPriority, mem, false);
		apeEvent.addHandler(eventHandler);
		// assigning the budget replenisher the highest priority
		new DeferrableServerBudgetReplenisher(new RelativeTime(100, 0), maxPriority, eventHandler, apeEvent, mem)
				.start();
		beginEventGeneration(q, apeEvent);
	}

	private static void beginEventGeneration(AperiodicEventPriorityQueue<InterruptibleAperiodicEvent> q,
			AsyncEvent apeEvent) {
		Random r = new Random();
		final int maxDelay = 5_000;
		int nextDelay = 0;
		int eventCounter = 0;

		// Event properties
		RelativeTime cost = new RelativeTime(150, 0);
		RelativeTime deadline = new RelativeTime(200, 0);

		while (true) {
			nextDelay = r.nextInt(maxDelay);
			String name = "Event#" + eventCounter++;

			RestartableAperiodicEvent event = new RestartableAperiodicEvent(cost, deadline, name);
			q.push(event);
			apeEvent.fire();
			try {
				Thread.sleep(nextDelay);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}
}
