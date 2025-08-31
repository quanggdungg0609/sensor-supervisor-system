package org.quangdung.domain.usecase.interfaces;

import java.util.Set;

import io.smallrye.mutiny.Uni;

/**
 * Interface for retrieving all alert email addresses
 */
public interface IGetAlertEmailsUseCase {
    /**
     * Retrieves all email addresses registered for alerts
     * 
     * @return Uni<Set<String>> containing all registered email addresses
     */
    Uni<Set<String>> execute();
}