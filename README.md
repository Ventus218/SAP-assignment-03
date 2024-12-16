# Assignment #03 - Software Architecture and Platforms - a.y. 2024-2025

v0.9.0-20241209

<!-- toc -->

- [Usage](#usage)
- [Requirements](#requirements)
  * [User Stories](#user-stories)
  * [Use cases](#use-cases)
    + [Scenarios](#scenarios)
  * [Business requirements](#business-requirements)
- [Analisys](#analisys)
  * [Bounded contexts](#bounded-contexts)
  * [Ubiquitous language](#ubiquitous-language)
- [Design](#design)
  * [EBikes and Users microservices](#ebikes-and-users-microservices)
  * [Rides microservice](#rides-microservice)
- [Deployment](#deployment)
- [Fault tolerance / recovering](#fault-tolerance--recovering)
- [Service discovery](#service-discovery)
- [Configuration](#configuration)

<!-- tocstop -->

**Description** 

- Develop a solution of the "EBike application" based on an event-driven microservices architecture, applying the Event Sourcing pattern where considered useful.

- Define a deployment of the application on a distributed infrastructure (e.g. a cluster) based on Kubernetes, exploiting the features provided by the framework.

- Consider an extension of the EBike case study, featuring a new kind of experimental e-bike, i.e. autonomous e-bike ("a-bike") for smart city environment. Main features of the a-bikes:
  - can autonomously reach the nearest station, after being used;
  - can autonomously reach a user, asking for the service.
  
  The a-bikes are meant to be deployed into a smart city featuring a digital twin which provides basic functionalities to support a-bike autonomous mobility. Propose a solution, discussing the design and including a demo implementation of a core part, capturing main aspects.  

    
**Deliverable**

A GitHub repo including sources and documentation. The link to the repo must be included in a file ``Assignment-03-<Surname>`` and submitted using a link on the course web site.

**Deadline** 

No deadlines.
 

## Usage
Running from the build tool (automatically assembles the jars):
```sh
sbt composeUpDev
```

Otherwise with the jars already built:
```sh
docker compose -f ./docker-compose.yml -f ./docker-compose.dev.yml --env-file ./development.env build
docker compose -f ./docker-compose.yml -f ./docker-compose.dev.yml  --env-file ./development.env up --force-recreate
```

Otherwise you can pull the images from dockerhub:
```sh
docker compose -f ./docker-compose.yml -f ./docker-compose.dev.yml -f ./docker-compose.hub.yml --env-file ./development.env up --force-recreate
```
 
## Requirements

### User Stories

|As a| I want to|so that I can|
|----|----------|-------------|
|user|go on a ride with a rented bike|leave it wherever i want|
|user|check my credit|understand if it needs to be recharged|
|user|recharge my credit|go on a ride|
|system administrator|see the current location of every bike|check if was left too far|
|system administrator|see which users are currently riding a bike|spot any anomaly if present|
|system administrator|see all the registered users and their credit|spot any anomaly if present|
|system administrator|add new bikes to the system|increase the number of bikes in the future|

### Use cases

![Use case diagram](./doc/diagrams/use-cases.png)

#### Scenarios

- Go on a ride:
    1. The user chooses an available bike and selects "Start ride"
    1. The user can see his credits updating while he's riding
    1. The user selects "End ride"

- Check credit:
    1. The user sees his credit right in the home screen

- Recharge credit:
    1. The user selects a "recharge credit" button
    1. The user inserts how much credits he wants to deposit
    1. The user confirms

- Add new bike:
    1. The system administrator chooses an id for the new bike and confirms
    1. The system checks that the id is valid, and if it's not it fails the operation
    1. The system register the new bike with the given valid id

- See registered users and their credit:
    1. The system administrator interface shows always every registered user alongside his credit

- Monitor rides
    1. The system administrator interface shows user usernames that are on a ride alongside the bike their riding

- Monitor bike positons
    1. The system administrator interface shows a graphical representation of the bike locations on a 2D space

### Business requirements
- The credit of the user must be decreased by 1 unit every second

## Analisys

### Bounded contexts
Given the requirements multiple bounded contexts were identified:

- System administrator interactions
- User interactions
- Users management
- E-bikes management
- Rides management

### Ubiquitous language

|Word|Definition|Synonyms|
|----|----------|--------|
|User|The actual app customer one which rents bikes to ride|Customer|
|Username|A text chosen by the user which uniquely identifies him inside the system|User id|
|Admin|An employee of the organization whose responsibility is to monitor the system and to take actions to let the system work as expected|System administrator|
|E-bike|An electric bike which can be rented by the users|Ebike, bike|
|E-bike location|The geographical location of the bike|E-bike position|
|Ride|The rental of a bike from a user which aims to use it to move from one place to another||
|Credit|An internal currency that the users exchange with bikes rental time||
|Recharge credit|Process executed by the user by which his credit is increased by the requested amount||
|Register new ebike|An action taken by the admin which has the outcome of making the system aware of a new bike which can then be rented|Create new ebike|
|Monitor ebikes/rides|Admin's capability to check the location of each bike and which users are riding them||

## Design

The system is designed follwing a microservice architecture where each bounded contexts is mapped to a single microservice or frontend.

![Components diagram](./doc/diagrams/components.png)

### EBikes and Users microservices

The EBikes microservice and the Users microservice are both built follwing the hexagonal architecture.

They don't depend on any other microservice.

![EBikes microservice components diagram](./doc/diagrams/ebikes-components.png)
![EBikes microservice domain model](./doc/diagrams/ebikes-microservice-domain-model.png)

![Users microservice components diagram](./doc/diagrams/users-components.png)
![Users microservice domain model](./doc/diagrams/users-microservice-domain-model.png)

### Rides microservice

The Rides microservice is built follwing the hexagonal architecture.

It depends on both the other microservices (EBikes and Users).

![Rides microservice components diagram](./doc/diagrams/rides-components.png)
![Rides microservice domain model](./doc/diagrams/rides-microservice-domain-model.png)

## Deployment
Each microservice will be deployed as a standalone Docker container while the two frontends will be deployed as standard GUI apps.

In order to achieve an effective and simple deployment a [docker compose file](./docker-compose.yml) has been written.

## Fault tolerance / recovering
The system will exploit the underlying deployment platform (Docker / Docker compose) to restart services in case of failure.

## Service discovery
A service discovery mechanism has to be implemented due to the subsequent reasons:
- Each microservice could be restarted in case of failure and as a consequence it could change it's network address
- Future versions of the software may require to create multiple instances of the same service due to heavy workloads and therefore the network address may change at runtime.

Given these requirements the built-in DNS service provided by Docker can be exploited to achieve the desired behavior.

## Configuration
Since the microservices configuration does not need to be changed at runtime the simplest way to provide an externalized configuration is through enviornment variables that will be passed at deploy-time.
