package nl.mijnummah.event.domain;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** A request that is well-formed but breaks a rule of the agenda (422), e.g. publishing a cancelled event. */
public class EventRuleViolationException extends ApplicationException {

    public EventRuleViolationException(String code, String message) {
        super(ErrorCategory.BUSINESS_RULE_VIOLATION, code, message);
    }
}
