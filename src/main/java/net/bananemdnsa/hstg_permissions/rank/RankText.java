package net.bananemdnsa.hstg_permissions.rank;

public final class RankText {

    private RankText() {}

    /** & starts a colour code, the same as in the locked message. */
    public static String colour(String text) {
        return text.replace('&', '§');
    }
}
