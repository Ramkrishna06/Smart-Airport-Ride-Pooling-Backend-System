package com.airport.ridepooling;

import com.airport.ridepooling.dto.RideRequestDTO;
import com.airport.ridepooling.model.Location;
import com.airport.ridepooling.model.RideStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Ride API endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RideControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Test successful ride request creation
     */
    @Test
    public void testRequestRide_Success() throws Exception {
        RideRequestDTO request = new RideRequestDTO(
            "John Doe",
            "1234567890",
            new Location(28.5562, 77.1000), // Delhi Airport
            new Location(28.7041, 77.1025), // Connaught Place
            2,
            15
        );
        
        mockMvc.perform(post("/api/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").exists())
                .andExpect(jsonPath("$.passengerId").exists())
                .andExpect(jsonPath("$.status").value(RideStatus.PENDING.name()))
                .andExpect(jsonPath("$.estimatedFare").isNumber())
                .andExpect(jsonPath("$.isPooled").value(false));
    }
    
    /**
     * Test ride pooling - second passenger matches with first
     */
    @Test
    public void testRidePooling_SuccessfulMatch() throws Exception {
        // First passenger
        RideRequestDTO request1 = new RideRequestDTO(
            "Alice",
            "1111111111",
            new Location(28.5562, 77.1000),
            new Location(28.7041, 77.1025),
            1,
            20
        );
        
        mockMvc.perform(post("/api/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        
        // Second passenger with similar route
        RideRequestDTO request2 = new RideRequestDTO(
            "Bob",
            "2222222222",
            new Location(28.5570, 77.1010), // Very close pickup
            new Location(28.7050, 77.1030), // Similar destination
            1,
            20
        );
        
        mockMvc.perform(post("/api/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(RideStatus.MATCHED.name()))
                .andExpect(jsonPath("$.isPooled").value(true))
                .andExpect(jsonPath("$.savingsPercentage").isNumber());
    }
    
    /**
     * Test validation - missing required fields
     */
    @Test
    public void testRequestRide_ValidationError() throws Exception {
        RideRequestDTO invalidRequest = new RideRequestDTO(
            null, // Missing name
            "1234567890",
            new Location(28.5562, 77.1000),
            new Location(28.7041, 77.1025),
            1,
            15
        );
        
        mockMvc.perform(post("/api/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * Test get ride details
     */
    @Test
    public void testGetRide_Success() throws Exception {
        // Create a ride first
        RideRequestDTO request = new RideRequestDTO(
            "Charlie",
            "3333333333",
            new Location(28.5562, 77.1000),
            new Location(28.7041, 77.1025),
            1,
            15
        );
        
        String response = mockMvc.perform(post("/api/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract ride ID from response
        Long rideId = objectMapper.readTree(response).get("rideId").asLong();
        
        // Get ride details
        mockMvc.perform(get("/api/rides/" + rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(rideId))
                .andExpect(jsonPath("$.passengers").isArray())
                .andExpect(jsonPath("$.passengers[0].name").value("Charlie"));
    }
    
    /**
     * Test ride cancellation
     */
    @Test
    public void testCancelRide_Success() throws Exception {
        // Create a ride
        RideRequestDTO request = new RideRequestDTO(
            "David",
            "4444444444",
            new Location(28.5562, 77.1000),
            new Location(28.7041, 77.1025),
            1,
            15
        );
        
        String response = mockMvc.perform(post("/api/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        Long passengerId = objectMapper.readTree(response).get("passengerId").asLong();
        
        // Cancel the ride
        mockMvc.perform(delete("/api/rides/passenger/" + passengerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ride cancelled successfully"));
    }
    
    /**
     * Test health check endpoint
     */
    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/rides/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
