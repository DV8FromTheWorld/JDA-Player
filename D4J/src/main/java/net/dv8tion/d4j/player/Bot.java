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

package net.dv8tion.d4j.player;

import net.dv8tion.jda.player.Playlist;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;
import org.json.JSONException;
import org.json.JSONObject;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.audio.impl.DefaultProvider;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Bot
{
    public static final float DEFAULT_VOLUME = 0.35f;

    public static void main(String[] args)
    {
        try
        {
            JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get("Config.json"))));
            IDiscordClient client = new ClientBuilder()
                    .withToken(obj.getString("token"))
                    .login();
            client.getDispatcher().registerListener(new Bot());
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("The config was not populated. Please provide a token.");
        }
        catch (DiscordException e)
        {
            System.out.println(e.getMessage());
        }
        catch (JSONException e)
        {
            System.err.println("Encountered a JSON error. Most likely caused due to an outdated or ill-formated config.\n" +
                    "Please delete the config so that it can be regenerated. JSON Error:\n");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            JSONObject obj = new JSONObject();
            obj.put("botToken", "");
            try
            {
                Files.write(Paths.get("Config.json"), obj.toString(4).getBytes());
                System.out.println("No config file was found. Config.json has been generated, please populate it!");
            }
            catch (IOException e1)
            {
                System.out.println("No config file was found and we failed to generate one.");
                e1.printStackTrace();
            }
        }
    }

    //Current commands
    // join [name]  - Joins a voice channel that has the provided name
    // leave        - Leaves the voice channel that the bot is currently in.
    // play         - Plays songs from the current queue. Starts playing again if it was previously paused
    // play [url]   - Adds a new song to the queue and starts playing if it wasn't playing already
    // pause        - Pauses audio playback
    // stop         - Completely stops audio playback, skipping the current song.
    // skip         - Skips the current song, automatically starting the next
    // nowplaying   - Prints information about the currently playing song (title, current time)
    // list         - Lists the songs in the queue
    // volume [val] - Sets the volume of the MusicPlayer [0.0 - 1.0]
    // restart      - Restarts the current song or restarts the previous song if there is no current song playing.
    // repeat       - Makes the player repeat the currently playing song
    // reset        - Completely resets the player, fixing all errors and clearing the queue.
    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) throws RateLimitException, DiscordException, MissingPermissionsException
    {
        IMessage msg = event.getMessage();
        //If the person who sent the message isn't a known auth'd user, ignore.
        try
        {
            //We specifically reread the admins.txt each time a command is run so that we can update the admins.txt
            // while the bot is running. Basically this is just me being lazy.
            if (!Files.readAllLines(Paths.get("admins.txt")).contains(msg.getAuthor().getID()))
                return;
        }
        catch (IOException e)
        {
            //Fail silently. Allows the admin system to be "disabled" when admins.txt does not exist.
//            e.printStackTrace();
        }

        String message = event.getMessage().getContent();
        IAudioManager manager = msg.getGuild().getAudioManager();
        MusicPlayer player;
        if (manager.getAudioProvider() instanceof DefaultProvider)
        {
            player = new MusicPlayer();
            player.setVolume(DEFAULT_VOLUME);
            manager.setAudioProvider(player);
        }
        else
        {
            player = (MusicPlayer) manager.getAudioProvider();
        }

        if (message.startsWith("volume "))
        {
            float volume = Float.parseFloat(message.substring("volume ".length()));
            volume = Math.min(1F, Math.max(0F, volume));
            player.setVolume(volume);
            msg.getChannel().sendMessage("Volume was changed to: " + volume);
        }

        if (message.equals("list"))
        {
            List<AudioSource> queue = player.getAudioQueue();
            if (queue.isEmpty())
            {
                msg.getChannel().sendMessage("The queue is currently empty!");
                return;
            }


            MessageBuilder builder = new MessageBuilder(msg.getClient());
            builder.appendContent("__Current Queue.  Entries: " + queue.size() + "__\n");
            for (int i = 0; i < queue.size() && i < 10; i++)
            {
                AudioInfo info = queue.get(i).getInfo();
//                builder.appendString("**(" + (i + 1) + ")** ");
                if (info == null)
                    builder.appendContent("*Could not get info for this song.*");
                else
                {
                    AudioTimestamp duration = info.getDuration();
                    builder.appendContent("`[");
                    if (duration == null)
                        builder.appendContent("N/A");
                    else
                        builder.appendContent(duration.getTimestamp());
                    builder.appendContent("]` " + info.getTitle() + "\n");
                }
            }

            boolean error = false;
            int totalSeconds = 0;
            for (AudioSource source : queue)
            {
                AudioInfo info = source.getInfo();
                if (info == null || info.getDuration() == null)
                {
                    error = true;
                    continue;
                }
                totalSeconds += info.getDuration().getTotalSeconds();
            }

            builder.appendContent("\nTotal Queue Time Length: " + AudioTimestamp.fromSeconds(totalSeconds).getTimestamp());
            if (error)
                builder.appendContent("`An error occured calculating total time. Might not be completely valid.");
            builder.withChannel(msg.getChannel()).build();
        }
        if (message.equals("nowplaying"))
        {
            if (player.isPlaying())
            {
                AudioTimestamp currentTime = player.getCurrentTimestamp();
                AudioInfo info = player.getCurrentAudioSource().getInfo();
                if (info.getError() == null)
                {
                    msg.getChannel().sendMessage(
                            "**Playing:** " + info.getTitle() + "\n" +
                                    "**Time:**    [" + currentTime.getTimestamp() + " / " + info.getDuration().getTimestamp() + "]");
                }
                else
                {
                    msg.getChannel().sendMessage(
                            "**Playing:** Info Error. Known source: " + player.getCurrentAudioSource().getSource() + "\n" +
                                    "**Time:**    [" + currentTime.getTimestamp() + " / (N/A)]");
                }
            }
            else
            {
                msg.getChannel().sendMessage("The player is not currently playing anything!");
            }
        }

        //Start an audio connection with a VoiceChannel
        if (message.startsWith("join "))
        {
            //Separates the name of the channel so that we can search for it
            String chanName = message.substring(5);

            //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
            IVoiceChannel channel = msg.getGuild().getVoiceChannels().stream().filter(
                    vChan -> vChan.getName().equalsIgnoreCase(chanName))
                    .findFirst().orElse(null);  //If there isn't a matching name, return null.
            if (channel == null)
            {
                msg.getChannel().sendMessage("There isn't a VoiceChannel in this Guild with the name: '" + chanName + "'");
                return;
            }
            channel.join();
        }
        //Disconnect the audio connection with the VoiceChannel.
        if (message.equals("leave"))
        {
            IDiscordClient client = msg.getClient();
            IVoiceChannel chan = client.getConnectedVoiceChannels()
                    .stream().filter(c -> c.getGuild() == msg.getGuild()).findFirst().orElse(null);
            if (chan != null)
                chan.leave();
        }

        if (message.equals("skip"))
        {
            player.skipToNext();
            msg.getChannel().sendMessage("Skipped the current song.");
        }

        if (message.equals("repeat"))
        {
            if (player.isRepeat())
            {
                player.setRepeat(false);
                msg.getChannel().sendMessage("The player has been set to **not** repeat.");
            }
            else
            {
                player.setRepeat(true);
                msg.getChannel().sendMessage("The player been set to repeat.");
            }
        }

        if (message.equals("shuffle"))
        {
            if (player.isShuffle())
            {
                player.setShuffle(false);
                msg.getChannel().sendMessage("The player has been set to **not** shuffle.");
            }
            else
            {
                player.setShuffle(true);
                msg.getChannel().sendMessage("The player been set to shuffle.");
            }
        }

        if (message.equals("reset"))
        {
            player.stop();
            player = new MusicPlayer();
            player.setVolume(DEFAULT_VOLUME);
            manager.setAudioProvider(player);
            msg.getChannel().sendMessage("Music player has been completely reset.");
        }

        //Start playing audio with our FilePlayer. If we haven't created and registered a FilePlayer yet, do that.
        if (message.startsWith("play"))
        {
            //If no URL was provided.
            if (message.equals("play"))
            {
                if (player.isPlaying())
                {
                    msg.getChannel().sendMessage("Player is already playing!");
                    return;
                }
                else if (player.isPaused())
                {
                    player.play();
                    msg.getChannel().sendMessage("Playback as been resumed.");
                }
                else
                {
                    if (player.getAudioQueue().isEmpty())
                        msg.getChannel().sendMessage("The current audio queue is empty! Add something to the queue first!");
                    else
                    {
                        player.play();
                        msg.getChannel().sendMessage("Player has started playing!");
                    }
                }
            }
            else if (message.startsWith("play "))
            {
                String infoMsg = "";
                String url = message.substring("play ".length());
                Playlist playlist = Playlist.getPlaylist(url, event.getMessage().getGuild().getID());
                List<AudioSource> sources = new LinkedList(playlist.getSources());
//                AudioSource source = new RemoteSource(url);
//                AudioSource source = new LocalSource(new File(url));
//                AudioInfo info = source.getInfo();   //Preload the audio info.
                if (sources.size() > 1)
                {
                    msg.getChannel().sendMessage("Found a playlist with **" + sources.size() + "** entries.\n" +
                            "Proceeding to gather information and queue sources. This may take some time...");
                    final MusicPlayer fPlayer = player;
                    Thread thread = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            for (Iterator<AudioSource> it = sources.iterator(); it.hasNext();)
                            {
                                AudioSource source = it.next();
                                AudioInfo info = source.getInfo();
                                List<AudioSource> queue = fPlayer.getAudioQueue();
                                if (info.getError() == null)
                                {
                                    queue.add(source);
                                    if (fPlayer.isStopped())
                                        fPlayer.play();
                                }
                                else
                                {
                                    try
                                    {
                                        msg.getChannel().sendMessage("Error detected, skipping source. Error:\n" + info.getError());
                                    }
                                    catch (MissingPermissionsException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    catch (RateLimitException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    catch (DiscordException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    it.remove();
                                }
                            }
                            try
                            {
                                msg.getChannel().sendMessage("Finished queuing provided playlist. Successfully queued **" + sources.size() + "** sources");
                            }
                            catch (MissingPermissionsException e)
                            {
                                e.printStackTrace();
                            }
                            catch (RateLimitException e)
                            {
                                e.printStackTrace();
                            }
                            catch (DiscordException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    };
                    thread.start();
                }
                else
                {
                    AudioSource source = sources.get(0);
                    AudioInfo info = source.getInfo();
                    if (info.getError() == null)
                    {
                        player.getAudioQueue().add(source);
                        infoMsg += "The provided URL has been added the to queue";
                        if (player.isStopped())
                        {
                            player.play();
                            infoMsg += " and the player has started playing";
                        }
                        msg.getChannel().sendMessage(infoMsg + ".");
                    }
                    else
                    {
                        msg.getChannel().sendMessage("There was an error while loading the provided URL.\n" +
                                "Error: " + info.getError());
                    }
                }
            }
        }
        if (message.equals("pause"))
        {
            player.pause();
            msg.getChannel().sendMessage("Playback has been paused.");
        }
        if (message.equals("stop"))
        {
            player.stop();
            msg.getChannel().sendMessage("Playback has been completely stopped.");
        }
        if (message.equals("restart"))
        {
            if (player.isStopped())
            {
                if (player.getPreviousAudioSource() != null)
                {
                    player.reload(true);
                    msg.getChannel().sendMessage("The previous song has been restarted.");
                }
                else
                {
                    msg.getChannel().sendMessage("The player has never played a song, so it cannot restart a song.");
                }
            }
            else
            {
                player.reload(true);
                msg.getChannel().sendMessage("The currently playing song has been restarted!");
            }
        }
    }
}
