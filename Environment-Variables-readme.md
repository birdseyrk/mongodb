Environment-Variables-readme.md

run in powershell

cd c:
    cd code\mongodb\java\hybriddb2\hybrid_events\mongo-hybrid-events

$env:MONGO_PASSWORD="mongoUser1"
$env:MONGO_HOST="192.168.1.105"
$env:MONGO_DB="hybriddb2"
$env:MONGO_USER="mongoUser1"
$env:MONGO_COLLECTION="hybrid_events"
$env:APP_PORT="8080"
$env:APP_BIND_HOST="0.0.0.0"

java -jar target\mongo-hybrid-events-1.0.0.jar
