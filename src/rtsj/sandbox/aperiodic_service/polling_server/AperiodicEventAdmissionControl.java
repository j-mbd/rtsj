package rtsj.sandbox.aperiodic_service.polling_server;

/**
 * Strategy for event admission. Can be cost based if events are not restartable
 * or deadline based if they are.
 * 
 * @author savvas
 *
 * @param <T>
 */

@FunctionalInterface
public interface AperiodicEventAdmissionControl<T extends RunnableAperiodicEvent> {

	boolean canAccept(T event);
}
