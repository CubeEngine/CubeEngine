package org.cubeengine.service.matcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ServiceProvider;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.user.UserStorageService;

import static java.util.stream.Collectors.toList;

@ServiceProvider(UserMatcher.class)
public class UserMatcher
{
    @Inject private StringMatcher sm;

    public Optional<User> match(String name, boolean searchOffline)
    {
        Game game = Sponge.getGame();
        if (name == null)
        {
            return null;
        }
        // Direct Match Online Players:
        Optional<Player> player = game.getServer().getPlayer(name);
        if (player.isPresent())
        {
            return Optional.of(player.get());
        }

        // Find Online Players with similar name
        Map<String, Player> onlinePlayerMap = new HashMap<>();
        for (Player onlineUser : game.getServer().getOnlinePlayers())
        {
            onlinePlayerMap.put(onlineUser.getName(), onlineUser);
        }
        String foundUser = sm.matchString(name, onlinePlayerMap.keySet());
        if (foundUser != null)
        {
            return Optional.of(onlinePlayerMap.get(foundUser));
        }

        UserStorageService storage = game.getServiceManager().provideUnchecked(UserStorageService.class);
        Optional<User> directMatchOffline = storage.get(name);
        if (directMatchOffline.isPresent())
        {
            return directMatchOffline;
        }

        if (searchOffline)
        {
            String match = sm.matchString(name, storage.getAll().stream().map(GameProfile::getName).collect(toList()));
            if (match != null)
            {
                return storage.get(match);
            }
        }

        return Optional.empty();
    }
}
