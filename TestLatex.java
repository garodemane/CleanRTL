import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestLatex {
    static Map<Character, String> superMap = new HashMap<>();
    static Map<Character, String> subMap = new HashMap<>();

    static {
        superMap.put('0', "\u2070"); superMap.put('1', "\u00B9"); superMap.put('2', "\u00B2");
        superMap.put('+', "\u207A"); superMap.put('-', "\u207B"); superMap.put('=', "\u207C");
        superMap.put('x', "\u02E3"); superMap.put('n', "\u207F");
        
        subMap.put('0', "\u2080"); subMap.put('1', "\u2081"); subMap.put('2', "\u2082");
        subMap.put('+', "\u208A"); subMap.put('-', "\u208B"); subMap.put('=', "\u208C");
        subMap.put('x', "\u2093"); subMap.put('n', "n");
    }

    static String toSup(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(superMap.getOrDefault(c, String.valueOf(c)));
        return sb.toString();
    }

    static String toSub(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(subMap.getOrDefault(c, String.valueOf(c)));
        return sb.toString();
    }

    static String latexSymbolToStr(String sym) {
        return sym.replace("\\infty", "\u221E");
    }

    public static void main(String[] args) {
        String s = "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt(\\pi)\n\\sum_{n=0}^{\\infty} r^n = \\frac{1}{1-r}";
        
        s = s.replace("\\left", "").replace("\\right", "");
        s = s.replace("\\{", "{").replace("\\}", "}");
        
        Matcher m1 = Pattern.compile("\\^\\{([^}]*)\\}").matcher(s);
        StringBuffer sb1 = new StringBuffer();
        while (m1.find()) {
            m1.appendReplacement(sb1, Matcher.quoteReplacement(toSup(latexSymbolToStr(m1.group(1)))));
        }
        m1.appendTail(sb1);
        s = sb1.toString();
        
        Matcher m2 = Pattern.compile("_\\{([^}]*)\\}").matcher(s);
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) {
            m2.appendReplacement(sb2, Matcher.quoteReplacement(toSub(latexSymbolToStr(m2.group(1)))));
        }
        m2.appendTail(sb2);
        s = sb2.toString();

        s = s.replace("\\int", "\u222B").replace("\\sum", "\u2211").replace("\\infty", "\u221E");
        
        System.out.println(s);
    }
}
