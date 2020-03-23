package rtsj.sandbox.aperiodic_service.polling_server;

import javax.realtime.AsyncEventHandler;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.ReleaseParameters;
import javax.realtime.SchedulingParameters;
import javax.realtime.memory.LTMemory;
import javax.realtime.memory.ScopedMemory;

/**
 * Instances are main (i.e. non-server) tasks.
 * 
 * @author savvas
 *
 */
public class PeriodicTask extends RealtimeThread {

	private final ScopedMemory mem = new LTMemory(2048);
	private final RelativeTime cost;

	/**
	 * REQUIRES:
	 * 
	 * 1) cost > 0
	 * 
	 * @param period
	 * @param priority
	 * @param cost
	 */
	public PeriodicTask(int priority, int period, RelativeTime cost, String name) {
		assert cost.compareToZero() > 0 : "Cost must not be negative";

		this.cost = (RelativeTime) cost.clone();
		SchedulingParameters pri = new PriorityParameters(priority);
		setSchedulingParameters(pri);
		RelativeTime _period = new RelativeTime(period, 0);
		ReleaseParameters rel = new PeriodicParameters(_period);
		rel.setDeadlineMissHandler(new MissHandler());
		setReleaseParameters(rel);

		setName(name);
	}

	@Override
	public void run() {
		while (true) {
			mem.enter(new Runnable() {
				@Override
				public void run() {
					TimeUtils.spinWait(cost);
				}
			});
			waitForNextPeriod();
		}
	}

	private class MissHandler extends AsyncEventHandler {

		@Override
		public void handleAsyncEvent() {
			System.out.println("Deadline missed for thread #" + getName());
		}
	}
}
