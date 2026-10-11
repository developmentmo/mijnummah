package nl.mijnummah.common.mail;

import java.util.Objects;

/** A plain-text email to one recipient. */
public record MailMessage(String to, String subject, String text) {

    public MailMessage {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(text, "text");
    }

    @Override
    public String toString() {
        // the text can contain one-time codes; never log it through toString
        return "MailMessage[subject=" + subject + "]";
    }
}
