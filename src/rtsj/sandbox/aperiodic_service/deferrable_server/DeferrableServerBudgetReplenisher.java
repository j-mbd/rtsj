package rtsj.sandbox.aperiodic_service.deferrable_server;

import javax.realtime.AsyncEvent;
import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;

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
 * Together with DeferrableServerEventHandler forms a logical implementation of
 * the Deferrable-Server algorithm for aperiodic service.
 * 
 * Responsible for toping-up the server's (i.e. handler's) budget when each
 * period of the server falls-through.
 * 
 * It's priority must be higher than that given to the handler in order to
 * guarantee a preemption at replenish time.
 * 
 */
public class DeferrableServerBudgetReplenisher extends RealtimeThread {

	private final DeferrableServerEventHandler deferrableServerEventHandler;
	private final AsyncEvent event;
	private final MemoryArea mem;

	public DeferrableServerBudgetReplenisher(RelativeTime period, int priority,
			DeferrableServerEventHandler deferrableServerEventHandler, AsyncEvent event, MemoryArea memoryArea) {
		super(new PriorityParameters(priority), new PeriodicParameters(period), null, null, null, null);
		this.deferrableServerEventHandler = deferrableServerEventHandler;
		this.event = event;
		mem = memoryArea;
	}

	@Override
	public void run() {
		while (true) {
			mem.enter(() -> {
				deferrableServerEventHandler.replenishBudget();
				// process any events that may be waiting for service due to insufficient budget
				event.fire();
			});
			waitForNextPeriod();
		}
	}
}
