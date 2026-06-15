package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.foundation.exception.ValidationException;

public final class CreateJournalCommandValidator {

    public static final int REFERENCE_ID_MAX = 128;

    private CreateJournalCommandValidator() {
    }

    public static void validate(CreateJournalCommand cmd) {
        if (cmd == null) {
            throw new ValidationException("create journal command required");
        }
        if (cmd.referenceId() == null || cmd.referenceId().isBlank()) {
            throw new ValidationException("referenceId required");
        }
        if (cmd.referenceId().length() > REFERENCE_ID_MAX) {
            throw new ValidationException("referenceId max " + REFERENCE_ID_MAX);
        }
        if (cmd.useCase() == null || cmd.useCase().isBlank()) {
            throw new ValidationException("useCase required");
        }
    }
}
