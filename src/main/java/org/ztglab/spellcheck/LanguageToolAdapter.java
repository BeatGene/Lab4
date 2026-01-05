package org.ztglab.spellcheck;

import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import java.util.ArrayList;
import java.util.List;

public class LanguageToolAdapter implements ISpellChecker {

    private final JLanguageTool tool;

    public LanguageToolAdapter() {
        Language lang = new AmericanEnglish();
        this.tool = new JLanguageTool(lang);
    }

    @Override
    public List<String> check(String text) throws Exception {
        List<RuleMatch> matches = tool.check(text);
        List<String> result = new ArrayList<>();

        for (RuleMatch match : matches) {
            int fromPos = match.getFromPos();
            int line = 1;
            int col = 1;
            for (int i = 0; i < fromPos; i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
            }

            String wrong = text.substring(match.getFromPos(), match.getToPos());
            String suggestion = match.getSuggestedReplacements().isEmpty()
                    ? "（无建议）"
                    : match.getSuggestedReplacements().get(0);

            result.add(String.format(
                    "第%d行，第%d列: \"%s\" -> 建议: %s",
                    line, col, wrong, suggestion
            ));
        }

        return result;
    }
}
