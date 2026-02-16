# Sample Test Data

This file contains realistic test scenarios for the ride pooling system using locations around Delhi's Indira Gandhi International Airport.

## Test Locations

### Airport
```json
{
  "name": "IGI Airport Terminal 3",
  "latitude": 28.5562,
  "longitude": 77.1000
}
```

### Destinations

#### Downtown Delhi
```json
{
  "name": "Connaught Place",
  "latitude": 28.7041,
  "longitude": 77.1025
}
```

#### Gurgaon
```json
{
  "name": "Cyber City, Gurgaon",
  "latitude": 28.4595,
  "longitude": 77.0266
}
```

#### Noida
```json
{
  "name": "Noida Sector 18",
  "latitude": 28.5355,
  "longitude": 77.3910
}
```

#### South Delhi
```json
{
  "name": "Hauz Khas",
  "latitude": 28.5494,
  "longitude": 77.2001
}
```

---

## Test Scenarios

### Scenario 1: Successful Ride Pooling (Same Destination)

**Passenger 1 (Alice):**
```bash
curl -X POST http://localhost:8080/api/rides/request \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Kumar",
    "phone": "9876543210",
    "pickupLocation": {
      "latitude": 28.5562,
      "longitude": 77.1000
    },
    "dropoffLocation": {
      "latitude": 28.7041,
      "longitude": 77.1025
    },
    "luggageCount": 1,
    "maxDetourMinutes": 20
  }'
```

**Expected:** New ride created, status = PENDING

**Passenger 2 (Bob) - Should Match:**
```bash
curl -X POST http://localhost:8080/api/rides/request \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Sharma",
    "phone": "9876543211",
    "pickupLocation": {
      "latitude": 28.5570,
      "longitude": 77.1010
    },
    "dropoffLocation": {
      "latitude": 28.7050,
      "longitude": 77.1030
    },
    "luggageCount": 1,
    "maxDetourMinutes": 15
  }'
```

**Expected:** Matched with Alice's ride, status = MATCHED, isPooled = true

---

### Scenario 2: No Match (Different Destination)

**Passenger 1 (Carol):**
```bash
curl -X POST http://localhost:8080/api/rides/request \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Carol Singh",
    "phone": "9876543212",
    "pickupLocation": {
      "latitude": 28.5562,
      "longitude": 77.1000
    },
    "dropoffLocation": {
      "latitude": 28.7041,
      "longitude": 77.1025
    },
    "luggageCount": 2,
    "maxDetourMinutes": 15
  }'
```

**Passenger 2 (David) - Should NOT Match:**
```bash
curl -X POST http://localhost:8080/api/rides/request \
  -H "Content-Type: application/json" \
  -d '{
    "name": "David Patel",
    "phone": "9876543213",
    "pickupLocation": {
      "latitude": 28.5565,
      "longitude": 77.1005
    },
    "dropoffLocation": {
      "latitude": 28.4595,
      "longitude": 77.0266
    },
    "luggageCount": 1,
    "maxDetourMinutes": 10
  }'
```

**Expected:** New ride created (different destination = Gurgaon)

---

### Scenario 3: Capacity Constraint (Luggage)

**Passenger 1 (Eve):**
```bash
curl -X POST http://localhost:8080/api/rides/request \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Eve Verma",
    "phone": "9876543214",
    "pickupLocation": {
      "latitude": 28.5562,
      "longitude": 77.1000
    },
    "dropoffLocation": {
      "latitude": 28.7041,
      "longitude": 77.1025
    },
    "luggageCount": 4,
    "maxDetourMinutes": 20
  }'
```

**Passenger 2 (Frank) - Should NOT Match:**
```bash
curl -X POST http://localhost:8080/api/rides/request \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Frank Reddy",
    "phone": "9876543215",
    "pickupLocation": {
      "latitude": 28.5570,
      "longitude": 77.1010
    },
    "dropoffLocation": {
      "latitude": 28.7050,
      "longitude": 77.1030
    },
    "luggageCount": 3,
    "maxDetourMinutes": 15
  }'
```

**Expected:** New ride (Eve has 4 bags, Frank has 3 = 7 total > 6 max)

---

### Scenario 4: Get Ride Details

```bash
# Get details of ride ID 1
curl -X GET http://localhost:8080/api/rides/1
```

**Expected:**
```json
{
  "rideId": 1,
  "status": "MATCHED",
  "passengers": [
    {
      "id": 1,
      "name": "Alice Kumar",
      "pickupLocation": {"latitude": 28.5562, "longitude": 77.1000},
      "dropoffLocation": {"latitude": 28.7041, "longitude": 77.1025},
      "luggageCount": 1
    },
    {
      "id": 2,
      "name": "Bob Sharma",
      "pickupLocation": {"latitude": 28.5570, "longitude": 77.1010},
      "dropoffLocation": {"latitude": 28.7050, "longitude": 77.1030},
      "luggageCount": 1
    }
  ],
  "availableSeats": 2,
  "availableLuggage": 4,
  "totalDistance": 18.5,
  "finalFare": 34.12
}
```

