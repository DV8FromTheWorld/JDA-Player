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
            System.out.println("Command: " + pBuilder.command());
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
                            System.out.println("fromFFmpeg is null");

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
                        e.printStackTrace();
                    }
                }
            };

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
