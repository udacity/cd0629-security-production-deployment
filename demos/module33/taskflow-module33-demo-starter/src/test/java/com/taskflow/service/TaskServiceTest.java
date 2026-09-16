package com.taskflow.service;

import com.taskflow.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module 29: the first real tests in this entire project. Every module
 * before this one added `spring-boot-starter-test` to pom.xml back in
 * Module 3 and never actually used it — a real gap, worth being honest
 * about, not something to quietly paper over.
 *
 * TaskService specifically has zero external dependencies (no injected
 * beans at all, just an in-memory list), which makes it the cleanest
 * candidate for a plain, fast, Spring-context-free unit test.
 *
 * Note on @PreAuthorize: TaskService.completeTask() carries a
 * @PreAuthorize annotation from Module 7. That annotation only does
 * anything when Spring wraps the real bean in a security-checking
 * proxy — a plain `new TaskService()` here never gets that proxy, so
 * these tests exercise the underlying logic only, not the security
 * enforcement itself. That's expected and fine: proving the
 * authorization rule actually blocks unauthorized users is what the
 * full running app (and Module 7's own manual test) already covers.
 */
class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService();
    }

    @Test
    void createTask_assignsOwnerAndStartsIncomplete() {
        Task task = taskService.createTask("Write tests", "dev1");

        assertEquals("Write tests", task.getTitle());
        assertEquals("dev1", task.getOwner());
        assertFalse(task.isCompleted());
        assertNotNull(task.getId());
    }

    @Test
    void listTasks_returnsEveryCreatedTask() {
        taskService.createTask("First task", "dev1");
        taskService.createTask("Second task", "manager1");

        List<Task> tasks = taskService.listTasks();

        assertEquals(2, tasks.size());
    }

    @Test
    void findById_locatesAnExistingTask() {
        Task created = taskService.createTask("Findable task", "dev1");

        Optional<Task> found = taskService.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("Findable task", found.get().getTitle());
    }

    @Test
    void completeTask_marksTheTaskAsComplete() {
        Task task = taskService.createTask("Task to complete", "dev1");

        taskService.completeTask(task);

        assertTrue(task.isCompleted());
    }
}
