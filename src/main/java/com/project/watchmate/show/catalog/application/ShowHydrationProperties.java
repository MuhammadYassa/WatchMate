package com.project.watchmate.show.catalog.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "watchmate.show-hydration")
public class ShowHydrationProperties {

    private int maxSynchronousMissingSeasons = 3;

    private int maxSynchronousEpisodes = 100;

    private int batchSize = 1;
}

