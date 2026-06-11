import java.util.regex.*;

public class TestMatrix {
    public static void main(String[] args) {
        String s = "\\begin{pmatrix}\n a & b \\\\\n c & d\n\\end{pmatrix}";
        
        s = s.replace("\\left", "").replace("\\right", "");
        s = s.replace("\\{", "{").replace("\\}", "}");
        s = s.replace("\\,", " ").replace("\\!", "").replace("\\;", " ").replace("\\:", " ");
        s = s.replace("\\\\", "\n");
        
        Pattern p = Pattern.compile("\\\\begin\\{(p?matrix|b?matrix|Bmatrix|vmatrix|Vmatrix|array)\\}([\\s\\S]*?)\\\\end\\{\\1\\}", Pattern.DOTALL);
        Matcher m = p.matcher(s);
        StringBuffer sb = new StringBuffer();
        while(m.find()) {
            String inner = m.group(2);
            String[] rows = inner.split("\n"); // in Kotlin I used \\\\ which splits by \\, but wait! In Kotlin I used "\\\\" to split!
            StringBuilder table = new StringBuilder("[\n");
            for(String row : rows) {
                if(row.trim().isEmpty()) continue;
                String[] cols = row.split("&");
                table.append(String.join(" | ", cols)).append("\n");
            }
            table.append("]");
            m.appendReplacement(sb, Matcher.quoteReplacement(table.toString()));
        }
        m.appendTail(sb);
        s = sb.toString();
        
        System.out.println(s);
    }
}
