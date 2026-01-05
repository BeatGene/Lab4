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
            int line = match.getLine();
            int col = match.getColumn();
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
