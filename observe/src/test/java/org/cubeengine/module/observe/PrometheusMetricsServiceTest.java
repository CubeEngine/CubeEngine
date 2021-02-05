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

import io.prometheus.client.hotspot.GarbageCollectorExports;
import org.apache.logging.log4j.LogManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.junit.Test;
import org.spongepowered.plugin.PluginContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.DoubleSupplier;

public class PrometheusMetricsServiceTest {

    @Test
    public void metrics() throws IOException {
        final PluginContainer plugin = (PluginContainer) Proxy.newProxyInstance(PluginContainer.class.getClassLoader(), new Class[]{ PluginContainer.class }, (proxy, method, args) -> {
            if (method.getName().equals("getLogger")) {
                return LogManager.getLogger();
            } else {
                return null;
            }
        });

        final TaskManager tm = (TaskManager) Proxy.newProxyInstance(TaskManager.class.getClassLoader(), new Class[] { TaskManager.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "runTask":
                case "runTaskAsync":
                    if (args.length == 1 && args[0] instanceof Runnable) {
                        ((Runnable) args[0]).run();
                    }
            }
            return null;
        });

        final ObserveConfig config = new ObserveConfig();
        final InetSocketAddress addr = new InetSocketAddress(config.bindAddress, 0);
        final PrometheusMetricsService metricsService = new PrometheusMetricsService(Thread::new, tm, addr, plugin.getLogger());
        metricsService.register(plugin, new GarbageCollectorExports());
        metricsService.register(plugin, PullGaugeCollector.<DoubleSupplier>build(Math::random).withGauge(PullGauge.build("test", DoubleSupplier::getAsDouble).help("dummy").build()).build());

        metricsService.startExporter();

        final String urlBase = "http://localhost:" + metricsService.getPort();

        System.out.println(readUrlData(new URL(urlBase + "/other")));
        System.out.println(readUrlData(new URL(urlBase + "/metrics")));
        System.out.println(readUrlData(new URL(urlBase + "/health")));

        metricsService.stopExporter();
    }

    private static String readUrlData(URL url) throws IOException {
        System.out.println("URL: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        if (connection.getResponseCode() == 404) {
            return "<<NOT FOUND>>";
        }
        final InputStream data = connection.getInputStream();
        byte[] buf = new byte[1000];
        ByteArrayOutputStream agg = new ByteArrayOutputStream();

        while (true) {
            int bytesRead = data.read(buf);
            if (bytesRead == -1) {
                break;
            }
            agg.write(buf, 0, bytesRead);
        }

        return agg.toString(StandardCharsets.UTF_8.toString());
    }

}