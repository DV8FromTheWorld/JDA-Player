package net.dv8tion.jda.player.source;

import net.dv8tion.jda.player.MusicPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class LocalStream extends AudioStream
{
    private Process ffmpegProcess;
    private Thread ffmpegErrGobler;
    private AudioTimestamp timestamp;

    public LocalStream(List<String> ffmpegLaunchArgs)
    {
        try
        {
            ProcessBuilder pBuilder = new ProcessBuilder();

            pBuilder.command(ffmpegLaunchArgs);
            MusicPlayer.LOG.debug("Command: " + pBuilder.command());
            ffmpegProcess = pBuilder.start();

            final Process ffmpegProcessF = ffmpegProcess;

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
                            MusicPlayer.LOG.warn("fromFFmpeg is null");

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        while (!isInterrupted() && ((amountRead = fromFFmpeg.read(buffer)) > -1))
                        {
                            String info = new String(Arrays.copyOf(buffer, amountRead));
                            if (info.contains("time="))
                            {
                                Matcher m = TIME_PATTERN.matcher(info);
                                if (m.find())
                                {
                                    timestamp = AudioTimestamp.fromFFmpegTimestamp(m.group());
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        MusicPlayer.LOG.fatal(e);
                    }
                }
            };

            ffmpegErrGobler.start();
            this.in = ffmpegProcess.getInputStream();
        }
        catch (IOException e)
        {
            MusicPlayer.LOG.fatal(e);
            try
            {
                close();
            }
            catch (IOException e1)
            {
                MusicPlayer.LOG.fatal(e1);
            }
        }
    }

    @Override
    public AudioTimestamp getCurrentTimestamp()
    {
        return timestamp;
    }

    @Override
    public void close() throws IOException
    {
        if (in != null)
        {
            in.close();
            in = null;
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
        super.close();
    }
}
