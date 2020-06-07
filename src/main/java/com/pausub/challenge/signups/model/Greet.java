package com.pausub.challenge.signups.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Greet {

    private String sender;
    private String receiver;
    private String message;
    private List<String> recentUserIds;

}
