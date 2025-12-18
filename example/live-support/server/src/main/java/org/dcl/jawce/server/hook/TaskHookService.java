package org.dcl.jawce.server.hook;

import lombok.extern.slf4j.Slf4j;
import org.dcl.jawce.server.model.entity.Task;
import org.dcl.jawce.server.repository.TaskRepository;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class TaskHookService {
    private final TaskRepository taskRepository;

    public TaskHookService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Hook newTask(Hook arg) {
        log.info("Create new task hook arg: {}", arg);
        try {
            Task newTask = new Task();
            newTask.setName(arg.getUserInput());
            newTask.setCreatedAt(LocalDateTime.now());

            var task = taskRepository.save(newTask);

            log.info("Created new task: {}", newTask);

        } catch (Exception e) {
            log.error("Failed to create new task: {}", e.getMessage());
        }

        return arg;
    }

    public Hook fetchTasks(Hook arg) {
        log.info("Fetch tasks hook arg: {}", arg);
        StringBuilder message = new StringBuilder();

        try {
            var tasks = taskRepository.findAll();

            if(tasks.isEmpty()) {
                message.append("No tasks found");
            } else {
                message.append("Found tasks (").append(tasks.size()).append("):").append("\\n\\n");
                tasks.forEach(task -> {

                    message.append("Task: ").append(task.getName()).append("\\n")
                            .append("ID: ").append(task.getId()).append("\\n")
                            .append("Created at: ").append(task.getCreatedAt()).append("\\n")
                            .append("Completed?: ").append(task.getCompleted()).append("\\n")
                            .append("_______________________").append("\\n");
                });
            }

            message.append("\\n").append("Type `menu` to go to Menu");
        } catch (Exception e) {
            log.error("Failed to fetch tasks: {}", e.getMessage());
            message.append(e.getMessage());
        }

        arg.setTemplateDynamicBody(
                TemplateDynamicBody
                        .builder()
                        .renderPayload(Map.of("message", message.toString()))
                        .build()
        );

        return arg;
    }
}
