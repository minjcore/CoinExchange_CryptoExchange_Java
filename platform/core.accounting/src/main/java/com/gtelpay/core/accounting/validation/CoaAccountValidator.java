package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.repository.CoaAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CoaAccountValidator {

    private final Set<String> known = ConcurrentHashMap.newKeySet();
    private final CoaAccountRepository repo;

    public CoaAccountValidator(CoaAccountRepository repo) {
        this.repo = repo;
    }

    public boolean exists(String code) {
        if (known.contains(code)) return true;
        boolean found = repo.existsById(code);
        if (found) known.add(code);
        return found;
    }
}
