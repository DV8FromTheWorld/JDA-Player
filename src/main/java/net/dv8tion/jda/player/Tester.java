package net.dv8tion.jda.player;

import com.sun.xml.internal.ws.util.StreamUtils;
import sun.misc.IOUtils;

import java.io.*;
import java.util.Arrays;

/**
 * Created by Austin on 3/3/2016.
 */
public class Tester
{

    public static void main(String[] args) throws IOException
    {
//        String url = "https://www.youtube.com/watch?v=1ArWgZuoB7g";   //If I had you
//        String url = "https://www.youtube.com/watch?v=S7OCzDNeENg";   //2 hour anime mix
//        String url = "http://www.nicovideo.jp/watch/sm10443906";
        String url = "https://soundcloud.com/ahsoftware/v4-kaai-yuki_demo-song02-full-ver";   //If I had you
        File ytdlFile = new File("youtube-dl");

        ProcessBuilder pBuilder = new ProcessBuilder();
//        pBuilder.redirectErrorStream(true);

//        pBuilder.command("cmd", "/C", "dir");
        pBuilder.command(
                "python",
                ytdlFile.getCanonicalPath(),
//                "-F",
                "-q",   //quiet. No standard out.
                "-f", "bestaudio/best",
//                "-k", //Keep after merge
//                "-x", //Strip audio form video
                String.format("\"%s\"", url),
                "-o", "-"
        );
        System.out.println("Command: " + pBuilder.command());
        Process ytdlProcess = pBuilder.start();

//        BufferedReader in = new BufferedReader(new InputStreamReader(ytdlProcess.getInputStream()));
//        String line;
//        while ((line = in.readLine()) != null)
//        {
//            System.out.println(line);
//        }

        pBuilder.command(
                "ffmpeg",
                "-i", "-",
                "-f", "mp3",
                "-"
        );

        Process ffmpegProcess = pBuilder.start();

        final Thread ytdlToFFmpegThread = new Thread()
        {
            @Override
            public void run()
            {
                InputStream fromYTDL = null;
                OutputStream toFFmpeg = null;
                try
                {
                    fromYTDL = ytdlProcess.getInputStream();
                    if (fromYTDL == null)
                        System.out.println("fromYTDL is null");

                    toFFmpeg = ffmpegProcess.getOutputStream();
                    if (toFFmpeg == null)
                        System.out.println("toFFmpeg is null");

                    byte[] buffer = new byte[1024];
                    int amountRead = -1;
                    while(!isInterrupted() && ((amountRead = fromYTDL.read(buffer)) > -1))
                    {
                        toFFmpeg.write(buffer, 0, amountRead);
                        toFFmpeg.flush();
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

        final Thread ffmpegToStdout = new Thread()
        {
            @Override
            public void run()
            {
                InputStream fromFFmpeg = null;
                try
                {
                    fromFFmpeg = ffmpegProcess.getInputStream();
                    if (fromFFmpeg == null)
                        System.out.println("fromYTDL is null");

                    byte[] buffer = new byte[1024];
                    int amountRead = -1;
                    while(!isInterrupted() && ((amountRead = fromFFmpeg.read(buffer)) > -1))
                    {
                        System.out.println(new String(Arrays.copyOf(buffer, amountRead)));
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    try
                    {
                        if (fromFFmpeg != null)
                            fromFFmpeg.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        ytdlToFFmpegThread.start();
        ffmpegToStdout.start();

        try
        {
            ytdlToFFmpegThread.join();
            ffmpegToStdout.join();
        }
        catch (InterruptedException e)
        {
            ytdlToFFmpegThread.interrupt();
            ffmpegToStdout.interrupt();
            System.out.println("We were interrupted.");
        }

        ytdlProcess.destroy();
        ffmpegProcess.destroy();

        System.out.println("YTDL ExitCode: " + ytdlProcess.exitValue());
        System.out.println("FFMpeg ExitCode: " + ffmpegProcess.exitValue());

//        BufferedReader in2 = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
//        String line2;
//        while ((line2 = in2.readLine()) != null)
//        {
//            System.out.println(line2);
//        }
    }
}
