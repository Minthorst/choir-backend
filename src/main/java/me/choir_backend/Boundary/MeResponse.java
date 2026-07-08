package me.choir_backend.Boundary;

import java.util.List;

public record MeResponse(boolean authenticated, List<String> roles) {}