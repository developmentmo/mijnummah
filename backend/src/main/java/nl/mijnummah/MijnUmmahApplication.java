package nl.mijnummah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mijn Ummah backend: one modular monolith serving every mosque (tenant) from one database.
 * Modules are the direct sub-packages; their boundaries are verified by {@code ModularityTest}.
 */
@SpringBootApplication
public class MijnUmmahApplication {

    public static void main(String[] args) {
        SpringApplication.run(MijnUmmahApplication.class, args);
    }
}
