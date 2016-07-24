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

package net.dv8tion.jda.player;

import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.RemoteSource;
import org.json.JSONObject;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Playlist
{
    public static final List<String> YOUTUBE_DL_PLAYLIST_ARGS =
            Collections.unmodifiableList(Arrays.asList(
                    "python",               //Launch python executor
                    "./youtube-dl",         //youtube-dl program file
                    "-q",                   //quiet. No standard out.
                    "-j",                   //Print JSON
                    "--flat-playlist"       //Get ONLY the urls of the playlist if this is a playlist.
            ));
    protected List<AudioSource> sources;
    protected Queue<AudioSource> currentOrder;
    protected AudioSource endOfList;
    protected String name;
    protected boolean repeatSingle = false;
    protected boolean repeatList = false;
    protected boolean shuffle = false;

    public Playlist(String name)
    {
        this.name = name;
        this.sources = new ArrayList<>();
    }

    public List<AudioSource> getSources()
    {
        return Collections.unmodifiableList(sources);
    }

    public static Playlist getPlaylist(String url)
    {
        List<String> infoArgs = new LinkedList<>();
        infoArgs.addAll(YOUTUBE_DL_PLAYLIST_ARGS);
        infoArgs.add("--"); //Url separator. Deals with YT ids that start with --
        infoArgs.add( url);

        //Fire up Youtube-dl and get all sources from the provided url.
        List<AudioSource> sources = new ArrayList<>();
        Scanner scan = null;
        try
        {
            Process infoProcess = new ProcessBuilder().command(infoArgs).start();
            Thread ytdlErrGobler = new Thread("RemoteStream ytdlErrGobler")
            {
                @Override
                public void run()
                {

                    try
                    {
                        InputStream fromYTDL = infoProcess.getErrorStream();
                        if (fromYTDL == null) {
                            System.out.println("fromYTDL is null");
                            return;
                        }

                        byte[] buffer = new byte[1024];
                        int amountRead = -1;
                        while (!isInterrupted() && ((amountRead = fromYTDL.read(buffer)) > -1))
                        {
                            System.out.println("ERR YTDL: " + new String(Arrays.copyOf(buffer, amountRead)));
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            };
            ytdlErrGobler.start();
            byte[] infoData = IOUtils.readFully(infoProcess.getInputStream(), -1, false);
            ytdlErrGobler.interrupt();
            if (infoData == null || infoData.length == 0)
                throw new NullPointerException("The YT-DL playlist process resulted in a null or zero-length INFO!");

            String sInfo = new String(infoData);
            scan = new Scanner(sInfo);

            JSONObject source = new JSONObject(scan.nextLine());
            if (source.has("_type"))//Is a playlist
            {
                sources.add(new RemoteSource(source.getString("url")));
                while (scan.hasNextLine())
                {
                    source = new JSONObject(scan.nextLine());
                    sources.add(new RemoteSource(source.getString("url")));
                }
            }
            else                    //Single source link
            {
                sources.add(new RemoteSource(source.getString("webpage_url")));
            }
        }
        catch (IOException e)
        {
            MusicPlayer.LOG.log(e);
        }
        finally
        {
            if (scan != null)
                scan.close();
        }

        //Now that we have all the sources we can create our Playlist object.
        Playlist playlist = new Playlist("New Playlist");
        playlist.sources = sources;
        return playlist;
    }
}
