package com.pausub.challenge.signups;

import com.pausub.challenge.signups.model.Greet;
import com.pausub.challenge.signups.model.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SignupRequestHandlerTest {

    private final SignupRequestHandler handler = new SignupRequestHandler();

    @Test
    public void generateResponseSuccess() {

        User user = User.builder()
                .name("Mark")
                .createdAt(new Date())
                .id("1589278470")
                .build();

        Greet expectedGreet = Greet.builder()
                .receiver("1589278470")
                .message("Hi Mark, welcome to komoot. Lise, Anna and Stephen also joined recently.")
                .sender("pausub@gmail.com")
                .recentUserIds(Arrays.asList("1", "2", "3"))
                .build();

        Greet actualGreet = handler.createGreeting(user);
        assertThat(actualGreet).isEqualTo(expectedGreet);
    }
    // TODO: non happy path tests

}
