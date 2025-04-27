package com.crm.authservice.auth_api1.models;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account"),
    RESET_PASSWORD("reset_password"),
    VERIFY_ACCOUNT("verify_account_email"),
    PASSWORD_RESET_CONFIRMATION("password_reset_confirmation");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }

    // Utility method to get enum by name
    public static EmailTemplateName fromName(String name) {
        for (EmailTemplateName template : values()) {
            if (template.getName().equals(name)) {
                return template;
            }
        }
        throw new IllegalArgumentException("No email template found with name: " + name);
    }
}
