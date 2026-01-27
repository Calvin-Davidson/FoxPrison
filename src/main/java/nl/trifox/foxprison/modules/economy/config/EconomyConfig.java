package nl.trifox.foxprison.modules.economy.config;

import java.util.ArrayList;
import java.util.List;

public final class EconomyConfig {

    private boolean enabled = true;

    private String defaultCurrencyId = "money";

    private List<CurrencyDefinition> currencies = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public String getDefaultCurrencyId() { return defaultCurrencyId; }
    public List<CurrencyDefinition> getCurrencies() { return currencies; }
}