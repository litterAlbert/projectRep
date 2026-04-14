public class TestEscape2 {
    public static void main(String[] args) {
        String url = "https://oss-cn-hangzhou.aliyuncs.com/test.pdf";
        System.out.println("ESCAPED: " + url.replaceAll("([^a-zA-Z0-9])", "\\\\$1"));
    }
}