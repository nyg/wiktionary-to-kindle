package edu.self.w2k.model;

import java.util.List;

final class Lists {

    private Lists() {}

    static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
