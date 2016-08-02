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

package net.dv8tion.jda.player.hooks;

import net.dv8tion.jda.player.hooks.events.*;

public abstract class PlayerListenerAdapter implements PlayerEventListener
{

    public void onPlay(PlayEvent event) {}
    public void onPause(PauseEvent event) {}
    public void onResume(ResumeEvent event) {}
    public void onStop(StopEvent event) {}
    public void onSkip(SkipEvent event) {}
    public void onFinish(FinishEvent event) {}
    public void onRepeat(RepeatEvent event) {}
    public void onReload(ReloadEvent event) {}
    public void onNext(NextEvent event) {}

    @Override
    public void onEvent(PlayerEvent event)
    {
        if (event instanceof PlayEvent)
            onPlay((PlayEvent) event);
        else if (event instanceof PauseEvent)
            onPause((PauseEvent) event);
        else if (event instanceof ResumeEvent)
            onResume((ResumeEvent) event);
        else if (event instanceof StopEvent)
            onStop((StopEvent) event);
        else if (event instanceof SkipEvent)
            onSkip((SkipEvent) event);
        else if (event instanceof FinishEvent)
            onFinish((FinishEvent) event);
        else if (event instanceof RepeatEvent)
            onRepeat((RepeatEvent) event);
        else if (event instanceof ReloadEvent)
            onReload((ReloadEvent) event);
        else if(event instanceof NextEvent)
            onNext((NextEvent) event);
    }
}
