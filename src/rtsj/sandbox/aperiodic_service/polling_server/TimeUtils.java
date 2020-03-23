package rtsj.sandbox.aperiodic_service.polling_server;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.RelativeTime;

public class TimeUtils {

	@SuppressWarnings("unchecked")
	public static void spinWait(RelativeTime time) {
		AbsoluteTime now = Clock.getRealtimeClock().getTime();
		AbsoluteTime waitUntil = now.add(time);
		while (now.compareTo(waitUntil) < 0) {
			Clock.getRealtimeClock().getTime(now);
		}
	}
}