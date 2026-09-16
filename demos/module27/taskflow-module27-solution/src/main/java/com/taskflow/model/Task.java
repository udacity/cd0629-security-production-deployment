package com.taskflow.model;

import java.util.concurrent.atomic.AtomicLong;

public class Task {

    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    private Long id;
    private String title;
    private String owner;
    private boolean completed;

    // Needed for JSON deserialization of incoming request bodies.
    public Task() {
    }

    public Task(String title, String owner) {
        this.id = SEQUENCE.getAndIncrement();
        this.title = title;
        this.owner = owner;
        this.completed = false;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}

