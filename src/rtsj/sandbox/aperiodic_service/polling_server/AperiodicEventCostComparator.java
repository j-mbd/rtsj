package rtsj.sandbox.aperiodic_service.polling_server;

import java.util.Comparator;

/**
 * Sorts aperiodic events lowest cost to highest.
 * 
 * Thread-safe
 * 
 * @author savvas
 *
 */
public class AperiodicEventCostComparator<T extends RunnableAperiodicEvent> implements Comparator<T> {

	@Override
	@SuppressWarnings("unchecked")
	public int compare(RunnableAperiodicEvent o1, RunnableAperiodicEvent o2) {
		return o1.cost().compareTo(o2.cost());
	}
}
