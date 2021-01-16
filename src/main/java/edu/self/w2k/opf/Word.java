package edu.self.w2k.opf;

public class Word {

    private final String lemma;
    private final String meanings;

    public Word(String lemma, String meanings) {
        this.lemma = lemma;
        this.meanings = meanings;
    }

    public String getLemma() {
        return lemma;
    }

    public String getMeanings() {
        return meanings;
    }
}
