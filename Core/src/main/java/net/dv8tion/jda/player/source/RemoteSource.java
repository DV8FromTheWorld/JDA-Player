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

import net.dv8tion.jda.player.AbstractMusicPlayer;
import org.json.JSONException;
import org.json.JSONObject;
import sun.misc.IOUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;

public class RemoteSource implements AudioSource
{
    public static final List<String> YOUTUBE_DL_LAUNCH_ARGS =
            Collections.unmodifiableList(Arrays.asList(
                    "python",               //Launch python executor
                    "./youtube-dl",         //youtube-dl program file
                    "--no-part",            //Do not use .part files (--Frag?)
                    "-q",                   //quiet. No standard out.
                    "-f", "bestaudio/best", //Format to download. Attempts best audio-only, followed by best video/audio combo
                    "--no-playlist",        //If the provided link is part of a Playlist, only grabs the video, not playlist too.
                    "-4",                   //Forcing Ipv4 for OVH's Ipv6 range is blocked by youtube
                    "--no-cache-dir",       //We don't want random screaming
                    "-o", "-"               //Output, output to STDout
            ));
    public static final List<String> FFMPEG_LAUNCH_ARGS =
            Collections.unmodifiableList(Arrays.asList(
                    "ffmpeg",       //Program launch
                    "-i", "-",      //Input file, specifies to read from STDin (pipe)
                    "-f", "s16be",  //Format.  PCM, signed, 16bit, Big Endian
                    "-ac", "2",     //Channels. Specify 2 for stereo audio.
                    "-ar", "48000", //Rate. Opus requires an audio rate of 48000hz
                    "-map", "a",    //Makes sure to only output audio, even if the specified format supports other streams
                    "-"             //Used to specify STDout as the output location (pipe)
            ));

    private final String url;
    private final List<String> ytdlLaunchArgsF;
    private final List<String> ffmpegLaunchArgsF;
    private AudioInfo audioInfo;

    public RemoteSource(String url)
    {
        this(url, null, null);
    }

    public RemoteSource(String url, List<String> ytdlLaunchArgs, List<String> ffmpegLaunchArgs)
    {
        if (url == null || url.isEmpty())
            throw new NullPointerException("String url provided to RemoteSource was null or empty.");
        this.url = url;
        this.ytdlLaunchArgsF = ytdlLaunchArgs;
        this.ffmpegLaunchArgsF = ffmpegLaunchArgs;
    }

    public String getSource()
    {
        return url;
    }

    @Override
    public synchronized AudioInfo getInfo()
    {
        if (audioInfo != null)
            return audioInfo;

        List<String> infoArgs = new LinkedList<>();
        if (ytdlLaunchArgsF != null)
        {
            infoArgs.addAll(ytdlLaunchArgsF);
            if (!infoArgs.contains("-q"))
                infoArgs.add("-q");
        }
        else
            infoArgs.addAll(YOUTUBE_DL_LAUNCH_ARGS);

        infoArgs.add("--ignore-errors");    //Ignore errors, obviously
        infoArgs.add("--no-warnings");      //Ignore warnings, because they ruin our JSON
        infoArgs.add("-j");                 //Dumps the json about the file into STDout
        infoArgs.add("--skip-download");    //Doesn't actually download the file.
        infoArgs.add("--");                 //Url separator. Deals with YT ids that start with --
        infoArgs.add(url);                  //specifies the URL to download.

        audioInfo = new AudioInfo();
        Process ytdlProcess = null;
        Process ffprobeProcess = null;
        InputStream ytdlStream = null;
        InputStream ffprobeStream = null;
        try
        {
            ytdlProcess = new ProcessBuilder().command(infoArgs).start();
            ytdlStream = ytdlProcess.getErrorStream();
            byte[] infoData = IOUtils.readFully(ytdlStream, -1, false);   //YT-DL outputs to STDerr
            if (infoData == null || infoData.length == 0)
                throw new NullPointerException("The Youtube-DL process resulted in a null or zero-length INFO!");

            String infoString = new String(infoData);
            if (infoString.startsWith("ERROR"))
            {
                audioInfo.error = infoString;
            }
            else
            {
                JSONObject info = new JSONObject(infoString);

                audioInfo.jsonInfo = info;
                audioInfo.title = !info.optString("title", "").isEmpty()
                        ? info.getString("title")
                        : !info.optString("fulltitle", "").isEmpty()
                        ? info.getString("fulltitle")
                        : null;
                audioInfo.origin = !info.optString("webpage_url", "").isEmpty()
                        ? info.getString("webpage_url")
                        : url;
                audioInfo.id = !info.optString("id", "").isEmpty()
                        ? info.getString("id")
                        : null;
                audioInfo.encoding = !info.optString("acodec", "").isEmpty()
                        ? info.getString("acodec")
                        : !info.optString("ext", "").isEmpty()
                        ? info.getString("ext")
                        : null;
                audioInfo.description = !info.optString("description", "").isEmpty()
                        ? info.getString("description")
                        : null;
                audioInfo.extractor = !info.optString("extractor", "").isEmpty()
                        ? info.getString("extractor")
                        : !info.optString("extractor_key").isEmpty()
                        ? info.getString("extractor_key")
                        : null;
                audioInfo.thumbnail = !info.optString("thumbnail", "").isEmpty()
                        ? info.getString("thumbnail")
                        : null;
                audioInfo.isLive = info.has("is_live") && !info.isNull("is_live") && info.getBoolean("is_live");
                audioInfo.duration = info.optInt("duration", -1) != -1
                        ? AudioTimestamp.fromSeconds(info.getInt("duration"))
                        : null;

                //Use FFprobe to find the duration because YT-DL didn't give it to us.
                if (audioInfo.duration == null)
                {
                    List<String> ffprobeInfoArgs = new LinkedList<>();
                    ffprobeInfoArgs.addAll(LocalSource.FFPROBE_INFO_ARGS);
                    ffprobeInfoArgs.add("-i");
                    ffprobeInfoArgs.add(info.optString("url", url));

                    ffprobeProcess = new ProcessBuilder().command(ffprobeInfoArgs).start();
                    ffprobeStream = ffprobeProcess.getInputStream();
                    infoData = IOUtils.readFully(ffprobeStream, -1, false);
                    if (infoData != null && infoData.length > 0)
                    {
                        info = new JSONObject(new String(infoData)).getJSONObject("format");

                        if (info.optDouble("duration", -1.0) != -1.0)
                        {
                            int duration = Math.round((float) info.getDouble("duration"));
                            audioInfo.duration = AudioTimestamp.fromSeconds(duration);
                        }
                    }
                }
            }
        }
        catch (IOException | JSONException e)
        {
            audioInfo.error = e.getMessage();
            AbstractMusicPlayer.LOG.log(e);
        }
        finally
        {
            try
            {
                if (ytdlProcess != null)
                    ytdlProcess.destroyForcibly();
            }
            catch (Throwable ignored) {}
            try
            {
                if (ytdlStream != null)
                    ytdlStream.close();
            }
            catch (Throwable ignored) {}
            try
            {
                if (ffprobeProcess != null)
                    ffprobeProcess.destroyForcibly();
            }
            catch (Throwable ignored) {}
            try
            {
                if (ffprobeStream != null)
                    ffprobeStream.close();;
            }
            catch (Throwable ignored) {}
        }
        return audioInfo;
    }

