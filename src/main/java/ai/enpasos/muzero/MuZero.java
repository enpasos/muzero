/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero;

import ai.djl.engine.Engine;
import ai.djl.engine.EngineException;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.gamebuffer.GameIO;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.agent.slow.play.PlayManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MuZero {


    // to auto restart on exception, e.g. memory leak, start
    // java -jar ./target/muzero-0.2.0-SNAPSHOT-jar-with-dependencies.jar
    // be careful - you have to kill the process by hand
    public static void main(String[] args) throws URISyntaxException, IOException {
        try {
            run();
        } catch (Exception e) {
            restartApplication();
        }
    }


    public static void run() {

      MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
   //    MuZeroConfig config = MuZeroConfig.getGoInstance(5);
    //    MuZeroConfig config = MuZeroConfig.getGoInstance(9);

        createNetworkModelIfNotExisting(config);

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();


        initialFillingBuffer(config, replayBuffer);

        int trainingStep = 0;


        do {


            trainingStep = NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 2);
            log.info("last training step = {}", trainingStep);

            log.info("numSimulations: " + config.getNumSimulations());
            if (trainingStep <= config.getNumberTrainingStepsOnRandomPlay()) {
                PlayManager.playParallel(replayBuffer, config, 1, true, false, 1);
            } else {

                // PlayManager.playParallel(replayBuffer, config, 1, true, false, 1, true);

                int numParallelPlays = config.getNumParallelPlays();
                int numberOfPlays = config.getNumPlays();

                log.info("numParallelPlays: " + numParallelPlays);
                log.info("numberOfPlays: " + numberOfPlays);


                PlayManager.playParallel(replayBuffer, config, numberOfPlays, false, false, numParallelPlays);
                replayBuffer.saveState();

            }

        } while (trainingStep < config.getNumberOfTrainingSteps());


    }

    private static void initialFillingBuffer(MuZeroConfig config, ReplayBuffer replayBuffer) {
        while (replayBuffer.getBuffer().getData().size() < config.getWindowSize()) {
            log.info(replayBuffer.getBuffer().getData().size() + " of " + config.getWindowSize());
            PlayManager.playParallel(replayBuffer, config, 1, false, true, 10000);
            replayBuffer.saveState();
        }
    }

    private static void createNetworkModelIfNotExisting(@NotNull MuZeroConfig config) {
        NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, null, 0);
    }


    private static void makeObjectDir(@NotNull MuZeroConfig config) {
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config) + "/" + (GameIO.getLatestObjectNo(config) + 1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteNetworksAndGames(@NotNull MuZeroConfig config) {
        try {
            FileUtils.forceDelete(new File(config.getOutputDir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config)));
            FileUtils.forceMkdir(new File(getGamesBasedir(config)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static @NotNull String getGamesBasedir(@NotNull MuZeroConfig config) {
        return config.getOutputDir() + "games";
    }


    public static @NotNull String getNetworksBasedir(@NotNull MuZeroConfig config) {
        if (config.getNetworkBaseDir() != null) return config.getNetworkBaseDir();
        return config.getOutputDir() + "networks";
    }


    private static void restartApplication() throws URISyntaxException, IOException {
//        File currentSourceFile = new File(MuZero.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//        if(!currentSourceFile.getName().endsWith(".jar"))
//            return;
//        List<String> command = List.of("java -jar muzero.jar");


                final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(MuZero.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if(!currentJar.getName().endsWith(".jar"))
            return;

        final ArrayList<String> command = new ArrayList<String>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        System.exit(0);
    }

}
