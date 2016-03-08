package net.dv8tion.jda.player;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Austin on 3/6/2016.
 */
public class AudioSource
{
    //Defined at the bottom of this file.
    public static final List<String> YOUTUBE_DL_LAUNCH_ARGS =
            Collections.unmodifiableList(Arrays.asList(
                    "python",               //Launch python executor
                    "./youtube-dl",         //youtube-dl program file
                    "-q",                   //quiet. No standard out.
                    "-f", "bestaudio/best", //Format to download. Attempts best audio-only, followed by best video/audio combo
                    "--no-playlist",        //If the provided link is part of a Playlist, only grabs the video, not playlist too.
                    "-o", "-"               //Output, output to STDout
            ));
    public static final List<String> FFMPEG_LAUNCH_ARGS =
            Collections.unmodifiableList(Arrays.asList(
                    "ffmpeg",       //Program launch
                    "-i", "-",      //Input file, specifies to read from STDin (pipe)
                    "-f", "mp3",    //Format, type MP3
                    "-map", "a",    //Makes sure to only output audio, even if the specified format supports other streams
                    "-"             //Used to specify STDout as the output location (pipe)
            ));

    private final String url;
    private final List<String> ytdlLaunchArgsF;
    private final List<String> ffmpegLaunchArgsF;

    public AudioSource(String url)
    {
        this(url, null, null);
    }

    public AudioSource(String url, List<String> ytdlLaunchArgs, List<String> ffmpegLaunchArgs)
    {
        if (url == null || url.isEmpty())
            throw new NullPointerException("String url provided to AudioSource was null or empty.");
        this.url = url;
        this.ytdlLaunchArgsF = ytdlLaunchArgs;
        this.ffmpegLaunchArgsF = ffmpegLaunchArgs;
    }

    public String getUrl()
    {
        return url;
    }

    public InputStream asStream()
    {
        List<String> ytdlLaunchArgs = new ArrayList<>();
        List<String> ffmpegLaunchArgs = new ArrayList<>();
        if (ytdlLaunchArgsF == null)
            ytdlLaunchArgs.addAll(YOUTUBE_DL_LAUNCH_ARGS);
        else
            ytdlLaunchArgs.addAll(ytdlLaunchArgsF);

        if (ffmpegLaunchArgsF == null)
            ffmpegLaunchArgs.addAll(FFMPEG_LAUNCH_ARGS);
        else
            ffmpegLaunchArgs.addAll(ytdlLaunchArgsF);

        ytdlLaunchArgs.add(url);    //specifies the URL to download.

        return new AudioStream(url,ytdlLaunchArgs, ffmpegLaunchArgs);
    }

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
        // Thread#interrupt() when an instance of AudioSource is in an async thread.
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
            e.printStackTrace();
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            try
            {
                fos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return file;
    }
}
