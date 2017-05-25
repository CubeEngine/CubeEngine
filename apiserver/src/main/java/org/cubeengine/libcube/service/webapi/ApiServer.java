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
package org.cubeengine.libcube.service.webapi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.logscribe.LogFactory;
import de.cubeisland.engine.logscribe.target.file.AsyncFileTarget;
import de.cubeisland.engine.modularity.asm.marker.ServiceProvider;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.util.LoggerUtil;
import org.cubeengine.reflect.Reflector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.cubeengine.module.authorization.Authorization;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.logging.LoggingUtil;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.service.webapi.exception.ApiStartupException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Locale.ENGLISH;
import static org.cubeengine.libcube.util.StringUtils.deCamelCase;

/**
 * This class represents the API server and provides methods to configure and control it
 */
@ServiceProvider(ApiServer.class)
public class ApiServer
{
    private final Log log;
    private final AtomicInteger maxContentLength = new AtomicInteger(1048576);
    private final AtomicBoolean compress = new AtomicBoolean(false);
    private final AtomicInteger compressionLevel = new AtomicInteger(9);
    private final AtomicInteger windowBits = new AtomicInteger(15);
    private final AtomicInteger memoryLevel = new AtomicInteger(9);
    private final AtomicReference<InetAddress> bindAddress = new AtomicReference<>(null);
    private final AtomicInteger port = new AtomicInteger(6561);
    private final AtomicReference<ServerBootstrap> bootstrap = new AtomicReference<>(null);
    private final AtomicReference<EventLoopGroup> eventLoopGroup = new AtomicReference<>(null);
    private final AtomicReference<Channel> channel = new AtomicReference<>(null);
    private final AtomicInteger maxThreads = new AtomicInteger(2);
    private final Set<String> disabledRoutes = new CopyOnWriteArraySet<>();
    private final AtomicBoolean enableWhitelist = new AtomicBoolean(false);
    private final Set<InetAddress> whitelist = new CopyOnWriteArraySet<>();
    private final AtomicBoolean enableBlacklist = new AtomicBoolean(false);
    private final Set<InetAddress> blacklist = new CopyOnWriteArraySet<>();
    private final AtomicBoolean enableAuthorizedList = new AtomicBoolean(false);
    private final Set<InetAddress> authorizedList = new CopyOnWriteArraySet<>();

    private final ConcurrentMap<String, ApiHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<WebSocketRequestHandler>> subscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger maxConnectionCount = new AtomicInteger(1);

    @Inject private Reflector reflector;
    @Inject private Path moduleFolder;
    @Inject private Log logger;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private TaskManager tm;
    @Inject private PermissionManager pm;
    @Inject private ThreadFactory tf;
    @Inject private Maybe<Authorization> am;
    @Inject private Modularity modularity;

    @Inject
    public ApiServer(LogFactory logFactory, FileManager fm, ThreadFactory tf)
    {
        this.log = logFactory.getLog(ApiServer.class, "WebAPI");
        this.log.addTarget(new AsyncFileTarget(LoggingUtil.getLogFile(fm, "WebAPI"),
                                                  LoggingUtil.getFileFormat(true, true),
                                                  true, LoggingUtil.getCycler(),
                                                  tf));
        // TODO this.log.addTarget(new LogProxyTarget(logFactory.getParent()));
        try
        {
            this.bindAddress.set(InetAddress.getLocalHost());
        }
        catch (UnknownHostException ignored)
        {
            this.log.warn("Failed to get the localhost!");
        }
    }

    @Enable
    public void onEnable()
    {
        reflector.getDefaultConverterManager().registerConverter(new InetAddressConverter(), InetAddress.class);

        this.configure(reflector.load(ApiConfig.class, moduleFolder.resolve("webapi.yml").toFile()));

        try
        {
            this.start();
            final ObjectMapper mapper = new ObjectMapper();
            LoggerUtil.attachCallback(null, (f, a, t, msg) -> {
                ObjectNode node = mapper.createObjectNode();
                node.put("msg", msg);
                this.fireEvent("console", node);
            });
        }
        catch (ApiStartupException ex)
        {
            this.logger.error(ex, "The web API will not be available as the server failed to start properly...");
        }

        this.registerApiHandlers(ApiServer.class, new CommandController(i18n, tm, cm));
    }

