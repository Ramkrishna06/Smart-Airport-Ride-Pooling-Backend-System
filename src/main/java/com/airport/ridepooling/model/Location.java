package com.airport.ridepooling.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a geographic location with latitude and longitude
 * Includes Haversine formula for distance calculation
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    
    private Double latitude;
    private Double longitude;
    
    /**
     * Calculate distance to another location using Haversine formula
     * Time Complexity: O(1)
     * @param other The target location
     * @return Distance in kilometers
     */
    public double distanceTo(Location other) {
        if (other == null) {
            return Double.MAX_VALUE;
        }
        
        final double EARTH_RADIUS_KM = 6371.0;
        
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(this.latitude)) * 
                   Math.cos(Math.toRadians(other.latitude)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Calculate estimated travel time between two locations
     * @param other The target location
     * @param averageSpeedKmh Average speed in km/h (default: 30 km/h for city traffic)
     * @return Estimated time in minutes
     */
    public double travelTimeTo(Location other, double averageSpeedKmh) {
        double distanceKm = distanceTo(other);
        return (distanceKm / averageSpeedKmh) * 60; // Convert to minutes
    }
    
    public double travelTimeTo(Location other) {
        return travelTimeTo(other, 30.0); // Default 30 km/h
    }
    
    @Override
    public String toString() {
        return String.format("(%.4f, %.4f)", latitude, longitude);
    }
}
