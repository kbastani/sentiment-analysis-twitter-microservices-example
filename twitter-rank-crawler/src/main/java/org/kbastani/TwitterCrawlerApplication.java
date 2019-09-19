package org.kbastani;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This is the entry class for the Spring Boot application, and will automatically scan
 * for beans and configuration classes that should be automatically configured
 *
 * @author kbastani
 */
@SpringCloudApplication
@EnableZuulProxy
@EnableScheduling
public class TwitterCrawlerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TwitterCrawlerApplication.class).run(args);
    }

}
