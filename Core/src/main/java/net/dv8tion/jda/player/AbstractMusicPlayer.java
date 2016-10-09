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

import net.dv8tion.jda.player.hooks.PlayerEventListener;
import net.dv8tion.jda.player.hooks.PlayerEventManager;
import net.dv8tion.jda.player.hooks.events.*;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioStream;
import net.dv8tion.jda.player.source.AudioTimestamp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class AbstractMusicPlayer
{
    public static final PlayerLog LOG = PlayerLog.getLog("JDAPlayer");
    protected PlayerEventManager eventManager = new PlayerEventManager();
    protected LinkedList<AudioSource> audioQueue = new LinkedList<>();
    protected AudioSource previousAudioSource = null;
    protected AudioSource currentAudioSource = null;
    protected AudioStream currentAudioStream = null;
    protected State state = State.STOPPED;
    protected boolean autoContinue = true;
    protected boolean shuffle = false;
    protected boolean repeat = false;
    protected float volume = 1.0F;

    protected enum State
    {
        PLAYING, PAUSED, STOPPED;
    }

    public void addEventListener(PlayerEventListener listener)
    {
        eventManager.register(listener);
    }

    public void removeEventListener(PlayerEventListener listener)
    {
        eventManager.unregister(listener);
    }

    public List<PlayerEventListener> getListeners()
    {
        return eventManager.getListeners();
    }

    public void setRepeat(boolean repeat)
    {
        this.repeat = repeat;
    }

    public boolean isRepeat()
    {
        return repeat;
    }

    public float getVolume()
    {
        return this.volume;
    }

    public void setVolume(float volume)
    {
        this.volume = volume;
    }

    public void setShuffle(boolean shuffle)
    {
        this.shuffle = shuffle;
    }

    public boolean isShuffle()
    {
        return shuffle;
    }

    public void reload(boolean autoPlay)
    {
        reload0(autoPlay, true);
    }

    public void skipToNext()
    {
        AudioSource skipped = currentAudioSource;
        playNext(false);

        eventManager.handle(new SkipEvent(this, skipped));
        if (state == State.STOPPED)
            eventManager.handle(new FinishEvent(this));
    }

    public LinkedList<AudioSource> getAudioQueue()
    {
        return audioQueue;
    }

    public AudioSource getCurrentAudioSource()
    {
        return currentAudioSource;
    }

    public AudioSource getPreviousAudioSource()
    {
        return previousAudioSource;
    }

    public AudioTimestamp getCurrentTimestamp()
    {
        if (currentAudioStream != null)
            return currentAudioStream.getCurrentTimestamp();
        else
            return null;
    }

    public void play()
    {
        play0(true);
    }

    public void pause()
    {
        if (state == State.PAUSED)
            return;

        if (state == State.STOPPED)
            throw new IllegalStateException("Cannot pause a stopped player!");

        state = State.PAUSED;
        eventManager.handle(new PauseEvent(this));
    }

    public void stop()
    {
        stop0(true);
    }

    public boolean isPlaying()
    {
        return state == State.PLAYING;
    }

    public boolean isPaused()
    {
        return state == State.PAUSED;
    }

    public boolean isStopped()
    {
        return state == State.STOPPED;
    }

    // ========= Internal Functions ==========

    protected void play0(boolean fireEvent)
    {
        if (state == State.PLAYING)
            return;

        if (currentAudioSource != null)
        {
            state = State.PLAYING;
            if (fireEvent)
                eventManager.handle(new PlayEvent(this));
            return;
        }

        if (audioQueue.isEmpty())
            throw new IllegalStateException("MusicPlayer: The audio queue is empty! Cannot start playing.");

        loadFromSource(audioQueue.removeFirst());
        state = State.PLAYING;

        if (fireEvent)
            eventManager.handle(new PlayEvent(this));
    }

    protected void stop0(boolean fireEvent)
    {
        if (state == State.STOPPED)
            return;

        state = State.STOPPED;
        try
        {
            currentAudioStream.close();
        }
        catch (IOException e)
        {
            LOG.log(e);
        }
        finally
        {
            previousAudioSource = currentAudioSource;
            currentAudioSource = null;
            currentAudioStream = null;
        }

        if (fireEvent)
            eventManager.handle(new StopEvent(this));
    }

    protected void reload0(boolean autoPlay, boolean fireEvent)
    {
        if (previousAudioSource == null && currentAudioSource == null)
            throw new IllegalStateException("Cannot restart or reload a player that has never been started!");

        stop0(false);
        loadFromSource(previousAudioSource);

        if (autoPlay)
            play0(false);
        if (fireEvent)
            eventManager.handle(new ReloadEvent(this));
    }

    protected void playNext(boolean fireEvent)
    {
        stop0(false);
        if (audioQueue.isEmpty())
        {
            if (fireEvent)
                eventManager.handle(new FinishEvent(this));
            return;
        }

        AudioSource source;
        if (shuffle)
        {
            Random rand = new Random();
            source = audioQueue.remove(rand.nextInt(audioQueue.size()));
        }
        else
            source = audioQueue.removeFirst();
        loadFromSource(source);

        play0(false);
        if (fireEvent)
            eventManager.handle(new NextEvent(this));
    }

    protected void sourceFinished()
    {
        if (autoContinue)
        {
            if(repeat)
            {
                reload0(true, false);
                eventManager.handle(new RepeatEvent(this));
            }
            else
            {
                playNext(true);
            }
        }
        else
            stop0(true);
    }

    protected void loadFromSource(AudioSource source)
    {
        AudioStream stream = source.asStream();
        currentAudioSource = source;
        currentAudioStream = stream;
    }
}
