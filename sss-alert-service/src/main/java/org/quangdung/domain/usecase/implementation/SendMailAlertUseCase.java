package org.quangdung.domain.usecase.implementation;

import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import org.quangdung.infrastructure.component.mail.MailComponent;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SendMailAlertUseCase implements ISendMailAlertUseCase {
    @Inject
    private MailComponent mailComponent;


    @Override
    public Uni<Void> execute(String level, String message) {
        String subtitle = switch (level) {
            case "warn" -> "[Warning] Alert Message";
            case "crit" -> "[CRITICAL] Alert Message";
            default -> "[INFO] Alert Message"; 
        };
        return mailComponent.sendAlertMail(subtitle, message);
    }
    
}
