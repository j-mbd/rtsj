package rtsj.sandbox.atomic_action;

import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.Interruptible;
import javax.realtime.RealtimeThread;
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
 * An abortable task that can participate in an atomic-action.
 *
 */
public class AtomicActionTask extends RealtimeThread implements Interruptible {

	private final AtomicActionControl control;
	private final AsynchronouslyInterruptedException aie;
	private final RelativeTime cost;
	private final boolean abort;

	public AtomicActionTask(AtomicActionControl control, AsynchronouslyInterruptedException aie, RelativeTime cost,
			String name, boolean abort) {
		this.control = control;
		this.aie = aie;
		this.cost = cost;
		this.abort = abort;
		setName(name);
	}

	@Override
	public void run() {
		aie.doInterruptible(this);
	}

	@Override
	public void interruptAction(AsynchronouslyInterruptedException exception) {
		// Should always be true really since no other AIEs are fired during "this" run
		assert aie.clear() : "Thread:" + getName()
				+ ": AIE is not the current one. Maybe this action started an Interruptible of its own?";
		try {
			System.out.println("Thread:" + getName() + " interrupted");
			control.abort();
			System.out.println("Thread:" + getName() + " exit abort");
		} catch (Exception e) {
			// TODO: Communicate this somehow to atomic-action?
			// throw new RuntimeException(e);
		}
	}

	@Override
	public void run(AsynchronouslyInterruptedException exception) throws AsynchronouslyInterruptedException {
		try {
			if (abort) {
				control.abort();
				return;
			}
			System.out.println("Thread:" + getName() + " arrived");
			control.arrived();
			System.out.println("Thread:" + getName() + " running");
			sleep(cost);
			boolean actionFailed = control.done();
			if (actionFailed) {
				System.out.println("Thread:" + getName() + " - Action failed, ran recovery action");
				control.abort();
				System.out.println("Thread:" + getName() + " exit abort");
			} else {
				System.out.println("Thread:" + getName() + " exit normal");
			}
		} catch (InterruptedException e) {
			// Never reached
		}
	}
}
