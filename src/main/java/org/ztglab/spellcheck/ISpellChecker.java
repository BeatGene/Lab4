package org.ztglab.spellcheck;

import java.util.List;

public interface ISpellChecker {
    /**
     * @param text 输入文本
     * @return 拼写错误结果列表，每个元素为可读字符串（如: 第1行，第5列: "recieve" -> 建议: receive）
     */
    List<String> check(String text) throws Exception;
}