    @Override
    public AudioStream asStream()
    {
        List<String> ytdlLaunchArgs = new ArrayList<>();
        List<String> ffmpegLaunchArgs = new ArrayList<>();
        if (ytdlLaunchArgsF == null)
            ytdlLaunchArgs.addAll(YOUTUBE_DL_LAUNCH_ARGS);
        else
        {
            ytdlLaunchArgs.addAll(ytdlLaunchArgsF);
            if (!ytdlLaunchArgs.contains("-q"))
                ytdlLaunchArgs.add("-q");
        }

        if (ffmpegLaunchArgsF == null)
            ffmpegLaunchArgs.addAll(FFMPEG_LAUNCH_ARGS);
        else
            ffmpegLaunchArgs.addAll(ffmpegLaunchArgsF);

        ytdlLaunchArgs.add("--");   //Url separator. Deals with YT ids that start with --
        ytdlLaunchArgs.add(url);    //specifies the URL to download.

        return new RemoteStream(ytdlLaunchArgs, ffmpegLaunchArgs);
    }

    @Override
    public File asFile(String path, boolean deleteIfExists) throws FileAlreadyExistsException, FileNotFoundException
    {
        if (path == null || path.isEmpty())
            throw new NullPointerException("Provided path was null or empty!");

        File file = new File(path);
        if (file.isDirectory())
            throw new IllegalArgumentException("The provided path is a directory, not a file!");
        if (file.exists())
        {
            if (!deleteIfExists)
            {
                throw new FileAlreadyExistsException("The provided path already has an existing file " +
                        " and the `deleteIfExists` boolean was set to false.");
            }
            else
            {
                if (!file.delete())
                    throw new UnsupportedOperationException("Cannot delete the file. Is it in use?");
            }
        }

        Thread currentThread = Thread.currentThread();
        FileOutputStream fos = new FileOutputStream(file);
        InputStream input = asStream();

        //Writes the bytes of the downloaded audio into the file.
        //Has detection to detect if the current thread has been interrupted to respect calls to
        // Thread#interrupt() when an instance of RemoteSource is in an async thread.
        //TODO: consider replacing with a Future.
        try
        {
            byte[] buffer = new byte[1024];
            int amountRead = -1;
            int i = 0;
            while (!currentThread.isInterrupted() && ((amountRead = input.read(buffer)) > -1))
            {
                fos.write(buffer, 0, amountRead);
            }
            fos.flush();
        }
        catch (IOException e)
        {
            AbstractMusicPlayer.LOG.log(e);
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                AbstractMusicPlayer.LOG.log(e);
            }

            try
            {
                fos.close();
            }
            catch (IOException e)
            {
                AbstractMusicPlayer.LOG.log(e);
            }
        }
        return file;
    }
}
