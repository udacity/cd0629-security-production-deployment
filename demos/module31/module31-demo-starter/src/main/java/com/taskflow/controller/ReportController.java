package com.taskflow.controller;

import com.taskflow.model.Task;
import com.taskflow.service.TaskService;
import org.owasp.encoder.Encode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Module 13 solution: task titles are now HTML-encoded before they ever
 * touch the response. A title containing a real <script> tag renders as
 * inert, visible text, not executable markup.
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
            html.append("<li>").append(Encode.forHtml(task.getTitle())).append("</li>");
        }
        html.append("</ul></body></html>");
        return html.toString();
    }
}

