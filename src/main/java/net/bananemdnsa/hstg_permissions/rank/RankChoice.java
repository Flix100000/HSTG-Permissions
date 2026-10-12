package net.bananemdnsa.hstg_permissions.rank;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Picks the rank from the stages a player has. Prefix and suffix are looked for apart, so a rank
 * stage can give the prefix and a progress stage the suffix.
 */
public final class RankChoice {

    // Highest first; a tie goes to the smaller id, so the answer never depends on load order.
    private static final Comparator<RankSource> ORDER =
            Comparator.comparingInt(RankSource::priority).reversed().thenComparing(RankSource::stageId);

    private RankChoice() {}

    public static Rank pick(Collection<RankSource> sources) {
        List<RankSource> sorted = sources.stream().sorted(ORDER).toList();
        return new Rank(first(sorted, RankSource::prefix), first(sorted, RankSource::suffix));
    }

    private static String first(List<RankSource> sorted, Function<RankSource, String> part) {
        for (RankSource source : sorted) {
            String text = part.apply(source);
            if (text != null && !text.isBlank()) return text;
        }
        return "";
    }
}
