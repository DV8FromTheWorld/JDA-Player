package net.dv8tion.jda.player;

import net.dv8tion.jda.audio.AudioConnection;
import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.utils.SimpleLog;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * Created by Austin on 3/8/2016.
 */
public class MusicPlayer extends Player
{
    protected LinkedList<AudioSource> audioQueue = new LinkedList<>();
    protected AudioSource previousAudioSource = null;
    protected AudioSource currentAudioSource = null;
    protected State state = State.STOPPED;
    protected boolean autoContinue = true;

    protected enum State
    {
        PLAYING, PAUSED, STOPPED;
    }

    public void reload(boolean autoPlay)
    {
        if (previousAudioSource == null && currentAudioSource == null)
            throw new IllegalStateException("Cannot restart or reload a player that has never been started!");

        stop0(false);
        loadFromSource(previousAudioSource);

        if (autoPlay)
            play();
        //TODO: fire onReload
    }

    public void skipToNext()
    {
        if (audioQueue.isEmpty())
        {
            stop0(true);
            return;
        }

        stop0(false);
        loadFromSource(audioQueue.removeFirst());

        play0(false);
        //TODO: fire onSkip
    }

    public LinkedList<AudioSource> getAudioQueue()
    {
        return audioQueue;
    }

    // ============ JDA Player interface overrides =============

    @Override
    public void play()
    {
        play0(true);
    }

    @Override
    public void pause()
    {
        if (state == State.PAUSED)
            return;

        if (state == State.STOPPED)
            throw new IllegalStateException("Cannot pause a stopped player!");

        state = State.PAUSED;
        //TODO: fire onPause
    }

    @Override
    public void restart()
    {
        reload(true);
    }

    @Override
    public byte[] provide20MsAudio()
    {
        if (audioSource == null || audioFormat == null)
            throw new IllegalStateException("The Audio source was never set for this player!\n" +
                    "Please provide an AudioInputStream using setAudioSource.");
        try
        {
            int amountRead;
            byte[] audio = new byte[AudioConnection.OPUS_FRAME_SIZE * audioFormat.getFrameSize()];
            amountRead = audioSource.read(audio, 0, audio.length);
            if (amountRead > -1)
            {
                return audio;
            }
            else
            {
                if (autoContinue)
                    skipToNext();
                else
                    stop0(true);
                return null;
            }
        }
        catch (IOException e)
        {
            SimpleLog.getLog("JDAPlayer").log(e);
        }
        return null;
    }

    @Override
    public void stop()
    {
        stop0(true);
    }

    @Override
    public boolean isPlaying()
    {
        return state == State.PLAYING;
    }

    @Override
    public boolean isPaused()
    {
        return state == State.PAUSED;
    }

    @Override
    public boolean isStopped()
    {
        return state == State.STOPPED;
    }

    @Override
    public boolean isStarted()
    {
        throw new UnsupportedOperationException("MusicPlayer doesn't support this");
    }

    // ========= Internal Functions ==========

    protected void play0(boolean fireEvent)
    {
        if (state == State.PLAYING)
            return;

        if (currentAudioSource != null)
        {
            state = State.PLAYING;
            return;
        }

        if (audioQueue.isEmpty())
            throw new IllegalStateException("MusicPlayer: The audio queue is empty! Cannot start playing.");

        loadFromSource(audioQueue.removeFirst());

        state = State.PLAYING;
        //TODO: fire onPlaying
    }

    protected void stop0(boolean fireEvent)
    {
        if (state == State.STOPPED)
            return;

        state = State.STOPPED;
        try
        {
            amplitudeAudioStream.close();
            audioSource.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            amplitudeAudioStream = null;
            audioSource = null;
            previousAudioSource = currentAudioSource;
            currentAudioSource = null;
        }
        //TODO: fire onStop
    }

    protected void loadFromSource(AudioSource source)
    {
        try
        {
            InputStream stream = source.asStream();
            AudioInputStream aStream = AudioSystem.getAudioInputStream(stream);
            setAudioSource(aStream);
            currentAudioSource = source;
        }
        catch (IOException | UnsupportedAudioFileException e)
        {
            throw new IllegalArgumentException("MusicPlayer: The AudioSource failed to load!\n" +
                    "-> AudioSource url: " + source.getUrl() + "\n" +
                    "-> Error: " + e.getMessage());
        }
    }
}
