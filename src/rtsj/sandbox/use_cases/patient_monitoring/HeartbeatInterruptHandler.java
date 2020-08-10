package rtsj.sandbox.use_cases.patient_monitoring;

import javax.realtime.AsyncEventHandler;
import javax.realtime.MemoryArea;
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
public class HeartbeatInterruptHandler extends AsyncEventHandler {

	private final RelativeTime maxNoBeatInterval;
	private final OneShotTimer oneShotTimer;
	private final VoltageControl voltageControl;

	public HeartbeatInterruptHandler(int priority, RelativeTime maxNoBeatInterval, OneShotTimer oneShotTimer,
			VoltageControl voltageControl, MemoryArea memoryArea) {
		super(new PriorityParameters(priority), null, null, memoryArea, null, false);
		this.maxNoBeatInterval = new RelativeTime(maxNoBeatInterval);
		this.oneShotTimer = oneShotTimer;
		this.voltageControl = voltageControl;
	}

	@Override
	public void handleAsyncEvent() {
		oneShotTimer.reschedule(maxNoBeatInterval);
		voltageControl.resetVoltage();
	}
}
