package org.quangdung.domain.usecase.interfaces;

import io.smallrye.mutiny.Uni;

/**
 * Interface for deleting an email address from the alert notification list
 */
public interface IDeleteAlertEmailUseCase {
    /**
     * Deletes an email address from the alert notification list
     * 
     * @param email The email address to delete
     * @return Uni<Long> indicating the number of emails removed (0 or 1)
     */
    Uni<Integer> execute(String email);
}