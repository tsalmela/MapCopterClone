/**
 * MapCopter currently uses the publish/subscribe style
 * <a href="http://square.github.io/otto/">Otto Event Bus</a> for asynchronous communication
 * from CopterManager to the activity. This package contains the event classes. See the
 * <a href="http://square.github.io/otto/">web page</a> for usage instructions and documentation.
 *
 * Use the {@link fi.oulu.mapcopter.MapCopterApplication#getDefaultBus()} to get access
 * to the main event bus.
 *
 */
package fi.oulu.mapcopter.event;