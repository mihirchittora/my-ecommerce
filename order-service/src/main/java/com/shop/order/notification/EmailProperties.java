package com.shop.order.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {
    private boolean enabled;
    private String from = "no-reply@morrow.local";
    private String fromName = "Morrow Commerce";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
}
