package org.quangdung.domain.usecase.interfaces;

import io.smallrye.mutiny.Uni;

/**
 * Interface for adding an email address to the alert notification list
 */
public interface IAddAlertEmailUseCase {
    /**
     * Adds an email address to the alert notification list
     * 
     * @param email The email address to add
     * @return Uni<Boolean> indicating success or failure
     */
    Uni<Boolean> execute(String email);
}