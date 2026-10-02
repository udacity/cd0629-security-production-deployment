package com.taskflow.controller;

import com.taskflow.model.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GET  /api/v1/tasks  -> open to everyone
 * POST /api/v1/tasks  -> requires authentication (Module 3)
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final List<Task> tasks = new CopyOnWriteArrayList<>();

    @GetMapping
    public List<Task> listTasks() {
        return tasks;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Untitled task");
        Task task = new Task(title);
        tasks.add(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }
}
