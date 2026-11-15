package edu.self.w2k.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionarySense;

/**
 * The senses of one entry as a tree. kaikki flattens a sub-sense into its own sense whose glosses
 * list the parent's gloss first, so rendering each gloss as its own item repeats every parent once
 * per child.
 */
final class SenseOutline {

    private final Map<String, Node> roots = new LinkedHashMap<>();

    private SenseOutline() {}

    static SenseOutline of(List<WiktionarySense> senses, LabelSource labelSource) {
        SenseOutline outline = new SenseOutline();
        for (WiktionarySense sense : senses) {
            if (sense != null) {
                outline.add(sense, labelSource);
            }
        }
        return outline;
    }

    boolean isEmpty() {
        return roots.isEmpty();
    }

    void appendTo(StringBuilder sb) {
        if (roots.size() == 1) {
            sb.append("<div>");
            appendNode(sb, roots.values().iterator().next());
            sb.append("</div>");
            return;
        }
        appendList(sb, roots.values());
    }

    private void add(WiktionarySense sense, LabelSource labelSource) {
        List<String> glosses = sense.glosses().stream().filter(gloss -> !HtmlText.isBlank(gloss)).toList();
        if (glosses.isEmpty()) {
            return;
        }
        List<String> displayed = labelSource.displayGlosses(sense, glosses);
        Map<String, Node> level = roots;
        Node node = null;
        for (int i = 0; i < glosses.size(); i++) {
            String text = displayed.get(i);
            node = level.computeIfAbsent(glosses.get(i), _ -> new Node(text));
            level = node.children;
        }
        node.describe(labelSource.labelsOf(sense), sense.examples());
    }

    private static void appendList(StringBuilder sb, Collection<Node> nodes) {
        sb.append("<ol>");
        for (Node node : nodes) {
            sb.append("<li>");
            appendNode(sb, node);
            sb.append("</li>");
        }
        sb.append("</ol>");
    }

    private static void appendNode(StringBuilder sb, Node node) {
        if (!node.labels.isEmpty()) {
            sb.append("<i>(").append(HtmlText.escape(String.join(", ", node.labels))).append(")</i> ");
        }
        sb.append(HtmlText.escape(node.text));
        Examples.appendTo(sb, node.examples);
        if (!node.children.isEmpty()) {
            appendList(sb, node.children.values());
        }
    }

    private static final class Node {

        private final String text;
        private final Map<String, Node> children = new LinkedHashMap<>();
        private final List<WiktionaryExample> examples = new ArrayList<>();
        private List<String> labels = List.of();

        private Node(String text) {
            this.text = text;
        }

        private void describe(List<String> senseLabels, List<WiktionaryExample> senseExamples) {
            if (labels.isEmpty()) {
                labels = senseLabels;
            }
            examples.addAll(senseExamples);
        }
    }
}
