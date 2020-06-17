package rtsj.sandbox.aperiodic_service.common;

import javax.realtime.AsyncEventHandler;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.ReleaseParameters;
import javax.realtime.SchedulingParameters;
import javax.realtime.memory.LTMemory;
import javax.realtime.memory.ScopedMemory;

import rtsj.sandbox.common.TimeUtils;

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
 * Instances are "regular" (i.e. non-server) tasks belonging to the periodic
 * task set.
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
					// emulate some task work
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
