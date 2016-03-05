package net.dv8tion.jda.player;

import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.utils.SimpleLog;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by Austin on 3/4/2016.
 */
public class MyPlayer extends Player
{
    protected boolean started = false;
    protected boolean playing = false;
    protected boolean paused = false;
    protected boolean stopped = true;

    public MyPlayer(String url)
    {

        Process ytdlProcess = null;
        Process ffmpegProcess = null;
        Thread ffmpegToStdout = null;
        Thread ytdlToFFmpegThread = null;

//        String url = "https://soundcloud.com/ahsoftware/v4-kaai-yuki_demo-song02-full-ver";   //If I had you
        File ytdlFile = new File("youtube-dl");

        try
        {
            ProcessBuilder pBuilder = new ProcessBuilder();

            pBuilder.command(
                    "python",
                    ytdlFile.getCanonicalPath(),
                    "-q",   //quiet. No standard out.
                    "-f", "bestaudio/best",
                    "--no-playlist",
//                    "--yes-playlist",
                    String.format("\"%s\"", url),
                    "-o", "-"
            );
            System.out.println("Command: " + pBuilder.command());
            ytdlProcess = pBuilder.start();

            pBuilder.command(
                    "ffmpeg",
                    "-i", "-",
                    "-f", "mp3",
                    "-"
            );
            System.out.println("Command: " + pBuilder.command());
            ffmpegProcess = pBuilder.start();

            final Process ytdlProcessF = ytdlProcess;
            final Process ffmpegProcessF = ffmpegProcess;
            ytdlToFFmpegThread = new Thread()
            {
                @Override
                public void run()
                {
                    InputStream fromYTDL = null;
                    OutputStream toFFmpeg = null;
                    try
                    {
                        fromYTDL = ytdlProcessF.getInputStream();
                        if (fromYTDL == null)
                            System.out.println("fromYTDL is null");

                        toFFmpeg = ffmpegProcessF.getOutputStream();
                        if (toFFmpeg == null)
                            System.out.println("toFFmpeg is null");

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        int i = 0;
                        while (!isInterrupted() && ((amountRead = fromYTDL.read(buffer)) > -1))
                        {
                            i++;
                            System.out.println("Writing: " + amountRead + "  " + i);
                            toFFmpeg.write(buffer, 0, amountRead);
                        }
                        toFFmpeg.flush();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        System.out.println("The thread has finished?");
                        try
                        {
                            if (fromYTDL != null)
                                fromYTDL.close();

                            if (toFFmpeg != null)
                                toFFmpeg.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            };

            Thread t = new Thread()
            {
                @Override
                public void run()
                {

                    try
                    {
                    InputStream fromYTDL = null;
                    InputStream fromFFmpeg = null;

                        fromYTDL = ytdlProcessF.getErrorStream();
                        if (fromYTDL == null)
                            System.out.println("fromYTDL is null");

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        int i = 0;
                        while (!isInterrupted() && ((amountRead = fromYTDL.read(buffer)) > -1))
                        {
                            i++;
                            System.out.println("ERR YTDL: " + new String(Arrays.copyOf(buffer, amountRead)));
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            Thread t2 = new Thread()
            {
                @Override
                public void run()
                {

                    try
                    {
                        InputStream fromFFmpeg = null;

                        fromFFmpeg = ffmpegProcessF.getErrorStream();
                        if (fromFFmpeg == null)
                            System.out.println("fromYTDL is null");

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        int i = 0;
                        while (!isInterrupted() && ((amountRead = fromFFmpeg.read(buffer)) > -1))
                        {
                            i++;
                            System.out.println("ERR FFMPEG: " + new String(Arrays.copyOf(buffer, amountRead)));
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            ytdlToFFmpegThread.start();
            t.start();
            t2.start();
            setAudioSource(AudioSystem.getAudioInputStream(ffmpegProcess.getInputStream()));
        }
        catch (IOException | UnsupportedAudioFileException e)
        {
            if (ytdlToFFmpegThread != null)
                ytdlToFFmpegThread.interrupt();
            if (ffmpegProcess != null)
                ffmpegProcess.destroy();
            if (ytdlProcess != null)
                ytdlProcess.destroy();
            e.printStackTrace();
        }
    }

    @Override
    public void play()
    {
        if (started && stopped)
            throw new IllegalStateException("Cannot start a player after it has been stopped.\n" +
                    "Please use the restart method or load a new file.");
        started = true;
        playing = true;
        paused = false;
        stopped = false;
    }

    @Override
    public void pause()
    {
        playing = false;
        paused = true;
    }

    @Override
    public void stop()
    {
        System.out.println("Stop was called");
        playing = false;
        paused = false;
        stopped = true;
//        try
//        {
//            bufferedResourceStream.close();
//            resourceStream.close();
//        }
//        catch (IOException e)
//        {
//            SimpleLog.getLog("JDAPlayer").fatal("Attempted to close the URLPlayer resource stream during stop() cleanup, but hit an IOException");
//            SimpleLog.getLog("JDAPlayer").log(e);
//        }
    }

    @Override
    public void restart()
    {
//        URL oldUrl = urlOfResource;
//        try
//        {
//            bufferedResourceStream.close();
//            resourceStream.close();
//            reset();
//            setAudioUrl(oldUrl);
//            play();
//        }
//        catch (IOException e)
//        {
//            SimpleLog.getLog("JDAPlayer").fatal("Attempted to restart the URLStream playback, but something went wrong!");
//            SimpleLog.getLog("JDAPlayer").log(e);
//        }
//        catch (UnsupportedAudioFileException e)
//        {
//            SimpleLog.getLog("JDAPlayer").log(e);
//        }
    }

    @Override
    public boolean isStarted()
    {
        return started;
    }

    @Override
    public boolean isPlaying()
    {
        return playing;
    }

    @Override
    public boolean isPaused()
    {
        return paused;
    }

    @Override
    public boolean isStopped()
    {
        return stopped;
    }

    protected void reset()
    {
        started = false;
        playing = false;
        paused = false;
        stopped = true;
//        urlOfResource = null;
//        resourceStream = null;
    }
}
