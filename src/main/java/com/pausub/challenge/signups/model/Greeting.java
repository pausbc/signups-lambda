package com.pausub.challenge.signups.model;

import lombok.*;

import java.util.List;

@Builder
@Value
public class Greeting {

    String sender;
    long receiver;
    String message;
    List<Long> recentUserIds;

}