    public Log getLog()
    {
        return this.log;
    }


    public void configure(final ApiConfig config)
    {
        checkNotNull(config, "The config must not be null!");

        try
        {
            this.setBindAddress(config.network.address);
        }
        catch (UnknownHostException ignored)
        {
            this.log.warn("Failed to resolve the host {}, ignoring the value...");
        }
        this.setPort(config.network.port);
        this.setMaxThreads(config.network.maxThreads);
        this.setMaxConnectionCount(config.network.maxConnectionPerIp);

        this.setCompressionEnabled(config.compression.enable);
        this.setCompressionLevel(config.compression.level);
        this.setCompressionWindowBits(config.compression.windowBits);
        this.setCompressionMemoryLevel(config.compression.memoryLevel);

        this.setWhitelistEnabled(config.whitelist.enable);
        this.setWhitelist(config.whitelist.ips);

        this.setBlacklistEnabled(config.blacklist.enable);
        this.setBlacklist(config.blacklist.ips);

        this.setAuthorizedListEnabled(config.authorizedList.enable);
        this.setAuthorizedList(config.authorizedList.ips);
    }

    /**
     * Starts the server
     *
     * @return fluent interface
     */
    public ApiServer start() throws ApiStartupException
    {
        if (!this.isRunning())
        {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();

            try
            {
                this.eventLoopGroup.set(new NioEventLoopGroup(this.maxThreads.get(),
                                  tf));
                serverBootstrap.group(this.eventLoopGroup.get())
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ApiServerInitializer(cm, am, this))
                    .localAddress(this.bindAddress.get(), this.port.get());

                this.bootstrap.set(serverBootstrap);
                this.channel.set(serverBootstrap.bind().sync().channel());
            }
            catch (Exception e)
            {
                this.bootstrap.set(null);
                this.channel.set(null);
                this.eventLoopGroup.getAndSet(null).shutdownGracefully(2, 5, TimeUnit.SECONDS);
                throw new ApiStartupException("The API server failed to start!", e);
            }
        }
        return this;
    }

    /**
     * Stops the server
     *
     * @return fluent interface
     */
    public ApiServer stop() // TODO shutdown service + unregisterApiHandlers
    {
        if (this.isRunning())
        {
            this.bootstrap.set(null);
            this.channel.set(null);
            this.eventLoopGroup.getAndSet(null).shutdownGracefully(2, 5, TimeUnit.SECONDS);
        }
        return this;
    }

    /**
     * Returns whether the server is running or not
     *
     * @return true if it is running
     */
    public boolean isRunning()
    {
        return (this.channel.get() != null && this.channel.get().isOpen());
    }

    public ApiHandler getApiHandler(String route)
    {
        if (route == null)
        {
            return null;
        }
        return this.handlers.get(route);
    }

    public void registerApiHandlers(final Class owner, final Object holder)
    {
        checkNotNull(holder, "The API holder must not be null!");

        for (Method method : holder.getClass().getDeclaredMethods())
        {
            Action aAction = method.getAnnotation(Action.class);
            if (aAction != null)
            {
                String route = aAction.value();
                if (route.isEmpty())
                {
                    route = deCamelCase(method.getName(), "/");
                }
                DependencyInformation info = modularity.getLifecycle(owner).getInformation();
                if (info instanceof ModuleMetadata)
                {
                    route = deCamelCase(((ModuleMetadata)info).getName().toLowerCase(), "-") + "/" + route;;
                }
                else
                {
                    route = deCamelCase(owner.getSimpleName().toLowerCase(), "-") + "/" + route;
                }
                route = HttpRequestHandler.normalizePath(route);
                String perm = null;
                if (aAction.needsAuth())
                {
                    perm = pm.getBasePermission(owner).getId() + ".webapi.";
                    if (method.isAnnotationPresent(ApiPermission.class))
                    {
                        ApiPermission apiPerm = method.getAnnotation(ApiPermission.class);
                        if (apiPerm.value().isEmpty())
                        {
                            perm = perm + route;
                        }
                        else
                        {
                            perm = perm + apiPerm.value();
                        }
                    }
                    else
                    {
                        perm = perm + route;
                    }
                }
                LinkedHashMap<String, Class> params = new LinkedHashMap<>();
                Class<?>[] types = method.getParameterTypes();
                Annotation[][] paramAnnotations = method.getParameterAnnotations();
                for (int i = 1; i < types.length; i++)
                {
                    Class<?> type = types[i];
                    Value val = null;
                    for (Annotation annotation : paramAnnotations[i])
                    {
                        if (annotation instanceof Value)
                        {
                            val = (Value)annotation;
                            break;
                        }
                    }
                    if (val == null)
                    {
                        throw new IllegalArgumentException("Missing Value Annotation for Additional Parameters");
                    }
                    if (params.put(val.value(), type) != null)
                    {
                        throw new IllegalArgumentException("Duplicate value in Value Annotation");
                    }
                }
                RequestMethod reqMethod = RequestMethod.GET;
                if (method.isAnnotationPresent(org.cubeengine.libcube.service.webapi.Method.class))
                {
                    reqMethod = method.getAnnotation(org.cubeengine.libcube.service.webapi.Method.class).value();
                }
                this.handlers.put(route, new ReflectedApiHandler(owner, route, perm, params, reqMethod, method, holder,
                                                                 cm));
            }
        }
    }

    public void unregisterApiHandler(String route)
    {
        this.handlers.remove(route);
    }

    public void unregisterApiHandlers(Class module)
    {
        Iterator<Map.Entry<String, ApiHandler>> iter = this.handlers.entrySet().iterator();

        ApiHandler handler;
        while (iter.hasNext())
        {
            handler = iter.next().getValue();
            if (handler.getOwner() == module)
            {
                iter.remove();
            }
        }
    }

    public void unregisterApiHandlers(Object holder)
    {
        Iterator<Map.Entry<String, ApiHandler>> iter = this.handlers.entrySet().iterator();

        ApiHandler handler;
        while (iter.hasNext())
        {
            handler = iter.next().getValue();
            if (handler instanceof ReflectedApiHandler)
            {
                if (((ReflectedApiHandler)handler).getHolder() == holder)
                {
                    iter.remove();
                }
            }
        }
    }

    public void unregisterApiHandlers()
    {
        this.handlers.clear();
    }

    public void setBindAddress(String address) throws UnknownHostException
    {
        this.setBindAddress(InetAddress.getByName(address));
    }

    /**
     * Sets the address the server will bind to on the next start
     *
     * @param address the address
     */
    public void setBindAddress(InetAddress address)
    {
        checkNotNull(address, "The address must not be null!");

        this.bindAddress.set(address);
    }

    /**
     * Returns the address the server is bound/will bind to
     *
     * @return the address
     */
    public InetAddress getBindAddress()
    {
        return this.bindAddress.get();
    }

    public InetAddress getBoundAddress()
    {
        if (this.isRunning())
        {
            return ((InetSocketAddress)this.channel.get().localAddress()).getAddress();
        }
        return null;
    }

    public void setPort(short port)
    {
        this.port.set(port);
    }

    /**
     * Returns the port the server is/will be listening on
     *
     * @return the post
     */
    public int getPort()
    {
        return this.port.get();
    }

    public short getBoundPort()
    {
        if (this.isRunning())
        {
            return (short)((InetSocketAddress)this.channel.get().localAddress()).getPort();
        }
        return -1;
    }

    public void setMaxThreads(int maxThreads)
    {
        this.maxThreads.set(maxThreads);
    }

    public int getMaxThreads()
    {
        return this.maxThreads.get();
    }

    public void setMaxContentLength(int mcl)
    {
        this.maxContentLength.set(mcl);
    }

    /**
     * Returns the maximum content length the client may send
     *
     * @return the maximum content length
     */
    public int getMaxContentLength()
    {
        return this.maxContentLength.get();
    }

    public void setCompressionEnabled(boolean state)
    {
        this.compress.set(state);
    }

    public boolean isCompressionEnabled()
    {
        return this.compress.get();
    }

    public void setCompressionLevel(int level)
    {
        this.compressionLevel.set(Math.max(1, Math.min(9, level)));
    }

    public int getCompressionLevel()
    {
        return this.compressionLevel.get();
    }

    public void setCompressionWindowBits(int bits)
    {
        this.windowBits.set(Math.max(9, Math.min(15, bits)));
    }

    public int getCompressionMemoryLevel()
    {
        return this.memoryLevel.get();
    }

    public void setCompressionMemoryLevel(int level)
    {
        this.memoryLevel.set(Math.max(1, Math.min(9, level)));
    }

    public int getCompressionWindowBits()
    {
        return this.windowBits.get();
    }

    /**
     * Returns whether whitelisting is enabled
     *
     * @return true if enabled
     */
    public boolean isWhitelistEnabled()
    {
        return this.enableWhitelist.get();
    }

    /**
     * Sets the enabled state of the whitelisting
     *
     * @param state true to enable, false to disable
     */
    public void setWhitelistEnabled(boolean state)
    {
        this.enableWhitelist.set(state);
    }

    public void whitelistAddress(InetAddress address)
    {
        this.whitelist.add(address);
    }

    public void whitelistAddress(InetSocketAddress address)
    {
        this.whitelistAddress(address.getAddress());
    }

    public void unWhitelistAddress(InetAddress address)
    {
        this.whitelist.remove(address);
    }

    public void unWhitelistAddress(InetSocketAddress address)
    {
        this.unWhitelistAddress(address.getAddress());
    }

    public void setWhitelist(Set<InetAddress> newWhitelist)
    {
        checkNotNull(newWhitelist, "The whitelist must not be null!");
        // TODO Validate.noNullElements(newWhitelist, "The whitelist must not contain null values!");

        this.whitelist.clear();
        this.whitelist.addAll(newWhitelist);
    }

    /**
     * Checks whether an InetAddress is whitelisted
     *
     * @param ip the IP
     * @return true if it is
     */
    public boolean isWhitelisted(InetAddress ip)
    {
        return !this.enableWhitelist.get() || this.whitelist.contains(ip);
    }


    public boolean isAuthorized(InetAddress ip)
    {
        return !this.enableAuthorizedList.get() || this.authorizedList.contains(ip);
    }

    /**
     * Checks whether an InetSocketAddress is whitelisted
     *
     * @param ip the IP
     * @return true if it is
     */
    public boolean isWhitelisted(InetSocketAddress ip)
    {
        return this.isWhitelisted(ip.getAddress());
    }

    /**
     * Sets the enabled state of the blacklisting
     *
     * @param state true to enable, false to disable
     */
    public void setBlacklistEnabled(boolean state)
    {
        this.enableBlacklist.set(state);
    }

    public void blacklistAddress(InetAddress address)
    {
        this.blacklist.add(address);
    }

    public void blacklistAddress(InetSocketAddress address)
    {
        this.blacklistAddress(address.getAddress());
    }

    public void unBlacklistAddress(InetAddress address)
    {
        this.blacklist.remove(address);
    }

    public void unBlacklistAddress(InetSocketAddress address)
    {
        this.unBlacklistAddress(address.getAddress());
    }

    public void setBlacklist(Set<InetAddress> newBlacklist)
    {
        checkNotNull(newBlacklist, "The blacklist must not be null!");
        // TODO Validate.noNullElements(newBlacklist, "The blacklist must not contain null values!");

        this.blacklist.clear();
        this.blacklist.addAll(newBlacklist);
    }

    /**
     * Returns whether blacklisting is enabled
     *
     * @return true if it is
     */
    public boolean isBlacklistEnabled()
    {
        return this.enableBlacklist.get();
    }

    /**
     * Checks whether an InetSocketAddress is blacklisted
     *
     * @param ip the IP
     * @return true if it is
     */
    public boolean isBlacklisted(InetSocketAddress ip)
    {
        return this.isBlacklisted(ip.getAddress());
    }

    /**
     * Checks whether an InetAddress is blacklisted
     *
     * @param ip the IP
     * @return true if it is
     */
    public boolean isBlacklisted(InetAddress ip)
    {
        return this.enableBlacklist.get() && this.blacklist.contains(ip);
    }

    public void setAuthorizedListEnabled(boolean enable)
    {
        this.enableAuthorizedList.set(enable);
    }

    public void setAuthorizedList(Set<InetAddress> newAuthorizedlist)
    {
        checkNotNull(newAuthorizedlist, "The autorizedlist must not be null!");
        // TODO Validate.noNullElements(newAuthorizedlist, "The autorizedlist must not contain null values!");

        this.authorizedList.clear();
        this.authorizedList.addAll(newAuthorizedlist);
    }

    public boolean isRouteDisabled(String route)
    {
        return this.disabledRoutes.contains(route);
    }

    public void disableRoute(String route)
    {
        this.disabledRoutes.add(route);
    }

    public void reenableRoute(String route)
    {
        this.disabledRoutes.remove(route);
    }

    public void reenableRoutes(String... routes)
    {
        for (String route : routes)
        {
            this.reenableRoute(route);
        }
    }

    public boolean isAddressAccepted(InetAddress address)
    {
        return this.isWhitelisted(address) && !this.isBlacklisted(address);
    }

    public void subscribe(String event, WebSocketRequestHandler requestHandler)
    {
        checkNotNull(event, "The event name must not be null!");
        checkNotNull(requestHandler, "The request handler must not be null!");
        event = event.toLowerCase(ENGLISH);

        Set<WebSocketRequestHandler> subscribedHandlers = this.subscriptions.get(event);
        if (subscribedHandlers == null)
        {
            this.subscriptions.put(event, subscribedHandlers = new CopyOnWriteArraySet<>());
        }
        subscribedHandlers.add(requestHandler);
    }

    public void unsubscribe(String event, WebSocketRequestHandler requestHandler)
    {
        checkNotNull(event, "The event name must not be null!");
        checkNotNull(requestHandler, "The request handler must not be null!");
        event = event.toLowerCase(ENGLISH);

        Set<WebSocketRequestHandler> subscribedHandlers = this.subscriptions.get(event);
        if (subscribedHandlers != null)
        {
            subscribedHandlers.remove(requestHandler);
            if (subscribedHandlers.isEmpty())
            {
                this.subscriptions.remove(event);
            }
        }
    }

    public void unsubscribe(String event)
    {
        checkNotNull(event, "The event name must not be null!");
        event = event.toLowerCase(ENGLISH);

        this.subscriptions.remove(event);
    }

    public void unsubscribe(WebSocketRequestHandler handler)
    {
        checkNotNull(handler, "The event name must not be null!");

        Iterator<Map.Entry<String, Set<WebSocketRequestHandler>>> iter = this.subscriptions.entrySet().iterator();

        Set<WebSocketRequestHandler> handlers;
        while (iter.hasNext())
        {
            handlers = iter.next().getValue();
            handlers.remove(handler);
            if (handlers.isEmpty())
            {
                iter.remove();
            }
        }
    }

    public void fireEvent(String event, ObjectNode data)
    {
        checkNotNull(event, "The event name must not be null!");
        event = event.toLowerCase(ENGLISH);

        Set<WebSocketRequestHandler> subscribedHandlers = this.subscriptions.get(event);
        if (subscribedHandlers != null)
        {
            for (WebSocketRequestHandler handler : subscribedHandlers)
            {
                handler.handleEvent(event, data);
            }
        }
    }

    public void setMaxConnectionCount(int maxConnectionCount)
    {
        this.maxConnectionCount.set(maxConnectionCount);
    }

    public int getMaxConnectionCount()
    {
        return maxConnectionCount.get();
    }
}
