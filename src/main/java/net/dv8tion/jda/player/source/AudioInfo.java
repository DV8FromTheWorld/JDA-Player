package net.dv8tion.jda.player.source;

import org.json.JSONObject;

/**
 * Created by Austin on 3/16/2016.
 */
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

    public AudioTimestamp getDuration()
    {
        return duration;
    }
}