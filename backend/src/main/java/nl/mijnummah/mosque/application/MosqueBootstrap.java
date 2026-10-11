package nl.mijnummah.mosque.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.domain.MosqueDetails;
import nl.mijnummah.mosque.infrastructure.MosqueBootstrapProperties;

/** Creates the first mosque from configuration, see {@link MosqueBootstrapProperties}. Runs after the admin bootstrap. */
@Component
@Order(2)
@EnableConfigurationProperties(MosqueBootstrapProperties.class)
class MosqueBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MosqueBootstrap.class);

    private final MosqueBootstrapProperties properties;
    private final MosqueService mosqueService;
    private final MosqueAdministratorService administratorService;
    private final UserDirectory userDirectory;

    MosqueBootstrap(MosqueBootstrapProperties properties, MosqueService mosqueService,
                    MosqueAdministratorService administratorService, UserDirectory userDirectory) {
        this.properties = properties;
        this.mosqueService = mosqueService;
        this.administratorService = administratorService;
        this.userDirectory = userDirectory;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.configured() || mosqueService.exists(properties.slug())) {
            return;
        }
        var details = new MosqueDetails(properties.name(), null, null, null, properties.city(), "NL", null, null, null,
                "Europe/Amsterdam");
        Mosque mosque = mosqueService.createUnchecked(properties.slug(), details);
        log.info("Created mosque '{}' ({})", mosque.getName(), mosque.getSlug());
        userDirectory.findActiveByEmail(properties.adminEmail()).ifPresent(user -> {
            administratorService.grantUnchecked(mosque.getId(), user.id(), MosqueRole.ADMIN);
            log.info("Made BOOTSTRAP_MOSQUE_ADMIN_EMAIL administrator of '{}'", mosque.getSlug());
        });
    }
}
