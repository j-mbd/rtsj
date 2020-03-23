package rtsj.sandbox.aperiodic_service.polling_server;

import java.util.Random;

import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;

public class App {

	public static void main(String... args) {

		int maxPriority = PriorityScheduler.instance().getMaxPriority();

		Thread t1 = new PeriodicTask(maxPriority - 1, 40, new RelativeTime(10, 0), "Thread#1");
		// higher period, lower priority
		Thread t2 = new PeriodicTask(maxPriority - 2, 60, new RelativeTime(20, 0), "Thread#2");

		t1.start();
		t2.start();

		AperiodicEventPriorityQueue<RunnableAperiodicEvent> q = new AperiodicEventPriorityQueue<RunnableAperiodicEvent>();

		// assign polling server highest priority as we can't guarantee ties will be
		// broken on favour of the server
		PollingServer ps = new PollingServer(maxPriority, new RelativeTime(50, 0), new RelativeTime(20, 0), q);
		ps.start();

		beginEventGeneration(q);
	}

	private static void beginEventGeneration(AperiodicEventPriorityQueue<RunnableAperiodicEvent> q) {
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
			try {
				Thread.sleep(nextDelay);
			} catch (InterruptedException e) {
			}
		}
	}
}
