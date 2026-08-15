package edu.self.w2k.kaikki;

import java.io.IOException;
import java.net.URI;

public interface PageFetcher {

    String fetch(URI uri) throws IOException;
}
