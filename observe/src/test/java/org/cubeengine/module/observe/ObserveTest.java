/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.observe;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import org.apache.logging.log4j.LogManager;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.observe.health.SimpleHealthCheckService;
import org.cubeengine.module.observe.metrics.PrometheusMetricsService;
import org.cubeengine.module.observe.web.WebServer;
import org.spongepowered.observer.healthcheck.HealthCheckCollection;
import org.spongepowered.observer.healthcheck.HealthState;
import org.spongepowered.observer.healthcheck.SimpleHealthCheckCollection;
import org.spongepowered.observer.metrics.Meter;
import org.spongepowered.observer.metrics.meter.Gauge;
import org.spongepowered.plugin.PluginContainer;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ObserveTest {

    @Test
    public void themAll() throws IOException {
        final PluginContainer plugin = (PluginContainer) Proxy.newProxyInstance(PluginContainer.class.getClassLoader(), new Class[]{ PluginContainer.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "logger":
                    return LogManager.getLogger();
                case "hashCode":
                    return 1;
                case "equals":
                    return true;
                default:
                    return null;
            }
        });

        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        final InetSocketAddress addr = new InetSocketAddress("0.0.0.0", 0);
        final WebServer webServer = new WebServer(addr, Thread::new, plugin.logger());
        CollectorRegistry registry = new CollectorRegistry();
        registry.register(new GarbageCollectorExports());
        final PrometheusMetricsService metricsService = new PrometheusMetricsService(registry, executorService);
        final Gauge gauge = Meter.newGauge().name("test").help("dummy").build();
        gauge.set(Math.random());
        assertTrue(webServer.registerHandlerAndStart("/metrics", metricsService));

        Counter.build().name("test_counter_default_registry").help("test counter").register();

        final HealthCheckCollection healthCollection = new SimpleHealthCheckCollection();
        healthCollection.registerProbe("test", () -> HealthState.HEALTHY);
        final SimpleHealthCheckService healthyService = new SimpleHealthCheckService(executorService, healthCollection, plugin.logger());
        assertTrue(webServer.registerHandlerAndStart("/healthy", healthyService));

        final HealthCheckCollection brokenCollection = new SimpleHealthCheckCollection();
        brokenCollection.registerProbe("test", () -> HealthState.BROKEN);
        final SimpleHealthCheckService brokenService = new SimpleHealthCheckService(executorService, brokenCollection, plugin.logger());
        assertTrue(webServer.registerHandlerAndStart("/broken", brokenService));

        final String urlBase = "http://localhost:" + webServer.getBoundAddress().getPort();

        assertThrows(FileNotFoundException.class, () -> {
            System.out.println(readUrlData(urlBase + "/other"));
        });

        final Pair<Integer, String> metricsResult = readUrlData(urlBase + "/metrics");
        assertEquals(200, metricsResult.getLeft().intValue());
        assertFalse(metricsResult.getRight().isEmpty());
        System.out.println(metricsResult.getRight());


        assertEquals(new Pair<>(200, "{\"state\":\"HEALTHY\",\"details\":{\"test\":\"HEALTHY\"}}"), readUrlData(urlBase + "/healthy"));

        assertThrows(IOException.class, () -> {
            readUrlData(urlBase + "/broken");
        });

        webServer.stop();
    }

    private static Pair<Integer, String> readUrlData(String uri) throws IOException {
        try
        {
            HttpURLConnection connection = (HttpURLConnection)new URI(uri).toURL().openConnection();
            connection.connect();
            final InputStream data = connection.getInputStream();
            byte[] buf = new byte[1000];
            ByteArrayOutputStream agg = new ByteArrayOutputStream();

            while (true)
            {
                int bytesRead = data.read(buf);
                if (bytesRead == -1)
                {
                    break;
                }
                agg.write(buf, 0, bytesRead);
            }

            return new Pair<>(connection.getResponseCode(), agg.toString(StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}