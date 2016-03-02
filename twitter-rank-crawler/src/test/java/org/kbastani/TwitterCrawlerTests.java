package org.kbastani;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kbastani.twitter.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TwitterCrawlerApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@ActiveProfiles("test")
public class TwitterCrawlerTests {

    @Value("${local.server.port}")
    private int port = 0;

    @Test
    @IfProfileValue(name = "test")
    public void crawlUser() {
        ResponseEntity<User> user1 = new TestRestTemplate().getForEntity("http://localhost:" + port + "/v1/user/kennybastani", User.class);
        assertEquals(HttpStatus.OK, user1.getStatusCode());
        ResponseEntity<User> user2 = new TestRestTemplate().getForEntity("http://localhost:" + port + "/v1/user/bridgetkromhout", User.class);
        assertEquals(HttpStatus.OK, user2.getStatusCode());
        ResponseEntity<User> user3 = new TestRestTemplate().getForEntity("http://localhost:" + port + "/v1/user/starbuxman", User.class);
        assertEquals(HttpStatus.OK, user3.getStatusCode());
    }
}
