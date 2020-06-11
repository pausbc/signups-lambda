package com.pausub.challenge.signups.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.util.Date;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName="Users")
public class User {

    @DynamoDBHashKey()
    private long id;
    private String name;
    private Date createdAt;
}
