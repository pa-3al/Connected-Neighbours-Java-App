package com.app.infrastructure.sync;

import com.app.domain.model.Media;

public record MediaConflict(Media local, Media server) {}