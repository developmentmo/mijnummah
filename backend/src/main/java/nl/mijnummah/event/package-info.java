/**
 * Agenda: events of a mosque (lectures, Friday programme, classes, ...) and registrations for them.
 * <p>
 * Every event belongs to exactly one mosque. Admin operations look events up by (id, mosque) after
 * {@code MosqueAccessService} approved the caller for that mosque, so an ID of another mosque's event is simply
 * "not found". Public API for other modules: {@link EventDirectory}.
 */
@ApplicationModule(displayName = "Event")
package nl.mijnummah.event;

import org.springframework.modulith.ApplicationModule;
