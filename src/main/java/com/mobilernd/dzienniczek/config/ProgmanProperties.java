package com.mobilernd.dzienniczek.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProgmanProperties {

    @Value("${PROGMAN_UNIT:}")
    private String unit;

    @Value("${PROGMAN_USERNAME:}")
    private String username;

    @Value("${PROGMAN_PASSWORD:}")
    private String password;

    @Value("${PROGMAN_CHILD_ID:}")
    private String childId;

    public String getUnit() {
        return unit;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getChildId() {
        return childId;
    }
}