package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ModelService {

    public static final String INTERRUPTED = "Interrupted!";
    @Autowired
    ModelQueue modelQueue;

    @Autowired
    ModelController modelController;

    @Autowired
    MuZeroConfig config;

    @Async()
    public CompletableFuture<NetworkIO> initialInference(Game game) {
        InitialInferenceTask task = new InitialInferenceTask(game);
        modelQueue.addInitialInferenceTask(task);

        while (task.isDone()) {
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

        while (tasks.stream().anyMatch(InitialInferenceTask::isDone)) {
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
    public CompletableFuture<Void> getEpoch() {
        ControllerTask task = new ControllerTask(ControllerTaskType.GET_EPOCH);
        modelQueue.addControllerTask(task);
        while (task.isDone()) {
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
        ControllerTask task = new ControllerTask(ControllerTaskType.LOAD_LATEST_MODEL_OR_CREATE_IF_NOT_EXISTING);
        return handleControllerTask(task);
    }


    public void shutdown() {
        try {
            modelController.destroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Async()
    public CompletableFuture<Void> loadLatestModel() {
        ControllerTask task = new ControllerTask(ControllerTaskType.LOAD_LATEST_MODEL);
        return handleControllerTask(task);
    }
    @Async()
    public CompletableFuture<Void> loadLatestModelOrCreateIfNotExisting(int epoch) {
        ControllerTask task = new ControllerTask(ControllerTaskType.LOAD_LATEST_MODEL_OR_CREATE_IF_NOT_EXISTING);
        task.epoch = epoch;
        return handleControllerTask(task);
    }

    @Async()
    public CompletableFuture<Void> loadLatestModel(int epoch) {
        ControllerTask task = new ControllerTask(ControllerTaskType.LOAD_LATEST_MODEL);
        task.epoch = epoch;
        return handleControllerTask(task);
    }
    @NotNull
    private CompletableFuture<Void> handleControllerTask(ControllerTask task) {
        modelQueue.addControllerTask(task);
        while (task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeControllerTask(task);
        return CompletableFuture.completedFuture(null);
    }




    @Async()
    public CompletableFuture<NetworkIO> recurrentInference(NDArray hiddenState, int action) {
            Node node = Node.builder()
        .hiddenState(hiddenState)
        .build();
        Node node2 = Node.builder()
            .action(config.newAction(4))
            .build();
         List<Node> searchPath = List.of(node, node2);

        RecurrentInferenceTask task = new RecurrentInferenceTask(searchPath);
        modelQueue.addRecurrentInferenceTask(task);

        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeRecurrentInferenceTask(task);
        return CompletableFuture.completedFuture(task.getNetworkOutput());
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
        return CompletableFuture.completedFuture(task.getNetworkOutput());
    }

    @Async()
    public CompletableFuture<Void> trainModel() {
        ControllerTask task = new ControllerTask(ControllerTaskType.TRAIN_MODEL);
        return handleControllerTask(task);
    }


    public void startScope() {
        ControllerTask task = new ControllerTask(ControllerTaskType.START_SCOPE);
          handleControllerTask(task).join();
    }


    public void  endScope() {
        ControllerTask task = new ControllerTask(ControllerTaskType.END_SCOPE);
         handleControllerTask(task).join();
    }
}
