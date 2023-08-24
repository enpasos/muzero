package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.d_model.djl.TrainingConfigFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.TrainingTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.d_model.djl.EpochHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;
import static ai.enpasos.muzero.platform.common.FileUtils.mkDir;
import static java.util.Map.entry;


@Component
@Slf4j
public class ModelController implements DisposableBean, Runnable {

    @Autowired
    ModelQueue modelQueue;

    @Autowired
    MuZeroConfig config;
    NDManager nDManager;
    @Autowired
    BatchFactory batchFactory;

    @Autowired
    TrainingConfigFactory trainingConfigFactory;
    @Autowired
    GameBuffer gameBuffer;
    private Network network;
    @Autowired
    private ModelState modelState;

    private final DurAndMem inferenceDuration = new DurAndMem();

    private NDScope ndScope;

    private volatile boolean running;


    @PostConstruct
    void init() {
        this.running = true;
        Thread thread = new Thread(this, "model");
        thread.start();
    }

    @Override
    public void run() {
        log.info("ModelController started.");
        try {
            while (running) {
                int numParallelInferences = config.getNumParallelInferences();
                controllerTasks();
                if (isMultiModelMode()) {
                    multiModelModeLoop(numParallelInferences);
                } else {
                    initialInferences(numParallelInferences );
                    recurrentInferences(numParallelInferences);
                }
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("ModelController error", e);
        } finally {
            close();
        }
        log.info("ModelController stopped.");
    }

    private void multiModelModeLoop(int numParallelInferences) throws InterruptedException {
      //  while(existsTaskForAnyEpoch()) {
            for (int epoch = this.controllerTask.startEpoch; epoch <= this.controllerTask.lastEpoch; epoch++) {
                loadModelOrCreateIfNotExisting(epoch);
              //  while (existsTaskForEpoch(epoch)) {
                boolean changes;
                do {
                    changes = false;
                    changes |= initialInferences(numParallelInferences, epoch);
                    changes |= recurrentInferences(numParallelInferences, epoch);
                    Thread.sleep(5);
                } while (changes);
              //
              //  }
            }
       // }
    }



    private void close() {
        closeGeneralNDManager();
        closeModel();
    }

    private void closeModel() {
        if (network != null && network.getModel() != null)
            network.getModel().close();
    }
    private void closeGeneralNDManager() {
        if (nDManager != null)
            nDManager.close();

    }

    boolean isMultiModelMode() {
        return controllerTask != null ;
    }

    private ControllerTask controllerTask;


    private void controllerTasks() {

        List<ControllerTask> localControllerTaskList = modelQueue.getControllerTasksNotStarted();

        if (localControllerTaskList.isEmpty()) return;

        for (ControllerTask task : localControllerTaskList) {
            switch (task.getTaskType()) {
                case START_MULTI_MODEL:
                    controllerTask = task;

                    break;
                case STOP_MULTI_MODEL:
                    controllerTask = null;
                    break;
                case LOAD_LATEST_MODEL:
                    closeModel();
                    Model model = Model.newInstance(config.getModelName(), Device.gpu());
                    log.info("loadLatestModel with model name {}", config.getModelName());
                    if (task.epoch == -1) {
                        network = new Network(config, model);
                    } else {
                        network = new Network(config, model, Paths.get(config.getNetworkBaseDir()),
                            Map.ofEntries(entry("epoch", task.epoch + "")));
                    }
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case LOAD_LATEST_MODEL_OR_CREATE_IF_NOT_EXISTING:
                    int epoch = task.epoch;
                    loadModelOrCreateIfNotExisting(epoch);
                    break;
                case TRAIN_MODEL_POLICY_VALUE:
                    trainNetwork(network.getModel(), TrainingTypeKey.POLICY_VALUE);
                    break;
                case TRAIN_MODEL_RULES:
                    trainNetwork(network.getModel(), TrainingTypeKey.RULES);
                    break;
                case START_SCOPE:
                    if (ndScope != null) {
                        ndScope.close();
                    }
                    ndScope = new NDScope();
                    break;
                case END_SCOPE:
                    if (ndScope != null) {
                        ndScope.close();
                    }
                    break;

            }
            task.setDone(true);
        }

    }

    private void loadModelOrCreateIfNotExisting(int epoch) {
        Model model;
        closeModel();
        model = Model.newInstance(config.getModelName(), Device.gpu());
        if (model.getBlock() == null) {
            MuZeroBlock block = new MuZeroBlock(config);
            model.setBlock(block);
            if (epoch == -1) {
                loadModelParametersOrCreateIfNotExisting(model);
            } else {
                loadModelParametersOrCreateIfNotExisting(model, Paths.get(config.getNetworkBaseDir()),
                        Map.ofEntries(entry("epoch", epoch + "")));
            }
        }
        network = new Network(config, model);
        nDManager = network.getNDManager().getParentManager().newSubManager();
        network.initActionSpaceOnDevice(nDManager);
        network.setHiddenStateNDManager(nDManager);
        this.modelState.setEpoch(getEpochFromModel(model));
    }


    private void trainNetwork(Model model, TrainingTypeKey trainingTypeKey) {
        try (NDScope nDScope = new NDScope()) {
//            if (config.offPolicyCorrectionOn()) {
//                determinePRatioMaxForCurrentEpoch();
//            }

            int epochLocal;
            int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
            boolean withSymmetryEnrichment = true;
            epochLocal = getEpochFromModel(model);
            DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epochLocal, trainingTypeKey);
            int finalEpoch = epochLocal;
            djlConfig.getTrainingListeners().stream()
                .filter(MyEpochTrainingListener.class::isInstance)
                .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = batchFactory.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.setMetrics(new Metrics());

                for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                    try (Batch batch = batchFactory.getBatch(trainer.getManager(), withSymmetryEnrichment, trainingTypeKey)) {
                        log.debug("trainBatch " + m);
                        MyEasyTrain.trainBatch(trainer, batch, config.isWithValueStd());
                        trainer.step();
                    }
                }

                // number of action paths

                handleMetrics(trainer, model, epochLocal);

                trainer.notifyListeners(listener -> listener.onEpoch(trainer));
            }
        }
        modelState.setEpoch(getEpochFromModel(model));

    }

