package edu.self.w2k.convert;

import java.io.IOException;

public class EbookConverterNotFoundException extends IOException {

    public EbookConverterNotFoundException() {
        super("ebook-convert not found. Install Calibre from https://calibre-ebook.com/download "
                + "and ensure it is on your PATH, or pass --ebook-convert <path>.");
    }
}
