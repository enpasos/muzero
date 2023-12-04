package ai.enpasos.muzero.platform.config;

import ai.djl.Device;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.KnownBounds;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.config.KnownBoundsType.FROM_VALUES;
import static ai.enpasos.muzero.platform.config.KnownBoundsType.MINUSONE_ONE;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;
import static ai.enpasos.muzero.platform.config.VTargetType.V_INFERENCE;

@Component
@ConfigurationProperties("muzero")
@Data
@Slf4j
@SuppressWarnings({"squid:S1104", "unchecked"})
public class MuZeroConfig {
    public static final boolean HIDDEN_STATE_REMAIN_ON_GPU = false;
    public Map<GameType, Conf> games;
    Action[] actions;
    private RunType run = RunType.NONE;
    private GameType activeGame;

    public Class<? extends Game> getGameClass() {
        try {
            return (Class<? extends Game>) Class.forName(getGameClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public Class<? extends Action> getActionClass() {
        try {
            return (Class<? extends Action>) Class.forName(getActionClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public KnownBounds getKnownBounds() {
        if (this.getKnownBoundsType() == MINUSONE_ONE) {
            return new KnownBounds(-1d, 1d);
        }
        if (this.getKnownBoundsType() == FROM_VALUES) {
            return new KnownBounds(this.getValues()[0], this.getValues()[this.getValues().length - 1]);
        }
        return new KnownBounds();
    }


    public KnownBounds getKnownBoundsEntropyQValues() {
            return new KnownBounds();
    }

    public Game newGame(boolean connectToEnvironment, boolean withFirstObservation) {
        try {
            Constructor<?> constructor = this.getGameClass().getConstructor(MuZeroConfig.class);
            Game game =  (Game) constructor.newInstance(this);
            if (connectToEnvironment) {game.connectToEnvironment();}
            if (withFirstObservation) {
                game.addObservationFromEnvironment();
                game.addLegalActionFromEnvironment();
            }
            return game;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Action newAction(int index) {
        if (actions == null) {
            int n = this.getActionSpaceSize();
            actions = new Action[n];
            for (int i = 0; i < n; i++) {
                try {
                    Constructor<?> constructor = this.getActionClass().getConstructor(MuZeroConfig.class);
                    actions[i] = (Action) constructor.newInstance(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                actions[i].setIndex(i);
            }

        }
        return actions[index];
    }

    public @NotNull String getGamesBasedir() {
        return getOutputDir() + "games";
    }


    public Device getInferenceDevice() {
        switch (this.getInferenceDeviceType()) {
            case CPU:
                return Device.cpu();
            case GPU:
            default:
                return Device.gpu();
        }
    }

    private Conf getConf() {
        return this.games.get(this.activeGame);
    }


    public String getModelName() {
        return getConf().modelName;
    }

    public String getGameClassName() {
        return getConf().gameClassName;
    }

    public String getActionClassName() {
        return getConf().actionClassName;
    }

    public PlayerMode getPlayerMode() {
        return getConf().playerMode;
    }


    public boolean isNetworkWithRewardHead() {
        return getConf().networkWithRewardHead;
    }


    public boolean offPolicyCorrectionOn() {
        return getConf().offPolicyCorrectionOn;
    }

    public boolean withLegalActionsHead() {
        return getConf().withLegalActionsHead;
    }

    public boolean allOrNothingOn() {
        return getConf().allOrNothingOn;
    }


    public SymmetryType getSymmetryType() {
        return getConf().symmetryType;
    }

    public String getNetworkBaseDir() {
        if (getConf().networkBaseDir != null) return getConf().networkBaseDir;
        return getConf().getOutputDir() + "networks";
    }

    public void setNetworkBaseDir(String networkBaseDir) {
        getConf().setNetworkBaseDir(networkBaseDir);
    }


    public int getNumObservationLayers() {
        return getConf().numObservationLayers;
    }

    public int getNumActionLayers() {
        return getConf().numActionLayers;
    }
    public int getNumChannels1() {
        return getConf().numChannels1;
    }

    public int getNumBottleneckChannels1() {
        return getConf().numBottleneckChannels1;
    }
    public int getNumResiduals1() {
        return getConf().numResiduals1;
    }
    public int getBroadcastEveryN1() {
        return getConf().broadcastEveryN1;
    }



    public int getNumChannels2() {
        return getConf().numChannels2;
    }
    public int getNumBottleneckChannels2() {
        return getConf().numBottleneckChannels2;
    }
    public int getNumResiduals2() {
        return getConf().numResiduals2;
    }
    public int getBroadcastEveryN2() {
        return getConf().broadcastEveryN2;
    }




    public int getNumChannels3() {
        return getConf().numChannels3;
    }
    public int getNumBottleneckChannels3() {
        return getConf().numBottleneckChannels3;
    }
    public int getNumResiduals3() {
        return getConf().numResiduals3;
    }
    public int getBroadcastEveryN3() {
        return getConf().broadcastEveryN3;
    }
    public int[] getValues() {

        int[] result = new int[getValueSpan() + 1];
        int[] vs = getConf().getValueInterval();
        IntStream.range(0, result.length).forEach(i -> result[i] = vs[0] + i);
        return result;
    }


    public int getValueSpan() {
        int[] vs = getConf().getValueInterval();
        return vs[vs.length - 1] - vs[0];
    }

    public int getNumberOfTrainingSteps() {
        return getConf().numberOfTrainingSteps;
    }

    public int getNumberOfTrainingStepsPerEpoch() {
        return getConf().numberOfTrainingStepsPerEpoch;
    }


    public int getBatchSize() {
        return getConf().batchSize;
    }

    public int getNumUnrollSteps() {
        return getConf().numUnrollSteps;
    }


    public int getTdSteps() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).getTdSteps();
    }

    public float getDiscount() {
        return getConf().discount;
    }

    public float getKomi() {
        return getConf().komi;
    }


    public float getWeightDecay() {
        return getConf().weightDecay;
    }

    public float getValueLossWeight() {
        return getConf().valueLossWeight;
    }
    public float getConsistencyLossWeight() {
        return getConf().consistencyLossWeight;
    }





    public float getLr(int step) {
        return (float)( getConf().lrInit * (1d + Math.cos(Math.PI * step /  getNumberOfTrainingSteps())) / 2d);
    }


    public int getSize() {
        return getConf().size;
    }

    public int getMaxMoves() {
        return getConf().maxMoves;
    }

    public int getBoardHeight() {
        return getConf().boardHeight;
    }

    public int getBoardWidth() {
        return getConf().boardWidth;
    }

    public int getActionSpaceSize() {
        return getConf().actionSpaceSize;
    }

    public int getNumberTrainingStepsOnStart() {
        return getConf().numberTrainingStepsOnStart;
    }


    public double getRootDirichletAlpha() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).rootDirichletAlpha;
    }
    public double getFractionOfPureExplorationAdded() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).fractionOfPureExplorationAdded;
    }
    public double getFractionOfPureExploitationAdded() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).fractionOfPureExploitationAdded;
    }


    public double getRootExplorationFraction() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).rootExplorationFraction;
    }


