package rtsj.sandbox.aperiodic_service.common;

public interface EventQueue<E> {

	/**
	 * Add new event to queue.
	 * 
	 * Does not block.
	 * 
	 * @param event
	 */
	void push(E event);

	/**
	 * Remove and return first event in queue.
	 * 
	 * Returns null if empty. Does not block.
	 * 
	 * @return
	 */
	E pop();

	/**
	 * Are there no events in the queue?
	 * 
	 * @return
	 */
	boolean isEmpty();
}
