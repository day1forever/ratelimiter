# Rate Limiter Service

## Requirements
1. Throttle Request based on user id + API name. For example, we want to limit user A can call getUserInfo 100 times within 1 minute window.
2. Use Sliding Window limit algorithm.

## Extra Features
1. Expose api to create custom limit info for different user, in this way, we can override user limitation if needed.
2. Expose api to return current user limitation info.
3. Expose api to white list API, rate limiter service will only do throttle on those white-listed APIs.

## Design

* Storage: it is a high write-read service, we need a storage provides a high efficiency in read&write. Redis is memory-based DB, it supports high concurrent read&write with great performance.
   In current version, we will have a local Redis for storage.
* Sliding Window Throttling Algorithm: split the window with fixed number, e.g with 100 timeframes. For 1 minute window, there will be 100 timeframe with 600ms. Count request number per 600ms, maintain a list of {timeFrameiId: count} pair in Redis. When ever a request comes, calculate the sum of count in pair list. If sum is bigger than limitation, throttle current request.
   Also check maintained list, remove pair if timeFrameId is our of current window.


## How to run

Run run.sh to run service locally, or have it invoked inside docker in production environment
if there is no config specified, it will use default config(assuming running locally)
```bash
./run.sh
```

## APIs
### getLimitInfo
get user limit info
```bash
curl http://localhost:20080/v20210131/limit-info/{userid}/{apiName}
```
Response
```json
{"limitNum":100,"duration":60,"durationUnit":"Second","userId":"userA","apiName":"getUserInfo"}
```
### createLimitInfo
create user limit info based on userId and apiName
```bash
curl --header "Content-Type: application/json" --request POST --data '{"limitNum": 100, "duration": 60, "durationUnit": "Second", "userId": "userA", "apiName": "getUserInfo"}' http://localhost:20080/v20210131/limit-info/
```
Response: http 200

### throttleRequest
check if request should be throttled based on userId and apiName
response: false request should not be throttled and should pass through; true the request should be blocked/throttled
```bash
curl --header "Content-Type: application/json" --request POST --data '{"limitNum": 5, "duration": 60, "durationUnit": "Second", "userId": "userA", "apiName": "getUserInfo"}' http://localhost:20080/v20210131/limit-info/

curl http://localhost:20080/v20210131/throttle/userA/getUserInfo
#false
curl http://localhost:20080/v20210131/throttle/userA/getUserInfo
#false
curl http://localhost:20080/v20210131/throttle/userA/getUserInfo
#false
curl http://localhost:20080/v20210131/throttle/userA/getUserInfo
#false
curl http://localhost:20080/v20210131/throttle/userA/getUserInfo
#false
curl http://localhost:20080/v20210131/throttle/userA/getUserInfo
#true
```

### registerApiName
whitelist an apiName, after whitelisting service can recogniza this apiName
```bash
curl --header "Content-Type: application/json" --request POST --header 'apiName: getUserInfo' http://localhost:20080/v20210131/apiName
```
Response: http 200

## Future Work
* For real production usage, we need a Redis cluster in terms of redundancy.
* Implement checking whitelisted apiName in API: createLimitInfo and throttleRequest
* Adding Authentication/Authorization module
* Adding more critical log/metrics