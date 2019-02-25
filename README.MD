# HLS server sample

Braindead HLS server that shows how http live streaming works.
You can read more about what HLS is and how it works [here](https://en.wikipedia.org/wiki/HTTP_Live_Streaming).

## Compiling

The project can be compiled using the following commands:

```bash
mvn package appassembler:assemble
```

Mind you I didn't include a wrapper here, but that's on me.

In addition to compiling the project, you will need to generate HLS-compatible segmented
video in `appassembler/bin/target` folder. Due to HLS limitations it must use h264 
video codec and be in mp4 container. Personally since I already had a h264 video, I just
used the following command with ffmpeg

```bash
ffmpeg -i pretty_video.mp4 -c copy -f segment -segment_list playlist.m3u8 playlist%d.ts
```

The argument explanations are as follows:

* `-i` - input file
* `-c` - Codec for encoding. By default, Ffmpeg reencodes the video,
 when there is no codec provided. Thus it has a special codec 
 called `copy` which disables reencoding mechanism and just runs
 the muxer/demuxer mechanism. This saves on resources and somewhat
 makes it "livestream ready".
* `-f` - target format. Segment format permits separating the file
 into HLS compatible segments, which are then downloaded via HTTP
 and rendered using `Blob` javascript API. You can read more about
 how segmenting works in [ffmpeg documentation](https://ffmpeg.org/ffmpeg-formats.html#segment_002c-stream_005fsegment_002c-ssegment).
* `-segment_list` - see `-f`. Filename, which is used for video's metadata.
* `%d` - in filename, when using `segment` format, denotes the number which 
 segment is the generated file. 

## Running

Since this project depends on `AppAssembler` plugin, provided by codehaus,
invoking `appassembler:assemble` goal creates Windows batch and 
MacOS/Linux shell scripts that run the project in `target/appassembler/`
directory. In addition, this makes a project deployable to target environment,
since all of its dependencies are copied into `repo` folder.

To run the project, execute `app` in `bin` folder.

By default, the project looks for files to serve in `{WORKING_DIRECTORY}/target`.
If you ran the project with generated binary, your working directory will
be parent to `bin` folder.

## License

Go nuts. This is just a proof of concept. Most of the time people do this with apache or something.