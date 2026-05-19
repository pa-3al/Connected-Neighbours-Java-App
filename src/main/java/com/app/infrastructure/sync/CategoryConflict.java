package com.app.infrastructure.sync;

import com.app.domain.model.Category;

public record CategoryConflict(Category local, Category server) {}