//    private void determinePRatioMaxForCurrentEpoch() {
//        int epoch = this.modelState.getEpoch();
//        List<Game> games = this.gameBuffer.getGames().stream()
//            .filter(game -> game.getEpisodeDO().getTrainingEpoch() == epoch && game.isReanalyse() )
//            .collect(Collectors.toList());
//        double pRatioMax = determinePRatioMax(games);
//        log.info("pRatioMaxREANALYSE({}): {}", epoch, pRatioMax);
//
//        List<Game> games2 = this.gameBuffer.getGames().stream()
//            .filter(game -> game.getEpisodeDO().getTrainingEpoch() == epoch && !game.isReanalyse() )
//            .collect(Collectors.toList());
//        double pRatioMax2 = determinePRatioMax(games2);
//        log.info("pRatioMax({}): {}", epoch, pRatioMax2);
//    }

    private double determinePRatioMax(List<Game> games) {
        double pRatioMax = games.stream().mapToDouble(Game::getPRatioMax).max().orElse(1.0);
        games.stream().forEach(game -> game.setPRatioMax(pRatioMax));
        return pRatioMax;
    }

    private void handleMetrics(Trainer trainer, Model model, int epoch) {
        Metrics metrics = trainer.getMetrics();


        // mean loss
        List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
        double meanLoss = ms.stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new);
        model.setProperty("MeanLoss", Double.toString(meanLoss));
        log.info("MeanLoss: " + meanLoss);

        // mean value
        // loss
        double meanValueLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("value_0") && !name.contains("entropy_loss_value"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        gameBuffer.putMeanValueLoss(epoch, meanValueLoss);
        meanValueLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("value_0") && name.contains("value") && !name.contains("entropy_loss_value"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty("MeanValueLoss", Double.toString(meanValueLoss));
        log.info("MeanValueLoss: " + meanValueLoss);

        // mean entropy value
        // loss
        double meanValueStdLoss = metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_value_std_0"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        gameBuffer.putMeanEntropyValueLoss(epoch, meanValueStdLoss);
        meanValueStdLoss += metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_value_std_0") && name.contains("loss_value_std"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        model.setProperty("MeanValueStdLoss", Double.toString(meanValueStdLoss));
        log.info("MeanValueStdLoss: " + meanValueStdLoss);

        // mean similarity
        // loss
        double meanSimLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_similarity_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        gameBuffer.putMeanValueLoss(epoch, meanSimLoss);
        meanSimLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_similarity_0") && name.contains("loss_similarity"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty("MeanSimilarityLoss", Double.toString(meanSimLoss));
        log.info("MeanSimilarityLoss: " + meanSimLoss);

        // mean policy loss
        double meanPolicyLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("policy_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        meanPolicyLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("policy_0") && name.contains("policy"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty("MeanPolicyLoss", Double.toString(meanPolicyLoss));
        log.info("MeanPolicyLoss: " + meanPolicyLoss);
    }

    private void loadModelParametersOrCreateIfNotExisting(Model model) {
        loadModelParametersOrCreateIfNotExisting(model,  null,  null);
    }

    private boolean isNumParametersLogged;
    private void loadModelParametersOrCreateIfNotExisting(Model model, Path modelPath, Map<String, ?> options) {
        try {
            String outputDir = config.getNetworkBaseDir();
            mkDir(outputDir);
            model.load(modelPath == null ? Paths.get(outputDir) : modelPath,  null,  options);
        } catch (Exception e) {

            final int epoch = -1;
            DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epoch, TrainingTypeKey.RULES);

            djlConfig.getTrainingListeners().stream()
                .filter(MyEpochTrainingListener.class::isInstance)
                .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(epoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = batchFactory.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.notifyListeners(listener -> listener.onEpoch(trainer));
            }
        }


        long numberOfParameters = model.getBlock().getParameters().stream().mapToLong(p -> p.getValue().getArray().size()).sum();
        if (!isNumParametersLogged) {
            log.info("{} parameters in the model.", numberOfParameters);
            isNumParametersLogged = true;
        }

    }

    private boolean initialInferences(int numParallelInferences) {
        return initialInferences(numParallelInferences, -1);
    }

    private boolean initialInferences(int numParallelInferences, int epochRestriction) {

        List<InitialInferenceTask> localInitialInferenceTaskList =
            modelQueue.getInitialInferenceTasksNotStarted(numParallelInferences, epochRestriction);

        if (localInitialInferenceTaskList.isEmpty()) return false;

        log.debug("runInitialInference() for {} games", localInitialInferenceTaskList.size());

        List<Game> games = localInitialInferenceTaskList.stream().map(InitialInferenceTask::getGame).collect(Collectors.toList());

        inferenceDuration.on();
        List<NetworkIO> networkOutput = network.initialInferenceListDirect(games);
        inferenceDuration.off();

        IntStream.range(0, localInitialInferenceTaskList.size()).forEach(g -> {
            InitialInferenceTask task = localInitialInferenceTaskList.get(g);
            NetworkIO networkIO = networkOutput.get(g);
            networkIO.setEpoch(epochRestriction);
            task.setNetworkOutput(networkIO);
            task.setDone(true);
        });

        return true;
    }

    private boolean recurrentInferences(int numParallelInferences) {
        return recurrentInferences(numParallelInferences, -1);
    }
    private boolean recurrentInferences(int numParallelInferences, int  epochRestriction) {

        List<RecurrentInferenceTask> localRecurrentInferenceTaskList =
            modelQueue.getRecurrentInferenceTasksNotStarted(numParallelInferences, epochRestriction);

        if (localRecurrentInferenceTaskList.isEmpty()) return true;

        log.debug("runRecurrentInference() for {} games", localRecurrentInferenceTaskList.size());

        List<List<Node>> searchPathList = localRecurrentInferenceTaskList.stream().map(RecurrentInferenceTask::getSearchPath).collect(Collectors.toList());


        inferenceDuration.on();
        List<NetworkIO> networkOutput = network.recurrentInference(searchPathList,   epochRestriction);
        inferenceDuration.off();

        IntStream.range(0, localRecurrentInferenceTaskList.size()).forEach(g -> {
            RecurrentInferenceTask task = localRecurrentInferenceTaskList.get(g);
            NetworkIO networkIO = networkOutput.get(g);
            networkIO.setEpoch(epochRestriction);
            task.setNetworkOutput(networkIO);
            task.setDone(true);
        });
        return false;
    }



//    private boolean existsTaskForEpoch(int epoch) {
//        return modelQueue.countInitialInferenceTasksNotStartedForAnEpoch(epoch)>0
//                || modelQueue.countRecurrentInferenceTasksNotStartedForAnEpoch(epoch)>0;
//    }
//
//    private boolean existsTaskForAnyEpoch() {
//        return modelQueue.countInitialInferenceTasksNotStarted()>0
//                || modelQueue.countRecurrentInferenceTasksNotStarted()>0;
//    }


    public DurAndMem getInferenceDuration() {
        return inferenceDuration;
    }

    @Override
    public void destroy() {
        running = false;
    }


}
