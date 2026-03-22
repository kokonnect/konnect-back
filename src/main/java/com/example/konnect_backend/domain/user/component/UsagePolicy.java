package com.example.konnect_backend.domain.user.component;

import com.example.konnect_backend.domain.user.entity.status.UsageType;
import org.springframework.stereotype.Component;

@Component
public class UsagePolicy {

    public int getLimit(boolean isGuest, UsageType usageType) {

        if (usageType == UsageType.DOCUMENT) {
            return isGuest ? 1 : 3;
        }

        if (usageType == UsageType.MESSAGE) {
            return isGuest ? 3 : 5;
        }

        return 0;
    }
}