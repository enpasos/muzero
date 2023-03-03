package ai.enpasos.muzero.platform.agent.c_model.service;

import ai.enpasos.muzero.platform.agent.c_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.b_planning.Node;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Component
@Slf4j
public class ModelService {

    public static final String INTERRUPTED = "Interrupted!";
    @Autowired
    ModelQueue modelQueue;

    @Autowired
    EnableSchedulingConfig enableSchedulingConfig;

    @Async()
    public CompletableFuture<NetworkIO> initialInference(Game game) {
        InitialInferenceTask task = new InitialInferenceTask(game);
        modelQueue.addInitialInferenceTask(task);

        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // log.error("ModelServices has been stopped.");
                Thread.currentThread().interrupt();
            }
        }
    //    game.getGameDTO().setTrainingEpoch(task.getNetworkOutput().getEpoch());
        modelQueue.removeInitialInferenceTask(task);
        return CompletableFuture.completedFuture(task.getNetworkOutput());
    }

    @Async()
    public CompletableFuture<List<NetworkIO>> initialInference(List<Game> games) {

        List<InitialInferenceTask> tasks = new ArrayList<>();
        games.forEach(game -> tasks.add(new InitialInferenceTask(game)));
        modelQueue.addInitialInferenceTasks(tasks);

        while (tasks.stream().anyMatch(task -> !task.isDone())) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
               // log.error("ModelServices has been stopped.");
                Thread.currentThread().interrupt();
            }
        }
        tasks.forEach(task -> modelQueue.removeInitialInferenceTask(task));
        List<NetworkIO> results = tasks.stream().map(InitialInferenceTask::getNetworkOutput).collect(Collectors.toList());
   //     IntStream.range(0, games.size()).forEach(i -> games.get(i).getGameDTO().setTrainingEpoch(results.get(i).getEpoch()));
        return CompletableFuture.completedFuture(results);
    }


    @Async()
    public CompletableFuture<Void> loadLatestModel() {
        ControllerTask task = new ControllerTask(ControllerTaskType.loadLatestModel);
        return handleControllerTask(task);
    }

    @Async()
    public CompletableFuture<Void> getEpoch() {
        ControllerTask task = new ControllerTask(ControllerTaskType.getEpoch);
        modelQueue.addControllerTask(task);
        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
              //  log.error("ModelServices has been stopped.");
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeControllerTask(task);
        return CompletableFuture.completedFuture(null);
    }

    @Async()
    public CompletableFuture<Void> loadLatestModelOrCreateIfNotExisting() {
        ControllerTask task = new ControllerTask(ControllerTaskType.loadLatestModelOrCreateIfNotExisting);
        return handleControllerTask(task);
    }

    @NotNull
    private CompletableFuture<Void> handleControllerTask(ControllerTask task) {
        modelQueue.addControllerTask(task);
        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
              //  log.error("ModelServices has been stopped.");
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeControllerTask(task);
        return CompletableFuture.completedFuture(null);
    }





    @Async()
    public CompletableFuture<NetworkIO> recurrentInference(List<Node> searchPath) {
        RecurrentInferenceTask task = new RecurrentInferenceTask(searchPath);
        modelQueue.addRecurrentInferenceTask(task);

        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
             //   log.error("ModelServices has been stopped.");
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeRecurrentInferenceTask(task);

    //    throw new MuZeroException("hallo");

        return CompletableFuture.completedFuture(task.getNetworkOutput());
    }

    @Async()
    public CompletableFuture<Void> trainModel() {
        ControllerTask task = new ControllerTask(ControllerTaskType.trainModel);
        return handleControllerTask(task);
    }

    @Async()
    public CompletableFuture<Void> startScope() {
        ControllerTask task = new ControllerTask(ControllerTaskType.startScope);
        return handleControllerTask(task);
    }

    @Async()
    public CompletableFuture<Void> endScope() {
        ControllerTask task = new ControllerTask(ControllerTaskType.endScope);
        return handleControllerTask(task);
    }
}