    public KnownBoundsType getKnownBoundsType() {
        return getConf().knownBoundsType;
    }




    public DeviceType getInferenceDeviceType() {
        return getConf().inferenceDeviceType;
    }

    // https://arxiv.org/pdf/1611.01144.pdf

    public void setInferenceDeviceType(DeviceType deviceType) {
        getConf().setInferenceDeviceType(deviceType);
    }

    public double getTemperatureRoot() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).temperatureRoot;
    }

    public void setTemperatureRoot(double temperature) {
        getConf().getPlayTypes().get(getConf().getPlayTypeKey()).temperatureRoot = temperature;
    }

    public double getFractionOfAlternativeActionGames() {
        return getConf().fractionOfAlternativeActionGames;
    }

    public String getOutputDir() {
        return getConf().outputDir;
    }

    public void setOutputDir(String outputDir) {
        getConf().setOutputDir(outputDir);
    }


    public int getInitialGumbelM() {
        return getConf().initialGumbelM;
    }

    public int getNumSimulations() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numSimulations;
    }

    public void setNumSimulations(int numSimulations) {
        getConf().getPlayTypes().get(getConf().getPlayTypeKey()).setNumSimulations(numSimulations);
    }

    public int getNumSimulationsHybrid() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numSimulationsHybrid;
    }

    public int getNumSimulations(Game game) {
        if (this.getTrainingTypeKey() == HYBRID &&
                game.isItExplorationTime(game.getGameDTO().getActions().size())  ) {
            return getNumSimulationsHybrid();
        } else {
            return getNumSimulations();
        }
    }

    public double getNumSimThreshold() {
        return getConf().numSimThreshold;
    }


    public int getNumParallelGamesPlayed() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numParallelGamesPlayed;
    }

    public boolean isForTdStep0ValueTraining() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).forTdStep0ValueTraining;
    }


    public boolean isGumbelActionSelection() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).gumbelActionSelection;
    }




    public boolean isWithGumbel() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).withGumbel;
    }
    public VTargetType getVTarget() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).getVTarget();
    }






    public PlayTypeKey getPlayTypeKey() {
        return getConf().getPlayTypeKey();
    }

    public void setPlayTypeKey(PlayTypeKey trainingTypeKey) {
        getConf().setPlayTypeKey(trainingTypeKey);
    }



    public int getCVisit() {
        return getConf().cVisit;
    }

    public void setCVisit(int cVisit) {
        getConf().setCVisit(cVisit);
    }


    public double getCScale() {
        return getConf().cScale;
    }

    public double getOffPolicyRatioLimit() {
        return getConf().offPolicyRatioLimit;
    }




    public int getNumPurePolicyPlays() {
        return getConf().numPurePolicyPlays;
    }

    public int getNumParallelInferences() {
        return getConf().numParallelInferences;
    }

    public int getNumChannelsHiddenLayerSimilarityProjector() {
        return getConf().numChannelsHiddenLayerSimilarityProjector;
    }

    public int getNumChannelsOutputLayerSimilarityProjector() {
        return getConf().numChannelsOutputLayerSimilarityProjector;
    }
    public int getNumChannelsHiddenLayerSimilarityPredictor() {
        return getConf().numChannelsHiddenLayerSimilarityPredictor;
    }

    public int getNumChannelsOutputLayerSimilarityPredictor() {
        return getConf().numChannelsOutputLayerSimilarityPredictor;
    }


    public void setWindowSize(int windowSize) {
        getConf().setWindowSize(windowSize);
    }

    public PlayTypeKey getTrainingTypeKey() {
        return getConf().playTypeKey;
    }

    public int getWindowSize() {
        return getConf().windowSize;
    }

    public Set<PlayTypeKey> getPlayTypeKeysForTraining() {
        return getConf().getPlayTypes().entrySet().stream()
            .filter(entry -> entry.getValue().isForTraining())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }


    @Data
    public static class Conf {
        public Map<PlayTypeKey, PlayType> playTypes;

        protected double numSimThreshold;


        protected String modelName;
        protected String gameClassName;
        protected String actionClassName;
        protected PlayerMode playerMode;
        protected boolean networkWithRewardHead;

        protected SymmetryType symmetryType;
        protected String networkBaseDir;

        protected int numObservationLayers;

        protected int numActionLayers ;


        protected int numChannels1;
        protected int broadcastEveryN1;
        protected int numBottleneckChannels1;
        protected int numResiduals1;


        protected int numChannels2;
        protected int broadcastEveryN2;
        protected int numBottleneckChannels2;
        protected int numResiduals2;


        protected int numChannels3;
        protected int broadcastEveryN3;
        protected int numBottleneckChannels3;
        protected int numResiduals3;

        protected int numberOfTrainingSteps;
        protected int numberOfTrainingStepsPerEpoch;
        protected int windowSize;

        protected int numChannelsHiddenLayerSimilarityProjector;
        protected int numChannelsOutputLayerSimilarityProjector;

        protected int numChannelsHiddenLayerSimilarityPredictor;
        protected int numChannelsOutputLayerSimilarityPredictor;
        protected double fractionOfAlternativeActionGames;

        protected int batchSize;
        protected int numUnrollSteps;
        protected float discount;
        protected float komi;
        protected float weightDecay;
        protected float valueLossWeight = 1f;
        protected float consistencyLossWeight = 1f;
        protected float lrInit;
        protected int size;
        protected int maxMoves;
        protected int boardHeight;
        protected int boardWidth;
        protected int actionSpaceSize;
        protected int numberTrainingStepsOnStart;

        protected KnownBoundsType knownBoundsType;

        protected DeviceType inferenceDeviceType;
        protected String outputDir;


        protected PlayTypeKey playTypeKey;
        protected int initialGumbelM;

        protected int cVisit;
        protected double cScale;
        protected int numPurePolicyPlays;
        protected int[] valueInterval;
        protected int numParallelInferences = 1;

        protected boolean offPolicyCorrectionOn;

        protected boolean allOrNothingOn;


        protected boolean withLegalActionsHead;
        protected double offPolicyRatioLimit;

        public PlayTypeKey getPlayTypeKey() {
            if (playTypeKey == null) {
                this.playTypes.entrySet().stream().filter(entry -> entry.getValue().isForTraining()).findFirst().ifPresent(entry -> this.playTypeKey = entry.getKey());
            }
            return playTypeKey;
        }


        @Data
        public static class PlayType {

            protected int numParallelGamesPlayed;

            protected boolean forTdStep0ValueTraining = true;
            protected int tdSteps;
            protected int numSimulations;
            protected int numSimulationsHybrid;
            protected double rootDirichletAlpha;
            protected double rootExplorationFraction;
            double temperatureRoot = 1.0;

            boolean gumbelActionSelection = true;

            boolean withGumbel = true;

            VTargetType vTarget = V_INFERENCE;

            boolean forTraining = true;

            protected double fractionOfPureExplorationAdded = 0d;
            protected double fractionOfPureExploitationAdded = 0d;
        }
    }

}
