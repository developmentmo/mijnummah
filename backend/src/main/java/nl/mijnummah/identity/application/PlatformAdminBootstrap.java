package nl.mijnummah.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.infrastructure.BootstrapProperties;

/** Creates the first platform administrator from configuration, see {@link BootstrapProperties}. */
@Component
@Order(1)
@EnableConfigurationProperties(BootstrapProperties.class)
class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final BootstrapProperties properties;
    private final UserAdminService userAdminService;

    PlatformAdminBootstrap(BootstrapProperties properties, UserAdminService userAdminService) {
        this.properties = properties;
        this.userAdminService = userAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.configured() || userAdminService.platformAdminExists()) {
            return;
        }
        userAdminService.createUnchecked(properties.email(), properties.displayNameOrDefault(), properties.password(),
                PlatformRole.PLATFORM_ADMIN);
        log.info("Created the first platform administrator ({})", properties.email());
    }
}
