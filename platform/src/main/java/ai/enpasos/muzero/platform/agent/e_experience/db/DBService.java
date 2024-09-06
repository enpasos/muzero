package ai.enpasos.muzero.platform.agent.e_experience.db;


import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.LegalActionsRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DBService {

    @Autowired
    EpisodeRepo episodeRepo;




    @Autowired
    TimestepRepo timestepRepo;

    @Autowired
    MuZeroConfig config;


    @Autowired
    LegalActionsRepo legalActionsRepo;

    public void clearDB() {
        episodeRepo.dropTable();
        episodeRepo.dropSequence();
        timestepRepo.dropTable();
        timestepRepo.dropSequence();
        legalActionsRepo.dropTable();
        legalActionsRepo.dropSequence();


    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EpisodeDO> saveEpisodesAndCommit(List<EpisodeDO> episodes) {
        List<TimeStepDO> timeStepDOS = episodes.stream().map(EpisodeDO::getTimeSteps).flatMap(list -> list.stream()).collect(Collectors.toList());
        Set<LegalActionsDO> las = timeStepDOS.stream().map(t -> t.getLegalact()).collect(Collectors.toSet());

        List<LegalActionsDO> knownLegalActions = legalActionsRepo.findAllByLegalActions(las.stream().map(la -> la.getLegalActions()).collect(Collectors.toList()));

        List<LegalActionsDO> unknownLegalActions = new ArrayList<>();
        unknownLegalActions.addAll(las);
        unknownLegalActions.removeAll(knownLegalActions);

        List<LegalActionsDO> legalActions = legalActionsRepo.saveAll(unknownLegalActions);
        legalActions.addAll(knownLegalActions);


        Map<LegalActionsDO, LegalActionsDO> map = new HashMap<>();

        legalActions.forEach(la -> map.put(la, la));

        episodes.stream().filter(EpisodeDO::isHybrid).forEach(episodeDO -> {
           long t = episodeDO.getTStartNormal();
            for (TimeStepDO timeStep : episodeDO.getTimeSteps()) {
                timeStep.setExploring(timeStep.getT() < t);
            }
        });
        episodes.stream().forEach(episodeDO -> {
            long t = episodeDO.getTStartNormal();
            for (TimeStepDO timeStep : episodeDO.getTimeSteps()) {
                LegalActionsDO legalActionsDO = map.get(timeStep.getLegalact());
                timeStep.setLegalact(legalActionsDO);
                legalActionsDO.getTimeSteps().add(timeStep);
            }
        });
        return episodeRepo.saveAllAndFlush(episodes);
    }

    @Transactional
    public List<Long> getNewEpisodeIds() {
        int limit = 50000;
        List<Long> newEpisodeIds = new ArrayList<>();
        int offset = 0;
        episodeRepo.updateMinUOK();
        List newIds;
        do {
            newIds = episodeRepo.findAllEpisodeIdsWithBoxSmallerOrEqualsMinUOk(limit, offset, -2);
            newEpisodeIds.addAll(newIds);
            offset += limit;
        } while (newIds.size() > 0);
        return newEpisodeIds;
    }

    @Transactional
    public Pair<List<EpisodeDO>, Integer> findAll(int pageNumber, int pageSize) {
        Page<EpisodeDO> result = episodeRepo.findAll(PageRequest.of(pageNumber, pageSize, Sort.by("id")));
        int totalPages = result.getTotalPages();
        Pair<List<EpisodeDO>, Integer> pair = new ImmutablePair<>(result.stream().map(e -> e.copy()).collect(Collectors.toList()), totalPages );
        return pair;
    }

    @Transactional
    public List<EpisodeDO> findTopNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findTopNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findRandomNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNRelevantForRewardLearningAndConvertToGameDTOList(int n) {
        List<Long> ids = timestepRepo.findRandomNEpisodeIdsRelevantForRewardLearning(this.config.getRewardLossThreshold(), n, 0);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNRelevantFromBoxAndConvertToGameDTOList(int n, int box) {
        List<Long> ids = timestepRepo.findRandomNEpisodeIdsFromBox(n, box);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNRelevantFromBoxZeroOrOneAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findRandomNEpisodeIdsFromBoxZeroOrOne(n );
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNRelevantForLegalActionLearningAndConvertToGameDTOList(int n) {
        List<Long> ids = timestepRepo.findRandomNEpisodeIdsRelevantForLegalActionLearning(this.config.getLegalActionLossMaxThreshold(), n, 0);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findNRandomEpisodeIdsWeightedAAndConvertToGameDTOList(int n) {
        int classN = 5;
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= classN; i++) {
            ids.addAll(timestepRepo.findNRandomEpisodeIdsWeightedA(i, n/classN));
        }
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findNEpisodeIdsWithHighestRewardLossAndConvertToGameDTOList(int n) {
        double minLoss = 0.001d; // everything else is good enough
        List<Long> ids = timestepRepo.findNEpisodeIdsWithHighestRewardLoss(n, minLoss);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }




    public int getMaxTrainingEpoch() {
        return episodeRepo.getMaxTrainingEpoch();
    }





    @Transactional
    public List<EpisodeDO> findEpisodeDOswithTimeStepDOs(List<Long> episodeIds) {

        List<TimeStepDO> timeStepDOs = timestepRepo.findTimeStepDOswithEpisodeIds(episodeIds);
     //   timeStepDOs.stream().forEach(t -> t.getNextTimeStep());

        List<EpisodeDO> episodeDOs = timeStepDOs.stream().map(t -> t.getEpisode()).distinct().collect(Collectors.toList());
        episodeDOs.forEach(e -> e.setTimeSteps(new ArrayList<>()));
        timeStepDOs.stream().forEach(t -> t.getEpisode().getTimeSteps().add(t));
     //   timeStepDOs.stream().forEach(t -> t.getValues().size());

        return episodeDOs;
    }





    //@Transactional
    public void markArchived(int epoch) {
        int n = 10000; // todo
        int n2 = 10;
       // int nCandidate = episodeRepo.countNotArchivedWithValueCount(n2);
       // if (nCandidate < n) return;
        Double quantile = episodeRepo.findTopQuantileWithHighestVariance(n, n2);
        log.info("quantile: {}", quantile);
        if (quantile == null) return;
        log.info("episodeRepo.markArchived(quantile) ...");
        episodeRepo.markArchived(quantile);
        log.info("timestepRepo.markArchived() ...");
        timestepRepo.markArchived();
        log.info("...markArchived.");
    }



    public List<Long> updateTimesteps_SandUOkandBox(List<TimeStepDO> timesteps  ) {
        List<Long> ids = new ArrayList<>();
        timesteps.stream().forEach(ts -> {

            boolean boxChangedA  = ts.updateBoxA(  );
            boolean boxChangedB  = ts.updateBoxB( config.getMaxUnrollSteps());

            if (ts.isSChanged() || ts.isUOkChanged() ||    boxChangedA ||    boxChangedB || ts.isUnrollStepsChanged()) {
                ids.add(ts.getId()) ;
                timestepRepo.updateAttributeSAndU(ts.getId(), (long) ts.getS(), ts.isSClosed(), ts.getUOk(), ts.isUOkClosed(), ts.getBoxA( ), ts.getBoxB( ) );
                if ( ts.getT() > 0) {
                    long id = ts.getEpisode().getTimeStep((ts.getT() - 1)).getId();
                    ids.add(id);
                    timestepRepo.updateNextUOk(id, ts.getUOk());
                }
                ts.setSChanged(false);
                ts.setUOkChanged(false);
            }
            ts.setUOkTestedA(false);
            ts.setUOkTestedB(false);

        });
        return ids;
    }

    public void setNextuoktarget(int unrollSteps) {
        // min(unrollSteps – 1, tmax-(t+1))

        episodeRepo.updateTmax();
        timestepRepo.updateNextUOkTarget(unrollSteps);
       // timestepRepo.updateTrainable( );

    }

//    public void updateBox0(int unrollSteps) {
//        timestepRepo.updateBox0(unrollSteps);
//    }

//    public void updateUnrollStepsOnEpisode(List<EpisodeDO> episodeDOList) {
//        episodeDOList.stream().forEach(episodeDO -> {
//            // find minimum of uOK on all timesteps
//            int minUOK = episodeDO.getTimeSteps().stream().filter(ts -> !ts.isUOkClosed()).mapToInt(ts -> ts.getUOk()).min().orElse(config.getMaxUnrollSteps());
//            episodeRepo.updateUnrollSteps(episodeDO.getId(), Math.max(1, minUOK + 1));
//        });
//    }


//    public void updateEpisodes_S(List<EpisodeDO> episodes) {
//        episodes.stream().forEach(e -> e.getTimeSteps().stream().filter(ts -> ts.getAction() != null).forEach(ts -> {
//                    if (ts.isSChanged()) {
//                        timestepRepo.updateAttributeS(ts.getId(), (long) ts.getS(), ts.isSClosed());
//                        ts.setSChanged(false);
//                    }
//                }
//        ));
//    }
//
//    public void updateEpisodes_uOK(List<EpisodeDO> episodes) {
//        episodes.stream().forEach(e -> e.getTimeSteps().stream().forEach(ts -> {
//                    if (ts.isUOkChanged()) {
//                        timestepRepo.updateAttributeUOk(ts.getId(), (long) ts.getUOk());
//                        ts.setSChanged(false);
//                    }
//                }
//        ));
//    }
}
