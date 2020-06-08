package rtsj.sandbox.atomic_action;

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
 * Coordinates the execution flow of threads in atomic-actions through an
 * activity protocol which supports the notion of abortable tasks.
 * 
 * Participants signal their presence by calling arrived(). When completed
 * normally they call done() otherwise they call abort().
 * 
 * Getting "false" from done() means that all threads have completed
 * successfully and typically no further action needs to be taken. Getting
 * "true" however means that one or more threads have aborted their runs in
 * which case receiving threads need to take some "abort" or "rollback" action
 * and signal the fact that they themselves have aborted their runs by calling
 * abort().
 * 
 * TODO: done() can be changed to return an int to allow for a richer set of
 * error conditions.
 * 
 * @author savvas
 *
 */
public interface AtomicActionControl {

	/**
	 * Signals a thread's intention to participate in an Atomic Action.
	 */
	void arrived() throws InterruptedException;

	/**
	 * Signals that a thread's action has completed successfully.
	 * 
	 * @return "false" if all threads completed successfully, "true" if one or more
	 *         threads aborted their runs
	 * @throws InterruptedException
	 */
	boolean done() throws InterruptedException;

	/**
	 * Signals that a thread's action has failed to complete.
	 */
	void abort() throws InterruptedException;
}
