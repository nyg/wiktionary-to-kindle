package edu.self.w2k.download;

import java.io.IOException;

public interface DumpDownloader {

    /**
     * Downloads the dump, or reports it as already present.
     *
     * @return where the dump now lives, and whether it had to be transferred
     * @throws java.io.InterruptedIOException if the calling thread was interrupted mid-transfer
     * @throws IOException                    if the dump could not be obtained
     */
    DownloadResult download() throws IOException;
}
