/*
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

import org.json.JSONException;
import org.json.JSONObject;
import sun.misc.IOUtils;

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
                "-ac", "2",     //Channels. Specify 2 for stereo audio.
                "-ar", "48000", //Rate. Opus requires an audio rate of 48000hz
                "-map", "a",    //Makes sure to only output audio, even if the specified format supports other streams
                "-"             //Used to specify STDout as the output location (pipe)
        ));
    public static final List<String> FFPROBE_INFO_ARGS =
            Collections.unmodifiableList(Arrays.asList(
                    "ffprobe",
                    "-show_format",
                    "-print_format", "json",
                    "-loglevel", "0"
            ));

    private File file;
    private AudioInfo audioInfo;

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
    public synchronized AudioInfo getInfo()
    {
        if (audioInfo != null)
            return audioInfo;

        audioInfo = new AudioInfo();
        try
        {
            List<String> infoArgs = new LinkedList<>();
            infoArgs.addAll(FFPROBE_INFO_ARGS);
            infoArgs.add("-i");
            infoArgs.add(file.getCanonicalPath());

            Process infoProcess = new ProcessBuilder().command(infoArgs).start();
            byte[] infoData = IOUtils.readFully(infoProcess.getInputStream(), -1, false);
            if (infoData == null || infoData.length == 0)
                throw new NullPointerException("The FFprobe process resulted in a null or zero-length INFO!");

            System.out.println(new String(infoData));

            JSONObject info = new JSONObject(new String(infoData));
            JSONObject format = info.getJSONObject("format");
            JSONObject tags = format.optJSONObject("tags");

            audioInfo.jsonInfo = info;
            audioInfo.origin = file.getCanonicalPath();
            audioInfo.extractor = "LocalSource";

            if (tags != null)
            {
                audioInfo.title = !tags.optString("title", "").isEmpty()
                        ? tags.getString("title")
                        : null;

                audioInfo.description =
                    "Title: " + (tags.has("title") ? tags.getString("title") : "N/A") + "\n" +
                    "Artist: " + (tags.has("artist") ? tags.getString("artist") : "N/A") + "\n" +
                    "Album: " + (tags.has("album") ? tags.getString("album") : "N/A") + "\n" +
                    "Genre: " + (tags.has("genre") ? tags.getString("genre") : "N/A") + "\n";
            }
            audioInfo.encoding = !format.optString("format_name", "").isEmpty()
                    ? format.getString("format_name")
                    : !format.optString("format_long_name", "").isEmpty()
                    ? format.getString("format_long_name")
                    : null;
            audioInfo.duration = format.has("duration")
                    ? AudioTimestamp.fromSeconds((int) format.getDouble("duration"))
                    : null;

        }
        catch (IOException e)
        {
            audioInfo.error = e.getMessage();
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            audioInfo.error = e.getMessage();
            e.printStackTrace();
        }
        return audioInfo;
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
            return new LocalStream(ffmpegLaunchArgs);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public File asFile(String path, boolean deleteOnExists) throws FileAlreadyExistsException, FileNotFoundException
    {
        return null;
    }
}
