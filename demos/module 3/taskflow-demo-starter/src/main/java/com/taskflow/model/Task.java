package com.taskflow.model;

import java.util.concurrent.atomic.AtomicLong;

public class Task {

    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    private Long id;
    private String title;

    // Needed for JSON deserialization of incoming request bodies.
    public Task() {
    }

    public Task(String title) {
        this.id = SEQUENCE.getAndIncrement();
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
