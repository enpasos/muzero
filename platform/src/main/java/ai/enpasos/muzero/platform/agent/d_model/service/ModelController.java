package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.*;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.TrainingDatasetType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.d_model.djl.EpochHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;
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
                initialInferences(numParallelInferences);
                recurrentInferences(numParallelInferences);
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

    private void close() {
        if (nDManager != null)
            nDManager.close();
        if (network != null && network.getModel() != null)
            network.getModel().close();

    }


    private void controllerTasks() {

        List<ControllerTask> localControllerTaskList = modelQueue.getControllerTasksNotStarted();

        if (localControllerTaskList.isEmpty()) return;

        for (ControllerTask task : localControllerTaskList) {
            switch (task.getTaskType()) {
                case SAVE_LATEST_MODEL:
                    Model model = network.getModel();
                    String modelName = config.getModelName();
                    try {
//                        model.setProperty("Epoch", String.valueOf(epoch));
//                        if (onSaveModel != null) {
//                            onSaveModel.accept(trainer);
//                        }
                        Path modelPath = Paths.get(config.getNetworkBaseDir());

                        model.save(modelPath, modelName);
                    } catch (IOException e) {
                        log.error("Failed to save checkpoint", e);
                    }
                    break;
                case SAVE_LATEST_MODEL_PARTS:
                    model = network.getModel();
                    modelName = config.getModelName();
                    try {
//                        model.setProperty("Epoch", String.valueOf(epoch));
//                        if (onSaveModel != null) {
//                            onSaveModel.accept(trainer);
//                        }
//                        Path modelPath = Paths.get(config.getNetworkBaseDir());
//                        ((DCLAware)model.getBlock()).setExportFilter(task.getExportFilter());
//                        model.save(modelPath, modelName);

                        List<String> parts = List.of("A", "B", "C");
                        modelName =  config.getModelName();
                        for (int p = 0; p < parts.size(); p++) {
                            if (task.getExportFilter()[p]) {
                                String part = parts.get(p);
                                boolean[] localExportFilter = new boolean[3];
                                localExportFilter[p] = true;
                                ((DCLAware)model.getBlock()).setExportFilter(localExportFilter);
                                 model.save(Paths.get(config.getNetworkBaseDir()),modelName + "-" + part );
                            }
                        }
                    } catch (IOException e) {
                        log.error("Failed to save checkpoint", e);
                    }
                    break;
                case LOAD_LATEST_MODEL:
                    close();
                    model = Model.newInstance(config.getModelName(), Device.gpu());
                    if (task.epoch == -1) {
                        log.info("loadLatestModel for lastest epoch with model name {}", config.getModelName());
                        network = new Network(config, model);
                    } else {
                        log.info("loadLatestModel for epoch {} with model name {}", task.epoch,  config.getModelName());
                        network = new Network(config, model, Paths.get(config.getNetworkBaseDir()),
                            Map.ofEntries(entry("epoch", task.epoch + "")));
                    }
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case LOAD_LATEST_MODEL_PARTS:
                    close();
                    model = Model.newInstance(config.getModelName(), Device.gpu());
                    Block block = new MuZeroBlock(config);
                    model.setBlock(block);


                    Map<String, ?> options = null;
                    if (task.epoch != -1) {
                        options = Map.ofEntries(entry("epoch", task.epoch + ""));
                    }


                    List<String> parts = List.of("A", "B", "C");
                    modelName =  config.getModelName();
                    for (int p = 0; p < parts.size(); p++) {
                        if (task.getExportFilter()[p]) {
                            String part = parts.get(p);
                            boolean[] localExportFilter = new boolean[3];
                            localExportFilter[p] = true;
                            ((DCLAware)model.getBlock()).setExportFilter(localExportFilter);
                            try {
                                model.load(Paths.get(config.getNetworkBaseDir()), modelName + "-" + part, options);
                            } catch (@NotNull IOException | MalformedModelException e) {
                                log.warn(e.getMessage());
                            }
                        }
                    }
                    ((DCLAware)model.getBlock()).setExportFilter(new boolean[]{true, true, true});
                    network = new Network(config, model);

                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    final int epoch = getEpochFromModel(model);
                    this.modelState.setEpoch(epoch);


                  // final int epoch = -1;
                    DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epoch,  false, config.isWithConsistencyLoss(), false, config.getNumUnrollSteps());

                    djlConfig.getTrainingListeners().stream()
                            .filter(MyEpochTrainingListener.class::isInstance)
                            .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(epoch));
                    try (Trainer trainer = model.newTrainer(djlConfig)) {
                        Shape[] inputShapes = batchFactory.getInputShapes();
                        trainer.initialize(inputShapes);
                     //   trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                    }



                    break;
                case LOAD_LATEST_MODEL_OR_CREATE_IF_NOT_EXISTING:
                    close();
                    model = Model.newInstance(config.getModelName(), Device.gpu());
                    if (model.getBlock() == null) {
                        block = new MuZeroBlock(config);
                        model.setBlock(block);
                        if (task.epoch == -1) {
                            loadModelParametersOrCreateIfNotExisting(model);
                        } else {
                            loadModelParametersOrCreateIfNotExisting(model, Paths.get(config.getNetworkBaseDir()),
                                    Map.ofEntries(entry("epoch", task.epoch + "")));
                        }
                    }
                    network = new Network(config, model);
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case TRAIN_MODEL:
                    trainNetwork( task.freeze, task.isBackground(), task.getTrainingDatasetType());
                    break;
                case TRAIN_MODEL_RULES:  // we start of with using the same method for training the rules but freezing the parameters
                    trainNetworkRules( task.freeze, task.isBackground(), task.getTrainingDatasetType());
                    break;
                    // TODO: only train rules part of the network
//                case TRAIN_MODEL_RULES:
//                    trainNetworkRules(  );
//                    break;
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


    private void trainNetwork( boolean[] freeze, boolean background, TrainingDatasetType trainingDatasetType) {
        Model model = network.getModel();

        MuZeroBlock muZeroBlock = (MuZeroBlock) model.getBlock();
        muZeroBlock.setRulesModel(false);
                try (NDScope nDScope = new NDScope()) {
                    int epochLocal;
                    int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
                    boolean withSymmetryEnrichment =  config.isWithSymmetryEnrichment();
                    epochLocal = getEpochFromModel(model);
                    DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epochLocal, background, config.isWithConsistencyLoss(), false, config.getNumUnrollSteps());
                    int finalEpoch = epochLocal;
                    djlConfig.getTrainingListeners().stream()
                            .filter(MyEpochTrainingListener.class::isInstance)
                            .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
                    try (Trainer trainer = model.newTrainer(djlConfig)) {
                        Shape[] inputShapes = batchFactory.getInputShapes();
                        trainer.initialize(inputShapes);
                        trainer.setMetrics(new Metrics());
                        ((DCLAware) model.getBlock()).freezeParameters(freeze);
                        for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                            try (Batch batch = batchFactory.getBatchFromBuffer(trainer.getManager(), withSymmetryEnrichment, config.getNumUnrollSteps(), config.getBatchSize(), trainingDatasetType)) {
                                log.debug("trainBatch " + m);
                                MyEasyTrain.trainBatch(trainer, batch);
                                trainer.step();
                            }
                        }

                        handleMetrics(trainer, model, epochLocal);
                        trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                    }
                }

                modelState.setEpoch(getEpochFromModel(model));


    }

    @Autowired
    EpisodeRepo episodeRepo;





    private void trainNetworkRules(boolean[] freeze, boolean background, TrainingDatasetType trainingDatasetType) {
        Model model = network.getModel();
        MuZeroBlock muZeroBlock = (MuZeroBlock) model.getBlock();
        muZeroBlock.setRulesModel(true);


        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(8);


        int epochLocal;
        boolean withSymmetryEnrichment = config.isWithSymmetryEnrichment();
        epochLocal = getEpochFromModel(model);


        // start pseudocode for one episode
        // boolean[][] b_OK = new boolean[tmax+1][tmax+1];
        // boolean[i][j]: starting i, ending j
        // int tmax = t of the last timestep with action
        //
        // for (int k = 0; k <= tmax; k++) {
        //     int t = tmax - k;
        //     int u = determineUnrollSteps(b_OK, t);   // 0 <= u <= tmax-t
        //     trainingOnTAndUnrollStepsAndUpdateB_OK(t, u, b_OK);
        //     b_OK can be completely filled up to here for i = t,
        // }
        // end pseudocode for one episode


        // pseudocode for episodes in a buffer
        //
        // how to batch the episodes within the buffer?
        // sort the episodes according to their u
        // take the batches starting from the smallest u
        // for each batch, determine the maximum u
        //
        // in principle keep the t loop, but globally iterate the difference to the maximum tmax per episode.
        // end pseudocode


        // start real code
        // first the buffer loop
        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setEpisodeIds(gameBuffer.getEpisodeIds());
        int w = 0;

        System.out.println("epoch;w;s;smax;i;sumMeanLoss;countNOK");
        for (RulesBuffer.EpisodeIdsWindowIterator iterator = rulesBuffer.new EpisodeIdsWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            Collections.shuffle(gameBuffer);
            // now the game buffer is filled with games from the database
            // find tmaxmax as the maximum tmax of all games
          //  int[] tmax = gameBuffer.stream().mapToInt(game -> game.getEpisodeDO().getLastTimeWithAction()).toArray();
          //  int tmaxmax = Arrays.stream(tmax).max().orElse(0);
            int tmaxmax = 0;
            boolean[][][] b_OK = new boolean[gameBuffer.size()][][];
            for (int e = 0; e < b_OK.length; e++) {
                int tmax = gameBuffer.get(e).getEpisodeDO().getLastTimeWithAction();
                tmaxmax = Math.max(tmaxmax, tmax);
                b_OK[e] = new boolean[tmax + 1][tmax + 1];
            }
            for (int k = 0; k <= tmaxmax; k++) {
                int[] us = ZipperFunctions.determineUnrollSteps(ZipperFunctions.trainingNeeded(b_OK),   k);
                int[] sortedIndices = ZipperFunctions.sortedAndFilteredIndices(us);
                // batch loop over timesteps with batch sizes of config.getBatchSize()
                for (int i = 0; i < sortedIndices.length; i += config.getBatchSize()) {

                    int i_start = i;
                    int i_end_excluded = Math.min(i + config.getBatchSize(), sortedIndices.length);
                    int u = us[i_end_excluded - 1];
                    int symFactor = config.getSymmetryType().getSymmetryEnhancementFactor();
                    boolean[][][] b_OK_batch = new boolean[symFactor*(i_end_excluded - i_start)][][];
                    // transfer b_OK from the global array to the batch array
                    for (int j = i_start; j < i_end_excluded; j++) {
                        for (int s = 0; s < symFactor; s++) {
                            b_OK_batch[s*(i_end_excluded - i_start) + (j - i_start)] = b_OK[sortedIndices[j]];
                        }
                    }

                    muZeroBlock.setNumUnrollSteps(u + 1);
                    Shape[] inputShapes = batchFactory.getInputShapesForRules(u);
                    try (Trainer trainer = model.newTrainer(trainingConfigFactory.setupTrainingConfig(epochLocal, background, false, true, u+1))) {
                        trainer.setMetrics(new Metrics());
                        trainer.initialize(inputShapes);
                        ((DCLAware) model.getBlock()).freezeParameters(freeze);
                        List<TimeStepDO> batchTimeSteps = extractTimeSteps(gameBuffer, i_start, i_end_excluded, sortedIndices, k);
                        try (Batch batch = batchFactory.getRulesBatchFromBuffer(batchTimeSteps, trainer.getManager(), withSymmetryEnrichment, u, config.getBatchSize())) {
                            Statistics stats = new Statistics();
                                    MyEasyTrainRules.trainBatch(trainer, batch, b_OK_batch, k, stats);

                            // transfer b_OK back from batch array to the global array
                            for (int j = i_start; j < i_end_excluded; j++) {
                                for (int s = 0; s < symFactor; s++) {
                                    b_OK[sortedIndices[j]] = b_OK_batch[s*(i_end_excluded - i_start) + (j - i_start)];
                                }
                            }

                            int countNOK = countNOKFromB_OK(b_OK_batch);



                            int count = stats.getCount();
                         //   int countNOK = (int) oks.getKey().stream().filter(b -> !b).count();
                         //  rememberOks(batchTimeSteps, oks.getKey(), u);
                            double sumLossR = stats.getSumLossReward();
                            double sumMeanLossR = sumLossR / count;

                            double sumLossL = stats.getSumLossLegalActions();
                            double sumMeanLossL = sumLossL / count;
                            System.out.println(epochLocal + ";" + w + ";" + u + ";"  + ";" + i + ";" + nf.format(sumMeanLossL)+ ";" + nf.format(sumMeanLossR)+ ";" + countNOK);
                            trainer.step();
                        }
                        handleMetrics(trainer, model, epochLocal);
                        trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                        epochLocal = getEpochFromModel(model);  // TODO: check epoch handling and numbering
                        modelState.setEpoch(epochLocal);
                    }
                }

            }
            w++;

        }




                //                for (int i = 0; i < sortedIndices.length; i++) {
//                    int j = sortedIndices[i];
//                    int t = tmax[j] - k;
//                    int s = 0;
//                    int smax = 0;
//                    while (s <= smax) {
//                        Shape[] inputShapes = batchFactory.getInputShapesForRules(s);
//                        try (Trainer trainer = model.newTrainer(trainingConfigFactory.setupTrainingConfig(epochLocal, background, false, true, s))) {
//                            trainer.setMetrics(new Metrics());
//                            ((DCLAware) model.getBlock()).freezeParameters(freeze);
//                            List<TimeStepDO> timesteps = extractTimeSteps(gameBuffer, s);
//                            Collections.shuffle(timesteps);
//                            for (int i = 0; i < timesteps.size(); i += config.getBatchSize()) {
//                                int fromIndex = i;
//                                int toIndex = Math.min(i + config.getBatchSize(), timesteps.size());
//                                List<TimeStepDO> batchTimeSteps = timesteps.subList(fromIndex, toIndex);
//                                try (Batch batch = batchFactory.getRulesBatchFromBuffer(batchTimeSteps, trainer.getManager(), withSymmetryEnrichment, s, config.getBatchSize())) {
//                                    Pair<List<Boolean>, Double> oks = MyEasyTrainRules.trainBatch(trainer, batch);
//                                    int count = oks.getKey().size();
//                                    int countNOK = (int) oks.getKey().stream().filter(b -> !b).count();
//                                    rememberOks(batchTimeSteps, oks.getKey(), s);
//                                    double sumLoss = oks.getValue();
//                                    double sumMeanLoss = sumLoss / count;
//                                    System.out.println(epochLocal + ";" + w + ";" + s + ";" + smax + ";" + i + ";" + nf.format(sumMeanLoss) + ";" + countNOK);
//                                    trainer.step();
//                                }
//                            }
//                            smax = gameBuffer.stream().mapToInt(game -> game.getEpisodeDO().getTimeSteps().stream()
//                                    .mapToInt(TimeStepDO::getS).max().orElse(0)
//                            ).max().orElse(0);
//                        }
//                    }
              //  }


         //   }

    //    }



//
//        // TODO: dynamic numUnrollStepsMax
//        for (int numUnrollSteps = 1; numUnrollSteps <= 9; numUnrollSteps++) {
//
//            muZeroBlock.setNumUnrollSteps(numUnrollSteps);
//
//
//            try (NDScope nDScope = new NDScope()) {
//
//                DefaultTrainingConfig djlConfigRules = trainingConfigFactory.setupTrainingConfig(epochLocal, background, false, true, numUnrollSteps);
//
//                int finalEpoch = epochLocal;
//                djlConfigRules.getTrainingListeners().stream()
//                        .filter(MyEpochTrainingListener.class::isInstance)
//                        .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
//
//
//                try (Trainer trainer = model.newTrainer(djlConfigRules)) {
//
//                    trainer.setMetrics(new Metrics());
//                    ((DCLAware) model.getBlock()).freezeParameters(freeze);
//
//                    RulesBuffer rulesBuffer = new RulesBuffer();
//                    rulesBuffer.setWindowSize(1000);
//                    rulesBuffer.setEpisodeIds(gameBuffer.getEpisodeIds());
//                    int w = 0;
//
//                    System.out.println("epoch;w;s;smax;i;sumMeanLoss;countNOK");
//                    for (RulesBuffer.EpisodeIdsWindowIterator iterator = rulesBuffer.new EpisodeIdsWindowIterator(); iterator.hasNext(); ) {
//                        List<Long> episodeIdsRulesLearningList = iterator.next();
//                        List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
//                        List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
//                        Collections.shuffle(gameBuffer);
//                        int smax = 0;
//                        for (int s = 0; s <= smax; s++) {
//                            Shape[] inputShapes = batchFactory.getInputShapesForRules(s);
//                            trainer.initialize(inputShapes);
//                            ((MuZeroBlock) model.getBlock()).setNumUnrollSteps(s);
//                            List<TimeStepDO> timesteps = extractTimeSteps(gameBuffer, s);
//                            Collections.shuffle(timesteps);
//
//                            // batch loop over timesteps with batch sizes of config.getBatchSize()
//                            for (int i = 0; i < timesteps.size(); i += config.getBatchSize()) {
//                                //  log.info("trainNetworkRules: w = {}, s = {}, i = {} of {}", w, s, i, timesteps.size());
//                                int fromIndex = i;
//                                int toIndex = Math.min(i + config.getBatchSize(), timesteps.size());
//                                List<TimeStepDO> batchTimeSteps = timesteps.subList(fromIndex, toIndex);
//
//                                try (Batch batch = batchFactory.getRulesBatchFromBuffer(batchTimeSteps, trainer.getManager(), withSymmetryEnrichment, s, config.getBatchSize())) {
//
//                                    Pair<List<Boolean>, Double> oks = MyEasyTrainRules.trainBatch(trainer, batch);
//                                    int count = oks.getKey().size();
//                                    int countNOK = (int) oks.getKey().stream().filter(b -> !b).count();
//                                    rememberOks(batchTimeSteps, oks.getKey(), s);
//                                    double sumLoss = oks.getValue();
//                                    double sumMeanLoss = sumLoss / count;
//                                    System.out.println(epochLocal + ";" + w + ";" + s + ";" + smax + ";" + i + ";" + nf.format(sumMeanLoss) + ";" + countNOK);
//                                    trainer.step();
//                                }
//
//                            }
//                            smax = gameBuffer.stream().mapToInt(game -> game.getEpisodeDO().getTimeSteps().stream()
//                                    .mapToInt(TimeStepDO::getS).max().orElse(0)
//                            ).max().orElse(0);
//                        }
//                        w++;
//
//                    }
//
//                    handleMetrics(trainer, model, epochLocal);
//                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
//                    epochLocal = getEpochFromModel(model);
//                    modelState.setEpoch(epochLocal);
//                }
//
//            }
//        }

    }

    private int countNOKFromB_OK(boolean[][][] bOkBatch) {
        int countNOK = 0;
        for (int e = 0; e < bOkBatch.length; e++) {
            for (int i = 0; i < bOkBatch[e].length; i++) { // from
                for (int j = i; j < bOkBatch[e][i].length; j++) { // to
                    if (!bOkBatch[e][i][j]) {
                        countNOK++;
                    }
                }
            }
        }
        return countNOK;
    }


