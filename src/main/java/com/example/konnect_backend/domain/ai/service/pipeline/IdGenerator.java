package com.example.konnect_backend.domain.ai.service.pipeline;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdGenerator {
    private AtomicLong id = new AtomicLong(1);

    public long newId() {
        return id.getAndIncrement();
    }
}
