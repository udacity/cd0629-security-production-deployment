package com.taskflow.service;

import com.taskflow.model.Task;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskService {

    private final List<Task> tasks = new CopyOnWriteArrayList<>();

    public Task createTask(String title, String owner) {
        Task task = new Task(title, owner);
        tasks.add(task);
        return task;
    }

    public List<Task> listTasks() {
        return tasks;
    }

    public Optional<Task> findById(Long id) {
        return tasks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    /**
     * Module 7: only the task's own owner can mark it complete, regardless
     * of role. #task refers to this method's parameter by name — this
     * project's pom.xml enables the -parameters compiler flag specifically
     * so this resolves reliably, without depending on IDE-specific debug
     * settings.
     */
    @PreAuthorize("#task.owner == authentication.name")
    public Task completeTask(Task task) {
        task.setCompleted(true);
        return task;
    }

    /**
     * Deliberately calls completeTask(...) directly, this == the same
     * object, not the Spring-managed proxy, so the @PreAuthorize check
     * above never runs. This method exists purely to demonstrate that
     * gotcha on camera.
     */
    public Task completeTaskInternally(Task task) {
        return completeTask(task);
    }

    public void deleteTask(Long id) {
        tasks.removeIf(t -> t.getId().equals(id));
    }
}
