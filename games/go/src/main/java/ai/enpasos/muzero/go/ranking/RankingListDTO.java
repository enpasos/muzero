package ai.enpasos.muzero.go.ranking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingListDTO {
    @Builder.Default
    List<RankingEntryDTO> rankings = new ArrayList<>();

    public void sortByElo() {
        rankings.sort(Comparator.comparingInt(RankingEntryDTO::getElo));
    }

    public void sortByEpoch() {
        rankings.sort(Comparator.comparingInt((RankingEntryDTO r) -> r.epochPlayer));
    }
}
