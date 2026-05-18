package com.app.infrastructure.sync;

import com.app.domain.model.Neighbourhood;

public record NeighbourhoodConflict(Neighbourhood local, Neighbourhood server) {}