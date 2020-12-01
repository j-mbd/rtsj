package rtsj.sandbox.use_cases.patient_monitoring;

import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;

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
public class VoltageApplicationAction extends RealtimeThread {

	private final VoltageControl voltageControl;
	private final MemoryArea memoryArea;
	private final Object waitMonitor;

	public VoltageApplicationAction(int priority, VoltageControl voltageControl, MemoryArea memoryArea,
			Object waitMonitor) {
		setSchedulingParameters(new PriorityParameters(priority));
		this.voltageControl = voltageControl;
		this.memoryArea = memoryArea;
		this.waitMonitor = waitMonitor;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (waitMonitor) {
				try {
					waitMonitor.wait();
				} catch (InterruptedException ie) {
					throw new RuntimeException(ie);
				}
			}
			memoryArea.enter(() -> {
				boolean shockApplied = voltageControl.increaseVoltageAndApplyShock();
				if (!shockApplied) {
					System.out.println("***WARNING: APPLYING MAX VOLTAGE***");
				}
			});
		}
	}
}
