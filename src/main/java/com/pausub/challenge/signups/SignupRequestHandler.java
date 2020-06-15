package com.pausub.challenge.signups;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.*;
import com.pausub.challenge.signups.model.Greeting;
import com.pausub.challenge.signups.model.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

public class SignupRequestHandler implements RequestHandler<SNSEvent, String> {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String SENDER_EMAIL = "pausub@gmail.com";
    private static final String GREET_MESSAGE = "Hi %s, welcome to komoot.";
    private static final String GREET_MESSAGE_TAIL = " %s also joined recently.";

    // 10 seconds retry timeout for easier debug.
    private static final int PUSH_NOTIFICATION_SERVICE_RETRY_TIMEOUT_MILLIS = 1000 * 10;
    private static final int USER_POOL_SIZE = 20;
    private static final int USERS_IN_GREET_COUNT = 3;

    private static final String PUSH_NOTIFICATION_ENDPOINT = "https://notification-backend-challenge.main.komoot.net";
    private static final int HTTP_REQUEST_RETRY_COUNT = 3;

    private static final Regions REGION = Regions.EU_WEST_1;

    private final AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
    private final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(dynamoDBClient);

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat(DATE_FORMAT)
            .setPrettyPrinting()
            .create();

    public String handleRequest(SNSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Event: " + event);
        String message = event.getRecords().get(0).getSNS().getMessage(); // assume single record per event

        logger.log("Message json: " + message);
        User user = gson.fromJson(message, User.class);

        validate(user);
        List<User> userPool = loadOrderedUserPool();
        logger.log("Initial user pool: " + gson.toJson(userPool));
        validateUserWithPool(user, userPool);

        updateAndPersistPool(user, userPool);

        Greeting greeting = createGreet(user, findUsersForGreet(user.getName(), userPool, USERS_IN_GREET_COUNT));
        String greetJson = gson.toJson(greeting);
        logger.log("Sending greet: " + greetJson);
        HttpResponse<String> greetResponse = sendGreet(greetJson, logger);
        logger.log("Greet response: " + gson.toJson(greetResponse.body()));
        return greetJson;
    }

    private HttpResponse<String> sendGreet(String greetJson, LambdaLogger logger) {
        for (int i = 0; i < HTTP_REQUEST_RETRY_COUNT; i++) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PUSH_NOTIFICATION_ENDPOINT))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(greetJson))
                        .build();
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                logger.log("HTTP send greet failed with: " + e.getMessage());
                logger.log(ExceptionUtils.getStackTrace(e));
            }
            try {
                Thread.sleep(PUSH_NOTIFICATION_SERVICE_RETRY_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        throw new RuntimeException(String.format("Failed to send notification after %d retries", HTTP_REQUEST_RETRY_COUNT));
    }

    List<User> findUsersForGreet(String currentUserName, List<User> userPool, int limit) {
        List<User> localUserPoolCopy = new ArrayList<>(userPool);
        Collections.shuffle(localUserPoolCopy);
        Map<String, User> uniqueUsersByName = localUserPoolCopy.stream()
                .filter(u -> !u.getName().equals(currentUserName)) // helps with name uniqueness
                .collect(toMap(User::getName, u -> u, (u1, u2) -> u1));
        return uniqueUsersByName.values().stream().limit(limit).collect(toList());
    }

    // Assumes valid input
    Greeting createGreet(User user, List<User> usersForGreet) {
        String greetMessage = createGreetMessage(user, usersForGreet.stream().map(User::getName).collect(toList()));
        return Greeting.builder()
                .message(greetMessage)
                .receiver(user.getId())
                .recentUserIds(usersForGreet.stream().map(User::getId).collect(toList()))
                .sender(SENDER_EMAIL)
                .build();
    }

    String createGreetMessage(User user, List<String> userNames) {
        return String.format(GREET_MESSAGE, user.getName()) + createRecentUsersMessagePart(userNames);
    }

    private String createRecentUsersMessagePart(List<String> recentUsers) {
        if (recentUsers.isEmpty()) {
            return "";
        } else if (recentUsers.size() == 1) {
            return String.format(GREET_MESSAGE_TAIL, recentUsers.get(0));
        } else {
            String tailJoined = recentUsers.stream().skip(1).collect(joining(", "));
            String usersMessage = String.format("%s and %s", tailJoined, recentUsers.get(0));
            return String.format(GREET_MESSAGE_TAIL, usersMessage);
        }
    }

    // If user already exists in pool - notification should not be sent
    private void validateUserWithPool(User currentUser, List<User> userPool) {
        boolean existsInPool = userPool.stream().anyMatch(user -> currentUser.getId() == user.getId());
        if (existsInPool) {
            throw new IllegalStateException(String.format("User with id %s already exists", currentUser.getId()));
        }
    }

    void updateAndPersistPool(User currentUser, List<User> initialUserPool) {
        List<User> updatedPool = createUpdatedUserPool(currentUser, initialUserPool, USER_POOL_SIZE);
        List<User> removedUsers = difference(initialUserPool, updatedPool);
        if (!removedUsers.isEmpty()) {
            dynamoDBMapper.batchDelete(removedUsers);
        }
        if (updatedPool.contains(currentUser)) {
            dynamoDBMapper.save(currentUser);
        }
    }

    <T> List<T> difference(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<>(list1);
        result.removeAll(list2);
        return result;
    }

    List<User> createUpdatedUserPool(User user, List<User> userPool, int userPoolSize) {
        List<User> localUsers = new ArrayList<>(userPool);
        localUsers.add(user);
        sortUsersByCreateDate(localUsers);
        return localUsers.stream().limit(userPoolSize).collect(toList());
    }

    private List<User> loadOrderedUserPool() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<User> users = dynamoDBMapper.scanPage(User.class, scanExpression).getResults();
        sortUsersByCreateDate(users);
        return users;
    }

    private void sortUsersByCreateDate(List<User> users) {
        users.sort(Comparator.comparing(User::getCreatedAt).reversed());
    }

    private void validate(User user) {
        requireNonNull(user, "User can not be null");
        requireNonNull(user.getCreatedAt(), "User createdAt date can not be null");
        if (StringUtils.isAllEmpty(user.getName())) {
            throw new IllegalStateException("User name can not be empty");
        }
    }
}
