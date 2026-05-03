package edu.self.w2k.kindling;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

interface HttpFetcher {
    Path getFile(URI uri, Path dest) throws IOException;
    String getString(URI uri) throws IOException;
}
