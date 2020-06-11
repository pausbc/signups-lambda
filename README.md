# Welcome to signups-lambda!

## Build
* To build a deployable jar: `mvn clean package shade:shade`. Corretto JDK 11 recommended.
* Can be deployed with [komoot-cdk](https://github.com/pausbc/komoot-cdk)

## Workflow
* Read events from SNS topic. Example event payload: 
```
{
    "created_at": "2020-06-11T15:18:33",
    "name": "Ethan",
    "id": 1591888713352
}
```
* Validate event
* Load recently registered users pool from dynamo db (size: 20)
* Check if user is in a pool. If yes - exit function (notification already sent).
* Add user to the pool if users created_at date is greater than earliest created_at user date in pool.
* Persists pool
* Create greeting
    * Randomize pool to get different users for each greeting even if pool contains the same items.
    * Look for 3 users in pool with unique names. Preferably with a different name than current user.
    * If less than 3 suitable users found - form a greeting with fewer users.
* Send greeting to https://notification-backend-challenge.main.komoot.net
    * If sending failed - wait for preconfigured time and retry for maximum 3 times

## Possible improvements
* Would prefer to push notifications using SNS que which then do POST to notification endpoint, but it should be able to 
accept SNS POST messages
* More resilient solution could be implemented using orchestration with step functions:
    * Move notifications to queues so that they would be pulled instead of pushed
    * Save failed notifications to separate queue, implement retry policy
    * Use environment variables instead of hard coded constants.
* Use aws lambda layers to reduce lambda deployment size
* Migrate to gradle (Kotlin DSL FTW!)
* And many more.