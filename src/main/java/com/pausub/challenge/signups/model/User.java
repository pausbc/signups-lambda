package com.pausub.challenge.signups.model;

import lombok.*;

import java.util.Date;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private String id;
    private String name;
    private Date createdAt;

}
