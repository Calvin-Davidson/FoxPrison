package nl.trifox.foxprison.modules.economy.config;

public final class CurrencyDefinition {

    private String id = "money";
    private String displayName = "Money";
    private String symbol = "$";
    private int decimals = 2;

    /**
     * Starting balance in MINOR units (cents).
     * Example: decimals=2, startingBalanceMinor=500 => 5.00
     */
    private long startingBalanceMinor = 0;

    private boolean enabled = true;

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public int getDecimals() { return decimals; }
    public long getStartingBalanceMinor() { return startingBalanceMinor; }
    public boolean isEnabled() { return enabled; }
}