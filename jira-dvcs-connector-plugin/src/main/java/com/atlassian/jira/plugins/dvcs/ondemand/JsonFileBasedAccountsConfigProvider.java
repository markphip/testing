package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.config.properties.JiraSystemProperties;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class JsonFileBasedAccountsConfigProvider implements AccountsConfigProvider
{
    
    /**
     * @see #absoluteConfigFilePath
     */
    public static final String ENV_ONDEMAND_CONFIGURATION = "ondemand.properties";
    
    /**
     * Default value of {@link #ENV_ONDEMAND_CONFIGURATION}
     */
    public static final String ENV_ONDEMAND_CONFIGURATION_DEFAULT = "/data/jirastudio/home/ondemand.properties";
    
    private static Logger log = LoggerFactory.getLogger(JsonFileBasedAccountsConfigProvider.class);
    private final String absoluteConfigFilePath = System.getProperty(ENV_ONDEMAND_CONFIGURATION, ENV_ONDEMAND_CONFIGURATION_DEFAULT);
    private final FeatureManager featureManager;

    public JsonFileBasedAccountsConfigProvider(FeatureManager featureManager)
    {
        this.featureManager = featureManager;
    }

    @Override
    public AccountsConfig provideConfiguration()
    {
        try
        {
            File configFile = getConfigFile();
            if (!configFile.exists() || !configFile.canRead())
            {
                throw new IllegalStateException(absoluteConfigFilePath + " file can not be read.");
            }

            AccountsConfig config = null;

            GsonBuilder builder = new GsonBuilder();
            builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
            Gson gson = builder.create();

            config = gson.fromJson(new InputStreamReader(new FileInputStream(configFile)), AccountsConfig.class);

            return config;
        } catch (JsonParseException json)
        {
            log.error("Failed to parse config file " + absoluteConfigFilePath, json);
            return null;
        } catch (Exception e)
        {
            log.debug("File not found, probably not ondemand instance or integrated account should be deleted. ", e);
            return null;
        }
    }

    File getConfigFile()
    {
        return new File(absoluteConfigFilePath);
    }

    @Override
    public boolean supportsIntegratedAccounts()
    {
        log.debug("ondemand = {} | devMode = {}", new Object[] { featureManager.isEnabled(CoreFeatures.ON_DEMAND),
                JiraSystemProperties.isDevMode() });
        return featureManager.isEnabled(CoreFeatures.ON_DEMAND) || JiraSystemProperties.isDevMode();
    }
    
}
