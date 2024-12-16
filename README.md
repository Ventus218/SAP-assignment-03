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

|As a|I want|so that|
|----|----------|-------------|
|user|to go on a ride with a rented bike|I can leave it wherever i want|
|business stakeholder|the bikes to autonomously go to the user when requested|provide a fantastic service to my customers|
|business stakeholder|the bikes to autonomously go back to the charging station|save money with respect to paying someone to do that|
|system administrator|to see the current location of every bike|I can check if was left too far|
|system administrator|to see which users are currently riding a bike|I can spot any anomaly if present|
|system administrator|to see all the registered users|I can spot any anomaly if present|
|system administrator|to add new bikes to the system|I can increase the number of bikes in the future|

### Business requirements
Autonomous bikes must follow the traffic laws (stopping at red traffic lights)

### Use cases

![Use case diagram](./doc/diagrams/use-cases.png)

#### Scenarios

- Go on a ride:
    1. The user chooses an available bike and selects "Start ride"
    1. The bike detaches from the charging station and rides up to the user
    1. The users rides the bike
    1. The user selects "End ride"
    1. The bike autonomously rides back to the charging station

- Add new bike:
    1. The system administrator chooses an id for the new bike and confirms
    1. The system checks that the id is valid, and if it's not it fails the operation
    1. The system register the new bike with the given valid id

- See registered users:
    1. The system administrator interface shows always every registered user

- Monitor rides
    1. The system administrator interface shows user usernames that are on a ride alongside the bike their riding

- Monitor bike positons
    1. The system administrator interface shows a graphical representation of the bike locations on a 2D space

## Analisys

### Bounded contexts
Given the requirements multiple bounded contexts were identified:

- System administrator interactions
- User interactions
- Users management
- E-bikes management
- Rides management
- Autonomous city riding

### Ubiquitous language

|Word|Definition|Synonyms|
|----|----------|--------|
|User|The actual app customer one which rents bikes to ride|Customer|
|Username|A text chosen by the user which uniquely identifies him inside the system|User id|
|Admin|An employee of the organization whose responsibility is to monitor the system and to take actions to let the system work as expected|System administrator|
|E-bike|An electric bike which can be rented by the users|Ebike, bike|
|E-bike location|The geographical location of the bike|E-bike position|
|Charging station|A phisical place in the city where ebikes can charge their batteries||
|Traffic light|An indicator on a street junction that indicates whether the traffic should stop or proceed|Semaphore|
|Traffic light state|A traffic light can be Red (stop) or Green (pass)|Light|
|Junction|A place where multiple streets meet||
|Street|A rideable road segment between two street junctions|Road|
|Ride|The rental of a bike from a user which aims to use it to move from one place to another||
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
