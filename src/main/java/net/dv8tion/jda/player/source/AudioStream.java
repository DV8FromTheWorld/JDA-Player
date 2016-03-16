package net.dv8tion.jda.player.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Austin on 3/6/2016.
 */
public class AudioStream extends BufferedInputStream
{
    //Represent the processes that control the Python Youtube-dl and the FFmpeg program.
    private Process ytdlProcess;
    private Process ffmpegProcess;

    //Async threads that deal with the piping of the outputs of the processes.
    private Thread ytdlToFFmpegThread;
    private Thread ytdlErrGobler;
    private Thread ffmpegErrGobler;

    private String url;
    private List<String> ytdlLaunchArgs;
    private List<String> ffmpegLaunchArgs;

    protected AudioStream(String url, List<String> ytdlLaunchArgs, List<String> ffmpegLaunchArgs)
    {
        super(null);
        this.url = url;
        this.ytdlLaunchArgs = ytdlLaunchArgs;
        this.ffmpegLaunchArgs = ffmpegLaunchArgs;
        setup();
    }

    private void setup()
    {
        try
        {
            ProcessBuilder pBuilder = new ProcessBuilder();

            pBuilder.command(ytdlLaunchArgs);
            System.out.println("Command: " + pBuilder.command());
            ytdlProcess = pBuilder.start();

            pBuilder.command(ffmpegLaunchArgs);
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
                        toFFmpeg = ffmpegProcessF.getOutputStream();

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        while (!isInterrupted() && ((amountRead = fromYTDL.read(buffer)) > -1))
                        {
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
                        try
                        {
                            if (fromYTDL != null)
                                fromYTDL.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        try
                        {
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

            ytdlErrGobler = new Thread()
            {
                @Override
                public void run()
                {

                    try
                    {
                        InputStream fromYTDL = null;

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

            ffmpegErrGobler = new Thread()
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
            ytdlErrGobler.start();
            ffmpegErrGobler.start();
            this.in = ffmpegProcess.getInputStream();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            try
            {
                close();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        if (in != null)
        {
            in.close();
            in = null;
        }
        if (ytdlToFFmpegThread != null)
        {
            ytdlToFFmpegThread.interrupt();
            ytdlToFFmpegThread = null;
        }
        if (ytdlErrGobler != null)
        {
            ytdlErrGobler.interrupt();
            ytdlErrGobler = null;
        }
        if (ffmpegErrGobler != null)
        {
            ffmpegErrGobler.interrupt();
            ffmpegErrGobler = null;
        }
        if (ffmpegProcess != null)
        {
            ffmpegProcess.destroy();
            ffmpegProcess = null;
        }
        if (ytdlProcess != null)
        {
            ytdlProcess.destroy();
            ytdlProcess = null;
        }
        super.close();
    }
}
