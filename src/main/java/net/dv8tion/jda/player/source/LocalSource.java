/**
 *     Copyright 2016 Austin Keener
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.player.source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LocalSource implements AudioSource
{
    public static final List<String> FFMPEG_LAUNCH_ARGS =
        Collections.unmodifiableList(Arrays.asList(
                "ffmpeg",       //Program launch
                "-f", "s16be",  //Format.  PCM, signed, 16bit, Big Endian
                "-map", "a",    //Makes sure to only output audio, even if the specified format supports other streams
                "-"             //Used to specify STDout as the output location (pipe)
        ));

    private File file;

    public LocalSource(File file)
    {
        if (file == null)
            throw new IllegalArgumentException("Provided file was null!");
        if (!file.exists())
            throw new IllegalArgumentException("Provided file does not exist!");
        if (file.isDirectory())
            throw new IllegalArgumentException("Provided file is actually a directory. Must provide a file!");
        if (!file.canRead())
            throw new IllegalArgumentException("Provided file is unreadable due to a lack of permissions");

        this.file = file;
    }

    @Override
    public String getSource()
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AudioInfo getInfo()
    {
        return null;
    }

    @Override
    public AudioStream asStream()
    {
        List<String> ffmpegLaunchArgs = new LinkedList<>();
        ffmpegLaunchArgs.addAll(FFMPEG_LAUNCH_ARGS);
        try
        {
            ffmpegLaunchArgs.add("-i");
            ffmpegLaunchArgs.add(file.getCanonicalPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        return new LocalStream(ffmpegLaunchArgs);
    }

    @Override
    public File asFile(String path, boolean deleteOnExists) throws FileAlreadyExistsException, FileNotFoundException
    {
        return null;
    }
}
