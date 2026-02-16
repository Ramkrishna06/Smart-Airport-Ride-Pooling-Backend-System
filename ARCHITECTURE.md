# System Architecture Documentation

## Table of Contents
1. [High-Level Architecture](#high-level-architecture)
2. [Component Diagram](#component-diagram)
3. [Database Schema](#database-schema)
4. [Request Flow](#request-flow)
5. [Deployment Architecture](#deployment-architecture)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Web App  │  │  Mobile  │  │   API    │  │  Admin   │   │
│  │          │  │   App    │  │ Clients  │  │  Panel   │   │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘   │
└────────┼─────────────┼─────────────┼─────────────┼─────────┘
         │             │             │             │
         └─────────────┴─────────────┴─────────────┘
                           │
                    HTTP/JSON (REST)
                           │
         ┌─────────────────▼────────────────────┐
         │      API GATEWAY / LOAD BALANCER     │
         │   (Future: Nginx/AWS ALB/Kong)       │
         └─────────────────┬────────────────────┘
                           │
         ┌─────────────────▼────────────────────┐
         │        APPLICATION LAYER              │
         │   ┌──────────────────────────────┐   │
         │   │   Spring Boot Application    │   │
         │   │                              │   │
         │   │  ┌─────────────────────┐    │   │
         │   │  │  RideController     │    │   │
         │   │  └──────────┬──────────┘    │   │
         │   │             │               │   │
         │   │  ┌──────────▼──────────┐    │   │
         │   │  │   RideService       │    │   │
         │   │  └──────────┬──────────┘    │   │
         │   │             │               │   │
         │   │  ┌──────────▼──────────┐    │   │
         │   │  │  MatchingService    │    │   │
         │   │  │  PricingService     │    │   │
         │   │  └──────────┬──────────┘    │   │
         │   │             │               │   │
         │   │  ┌──────────▼──────────┐    │   │
         │   │  │   Repositories      │    │   │
         │   │  │  (Spring Data JPA)  │    │   │
         │   │  └──────────┬──────────┘    │   │
         │   └─────────────┼───────────────┘   │
         └─────────────────┼────────────────────┘
                           │
         ┌─────────────────▼────────────────────┐
         │         DATA LAYER                   │
         │   ┌──────────────────────────────┐   │
         │   │      H2 Database (Dev)       │   │
         │   │  PostgreSQL (Production)     │   │
         │   │                              │   │
         │   │  Tables:                     │   │
         │   │  - rides                     │   │
         │   │  - passengers                │   │
         │   └──────────────────────────────┘   │
         └──────────────────────────────────────┘
```

---

## Component Diagram

```
┌────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              RideController                          │  │
│  │  + requestRide(RideRequestDTO)                       │  │
│  │  + getRide(Long rideId)                              │  │
│  │  + cancelRide(Long passengerId)                      │  │
│  │  + getSurgeInfo()                                    │  │
│  └───────────────────┬──────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
                       │
                       │ uses
                       ▼
┌────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │               RideService                            │  │
│  │  + requestRide(RideRequestDTO): RideResponseDTO     │  │
│  │  + getRideDetails(Long): RideDetailsDTO             │  │
│  │  + cancelRide(Long passengerId): void               │  │
│  │  - requestRideWithRetry(DTO, attempt): Response     │  │
│  │  - addPassengerToExistingRide(): Response           │  │
│  │  - createNewRide(): Response                        │  │
│  └───────────┬──────────────────────────────────────────┘  │
│              │                                              │
│              │ uses                                         │
│              ▼                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            MatchingService                           │  │
│  │  + findBestMatch(Passenger): Ride                   │  │
│  │  - isWithinSearchRadius(Ride, Pass): boolean        │  │
│  │  - calculateDetourCost(Ride, Pass): double          │  │
│  │  - exceedsDetourTolerance(...): boolean             │  │
│  └───────────┬──────────────────────────────────────────┘  │
│              │                                              │
│  ┌───────────▼──────────────────────────────────────────┐  │
│  │            PricingService                            │  │
│  │  + calculateFare(distance, count): double           │  │
│  │  + getCurrentSurgeInfo(): SurgeInfo                 │  │
│  │  - calculateSurgeMultiplier(): double               │  │
│  │  - calculateSharingDiscount(int): double            │  │
│  └───────────┬──────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
               │
               │ uses
               ▼
┌────────────────────────────────────────────────────────────┐
│                   REPOSITORY LAYER                          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          RideRepository (JpaRepository)              │  │
│  │  + findByStatus(RideStatus): List<Ride>            │  │
│  │  + findAvailableRidesByStatus(...): List<Ride>     │  │
│  │  + countByStatus(RideStatus): long                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │       PassengerRepository (JpaRepository)            │  │
│  │  + findByRideId(Long): List<Passenger>              │  │
│  │  + findByPhone(String): List<Passenger>             │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
               │
               │ manages
               ▼
┌────────────────────────────────────────────────────────────┐
│                      DOMAIN MODEL                           │
│                                                             │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │      Ride        │1      * │    Passenger     │         │
│  │──────────────────│◄────────│──────────────────│         │
│  │ - id: Long       │         │ - id: Long       │         │
│  │ - status         │         │ - name           │         │
│  │ - passengers     │         │ - phone          │         │
│  │ - availableSeats │         │ - pickupLocation │         │
│  │ - totalDistance  │         │ - dropoffLocation│         │
│  │ - finalFare      │         │ - luggageCount   │         │
│  │ - version        │         │ - ride           │         │
│  │──────────────────│         │ - version        │         │
│  │ + canAccommodate │         │──────────────────│         │
│  │ + addPassenger   │         │ + getDirectDist  │         │
│  │ + removePassenger│         │                  │         │
│  └────────┬─────────┘         └────────┬─────────┘         │
│           │                            │                    │
│           │ embeds                     │ embeds            │
│           ▼                            ▼                    │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │   RideStatus     │         │    Location      │         │
│  │──────────────────│         │──────────────────│         │
│  │ PENDING          │         │ - latitude       │         │
│  │ MATCHED          │         │ - longitude      │         │
│  │ IN_PROGRESS      │         │──────────────────│         │
│  │ COMPLETED        │         │ + distanceTo()   │         │
│  │ CANCELLED        │         │ + travelTimeTo() │         │
│  └──────────────────┘         └──────────────────┘         │
└────────────────────────────────────────────────────────────┘
```

---

## Database Schema

### Entity-Relationship Diagram

```
┌─────────────────────────┐
│        rides            │
├─────────────────────────┤
│ PK  id                  │
│     status              │
│     max_seats           │
│     available_seats     │
│     max_luggage         │
│     available_luggage   │
│     total_distance      │
│     base_fare           │
│     final_fare          │
│     created_at          │
│     scheduled_time      │
│     version             │
└────────────┬────────────┘
             │
             │ 1:N
             │
             ▼
┌─────────────────────────┐
│      passengers         │
├─────────────────────────┤
│ PK  id                  │
│     name                │
│     phone               │
│     pickup_lat          │
│     pickup_lon          │
│     dropoff_lat         │
│     dropoff_lon         │
│     luggage_count       │
│     max_detour_minutes  │
│ FK  ride_id             │◄─────┐
│     requested_at        │      │
│     version             │      │
└─────────────────────────┘      │
                                 │
                        Foreign Key Relationship
```

### Indexes Strategy

```
rides table:
├─ PRIMARY KEY (id)                    [Clustered Index]
├─ INDEX idx_ride_status (status)      [B-tree]
└─ INDEX idx_ride_created_at           [B-tree]
   (created_at DESC)

passengers table:
├─ PRIMARY KEY (id)                    [Clustered Index]
├─ FOREIGN KEY idx_passenger_ride      [B-tree]
│  (ride_id)
└─ SPATIAL INDEX idx_pickup_location   [R-tree - Future]
   (pickup_lat, pickup_lon)
```

---

## Request Flow Diagrams

### Request Ride Flow (With Matching)

```
┌──────┐                                    ┌──────────┐
│Client│                                    │  Server  │
└───┬──┘                                    └─────┬────┘
    │                                             │
    │ POST /api/rides/request                    │
    │ {passenger details}                        │
    ├────────────────────────────────────────────►
    │                                             │
    │                             ┌───────────────▼──────────────┐
    │                             │ 1. Validate Request          │
    │                             │    - Check required fields   │
    │                             │    - Validate locations      │
    │                             └───────────────┬──────────────┘
    │                                             │
    │                             ┌───────────────▼──────────────┐
    │                             │ 2. Create Passenger Entity   │
    │                             └───────────────┬──────────────┘
    │                                             │
    │                             ┌───────────────▼──────────────┐
    │                             │ 3. Find Matching Ride        │
    │                             │    MatchingService.find()    │
    │                             │    - Query active rides      │
    │                             │    - Check constraints       │
    │                             │    - Calculate detours       │
    │                             └───────────────┬──────────────┘
    │                                             │
    │                                      ┌──────┴──────┐
    │                                      │             │
    │                         Match Found? │      No     │ Match Not Found
    │                              YES     │             │
    │                                      │             │
    │              ┌───────────────────────▼┐           ┌▼──────────────────┐
    │              │ 4a. Add to Existing    │           │ 4b. Create New    │
    │              │     Ride               │           │     Ride          │
    │              │  - Add passenger       │           │  - Init ride      │
    │              │  - Update capacity     │           │  - Add passenger  │
    │              │  - Recalc pricing      │           │  - Calc pricing   │
    │              │  - Save (Opt Lock)     │           │  - Save           │
    │              └────────────┬───────────┘           └─────────┬─────────┘
    │                           │                                 │
    │                           └───────────┬─────────────────────┘
    │                                       │
    │                       ┌───────────────▼──────────────┐
    │                       │ 5. Build Response DTO        │
    │                       │    - Ride ID                 │
    │                       │    - Passenger ID            │
    │                       │    - Fare                    │
    │                       │    - Savings %               │
    │                       └───────────────┬──────────────┘
    │                                       │
    │ 200 OK                                │
    │ {response}                            │
    ◄────────────────────────────────────────
    │
```

### Concurrent Booking Scenario (Optimistic Locking)

```
Thread 1                    Database                Thread 2
   │                           │                        │
   │ 1. Read Ride (v=1)        │                        │
   ├──────────────────────────►│                        │
   │ ◄──────────────────────────┤                        │
   │ (availableSeats=1)        │                        │
   │                           │   2. Read Ride (v=1)   │
   │                           │◄───────────────────────┤
   │                           │────────────────────────►
   │                           │  (availableSeats=1)    │
   │                           │                        │
   │ 3. Update Ride            │                        │
   │    SET available=0        │                        │
   │    WHERE v=1              │                        │
   ├──────────────────────────►│                        │
   │ ✅ SUCCESS (v=2)          │                        │
   │                           │                        │
   │                           │   4. Update Ride       │
   │                           │      WHERE v=1         │
   │                           │◄───────────────────────┤
   │                           │ ❌ FAIL (v≠1)          │
   │                           │────────────────────────►
   │                           │   OptimisticLockEx     │
   │                           │                        │
   │                           │   5. Retry: Re-read    │
   │                           │◄───────────────────────┤
   │                           │────────────────────────►
   │                           │   (availableSeats=0)   │
   │                           │                        │
   │                           │   6. No match          │
   │                           │      → New ride        │
```

---

## Deployment Architecture

### Development (Current)

```
┌────────────────────────────────────────┐
│        Developer Machine               │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │     Spring Boot Application      │  │
│  │     Embedded Tomcat (8080)       │  │
│  └─────────────┬────────────────────┘  │
│                │                        │
│  ┌─────────────▼────────────────────┐  │
│  │      H2 In-Memory Database       │  │
│  │      (Auto-created on start)     │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
```

### Production (Recommended)

```
┌─────────────────────────────────────────────────────┐
│                   AWS CLOUD                          │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │           Application Load Balancer            │ │
│  │           (Auto-scaling target)                │ │
│  └──────────┬────────────────┬────────────────────┘ │
│             │                │                       │
│  ┌──────────▼────┐  ┌────────▼────────┐             │
│  │  EC2 Instance  │  │  EC2 Instance   │             │
│  │  Spring Boot   │  │  Spring Boot    │             │
│  │  (App Server)  │  │  (App Server)   │             │
│  └────────┬───────┘  └────────┬────────┘             │
│           │                   │                       │
│           └─────────┬─────────┘                       │
│                     │                                 │
│           ┌─────────▼───────────┐                     │
│           │   RDS PostgreSQL    │                     │
│           │   (Multi-AZ)        │                     │
│           └─────────────────────┘                     │
│                                                       │
│  Optional:                                            │
│  ┌─────────────────────┐  ┌──────────────────────┐   │
│  │   ElastiCache       │  │   CloudWatch         │   │
│  │   (Redis - Caching) │  │   (Monitoring/Logs)  │   │
│  └─────────────────────┘  └──────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

---

## Technology Stack Details

```
┌──────────────────────────────────────────┐
│         BACKEND STACK                    │
├──────────────────────────────────────────┤
│ Framework:    Spring Boot 3.2.1          │
│ Language:     Java 17                    │
│ Build:        Maven 3.8+                 │
│ ORM:          Hibernate/JPA              │
│ Database:     H2 (Dev), PostgreSQL (Prod)│
│ API Docs:     SpringDoc OpenAPI          │
│ Validation:   Jakarta Bean Validation    │
│ Testing:      JUnit 5, Mockito           │
│ Logging:      SLF4J + Logback            │
└──────────────────────────────────────────┘
```

---

## Design Patterns Applied

```
┌─────────────────────────────────────────────────┐
│ Pattern              │ Implementation           │
├──────────────────────┼──────────────────────────┤
│ Repository           │ RideRepository,          │
│                      │ PassengerRepository      │
├──────────────────────┼──────────────────────────┤
│ Service Layer        │ RideService,             │
│                      │ MatchingService          │
├──────────────────────┼──────────────────────────┤
│ DTO (Data Transfer)  │ RideRequestDTO,          │
│                      │ RideResponseDTO          │
├──────────────────────┼──────────────────────────┤
│ Strategy             │ MatchingService          │
│                      │ (pluggable algorithms)   │
├──────────────────────┼──────────────────────────┤
│ Singleton            │ Spring Beans (@Service)  │
├──────────────────────┼──────────────────────────┤
│ Factory              │ Entity creation helpers  │
├──────────────────────┼──────────────────────────┤
│ Template Method      │ requestRideWithRetry()   │
└─────────────────────────────────────────────────┘
```

---

## Scalability Considerations

### Vertical Scaling
```
Single Server Capacity:
├─ CPU: 4 cores → 8 cores
├─ RAM: 4GB → 16GB
├─ Handles: ~500 req/s
└─ Limitation: Hardware ceiling
```

### Horizontal Scaling
```
Load Balanced Setup:
├─ 3 App Servers
├─ Each handles: 200 req/s
├─ Total capacity: 600 req/s
├─ Database: Shared (bottleneck)
└─ Solution: Read replicas
```

### Database Sharding (Future)
```
Geographic Sharding:
├─ Shard 1: North India rides
├─ Shard 2: South India rides
├─ Shard 3: West India rides
└─ Reduces contention by 66%
```

---

## Monitoring & Observability

```
┌──────────────────────────────────────┐
│     Metrics to Monitor               │
├──────────────────────────────────────┤
│ • Request latency (p50, p95, p99)    │
│ • Throughput (req/sec)               │
│ • Error rate (4xx, 5xx)              │
│ • Database query time                │
│ • Connection pool usage              │
│ • Active rides count                 │
│ • Matching algorithm performance     │
│ • Optimistic lock conflicts          │
└──────────────────────────────────────┘
```

---

## Security Considerations

```
Current Implementation:
├─ Input Validation (Jakarta Validation)
├─ Exception Handling (Global Handler)
└─ Version Control (Optimistic Locking)

Production Requirements:
├─ Authentication (JWT/OAuth2)
├─ Authorization (Role-based)
├─ HTTPS/TLS
├─ Rate Limiting
├─ SQL Injection Prevention (JPA)
└─ CORS Configuration
```

---

This architecture is designed for:
- ✅ Maintainability (Clean separation of concerns)
- ✅ Scalability (Stateless app servers)
- ✅ Reliability (Optimistic locking, retries)
- ✅ Performance (Indexed queries, caching ready)
- ✅ Testability (Dependency injection, mocking)
