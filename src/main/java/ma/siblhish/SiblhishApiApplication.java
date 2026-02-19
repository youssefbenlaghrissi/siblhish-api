package ma.siblhish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiblhishApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiblhishApiApplication.class, args);
    }

}
