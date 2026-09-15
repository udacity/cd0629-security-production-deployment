package com.taskflow.controller;

import com.taskflow.model.Task;
import com.taskflow.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * GET   /api/v1/tasks              -> open to everyone
 * POST  /api/v1/tasks              -> requires authentication (Module 3)
 * DELETE /api/v1/tasks/{id}        -> requires ROLE_MANAGER or higher (Module 7)
 * PATCH /api/v1/tasks/{id}/complete -> requires authentication at the URL
 *                                      level, ownership enforced inside
 *                                      TaskService (Module 7)
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> listTasks() {
        return taskService.listTasks();
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Map<String, String> body,
                                            Authentication authentication) {
        String title = body.getOrDefault("title", "Untitled task");
        Task task = taskService.createTask(title, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Task> completeTask(@PathVariable Long id) {
        Task task = taskService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No task with id " + id));
        return ResponseEntity.ok(taskService.completeTask(task));
    }
}
