package nl.mijnummah.media.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.mijnummah.media.domain.VideoProviders;

@Configuration(proxyBeanMethods = false)
class MediaConfiguration {

    @Bean
    VideoProviders videoProviders() {
        return VideoProviders.defaults();
    }
}
