# JDA-Player

JDA-Player is a music player implementation for the Java Discord Api (JDA) which provides functionality to stream audio to a Discord VoiceChannel.
It provides functionality to do this from both local files (LocalSource) and from remote sources like Youtube and Soundcloud (RemoteSource).
It uses Youtube-dl for remote resource downloading, a combination of Youtube-dl and FFprobe for info gathering, and FFmpeg for audio encoding/decoding and general processing.

## Wiki/Examples

We provide a small set of examples in the [Repository's Wiki](https://github.com/DV8FromTheWorld/JDA-Player/wiki).
If you need further help you can join the [Discord Guild](https://discordapp.com/invite/0hMr4ce0tIk3pSjp) to ask any possible questions.

## Dependencies

This project **requires**:
* Java 8
* [JDA](https://github.com/DV8FromTheWorld/JDA)
* Python 2.7/2.8/3.4+
* FFmpeg
* FFprobe
* [Youtube-dl](https://rg3.github.io/youtube-dl/download.html)

[Installation](https://github.com/DV8FromTheWorld/JDA-Player/wiki/Installation)
