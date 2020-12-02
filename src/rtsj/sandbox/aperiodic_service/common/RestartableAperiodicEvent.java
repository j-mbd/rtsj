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
 * AVAILABLE.
 * 
 * 
 * Keeps track of how much of its cost has been executed and how much is
 * remaining so that if processing is asynchronously interrupted, the remaining
 * processing cost is calculated and applied on the next run.
 * 
 * NOTE: Some protected methods could be be private and not overridable. If no
 * further subclasses are anticipated then this is probably the best strategy.
 * (i.e. a trade-off between runtime cost and extensibility)
 *
 */
public class RestartableAperiodicEvent extends InterruptibleAperiodicEvent
		implements Comparable<RestartableAperiodicEvent> {

	// These values may be modified from within scoped memory (through the set*()
	// methods), hence it's a good idea if all assignments are made in constructor
	// and are final
	private final RelativeTime remainingCost;
	private final AbsoluteTime processingStart;
	private final AbsoluteTime processingInterruptedEnd;
	private final RelativeTime totalCostToInterruprion;

	private final Clock clk;

	public RestartableAperiodicEvent(RelativeTime totalCost, RelativeTime deadline, String name) {
		super(totalCost, deadline, name);
		remainingCost = new RelativeTime(totalCost);
		processingStart = new AbsoluteTime();
		processingInterruptedEnd = new AbsoluteTime();
		totalCostToInterruprion = new RelativeTime();
		clk = Clock.getRealtimeClock();
	}

	@Override
	public void interruptAction(AsynchronouslyInterruptedException exception) {
		if (exception.clear()) {
			targetAieCaught();
		} else if (AsynchronouslyInterruptedException.getGeneric().clear()) {
			genericAieCaught();
		} else {
			throw new RuntimeException(
					"RestartableAperiodicEvent: A \"hand-thrown\" AsynchronouslyInterruptedException was raised",
					exception);
		}
	}

	@Override
	public void run(AsynchronouslyInterruptedException exception) throws AsynchronouslyInterruptedException {
		clk.getTime(processingStart);
		System.out.println("Processing event " + name + " at " + clk.getTime());
		runLogic();
		wasInterrupted = false;
		System.out.println("Processing of event " + name + " completed successfully at " + clk.getTime());
	}

	/**
	 * The actual work to be done.
	 * 
	 */
	protected void runLogic() {
		TimeUtils.spinWait(remainingCost);
	}

	/**
	 * Timed timer timed-out.
	 * 
	 * @param exception
	 */
	protected void targetAieCaught() {
		wasInterrupted = true;
		handleAie();
	}

	/**
	 * The thread running the logic inside this AsynchronouslyInterruptedException
	 * was externally interrupted by a call to interrupt().
	 * 
	 * @param exception
	 */
	protected void genericAieCaught() {
		wasGenericInterrupted = true;
		handleAie();
	}

	private void handleAie() {
		System.out.println("Processing event " + name + " interrupted at " + clk.getTime());
		clk.getTime(processingInterruptedEnd);
		recalculateRemainingCost();
		amendRestartabilityStatus();
		if (deadlineMissed()) {
			handleDeadlineMiss();
		}
	}

	// both the following two methods are only called from one place only so
	// inlining shouldn't increase size

	private void recalculateRemainingCost() {
		processingInterruptedEnd.subtract(processingStart, totalCostToInterruprion);
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
