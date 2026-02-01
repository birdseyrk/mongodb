build-readme

open a bash window

    cd /c/code/mongodb/java/hybriddb2/hybrid_events/mongo-hybrid-events
    mvn clean
    mvn package   (or mvn clean package -DskipTests)
    mvn clean package -DskipTests
    mvn tests

running the tests


    cd /c/code/mongodb/java/hybriddb2/hybrid_events/mongo-hybrid-events
    mvn test

5) How to build + deploy with Git, Jenkins, Ansible
A) On Windows: create repo + push to Git

From the project root:

git init
git add .
git commit -m "Mongo hybrid events REST service + Ansible deploy + Jenkins pipeline"
git remote add origin <YOUR_GIT_REMOTE_URL>
git branch -M main
git push -u origin main

B) Jenkins: one-time setup

Create a Jenkins Pipeline job pointing at your Git repo (Pipeline from SCM).

Add Jenkins Credentials:

SSH key to Linux server:

ID: linux-deploy-ssh

Type: “SSH Username with private key”

user: your deploy user (must exist on Ubuntu server)

Mongo password:

ID: mongo_password

Type: “Secret text”

value: actual password for mongoUser1

Ensure Jenkins agent has:

Java 17 JDK

Maven

Ansible

(Optional) Docker, if you run Testcontainers tests

C) What Jenkins will do on each build

mvn clean package -DskipTests

mvn test (remove this stage or skip if no Docker)

ansible-playbook ... deploys jar + systemd service on Ubuntu

D) Ubuntu server: verify service

On the Ubuntu server:

sudo systemctl status mongo-hybrid
journalctl -u mongo-hybrid -f

6) Where you put JSON records to insert (continuous + file import)

Put the JSON file on the server here (Ansible creates this directory):

/opt/mongo-hybrid/inbox/events.json

File format should be a JSON array:

[
  {
    "eventTs": "2026-01-30T12:34:56Z",
    "id": "id_1",
    "payload": { "temperature": 55, "status": "OK", "meta": { "sequence": 1, "source": "ansible" } }
  }
]


Important: Field naming

In Java we use eventTs, but Mongo field is event_ts.

For REST JSON, Jackson will use eventTs by default.
If you want REST JSON to use event_ts exactly, tell me and I’ll add Jackson annotations so REST and Mongo match perfectly.

Trigger import:

curl -X POST http://localhost:8080/events/import-file \
  -H "Content-Type: application/json" \
  -d '{"path":"/opt/mongo-hybrid/inbox/events.json"}'

Two small decisions (tell me and I’ll lock the JSON format)

For REST input, do you want the timestamp field to be eventTs or event_ts?

Do you want the service bound to 127.0.0.1 (safer) or 0.0.0.0 (reachable from your network)? (Right now it’s 0.0.0.0 via Ansible var.)