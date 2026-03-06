package nl.trifox.foxprison.modules.ranks.data;

public class PlayerRankData {
    private String rankId = "a";
    private int prestige = 0;

    public String getRankId() { return rankId; }
    public void setRankId(String rankId) { this.rankId = rankId; }

    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestige) {
        this.prestige = prestige;
    }
}
