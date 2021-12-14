package ai.enpasos.muzero.platform.config;

import ai.djl.Device;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.KnownBounds;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.BiFunction;

import static ai.enpasos.muzero.platform.config.KnownBoundsType.BOARD_GAME;

@Component
@ConfigurationProperties("muzero")
@Data
@Slf4j
public class MuZeroConfig {
    public static final boolean HIDDEN_STATE_REMAIN_ON_GPU = false;

    public GameType activeGame;
    public Map<GameType, Conf> games;

   @Data
   public static class Conf {
       protected double komi;
       protected String modelName;
       protected String gameClassName;
       protected String actionClassName;
       protected PlayerMode playerMode;
       protected boolean networkWithRewardHead;
       protected SymmetryType symmetryType;
       protected String networkBaseDir;
       protected boolean withRewardHead;
       protected int numObservationLayers;
       protected int numActionLayers;
       protected int numChannels;
       protected int numHiddenStateChannels;
       protected int numResiduals;
       protected int numberOfTrainingSteps;
       protected int numberOfTrainingStepsPerEpoch;
       protected int windowSize;
       protected int batchSize;
       protected int numUnrollSteps;
       protected int tdSteps;
       protected float discount;
       protected float weightDecay;
       protected float valueLossWeight;
       protected float lrInit;
       protected boolean absorbingStateDropToZero;
       protected int size;
       protected int maxMoves;
       protected int boardHeight;
       protected int boardWidth;
       protected int actionSpaceSize;
       protected int numberTrainingStepsOnRandomPlay;
       protected double rootDirichletAlpha;
       protected double rootExplorationFraction;
       protected KnownBoundsType knownBoundsType;
       protected double pbCInit;
       protected double pbCBase;
       protected DeviceType inferenceDeviceType;
       protected String outputDir;
       protected int numEpisodes;
       protected int numSimulations;
       protected int numParallelGamesPlayed;
       int visitSoftmaxTemperatureThreshold;
   }

    public Class getGameClass() {
        try {
            return Class.forName(getGameClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public Class getActionClass() {
        try {
            return Class.forName(getActionClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public BiFunction<Integer, Integer, Double> visitSoftmaxTemperatureFunction() {
        return (numMoves, trainingSteps) -> (numMoves < getVisitSoftmaxTemperatureThreshold()) ? 1.0 : 0.0;
    }


    public KnownBounds getKnownBounds() {
        if (this.getKnownBoundsType() == BOARD_GAME) {
            return new KnownBounds(-1d, 1d);
        }
        return new KnownBounds();
    }

    public Game newGame() {
        try {
            Constructor<?> constructor = this.getGameClass().getConstructor(MuZeroConfig.class);
            return (Game) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Action newAction(int index) {
        Action action = this.newAction();
        action.setIndex(index);
        return action;
    }

    public Action newAction() {
        try {
            Constructor<?> constructor = this.getActionClass().getConstructor(MuZeroConfig.class);
            return (Action) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public @NotNull String getGamesBasedir() {
        return  getOutputDir() + "games";
    }



    public Device getInferenceDevice() {
        switch(this.getInferenceDeviceType()) {
            case CPU: return Device.cpu();
            case GPU:
            default: return Device.gpu();
        }
    }

    private Conf getConf() {
        return this.games.get(this.activeGame);
    }
    public double getKomi() {
        return getConf().komi;
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

    public SymmetryType getSymmetryType() {
        return getConf().symmetryType;
    }



    public String getNetworkBaseDir() {
        if ( getConf().networkBaseDir != null) return  getConf().networkBaseDir;
        return  getConf().getOutputDir() + "networks";
    }

    public void setNetworkBaseDir(String networkBaseDir) {
        getConf().setNetworkBaseDir(networkBaseDir);
    }


    public boolean isWithRewardHead() {
        return getConf().withRewardHead;
    }

    public int getNumObservationLayers() {
        return getConf().numObservationLayers;
    }

    public int getNumActionLayers() {
        return getConf().numActionLayers;
    }

    public int getNumChannels() {
        return getConf().numChannels;
    }

    public int getNumHiddenStateChannels() {
        return getConf().numHiddenStateChannels;
    }

    public int getNumResiduals() {
        return getConf().numResiduals;
    }

    public int getNumberOfTrainingSteps() {
        return getConf().numberOfTrainingSteps;
    }

    public int getNumberOfTrainingStepsPerEpoch() {
        return getConf().numberOfTrainingStepsPerEpoch;
    }

    public int getWindowSize() {
        return getConf().windowSize;
    }

    public int getBatchSize() {
        return getConf().batchSize;
    }

    public int getNumUnrollSteps() {
        return getConf().numUnrollSteps;
    }

    public int getTdSteps() {
        return getConf().tdSteps;
    }

    public float getDiscount() {
        return getConf().discount;
    }

    public float getWeightDecay() {
        return getConf().weightDecay;
    }

    public float getValueLossWeight() {
        return getConf().valueLossWeight;
    }

    public float getLrInit() {
        return getConf().lrInit;
    }

    public boolean isAbsorbingStateDropToZero() {
        return getConf().absorbingStateDropToZero;
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

    public int getNumberTrainingStepsOnRandomPlay() {
        return getConf().numberTrainingStepsOnRandomPlay;
    }

    public double getRootDirichletAlpha() {
        return getConf().rootDirichletAlpha;
    }

    public double getRootExplorationFraction() {
        return getConf().rootExplorationFraction;
    }

    public KnownBoundsType getKnownBoundsType() {
        return getConf().knownBoundsType;
    }

    public double getPbCInit() {
        return getConf().pbCInit;
    }

    public double getPbCBase() {
        return getConf().pbCBase;
    }

    public DeviceType getInferenceDeviceType() {
        return getConf().inferenceDeviceType;
    }

    public void setInferenceDeviceType(DeviceType deviceType) {
       getConf().setInferenceDeviceType(deviceType);
    }

    public String getOutputDir() {
        return getConf().outputDir;
    }

    public int getNumEpisodes() {
        return getConf().numEpisodes;
    }

    public int getNumSimulations() {
        return getConf().numSimulations;
    }

    public int getNumParallelGamesPlayed() {
        return getConf().numParallelGamesPlayed;
    }

    public int getVisitSoftmaxTemperatureThreshold() {
        return getConf().visitSoftmaxTemperatureThreshold;
    }

}
