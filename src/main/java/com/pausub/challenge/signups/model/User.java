package com.pausub.challenge.signups.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.util.Date;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName="User")
public class User {

    @DynamoDBHashKey()
    private String id;
    private String name;
    private Date createdAt;
}
