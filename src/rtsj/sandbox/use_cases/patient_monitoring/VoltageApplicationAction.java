package rtsj.sandbox.use_cases.patient_monitoring;

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;

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
public class VoltageApplicationAction extends BoundAsyncEventHandler {

	private final VoltageControl voltageControl;
	private final MemoryArea memoryArea;

	public VoltageApplicationAction(int priority, VoltageControl voltageControl, MemoryArea memoryArea) {
		setSchedulingParameters(new PriorityParameters(priority));
		this.voltageControl = voltageControl;
		this.memoryArea = memoryArea;
	}

	@Override
	public void handleAsyncEvent() {
		memoryArea.enter(() -> {
			boolean shockApplied = voltageControl.increaseVoltageAndApplyShock();
			if (!shockApplied) {
				System.out.println("***WARNING: APPLYING MAX VOLTAGE***");
			}
		});
	}
}
