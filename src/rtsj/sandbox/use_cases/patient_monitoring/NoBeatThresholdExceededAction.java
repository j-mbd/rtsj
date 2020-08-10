package rtsj.sandbox.use_cases.patient_monitoring;

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
 * FREELY (OR EVEN NOT FREELY) AVAILABLE.
 *
 */
public class NoBeatThresholdExceededAction extends AsyncEventHandler {

	private final OneShotTimer heartBeatWatchdog;
	private final RelativeTime maxNoBeatInterval;
	private final Object alarmNotificationMonitor;
	private final Object voltageApplicationMonitor;

	public NoBeatThresholdExceededAction(int priority, OneShotTimer heartBeatWatchdog, RelativeTime maxNoBeatInterval,
			Object alarmNotificationMonitor, Object voltageApplicationMonitor) {
		setSchedulingParameters(new PriorityParameters(priority));
		this.heartBeatWatchdog = heartBeatWatchdog;
		this.maxNoBeatInterval = new RelativeTime(maxNoBeatInterval);
		this.alarmNotificationMonitor = alarmNotificationMonitor;
		this.voltageApplicationMonitor = voltageApplicationMonitor;
	}

	@Override
	public void handleAsyncEvent() {
		wakeUpWaiter(alarmNotificationMonitor);
		wakeUpWaiter(voltageApplicationMonitor);
		heartBeatWatchdog.reschedule(maxNoBeatInterval);
		heartBeatWatchdog.start();
	}

	private void wakeUpWaiter(Object waitMonitor) {
		synchronized (waitMonitor) {
			waitMonitor.notify();
		}
	}
}
