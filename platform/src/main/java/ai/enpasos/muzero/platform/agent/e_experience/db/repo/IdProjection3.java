package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

public interface IdProjection3 extends IdProjection {
    Long getEpisodeId();
    Long getId();
    Integer getT();

    // state info
    Integer getUOk();
    Boolean getTrainable();
    Integer getBox();


}
