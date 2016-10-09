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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class RemoteStream extends AudioStream
{
    //Represent the processes that control the Python Youtube-dl and the FFmpeg program.
    private Process ytdlProcess;
    private Process ffmpegProcess;

    //Async threads that deal with the piping of the outputs of the processes.
    private Thread ytdlToFFmpegThread;
    private Thread ytdlErrGobler;
    private Thread ffmpegErrGobler;

    private List<String> ytdlLaunchArgs;
    private List<String> ffmpegLaunchArgs;
    private AudioTimestamp timestamp = AudioTimestamp.fromSeconds(0);

    protected RemoteStream(List<String> ytdlLaunchArgs, List<String> ffmpegLaunchArgs)
    {
        try
        {
            ProcessBuilder pBuilder = new ProcessBuilder();

            pBuilder.command(ytdlLaunchArgs);
            AbstractMusicPlayer.LOG.debug("Command: " + pBuilder.command());
            ytdlProcess = pBuilder.start();

            pBuilder.command(ffmpegLaunchArgs);
            AbstractMusicPlayer.LOG.debug("Command: " + pBuilder.command());
            ffmpegProcess = pBuilder.start();

            final Process ytdlProcessF = ytdlProcess;
            final Process ffmpegProcessF = ffmpegProcess;

            ytdlToFFmpegThread = new Thread("RemoteSource ytdlToFFmpeg Bridge")
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
                        //If the pipe being closed caused this problem, it was because it tried to write when it closed.
                        String msg = e.getMessage().toLowerCase();
                        if (e.getMessage().contains("The pipe has been ended") || e.getMessage().contains("Broken pipe"))
                            AbstractMusicPlayer.LOG.trace("RemoteStream encountered an 'error' : " + e.getMessage() + " (not really an error.. probably)");
                        else
                            AbstractMusicPlayer.LOG.log(e);
                    }
                    finally
                    {
                        try
                        {
                            if (fromYTDL != null)
                                fromYTDL.close();
                        }
                        catch (Throwable e) {}
                        try
                        {
                            if (toFFmpeg != null)
                                toFFmpeg.close();
                        }
                        catch (Throwable e) {}
                    }
                }
            };

            ytdlErrGobler = new Thread("RemoteStream ytdlErrGobler")
            {
                @Override
                public void run()
                {
                    InputStream fromYTDL = null;
                    try
                    {
                        fromYTDL = ytdlProcessF.getErrorStream();
                        if (fromYTDL == null)
                            AbstractMusicPlayer.LOG.fatal("RemoteStream: YTDL-ErrGobler: fromYTDL is null");

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        while (!isInterrupted() && ((amountRead = fromYTDL.read(buffer)) > -1))
                        {
                            AbstractMusicPlayer.LOG.warn("ERR YTDL: " + new String(Arrays.copyOf(buffer, amountRead)));
                        }
                    }
                    catch (IOException e)
                    {
                        AbstractMusicPlayer.LOG.log(e);
                    }
                    finally
                    {
                        try
                        {
                            if (fromYTDL != null)
                                fromYTDL.close();
                        }
                        catch (Throwable ignored) {}
                    }
                }
            };

            ffmpegErrGobler = new Thread("RemoteStream ffmpegErrGobler")
            {
                @Override
                public void run()
                {
                    InputStream fromFFmpeg = null;
                    try
                    {
                        fromFFmpeg = ffmpegProcessF.getErrorStream();
                        if (fromFFmpeg == null)
                            AbstractMusicPlayer.LOG.fatal("RemoteStream: FFmpeg-ErrGobler: fromYTDL is null");

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
                        AbstractMusicPlayer.LOG.log(e);
                    }
                    finally
                    {
                        try
                        {
                            if (fromFFmpeg != null)
                                fromFFmpeg.close();
                        }
                        catch (Throwable ignored) {}
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
            AbstractMusicPlayer.LOG.log(e);
            try
            {
                close();
            }
            catch (IOException e1)
            {
                AbstractMusicPlayer.LOG.log(e1);
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
        try
        {
            if (in != null)
            {
                in.close();
                in = null;
            }
        }
        catch (Throwable ignored) {}
        try
        {
            if (ytdlToFFmpegThread != null)
            {
                ytdlToFFmpegThread.interrupt();
                ytdlToFFmpegThread = null;
            }
        }
        catch (Throwable ignored) {}
        try
        {
            if (ytdlErrGobler != null)
            {
                ytdlErrGobler.interrupt();
                ytdlErrGobler = null;
            }
        }
        catch (Throwable ignored) {}
        try
        {
            if (ffmpegErrGobler != null)
            {
                ffmpegErrGobler.interrupt();
                ffmpegErrGobler = null;
            }
        }
        catch (Throwable ignored) {}
        try
        {
            if (ffmpegProcess != null)
            {
                ffmpegProcess.destroyForcibly();
                ffmpegProcess = null;
            }
        }
        catch (Throwable ignored) {}
        try
        {
            if (ytdlProcess != null)
            {
                ytdlProcess.destroyForcibly();
                ytdlProcess = null;
            }
        }
        catch (Throwable ignored) {}
        try
        {
            super.close();
        }
        catch (Throwable ignored) {}
    }
}
