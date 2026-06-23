package com.project.watchmate.common.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "watchmate.cors")
public class WatchMateCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
