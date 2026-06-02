package com.tpi.pokemon.game.engine.effect.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class Xy1CardByCardVerificationDocTest {
    @Test
    void phase11G5DocumentListsAll146OfficialXy1CardsWithoutDuplicates() throws IOException {
        List<String> rows = xy1Rows();

        assertThat(rows).hasSize(146);
        List<String> cardIds = rows.stream().map(this::cardIdFromRow).toList();
        assertThat(cardIds).doesNotHaveDuplicates();
        assertThat(cardIds).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, 146).mapToObj(number -> "xy1-" + number).toList());
    }

    @Test
    void phase11G5DocumentHasNoUnclassifiedPendingRows() throws IOException {
        assertThat(xy1Rows())
                .noneMatch(row -> row.contains("PENDING_ROW_CREATION"))
                .noneMatch(row -> row.contains("PENDING_CARD_BY_CARD_CLASSIFICATION"));
    }

    @Test
    void phase11G5DocumentDoesNotMarkOpenGapsAsFullyTested() throws IOException {
        for (String row : xy1Rows()) {
            List<String> columns = columns(row);
            String cardId = columns.get(0);
            String implementationStatus = columns.get(10);
            String testedStatus = columns.get(11);
            String gapType = columns.get(12);

            if (testedStatus.equals("no")
                    || implementationStatus.contains("NOT_IMPLEMENTED_YET")
                    || implementationStatus.contains("REQUIRES_CUSTOM_HANDLER")
                    || implementationStatus.contains("REQUIRES_UI_SELECTION")
                    || !gapType.equals("NO_OPEN_GAP_FOR_DECLARED_SCOPE")) {
                assertThat(implementationStatus)
                        .as(cardId + " must not claim FULLY_TESTED while an open gap remains")
                        .doesNotContain("FULLY_TESTED");
            }
        }
    }

    private List<String> xy1Rows() throws IOException {
        return Files.readAllLines(resolveDocPath()).stream()
                .filter(line -> line.startsWith("| xy1-"))
                .toList();
    }

    private String cardIdFromRow(String row) {
        return columns(row).get(0);
    }

    private List<String> columns(String row) {
        return java.util.Arrays.stream(row.split("\\|", -1))
                .skip(1)
                .limit(14)
                .map(String::trim)
                .toList();
    }

    private Path resolveDocPath() {
        Path fromRepositoryRoot = Paths.get("docs", "12-xy1-card-by-card-verification.md");
        if (Files.exists(fromRepositoryRoot)) {
            return fromRepositoryRoot;
        }
        return Paths.get("..", "docs", "12-xy1-card-by-card-verification.md");
    }
}
