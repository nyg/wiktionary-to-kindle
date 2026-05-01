package edu.self.w2k.command;

import edu.self.w2k.download.DumpDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DownloadCommand implements Command {

    private final DumpDownloader downloader;

    @Override
    public void run() {
        downloader.download();
    }
}
