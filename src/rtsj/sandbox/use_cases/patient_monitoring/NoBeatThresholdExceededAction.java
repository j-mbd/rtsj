package rtsj.sandbox.use_cases.patient_monitoring;

import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.realtime.OneShotTimer;
import javax.realtime.PriorityParameters;
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
 * AVAILABLE.
 *
 * 
 */
public class NoBeatThresholdExceededAction extends AsyncEventHandler {

	private final AsyncEvent noBeatEvent;
	private final OneShotTimer heartBeatWatchdog;
	private final RelativeTime maxNoBeatInterval;

	public NoBeatThresholdExceededAction(int priority, OneShotTimer heartBeatWatchdog, RelativeTime maxNoBeatInterval,
			AsyncEvent noBeatEvent) {
		setSchedulingParameters(new PriorityParameters(priority));
		this.noBeatEvent = noBeatEvent;
		this.heartBeatWatchdog = heartBeatWatchdog;
		this.maxNoBeatInterval = new RelativeTime(maxNoBeatInterval);

	}

	/**
	 * No heartbeat received within the maximum threshold.
	 */
	@Override
	public void handleAsyncEvent() {
		// release actions
		noBeatEvent.fire();
		// re-configure watchdog for next beat duration
		heartBeatWatchdog.reschedule(maxNoBeatInterval);
		// watchdog must be made active and enabled again since it has fired now
		heartBeatWatchdog.start();
	}
}
