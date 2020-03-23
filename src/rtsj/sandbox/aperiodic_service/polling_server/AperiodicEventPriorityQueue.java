package rtsj.sandbox.aperiodic_service.polling_server;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Provides access to aperiodic events returned in an order defined by the given
 * comparator or the elements' natural ordering. Initial capacity may be set but
 * the total capacity is unbounded.
 * 
 * NOTE: If instantiated with no Comparator, pushed elements must implement
 * Comparable.
 * 
 * Thread-safe
 * 
 * @author savvas
 *
 */
public class AperiodicEventPriorityQueue<T extends RunnableAperiodicEvent> {

	private static final int DEFAULT_QUEUE_SIZE = 10;

	private final Queue<T> q;

	private AperiodicEventAdmissionControl<T> admissionControl;

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
	public AperiodicEventPriorityQueue(Comparator<T> comparator) {
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
	public AperiodicEventPriorityQueue(int initialSize, Comparator<T> comparator) {
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

	public AperiodicEventPriorityQueue(int initialSize, Comparator<T> comparator,
			AperiodicEventAdmissionControl<T> aperiodicEventAdmissionControl) {
		this(initialSize, comparator);
		assert aperiodicEventAdmissionControl != null : "aperiodicEventAdmissionControl must not be null";
		this.admissionControl = aperiodicEventAdmissionControl;
	}

	/**
	 * Add new event to queue.
	 * 
	 * Never blocks as queue is unbounded.
	 * 
	 * @param event
	 */
	public void push(T event) {
		if (admissionControl != null) {
			if (admissionControl.canAccept(event)) {
				q.offer(event);
			}
		} else {
			q.offer(event);
		}
	}

	/**
	 * Return and remove first element from queue.
	 * 
	 * Returns null if empty.
	 * 
	 * @return
	 */
	public T pop() {
		return q.poll();
	}

	/**
	 * How many events are there in the queue "now".
	 * 
	 * @return
	 */
	public int size() {
		return q.size();
	}
}
