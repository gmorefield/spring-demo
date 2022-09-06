package com.example.springdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("info.kube")
public class InfoProps {
    private String prop;
    private String prop2;
    
    public String getProp() {
        return prop;
    }
    public void setProp(String prop) {
        this.prop = prop;
    }
    public String getProp2() {
        return prop2;
    }
    public void setProp2(String prop2) {
        this.prop2 = prop2;
    }
}
