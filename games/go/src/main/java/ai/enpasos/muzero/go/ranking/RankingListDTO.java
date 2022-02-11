package ai.enpasos.muzero.go.ranking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingListDTO {
    @Builder.Default
    List<RankingEntryDTO> rankings = new ArrayList<>();
}
