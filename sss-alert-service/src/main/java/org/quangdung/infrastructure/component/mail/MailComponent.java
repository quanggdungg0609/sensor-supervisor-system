package org.quangdung.infrastructure.component.mail;

import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailComponent {
    @Inject
    private ReactiveMailer mailer;

    @Inject
    private Logger log;

    public Uni<Void> sendAlertMail(String subject, String body, Set<String> emails){
        log.info("Sending mail...");
        Mail mail = new Mail()
            .setSubject(subject)
            .setText(body);
        for(String email : emails){
            mail.addTo(email);
        }

        return mailer.send(mail).onItem().invoke(()->{
            log.info("Mail sent successfully!");
        })
        .onFailure().invoke(throwable -> log.error(throwable));
    }

}
