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

import net.dv8tion.jda.player.AbstractMusicPlayer;
import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.audio.IAudioProvider;
import sx.blah.discord.handle.audio.impl.AudioManager;

import java.io.IOException;
import java.util.Arrays;

public class MusicPlayer extends AbstractMusicPlayer implements IAudioProvider
{

    public static final int PCM_FRAME_SIZE = 4;
    private byte[] buffer = new byte[AudioManager.OPUS_FRAME_SIZE * PCM_FRAME_SIZE];
    private byte[] noData = new byte[0];

    @Override
    public boolean isReady()
    {
        return state.equals(State.PLAYING);
    }

    @Override
    public byte[] provide()
    {

//        if (currentAudioStream == null || audioFormat == null)
//            throw new IllegalStateException("The Audio source was never set for this player!\n" +
//                    "Please provide an AudioInputStream using setAudioSource.");
        try
        {
            int amountRead = currentAudioStream.read(buffer, 0, buffer.length);
            if (amountRead > -1)
            {
                if (amountRead < buffer.length) {
                    Arrays.fill(buffer, amountRead, buffer.length - 1, (byte) 0);
                }
                if (volume != 1) {
                    short sample;
                    for (int i = 0; i < buffer.length; i+=2) {
                        sample = (short)((buffer[ i+ 1] & 0xff) | (buffer[i] << 8));
                        sample = (short) (sample * volume);
                        buffer[i + 1] = (byte)(sample & 0xff);
                        buffer[i] = (byte)((sample >> 8) & 0xff);
                    }
                }
                return buffer;
            }
            else
            {
                sourceFinished();
                return noData;
            }
        }
        catch (IOException e)
        {
            LOG.warn("A source closed unexpectantly? Oh well I guess...");
            sourceFinished();
        }
        return noData;
    }
}
