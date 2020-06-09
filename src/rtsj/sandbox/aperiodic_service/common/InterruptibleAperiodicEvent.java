package rtsj.sandbox.aperiodic_service.common;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.Interruptible;
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
 * RunnableAperiodicEvents implement Interruptible so they can be used as the
 * target of AsynchronouslyInterruptedException* classes where the "run" method
 * specifies main processing and the "interruptAction" the logic to apply when
 * the handler's budget is about to be exceeded and processing hasn't finished
 * yet.
 * 
 * Not thread-safe.
 * 
 * @author savvas
 *
 */

public abstract class InterruptibleAperiodicEvent implements Interruptible {

	protected final Clock clk;
	protected final RelativeTime totalCost;
	protected final RelativeTime deadline;
	protected final AbsoluteTime absoluteDeadline;
	protected final AbsoluteTime creationTime;
	protected final String name;

	protected boolean wasInterrupted;
	protected boolean canRestart;
	protected RelativeTime remainingCost;

	protected InterruptibleAperiodicEvent(RelativeTime cost, RelativeTime deadline, String name) {
		clk = Clock.getRealtimeClock();
		this.totalCost = cost;
		this.deadline = deadline;
		// creation time and absolute deadline may not necessarily be the same as
		// "physical" event time
		creationTime = clk.getTime();
		absoluteDeadline = creationTime.add(deadline);
		this.name = name;
		wasInterrupted = false;
		canRestart = true;
		remainingCost = new RelativeTime(totalCost);

	}

	// ****************************STATUS REPORT****************************
	/**
	 * What is the total cost of this event?
	 * 
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public RelativeTime cost() {
		return new RelativeTime(totalCost);
	}

	/**
	 * What is the remaining cost of this event after some processing?
	 * 
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public RelativeTime remainingCost() {
		return new RelativeTime(remainingCost);
	}

	/**
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public RelativeTime relativeDeadline() {
		return new RelativeTime(deadline);
	}

	/**
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public AbsoluteTime absoluteDeadline() {
		return new AbsoluteTime(absoluteDeadline);
	}

	/**
	 * Not necessarily same as "arrival time".
	 * 
	 * NOTE: Each invocation creates a new instance. Override and simply return the
	 * current instance if this method is to be called too often.
	 * 
	 * @return
	 */
	public AbsoluteTime creationTime() {
		return new AbsoluteTime(creationTime);
	}

	public boolean wasInterrupted() {
		return wasInterrupted;
	}

	/**
	 * Can processing for this event restart after being interrupted?
	 * 
	 * "true" implies (isRestartable() == true)
	 * 
	 * @return
	 */
	public boolean canRestart() {
		return canRestart;
	}

	/**
	 * Is this event of type "restartable"?
	 * 
	 * @return
	 */
	public boolean isTypeRestartable() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean deadlineMissed() {
		return clk.getTime().compareTo(absoluteDeadline) > 0;
	}

	// ****************************STATE CHANGE****************************

	public void reset() {
		wasInterrupted = false;
		canRestart = true;
	}
}
