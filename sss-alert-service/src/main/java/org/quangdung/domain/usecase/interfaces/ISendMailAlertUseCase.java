package org.quangdung.domain.usecase.interfaces;

import io.smallrye.mutiny.Uni;

public interface ISendMailAlertUseCase {
    Uni<Void> execute(String level, String message);
}
