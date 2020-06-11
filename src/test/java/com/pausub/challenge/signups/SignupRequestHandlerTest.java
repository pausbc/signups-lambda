package com.pausub.challenge.signups;

import com.pausub.challenge.signups.model.Greeting;
import com.pausub.challenge.signups.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;

public class SignupRequestHandlerTest {

    private final SignupRequestHandler handler = new SignupRequestHandler();

    @Test
    public void createGreetSuccess() {
        User user = createUser(1589278470,"Mark");
        List<User> users = createGreetUsers();

        Greeting expectedGreeting = Greeting.builder()
                .receiver(1589278470)
                .message("Hi Mark, welcome to komoot. Anna, Stephen and Lise also joined recently.")
                .sender("pausub@gmail.com")
                .recentUserIds(asList(627362498L, 1093883245L, 304390273L))
                .build();

        Greeting actualGreeting = handler.createGreet(user, users);
        assertThat(actualGreeting).isEqualTo(expectedGreeting);
    }

    @Test
    public void findUsersForGreetEmptyList() {
        assertThat(handler.findUsersForGreet("Mark", emptyList(), 3)).isEmpty();
    }

    @Test
    public void findUsersForGreetReturnsUnique() {
        List<User> result = handler.findUsersForGreet("Tom", createUserPool(), 4);
        assertThat(result).hasSize(3);
        assertThat(result.stream().filter(u -> u.getName().equals("Stephen")).count()).isEqualTo(1);
    }

    @Test
    public void findUsersForGreetReturnsNamesFilteringCurrentUserName() {
        List<User> result = handler.findUsersForGreet("Lise", createUserPool(), 4);
        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(u -> u.getName().equals("Lise")).count()).isZero();
    }

    @Test
    public void createGreetMessageNoRecentUsers() {
        String message = handler.createGreetMessage(createUser(1589278470,"Mark"), emptyList());
        assertThat(message).isEqualTo("Hi Mark, welcome to komoot.");
    }

    @Test
    public void createGreetMessageSingleRecentUser() {
        String message = handler.createGreetMessage(createUser(1589278470,"Mark"), singletonList("Lise"));
        assertThat(message).isEqualTo("Hi Mark, welcome to komoot. Lise also joined recently.");
    }

    @Test
    public void createGreetMessage2RecentUsers() {
        String message = handler.createGreetMessage(
                createUser(1589278470,"Mark"),
                asList("Karl", "Lise")
        );
        assertThat(message).isEqualTo("Hi Mark, welcome to komoot. Lise and Karl also joined recently.");
    }

    @Test
    public void createGreetMessage4RecentUsers() {
        String message = handler.createGreetMessage(
                createUser(1589278470,"Mark"),
                asList("Lise", "Karl", "Anna", "Stephen")
        );
        assertThat(message).isEqualTo("Hi Mark, welcome to komoot. Karl, Anna, Stephen and Lise also joined recently.");
    }

    @Test
    public void createUpdatedUserPoolEmptyPool() {
        assertThat(createUpdatedUserPool(createUser(1, "Erik"), emptyList())).hasSize(1);
    }

    @Test
    public void createUpdatedUserPoolSingleUser() {
        List<User> pool = singletonList(createUser(1, "John"));
        List<User> result = createUpdatedUserPool(createUser(2, "Erik"), pool);
        assertThat(result).hasSize(2);
    }

    @Test
    public void createUpdatedUserPoolRemoveOldest() {
        User user = createUser(2, "John");

        List<User> pool = asList(
                createUser(627362498, "Lise"),
                createUser(1093883245, "Anna"),
                createOldUser()
        );

        List<User> result = createUpdatedUserPool(user, pool);
        assertThat(result).hasSize(3);
        assertThat(result.stream().noneMatch(u -> u.getId() == 1)).isTrue();
    }

    @Test
    public void createUpdatedUserPoolRemoveCurrentIfOldest() {
        List<User> result = createUpdatedUserPool(createOldUser(), createGreetUsers());
        assertThat(result).hasSize(3);
        assertThat(result.stream().noneMatch(u -> u.getId() == 1)).isTrue();
    }

    @Test
    public void difference() {
        assertThat(handler.difference(asList(1, 2, 3), singletonList(3))).isEqualTo(Arrays.asList(1, 2));
    }

    private List<User> createUpdatedUserPool(User user, List<User> users) {
        return handler.createUpdatedUserPool(user, users, 3);
    }

    private List<User> createUserPool() {
        return concat(createGreetUsers().stream(), of(createUser(304390274, "Stephen"))).collect(toList());
    }

    private User createOldUser() {
        return User.builder()
                .name("Uve")
                .createdAt(Date.from(Instant.now().minusSeconds(10)))
                .id(1)
                .build();
    }

    // Unique users
    private List<User> createGreetUsers() {
        return asList(
                createUser(627362498, "Lise"),
                createUser(1093883245, "Anna"),
                createUser(304390273, "Stephen")
        );
    }

    private User createUser(long id, String name) {
        return User.builder()
                .name(name)
                .createdAt(new Date())
                .id(id)
                .build();
    }

}
