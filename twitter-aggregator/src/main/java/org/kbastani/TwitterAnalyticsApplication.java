package org.kbastani;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This is the entry class for the Spring Boot application, and will automatically scan
 * for beans and configuration classes that should be automatically configured
 *
 * @author kbastani
 */
@SpringBootApplication
@EnableScheduling
public class TwitterAnalyticsApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TwitterAnalyticsApplication.class).web(true).run(args);
    }

}
