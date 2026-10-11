package nl.mijnummah.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailProperties.class)
class MailConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MailConfiguration.class);

    @Bean
    Mailer mailer(MailProperties properties, ObjectProvider<JavaMailSender> javaMailSender) {
        return switch (properties.provider()) {
            case NONE -> new UnavailableMailer();
            case LOG -> {
                log.warn("MAIL_PROVIDER=log: emails are written to the log and NOT sent. Local development only.");
                yield new LoggingMailer();
            }
            case SMTP -> {
                JavaMailSender sender = javaMailSender.getIfAvailable();
                if (sender == null) {
                    throw new IllegalStateException(
                            "MAIL_PROVIDER=smtp needs an SMTP server: set spring.mail.host (SPRING_MAIL_HOST)");
                }
                yield new SmtpMailer(sender, properties.from());
            }
        };
    }

    static final class UnavailableMailer implements Mailer {

        @Override
        public boolean available() {
            return false;
        }

        @Override
        public void send(MailMessage message) {
            throw new IllegalStateException("Email is not configured (MAIL_PROVIDER=none)");
        }
    }

    static final class LoggingMailer implements Mailer {

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void send(MailMessage message) {
            log.info("[MAIL_PROVIDER=log, not sent] To: {}\nSubject: {}\n\n{}", message.to(), message.subject(),
                    message.text());
        }
    }

    static final class SmtpMailer implements Mailer {

        private final JavaMailSender sender;
        private final String from;

        SmtpMailer(JavaMailSender sender, String from) {
            this.sender = sender;
            this.from = from;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void send(MailMessage message) {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(message.to());
            mail.setSubject(message.subject());
            mail.setText(message.text());
            try {
                sender.send(mail);
            } catch (MailException ex) {
                throw new MailDeliveryException("SMTP server did not accept the message", ex);
            }
        }
    }
}
