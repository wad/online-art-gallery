package org.wadhome.oag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OagApplication {

    public static void main(String[] args) {
        SpringApplication.run(OagApplication.class, args);
    }
}
