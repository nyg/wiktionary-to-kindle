package edu.self.w2k.command;

import java.io.IOException;

import edu.self.w2k.download.DownloadResult;
import edu.self.w2k.download.DumpDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DownloadCommand implements Command {

    private final DumpDownloader downloader;

    @Override
    public void run() throws IOException {
        execute();
    }

    /**
     * Same work as {@link #run()}, but surfaces where the dump landed. The GUI chains straight into
     * generation and so needs the path, rather than re-globbing the dumps directory for it.
     */
    public DownloadResult execute() throws IOException {
        return downloader.download();
    }
}
