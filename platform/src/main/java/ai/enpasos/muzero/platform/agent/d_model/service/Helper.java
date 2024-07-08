package ai.enpasos.muzero.platform.agent.d_model.service;


import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.training.Trainer;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;

@Component
@Slf4j
public class Helper {

    @Autowired
    private GameBuffer gameBuffer;



    public void handleMetrics( Trainer trainer, Model model, int epoch) {
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

}
