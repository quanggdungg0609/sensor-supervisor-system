package org.quangdung.domain.usecase.interfaces;

import io.smallrye.mutiny.Uni;

public interface IDeleteAllThresholdsUseCase {
    Uni<Void> execute(String clientId);
}
