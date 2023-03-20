package ai.enpasos.muzero.platform.agent.d_model.service;


import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModelQueue {

    private List<InitialInferenceTask> initialInferenceTasks;
    private List<RecurrentInferenceTask> recurrentInferenceTasks;
    private List<ControllerTask> controllerTasks;


    public long countInitialInferenceTasksNotStarted() {
        synchronized(initialInferenceTasks) {
         //   return initialInferenceTasks.size();
            return initialInferenceTasks.stream()
                .filter(InitialInferenceTask::isDone)
                .count();

        }
    }
    public long countRecurrentInferenceTasksNotStarted() {
        synchronized(recurrentInferenceTasks) {
           // return recurrentInferenceTasks.size();
            return recurrentInferenceTasks.stream()
                .filter(task -> !task.isDone())
                .count();
        }
    }
    public List<InitialInferenceTask> getInitialInferenceTasksNotStarted(int num) {
        synchronized(initialInferenceTasks) {
            return initialInferenceTasks.stream()
                .filter(InitialInferenceTask::isDone)
                .limit(num)  // as we use a list this should be the first once entered in the list (FIFO) - but we should test this
                .collect(Collectors.toList());
        }
    }

    public List<ControllerTask> getControllerTasksNotStarted() {
        synchronized(controllerTasks) {
            return controllerTasks.stream()
                .filter(ControllerTask::isDone)
                .collect(Collectors.toList());
        }
    }

    public List<RecurrentInferenceTask> getRecurrentInferenceTasksNotStarted(int num) {
        synchronized(recurrentInferenceTasks) {
            return recurrentInferenceTasks.stream()
                .filter(task -> !task.isDone())
                .limit(num)  // as we use a list this should be the first once entered in the list (FIFO) - but we should test this
                .collect(Collectors.toList());
        }
    }


    public List<InitialInferenceTask> getInitialInferenceTasks() {
        // being careful with the synchronization
        return List.of(initialInferenceTasks.toArray(new InitialInferenceTask[0]));
    }

    public List<RecurrentInferenceTask> getRecurrentInferenceTasks() {
        // being careful with the synchronization
        return List.of(recurrentInferenceTasks.toArray(new RecurrentInferenceTask[0]));
    }

    @PostConstruct
    public void init() {
        initialInferenceTasks = Collections.synchronizedList(new ArrayList<>());
        recurrentInferenceTasks = Collections.synchronizedList(new ArrayList<>());
        controllerTasks = Collections.synchronizedList(new ArrayList<>());
    }

    public void addControllerTask(ControllerTask task) {
        controllerTasks.add(task);
    }

    public void removeControllerTask(ControllerTask task) {
        controllerTasks.remove(task);
    }

    public void addInitialInferenceTasks(List<InitialInferenceTask> tasks) {
        initialInferenceTasks.addAll(tasks);
    }

    public void addInitialInferenceTask(InitialInferenceTask task) {
        initialInferenceTasks.add(task);
    }

    public void removeInitialInferenceTask(InitialInferenceTask task) {
        initialInferenceTasks.remove(task);
    }

    public void addRecurrentInferenceTask(RecurrentInferenceTask task) {
        recurrentInferenceTasks.add(task);
    }

    public void removeRecurrentInferenceTask(RecurrentInferenceTask task) {
        recurrentInferenceTasks.remove(task);
    }

    public boolean isEmpty() {
        return initialInferenceTasks.isEmpty() && recurrentInferenceTasks.isEmpty();
    }


}
