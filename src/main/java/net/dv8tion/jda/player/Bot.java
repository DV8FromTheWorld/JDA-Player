package net.dv8tion.jda.player;
/**
 *    Copyright 2015-2016 Austin Keener
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

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;
import net.dv8tion.jda.player.source.RemoteSource;
import org.json.JSONObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.security.auth.login.LoginException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Bot extends ListenerAdapter
{
    private MusicPlayer player = null;
    private float volume = 0.35f;

    public Bot()
    {
        player = new MusicPlayer();
        player.setVolume(volume);
    }

    public static void main(String[] args)
    {
        try
        {
            JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get("Config.json"))));
            JDA api = new JDABuilder()
                    .setEmail(obj.getString("Email"))
                    .setPassword(obj.getString("password"))
                    .addListener(new Bot())
                    .buildBlocking();
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("The config was not populated. Please enter an email and password.");
        }
        catch (LoginException e)
        {
            System.out.println("The provided email / password combination was incorrect. Please provide valid details.");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            JSONObject obj = new JSONObject();
            obj.put("Email", "");
            obj.put("password", "");
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

    public void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {
        //If the person who sent the message isn't a known auth'd user, ignore.
        try
        {
            if (!Files.readAllLines(Paths.get("admins.txt")).contains(event.getAuthor().getId()))
                return;
        }
        catch (IOException e)
        {
            //Fail silently. Allows the admin system to be "disabled".
//            e.printStackTrace();
        }
//        if (       !event.getAuthor().getId().equals("107562988810027008")  //DV8FromTheWorld
//                && !event.getAuthor().getId().equals("110172485864964096")  //TheVolatileV
//                && !event.getAuthor().getId().equals("107562872392949760")  //GadgetTvMan
//                && !event.getAuthor().getId().equals("107490111414882304")  //Almighty Alpaca
//                && !event.getAuthor().getId().equals("122758889815932930")  //Kantenkugel
//                && !event.getAuthor().getId().equals("109871314214326272")  //Etaran
//                )
//            return;

        String message = event.getMessage().getContent();

        if (message.equals("nowplaying"))
        {
            if (player.isPlaying())
            {
                AudioTimestamp currentTime = player.getCurrentTimestamp();
                AudioInfo info = player.getCurrentAudioSource().getInfo();
                event.getChannel().sendMessage(
                        "**Playing:** " + info.getTitle() + "\n" +
                        "**Time:**    [" + currentTime.getTimestamp() + " / " + info.getDuration().getTimestamp() + "]");
            }
            else
            {
                event.getChannel().sendMessage("The player is not currently playing anything!");
            }
        }
        if (message.startsWith("file "))
        {
            AudioSource source = new RemoteSource(message.substring("file ".length()));
            try
            {
                source.asFile("ouuuuut.mp3", false);
            }
            catch (FileAlreadyExistsException e)
            {
                e.printStackTrace();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        if (message.startsWith("eval "))
        {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            try
            {
                engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");
                engine.put("event", event);
                engine.put("channel", event.getChannel());
                engine.put("api", event.getJDA());
                Object out = engine.eval(
                        "(function() {" +
                                "with (imports) {" +
                                message.substring("eval ".length()) +
                                "}" +
                                "})();");
                event.getChannel().sendMessage(out == null ? "Executed without error." : out.toString());
            }
            catch (ScriptException e)
            {
                e.printStackTrace();
            }

        }
        if (message.startsWith("volume "))
        {
            volume = Float.parseFloat(message.substring("volume ".length()));
            if (volume > 1.5f || volume < 0.0f)
                volume = 1.0f;
            player.setVolume(volume);
            event.getChannel().sendMessage("Volume was changed to: " + volume);
        }

        //Start an audio connection with a VoiceChannel
        if (message.startsWith("join "))
        {
            //Separates the name of the channel so that we can search for it
            String chanName = message.substring(5);

            //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
            VoiceChannel channel = event.getGuild().getVoiceChannels().stream().filter(
                    vChan -> vChan.getName().equalsIgnoreCase(chanName))
                    .findFirst().orElse(null);  //If there isn't a matching name, return null.
            if (channel == null)
            {
                event.getChannel().sendMessage("There isn't a VoiceChannel in this Guild with the name: '" + chanName + "'");
                return;
            }
            event.getJDA().getAudioManager().openAudioConnection(channel);
        }
        //Disconnect the audio connection with the VoiceChannel.
        if (message.equals("leave"))
            event.getJDA().getAudioManager().closeAudioConnection();

        if (message.equals("skip"))
        {
            player.skipToNext();
            event.getChannel().sendMessage("Skipped the current song.");
        }

        if (message.equals("repeat"))
        {
            if (player.isRepeat())
            {
                player.setRepeat(false);
                event.getChannel().sendMessage("The player has been set to **not** repeat.");
            }
            else
            {
                player.setRepeat(true);
                event.getChannel().sendMessage("The player been set to repeat.");
            }
        }

        if (message.equals("shuffle"))
        {
            if (player.isShuffle())
            {
                player.setShuffle(false);
                event.getChannel().sendMessage("The player has been set to **not** shuffle.");
            }
            else
            {
                player.setShuffle(true);
                event.getChannel().sendMessage("The player been set to shuffle.");
            }
        }

        if (message.equals("reset"))
        {
            player.stop();
            player = new MusicPlayer();
            player.setVolume(volume);
            event.getJDA().getAudioManager().setSendingHandler(player);
            event.getChannel().sendMessage("Music player has been completely reset.");
        }

        //Start playing audio with our FilePlayer. If we haven't created and registered a FilePlayer yet, do that.
        if (message.startsWith("play"))
        {
            //Make sure our player has been provided to JDA to provide audio data to send.
            if (event.getJDA().getAudioManager().getSendingHandler() == null)
                event.getJDA().getAudioManager().setSendingHandler(player);

            //If no URL was provided.
            if (message.equals("play"))
            {
                if (player.isPlaying())
                {
                    event.getChannel().sendMessage("Player is already playing!");
                    return;
                }
                else if (player.isPaused())
                {
                    player.play();
                    event.getChannel().sendMessage("Playback as been resumed.");
                }
                else
                {
                    if (player.getAudioQueue().isEmpty())
                        event.getChannel().sendMessage("The current audio queue is empty! Add something to the queue first!");
                    else
                    {
                        player.play();
                        event.getChannel().sendMessage("Player has started playing!");
                    }
                }
            }
            else if (message.startsWith("play "))
            {
                String msg = "";
                String url = message.substring("play ".length());
                AudioSource source = new RemoteSource(url);
                player.getAudioQueue().add(source);
                msg += "The provided URL has been added the to queue";
                if (player.isStopped())
                {
                    player.play();
                    msg += " and the player has started playing";
                }
                event.getChannel().sendMessage(msg + ".");
            }
        }
        if (message.equals("pause"))
        {
            player.pause();
            event.getChannel().sendMessage("Playback has been paused.");
        }
        if (message.equals("stop"))
        {
            player.stop();
            event.getChannel().sendMessage("Playback has been completely stopped.");
        }
        if (message.equals("restart"))
        {
            if (player.isStopped())
            {
                if (player.getPreviousAudioSource() != null)
                {
                    player.restart();
                    event.getChannel().sendMessage("The previous song has been restarted.");
                }
                else
                {
                    event.getChannel().sendMessage("The player has never played a song, so it cannot restart a song.");
                }
            }
            else
            {
                player.restart();
                event.getChannel().sendMessage("The currently playing song has been restarted!");
            }
        }
    }
}
