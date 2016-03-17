package net.dv8tion.jda.player.source;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Austin on 3/6/2016.
 */
public interface AudioSource
{
    String getSource();
    AudioInfo getInfo();
    AudioStream asStream();
    File asFile(String path, boolean deleteOnExists) throws FileAlreadyExistsException, FileNotFoundException;
}
