package org.kbastani;

import org.junit.Test;
import org.kbastani.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@WebAppConfiguration
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
