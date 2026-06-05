package com.project.watchmate.common.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {
    private static final Duration TMDB_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration TMDB_RESPONSE_TIMEOUT = Duration.ofSeconds(8);
    private static final long TMDB_IO_TIMEOUT_SECONDS = 8L;

    @Value("${tmdb.api.token}")
    private String tmdbToken;

    @Bean
    public WebClient tmdbWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TMDB_CONNECT_TIMEOUT.toMillis())
            .responseTimeout(TMDB_RESPONSE_TIMEOUT)
            .doOnConnected(connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(TMDB_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(TMDB_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl("https://api.themoviedb.org/3")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbToken)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build())
            .build();
    }
}


