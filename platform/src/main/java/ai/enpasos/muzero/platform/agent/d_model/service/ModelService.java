package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.TrainingTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.*;


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

        while (task.isNotDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeInitialInferenceTask(task);
        return CompletableFuture.completedFuture(task.getNetworkOutput());
    }

    @Async()
    public CompletableFuture<List<NetworkIO>> initialInference(List<Game> games) {

        List<InitialInferenceTask> tasks = new ArrayList<>();
        games.forEach(game -> tasks.add(new InitialInferenceTask(game)));
        modelQueue.addInitialInferenceTasks(tasks);

        while (tasks.stream().anyMatch(InitialInferenceTask::isNotDone)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        tasks.forEach(task -> modelQueue.removeInitialInferenceTask(task));
        List<NetworkIO> results = tasks.stream().map(InitialInferenceTask::getNetworkOutput).collect(Collectors.toList());
        return CompletableFuture.completedFuture(results);
    }

    @Async()
    public CompletableFuture<List<NetworkIO>> initialInference(List<Game> games, int startEpoch, int endEpoch) {

        modelQueue.addControllerTask(
                ControllerTask.builder()
                        .taskType(ControllerTaskType.START_MULTI_MODEL)
                        .startEpoch(startEpoch)
                        .lastEpoch(endEpoch)
                        .build());

        List<InitialInferenceTask> tasks = new ArrayList<>();
        Map<Integer, List<InitialInferenceTask>> map = new TreeMap<>();

        for (int g = 0; g < games.size(); g++) {
            Game game = games.get(g);
            List<InitialInferenceTask> tasks2 = new ArrayList<>();
            map.put(g, tasks2);
            for (int epoch = startEpoch; epoch <= endEpoch; epoch++) {
                InitialInferenceTask task = new InitialInferenceTask(game, epoch);
                tasks.add(task);
                tasks2.add(task);
            }
        }

        modelQueue.addInitialInferenceTasks(tasks);

        while (tasks.stream().anyMatch(InitialInferenceTask::isNotDone)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        tasks.forEach(task -> modelQueue.removeInitialInferenceTask(task));

        modelQueue.addControllerTask(
                ControllerTask.builder()
                        .taskType(ControllerTaskType.STOP_MULTI_MODEL)
                        .build());


        List<NetworkIO> results = map.keySet().stream().map(g -> {
            List<InitialInferenceTask> tasks2 = map.get(g);
            int c = tasks2.size();
            double valueMean = tasks2.stream().mapToDouble(t -> t.getNetworkOutput().getValue()).sum() / c;
            double valueVariance = tasks2.stream().mapToDouble(t -> {
                double v = t.getNetworkOutput().getValue() - valueMean;
                return v * v;
            }).sum() / c;
            int n = config.getActionSpaceSize();
            float[] p = new float[n];
            IntStream.range(0, c).forEach(i -> {
                NetworkIO io = tasks2.get(i).getNetworkOutput();
                IntStream.range(0, n).forEach(a -> {
                    p[a] += io.getPolicyValues()[a];
                });
            });
            IntStream.range(0, n).forEach(a -> {
                p[a] /= n;
            });

            NetworkIO io = tasks2.get(c - 1).getNetworkOutput();
            io.setValue(valueMean);
            io.setValueStd(Math.sqrt(valueVariance));
            io.setNumModels(c);
            io.setPolicyValues(p);
            io.setLogits(toFloat(ln(toDouble(p))));
            IntStream.range(0, c).forEach(i -> {
                        NetworkIO ioLocal = tasks2.get(i).getNetworkOutput();
                        io.getMapHiddenStateToEpoch().put(ioLocal.getEpoch(), ioLocal.getHiddenState());
                    }
            );
            return io;
        }).collect(Collectors.toList());


            return CompletableFuture.completedFuture(results);
    }


    @Async()
    public CompletableFuture<Void> getEpoch() {
        ControllerTask task = new ControllerTask(ControllerTaskType.GET_EPOCH);
        modelQueue.addControllerTask(task);
        while (task.isNotDone()) {
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
        while (task.isNotDone()) {
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
                .action(config.newAction(action))
                .build();
        List<Node> searchPath = List.of(node, node2);

        RecurrentInferenceTask task = new RecurrentInferenceTask(searchPath);
        modelQueue.addRecurrentInferenceTask(task);

        while (task.isNotDone()) {
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

        while (task.isNotDone()) {
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
    public CompletableFuture<NetworkIO> recurrentInference(List<Node> searchPath, int startEpoch, int endEpoch) {

        modelQueue.addControllerTask(
                ControllerTask.builder()
                        .taskType(ControllerTaskType.START_MULTI_MODEL)
                        .startEpoch(startEpoch)
                        .lastEpoch(endEpoch)
                        .build());

        List<RecurrentInferenceTask> tasks = new ArrayList<>();

        for (int epoch = startEpoch; epoch <= endEpoch; epoch++) {
            RecurrentInferenceTask task = new RecurrentInferenceTask(searchPath, epoch);
            tasks.add(task);
            modelQueue.addRecurrentInferenceTask(task);
        }


        while (tasks.stream().anyMatch(RecurrentInferenceTask::isNotDone)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        tasks.forEach(task -> modelQueue.removeRecurrentInferenceTask(task));

        modelQueue.addControllerTask(
                ControllerTask.builder()
                        .taskType(ControllerTaskType.STOP_MULTI_MODEL)
                        .build());


        int c = tasks.size();
        double valueMean = tasks.stream().mapToDouble(t -> t.getNetworkOutput().getValue()).sum() / c;
        double valueVariance = tasks.stream().mapToDouble(t -> {
            double v = t.getNetworkOutput().getValue() - valueMean;
            return v * v;
        }).sum() / c;
        int n = config.getActionSpaceSize();
        float[] p = new float[n];
        IntStream.range(0, c).forEach(i -> {
            NetworkIO io = tasks.get(i).getNetworkOutput();
            IntStream.range(0, n).forEach(a -> {
                p[a] += io.getPolicyValues()[a];
            });
        });
        IntStream.range(0, n).forEach(a -> {
            p[a] /= n;
        });

        NetworkIO io = tasks.get(c - 1).getNetworkOutput();
        io.setValue(valueMean);
        io.setValueStd(Math.sqrt(valueVariance));
        io.setNumModels(c);
        io.setPolicyValues(p);
        io.setLogits(toFloat(ln(toDouble(p))));
        IntStream.range(0, c).forEach(i -> {
                    NetworkIO ioLocal = tasks.get(i).getNetworkOutput();
                    io.getMapHiddenStateToEpoch().put(ioLocal.getEpoch(), ioLocal.getHiddenState());
                }
        );
        return CompletableFuture.completedFuture(io);
    }


    @Async()
    public CompletableFuture<Void> trainModel(TrainingTypeKey trainingTypeKey) {
        ControllerTask task = new ControllerTask(ControllerTaskType.TRAIN_MODEL_POLICY_VALUE);
        if (trainingTypeKey == TrainingTypeKey.RULES) {
            task = new ControllerTask(ControllerTaskType.TRAIN_MODEL_RULES);
        }
        return handleControllerTask(task);
    }


    public void startScope() {
        ControllerTask task = new ControllerTask(ControllerTaskType.START_SCOPE);
        handleControllerTask(task).join();
    }


    public void endScope() {
        ControllerTask task = new ControllerTask(ControllerTaskType.END_SCOPE);
        handleControllerTask(task).join();
    }
}
