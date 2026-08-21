package com.isovalidator.iso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;


/**
 * Allowed origins come from an env var so this works across
 * environments without a code change:
 *
 *   Railway -> this service -> Variables -> ALLOWED_ORIGINS
 *
 * Comma-separated, e.g.:
 *   https://iso20022-validator-ui.vercel.app,http://localhost:5173
 *
 * Uses allowedOriginPatterns (not allowedOrigins) so a wildcard also
 * works for Vercel's per-branch preview URLs, e.g.:
 *   https://iso20022-validator-ui-*.vercel.app
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ALLOWED_ORIGINS:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();

        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
