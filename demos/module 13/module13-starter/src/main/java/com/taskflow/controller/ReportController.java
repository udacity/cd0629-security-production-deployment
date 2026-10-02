package com.taskflow.controller;

import com.taskflow.model.Task;
import com.taskflow.service.TaskService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DELIBERATELY VULNERABLE — Module 13's starter state.
 *
 * TaskFlow is otherwise a pure JSON API, but XSS only matters where HTML
 * actually gets rendered. This endpoint exists specifically to give that
 * mitigation something real to fix: a server-rendered report page,
 * exactly the kind of internal admin view real teams build.
 */
@RestController
public class ReportController {

    private final TaskService taskService;

    public ReportController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping(value = "/api/v1/tasks/report", produces = MediaType.TEXT_HTML_VALUE)
    public String report() {
        StringBuilder html = new StringBuilder("<html><body><h1>Task Report</h1><ul>");
        for (Task task : taskService.listTasks()) {
            // Vulnerable: the task title goes straight into the HTML,
            // unescaped. A title containing a real <script> tag executes.
            html.append("<li>").append(task.getTitle()).append("</li>");
        }
        html.append("</ul></body></html>");
        return html.toString();
    }
}
