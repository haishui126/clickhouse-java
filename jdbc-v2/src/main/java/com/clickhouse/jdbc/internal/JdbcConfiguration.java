package com.clickhouse.jdbc.internal;

import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.ClientConfigProperties;
import com.clickhouse.client.api.http.ClickHouseHttpProto;
import com.clickhouse.data.ClickHouseUtils;
import com.clickhouse.jdbc.Driver;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverPropertyInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JdbcConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JdbcConfiguration.class);
    public static final String PREFIX_CLICKHOUSE = "jdbc:clickhouse:";
    public static final String PREFIX_CLICKHOUSE_SHORT = "jdbc:ch:";

    final boolean disableFrameworkDetection;

    private final Map<String, String> clientProperties;

    private final Map<String, String> driverProperties;

    private final String connectionUrl;

    public boolean isDisableFrameworkDetection() {
        return disableFrameworkDetection;
    }

    /**
     * Parses URL to get property and target host.
     * Properties that are passed in the {@code info} parameter will override that are set in the {@code url}.
     * @param url - JDBC url
     * @param info - Driver and Client properties.
     */
    public JdbcConfiguration(String url, Properties info) {
        this.connectionUrl = cleanUrl(url);
        this.disableFrameworkDetection = Boolean.parseBoolean(info.getProperty("disable_frameworks_detection", "false"));
        this.clientProperties = new HashMap<>();
        this.driverProperties = new HashMap<>();
        initProperties(this.connectionUrl, info);
    }

    public static boolean acceptsURL(String url) {
        // TODO: should be also checked for http/https
        return url.startsWith(PREFIX_CLICKHOUSE) || url.startsWith(PREFIX_CLICKHOUSE_SHORT);
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    /**
     * Transforms JDBC URL to WEB network. Method doesn't do guessing.
     * If protocol is not set - then {@code https} used by default.
     * User can always pass any protocol explicitly what is always should be encouraged.
     * @param url - JDBC url
     * @return URL without JDBC prefix
     */
    protected String cleanUrl(String url) {
        url = stripUrlPrefix(url);
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        return url;
    }
    private String stripUrlPrefix(String url) {
        if (url.startsWith(PREFIX_CLICKHOUSE)) {
            return url.substring(PREFIX_CLICKHOUSE.length());
        } else if (url.startsWith(PREFIX_CLICKHOUSE_SHORT)) {
            return url.substring(PREFIX_CLICKHOUSE_SHORT.length());
        } else {
            throw new IllegalArgumentException("URL is not supported.");
        }
    }

    List<DriverPropertyInfo> listOfProperties;

    private void initProperties(String url, Properties providedProperties) {
        log.debug("Parsing url: " + url + ", properties: " + providedProperties);
        Map<String, String> props = new HashMap<>();
        for (Map.Entry<Object, Object> entry : providedProperties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                props.put((String) entry.getKey(), (String) entry.getValue());
            } else {
                throw new RuntimeException("Cannot apply non-String properties");
            }
        }
        Map<String, String> mergedProperties = ClickHouseUtils.extractParameters(url, props);
        Map<String, DriverPropertyInfo> propertyInfos = new HashMap<>();
        for (Map.Entry<String, String> prop : mergedProperties.entrySet()) {
            DriverPropertyInfo propertyInfo = new DriverPropertyInfo(prop.getKey(), prop.getValue());
            propertyInfo.description = "(User Defined)";
            propertyInfos.put(prop.getKey(), propertyInfo);
            if (prop.getValue() != null) {
                if (prop.getKey().contains("driver.")) {
                    driverProperties.put(prop.getKey(), prop.getValue());
                } else {
                    clientProperties.put(prop.getKey(), prop.getValue());
                }
            }
        }
        // set know properties
        for (ClientConfigProperties clientProp : ClientConfigProperties.values()) {
            DriverPropertyInfo propertyInfo = propertyInfos.get(clientProp.getKey());
            if (propertyInfo == null) {
                propertyInfo = new DriverPropertyInfo(clientProp.getKey(), clientProp.getDefaultValue());
                // TODO: read description from resource file
                propertyInfos.put(clientProp.getKey(), propertyInfo);
            }
        }

        for (DriverProperties driverProp : DriverProperties.values()) {
            DriverPropertyInfo propertyInfo = propertyInfos.get(driverProp.getKey());
            if (propertyInfo == null) {
                propertyInfo = new DriverPropertyInfo(driverProp.getKey(), driverProp.getDefaultValue());
                propertyInfos.put(driverProp.getKey(), propertyInfo);
            }
        }

        listOfProperties = propertyInfos.values().stream().sorted(Comparator.comparing(o -> o.name)).toList();
    }

    /**
     * Returns a list of driver property information.
     * @return a list of driver property information for the driver
     */
    public List<DriverPropertyInfo> getDriverPropertyInfo() {
        return listOfProperties;
    }

    public Client.Builder applyClientProperties(Client.Builder builder) {
        builder.addEndpoint(connectionUrl)
                .setOptions(clientProperties);

        return builder;
    }
}
