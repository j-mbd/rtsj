package rtsj.sandbox.aperiodic_service.common;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

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
 * Provides access to aperiodic events returned in an order defined by the given
 * comparator or the elements' natural ordering. Initial capacity may be set but
 * the total capacity is unbounded.
 * 
 * NOTE: If instantiated with no Comparator, pushed elements must implement
 * Comparable.
 * 
 * Thread-safe
 *
 */
public class AperiodicEventPriorityQueue<E extends InterruptibleAperiodicEvent> implements EventQueue<E> {

	private static final int DEFAULT_QUEUE_SIZE = 10;

	private final Queue<E> q;

	private AperiodicEventAdmissionControl<E> admissionControl;

	public AperiodicEventPriorityQueue() {
		this(DEFAULT_QUEUE_SIZE);
	}

	/**
	 * REQUIRES:
	 * 
	 * 1) initialSize >= 0
	 * 
	 * @param initialSize
	 */
	public AperiodicEventPriorityQueue(int initialSize) {
		assert initialSize >= 0 : "initialSize" + initialSize + " must not be negative";
		q = new PriorityBlockingQueue<>(initialSize);
		admissionControl = null;
	}

	/**
	 * REQUIRES:
	 * 
	 * 1) comparator != null
	 * 
	 * @param comparator
	 */
	public AperiodicEventPriorityQueue(Comparator<E> comparator) {
		this(DEFAULT_QUEUE_SIZE, comparator);
	}

	/**
	 * REQUIRES:
	 * 
	 * 1) initialSize >= 0
	 * 
	 * 2) comparator != null
	 * 
	 * @param initialSize
	 * @param comparator
	 */
	public AperiodicEventPriorityQueue(int initialSize, Comparator<E> comparator) {
		assert initialSize >= 0 : "initialSize" + initialSize + " must not be negative";
		assert comparator != null : "comparator must not be null";
		q = new PriorityBlockingQueue<>(initialSize, comparator);
		this.admissionControl = null;
	}

	/**
	 * REQUIRES:
	 * 
	 * 1) initialSize >= 0
	 * 
	 * 2) comparator != null
	 * 
	 * 3) aperiodicEventAdmissionControl != null
	 * 
	 * @param initialSize
	 * @param comparator
	 * @param aperiodicEventAdmissionControl
	 */

	public AperiodicEventPriorityQueue(int initialSize, Comparator<E> comparator,
			AperiodicEventAdmissionControl<E> aperiodicEventAdmissionControl) {
		this(initialSize, comparator);
		assert aperiodicEventAdmissionControl != null : "aperiodicEventAdmissionControl must not be null";
		this.admissionControl = aperiodicEventAdmissionControl;
	}

	@Override
	public void push(E event) {
		if (admissionControl != null) {
			if (admissionControl.canAccept(event)) {
				q.offer(event);
			}
		} else {
			q.offer(event);
		}
	}

	@Override
	public E pop() {
		return q.poll();
	}

	@Override
	public boolean isEmpty() {
		return q.isEmpty();
	}
}
