package nl.mijnummah.mosque.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A mosque (tenant). Its slug is fixed after creation: it is part of public URLs and app configuration. */
@Entity
@Table(name = "mosque")
public class Mosque {

    @Id
    private UUID id;

    @Column(name = "slug", nullable = false, updatable = false, length = 60)
    private String slug;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Mosque() {
        // for JPA
    }

    public static Mosque create(String slug, MosqueDetails details, Instant now) {
        Mosque mosque = new Mosque();
        mosque.id = UUID.randomUUID();
        mosque.slug = Objects.requireNonNull(slug, "slug");
        mosque.active = true;
        mosque.createdAt = Objects.requireNonNull(now, "now");
        mosque.apply(details, now);
        return mosque;
    }

    public void update(MosqueDetails details, Instant now) {
        apply(details, now);
    }

    public void setActive(boolean active, Instant now) {
        this.active = active;
        this.updatedAt = now;
    }

    private void apply(MosqueDetails details, Instant now) {
        name = details.name();
        description = details.description();
        street = details.street();
        postalCode = details.postalCode();
        city = details.city();
        countryCode = details.countryCode();
        email = details.email();
        phone = details.phone();
        websiteUrl = details.websiteUrl();
        timeZone = details.timeZone();
        updatedAt = now;
    }

    public MosqueDetails details() {
        return new MosqueDetails(name, description, street, postalCode, city, countryCode, email, phone, websiteUrl,
                timeZone);
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
