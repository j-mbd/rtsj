package rtsj.sandbox.use_cases.patient_monitoring;

import javax.realtime.device.RawInt;
import javax.realtime.device.RawMemoryFactory;

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
 * INVARIANT:
 * 
 * getCurrentVoltage() E [0,getMaxVoltageThreshold()]
 * 
 * NOTE: All registers are assumed to be memory-mapped.
 * 
 * NOTE: The invariant is not part of requirements but was added for extra
 * safety.
 * 
 */

public class VoltageControl {

	private final RawInt register;
	private final int voltageIncreaseStep;
	private final int maxVoltageThreshold;

	private int currentVoltage;

	public VoltageControl(long registerAddress, int count, int stride, int voltageIncreaseStep,
			int maxVoltageThreashold) {
		register = RawMemoryFactory.getDefaultFactory().createRawInt(RawMemoryFactory.MEMORY_MAPPED_REGION,
				registerAddress, count, stride);
		this.voltageIncreaseStep = voltageIncreaseStep;
		this.maxVoltageThreshold = maxVoltageThreashold;
	}

	/**
	 * @return true if voltage was increased and shock was applied, false if shock
	 *         was applied but voltage wasn't increased because the max voltage
	 *         threshold has been reached.
	 */
	public synchronized boolean increaseVoltageAndApplyShock() {
		if (voltageIncrementLegal()) {
			increaseVoltage();
			applyShock();
			return true;
		}
		applyShock();
		return false;
	}

	// Are we allowed to increase the voltage with a step increment?
	private boolean voltageIncrementLegal() {
		return (currentVoltage + voltageIncreaseStep) <= maxVoltageThreshold;
	}

	/**
	 * REQUIRES:
	 * 
	 * voltageIncrementLegal() == true
	 * 
	 * ENSURES:
	 * 
	 * currentVoltage = currentVoltage + voltageIncreaseStep;
	 * 
	 * IMPORTANT: Any precondition tests MUST be done within a synchronised block to
	 * avoid stale assessments.
	 * 
	 */
	private void increaseVoltage() {
		assert voltageIncrementLegal() == false : "Cannot increase voltage, max threshold [" + maxVoltageThreshold
				+ "] would be violated";
		currentVoltage += voltageIncreaseStep;
		assert currentVoltage >= 0 && currentVoltage <= maxVoltageThreshold : "currentVoltage [" + currentVoltage
				+ "] not within [0," + maxVoltageThreshold + "] limits";
	}

	private void applyShock() {
		// offset "0"
		register.setInt(currentVoltage);
	}

	public synchronized void resetVoltage() {
		currentVoltage = 0;
		assert currentVoltage >= 0 && currentVoltage <= maxVoltageThreshold : "currentVoltage [" + currentVoltage
				+ "] not within [0," + maxVoltageThreshold + "] limits";
	}

	public synchronized int getMaxVoltageThreshold() {
		return maxVoltageThreshold;
	}

	public synchronized int getCurrentVoltage() {
		return currentVoltage;
	}
}
