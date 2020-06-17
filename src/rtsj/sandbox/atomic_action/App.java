package rtsj.sandbox.atomic_action;

import java.util.Arrays;

import javax.realtime.AsynchronouslyInterruptedException;
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
 * A sample simulation of three threads participating in an atomic-action. When
 * testing, care must be taken to ensure all scenarios reflecting all possible
 * thread histories are covered. Consider the following matrix for three
 * threads, for example, where "0" means task completed successfully and "1" the
 * task failed. In addition to this, all potential finish outcomes need to be
 * taken into account (i.e. t2 finishes first, then t1 then t3 and so on). So,
 * for each row the following permutation of finish histories needs to be tested
 * (e.g. "t2t1t3" means t2 finishes first then t1 and then t3):
 * 
 * (t1t2t3, t1t3t2, t2t1t3, t2t3t1, t3t1t2, t3t2t1)
 * 
 * Note, some tests may be redundant but having them won't hurt. :-)
 * 
 * ----------------------------------------------------------------------------
 * t1t2t3
 *
 * -----
 * 
 * 0 0 0
 * 
 * 0 0 1
 * 
 * 0 1 0
 * 
 * 0 1 1
 * 
 * 1 0 0
 * 
 * 1 0 1
 * 
 * 1 1 0
 * 
 * 1 1 1
 *
 */
public class App {

	public static void main(String... args) {

		AsynchronouslyInterruptedException t1Aie = new AsynchronouslyInterruptedException();
		AsynchronouslyInterruptedException t2Aie = new AsynchronouslyInterruptedException();
		AsynchronouslyInterruptedException t3Aie = new AsynchronouslyInterruptedException();

		AtomicActionControl control = new AIEAtomicActionControl(Arrays.asList(t1Aie, t2Aie, t3Aie));

		Thread t1 = new AtomicActionTask(control, t1Aie, new RelativeTime(2, 0), "t1", false);
		Thread t2 = new AtomicActionTask(control, t1Aie, new RelativeTime(4, 0), "t2", false);
		Thread t3 = new AtomicActionTask(control, t1Aie, new RelativeTime(6, 0), "t3", false);

		t1.start();
		t2.start();
		t3.start();
	}
}
