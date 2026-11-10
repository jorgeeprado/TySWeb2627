package edu.uclm.esi.tysweb.bealquileres.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureDto(PropertiesDto properties, String id, GeometryDto geometry) {
}   