package rtsj.sandbox.atomic_action;

import java.util.ArrayList;
import java.util.List;

import javax.realtime.AsynchronouslyInterruptedException;

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
 * An AtomicActionControl implementation based on
 * AsynchronouslyInterruptedException's.
 * 
 * Applies an all-come-all-leave scheme i.e. all expected threads need to arrive
 * and all need to leave. (may cause a "deserter" issue)
 *
 */
public class AIEAtomicActionControl implements AtomicActionControl {

	private final List<AsynchronouslyInterruptedException> aies;
	private final int threadCount;
	private int arrived;
	private int done;
	private int doneAborted;
	private boolean aborted;
	private boolean allowDoneComplete;
	private boolean allowAbortedComplete;

	/**
	 * REQUIRES:
	 * 
	 * 1) aies != null
	 * 
	 * 2) aies.size() > 1
	 * 
	 * @param aies The AIEs to fire when one thread aborts it's execution. List size
	 *             implies the desired number of threads.
	 */
	public AIEAtomicActionControl(List<AsynchronouslyInterruptedException> aies) {
		assert aies != null : "aies is null";
		assert aies.size() > 1 : "At least two participants are required for an atomic-action";
		this.aies = new ArrayList<>(aies);
		threadCount = this.aies.size();
	}

	@Override
	public synchronized void arrived() throws InterruptedException {
		while (arrived == threadCount) {
			wait();
		}
		++arrived;
	}

	@Override
	public synchronized boolean done() throws InterruptedException {
		++done;
		while (done != threadCount && !allowDoneComplete && !aborted) {
			wait();
		}
		--done;
		// some housekeeping for last thread
		if ((done == 0) && !aborted) {
			reset();
		} else if (!allowDoneComplete && !aborted) {
			allowDoneComplete = true;
			notifyAll();
		}
		return aborted;
	}

	@Override
	public synchronized void abort() throws InterruptedException {
		if (!aborted) {
			aborted = true;
			aies.stream().forEach(AsynchronouslyInterruptedException::fire);
			allowDoneComplete = true;
			// notify any threads that may have already finished successfully (i.e.
			// currently blocked on exit())
			notifyAll();
		}
		++doneAborted;
		while (doneAborted != threadCount && !allowAbortedComplete) {
			wait();
		}
		--doneAborted;
		// some housekeeping for last thread
		if (doneAborted == 0 && aborted) {
			reset();
		} else if (!allowAbortedComplete) {
			allowAbortedComplete = true;
			notifyAll();
		}
	}

	// should always be called whilst holding "this" lock
	private void reset() {
		arrived = done = doneAborted = 0;
		aborted = allowDoneComplete = allowAbortedComplete = false;
		// release all blocked on entry (new batch)
		notifyAll();
	}
}
