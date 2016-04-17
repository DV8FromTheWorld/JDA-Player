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

import org.json.JSONObject;

public class AudioInfo
{
    protected JSONObject jsonInfo;
    protected String title;
    protected String origin;
    protected String id;
    protected String encoding;
    protected String description;
    protected String extractor;
    protected String thumbnail;
    protected String error;
    protected AudioTimestamp duration;

    public JSONObject getJsonInfo()
    {
        return jsonInfo;
    }

    public String getTitle()
    {
        return title;
    }

    public String getOrigin()
    {
        return origin;
    }

    public String getId()
    {
        return id;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public String getDescription()
    {
        return description;
    }

    public String getExtractor()
    {
        return extractor;
    }

    public String getThumbnail()
    {
        return thumbnail;
    }

    public String getError()
    {
        return error;
    }

    public AudioTimestamp getDuration()
    {
        return duration;
    }
}