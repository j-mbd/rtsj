package rtsj.sandbox.use_cases.patient_monitoring;

import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.realtime.MemoryArea;
import javax.realtime.OneShotTimer;
import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;
import javax.realtime.memory.LTMemory;

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
 * PATIENT MONITORING SYSTEM - REQUIREMENTS:
 * 
 * The system is arranged so that an interrupt is generated every time the
 * patient's heart beats. In addition, a mild electric shock can be administered
 * via a device control register, the address of which is 177760 (octal). The
 * register is set up so that every time an integer value x is assigned to it
 * the patient receives x volts over a a small period of time.
 * 
 * If no heartbeat is recorded within a 5 second period the patient's life is in
 * danger. Two actions should be taken when the patient's heart fails: the first
 * is that a 'supervisor' task should be notified so that it may sound the
 * hospital alarm and the second is that a single electric shock of 5 volts
 * should be administered. If the patient fails to respond, the voltage should
 * be increased by 1 volt for every further 5 seconds.
 *
 */
public class App {

	private static final long MAX_NO_BEAT_MILLIS = 5_000;
	private static final String HEARTBEAT_INTERRUPT = "HEARTBEAT_INTERRUPT";

	public static void main(String... args) {
		AsyncEvent heartBeatEvent = new AsyncEvent();
		heartBeatEvent.bindTo(HEARTBEAT_INTERRUPT);

		RelativeTime maxNoBeatInterval = new RelativeTime(MAX_NO_BEAT_MILLIS, 0);

		final int maxPriority = PriorityScheduler.instance().getMaxPriority();
		final int interruptHandlerPriority = maxPriority;
		final int noBeatActionPriority = maxPriority - 1;
		final int alarmActionPriority = maxPriority - 2;
		final int voltageActionPriority = maxPriority - 3;

		VoltageControl voltageControl = new VoltageControl(0177760, 0, 0, 1, 20);

		MemoryArea alarmActionMemoryArea = new LTMemory(2048);
		Object alarmNotificationMonitor = new Object();

		MemoryArea voltageActionMemoryArea = new LTMemory(2048);
		Object voltageApplicationMonitor = new Object();

		RelativeTime maxNoBeatIntervalTime = new RelativeTime(maxNoBeatInterval);

		// *** CREATE AND START ALL COMPONENTS ***
		OneShotTimer heartBeatWatchdog = new OneShotTimer(maxNoBeatIntervalTime, null); // handler attached a bit later
		NoBeatThresholdExceededAction noBeatThresholdExceededAction = new NoBeatThresholdExceededAction(
				noBeatActionPriority, heartBeatWatchdog, maxNoBeatIntervalTime, alarmNotificationMonitor,
				voltageApplicationMonitor);
		heartBeatWatchdog.setHandler(noBeatThresholdExceededAction);

		MemoryArea interruptHandlerMemoryArea = new LTMemory(2048);
		AsyncEventHandler heartbeatInterruptHandler = new HeartbeatInterruptHandler(interruptHandlerPriority,
				maxNoBeatIntervalTime, heartBeatWatchdog, voltageControl, interruptHandlerMemoryArea);
		heartBeatEvent.addHandler(heartbeatInterruptHandler);

		heartBeatWatchdog.start();
		new AlarmNotificationAction(alarmActionPriority, alarmActionMemoryArea, alarmNotificationMonitor).start();
		new VoltageApplicationAction(voltageActionPriority, voltageControl, voltageActionMemoryArea,
				voltageApplicationMonitor).start();
	}
}
