package org.quangdung.infrastructure.component.mail;

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

    public Uni<Void> sendAlertMail(String subject, String body){
        log.info("Sending mail...");
        Mail mail = new Mail()
            .addTo("sangvo0507@gmail.com")
            // .addTo("pjoffre@lanestel.fr") 
            .setSubject(subject)
            .setText(body);
        return mailer.send(mail).onItem().invoke(()->{
            log.info("Mail sent successfully!");
        })
        .onFailure().invoke(throwable -> log.error(throwable));
    }

}
