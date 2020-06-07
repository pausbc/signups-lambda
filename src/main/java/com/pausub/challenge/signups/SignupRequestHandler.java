package com.pausub.challenge.signups;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pausub.challenge.signups.model.Greet;
import com.pausub.challenge.signups.model.User;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class SignupRequestHandler implements RequestHandler<Map<String, String>, String> {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final String SENDER_EMAIL = "pausub@gmail.com";
    private static final String GREET_MESSAGE = "Hi %s, welcome to komoot. %s also joined recently.";

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .setDateFormat(DATE_FORMAT)
            .setPrettyPrinting()
            .create();

    public String handleRequest(Map<String,String> event, Context context) {
        LambdaLogger logger = context.getLogger();
        String eventJson = gson.toJson(event);
        logger.log("EVENT: " + eventJson);
        User user = gson.fromJson(eventJson, User.class);
        validateUser(user);
        String response = gson.toJson(createGreeting(user));
        logger.log("RESPONSE: " + response);
        return response;
    }

    // Assumes valid input
    Greet createGreeting(User user) {
        List<String> recentUsers = Arrays.asList("Lise", "Anna", "Stephen");
        List<String> recentUserIds = Arrays.asList("1", "2", "3");
        return Greet.builder()
                .message(createGreetMessage(user, recentUsers))
                .receiver(user.getId())
                .recentUserIds(recentUserIds)
                .sender(SENDER_EMAIL)
                .build();

    }

    private void validateUser(User user) {
        requireNonNull(user, "User can not be null");
        requireNonNull(user.getCreatedAt(), "User createdAt date can not be null");
        validateAllEmpty(user.getId(), "User id");
        validateAllEmpty(user.getName(), "User name");
    }

    private void validateAllEmpty(String field, String fieldDescription) {
        if (StringUtils.isAllEmpty(field)) {
            throw new IllegalStateException(fieldDescription + " can not be empty");
        }
    }

    private String createGreetMessage(User user, List<String> recentUsers) {
        return String.format(GREET_MESSAGE, user.getName(), toRecentUsersString(recentUsers));
    }

    private String toRecentUsersString(List<String> recentUsers) {
        return String.format("%s, %s and %s", recentUsers.get(0), recentUsers.get(1), recentUsers.get(2));
    }

}
