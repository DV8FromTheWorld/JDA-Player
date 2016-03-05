package net.dv8tion.jda.player;

import net.dv8tion.jda.audio.player.Player;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

/**
 * Created by Austin on 3/4/2016.
 */
public class MyPlayer extends Player
{
    protected String url;
    protected boolean started = false;
    protected boolean playing = false;
    protected boolean paused = false;
    protected boolean stopped = true;

    public MyPlayer(String url)
    {
        this.url = url;
        AudioSource source = new AudioSource(url);
        try
        {
            this.setAudioSource(AudioSystem.getAudioInputStream(source.asStream()));
        }
        catch (UnsupportedAudioFileException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
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
        try
        {
            audioSource.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void restart()
    {
        try
        {
            reset();
            AudioSource source = new AudioSource(url);
            this.setAudioSource(AudioSystem.getAudioInputStream(source.asStream()));
            play();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedAudioFileException e)
        {
            e.printStackTrace();
        }
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
        try
        {
            audioSource.close();
            audioSource = null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
