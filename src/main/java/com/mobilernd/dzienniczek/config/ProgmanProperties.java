package com.mobilernd.dzienniczek.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProgmanProperties {

    @Value("${progman.unit}")
    private String unit;

    @Value("${progman.username}")
    private String username;

    @Value("${progman.password}")
    private String password;

    @Value("${progman.childId}")
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