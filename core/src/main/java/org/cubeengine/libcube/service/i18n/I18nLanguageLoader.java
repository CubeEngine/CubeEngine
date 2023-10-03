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
package org.cubeengine.libcube.service.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.cubeengine.i18n.language.DefinitionLoadingException;
import org.cubeengine.i18n.language.LanguageDefinition;
import org.cubeengine.i18n.language.LanguageLoader;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.service.filesystem.FileManager;

import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

public class I18nLanguageLoader extends LanguageLoader
{
    private final Map<Locale, LocaleConfiguration> configurations = new HashMap<>();
    private final Reflector reflector;
    private Logger log;

    public I18nLanguageLoader(Reflector reflector, FileManager fm, Logger log)
    {
        this.reflector = reflector;
        this.log = log;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fm.getLanguagePath(), YAML))
        {
            // Search override Languages under CubeEngine/languages
            for (Path path : directoryStream)
            {
                LocaleConfiguration config = reflector.load(LocaleConfiguration.class, path.toFile(), false);
                this.configurations.put(config.getLocale(), config);
                Locale[] clones = config.getClones();
                if (clones != null)
                {
                    for (Locale clone : clones)
                    {
                        this.configurations.put(clone, config);
                    }
                }
            }
        }
        catch (IOException ex)
        {
            log.error("Failed to load language configurations!", ex);
        }
    }

    protected void loadLanguage(InputStream languageFile) throws IOException
    {
        try (Reader reader = new InputStreamReader(languageFile))
        {
            LocaleConfiguration config = reflector.load(LocaleConfiguration.class, reader);
            if (!this.configurations.containsKey(config.getLocale()))
            {
                this.configurations.put(config.getLocale(), config);
            }
            Locale[] clones = config.getClones();
            if (clones != null)
            {
                for (Locale clone : clones)
                {
                    if (!this.configurations.containsKey(clone))
                    {
                        this.configurations.put(clone, config);
                    }
                }
            }
        }
    }

    @Override
    public LanguageDefinition loadDefinition(Locale locale) throws DefinitionLoadingException
    {
        return this.configurations.get(locale);
    }
}
