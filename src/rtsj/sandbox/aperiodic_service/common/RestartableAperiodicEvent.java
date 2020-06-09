package rtsj.sandbox.aperiodic_service.common;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.Clock;
import javax.realtime.RelativeTime;

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
 * Keeps track of how much of its cost has been executed and how much is
 * remaining. This way, if processing is asynchronously interrupted, the
 * remaining processing cost is calculated and applied on the next run.
 * 
 * @author savvas
 *
 */
public class RestartableAperiodicEvent extends InterruptibleAperiodicEvent
		implements Comparable<RestartableAperiodicEvent> {

	// These values may be modified from within scoped memory (through the set*()
	// methods), hence it's a good idea if all assignments are made here and are
	// final
	private final RelativeTime remainingCost;
	private final AbsoluteTime processingStart;
	private final AbsoluteTime processingInterruptedEnd;

	private final Clock clk;

	public RestartableAperiodicEvent(RelativeTime totalCost, RelativeTime deadline, String name) {
		super(totalCost, deadline, name);
		remainingCost = new RelativeTime(totalCost);
		processingStart = new AbsoluteTime();
		processingInterruptedEnd = new AbsoluteTime();
		clk = Clock.getRealtimeClock();
	}

	@Override
	public void interruptAction(AsynchronouslyInterruptedException exception) {
		System.out.println("Processing event " + name + " interrupted at " + clk.getTime());
		wasInterrupted = true;
		clk.getTime(processingInterruptedEnd);
		recalculateRemainingCost();
		amendRestartabilityStatus();
		if (deadlineMissed()) {
			handleDeadlineMiss();
		}
	}

	@Override
	public void run(AsynchronouslyInterruptedException exception) throws AsynchronouslyInterruptedException {
		clk.getTime(processingStart);
		System.out.println("Processing event " + name + " at " + clk.getTime());
		TimeUtils.spinWait(remainingCost);
		wasInterrupted = false;
		System.out.println("Processing of event " + name + " complteted successfully at " + clk.getTime());
	}

	private void recalculateRemainingCost() {
		RelativeTime totalCostToInterruprion = processingInterruptedEnd.subtract(processingStart);
		remainingCost.subtract(totalCostToInterruprion, remainingCost);
	}

	private void amendRestartabilityStatus() {
		if (remainingCost.compareToZero() <= 0 || deadlineMissed()) {
			canRestart = false;
		} else {
			canRestart = true;
		}
	}

	private void handleDeadlineMiss() {
		// Could also create a separate class whose instance is wired and used here
		System.out.println("Deadline missed for event " + this);
	}

	@Override
	public String toString() {
		return "RestartableAperiodicEvent [absoluteDeadline=" + absoluteDeadline + ", cost=" + totalCost + ", deadline="
				+ deadline + ", creationTime=" + creationTime + ", name=" + name + "]";
	}

	// ****************************STATUS REPORT****************************

	@Override
	public boolean isTypeRestartable() {
		return true;
	}

	/**
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	@Override
	public RelativeTime remainingCost() {
		return new RelativeTime(remainingCost);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(RestartableAperiodicEvent other) {
		return this.remainingCost.compareTo(other.remainingCost);
	}
}
