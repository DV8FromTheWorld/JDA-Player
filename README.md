# JDA-Player

JDA-Player is a music player implementation for JDA (Java Discord API) which provides functionality to stream audio to a Discord VoiceChannel. 

## Deprecated

JDA-Player is deprecated and we will no longer offer support for possible issues and bugs.

Please use a more powerful library like [**lavaplayer**](https://github.com/sedmelluq/lavaplayer), which is way more efficient and optimized, and runs exclusively on Java (no need for ffmpeg/ytdl/python).

If you have any questions feel free to join the [JDA Guild on Discord](https://discord.gg/0hMr4ce0tIk3pSjp) and request support in the lavaplayer channel.

## Information

JDA-Player provides functionality to do this from both local files (LocalSource) and from remote sources like Youtube and Soundcloud (RemoteSource).
It uses Youtube-dl for remote resource downloading, a combination of Youtube-dl and FFprobe for info gathering, and FFmpeg for audio encoding/decoding and general processing.

JDA-Player also has support for [Discord4J](https://github.com/austinv11/Discord4J), an alternate api to JDA. This separate verison is refered to as D4J-Player, but it remains within this repo.

## Wiki/Examples

We provide a small set of examples in the [Repository's Wiki](https://github.com/DV8FromTheWorld/JDA-Player/wiki).
If you need further help you can join the [Discord Guild](https://discordapp.com/invite/0hMr4ce0tIk3pSjp) to ask any possible questions.

## Dependencies

This project **requires**:
* Java 8
* [JDA](https://github.com/DV8FromTheWorld/JDA) or [Discord4J](https://github.com/austinv11/Discord4J)
* Python 2.7/2.8/3.4+
* FFmpeg
* FFprobe
* [Youtube-dl](https://rg3.github.io/youtube-dl/download.html)

[Installation](https://github.com/DV8FromTheWorld/JDA-Player/wiki/Installation)