---

### Scenario 5: Cancel Ride

```bash
# Cancel passenger ID 2 (Bob)
curl -X DELETE http://localhost:8080/api/rides/passenger/2
```

**Expected:**
```json
{
  "message": "Ride cancelled successfully",
  "passengerId": "2"
}
```

**After cancellation, check ride 1:**
```bash
curl -X GET http://localhost:8080/api/rides/1
```

**Expected:** Only Alice remains, availableSeats = 3, fare recalculated

---

### Scenario 6: Surge Pricing Test

**Step 1: Create 50 pending rides (simulate high demand)**
```bash
for i in {1..50}
do
  curl -X POST http://localhost:8080/api/rides/request \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"Passenger $i\",
      \"phone\": \"987654${i}\",
      \"pickupLocation\": {\"latitude\": 28.5562, \"longitude\": 77.1000},
      \"dropoffLocation\": {\"latitude\": 28.7041, \"longitude\": 77.1025},
      \"luggageCount\": 1,
      \"maxDetourMinutes\": 15
    }"
done
```

**Step 2: Check surge multiplier**
```bash
curl -X GET http://localhost:8080/api/rides/pricing/surge
```

**Expected:**
```json
{
  "activeRides": 50,
  "multiplier": 1.25,
  "isSurging": true
}
```

---

## Postman Collection

Import this JSON into Postman for easy testing:

```json
{
  "info": {
    "name": "Ride Pooling API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Request Ride - Alice",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Alice Kumar\",\n  \"phone\": \"9876543210\",\n  \"pickupLocation\": {\n    \"latitude\": 28.5562,\n    \"longitude\": 77.1000\n  },\n  \"dropoffLocation\": {\n    \"latitude\": 28.7041,\n    \"longitude\": 77.1025\n  },\n  \"luggageCount\": 1,\n  \"maxDetourMinutes\": 20\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "http://localhost:8080/api/rides/request",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "rides", "request"]
        }
      }
    },
    {
      "name": "Get Ride Details",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/rides/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "rides", "1"]
        }
      }
    },
    {
      "name": "Cancel Ride",
      "request": {
        "method": "DELETE",
        "url": {
          "raw": "http://localhost:8080/api/rides/passenger/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "rides", "passenger", "1"]
        }
      }
    },
    {
      "name": "Get Surge Info",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/rides/pricing/surge",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "rides", "pricing", "surge"]
        }
      }
    }
  ]
}
```

---

## Database Console Access

To view the database directly:

1. Go to: http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:ridepooling`
3. Username: `sa`
4. Password: (leave empty)

**Useful Queries:**
```sql
-- View all rides
SELECT * FROM rides;

-- View all passengers
SELECT * FROM passengers;

-- View pooled rides
SELECT r.id, r.status, COUNT(p.id) as passenger_count
FROM rides r
LEFT JOIN passengers p ON r.id = p.ride_id
GROUP BY r.id
HAVING passenger_count > 1;

-- View active rides with capacity
SELECT id, status, available_seats, available_luggage, created_at
FROM rides
WHERE status = 'PENDING' OR status = 'MATCHED';
```

---

## Performance Testing

### Using Apache Bench (ab)

**Test 1: Simple load (10 concurrent, 100 requests)**
```bash
ab -n 100 -c 10 -p test-payload.json -T application/json http://localhost:8080/api/rides/request
```

**test-payload.json:**
```json
{
  "name": "Test User",
  "phone": "9999999999",
  "pickupLocation": {"latitude": 28.5562, "longitude": 77.1000},
  "dropoffLocation": {"latitude": 28.7041, "longitude": 77.1025},
  "luggageCount": 1,
  "maxDetourMinutes": 15
}
```

**Expected Metrics:**
- Requests per second: > 100
- Average latency: < 300ms
- 99th percentile: < 500ms

---

## Expected Results Summary

| Scenario | Passengers | Expected Outcome |
|----------|-----------|------------------|
| 1 | Alice + Bob (similar route) | ✅ Matched, pooled = true |
| 2 | Carol + David (diff dest) | ❌ Separate rides |
| 3 | Eve + Frank (luggage) | ❌ Separate rides (capacity) |
| 4 | Get ride details | ✅ Returns all passengers |
| 5 | Cancel Bob | ✅ Ride rebalanced |
| 6 | High demand | ✅ Surge active (1.25x) |