//    private void rememberOks(List<TimeStepDO> batchTimeSteps,  List<Boolean>  oks, int s) {
//        IntStream.range(0, batchTimeSteps.size()).forEach(i -> {
//            TimeStepDO timeStepDO = batchTimeSteps.get(i);
//            timeStepDO.setRuleTrainingSuccess(oks.get(i));
//            timeStepDO.setRuleTrained(s);
//            if (oks.get(i)) {
//                timeStepDO.setS(s+1);
//            }
//        });
//    }


    private List<TimeStepDO> extractTimeSteps(List<Game> gameBuffer, int iStart, int iEndExcluded, int[] sortedIndices, int k) {
        List<Game> games = new ArrayList<>();
        try {
            for (int i = iStart; i < iEndExcluded; i++) {
                games.add(gameBuffer.get(sortedIndices[i]));  // TODO check
            }
            return games.stream().map(game -> game.getEpisodeDO().getTimeSteps().get(game.getEpisodeDO().getLastTimeWithAction() - k))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("extractTimeSteps", e);
            throw e;
        }
    }


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

        // mean reward
        // loss
        double meanRewardLoss = metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_reward_0"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
       // gameBuffer.putMeanEntropyValueLoss(epoch, meanEntropyValueLoss);
        meanRewardLoss += metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_reward_0") && name.contains("loss_reward"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        model.setProperty("MeanRewardLoss", Double.toString(meanRewardLoss));
        log.info("MeanRewardLoss: " + meanRewardLoss);

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

        // mean legal action loss
        double meanLegalActionLoss = metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_legal_actions_0"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        meanLegalActionLoss += metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_legal_actions_0") && name.contains("loss_legal_actions"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        model.setProperty("MeanLegalActionLoss", Double.toString(meanLegalActionLoss));
        log.info("MeanLegalActionLoss: " + meanLegalActionLoss);
    }

    private void loadModelParametersOrCreateIfNotExisting(Model model) {
        loadModelParametersOrCreateIfNotExisting(model,  null,  null);
    }
    private void loadModelParametersOrCreateIfNotExisting(Model model, Path modelPath, Map<String, ?> options) {
        try {
            String outputDir = config.getNetworkBaseDir();
            mkDir(outputDir);
            model.load(modelPath == null ? Paths.get(outputDir) : modelPath,  null,  options);
        } catch (Exception e) {
            e.printStackTrace();
            final int epoch = -1;
            DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epoch,  false, config.isWithConsistencyLoss(), false, config.getNumUnrollSteps());

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
        log.info("{} parameters in the model.", numberOfParameters);

    }

    private void initialInferences(int numParallelInferences) {

        List<InitialInferenceTask> localInitialInferenceTaskList =
            modelQueue.getInitialInferenceTasksNotStarted(numParallelInferences);

        if (localInitialInferenceTaskList.isEmpty()) return;

        log.debug("runInitialInference() for {} games", localInitialInferenceTaskList.size());

        List<Game> games = localInitialInferenceTaskList.stream().map(InitialInferenceTask::getGame).collect(Collectors.toList());

        inferenceDuration.on();
        List<NetworkIO> networkOutput = network.initialInferenceListDirect(games);
        inferenceDuration.off();

        IntStream.range(0, localInitialInferenceTaskList.size()).forEach(g -> {
            InitialInferenceTask task = localInitialInferenceTaskList.get(g);
            task.setNetworkOutput(networkOutput.get(g));
            task.setDone(true);
        });


    }

    private void recurrentInferences(int numParallelInferences) {

        List<RecurrentInferenceTask> localRecurrentInferenceTaskList =
            modelQueue.getRecurrentInferenceTasksNotStarted(numParallelInferences);

        if (localRecurrentInferenceTaskList.isEmpty()) return;

        log.debug("runRecurrentInference() for {} games", localRecurrentInferenceTaskList.size());

        List<List<Node>> searchPathList = localRecurrentInferenceTaskList.stream().map(RecurrentInferenceTask::getSearchPath).collect(Collectors.toList());


        inferenceDuration.on();
        List<NetworkIO> networkOutput = network.recurrentInference(searchPathList);
        inferenceDuration.off();

        IntStream.range(0, localRecurrentInferenceTaskList.size()).forEach(g -> {
            RecurrentInferenceTask task = localRecurrentInferenceTaskList.get(g);
            task.setNetworkOutput(networkOutput.get(g));
            task.setDone(true);
        });

    }


    public DurAndMem getInferenceDuration() {
        return inferenceDuration;
    }

    @Override
    public void destroy() {
        running = false;
    }


}
